package trplugins.menu.module.internal.hook.impl

import org.bukkit.OfflinePlayer
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
            val clazz = Class.forName(apiClassName)
            clazz.getMethod("get").invoke(null)
        } catch (e: Exception) {
            null
        }
    }

    fun getBalance(player: OfflinePlayer, currencyId: String): Double {
        val api = getApiInstance() ?: return 0.0
        return try {
            val method = api.javaClass.getMethod("getBalance", OfflinePlayer::class.java, String::class.java)
            (method.invoke(api, player, currencyId) as Number).toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    fun takeBalance(player: OfflinePlayer, currencyId: String, amount: Double): Boolean {
        val api = getApiInstance() ?: return false
        return try {
            val method = api.javaClass.getMethod("takeBalance", OfflinePlayer::class.java, String::class.java, Double::class.java)
            method.invoke(api, player, currencyId, amount) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    fun giveBalance(player: OfflinePlayer, currencyId: String, amount: Double): Boolean {
        val api = getApiInstance() ?: return false
        return try {
            val method = api.javaClass.getMethod("giveBalance", OfflinePlayer::class.java, String::class.java, Double::class.java)
            method.invoke(api, player, currencyId, amount) as Boolean
        } catch (e: Exception) {
            false
        }
    }

}
