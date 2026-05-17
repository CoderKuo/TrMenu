package trplugins.menu.module.internal.hook.impl

import net.momirealms.craftengine.bukkit.api.CraftEngineItems
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import trplugins.menu.module.internal.hook.HookAbstract

class HookCraftEngine : HookAbstract() {

    private val empty = buildItem(XMaterial.BEDROCK) { name = "UNHOOKED_${super.name.uppercase()}" }

    fun getItem(material: String, player: Player): ItemStack {
        val item = CraftEngineItems.byId(material) ?: return empty
        return item.buildBukkitItem(player)
    }
}