/*
 * *
 *  * @author Arasple
 *  * @date 2021/2/1 17:39
 *
 */

package trplugins.menu.module.display.item

import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.script.evalScript
import trplugins.menu.util.Regexs

data class DisplayEnchant(
    val key: String,
    val level: String = "1"
) {

    private val isKeyDynamic = Regexs.containsPlaceholder(key)
    private val isLevelDynamic = Regexs.containsPlaceholder(level) || level.toIntOrNull() == null
    val isDynamic = isKeyDynamic || isLevelDynamic

    fun enchantKey(session: MenuSession): String {
        return if (isKeyDynamic) session.parse(key) else key
    }

    fun enchantLevel(session: MenuSession): Int {
        val parsed = if (isLevelDynamic) session.parse(level) else level
        return parsed.toIntOrNull() ?: session.placeholderPlayer.evalScript(level).asInt(1)
    }
}