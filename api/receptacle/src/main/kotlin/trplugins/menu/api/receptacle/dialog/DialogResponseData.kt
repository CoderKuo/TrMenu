package trplugins.menu.api.receptacle.dialog

data class DialogResponseData(
    val actionId: String?,
    val closeAction: Boolean = false,
    val values: Map<String, Any?> = emptyMap()
)
