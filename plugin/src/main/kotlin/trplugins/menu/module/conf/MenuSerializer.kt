package trplugins.menu.module.conf

import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.InventoryView
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.warning
import taboolib.common.util.asList
import taboolib.library.configuration.ConfigurationSection
import taboolib.library.xseries.XItemFlag
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import taboolib.module.lang.Language
import taboolib.module.lang.TypeList
import taboolib.module.lang.TypeText
import taboolib.module.nms.ItemTag
import taboolib.module.nms.ItemTagData
import trplugins.menu.TrMenu.SETTINGS
import trplugins.menu.TrMenu.actionHandle
import trplugins.menu.api.menu.ISerializer
import trplugins.menu.api.reaction.Reactions
import trplugins.menu.api.receptacle.MenuTaskData
import trplugins.menu.api.receptacle.MenuTaskSubData
import trplugins.menu.api.receptacle.ReceptacleClickType
import trplugins.menu.api.suffixes
import trplugins.menu.module.conf.prop.SerializeError
import trplugins.menu.module.conf.prop.SerialzeResult
import trplugins.menu.TrMenu
import trplugins.menu.module.display.Menu
import trplugins.menu.module.display.MenuRenderType
import trplugins.menu.module.display.MenuSettings
import trplugins.menu.module.display.dialog.model.DialogActionSpec
import trplugins.menu.module.display.dialog.model.DialogBooleanSpec
import trplugins.menu.module.display.dialog.model.DialogCompilerSpec
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
import trplugins.menu.module.display.dialog.model.DialogScreenType
import trplugins.menu.module.display.dialog.model.DialogSectionSpec
import trplugins.menu.module.display.dialog.model.DialogSingleOptionSpec
import trplugins.menu.module.display.dialog.model.DialogUnsupportedPolicy
import trplugins.menu.module.display.dialog.model.DialogWidgetKind
import trplugins.menu.module.display.dialog.model.DialogWidgetSpec
import trplugins.menu.module.display.icon.Icon
import trplugins.menu.module.display.icon.IconProperty
import trplugins.menu.module.display.icon.Position
import trplugins.menu.module.display.item.Item
import trplugins.menu.module.display.item.Lore
import trplugins.menu.module.display.item.Meta
import trplugins.menu.module.display.layout.Layout
import trplugins.menu.module.display.layout.MenuLayout
import trplugins.menu.module.display.texture.Texture
import trplugins.menu.module.internal.script.js.ScriptFunction
import trplugins.menu.util.bukkit.ItemMatcher
import trplugins.menu.util.collections.CycleList
import trplugins.menu.util.collections.IndivList
import trplugins.menu.util.conf.Property
import trplugins.menu.util.parseIconId
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.forEach
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

/**
 * @author Arasple
 * @date 2021/1/25 10:18
 */
object MenuSerializer : ISerializer {

