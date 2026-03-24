package trplugins.menu.module.display.dialog.runtime

import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.nmsProxy
import trplugins.menu.api.receptacle.dialog.DialogNms
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.display.MenuRenderType

@PlatformSide(Platform.BUKKIT)
object DialogListener {

    @SubscribeEvent
    fun onPacket(e: PacketReceiveEvent) {
        val session = MenuSession.getSession(e.player)
        if (session.renderType != MenuRenderType.DIALOG) {
            return
        }
        val response = runCatching { nmsProxy<DialogNms>().parseResponse(e.packet.source) }.getOrNull() ?: return
        e.isCancelled = true
        DialogMenuRenderer.handleResponse(session, response)
    }
}
