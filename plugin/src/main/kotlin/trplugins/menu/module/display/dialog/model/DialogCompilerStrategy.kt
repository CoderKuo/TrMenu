package trplugins.menu.module.display.dialog.model

enum class DialogCompilerStrategy {
    AUTO,
    STRICT,
    FALLBACK;

    companion object {
        fun from(value: String?): DialogCompilerStrategy {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AUTO
        }
    }
}
