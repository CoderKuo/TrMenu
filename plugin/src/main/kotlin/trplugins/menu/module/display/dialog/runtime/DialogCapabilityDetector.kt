package trplugins.menu.module.display.dialog.runtime

import org.bukkit.entity.Player
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.nmsProxy
import trplugins.menu.TrMenu
import trplugins.menu.api.receptacle.dialog.DialogNMS
import trplugins.menu.module.display.dialog.mixin.NoopDialogMixinAssist
import trplugins.menu.module.display.dialog.model.DialogMenuSpec

object DialogCapabilityDetector {

    fun isVersionSupported(minVersion: Int): Boolean {
        return MinecraftVersion.majorLegacy >= minVersion
    }

    fun isRuntimeAvailable(): Boolean {
        if (!TrMenu.SETTINGS.getBoolean("Options.Dialogs.Enable", true)) {
            return false
        }
        return runCatching { nmsProxy<DialogNMS>().supportsDialogs() }.getOrDefault(false)
    }

    fun isAvailable(player: Player, spec: DialogMenuSpec): Boolean {
        if (!player.isOnline) {
            return false
        }
        if (!isVersionSupported(spec.minVersion)) {
            return false
        }
        return isRuntimeAvailable() || NoopDialogMixinAssist.enabled()
    }
}
