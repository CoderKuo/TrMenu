package trplugins.menu.module.display

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.console
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.submit
import taboolib.module.chat.component
import taboolib.module.configuration.Configuration
import taboolib.module.lang.Type
import taboolib.platform.util.cancelNextChat
import trplugins.menu.TrMenu
import trplugins.menu.api.action.impl.menu.SetTitle
import trplugins.menu.api.event.MenuOpenEvent
import trplugins.menu.api.event.MenuPageChangeEvent
import trplugins.menu.api.receptacle.provider.PlatformProvider
import trplugins.menu.api.receptacle.vanilla.window.WindowReceptacle
import trplugins.menu.module.crafting.CraftingSpec
import trplugins.menu.module.display.dialog.model.DialogMenuSpec
import trplugins.menu.module.display.dialog.runtime.DialogMenuRenderer
import trplugins.menu.module.display.icon.Icon
import trplugins.menu.module.display.layout.MenuLayout
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.script.evalAction
import trplugins.menu.module.internal.script.evalScript
import trplugins.menu.util.colorify
import trplugins.menu.util.parseJson
import java.util.function.Consumer

/**
 * @author Arasple
 * @date 2021/1/24 10:01
 */
class Menu(
    val id: String,
    val settings: MenuSettings,
    val layout: MenuLayout,
    val icons: Set<Icon>,
    val renderType: MenuRenderType = MenuRenderType.WINDOW,
    val dialogSpec: DialogMenuSpec? = null,
    conf: Configuration,
    private val langKey: String? = null,
    lang: Map<String, Map<String, Type>>? = null,
    val craftingSpec: CraftingSpec? = null
) {

    companion object {

        val menus = mutableListOf<Menu>()
        val BEDROCK_DELAY get() = TrMenu.SETTINGS.getLong("Options.Bedrock-Open-Delay", 20L)

    }

    var conf: Configuration = conf
        internal set

    var lang: Map<String, Map<String, Type>>? = lang
        internal set

    val viewers: MutableSet<String> = mutableSetOf()

    fun open(
        viewer: Player,
        page: Int? = null,
        reason: MenuOpenEvent.Reason,
        block: Consumer<MenuSession>
    ) =
        open(viewer, page, reason) { menuSession -> block.accept(menuSession) }

    /**
     * 开启菜单
     */
    fun open(
        viewer: Player,
        page: Int? = null,
        reason: MenuOpenEvent.Reason,
        block: (MenuSession) -> Unit = {}
    ) {
        val session = MenuSession.getSession(viewer)
        viewers.add(viewer.name)

        val determinedPage = page ?: settings.determinePage(session)

        var menuSwitch = false
        if (session.menu == this) {
            return page(viewer, determinedPage)
        } else if (session.menu != null) {
            menuSwitch = true
            if (session.renderType == MenuRenderType.DIALOG) {
                DialogMenuRenderer.close(session, true)
            } else if (PlatformProvider.isBedrockPlayer(viewer)) {
                session.receptacle?.close(true)
            }
            session.shut()
        }

        val menuOpenEvent = MenuOpenEvent(session, this, determinedPage, reason)
        menuOpenEvent.call()

        if (menuOpenEvent.isCancelled) return
        session.menu = this
        session.renderType = renderType
        session.cacheNodes()
        block(session)

        if (!Metadata.byBukkit(viewer, "FORCE_OPEN") && !settings.openEvent.eval(adaptPlayer(session.viewer))) {
            session.menu = null
            session.renderType = null
            return
        }
        if (renderType == MenuRenderType.DIALOG) {
            viewer.cancelNextChat(false)
            session.page = determinedPage
            loadTasks(session)
            DialogMenuRenderer.open(session, determinedPage)
            return
        }
        openWindow(viewer, determinedPage, session, menuSwitch)
    }

    /**
     * 本菜单内切换页码
     */
    fun page(viewer: Player, page: Int, title: String? = null) {
        val session = MenuSession.getSession(viewer)
        if (renderType == MenuRenderType.DIALOG) {
            val spec = dialogSpec ?: return
            if (page < 0 || page >= spec.pageCount()) return
            val menuPageChangeEvent = MenuPageChangeEvent(session, session.page, page, false)
            menuPageChangeEvent.call()
            if (menuPageChangeEvent.isCancelled) return
            DialogMenuRenderer.page(session, page, title)
            return
        }
        if (page < 0 || page > layout.getSize()) return
        pageWindow(viewer, page, title, session)
    }

    private fun openWindow(viewer: Player, determinedPage: Int, session: MenuSession, menuSwitch: Boolean) {
        if (session.receptacle != null || session.menu != this) {
            return
        }
        viewer.cancelNextChat(false)
        val layout = layout[determinedPage]
        val receptacle: WindowReceptacle

        session.page = determinedPage
        session.receptacle = layout.baseReceptacle().also { receptacle = it }
        session.playerItemSlots()

        layout.initReceptacle(session)
        loadTitle(session)
        loadIcon(session)
        loadTasks(session)

        if (menuSwitch && BEDROCK_DELAY > 0 && PlatformProvider.isBedrockPlayer(viewer)) {
            submit(async = Bukkit.isPrimaryThread(), delay = BEDROCK_DELAY) {
                receptacle.open(viewer)
                settings.properties.forEach { (id, value) ->
                    if (id >= 0 && value != null) {
                        receptacle.property(id, value)
                    }
                }
            }
        } else {
            receptacle.open(viewer)
            settings.properties.forEach { (id, value) ->
                if (id >= 0 && value != null) {
                    receptacle.property(id, value)
                }
            }
        }
    }

    private fun pageWindow(viewer: Player, page: Int, title: String? = null, session: MenuSession = MenuSession.getSession(viewer)) {
        val previous = session.layout() ?: return
        val layout = layout[page]
        val receptacle: WindowReceptacle
        val override = previous.isSimilar(layout) && session.receptacle != null && title == null

        val menuPageChangeEvent = MenuPageChangeEvent(session, session.page, page, override)
        menuPageChangeEvent.call()

        if (menuPageChangeEvent.isCancelled) return
        if (override) {
            receptacle = session.receptacle!!
            receptacle.clear()
        } else {
            session.receptacle = layout.baseReceptacle().also { receptacle = it }
            layout.initReceptacle(session)
        }

        session.page = page
        session.playerItemSlots()
        loadIcon(session)

        if (override) {
            receptacle.refresh()
            session.updateActiveSlots()
        } else {
            if (title == null) {
                if (!settings.title(session).cyclable()) {
                    loadTitle(session)
                }
            } else {
                val parseTitle = formatTitle(title)
                session.receptacle?.title(parseTitle, update = false)
            }
            if (BEDROCK_DELAY > 0 && PlatformProvider.isBedrockPlayer(viewer)) {
                submit(async = Bukkit.isPrimaryThread(), delay = BEDROCK_DELAY) {
                    receptacle.open(viewer)
                }
            } else {
                receptacle.open(viewer)
            }
        }
    }

    /**
     * 加载容器标题 & 自动更新
     */
    private fun loadTitle(session: MenuSession) {
        val title = settings.title(session)
        session.receptacle?.title(title.next(session.id)?.let {
            formatTitle(session.parse(it))
        } ?: pluginId, update = false)

        val setTitle = {
            session.receptacle?.title(title.next(session.id)?.let {
                formatTitle(session.parse(it))
            } ?: pluginId)
        }

        if (settings.titleUpdate > 0 && title.cyclable()) {
            session.arrange(submit(delay = 10, period = settings.titleUpdate.toLong(), async = true) {
                setTitle()
            })
        }
    }

    private fun formatTitle(title: String): String {
        val colored = if (SetTitle.useComponent && runCatching { title.parseJson() }.isSuccess) {
            title
        } else {
            title.colorify()
        }
        return if (SetTitle.useComponent) colored.component().build().toRawMessage() else colored
    }

    private fun loadIcon(session: MenuSession) {
        session.shutTemps()
        icons
            .forEach {
                it.position.reset(session)

                if (it.isAvailable(session)) {
                    try {
                        it.onRefresh(session)
                        it.startup(session)
                        session.activeIcons.add(it)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        console().sendMessage("ICON: ${it.id}")
                    }
                }
            }
    }

    private fun loadTasks(session: MenuSession) {
        settings.tasks.forEach { taskData ->
            taskData.actions.forEach { sub ->
                session.arrange(
                    submit(delay = 5L, period = taskData.period, async = true) {
                        val asBoolean = sub.condition.evalScript(session).asBoolean(false)
                        if (asBoolean) {
                            session.placeholderPlayer.evalAction(sub.actions.joinToString(" "))
                        }
                    }
                )
            }
        }
    }

    fun getIcon(id: String): Icon? {
        return icons.find { it.id == id }
    }

    fun getLocaleNode(locale: String, key: String): Type? {
        if (lang == null) {
            return null
        }
        return lang?.get(locale)?.let { provided ->
            provided[key]
        } ?: lang?.get("default")?.let { default ->
            default[key]
        }
    }

    fun getLocaleValue(locale: String, key: String): Any? {
        if (langKey == null) {
            return null
        }
        return conf.getConfigurationSection("$langKey.$locale")?.let { provided ->
            provided.getKeys(true).find { it.equals(key, ignoreCase = true) }?.let { provided[it] }
        } ?: conf.getConfigurationSection("$langKey.default")?.let { default ->
            default.getKeys(true).find { it.equals(key, ignoreCase = true) }?.let { default[it] }
        }
    }

    private fun forViewers(block: (Player) -> Unit) {
        viewers.mapNotNull { Bukkit.getPlayerExact(it) }.forEach(block)
    }

    @Suppress("unused")
    fun forSessions(block: Consumer<MenuSession>) =
        forSessions { block.accept(it) }


    fun forSessions(block: (MenuSession) -> Unit) {
        forViewers { block(MenuSession.getSession(it)) }
    }

    /**
     * PRIVATE
     */
    fun isFreeSlot(slot: Int): Boolean {
        return settings.freeSlots.contains(slot)
    }

    fun removeViewer(viewer: Player) {
        viewers.remove(viewer.name)
    }

    fun removeViewers() {
        viewers.clear()
    }

}
