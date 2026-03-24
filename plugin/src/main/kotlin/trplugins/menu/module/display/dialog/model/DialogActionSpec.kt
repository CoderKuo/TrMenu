package trplugins.menu.module.display.dialog.model

import trplugins.menu.api.reaction.Reactions

data class DialogActionSpec(
    val id: String,
    val label: String,
    val width: Int? = null,
    val nextPage: Int? = null,
    val exitAction: Boolean = false,
    val actions: Reactions,
    val denyActions: Reactions? = null,
)