    /**
     * Ⅰ. 载入菜单
     */
    override fun serializeMenu(file: File): SerialzeResult {
        val id = file.nameWithoutExtension
        val result = SerialzeResult(SerialzeResult.Type.MENU)
        // 文件格式检测
        if (!Type.entries.any { it -> it.suffixes.any { file.extension.equals(it, true) } }) {
            result.state = SerialzeResult.State.IGNORE
            return result
        }
        // 文件有效检测
        if (!(file.isFile && file.length() > 0 && file.canRead())) {
            result.submitError(SerializeError.INVALID_FILE, file.name)
            return result
        }
        // 菜单类型
        val type = Type.entries.find { it -> it.suffixes.any { file.extension.equals(it, true) } }!!
        // 加载菜单配置
        val conf = Configuration.loadFromFile(file, type, concurrent = SETTINGS.getBoolean("Options.Load-Menu-Concurrent", true))

        val langKey = Property.LANG.getKey(conf)
        val languages: Map<String, ConfigurationSection> = when (val section = conf.getConfigurationSection(langKey)) {
            is ConfigurationSection -> {
                val map = mutableMapOf<String, ConfigurationSection>()
                section.getKeys(false).forEach { locale -> section.getConfigurationSection(locale)?.also { map[locale] = it } }
                map
            }
            else -> emptyMap()
        }

        val renderType = MenuRenderType.from(Property.RENDER_TYPE.ofString(conf, MenuRenderType.WINDOW.name))

        // 读取菜单设置
        val settings = serializeSetting(conf, languages)
        if (!settings.succeed()) {
            result.submitErrors(settings).also {
                return result
            }
        }

        val dialog = if (renderType == MenuRenderType.DIALOG) {
            serializeDialog(conf)
        } else null
        if (dialog != null && !dialog.succeed()) {
            result.submitErrors(dialog).also { return result }
        }

        // 读取菜单布局
        val layout = if (renderType == MenuRenderType.DIALOG) {
            serializeDialogPlaceholderLayout((dialog?.result as? DialogMenuSpec)?.pageCount() ?: 1)
        } else {
            serializeLayout(conf)
        }
        if (!layout.succeed()) {
            result.submitErrors(layout).also { return result }
        }
        // 读取菜单图标
        val icons = if (renderType == MenuRenderType.DIALOG) {
            SerialzeResult(SerialzeResult.Type.ICON).also { it.result = emptyList<Icon>() }
        } else {
            serializeIcons(conf, languages, layout.asLayout())
        }
        if (!icons.succeed()) {
            result.submitErrors(icons).also {
                return result
            }
        }

        // 读取菜单语言
        val lang: Map<String, ConcurrentHashMap<String, taboolib.module.lang.Type>>? = if (languages.isEmpty()) null else {
            val map = mutableMapOf<String, ConcurrentHashMap<String, taboolib.module.lang.Type>>()
            languages.forEach { entry ->
                val nodes = serializeLocaleNodes(entry.key, entry.value, ConcurrentHashMap())
                if (nodes.isNotEmpty()) {
                    map[entry.key.lowercase()] = nodes
                }
            }
            map
        }

        // 返回菜单
        Menu(
            id,
            settings.result as MenuSettings,
            layout.result as MenuLayout,
            icons.asIcons(),
            renderType,
            dialog?.result as? DialogMenuSpec,
            conf,
            if (languages.isNotEmpty()) langKey else null,
            lang
        ).also {
            result.result = it
            return result
        }
    }

    /**
     * Ⅱ. 载入菜单设置 MenuSettings
     */
    override fun serializeSetting(conf: Configuration): SerialzeResult {
        return serializeSetting(conf, emptyMap())
    }

    private fun serializeSetting(conf: Configuration, languages: Map<String, ConfigurationSection>): SerialzeResult {
        val result = SerialzeResult(SerialzeResult.Type.MENU_SETTING)
        val options = Property.OPTIONS.ofSection(conf)
        val bindings = Property.BINDINGS.ofSection(conf)
        val events = Property.EVENTS.ofSection(conf)
        val tasks = Property.TASKS.ofSection(conf)
        val funs = Property.FUNCTIONS.ofMap(conf, true)
        val title = Property.TITLE.ofStringList(conf, listOf(pluginId))
        val titleUpdate = Property.TITLE_UPDATE.ofInt(conf, -20)
        val properties = Property.PROPERTIES.ofMap(conf,
            keyTransform = { key ->
                val name = key.replace('-', '_')
                InventoryView.Property.entries.find { it.name.equals(name, ignoreCase = true) }?.id ?: -1
            },
            valueTransform = { value -> value.toString().toIntOrNull() }
        )
        val optionEnableArguments = Property.OPTION_ENABLE_ARGUMENTS.ofBoolean(options, true)
        val optionDefaultArguments = Property.OPTION_DEFAULT_ARGUMENTS.ofStringList(options)
        val optionFreeSlots = Property.OPTION_FREE_SLOTS.ofStringList(conf)
        val optionDefaultLayout = Property.OPTION_DEFAULT_LAYOUT.ofString(options, "0")
        val optionHidePlayerInventory = Property.OPTION_HIDE_PLAYER_INVENTORY.ofBoolean(options, false)
//        val optionHidePurePacket = Property.OPTION_PURE_PACKET.ofBoolean(options, true)
        val optionMinClickDelay = Property.OPTION_MIN_CLICK_DELAY.ofInt(options, 200)
        val optionDependExpansions = Property.OPTION_DEPEND_EXPANSIONS.ofStringList(options)
        val boundCommands = Property.BINDING_COMMANDS.ofStringList(bindings)
        val boundItems = Property.BINDING_ITEMS.ofStringList(bindings)
        val eventOpen = Property.EVENT_OPEN.ofList(events)
        val eventClose = Property.EVENT_CLOSE.ofList(events)
        val eventClick = Property.EVENT_CLICK.ofList(events)
        val commandFakeOp = Property.COMMAND_FAKE_OP.ofBoolean(options, true)

        val settings = MenuSettings(
            CycleList(title),
            titleUpdate,
            properties,
            optionEnableArguments,
            optionDefaultArguments.toTypedArray(),
            optionFreeSlots.flatMap { Position.Slot.readStaticSlots(it) }.toSet(),
            optionDefaultLayout.toIntOrNull() ?: optionDefaultLayout,
            optionDependExpansions.toTypedArray(),
            optionMinClickDelay,
            optionHidePlayerInventory,
            boundCommands.map { it.toRegex() },
            boundItems.map { ItemMatcher.of(it) }.toTypedArray(),
            Reactions.ofReaction(actionHandle, eventOpen),
            Reactions.ofReaction(actionHandle, eventClose),
            Reactions.ofReaction(actionHandle, eventClick),
            mutableListOf<MenuTaskData>().apply {
                tasks?.getKeys(false)?.forEach { subKey ->
                    val period = tasks.getLong("$subKey.period", -1)
                    add(
                        MenuTaskData(subKey, period, mutableListOf<MenuTaskSubData>().apply {
                            tasks.getMapList("$subKey.task").forEach z@{ action ->
                                val type = action["condition"]?.toString() ?: return@z
                                val reaction = action["actions"]?.asList() ?: return@z
                                add(MenuTaskSubData(type, reaction))
                            }
                        })
                    )
                }
            },
            funs.map { ScriptFunction(it.key, it.value.toString()) }.toSet(),
            commandFakeOp
        )

        // i18n
        languages.forEach { entry ->
            val titleI18n = Property.TITLE.of(entry.value)
            if (titleI18n != null) {
                settings.addI18nTitle(entry.key, CycleList(Property.asList(titleI18n)))
            }
        }

        result.result = settings
        return result
    }

