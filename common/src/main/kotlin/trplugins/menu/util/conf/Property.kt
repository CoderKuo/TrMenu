package trplugins.menu.util.conf

import taboolib.library.configuration.ConfigurationSection
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.module.configuration.ConfigSection
import taboolib.module.configuration.Configuration

/**
 * @author Arasple
 * @date 2020/6/27 21:18
 */
@Suppress("UNCHECKED_CAST")
enum class Property(val default: String, val regex: Regex) {

    /**
     * 菜单标题
     */
    TITLE("Title", "(title|name)s?"),

    /**
     * 动态标题更新周期
     */
    TITLE_UPDATE("Title-Update", "titles?-?updates?"),

    /**
     * 菜单布局
     */
    LAYOUT("Layout", "(layout|shape)s?"),

    /**
     * 菜单布局 - 玩家容器
     */
    LAYOUT_PLAYER_INVENTORY("PlayerInventory", "(layout|shape)?-?player-?inv(entory)?s?"),

    /**
     * 容器类型
     */
    INVENTORY_TYPE("Type", "(inv(entory)?)?-?types?"),

    /**
     * 容器大小
     */
    SIZE("Size", "(size|row)s?"),

    /**
     * 菜单类型属性
     */
    PROPERTIES("Properties", "propert(y|ies?)"),

    /**
     * 菜单选项设置
     */
    OPTIONS("Options", "(option|setting)s?"),

    /**
     * 菜单渲染类型
     */
    RENDER_TYPE("Render-Type", "render-?types?"),

    /**
     * Dialogs 根节点
     */
    DIALOG("Dialog", "dialogs?"),

    /**
     * Dialogs 最低版本
     */
    DIALOG_MIN_VERSION("Min-Version", "min-?versions?"),

    /**
     * Dialogs 回退菜单
     */
    DIALOG_FALLBACK_MENU("Fallback-Menu", "fallback-?menus?"),

    /**
     * Dialogs 是否允许 ESC 关闭
     */
    DIALOG_ALLOW_ESC_CLOSE("Allow-Esc-Close", "allow-?esc-?closes?"),

    /**
     * Dialogs 外部标题
     */
    DIALOG_EXTERNAL_TITLE("External-Title", "external-?titles?"),

    /**
     * Dialogs 页列表
     */
    DIALOG_PAGES("Pages", "pages?"),

    /**
     * 菜单选项 - 是否启用传递参数
     */
    OPTION_ENABLE_ARGUMENTS("Arguments", "(transfer)?-?arg(ument)?s?"),

    /**
     * 菜单选项 - 默认参数
     */
    OPTION_DEFAULT_ARGUMENTS("Default-Arguments", "def(ault)?-?arg(ument)?s?"),

    /**
     * 菜单选项 - 解锁槽位
     */
    OPTION_FREE_SLOTS("Free-Slots", "free-?slots?"),

    /**
     * 菜单选项 - 默认布局
     */
    OPTION_DEFAULT_LAYOUT("Default-Layout", "def(ault)?-?lay(out)?s?"),

    /**
     * 菜单选项 - 隐藏玩家容器
     */
    OPTION_HIDE_PLAYER_INVENTORY("Hide-Player-Inventory", "hide-?player-?inv(entory)?s?"),

    /**
     * 菜单选项 - 使用纯发包菜单
     */
    OPTION_PURE_PACKET("Pure-Packet", "pure-?pack(et)?s?"),

    /**
     * 菜单选项 - 防频繁点击
     */
    OPTION_MIN_CLICK_DELAY("Min-Click-Delay", "min-?click-?delay"),

    /**
     * 菜单选项 - 依赖的 PlaceholderAPI 拓展
     */
    OPTION_DEPEND_EXPANSIONS("Depend-Expansions", "depend-?expansions?"),

    /**
     * 菜单绑定
     */
    BINDINGS("Bindings", "(bind(ing)?|bound)s?"),

    /**
     * 菜单绑定 - 命令
     */
    BINDING_COMMANDS("Commands", "(command|cmd)s?"),

    /**
     * 菜单绑定 - 物品
     */
    BINDING_ITEMS("Items", "items?"),

    /**
     * 菜单事件
     */
    EVENTS("Events", "events?"),

    /**
     * 菜单事件 - 开启菜单
     */
    EVENT_OPEN("Open", "opens?"),

    /**
     * 菜单事件 - 关闭菜单
     */
    EVENT_CLOSE("Close", "closes?"),

    /**
     * 菜单事件 - 点击菜单
     */
    EVENT_CLICK("Click", "clicks?"),

