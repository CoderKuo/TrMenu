package trplugins.menu.module.display.dialog.model

data class DialogCompilerSpec(
    val strategy: DialogCompilerStrategy = DialogCompilerStrategy.AUTO,
    val unsupportedPolicy: DialogUnsupportedPolicy = DialogUnsupportedPolicy.FALLBACK_MENU,
    val gridColumns: Int = 12,
    val contentMaxWidth: Int = 360,
    val mixinAssist: Boolean = false
)
