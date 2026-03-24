package trplugins.menu.module.display.dialog.model

import trplugins.menu.api.reaction.Reactions

data class DialogActionSpec(
    val id: String,
    val label: String,
    val width: Int? = null,
    val closesDialog: Boolean = true,
    val nextPage: Int? = null,
    val actions: Reactions,
    val denyActions: Reactions? = null,
)
