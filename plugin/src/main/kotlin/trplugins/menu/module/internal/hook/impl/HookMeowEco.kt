package trplugins.menu.module.internal.hook.impl

import org.bukkit.OfflinePlayer
import org.bukkit.Bukkit
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import trplugins.menu.module.internal.hook.HookAbstract
import java.util.UUID

/**
 * @author Arasple
 * @date 2024/3/7
 */
class HookMeowEco : HookAbstract() {

    private val apiClassName = "com.xiaoyiluck.meoweco.api.MeowEcoAPI"

    private fun getApiInstance(): Any? {
        if (!isHooked) return null
        return runCatching {
            val apiClass = Class.forName(apiClassName)
            val registration = Bukkit.getServicesManager().invokeMethod<Any>("getRegistration", apiClass) ?: return null
            registration.invokeMethod<Any>("getProvider")
        }.getOrNull()
    }

    private fun invokeApi(api: Any, methodName: String, vararg rawArgs: Any?): Any? {
        return runCatching { api.invokeMethod<Any?>(methodName, *rawArgs) }.getOrNull()
    }

    private fun asDouble(value: Any?): Double {
        return when (value) {
            is Number -> value.toDouble()
            else -> value?.toString()?.toDoubleOrNull() ?: 0.0
        }
    }

    private fun asBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            else -> value?.toString()?.toBooleanStrictOrNull() ?: false
        }
    }

    fun getBalance(player: OfflinePlayer, currencyId: String): Double {
        val api = getApiInstance() ?: return 0.0
        return asDouble(invokeApi(api, "getBalance", player.uniqueId, currencyId))
    }

    fun takeBalance(player: OfflinePlayer, currencyId: String, amount: Double): Boolean {
        val api = getApiInstance() ?: return false
        return asBoolean(invokeApi(api, "withdraw", player.uniqueId, currencyId, amount))
    }

    fun giveBalance(player: OfflinePlayer, currencyId: String, amount: Double): Boolean {
        val api = getApiInstance() ?: return false
        return asBoolean(invokeApi(api, "deposit", player.uniqueId, currencyId, amount))
    }

    fun getAvailableBalance(player: OfflinePlayer, currencyId: String): Double {
        val api = getApiInstance() ?: return 0.0
        return asDouble(invokeApi(api, "getAvailableBalance", player.uniqueId, currencyId))
    }

}
