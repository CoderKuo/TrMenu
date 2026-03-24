package trplugins.menu.module.display.dialog.model

data class DialogItemDisplaySpec(
    val material: String,
    val name: String? = null,
    val lore: List<String> = emptyList(),
    val amount: Int = 1
)
