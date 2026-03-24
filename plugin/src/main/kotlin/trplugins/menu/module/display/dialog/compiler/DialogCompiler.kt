package trplugins.menu.module.display.dialog.compiler

import org.bukkit.inventory.ItemStack
import taboolib.platform.util.buildItem
import trplugins.menu.TrMenu
import trplugins.menu.api.reaction.Reactions
import trplugins.menu.api.receptacle.dialog.DialogActionPayload
import trplugins.menu.api.receptacle.dialog.DialogElementPayload
import trplugins.menu.api.receptacle.dialog.DialogOptionPayload
import trplugins.menu.api.receptacle.dialog.DialogPayload
import trplugins.menu.module.display.Menu
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.display.dialog.model.DialogActionSpec
import trplugins.menu.module.display.dialog.model.DialogBodySpec
import trplugins.menu.module.display.dialog.model.DialogBooleanSpec
import trplugins.menu.module.display.dialog.model.DialogCompilerStrategy
import trplugins.menu.module.display.dialog.model.DialogInputSpec
import trplugins.menu.module.display.dialog.model.DialogItemDisplaySpec
import trplugins.menu.module.display.dialog.model.DialogItemSpec
import trplugins.menu.module.display.dialog.model.DialogLayoutSpec
import trplugins.menu.module.display.dialog.model.DialogMenuSpec
import trplugins.menu.module.display.dialog.model.DialogMultiOptionSpec
import trplugins.menu.module.display.dialog.model.DialogNumberRangeSpec
import trplugins.menu.module.display.dialog.model.DialogOptionSpec
import trplugins.menu.module.display.dialog.model.DialogPageSpec
import trplugins.menu.module.display.dialog.model.DialogPlainMessageSpec
import trplugins.menu.module.display.dialog.model.DialogRuntimeState
import trplugins.menu.module.display.dialog.model.DialogSingleOptionSpec
import trplugins.menu.module.display.dialog.model.DialogWidgetKind
import trplugins.menu.module.display.texture.Texture

object DialogCompiler {

    fun compile(menu: Menu, session: MenuSession, spec: DialogMenuSpec, pageIndex: Int): DialogCompileResult {
        val page = spec.page(pageIndex) ?: throw DialogCompileException("Dialog page $pageIndex does not exist in menu ${menu.id}")
        val resolvedPage = if (page.layoutSpec != null) {
            compileLayout(page, spec, session)
        } else {
            page
        }

        val body = mutableListOf<DialogElementPayload>()
        val inputIds = mutableSetOf<String>()
        val optionIds = mutableSetOf<String>()
        val booleanIds = mutableSetOf<String>()
        val multiOptionGroups = mutableMapOf<String, Map<String, String>>()
        resolvedPage.body.forEach { bodySpec ->
            when (bodySpec) {
                is DialogInputSpec -> {
                    body += compileBody(session, bodySpec)
                    inputIds += bodySpec.id
                }
                is DialogBooleanSpec -> {
                    body += compileBody(session, bodySpec)
                    booleanIds += bodySpec.id
                }
                is DialogSingleOptionSpec -> {
                    body += compileBody(session, bodySpec)
                    optionIds += bodySpec.id
                }
                is DialogNumberRangeSpec -> {
                    body += compileBody(session, bodySpec)
                    optionIds += bodySpec.id
                }
                is DialogMultiOptionSpec -> {
                    val expansion = compileMultiOption(session, bodySpec)
                    body += expansion.elements
                    booleanIds += expansion.syntheticIds.keys
                    optionIds += bodySpec.id
                    multiOptionGroups[bodySpec.id] = expansion.syntheticIds
                }
                else -> body += compileBody(session, bodySpec)
            }
        }

        val actions = resolvedPage.actions.map { compileAction(session, it) }
        val exitAction = resolvedPage.exitAction?.let { compileAction(session, it) }
        val actionMap = buildMap<String, DialogActionSpec> {
            resolvedPage.actions.forEach { put(it.id, it) }
            resolvedPage.exitAction?.also { put(it.id, it) }
        }
        val payload = DialogPayload(
            menuId = menu.id,
            pageId = resolvedPage.id,
            pageIndex = pageIndex,
            screenType = resolvedPage.type.name.lowercase(),
            title = resolvedPage.title?.let(session::parse),
            externalTitle = spec.externalTitle?.let(session::parse),
            allowEscClose = spec.allowEscClose,
            body = body,
            actions = actions,
            exitAction = exitAction
        )
        val state = DialogRuntimeState(
            menuId = menu.id,
            page = pageIndex,
            pageId = resolvedPage.id,
            payload = payload,
            actionMap = actionMap,
            inputIds = inputIds,
            optionIds = optionIds,
            booleanIds = booleanIds,
            multiOptionGroups = multiOptionGroups
        )
        return DialogCompileResult(payload, state)
    }