    private fun serializeDialogPlaceholderLayout(pageCount: Int): SerialzeResult {
        val result = SerialzeResult(SerialzeResult.Type.MENU_LAYOUT)
        val safePages = pageCount.coerceAtLeast(1)
        val layouts = Array(safePages) {
            Layout(1, InventoryType.CHEST, emptyList(), emptyList())
        }
        result.result = MenuLayout(layouts)
        return result
    }

    /**
     * Ⅲ. 载入布局功能 Layout
     */
    override fun serializeLayout(conf: Configuration): SerialzeResult {
        val result = SerialzeResult(SerialzeResult.Type.MENU_LAYOUT)
        val layouts = mutableListOf<Layout>()
        val layout = Property.LAYOUT.ofLists(conf)
        val playerInventory = Property.LAYOUT_PLAYER_INVENTORY.ofLists(conf)
        val inventoryType = Property.INVENTORY_TYPE.ofString(conf, "CHEST")
        val bukkitType = InventoryType.entries.find { it.name.equals(inventoryType, true) } ?: InventoryType.CHEST
        val rows = Property.SIZE.ofInt(conf, 0).let {
            if (it > 6) return@let it / 9
            else it
        }

        for (index in 0 until max(layout.size, playerInventory.size)) {
            val lay = arrayOf(layout.getOrElse(index) { listOf() }, playerInventory.getOrElse(index) { listOf() })
            layouts += Layout(rows, bukkitType, lay[0], lay[1])
        }
        if (layouts.isEmpty()) {
            val lay = arrayOf(listOf<String>(), listOf())
            layouts += Layout(rows, bukkitType, lay[0], lay[1])
        }

        result.result = MenuLayout(layouts.toTypedArray())
        return result
    }

    /**
     * Ⅳ. 载入图标功能 Icons
     */
    override fun serializeIcons(conf: Configuration, layout: MenuLayout): SerialzeResult {
        return serializeIcons(conf, emptyMap(), layout)
    }

