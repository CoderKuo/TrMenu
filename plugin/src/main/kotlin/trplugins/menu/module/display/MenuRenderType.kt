package trplugins.menu.module.display

enum class MenuRenderType {
    WINDOW,
    DIALOG;

    companion object {
        fun from(value: String?): MenuRenderType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WINDOW
        }
    }
}
