package trplugins.menu.module.display.dialog.model

import trplugins.menu.api.reaction.Reactions

data class DialogPageSpec(
    val id: String,
    val type: DialogScreenType,
    val title: String? = null,
    val body: List<DialogBodySpec> = emptyList(),
    val actions: List<DialogActionSpec> = emptyList(),
    val exitAction: DialogActionSpec? = null,
    val layoutSpec: DialogLayoutSpec? = null,
    val onClose: Reactions? = null,
)