    fun serializeIcons(conf: Configuration, languages: Map<String, ConfigurationSection>, layout: MenuLayout): SerialzeResult {
        val result = SerialzeResult(SerialzeResult.Type.ICON)

        // i18n
        val iconsI18n: Map<String, ConfigurationSection>? = if (languages.isEmpty()) null else {
            val map = mutableMapOf<String, ConfigurationSection>()
            languages.forEach { entry ->
                Property.ICONS.ofSection(entry.value)?.also { map[entry.key] = it }
            }
            if (map.isEmpty()) null else map
        }

        val icons = Property.ICONS.ofMap(conf).map { (id, value) ->
            val section = Property.asSection(value).let { it ->
                if (it !is Configuration) return@let null
                return@let Configuration.loadFromString(it.saveToString().split("\n").joinToString("\n") {
//                    VariableReader("@", "@")
                    it.parseIconId(id)
                }, conf.type, concurrent = SETTINGS.getBoolean("Options.Load-Menu-Concurrent", true))
            }

            // i18n
            val sectionI18n: Map<String, ConfigurationSection>? = if (iconsI18n == null) null else {
                val map = mutableMapOf<String, ConfigurationSection>()
                iconsI18n.forEach { entry ->
                    entry.value.getConfigurationSection(id)?.also { map[entry.key] = it }
                }
                if (map.isEmpty()) null else map
            }

            val refresh = Property.ICON_REFRESH.ofInt(section, -1)
            val update = Property.ICON_UPDATE.ofIntList(section)
            val display = Property.ICON_DISPLAY.ofSection(section)
            val action = Property.ACTIONS.ofSection(section, "all")
            val defIcon = loadIconProperty(sectionI18n, null, section, display, action, -1)
            val slots = Property.ICON_DISPLAY_SLOT.ofLists(display)
            var pages = Property.ICON_DISPLAY_PAGE.ofIntList(display)
            var order = 0
            val search = layout.search(id, pages)

            val position =
                if (slots.isNotEmpty()) {
                    val slot = CycleList(slots.map { Position.Slot.from(it) })
                    if (pages.isEmpty()) pages = pages.plus(0)
                    Position(pages.associateWith { slot })
                } else Position(search.mapValues { CycleList(Position.Slot.from(it.value)) })

            var index = 0
            val subs = Property.ICON_SUB_ICONS.ofList(section).map {
                // i18n
                val subSectionI18n: Map<String, ConfigurationSection>? = if (sectionI18n == null) null else {
                    val map = mutableMapOf<String, ConfigurationSection>()
                    sectionI18n.forEach { entry ->
                        entry.value.getConfigurationSection(index++.toString())?.also { map[entry.key] = it }
                    }
                    if (map.isEmpty()) null else map
                }

                val sub = Property.asSection(it)
                val subDisplay = Property.ICON_DISPLAY.ofSection(sub)
                val subAction = Property.ACTIONS.ofSection(sub, "all")
                loadIconProperty(subSectionI18n, defIcon, sub, subDisplay, subAction, order++)
            }.sortedBy { it.priority }

            if (defIcon.display.texture.isEmpty() || subs.any { it.display.texture.isEmpty() }) {
                result.submitError(SerializeError.INVALID_ICON_UNDEFINED_TEXTURE, id)
            }

            Icon(id, refresh.toLong(), update.toTypedArray(), position, defIcon, IndivList(subs))
        }

        result.result = icons

        return result
    }