    private fun compileLayout(page: DialogPageSpec, spec: DialogMenuSpec, session: MenuSession): DialogPageSpec {
        val layoutSpec = page.layoutSpec ?: return page
        val nodes = layoutSpec.widgets
            .filter { widget -> widget.condition.isBlank() || session.parse(widget.condition).equals("true", true) }
            .map { widget ->
                validateWidget(widget, spec.compiler, layoutSpec)
                DialogWidgetNode(widget.id, widget.anchor, widget.row, widget.colStart, widget.colSpan, widget.order, widget)
            }
            .sortedWith(compareBy<DialogWidgetNode>({ it.anchor }, { it.row }, { it.colStart }, { it.order }, { it.id }))

        val body = mutableListOf<DialogBodySpec>()
        val actions = mutableListOf<DialogActionSpec>()
        var exitAction = page.exitAction
        nodes.forEach { node ->
            val width = node.widget.width ?: mapWidth(node.colSpan, spec.compiler)
            when (node.widget.kind) {
                DialogWidgetKind.TEXT -> body += DialogPlainMessageSpec(node.id, node.widget.text, width)
                DialogWidgetKind.ITEM -> body += DialogItemSpec(
                    node.id,
                    node.widget.display ?: throw DialogCompileException("Dialog widget ${node.id} missing display section."),
                    width
                )
                DialogWidgetKind.INPUT -> body += DialogInputSpec(
                    node.id,
                    node.widget.label ?: node.id,
                    node.widget.placeholder,
                    node.widget.defaultValue,
                    node.widget.maxLength,
                    width
                )
                DialogWidgetKind.BOOLEAN -> body += DialogBooleanSpec(
                    node.id,
                    node.widget.label ?: node.id,
                    node.widget.initialBoolean,
                    width
                )
                DialogWidgetKind.SINGLE_OPTION -> body += DialogSingleOptionSpec(
                    node.id,
                    node.widget.label ?: node.id,
                    node.widget.options,
                    node.widget.defaultValue,
                    width
                )
                DialogWidgetKind.MULTI_OPTION -> body += DialogMultiOptionSpec(
                    node.id,
                    node.widget.label ?: node.id,
                    node.widget.options,
                    node.widget.defaultValues,
                    width
                )
                DialogWidgetKind.NUMBER_RANGE -> body += DialogNumberRangeSpec(
                    node.id,
                    node.widget.label ?: node.id,
                    node.widget.min ?: 0.0,
                    node.widget.max ?: 0.0,
                    node.widget.step,
                    node.widget.defaultValue?.toDoubleOrNull(),
                    width
                )
                DialogWidgetKind.ACTION -> {
                    val action = DialogActionSpec(
                        node.id,
                        node.widget.label ?: node.id,
                        width,
                        node.widget.closeOnClick,
                        node.widget.nextPage,
                        node.widget.exitAction,
                        Reactions.ofReaction(TrMenu.actionHandle, node.widget.actions)
                    )
                    if (action.exitAction) {
                        exitAction = action
                    } else {
                        actions += action
                    }
                }
            }
        }
        return page.copy(body = body, actions = actions, exitAction = exitAction)
    }

    private fun validateWidget(widget: trplugins.menu.module.display.dialog.model.DialogWidgetSpec, compiler: trplugins.menu.module.display.dialog.model.DialogCompilerSpec, layoutSpec: DialogLayoutSpec) {
        if (widget.colStart < 1) {
            throw DialogCompileException("Dialog widget ${widget.id} has invalid col-start ${widget.colStart}")
        }
        if (widget.colSpan <= 0) {
            throw DialogCompileException("Dialog widget ${widget.id} has invalid col-span ${widget.colSpan}")
        }
        if (widget.colStart + widget.colSpan - 1 > compiler.gridColumns) {
            when (compiler.strategy) {
                DialogCompilerStrategy.STRICT -> throw DialogCompileException("Dialog widget ${widget.id} exceeds grid columns ${compiler.gridColumns}")
                else -> return
            }
        }
        if (layoutSpec.sections.isNotEmpty() && !layoutSpec.sections.containsKey(widget.anchor)) {
            throw DialogCompileException("Dialog widget ${widget.id} references unknown anchor ${widget.anchor}")
        }
    }

