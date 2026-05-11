package trplugins.menu

import org.bukkit.Bukkit
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.console
import taboolib.common.platform.function.pluginVersion
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.lang.Language
import taboolib.module.lang.sendLang
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.bukkitPlugin
import trplugins.menu.api.action.ActionHandle
import trplugins.menu.api.action.impl.menu.SetTitle
import trplugins.menu.api.action.impl.send.Tell
import trplugins.menu.api.receptacle.provider.PlatformProvider
import trplugins.menu.api.receptacle.vanilla.window.NMS
import trplugins.menu.module.conf.Loader
import trplugins.menu.module.conf.prop.RunningPerformance
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.display.session
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.database.GlobalDataDao
import trplugins.menu.module.internal.database.MetaDataDao
import trplugins.menu.module.internal.hook.HookPlugin
import trplugins.menu.module.internal.inputer.Inputer.Companion.cancelWords
import trplugins.menu.module.internal.listener.ListenerItemInteract.interactCooldown
import trplugins.menu.module.internal.script.Bindings
import trplugins.menu.module.internal.script.evalScript
import trplugins.menu.module.internal.service.RegisterCommands
import trplugins.menu.module.internal.service.Shortcuts
import trplugins.menu.util.bukkit.Heads

/**
 * @author Arasple
 * @date 2021/1/24 9:51
 */
object TrMenu : Plugin() {

    @Config("settings.yml", migrate = true, autoReload = true)
    lateinit var SETTINGS: Configuration
        private set

    val plugin by lazy { bukkitPlugin }

    var performance = RunningPerformance.NORMAL
        private set

    lateinit var actionHandle: ActionHandle
        private set

    override fun onLoad() {
        Language.default = "zh_CN"
        actionHandle = ActionHandle({ t, u -> t.evalScript(u) }, { t, u -> t.session().parse(u) }, "kether")
        console().sendLang("Plugin-Loading", Bukkit.getVersion())
    }

    override fun onEnable() {
        SETTINGS.onReload { onSettingsReload() }
        onSettingsReload()
        Loader.loadMenus()
        Metadata.database
        MetaDataDao.door
        GlobalDataDao.door
        console().sendLang("Plugin-Enabled", pluginVersion)
        console().sendLang("Plugin-Version")
        HookPlugin.printInfo()
    }

    override fun onDisable() {
        MenuSession.SESSIONS.entries.removeIf {
            it.value.close(closePacket = true, updateInventory = true)
            true
        }
    }

    private fun onSettingsReload() {
        Language.default = SETTINGS.getString("Language.Default") ?: "zh_CN"
        MenuSession.langPlayer = SETTINGS.getString("Language.Player") ?: ""
        SETTINGS.getConfigurationSection("Language.CodeTransfer")?.also {
            Language.languageCodeTransfer.clear()
            it.getKeys(false).forEach { lang ->
                Language.languageCodeTransfer[lang] = it.getString(lang) ?: Language.default
            }
        }

        performance = kotlin.runCatching {
            RunningPerformance.valueOf(SETTINGS.getString("Options.Running-Performance") ?: "Normal")
        }.getOrNull() ?: RunningPerformance.NORMAL

        cancelWords.reload()
        interactCooldown.reload()
        Shortcuts.Type.load()
        RegisterCommands.load()
        Bindings.load()
        Tell.useComponent = SETTINGS.getBoolean("Action.Using-Component", true)
        SetTitle.useComponent = if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_14)) SETTINGS.getBoolean("Action.Title-Using-Component", false) else false
        SetTitle.useMiniMessage = if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_16)) SETTINGS.getBoolean("Action.Title-MiniMessage", false) else false
        PlatformProvider.compute()
        NMS.javaStaticInventory = SETTINGS.getBoolean("Options.Static-Inventory.Java", false)
        NMS.bedrockStaticInventory = SETTINGS.getBoolean("Options.Static-Inventory.Bedrock", false)
        NMS.createIdPacketInventory = SETTINGS.getBoolean("Options.Packet-Inventory.Create-Id", false)
        Heads.headConnectTimeout = SETTINGS.getInt("Menu.Icon.Item.Head-Connect-Timeout", 500)
        Heads.headReadTimeout = SETTINGS.getInt("Menu.Icon.Item.Head-Read-Timeout", 2500)
    }

}