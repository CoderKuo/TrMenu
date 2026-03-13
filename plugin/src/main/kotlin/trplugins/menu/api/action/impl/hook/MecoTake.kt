package trplugins.menu.api.action.impl.hook

import taboolib.common.platform.ProxyPlayer
import trplugins.menu.api.action.ActionHandle
import trplugins.menu.api.action.base.ActionBase
import trplugins.menu.api.action.base.ActionContents
import trplugins.menu.module.internal.hook.HookPlugin

/**
 * @author Arasple
 * @date 2024/3/7
 */
class MecoTake(handle: ActionHandle) : ActionBase(handle) {

    override val regex = "meco-take".toRegex()

    override fun onExecute(contents: ActionContents, player: ProxyPlayer, placeholderPlayer: ProxyPlayer) {
        val args = contents.stringContent().parseContent(placeholderPlayer).trim().split(Regex("\\s+"))
        if (args.size >= 2) {
            val currencyId = args[0]
            val amount = args[1].toDoubleOrNull() ?: 0.0
            HookPlugin.getMeowEco().takeBalance(player.cast(), currencyId, amount)
        }
    }

}
