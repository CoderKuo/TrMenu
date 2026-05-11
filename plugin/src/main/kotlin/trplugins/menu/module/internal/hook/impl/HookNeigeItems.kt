package trplugins.menu.module.internal.hook.impl

import org.bukkit.inventory.ItemStack
import pers.neige.neigeitems.manager.ItemManager
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import trplugins.menu.module.internal.hook.HookAbstract

/**
 * @author lilingfengdev
 * @date 2024/7/2 9:17
 */
class HookNeigeItems : HookAbstract() {

    private val empty = buildItem(XMaterial.BEDROCK) { name = "UNHOOKED_${super.name.uppercase()}" }

    fun getItem(id: String): ItemStack {
        if (checkHooked()) {
            return ItemManager.getItemStack(id) ?: empty
        }
        return empty
    }

    fun getItem(id: String, data: String?): ItemStack {
        if (checkHooked()) {
            return ItemManager.getItemStack(id, data) ?: empty
        }
        return empty
    }

    fun getId(itemStack: ItemStack): String {
        if (checkHooked()) {
            return ItemManager.isNiItem(itemStack)?.id ?: "UNKNOWN"
        }
        return "UNHOOKED"
    }

}