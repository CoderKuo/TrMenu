package trplugins.menu.module.internal.item

import org.bukkit.inventory.ItemStack
import trplugins.menu.api.event.CustomItemSourceEvent
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.hook.HookPlugin
import trplugins.menu.module.internal.hook.impl.HookSkulls
import trplugins.menu.module.internal.script.asItemStack
import trplugins.menu.module.internal.script.jexl.JexlAgent
import trplugins.menu.module.internal.script.js.JavaScriptAgent

/**
 * @author Arasple
 * @date 2021/1/27 12:04
 */
object ItemSource {

    fun fromSource(session: MenuSession, string: String): ItemStack? {
        val identifier = string.split(":", "=", limit = 2)
        val name = identifier[0].replace("-", "").uppercase()
        val id = identifier[1]
        return when (name) {
            "HEADDATABASE", "HDB" -> {
                if (id.equals("RANDOM", true)) HookPlugin.getHeadDatabase().getRandomHead()
                else HookPlugin.getHeadDatabase().getHead(id)
            }

            "SKULLS" -> {
                if (id.equals("RANDOM", true)) HookPlugin[HookSkulls::class.java].getRandomSkull()
                else HookPlugin[HookSkulls::class.java].getSkull(id)
            }

            "JAVASCRIPT", "JS" -> JavaScriptAgent.eval(session, id).asItemStack()
            "JEXL" -> JexlAgent.eval(session, id).asItemStack()
            "ORAXEN", "ORX" -> HookPlugin.getOraxen().getItem(id)
            "ITEMSADDER", "IA" -> HookPlugin.getItemsAdder().getItem(id)
            "ZAPHKIEL", "ZL" -> HookPlugin.getZaphkiel().getItem(id)
            "SXITEM", "SI" -> HookPlugin.getSXItem().getItem(id, session.placeholderPlayer)
            "MagicCosmeticsE", "MAGICE" -> HookPlugin.getMagicCosmetics().getEquipped(id, session.placeholderPlayer)
            "MagicCosmeticsI", "MAGICI" -> HookPlugin.getMagicCosmetics().getCosmeticItem(id)
            "MMOITEMS", "MI" -> HookPlugin.getMMOItems().getItem(id)
            "MAGICGEM", "MG" -> HookPlugin.getMagicGem().getItem(id)
            "NEIGEITEMS", "NI" -> {
    val dataIdx = id.indexOf(":data:")
    if (dataIdx != -1) {
        val realId = id.substring(0, dataIdx)
        val data = id.substring(dataIdx + 6)
        HookPlugin.getNeigeItem().getItem(realId, data)
    } else {
        HookPlugin.getNeigeItem().getItem(id)
    }
}
            "ECOITEMS", "EI" -> HookPlugin.getEcoItem().getItem(id)
            "HMCCosmetics", "HMC" -> HookPlugin.getHMCCosmetics().getItem(id)
            "MYTHICMOBS", "MM" -> HookPlugin.getMythicMobs().getItem(id)
            "AZUREFLOW", "AF" -> HookPlugin.getAzureFlow().getItem(id, session.placeholderPlayer)
            "PXRPG", "PX" -> HookPlugin.getPxRpg().getItem(id, session.placeholderPlayer)
            "NEXO" -> HookPlugin.getNexo().getItem(id)
            "CRAFTENGINE", "CE" -> HookPlugin.getCraftEngine().getItem(id, session.placeholderPlayer)
            else -> CustomItemSourceEvent(name, id, session).also { it.call() }.source
        }
    }
}