    /**
     * 需求条件
     */
    CONDITION("condition", "(require(ment)?|cond(ition)?)s?"),

    /**
     * 优先级
     */
    PRIORITY("priority", "pri(ority)?s?"),

    /**
     * 继承（默认图标）
     */
    INHERIT("inherit", "inherits?"),

    /**
     * 附加（默认图标）
     */
    APPEND("append", "appends?"),

    /**
     * 周期
     */
    PERIOD("period", "(period|time)s?"),

    /**
     * 执行动作集合
     */
    ACTIONS("actions", "(list|action|click|execute|cmd)s?"),

    /**
     * (拒绝) 反馈动作
     */
    DENY_ACTIONS("deny-actions", "deny(-)?(list|action|click|execute|cmd)?s?"),

    /**
     * Dialogs 页 ID
     */
    DIALOG_PAGE_ID("Id", "ids?"),

    /**
     * Dialogs 页类型
     */
    DIALOG_PAGE_TYPE("Type", "types?"),

    /**
     * Dialogs 页标题
     */
    DIALOG_PAGE_TITLE("Title", "titles?"),

    /**
     * Dialogs 页正文
     */
    DIALOG_PAGE_BODY("Body", "bod(y|ies)"),

    /**
     * Dialogs 页动作
     */
    DIALOG_PAGE_ACTIONS("Actions", "actions?"),

    /**
     * Dialogs 编译器
     */
    DIALOG_COMPILER("Compiler", "compilers?"),

    /**
     * Dialogs 编译策略
     */
    DIALOG_COMPILER_STRATEGY("Strategy", "strateg(y|ies)"),

    /**
     * Dialogs 不支持策略
     */
    DIALOG_COMPILER_UNSUPPORTED_POLICY("Unsupported-Policy", "unsupported-?polic(y|ies)"),

    /**
     * Dialogs 网格列数
     */
    DIALOG_COMPILER_GRID_COLUMNS("Grid-Columns", "grid-?columns?"),

    /**
     * Dialogs 内容最大宽度
     */
    DIALOG_COMPILER_CONTENT_MAX_WIDTH("Content-Max-Width", "content-?max-?width"),

    /**
     * Dialogs Mixin Assist
     */
    DIALOG_COMPILER_MIXIN_ASSIST("Mixin-Assist", "mixin-?assist"),

    /**
     * Dialogs 布局
     */
    DIALOG_LAYOUT("Layout", "layouts?"),

    /**
     * Dialogs 布局行距
     */
    DIALOG_LAYOUT_ROW_GAP("Row-Gap", "row-?gaps?"),

    /**
     * Dialogs 布局分区
     */
    DIALOG_LAYOUT_SECTIONS("Sections", "sections?"),

    /**
     * Dialogs Widgets
     */
    DIALOG_WIDGETS("Widgets", "widgets?"),

    /**
     * Dialogs Widget 种类
     */
    DIALOG_WIDGET_KIND("kind", "kinds?"),

    /**
     * Dialogs Widget 锚点
     */
    DIALOG_WIDGET_ANCHOR("anchor", "anchors?"),

    /**
     * Dialogs Widget 行号
     */
    DIALOG_WIDGET_ROW("row", "rows?"),

    /**
     * Dialogs Widget 起始列
     */
    DIALOG_WIDGET_COL_START("col-start", "col-?starts?"),

    /**
     * Dialogs Widget 跨列
     */
    DIALOG_WIDGET_COL_SPAN("col-span", "col-?spans?"),

    /**
     * Dialogs Widget 顺序
     */
    DIALOG_WIDGET_ORDER("order", "orders?"),

    /**
     * 菜单图标
     */
    ICONS("Icons", "(icon|button)s?"),

    /**
     * 菜单图标 - 刷新
     */
    ICON_REFRESH("refresh", "refreshs?"),

    /**
     * 菜单图标 - 更新
     */
    ICON_UPDATE("update", "updates?"),

    /**
     * 菜单图标 - 显示
     */
    ICON_DISPLAY("display", "displays?"),

    /**
     * 菜单图标显示 - 页码
     */
    ICON_DISPLAY_PAGE("page", "pages?"),

    /**
     * 菜单图标显示 - 槽位
     */
    ICON_DISPLAY_SLOT("slot", "(slot|pos(ition)?)s?"),

    /**
     * 菜单图标显示 - 物品名称
     */
    ICON_DISPLAY_NAME("name", "(display)?-?names?"),

    /**
     * 菜单图标显示 - 物品材质
     */
    ICON_DISPLAY_MATERIAL("material", "(mat(erial)?|texture)s?"),

