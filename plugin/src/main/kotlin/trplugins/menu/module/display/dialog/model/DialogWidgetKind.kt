package trplugins.menu.module.display.dialog.model

enum class DialogWidgetKind {
    ITEM,
    TEXT,
    INPUT,
    BOOLEAN,
    SINGLE_OPTION,
    MULTI_OPTION,
    NUMBER_RANGE,
    ACTION;

    companion object {
        fun from(value: String?): DialogWidgetKind {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TEXT
        }
    }
}
