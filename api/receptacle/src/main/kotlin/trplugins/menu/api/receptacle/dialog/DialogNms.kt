package trplugins.menu.api.receptacle.dialog

import org.bukkit.entity.Player

abstract class DialogNms {

    abstract fun supportsDialogs(): Boolean

    abstract fun open(player: Player, payload: DialogPayload)

    abstract fun close(player: Player)

    abstract fun parseResponse(packet: Any): DialogResponseData?
}
