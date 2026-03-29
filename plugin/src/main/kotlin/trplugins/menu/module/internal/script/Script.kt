package trplugins.menu.module.internal.script

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.adaptPlayer
import trplugins.menu.TrMenu.actionHandle
import trplugins.menu.api.TrMenuAPI
import trplugins.menu.api.action.base.ActionEntry
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.script.jexl.JexlAgent
import trplugins.menu.module.internal.script.js.JavaScriptAgent
import trplugins.menu.module.internal.script.nova.NovaScriptAgent
import trplugins.menu.util.EvalResult

/**
 * TrMenu
 * trmenu.module.internal.script.Script
 *
 * @author Score2
 * @since 2022/01/09 17:48
 */

fun EvalResult.asItemStack(): ItemStack? {
    return any as ItemStack?
}

// in Condition.Companion
fun MenuSession.evalScript(script: String?): EvalResult {
    return placeholderPlayer.evalScript(script)
}

// in Condition
fun String.evalScript(session: MenuSession): EvalResult {
    return if (isEmpty()) EvalResult.TRUE
    else session.placeholderPlayer.evalScript(this)
}

fun ProxyPlayer.evalScript(script: String?) =
    cast<Player>().evalScript(script)

fun Player.evalScript(script: String?): EvalResult {
    script ?: return EvalResult(null)
    val (isJavaScript, js) = JavaScriptAgent.serialize(script)
    if (isJavaScript) {
        return JavaScriptAgent.eval(MenuSession.getSession(this), js!!)
    }
    val (isJexlScript,jexl) = JexlAgent.serialize(script)
    if (isJexlScript) {
        return JexlAgent.eval(MenuSession.getSession(this), jexl!!)
    }
    val (isNovaScript, nova) = NovaScriptAgent.serialize(script)
    if (isNovaScript) {
        return NovaScriptAgent.eval(MenuSession.getSession(this), nova!!)
    }
    return TrMenuAPI.instantKether(this, script)
}

/*

对于 Action 来说,不关心返回值,所以 Action 和 Condition 就不应该用一个，因为 runAction 没有返回值
 */
fun Player.evalAction(script: String?) {
    script ?: return
    actionHandle.runAction(adaptPlayer(this), ActionEntry.of(actionHandle,script))
}