    /**
     * Func Ⅴ. 载入图标显示部分
     */
    private val loadIconProperty: (Map<String, ConfigurationSection>?, IconProperty?, Configuration?, Configuration?, Configuration?, Int) -> IconProperty =
        { sectionI18n, def, it, display, action, order ->
            // Inheritance
            val inherit = if (def != null) Property.INHERIT.ofIconPropertyList(it) else listOf()
            val append = if (def != null) Property.APPEND.ofIconPropertyList(it) else listOf()

            // Item
            val name = Property.ICON_DISPLAY_NAME.ofStringList(display)
            val texture = Property.ICON_DISPLAY_MATERIAL.ofStringList(display)
            val lore = Property.ICON_DISPLAY_LORE.ofLists(display)

            // Meta
            val amount = if (inherit.contains(Property.ICON_DISPLAY_AMOUNT)) def!!.display.meta.amount else Property.ICON_DISPLAY_AMOUNT.ofString(display, "1")
            val shiny = if (inherit.contains(Property.ICON_DISPLAY_SHINY)) def!!.display.meta.shiny else Property.ICON_DISPLAY_SHINY.ofString(display, "false")
            val flags = if (inherit.contains(Property.ICON_DISPLAY_FLAGS)) {
                def!!.display.meta.flags
            } else Property.ICON_DISPLAY_FLAGS.ofStringList(display).mapNotNull { flag ->
//                ItemFlag.entries.find { it.name.equals(flag, true) }
                XItemFlag.of(flag).getOrNull()?.get()
            }.toTypedArray()
            val nbt = if (inherit.contains(Property.ICON_DISPLAY_NBT)) {
                def!!.display.meta.nbt
            } else {
                ItemTag().also { Property.ICON_DISPLAY_NBT.ofMap(display).forEach { (key, value) -> it[key] = ItemTagData.toNBT(value) } }
            }
            val tooltipStyle = if (inherit.contains(Property.ICON_DISPLAY_TOOLTIP)) {
                def!!.display.meta.tooltip
            } else Property.ICON_DISPLAY_TOOLTIP.ofString(display, "")
            val itemModel = if (inherit.contains(Property.ICON_DISPLAY_ITEM_MODEL)) {
                def!!.display.meta.itemModel
            } else Property.ICON_DISPLAY_ITEM_MODEL.ofString(display, "")
            val hideTooltip = if (inherit.contains(Property.ICON_DISPLAY_HIDE_TOOLTIP)) {
                def!!.display.meta.hideTooltip
            } else Property.ICON_DISPLAY_HIDE_TOOLTIP.ofString(display, "false")
            val unbreakable = if (inherit.contains(Property.ICON_DISPLAY_UNBREAKABLE)) {
                def!!.display.meta.unbreakable
            } else Property.ICON_DISPLAY_UNBREAKABLE.ofString(display, "false")
            val data = if (inherit.contains(Property.ICON_DISPLAY_DATA)) {
                def!!.display.meta.data
            } else Property.ICON_DISPLAY_DATA.ofString(display, "")

            // only for the subIcon
            val priority = Property.PRIORITY.ofInt(it, order)
            val condition = Property.CONDITION.ofString(it, "")

            // Actions
            val clickActions = mutableMapOf<Set<ReceptacleClickType>, Reactions>()
            if (def != null && inherit.contains(Property.ACTIONS)) {
                clickActions.putAll(def.action)
            }
            action?.getValues(false)?.forEach { (type, reaction) ->
                val clickTypes = ReceptacleClickType.matches(type)
                if (clickTypes.isNotEmpty()) {
                    val reactions = Reactions.ofReaction(actionHandle, reaction)
                    if (!reactions.isEmpty()) {
                        clickActions[clickTypes].also { clickActions[clickTypes] = it?.copyAndThen(reactions) ?: reactions }
                    }
                }
            }
            if (def != null && !inherit.contains(Property.ACTIONS) && append.contains(Property.ACTIONS)) {
                def.action.forEach { (clickTypes, reaction) ->
                    clickActions[clickTypes].also { if (it == null) clickActions[clickTypes] = reaction else it.andThen(reaction) }
                }
            }

            val item = Item(
                // 图标材质
                if (def != null && texture.isEmpty()) def.display.texture
                else CycleList(texture.map { Texture.createTexture(it) }),
                // 图标显示名称
                if (def != null && inherit.contains(Property.ICON_DISPLAY_NAME) && name.isEmpty()) def.display.name
                else CycleList(name),
                // 图标显示描述
                if (def != null && inherit.contains(Property.ICON_DISPLAY_LORE) && lore.isEmpty()) def.display.lore
                else CycleList(lore.map { Lore(line(it)) }),
                // 图标附加属性
                Meta(amount, shiny, flags, nbt, tooltipStyle, itemModel, hideTooltip, unbreakable, data)
            )

            // i18n
            if (def != null && inherit.contains(Property.ICON_DISPLAY_NAME) && name.isEmpty()) {
                item.nameI18n.putAll(def.display.nameI18n)
            }
            if (def != null && inherit.contains(Property.ICON_DISPLAY_LORE) && lore.isEmpty()) {
                item.loreI18n.putAll(def.display.loreI18n)
            }
            sectionI18n?.forEach { (locale, conf) ->
                val nameI18n = Property.ICON_DISPLAY_NAME.ofStringList(conf)
                if (nameI18n.isNotEmpty()) {
                    item.addI18nName(locale, CycleList(nameI18n))
                }
                val loreI18n = Property.ICON_DISPLAY_LORE.ofLists(conf)
                if (loreI18n.isNotEmpty()) {
                    item.addI18nLore(locale, CycleList(loreI18n.map { Lore(line(it)) }))
                }
            }

            IconProperty(priority, condition, item, clickActions)
        }

