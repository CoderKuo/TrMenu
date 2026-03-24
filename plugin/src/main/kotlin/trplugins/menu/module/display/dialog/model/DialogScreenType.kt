package trplugins.menu.module.display.dialog.model

enum class DialogScreenType {
    NOTICE,
    CONFIRMATION,
    MULTI_ACTION;

    companion object {
        fun from(value: String?): DialogScreenType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NOTICE
        }
    }
}
