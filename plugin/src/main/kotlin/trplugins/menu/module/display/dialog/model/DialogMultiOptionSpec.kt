package trplugins.menu.module.display.dialog.model

data class DialogMultiOptionSpec(
    override val id: String,
    val label: String,
    val options: List<DialogOptionSpec>,
    val defaultValue: List<String> = emptyList(),
    override val width: Int? = null
) : DialogBodySpec
