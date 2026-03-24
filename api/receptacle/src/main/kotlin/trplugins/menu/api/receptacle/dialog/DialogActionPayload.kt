package trplugins.menu.api.receptacle.dialog

data class DialogActionPayload(
    val id: String,
    val label: String,
    val width: Int? = null,
    val closesDialog: Boolean = true,
    val nextPage: Int? = null,
    val exitAction: Boolean = false
)
