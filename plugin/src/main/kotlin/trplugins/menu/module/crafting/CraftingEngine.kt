package trplugins.menu.module.crafting

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.adaptPlayer
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.item.ItemSource

object CraftingEngine {

    /**
     * Đọc 9 item từ input slots, so sánh với tất cả recipe
     * Trả về recipe đầu tiên match, hoặc null nếu không có
     */
    fun check(session: MenuSession, spec: CraftingSpec): CraftingRecipe? {
        val grid = spec.inputSlots.map { session.receptacle?.getElement(it) }
        return spec.recipes.firstOrNull { match(grid, it) }
    }

    /**
     * Lấy kết quả: xóa input, give result, chạy actions
     */
    fun take(session: MenuSession, spec: CraftingSpec, recipe: CraftingRecipe) {
        // xóa item ở input slots
        spec.inputSlots.forEach { slot ->
            val current = session.receptacle?.getElement(slot) ?: return@forEach
            val newAmount = current.amount - 1
            session.receptacle?.setElement(if (newAmount <= 0) null else current.clone().also { it.amount = newAmount }, slot)
        }
        // xóa result slot
        session.receptacle?.setElement(null, spec.resultSlot)
        // give result cho player
        val result = buildResultItem(session, recipe.result)
        session.viewer.inventory.addItem(result)
        // chạy actions
        recipe.actions.eval(adaptPlayer(session.viewer))
        // check lại sau khi lấy
        val next = check(session, spec)
        updateResultSlot(session, spec, next)
    }

    /**
     * Cập nhật slot kết quả dựa trên recipe match hiện tại
     */
    fun updateResultSlot(session: MenuSession, spec: CraftingSpec, recipe: CraftingRecipe?) {
        val item = if (recipe != null) buildResultItem(session, recipe.result) else null
        session.receptacle?.setElement(item, spec.resultSlot)
    }

    private fun match(grid: List<ItemStack?>, recipe: CraftingRecipe): Boolean {
        return if (recipe.shapeless) matchShapeless(grid, recipe) else matchShaped(grid, recipe)
    }

    /**
     * Shaped: đúng vị trí theo shape 3x3
     */
    private fun matchShaped(grid: List<ItemStack?>, recipe: CraftingRecipe): Boolean {
        if (grid.size < 9) return false
        // parse shape thành map slot → char
        val shapeGrid = parseShape(recipe.shape)
        for (i in 0..8) {
            val expected = shapeGrid[i]
            val actual = grid.getOrNull(i)
            if (expected == null || expected == ' ') {
                if (actual != null && actual.type != Material.AIR) return false
            } else {
                val ingredientStr = recipe.ingredients[expected] ?: return false
                if (actual == null || actual.type == Material.AIR) return false
                if (!itemMatches(actual, ingredientStr)) return false
            }
        }
        return true
    }

    /**
     * Shapeless: không cần đúng vị trí, chỉ cần đủ nguyên liệu
     */
    private fun matchShapeless(grid: List<ItemStack?>, recipe: CraftingRecipe): Boolean {
        val required = recipe.ingredients.values.toMutableList()
        val available = grid.filter { it != null && it.type != Material.AIR }.toMutableList()
        if (available.size != required.size) return false
        for (req in required) {
            val found = available.indexOfFirst { itemMatches(it!!, req) }
            if (found == -1) return false
            available.removeAt(found)
        }
        return true
    }

    /**
     * Parse shape list thành array 9 char (3x3)
     * Ví dụ: ["D D", "D D", "_ S _"] → ['D',' ','D','D',' ','D',' ','S',' ']
     */
    private fun parseShape(shape: List<String>): Array<Char?> {
        val result = arrayOfNulls<Char>(9)
        shape.take(3).forEachIndexed { row, line ->
            val chars = line.replace(" ", "").padEnd(3, ' ')
            chars.take(3).forEachIndexed { col, c ->
                result[row * 3 + col] = if (c == '_') ' ' else c
            }
        }
        return result
    }

    /**
     * So sánh item với ingredient string
     * Hỗ trợ: material thường, source:NEXO:id, source:MMOItems:id, v.v.
     */
    private fun itemMatches(item: ItemStack, ingredient: String): Boolean {
        if (ingredient.contains(":")) {
            // dùng ItemSource để lấy item mẫu rồi so sánh type
            // không có session ở đây nên chỉ so sánh material name cho source đơn giản
            val parts = ingredient.split(":", limit = 2)
            if (parts[0].equals("material", true) || parts[0].equals("mat", true)) {
                return item.type.name.equals(parts[1].trim().uppercase().replace(" ", "_"), true)
            }
            // fallback: so sánh tên material
            return item.type.name.equals(ingredient.trim().uppercase().replace(" ", "_"), true)
        }
        val xMat = XMaterial.matchXMaterial(ingredient.trim().uppercase().replace(" ", "_")).orElse(null) ?: return false
        return item.type.name.equals(xMat.name, true)
    }

    private fun buildResultItem(session: MenuSession, result: CraftingResult): ItemStack {
        // hỗ trợ source: prefix
        if (result.material.contains(":")) {
            val item = ItemSource.fromSource(session, result.material.substringAfter(":"))
            if (item != null) {
                item.amount = result.amount
                return item
            }
        }
        val xMat = XMaterial.matchXMaterial(result.material.uppercase().replace(" ", "_")).orElse(XMaterial.STONE) ?: XMaterial.STONE
        return buildItem(xMat) { amount = result.amount }
    }
}
