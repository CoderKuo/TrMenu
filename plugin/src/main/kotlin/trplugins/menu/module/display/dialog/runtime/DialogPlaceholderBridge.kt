package trplugins.menu.module.display.dialog.runtime

import org.bukkit.entity.Player
import trplugins.menu.api.receptacle.dialog.DialogResponseData
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.display.dialog.model.DialogRuntimeState
import trplugins.menu.module.internal.data.Metadata

object DialogPlaceholderBridge {

    private const val PREFIX = "dialog_"

    fun clear(player: Player) {
        val meta = Metadata.getMeta(player).data
        meta.keys.filter { it.startsWith(PREFIX) }.toList().forEach(meta::remove)
    }

    fun writeOpenState(session: MenuSession, state: DialogRuntimeState) {
        val meta = Metadata.getMeta(session.viewer)
        meta["dialog_menu"] = state.menuId
        meta["dialog_page"] = state.page.toString()
        meta["dialog_page_id"] = state.pageId
        meta["dialog_render_type"] = "DIALOG"
    }

    fun writeResponse(session: MenuSession, state: DialogRuntimeState, response: DialogResponseData) {
        val meta = Metadata.getMeta(session.viewer)
        response.actionId?.also { meta["dialog_action"] = it }
        response.values.forEach { (key, value) ->
            val stringValue = value?.toString() ?: ""
            state.values[key] = value
            meta["dialog_$key"] = stringValue
            when {
                state.inputIds.contains(key) -> meta["dialog_input_$key"] = stringValue
                state.optionIds.contains(key) -> meta["dialog_option_$key"] = stringValue
                state.booleanIds.contains(key) -> meta["dialog_boolean_$key"] = stringValue
            }
        }
    }
}
