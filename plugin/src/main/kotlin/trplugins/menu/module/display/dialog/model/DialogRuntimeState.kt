package trplugins.menu.module.display.dialog.model

import trplugins.menu.api.receptacle.dialog.DialogPayload

data class DialogRuntimeState(
    val menuId: String,
    val page: Int,
    val pageId: String,
    val payload: DialogPayload,
    val actionMap: Map<String, DialogActionSpec>,
    val inputIds: Set<String>,
    val optionIds: Set<String>,
    val booleanIds: Set<String>,
    val multiOptionGroups: Map<String, Map<String, String>> = emptyMap()
) {
    val values: MutableMap<String, Any?> = mutableMapOf()
}
