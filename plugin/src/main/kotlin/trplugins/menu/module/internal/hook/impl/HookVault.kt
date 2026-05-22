package trplugins.menu.module.internal.hook.impl

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import trplugins.menu.module.internal.hook.HookAbstract


/**
 * @author Arasple
 * @date 2021/2/9 11:45
 */
class HookVault : HookAbstract() {

    /**
     * Active 阶段缓存的“主类直接实现 [Economy] 接口”的插件实例。
     *
     * 仅作为 ServicesManager 兜底；服务器 [taboolib.common.LifeCycle.ACTIVE]
     * 时由 [HookPlugin] 统一触发 [onServerActive] 计算一次，
     * 之后金钱操作直接读取该字段，避免每次都遍历 [Bukkit.getPluginManager] 插件表。
     *
     * 覆盖 EzEconomy 这类既不命中 Vault 内置 hookEconomy 名单、
     * 自身也未调用 ServicesManager#register 的经济插件。
     */
    @Volatile
    private var pluginEconomy: Economy? = null

    override fun onServerActive() {
        if (!isHooked) return
        pluginEconomy = resolveFromPlugins()
    }

    /**
     * 当前可用的 Vault Economy provider。
     *
     * 1. 优先从 Bukkit ServicesManager 取最高优先级 Economy 注册项；
     *    实现插件已禁用时跳过。
     * 2. ServicesManager 中没有时回落到 Active 阶段缓存的 [pluginEconomy]。
     */
    private val economyAPI: Economy?
        get() {
            if (!isHooked) {
                reportAbuse()
                return null
            }
            return resolveFromServices() ?: pluginEconomy
        }

    private fun resolveFromServices(): Economy? {
        val registration = Bukkit.getServicesManager().getRegistration(Economy::class.java) ?: return null
        return if (registration.plugin.isEnabled) registration.provider else null
    }

    private fun resolveFromPlugins(): Economy? {
        return Bukkit.getPluginManager().plugins
            .asSequence()
            .filter { it.isEnabled && it.name != name }
            .filterIsInstance<Economy>()
            .firstOrNull()
    }

    /**
     * 当前 Economy provider 背后的实现插件实例。
     *
     * provider 自身就是 Bukkit Plugin（兜底分支）时直接返回；
     * 否则按 [Economy.getName] 在 Bukkit 插件表中匹配。
     */
    val economyPlugin: Plugin?
        get() {
            val provider = economyAPI ?: return null
            return provider as? Plugin
                ?: Bukkit.getPluginManager().getPlugin(provider.name)
        }

    /**
     * 仅当 provider 与实现插件均可用时才返回 [Economy]，
     * 避免实现插件被禁用后调用残留 provider 抛错。
     */
    private fun economy(): Economy? {
        val provider = economyAPI ?: return null
        return if (economyPlugin?.isEnabled == true) provider else null
    }

    fun takeMoney(player: OfflinePlayer, money: Double) {
        economy()?.withdrawPlayer(player, money)
    }

    fun addMoney(player: OfflinePlayer, money: Double) {
        economy()?.depositPlayer(player, money)
    }

    fun hasMoney(player: OfflinePlayer, money: Double): Boolean {
        return economy()?.has(player, money) ?: false
    }

    private fun getMoney(player: OfflinePlayer): Double {
        return economy()?.getBalance(player) ?: 0.0
    }

    fun setMoney(player: OfflinePlayer, money: Double) {
        addMoney(player, money - getMoney(player))
    }

}
