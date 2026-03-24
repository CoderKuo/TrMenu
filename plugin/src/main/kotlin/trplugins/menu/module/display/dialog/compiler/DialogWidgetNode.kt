package trplugins.menu.module.display.dialog.compiler

import trplugins.menu.module.display.dialog.model.DialogWidgetSpec

data class DialogWidgetNode(
    val id: String,
    val anchor: String,
    val row: Int,
    val colStart: Int,
    val colSpan: Int,
    val order: Int,
    val widget: DialogWidgetSpec
)
