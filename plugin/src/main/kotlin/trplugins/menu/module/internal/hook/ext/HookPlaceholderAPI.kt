package trplugins.menu.module.internal.hook.ext

import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.pluginId
import taboolib.platform.compat.PlaceholderExpansion
import taboolib.platform.util.sendLang
import trplugins.menu.TrMenu
import trplugins.menu.api.event.MenuOpenEvent
import trplugins.menu.module.display.Menu
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.script.jexl.JexlAgent
import trplugins.menu.module.internal.script.js.JavaScriptAgent
import trplugins.menu.util.ignoreCase

/**
 * @author Arasple
 * @date 2021/2/4 11:37
 */
object HookPlaceholderAPI : PlaceholderExpansion {

    private val enabledParseJavaScript get() = TrMenu.SETTINGS.getBoolean("Options.Placeholders.JavaScript-Parse", false)
    private val enabledParseJexlScript get() = TrMenu.SETTINGS.getBoolean("Options.Placeholders.Jexl-Parse", false)

    override val identifier = pluginId

    override fun onPlaceholderRequest(player: Player?, args: String): String {
        player ?: return "___"

        val session = MenuSession.getSession(player)
        val params = args.split("_")
        val key = params.getOrElse(1) { "" }
        val value = { params.slice(1 until params.size).joinToString("_") }

        return kotlin.runCatching { when (params[0].lowercase()) {
            "menus" -> Menu.menus.size
            "args" -> session.arguments[key.toIntOrNull() ?: 0]
            "meta" -> Metadata.getMeta(player)[key]
            "dialog" -> Metadata.getMeta(player)["dialog_${value()}"]
            "data" -> Metadata.getData(player)[key]
            "globaldata" -> runCatching { Metadata.globalData[value()].toString() }.getOrElse { "null" }
            "node" -> session.getNodeValue(value())?.toString() ?: "null"
            "menu" -> menu(session, params)
            "locale" -> session.locale
            "js" -> if (enabledParseJavaScript) if (params.size > 1) JavaScriptAgent.eval(session, params[1]).asString() else "" else "UNABLE_PARSE"
            "jexl" -> if (enabledParseJexlScript) if (params.size > 1) JexlAgent.eval(session, params[1]).asString() else "" else "UNABLE_PARSE"
            else -> ""
        } }.getOrNull().toString()

    }

    private fun menu(session: MenuSession, params: List<String>): String {
        val menu = session.menu
        return when (params[1]) {
            "page" -> session.page.toString()
            "pages" -> menu?.dialogSpec?.pageCount()?.toString() ?: menu?.layout?.layouts?.size.toString()
            "next" -> (session.page + 1).toString()
            "prev" -> (session.page - 1).toString()
            "title" -> menu?.settings?.title(session)?.get(session.id).toString()
            else -> ""
        }
    }

    @SubscribeEvent
    fun onMenuOpen(e: MenuOpenEvent) {
        val menu = e.menu
        val viewer = e.session.viewer
        val expansions = menu.settings.dependExpansions

        Metadata.getMeta(viewer)["open_event_reason"] = e.reason.name

        if (expansions.isNotEmpty()) {
            e.isCancelled = true
            viewer.sendLang("Menu-Expansions-Header", expansions.size)
            expansions.forEach { viewer.sendLang("Menu-Expansions-Format", it) }
        }
    }

}
