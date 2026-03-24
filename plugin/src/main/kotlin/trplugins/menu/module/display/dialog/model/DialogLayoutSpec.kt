package trplugins.menu.module.display.dialog.model

data class DialogLayoutSpec(
    val rowGap: Int = 1,
    val sections: Map<String, DialogSectionSpec> = emptyMap(),
    val widgets: List<DialogWidgetSpec> = emptyList()
)