    /**
     * 菜单图标显示 - 物品描述
     */
    ICON_DISPLAY_LORE("lore", "(display)?-?lores?"),

    /**
     * 菜单图标显示 - 物品损伤值
     */
    ICON_DISPLAY_DATA("data", "(display)?-?data"),

    /**
     * 菜单图标显示 - 物品数量
     */
    ICON_DISPLAY_AMOUNT("amount", "(amt|amount)s?"),

    /**
     * 菜单图标显示 - 附魔
     */
    ICON_DISPLAY_ENCHANT("enchant", "enchant(ment)?s?"),

    /**
     * 菜单图标显示 - 发光
     */
    ICON_DISPLAY_SHINY("shiny", "(shiny|glow)s?"),

    /**
     * 菜单图标显示 - 标签
     */
    ICON_DISPLAY_FLAGS("flags", "flags?"),

    /**
     * 菜单图标显示 - NBT
     */
    ICON_DISPLAY_NBT("nbt", "nbts?"),

    /**
     * 菜单图标 - Tooltip Style
     */
    ICON_DISPLAY_TOOLTIP("tooltip", "tooltip(_style)?"),

    /**
     * 菜单图标 - ItemModel
     */
    ICON_DISPLAY_ITEM_MODEL("model", "(item)?_?model"),

    /**
     * 菜单图标 - ItemModel
     */
    ICON_DISPLAY_HIDE_TOOLTIP("hide_tooltip", "hide_?tool(tip)?"),

    /**
     * 菜单图标 - Unbreakable
     */
    ICON_DISPLAY_UNBREAKABLE("unbreakable", "unbreak(able)?"),

    /**
     * 菜单图标 - 子图标
     */
    ICON_SUB_ICONS("icons", "(sub|priority)?icons?"),

    /**
     * 菜单内置任务
     */
    TASKS("Tasks", "(task|schedule)s?"),

    /**
     * 菜单内置脚本
     */
    FUNCTIONS("Functions", "(fun(ction)?|script)s?"),

    /**
     * 菜单内置国际化
     */
    LANG("Lang", "lang(uage)?|internationalization|i18n"),

    /**
     * 菜单内虚拟OP命令
     */
    COMMAND_FAKE_OP("Command-Fake-Op", "command-?fake-?op"),

    /**
     * Crafting - root section
     */
    CRAFTING("Crafting", "craft(ing)?"),

    /**
     * Crafting - input slots
     */
    CRAFTING_INPUT_SLOTS("Input-Slots", "input-?slots?"),

    /**
     * Crafting - result slot
     */
    CRAFTING_RESULT_SLOT("Result-Slot", "result-?slots?"),

    /**
     * Crafting - recipes list
     */
    CRAFTING_RECIPES("Recipes", "recipes?"),

    /**
     * Crafting recipe - shapeless flag
     */
    CRAFTING_SHAPELESS("Shapeless", "shapeless"),

    /**
     * Crafting recipe - shape
     */
    CRAFTING_SHAPE("Shape", "shapes?"),

    /**
     * Crafting recipe - ingredients
     */
    CRAFTING_INGREDIENTS("Ingredients", "ingredients?"),

    /**
     * Crafting recipe - result
     */
    CRAFTING_RESULT("Result", "results?"),

    /**
     * Crafting recipe - result material
     */
    CRAFTING_RESULT_MATERIAL("Material", "(mat(erial)?|texture)s?"),

    /**
     * Crafting recipe - result amount
     */
    CRAFTING_RESULT_AMOUNT("Amount", "(amt|amount)s?");

    constructor(default: String, regex: String) : this(default, Regex("(?i)$regex"))

    override fun toString(): String = default

    fun ofString(conf: ConfigurationSection?, def: String? = null): String {
        return of(conf, def).toString()
    }

    fun ofBoolean(conf: ConfigurationSection?, def: Boolean = false): Boolean {
        return ofString(conf, def.toString()).toBoolean()
    }

    fun ofInt(conf: ConfigurationSection?, def: Int = -1): Int {
        return ofString(conf).toIntOrNull() ?: def
    }

    fun ofList(conf: ConfigurationSection?): List<Any> {
        return asAnyList(of(conf))
    }

    fun ofIntList(conf: ConfigurationSection?, def: List<Int> = listOf()): List<Int> {
        return asIntList(of(conf, def))
    }

    fun ofStringList(conf: ConfigurationSection?, def: List<String> = listOf()): List<String> {
        return asList(of(conf, def))
    }