    private fun serializeDialog(conf: Configuration): SerialzeResult {
        val result = SerialzeResult(SerialzeResult.Type.MENU)
        val dialogSection = Property.DIALOG.ofSection(conf)
        if (dialogSection == null) {
            result.submitError(RuntimeException("Dialog section is required when Render-Type is DIALOG."))
            result.state = SerialzeResult.State.FAILED
            return result
        }
        val compilerSection = Property.DIALOG_COMPILER.ofSection(dialogSection)
        val compiler = DialogCompilerSpec(
            strategy = DialogCompilerStrategy.from(Property.DIALOG_COMPILER_STRATEGY.ofString(compilerSection, "AUTO")),
            unsupportedPolicy = DialogUnsupportedPolicy.from(Property.DIALOG_COMPILER_UNSUPPORTED_POLICY.ofString(compilerSection, "FALLBACK_MENU")),
            gridColumns = Property.DIALOG_COMPILER_GRID_COLUMNS.ofInt(compilerSection, 12),
            contentMaxWidth = Property.DIALOG_COMPILER_CONTENT_MAX_WIDTH.ofInt(compilerSection, 360),
            mixinAssist = Property.DIALOG_COMPILER_MIXIN_ASSIST.ofBoolean(compilerSection, false)
        )
        val pages = dialogSection.getMapList(Property.DIALOG_PAGES.default).mapIndexed { index, pageMap ->
            serializeDialogPage(index, Property.asSection(pageMap) ?: Configuration.empty())
        }
        if (pages.isEmpty()) {
            result.submitError(RuntimeException("Dialog pages are required when Render-Type is DIALOG."))
            result.state = SerialzeResult.State.FAILED
            return result
        }
        result.result = DialogMenuSpec(
            minVersion = parseDialogVersion(Property.DIALOG_MIN_VERSION.ofString(dialogSection, "1.21.6")),
            fallbackMenu = Property.DIALOG_FALLBACK_MENU.ofString(dialogSection),
            allowEscClose = Property.DIALOG_ALLOW_ESC_CLOSE.ofBoolean(dialogSection, true),
            externalTitle = Property.DIALOG_EXTERNAL_TITLE.ofString(dialogSection),
            compiler = compiler,
            pages = pages
        )
        return result
    }

    private fun serializeDialogPage(index: Int, section: Configuration): DialogPageSpec {
        val layout = serializeDialogLayout(section)
        val pageActions = if (layout != null) emptyList() else section.getMapList(Property.DIALOG_PAGE_ACTIONS.default).mapIndexed { actionIndex, actionMap ->
            serializeDialogAction(actionIndex, Property.asSection(actionMap) ?: Configuration.empty())
        }
        val explicitExitAction = section["Exit-Action"]?.let {
            serializeDialogAction(-1, Property.asSection(it) ?: Configuration.empty()).copy(exitAction = true)
        }
        val (exitAction, actions) = if (explicitExitAction != null) {
            explicitExitAction to pageActions.filterNot { it.exitAction }
        } else {
            val marked = pageActions.firstOrNull { it.exitAction }
            marked to pageActions.filterNot { it.exitAction }
        }
        val body = if (layout != null) emptyList() else section.getMapList(Property.DIALOG_PAGE_BODY.default).mapIndexedNotNull { bodyIndex, bodyMap ->
            serializeDialogBody(bodyIndex, Property.asSection(bodyMap) ?: Configuration.empty())
        }
        return DialogPageSpec(
            id = Property.DIALOG_PAGE_ID.ofString(section, "page_$index"),
            type = DialogScreenType.from(Property.DIALOG_PAGE_TYPE.ofString(section, "NOTICE")),
            title = Property.DIALOG_PAGE_TITLE.ofString(section),
            body = body,
            actions = actions,
            exitAction = exitAction,
            layoutSpec = layout,
            onClose = Reactions.ofReaction(TrMenu.actionHandle, section["On-Close"])
        )
    }

