package trplugins.menu.api.action.impl.script

import taboolib.common.platform.ProxyPlayer
import trplugins.menu.api.action.ActionHandle
import trplugins.menu.api.action.base.ActionBase
import trplugins.menu.api.action.base.ActionContents
import trplugins.menu.module.display.session
import trplugins.menu.module.internal.script.nova.NovaScriptAgent

class NovaScript(handle: ActionHandle) : ActionBase(handle) {

    override val regex = "nova(script)?".toRegex()

    override fun readContents(contents: Any): ActionContents {
        NovaScriptAgent.preCompile(contents.toString())
        return super.readContents(contents)
    }

    override fun onExecute(contents: ActionContents, player: ProxyPlayer, placeholderPlayer: ProxyPlayer) {
        NovaScriptAgent.eval(player.session(), contents.stringContent())
    }
}
