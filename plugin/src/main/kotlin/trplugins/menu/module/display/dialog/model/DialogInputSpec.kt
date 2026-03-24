package trplugins.menu.module.display.dialog.model

data class DialogInputSpec(
    override val id: String,
    val label: String,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    val maxLength: Int? = null,
    override val width: Int? = null
) : DialogBodySpec