    private fun serializeDialogBody(index: Int, section: Configuration): trplugins.menu.module.display.dialog.model.DialogBodySpec? {
        val renderer = section.getString("Renderer")?.lowercase() ?: section.getString("Type")?.lowercase() ?: return null
        val id = section.getString("Id") ?: "body_$index"
        val width = section.getInt("Width").takeIf { it > 0 }
        return when (renderer) {
            "plain_message", "text", "message" -> DialogPlainMessageSpec(id, section.getStringList("Text"), width)
            "item" -> DialogItemSpec(id, serializeDialogItemDisplay(Property.asSection(section["Display"]) ?: Configuration.empty()), width)
            "input" -> DialogInputSpec(
                id = id,
                label = section.getString("Label") ?: id,
                placeholder = section.getString("Placeholder"),
                defaultValue = section.getString("Default-Value"),
                maxLength = section.getInt("Max-Length").takeIf { it > 0 },
                width = width
            )
            "boolean", "toggle" -> DialogBooleanSpec(
                id = id,
                label = section.getString("Label") ?: id,
                initial = section.getBoolean("Initial", false),
                width = width
            )
            "single_option", "single-option", "select" -> DialogSingleOptionSpec(
                id = id,
                label = section.getString("Label") ?: id,
                options = serializeDialogOptions(section),
                defaultValue = section.getString("Default-Value"),
                width = width
            )
            "multi_option", "multi-option" -> DialogMultiOptionSpec(
                id = id,
                label = section.getString("Label") ?: id,
                options = serializeDialogOptions(section),
                defaultValue = section.getStringList("Default-Value"),
                width = width
            )
            "number_range", "number-range", "range" -> DialogNumberRangeSpec(
                id = id,
                label = section.getString("Label") ?: id,
                min = section.getDouble("Min"),
                max = section.getDouble("Max"),
                step = section.getDouble("Step").takeIf { it > 0 } ?: 1.0,
                defaultValue = section.getString("Default-Value")?.toDoubleOrNull(),
                width = width
            )
            else -> null
        }
    }

    private fun serializeDialogAction(index: Int, section: Configuration): DialogActionSpec {
        return DialogActionSpec(
            id = section.getString("Id") ?: "action_$index",
            label = section.getString("Label") ?: "Action-$index",
            width = section.getInt("Width").takeIf { it > 0 },
            closesDialog = section.getBoolean("Close-On-Click", true),
            nextPage = section.getString("Next-Page")?.toIntOrNull() ?: section.getInt("Next-Page").takeIf { section.contains("Next-Page") },
            exitAction = section.getBoolean("Exit", false),
            actions = Reactions.ofReaction(TrMenu.actionHandle, section["Execute"] ?: section["Actions"]),
            denyActions = section["Deny"]?.let { Reactions.ofReaction(TrMenu.actionHandle, it) }
        )
    }

    private fun serializeDialogLayout(section: Configuration): DialogLayoutSpec? {
        val widgetsSection = Property.DIALOG_WIDGETS.ofSection(section) ?: return null
        val layoutSection = Property.DIALOG_LAYOUT.ofSection(section)
        val sectionsSection = Property.DIALOG_LAYOUT_SECTIONS.ofSection(layoutSection)
        val sections = sectionsSection
            ?.getKeys(false)
            ?.associateWith { key ->
                val sub = sectionsSection.getConfigurationSection(key)
                DialogSectionSpec(key, sub?.getInt("row", 0) ?: 0)
            }
            ?: emptyMap()
        val widgets = widgetsSection.getKeys(false).map { key ->
            val widget = widgetsSection.getConfigurationSection(key) ?: Configuration.empty()
            DialogWidgetSpec(
                id = key,
                kind = DialogWidgetKind.from(Property.DIALOG_WIDGET_KIND.ofString(widget, "TEXT")),
                anchor = Property.DIALOG_WIDGET_ANCHOR.ofString(widget, "content"),
                row = Property.DIALOG_WIDGET_ROW.ofInt(widget, 1),
                colStart = Property.DIALOG_WIDGET_COL_START.ofInt(widget, 1),
                colSpan = Property.DIALOG_WIDGET_COL_SPAN.ofInt(widget, 12),
                order = Property.DIALOG_WIDGET_ORDER.ofInt(widget, 0),
                width = widget.getInt("width").takeIf { it > 0 },
                text = widget.getStringList("text"),
                label = widget.getString("label"),
                placeholder = widget.getString("placeholder"),
                display = widget["display"]?.let { serializeDialogItemDisplay(Property.asSection(it) ?: Configuration.empty()) },
                options = serializeDialogOptions(widget),
                initialBoolean = widget.getBoolean("initial", false),
                defaultValue = widget.getString("default-value"),
                defaultValues = widget.getStringList("default-values").ifEmpty { widget.getStringList("default-value") },
                min = widget.getString("min")?.toDoubleOrNull(),
                max = widget.getString("max")?.toDoubleOrNull(),
                step = widget.getString("step")?.toDoubleOrNull() ?: 1.0,
                maxLength = widget.getInt("max-length").takeIf { it > 0 },
                closeOnClick = widget.getBoolean("close-on-click", true),
                nextPage = widget.getString("next-page")?.toIntOrNull(),
                exitAction = widget.getBoolean("exit", false) || widget.getBoolean("exit-action", false),
                actions = Property.asList(widget["execute"] ?: widget["actions"]),
                condition = widget.getString("condition") ?: ""
            )
        }
        return DialogLayoutSpec(
            rowGap = Property.DIALOG_LAYOUT_ROW_GAP.ofInt(layoutSection, 1),
            sections = sections,
            widgets = widgets
        )
    }

