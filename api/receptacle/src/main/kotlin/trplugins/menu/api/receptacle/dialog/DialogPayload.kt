package trplugins.menu.api.receptacle.dialog

data class DialogPayload(
    val menuId: String,
    val pageId: String,
    val pageIndex: Int,
    val screenType: String,
    val title: String?,
    val externalTitle: String?,
    val allowEscClose: Boolean,
    val body: List<DialogElementPayload>,
    val actions: List<DialogActionPayload>
)
