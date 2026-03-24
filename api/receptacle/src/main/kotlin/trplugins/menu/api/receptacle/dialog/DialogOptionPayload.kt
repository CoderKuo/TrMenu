package trplugins.menu.api.receptacle.dialog

data class DialogOptionPayload(
    val id: String,
    val title: String,
    val description: String? = null
)