    private fun serializeDialogOptions(section: ConfigurationSection): List<DialogOptionSpec> {
        return section.getMapList("Options").mapIndexed { index, optionMap ->
            val option = Property.asSection(optionMap) ?: Configuration.empty()
            DialogOptionSpec(
                id = option.getString("Id") ?: "option_$index",
                title = option.getString("Title") ?: option.getString("Name") ?: "Option-$index",
                description = option.getString("Description")
            )
        }
    }

    private fun serializeDialogItemDisplay(section: Configuration): DialogItemDisplaySpec {
        return DialogItemDisplaySpec(
            material = section.getString("material") ?: section.getString("Material") ?: "STONE",
            name = section.getString("name") ?: section.getString("Name"),
            lore = section.getStringList("lore").ifEmpty { section.getStringList("Lore") },
            amount = (section.getString("amount") ?: section.getString("Amount"))?.toIntOrNull() ?: 1
        )
    }

    private fun parseDialogVersion(version: String?): Int {
        val value = version?.trim().orEmpty()
        if (value.matches(Regex("\\d+"))) {
            return value.toInt()
        }
        val split = value.split('.')
        val major = split.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = split.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = split.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }

    val line: (List<String>) -> List<String> =
        { origin -> mutableListOf<String>().also { list -> origin.forEach { list.addAll(it.split("\n")) } } }

    // Method body taken from Taboolib, licensed under the MIT License
    //
    // Copyright (c) 2018 Bkm016
    private fun serializeLocaleNodes(code: String, file: ConfigurationSection, nodes: ConcurrentHashMap<String, taboolib.module.lang.Type>, root: String = ""): ConcurrentHashMap<String, taboolib.module.lang.Type> {
        file.getKeys(false).forEach { node ->
            val key = "$root$node"
            when (val obj = file[node]) {
                // 标准文本
                is String -> {
                    nodes[key] = TypeText(obj)
                }
                // 列表
                is List<*> -> {
                    val list = obj.mapNotNull { sub ->
                        if (sub is Map<*, *>) {
                            serializeLocaleNode(code, sub.map { it.key.toString() to it.value!! }.toMap(), key)
                        } else {
                            TypeText(sub.toString())
                        }
                    }
                    // Only load as TypeList a list with more than 1 element
                    if (list.size == 1) {
                        nodes[key] = list[0]
                    } else {
                        nodes[key] = TypeList(list)
                    }
                }
                // 嵌套
                is ConfigurationSection -> {
                    // Only load sections with specified type
                    // Not detecting "type" since is a word that can be used on any locale section
                    if (obj.contains("==")) {
                        val type = serializeLocaleNode(code, obj.getValues(false).map { it.key to it.value!! }.toMap(), key)
                        if (type != null) {
                            nodes[key] = type
                        }
                    } else {
                        serializeLocaleNodes(code, obj, nodes, "$key.")
                    }
                }
                // 其他
                else -> warning("Unsupported language node: $key ($code)")
            }
        }
        return nodes
    }

    // Method body taken from Taboolib, licensed under the MIT License
    //
    // Copyright (c) 2018 Bkm016
    private fun serializeLocaleNode(code: String, map: Map<String, Any>, node: String?): taboolib.module.lang.Type? {
        return if (map.containsKey("type") || map.containsKey("==")) {
            val type = (map["type"] ?: map["=="]).toString().lowercase()
            val typeInstance = Language.languageType[type]?.getDeclaredConstructor()?.newInstance()
            if (typeInstance != null) {
                typeInstance.init(map)
            } else {
                warning("Unsupported language type: $node > $type ($code)")
            }
            typeInstance
        } else {
            warning("Missing language type: $map ($code)")
            null
        }
    }
}