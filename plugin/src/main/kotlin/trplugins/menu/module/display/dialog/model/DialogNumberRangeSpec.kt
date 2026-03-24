package trplugins.menu.module.display.dialog.model

data class DialogNumberRangeSpec(
    override val id: String,
    val label: String,
    val min: Double,
    val max: Double,
    val step: Double = 1.0,
    val defaultValue: Double? = null,
    override val width: Int? = null
) : DialogBodySpec
