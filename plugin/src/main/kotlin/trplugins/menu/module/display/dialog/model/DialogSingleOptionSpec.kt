package trplugins.menu.module.display.dialog.model

data class DialogSingleOptionSpec(
    override val id: String,
    val label: String,
    val options: List<DialogOptionSpec>,
    val defaultValue: String? = null,
    override val width: Int? = null
) : DialogBodySpec
