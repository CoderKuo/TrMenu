package trplugins.menu.module.internal.listener

import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import trplugins.menu.api.receptacle.setViewingReceptacle
import trplugins.menu.module.display.Menu
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.data.Metadata

/**
 * @author Arasple
 * @date 2021/1/27 12:14
 */
object ListenerQuit {

    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        val player = e.player
        val session = MenuSession.getSession(player)
        session.shut()
        if (Metadata.isUseLegacy) {
            submit(async = true) {
                Metadata.pushData(player)
            }
        } else {
            // Capture the snapshot before another session can modify this player's data.
            Metadata.pushData(player)
        }
        Menu.menus.forEach {
            it.icons.forEach { icon -> icon.onReset(session) }
        }
        MenuSession.removeSession(player)
        player.setViewingReceptacle(null)
    }

}