    private fun mapWidth(colSpan: Int, compiler: trplugins.menu.module.display.dialog.model.DialogCompilerSpec): Int {
        return ((colSpan.toDouble() / compiler.gridColumns.toDouble()) * compiler.contentMaxWidth.toDouble()).toInt().coerceAtLeast(80)
    }

    private fun compileAction(session: MenuSession, spec: DialogActionSpec): DialogActionPayload {
        return DialogActionPayload(
            id = spec.id,
            label = session.parse(spec.label),
            width = spec.width,
            closesDialog = spec.closesDialog,
            nextPage = spec.nextPage,
            exitAction = spec.exitAction
        )
    }

    private fun compileMultiOption(session: MenuSession, spec: DialogMultiOptionSpec): MultiOptionExpansion {
        val syntheticIds = linkedMapOf<String, String>()
        val elements = spec.options.mapIndexed { index, option ->
            val syntheticId = "${spec.id}__${index}"
            syntheticIds[syntheticId] = option.id
            DialogElementPayload(
                id = syntheticId,
                type = "boolean",
                label = session.parse(option.title),
                width = spec.width,
                boolValue = spec.defaultValue.contains(option.id)
            )
        }
        return MultiOptionExpansion(elements, syntheticIds)
    }

    private fun compileBody(session: MenuSession, spec: DialogBodySpec): DialogElementPayload {
        return when (spec) {
            is DialogPlainMessageSpec -> DialogElementPayload(
                id = spec.id,
                type = "plain_message",
                text = spec.text.map(session::parse),
                width = spec.width
            )
            is DialogItemSpec -> DialogElementPayload(
                id = spec.id,
                type = "item",
                width = spec.width,
                item = buildItem(session, spec.display)
            )
            is DialogInputSpec -> DialogElementPayload(
                id = spec.id,
                type = "input",
                label = session.parse(spec.label),
                width = spec.width,
                placeholder = spec.placeholder?.let(session::parse),
                value = spec.defaultValue?.let(session::parse),
                maxLength = spec.maxLength
            )
            is DialogBooleanSpec -> DialogElementPayload(
                id = spec.id,
                type = "boolean",
                label = session.parse(spec.label),
                width = spec.width,
                boolValue = spec.initial
            )
            is DialogSingleOptionSpec -> DialogElementPayload(
                id = spec.id,
                type = "single_option",
                label = session.parse(spec.label),
                width = spec.width,
                value = spec.defaultValue?.let(session::parse),
                options = compileOptions(session, spec.options)
            )
            is DialogMultiOptionSpec -> DialogElementPayload(
                id = spec.id,
                type = "multi_option",
                label = session.parse(spec.label),
                width = spec.width,
                value = spec.defaultValue.joinToString(",") { session.parse(it) },
                options = compileOptions(session, spec.options)
            )
            is DialogNumberRangeSpec -> DialogElementPayload(
                id = spec.id,
                type = "number_range",
                label = session.parse(spec.label),
                width = spec.width,
                value = spec.defaultValue?.toString(),
                min = spec.min,
                max = spec.max,
                step = spec.step
            )
        }
    }

    private fun compileOptions(session: MenuSession, options: List<DialogOptionSpec>): List<DialogOptionPayload> {
        return options.map { option ->
            DialogOptionPayload(
                id = option.id,
                title = session.parse(option.title),
                description = option.description?.let(session::parse)
            )
        }
    }

    private fun buildItem(session: MenuSession, display: DialogItemDisplaySpec): ItemStack {
        val base = Texture.createTexture(display.material).generate(session)
        return buildItem(base) {
            display.name?.let { name = session.parse(it) }
            if (display.lore.isNotEmpty()) {
                lore.clear()
                lore.addAll(display.lore.map(session::parse))
            }
            if (display.amount > 0) {
                amount = display.amount
            }
        }
    }

    private data class MultiOptionExpansion(
        val elements: List<DialogElementPayload>,
        val syntheticIds: Map<String, String>
    )
}
