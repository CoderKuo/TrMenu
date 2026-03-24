package trplugins.menu.module.display.dialog.runtime

import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.warning
import taboolib.module.nms.nmsProxy
import trplugins.menu.api.receptacle.dialog.DialogNms
import trplugins.menu.api.receptacle.dialog.DialogResponseData
import trplugins.menu.module.display.Menu
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.display.dialog.compiler.DialogCompiler
import trplugins.menu.module.display.dialog.compiler.DialogCompileException

object DialogMenuRenderer {

    fun open(session: MenuSession, page: Int) {
        val menu = session.menu ?: return
        val spec = menu.dialogSpec ?: return
        if (!DialogCapabilityDetector.isAvailable(session.viewer, spec)) {
            fallback(menu, session, "Dialogs runtime is unavailable or version unsupported.")
            return
        }
        val compiled = runCatching { DialogCompiler.compile(menu, session, spec, page) }.getOrElse {
            fallback(menu, session, it.message ?: "Dialog compile failed")
            return
        }
        session.renderType = menu.renderType
        session.page = page
        session.dialogState = compiled.state
        DialogPlaceholderBridge.clear(session.viewer)
        DialogPlaceholderBridge.writeOpenState(session, compiled.state)
        runCatching {
            nmsProxy<DialogNms>().open(session.viewer, compiled.payload)
        }.onFailure {
            fallback(menu, session, it.message ?: "Dialog open failed")
        }
    }

    fun page(session: MenuSession, page: Int, title: String? = null) {
        val menu = session.menu ?: return
        val spec = menu.dialogSpec ?: return
        val targetPage = if (page in 0 until spec.pageCount()) page else return
        if (title != null) {
            session.dialogState?.values?.set("page_title_override", title)
        }
        open(session, targetPage)
    }

    fun refresh(session: MenuSession) {
        val currentPage = session.dialogState?.page ?: session.page
        open(session, currentPage)
    }

    fun close(session: MenuSession, sendPacket: Boolean = true) {
        if (sendPacket) {
            runCatching { nmsProxy<DialogNms>().close(session.viewer) }
        }
        session.dialogState?.let { state ->
            session.menu?.dialogSpec?.page(state.page)?.onClose?.eval(adaptPlayer(session.viewer))
        }
        DialogPlaceholderBridge.clear(session.viewer)
        session.dialogState = null
        session.renderType = null
    }

    fun handleResponse(session: MenuSession, response: DialogResponseData) {
        val state = session.dialogState ?: return
        val actionId = response.actionId
        DialogPlaceholderBridge.writeResponse(session, state, response)
        if (response.closeAction) {
            close(session)
            return
        }
        val action = actionId?.let { state.actionMap[it] } ?: return
        val success = action.actions.eval(adaptPlayer(session.viewer))
        if (!success) {
            action.denyActions?.eval(adaptPlayer(session.viewer))
            return
        }
        if (action.nextPage != null) {
            page(session, action.nextPage)
            return
        }
        if (action.closesDialog) {
            close(session)
        }
    }

    private fun fallback(menu: Menu, session: MenuSession, reason: String) {
        warning("[Dialog] ${menu.id}: $reason")
        DialogPlaceholderBridge.clear(session.viewer)
        session.dialogState = null
        session.renderType = null
        val fallbackMenu = menu.dialogSpec?.fallbackMenu
        if (!fallbackMenu.isNullOrBlank()) {
            trplugins.menu.api.TrMenuAPI.getMenuById(fallbackMenu)?.open(session.viewer, reason = trplugins.menu.api.event.MenuOpenEvent.Reason.UNKNOWN) {
                it.arguments = session.arguments
                it.agent = session.agent
            }
        }
    }
}
