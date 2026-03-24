package trplugins.menu.module.display.dialog.compiler

import trplugins.menu.api.receptacle.dialog.DialogPayload
import trplugins.menu.module.display.dialog.model.DialogRuntimeState

data class DialogCompileResult(
    val payload: DialogPayload,
    val state: DialogRuntimeState
)
