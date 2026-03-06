package trplugins.menu.api.action.impl.hook

import com.xiaoyiluck.meoweco.api.MeowEcoAPI
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyPlayer
import trplugins.menu.api.action.ActionHandle
import trplugins.menu.api.action.base.ActionBase
import trplugins.menu.api.action.base.ActionContents

class MecoGive(handle: ActionHandle) : ActionBase(handle) {

    override val regex = "meco-give".toRegex()

    override fun onExecute(contents: ActionContents, player: ProxyPlayer, placeholderPlayer: ProxyPlayer) {
        val args = contents.stringContent().parseContent(placeholderPlayer).trim().split(Regex("\\s+"), 2)
        if (args.size < 2) {
            return
        }
        val currencyId = args[0]
        val amount = args[1].toDoubleOrNull() ?: return
        if (amount <= 0.0) {
            return
        }
        val bukkitPlayer = player.cast<Player>()
        MeowEcoAPI.get()?.deposit(bukkitPlayer.uniqueId, currencyId, amount)
    }
}
