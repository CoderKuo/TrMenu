package trplugins.menu.api.action.impl.menu

import taboolib.common.platform.ProxyPlayer
import taboolib.module.chat.colored
import taboolib.module.chat.component
import trplugins.menu.api.action.ActionHandle
import trplugins.menu.api.action.base.ActionBase
import trplugins.menu.api.action.base.ActionContents
import trplugins.menu.module.display.session
import trplugins.menu.util.colorify

/**
 * TrMenu
 * trplugins.menu.api.action.impl.menu.SetTitle
 *
 * @author Score2
 * @since 2022/02/14 12:21
 */
class SetTitle(handle: ActionHandle) : ActionBase(handle) {
    companion object {
        var useComponent = true
    }

    override val regex = "set-?title".toRegex()

    override fun onExecute(contents: ActionContents, player: ProxyPlayer, placeholderPlayer: ProxyPlayer) {
        val session = player.session()
        val receptacle = session.receptacle ?: return
        val title = contents.stringContent().parseContent(placeholderPlayer).colorify().let {
            if (useComponent) it.colored().component().build().toRawMessage() else it
        }
        receptacle.title(title)
    }

}
