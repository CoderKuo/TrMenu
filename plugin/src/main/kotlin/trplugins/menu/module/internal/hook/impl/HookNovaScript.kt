package trplugins.menu.module.internal.hook.impl

import com.dakuo.novascript.NovaScriptAPI
import com.dakuo.novascript.ScriptHandler0
import com.dakuo.novascript.ScriptHandler1
import com.dakuo.novascript.ScriptHandler2
import com.dakuo.novascript.ScriptHandler3
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import trplugins.menu.api.TrMenuAPI
import trplugins.menu.api.event.MenuOpenEvent
import trplugins.menu.module.display.Menu
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.hook.HookAbstract

class HookNovaScript : HookAbstract() {

    override fun bindingScript() {
        super.bindingScript()
        val p = plugin ?: return
        if (!p.isEnabled) return

        NovaScriptAPI.defineLibrary(p.name, "trmenu") { lib ->
            // 菜单操作
            lib.defineFunction("openMenu", ScriptHandler2 { player, menuId ->
                val p = asPlayer(player)
                val menu = TrMenuAPI.getMenuById(menuId.toString())
                if (p != null && menu != null) {
                    menu.open(p, reason = MenuOpenEvent.Reason.CONSOLE)
                }
                null
            })

            lib.defineFunction("closeMenu", ScriptHandler1 { player ->
                val p = asPlayer(player)
                if (p != null) {
                    val session = MenuSession.getSession(p)
                    session.close(closePacket = true, updateInventory = true)
                }
                null
            })

            lib.defineFunction("getMenuById", ScriptHandler1 { menuId ->
                TrMenuAPI.getMenuById(menuId.toString())
            })

            lib.defineFunction("getMenuList", ScriptHandler0 {
                Menu.menus.map { it.id }
            })

            // 玩家持久数据
            lib.defineFunction("getData", ScriptHandler2 { player, key ->
                val p = asPlayer(player) ?: return@ScriptHandler2 null
                Metadata.getData(p)[key.toString()]
            })

            lib.defineFunction("setData", ScriptHandler3 { player, key, value ->
                val p = asPlayer(player) ?: return@ScriptHandler3 null
                Metadata.setData(p, Metadata.DataType.DATA, key.toString(), value)
                null
            })

            // 玩家元数据
            lib.defineFunction("getMeta", ScriptHandler2 { player, key ->
                val p = asPlayer(player) ?: return@ScriptHandler2 null
                Metadata.getMeta(p)[key.toString()]
            })

            lib.defineFunction("setMeta", ScriptHandler3 { player, key, value ->
                val p = asPlayer(player) ?: return@ScriptHandler3 null
                Metadata.setData(p, Metadata.DataType.META, key.toString(), value)
                null
            })

            // 全局数据
            lib.defineFunction("getGlobalData", ScriptHandler1 { key ->
                Metadata.getGlobalData(key.toString())
            })

            lib.defineFunction("setGlobalData", ScriptHandler2 { key, value ->
                Metadata.setGlobalData(key.toString(), value)
                null
            })

            // Kether 脚本执行
            lib.defineFunction("eval", ScriptHandler2 { player, script ->
                val p = asPlayer(player) ?: return@ScriptHandler2 null
                TrMenuAPI.instantKether(p, script.toString()).any
            })

            // 会话信息
            lib.defineFunction("isViewingMenu", ScriptHandler1 { player ->
                val p = asPlayer(player) ?: return@ScriptHandler1 false
                MenuSession.getSession(p).isViewing()
            })

            lib.defineFunction("getViewingMenuId", ScriptHandler1 { player ->
                val p = asPlayer(player) ?: return@ScriptHandler1 null
                MenuSession.getSession(p).menu?.id
            })
        }
    }

    private fun asPlayer(obj: Any?): Player? {
        return when (obj) {
            is Player -> obj
            is String -> Bukkit.getPlayerExact(obj)
            else -> null
        }
    }
}
