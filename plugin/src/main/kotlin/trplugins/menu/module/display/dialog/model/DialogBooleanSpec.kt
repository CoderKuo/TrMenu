package trplugins.menu.module.display.dialog.model

data class DialogBooleanSpec(
    override val id: String,
    val label: String,
    val initial: Boolean = false,
    override val width: Int? = null
) : DialogBodySpec
