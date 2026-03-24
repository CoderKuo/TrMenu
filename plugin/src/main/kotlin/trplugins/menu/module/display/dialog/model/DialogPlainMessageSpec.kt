package trplugins.menu.module.display.dialog.model

data class DialogPlainMessageSpec(
    override val id: String,
    val text: List<String>,
    override val width: Int? = null
) : DialogBodySpec