    fun ofIconPropertyList(conf: ConfigurationSection?, def: List<Property> = listOf()): List<Property> {
        val value = of(conf, def)
        if (value !is List<*> && value.toString().equals("true", true)) {
            return listOf(ICON_DISPLAY_NAME, ICON_DISPLAY_LORE)
        }
        return when (value) {
            is List<*> -> value.mapNotNull { property ->
                Property.entries.find {
                    it.name.equals(property.toString(), true) || it.name.equals("ICON_$property", true)
                }
            }

            else -> listOfNotNull(Property.entries.find {
                it.name.equals(value.toString(), true) || it.name.equals("ICON_$value", true)
            })
        }
    }

    fun ofSection(conf: ConfigurationSection?, defKey: String? = null): Configuration? {
        return asSection(of(conf), defKey)
    }

    fun ofMap(conf: ConfigurationSection?, deep: Boolean = false): Map<String, Any?> {
        return ofSection(conf)?.getValues(deep) ?: mapOf()
    }

    fun <K, V> ofMap(conf: ConfigurationSection?, deep: Boolean = false, keyTransform: (String) -> K = { it as K }, valueTransform: (Any?) -> V = { it as V }): Map<K, V> {
        return ofMap(conf, deep).mapKeys { keyTransform(it.key) }.mapValues { valueTransform(it.value) }
    }

    fun ofLists(conf: ConfigurationSection?): List<List<String>> {
        val list = ofList(conf)
        return if (list.firstOrNull() is List<*>) {
            list.map { asList(it) }
        } else {
            val strs = asList(list)
            if (strs.isEmpty()) listOf() else listOf(strs)
        }
    }

    fun of(conf: ConfigurationSection?, def: Any? = null): Any? {
        return conf?.get(getKey(conf)) ?: def
    }

    fun getKey(conf: ConfigurationSection): String {
        return getSectionKey(conf, this)
    }

    companion object {

        fun asList(any: Any?): List<String> {
            if (any == null) return mutableListOf()
            return when (any) {
                is List<*> -> any.map { it.toString() }
                else -> listOf(any.toString())
            }
        }

        fun asAnyList(any: Any?): List<Any> {
            if (any == null) return mutableListOf()
            val result = mutableListOf<Any>()
            when (any) {
                is List<*> -> any.forEach { it?.let { any -> result.add(any) } }
                else -> result.add(any.toString())
            }
            return result
        }

        fun asIntList(any: Any?): List<Int> {
            if (any == null) return mutableListOf()
            val result = mutableListOf<Int>()
            when (any) {
                is List<*> -> any.forEach { result.add(it.toString().toInt()) }
                else -> result.add(any.toString().toInt())
            }
            return result
        }

        fun asArray(any: Any?): Array<String> = asList(any).toTypedArray()

        fun asIntArray(any: Any?): Array<Int> = asIntList(any).toTypedArray()

        fun asBoolean(any: Any?): Boolean = any.toString().toBoolean()

        fun asInt(any: Any?, def: Int = 0): Int = any.toString().toIntOrNull() ?: def

        fun asLong(any: Any?, def: Long = 0): Long = any.toString().toLongOrNull() ?: def

        fun <T> asLists(any: Any): List<List<T>> {
            val results = mutableListOf<List<T>>()
            when (any) {
                is List<*> -> {
                    if (any.isNotEmpty()) {
                        if (any[0] is List<*>) any.forEach { results.add(it as List<T>) }
                        else results.add(any as List<T>)
                    }
                }
                else -> results.add(listOf(any as T))
            }
            return results
        }

        fun asSection(any: Any?, defKey: String? = null): Configuration? = Configuration.empty().let {
            when (any) {
                is Configuration -> return any
                is ConfigSection -> {

                    return Configuration.empty().apply { setProperty("root", any.getProperty("root")) }
                }
                is Map<*, *> -> {
                    any.entries.forEach { entry -> it[entry.key.toString()] = entry.value }
                    return@let it
                }
                is List<*> -> if (defKey != null) {
                    it[defKey] = any
                    return@let it
                } else any.forEach { any ->
                    val args = any.toString().split(Regex(":"), 2)
                    if (args.size == 2) it[args[0]] = args[1]
                    return@let it
                }
            }
            return@let null
        }

        fun getSectionKey(section: ConfigurationSection?, property: Property) =
            getSectionKey(section, property.regex, property.default, false)

        fun getSectionKey(section: ConfigurationSection?, regex: Regex, default: String = "", deep: Boolean = false) =
            section?.getKeys(deep)?.firstOrNull { it.matches(regex) } ?: default

    }

}