package trplugins.menu.api.receptacle.dialog

import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion

class DialogNmsImpl : DialogNms() {

    private val requiredClasses = listOf(
        "net.minecraft.network.protocol.Packet",
        "net.minecraft.server.level.ServerPlayer"
    )

    override fun supportsDialogs(): Boolean {
        if (MinecraftVersion.majorLegacy < 12106) {
            return false
        }
        if (!requiredClasses.all { className -> runCatching { Class.forName(className) }.isSuccess }) {
            return false
        }
        // 当前阶段仅落骨架，不在这里伪装成“已支持”。
        // 真正的 1.21.6+ dialogs 发包/回包映射实现到位后，再切换为 true。
        return false
    }

    override fun open(player: Player, payload: DialogPayload) {
        throw UnsupportedOperationException("Dialogs NMS bridge is not available for the current runtime.")
    }

    override fun close(player: Player) {
        // Dialogs 暂无通用 close packet 映射时，保持空实现，由上层负责清理会话。
    }

    override fun parseResponse(packet: Any): DialogResponseData? {
        return null
    }
}
