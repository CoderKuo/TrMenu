package trplugins.menu.module.display.dialog.model

enum class DialogUnsupportedPolicy {
    FALLBACK_MENU,
    ERROR,
    IGNORE;

    companion object {
        fun from(value: String?): DialogUnsupportedPolicy {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FALLBACK_MENU
        }
    }
}
