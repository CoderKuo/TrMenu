package trplugins.menu.module.display.dialog.model

data class DialogMenuSpec(
    val minVersion: Int = 12106,
    val fallbackMenu: String? = null,
    val allowEscClose: Boolean = true,
    val externalTitle: String? = null,
    val compiler: DialogCompilerSpec = DialogCompilerSpec(),
    val pages: List<DialogPageSpec> = emptyList()
) {

    fun page(index: Int): DialogPageSpec? {
        return pages.getOrNull(index)
    }

    fun pageCount(): Int {
        return pages.size
    }
}
