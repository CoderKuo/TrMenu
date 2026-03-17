package trplugins.menu.util

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import trplugins.menu.TrMenu
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * @author Arasple
 * @date 2020/3/8 21:19
 */
object Bungees {

    private const val CHANNEL_BUNGEE = "BungeeCord"
    private const val CHANNEL_VELOCITY = "bungeecord:main"

    private val proxyType: ProxyType
        get() = runCatching {
            ProxyType.valueOf(TrMenu.SETTINGS.getString("Options.Proxy", "AUTO")!!.uppercase())
        }.getOrElse { ProxyType.AUTO }

    private val channels: List<String>
        get() = when (proxyType) {
            ProxyType.BUNGEE -> listOf(CHANNEL_BUNGEE)
            ProxyType.VELOCITY -> listOf(CHANNEL_VELOCITY)
            ProxyType.AUTO -> listOf(CHANNEL_BUNGEE, CHANNEL_VELOCITY)
        }

    init {
        val messenger = Bukkit.getMessenger()
        // init 阶段按 AUTO 注册两个 channel，确保后续任何配置都能工作
        listOf(CHANNEL_BUNGEE, CHANNEL_VELOCITY).forEach { channel ->
            if (!messenger.isOutgoingChannelRegistered(TrMenu.plugin, channel)) {
                messenger.registerOutgoingPluginChannel(TrMenu.plugin, channel)
            }
        }
    }

    fun connect(player: Player, server: String) = sendBungeeData(player, "Connect", server)

    fun sendBungeeData(player: Player, vararg args: String) {
        val byteArray = ByteArrayOutputStream()
        val out = DataOutputStream(byteArray)
        for (arg in args) {
            try {
                out.writeUTF(arg)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val data = byteArray.toByteArray()
        // 根据配置的代理类型决定向哪些 channel 发送
        channels.forEach { channel ->
            player.sendPluginMessage(TrMenu.plugin, channel, data)
        }
    }

    enum class ProxyType {
        BUNGEE, VELOCITY, AUTO
    }
}
