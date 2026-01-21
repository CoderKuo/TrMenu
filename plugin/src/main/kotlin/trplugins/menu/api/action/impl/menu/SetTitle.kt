package trplugins.menu.api.action.impl.menu

import taboolib.common.platform.ProxyPlayer
import taboolib.module.chat.component
import trplugins.menu.api.action.ActionHandle
import trplugins.menu.api.action.base.ActionBase
import trplugins.menu.api.action.base.ActionContents
import trplugins.menu.module.display.session
import trplugins.menu.util.colorify
import trplugins.menu.util.parseJson

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
        var title = contents.stringContent().parseContent(placeholderPlayer)
        title = if (useComponent && runCatching { title.parseJson() }.isSuccess) {
            title
        } else {
            title.colorify()
        }
        if (useComponent) {
            title = title.component().build().toRawMessage()
        }
        receptacle.title(title)
    }

}
