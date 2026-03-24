package trplugins.menu.module.display.dialog.model

data class DialogItemSpec(
    override val id: String,
    val display: DialogItemDisplaySpec,
    override val width: Int? = null
) : DialogBodySpec
