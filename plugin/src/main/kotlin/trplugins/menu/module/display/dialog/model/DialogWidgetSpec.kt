package trplugins.menu.module.display.dialog.model

data class DialogWidgetSpec(
    val id: String,
    val kind: DialogWidgetKind,
    val anchor: String,
    val row: Int,
    val colStart: Int,
    val colSpan: Int,
    val order: Int = 0,
    val width: Int? = null,
    val text: List<String> = emptyList(),
    val label: String? = null,
    val placeholder: String? = null,
    val display: DialogItemDisplaySpec? = null,
    val options: List<DialogOptionSpec> = emptyList(),
    val initialBoolean: Boolean = false,
    val defaultValue: String? = null,
    val defaultValues: List<String> = emptyList(),
    val min: Double? = null,
    val max: Double? = null,
    val step: Double = 1.0,
    val maxLength: Int? = null,
    val closeOnClick: Boolean = true,
    val nextPage: Int? = null,
    val exitAction: Boolean = false,
    val actions: List<Any> = emptyList(),
    val condition: String = ""
)
