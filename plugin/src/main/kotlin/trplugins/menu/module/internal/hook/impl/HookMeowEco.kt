package trplugins.menu.module.internal.hook.impl

import org.bukkit.OfflinePlayer
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import trplugins.menu.module.internal.hook.HookAbstract

/**
 * @author Arasple
 * @date 2024/3/7
 */
class HookMeowEco : HookAbstract() {

    private val apiClassName = "com.xiaoyiluck.meoweco.api.MeowEcoAPI"

    private fun getApiInstance(): Any? {
        if (!isHooked) return null
        return try {
            Class.forName(apiClassName).invokeMethod<Any>("get")
        } catch (e: Throwable) {
            null
        }
    }

    fun getBalance(player: OfflinePlayer, currencyId: String): Double {
        val api = getApiInstance() ?: return 0.0
        return try {
            api.invokeMethod<Number>("getBalance", player, currencyId)?.toDouble() ?: 0.0
        } catch (e: Throwable) {
            0.0
        }
    }

    fun takeBalance(player: OfflinePlayer, currencyId: String, amount: Double): Boolean {
        val api = getApiInstance() ?: return false
        return try {
            api.invokeMethod<Boolean>("takeBalance", player, currencyId, amount) ?: false
        } catch (e: Throwable) {
            false
        }
    }

    fun giveBalance(player: OfflinePlayer, currencyId: String, amount: Double): Boolean {
        val api = getApiInstance() ?: return false
        return try {
            api.invokeMethod<Boolean>("giveBalance", player, currencyId, amount) ?: false
        } catch (e: Throwable) {
            false
        }
    }

}
