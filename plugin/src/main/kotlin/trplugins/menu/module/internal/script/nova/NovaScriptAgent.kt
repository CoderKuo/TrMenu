package trplugins.menu.module.internal.script.nova

import com.dakuo.novascript.NovaCompiled
import com.dakuo.novascript.NovaScriptAPI
import com.dakuo.novascript.ScriptHandler1
import com.dakuo.novascript.ScriptHandler2
import com.google.common.collect.Maps
import org.bukkit.Bukkit
import trplugins.menu.api.TrMenuAPI
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.hook.HookPlugin
import trplugins.menu.module.internal.script.Assist
import trplugins.menu.util.EvalResult

object NovaScriptAgent {

    private val prefixes = arrayOf(
        "nova: ",
    )

    private val compiledScripts = Maps.newConcurrentMap<String, NovaCompiled>()

    /** 当前正在执行的 session，编译时定义的函数通过此字段访问运行时上下文 */
    private val currentSession = ThreadLocal<MenuSession?>()

    fun serialize(script: String): Pair<Boolean, String?> {
        prefixes.firstOrNull { script.startsWith(it) }?.let {
            return true to script.removePrefix(it)
        }
        return false to null
    }

    fun preCompile(script: String) {
        if (!HookPlugin.getNovaScript().isHooked) return
        compiledScripts.computeIfAbsent(script) { compile(it) }
    }

    fun eval(session: MenuSession, script: String): EvalResult {
        if (!HookPlugin.getNovaScript().isHooked) return EvalResult(null)
        val compiled = compiledScripts.computeIfAbsent(script) { compile(it) }
        currentSession.set(session)
        try {
            return EvalResult(compiled.run(
                "session", session,
                "player", session.viewer,
                "sender", session.viewer,
                "args", session.arguments,
                "menuId", session.menu?.id,
                "data", Metadata.getData(session.viewer).data,
                "meta", Metadata.getMeta(session.viewer).data,
                "config", session.menu?.conf,
            ))
        } finally {
            currentSession.remove()
        }
    }

    private fun compile(script: String): NovaCompiled {
        return NovaScriptAPI.compileToBytecode(script) { setup ->
            // 静态对象
            setup.set("bukkitServer", Bukkit.getServer())
            setup.set("utils", Assist.INSTANCE)

            // 占位符解析: parse("{player_name}") -> "Steve"
            setup.defineFunction("parse", ScriptHandler1 { str ->
                currentSession.get()?.parse(str.toString()) ?: str.toString()
            })
            setup.defineFunction("parseAsInt", ScriptHandler1 { str ->
                currentSession.get()?.parse(str.toString())?.toIntOrNull() ?: 0
            })
            setup.defineFunction("parseAsDouble", ScriptHandler1 { str ->
                currentSession.get()?.parse(str.toString())?.toDoubleOrNull() ?: 0.0
            })

            // 菜单节点: node("Title") -> 菜单配置中的值
            setup.defineFunction("node", ScriptHandler1 { key ->
                currentSession.get()?.getNodeValue(key.toString())
            })
            setup.defineFunction("nodeAsInt", ScriptHandler1 { key ->
                currentSession.get()?.getNodeValue(key.toString())?.toString()?.toIntOrNull() ?: 0
            })
            setup.defineFunction("nodeAsDouble", ScriptHandler1 { key ->
                currentSession.get()?.getNodeValue(key.toString())?.toString()?.toDoubleOrNull() ?: 0.0
            })

            // Kether 脚本执行
            setup.defineFunction("kether", ScriptHandler1 { script ->
                val s = currentSession.get() ?: return@ScriptHandler1 null
                TrMenuAPI.instantKether(s.placeholderPlayer, script.toString()).any
            })

            // 全局数据快捷访问
            setup.defineFunction("globalData", ScriptHandler1 { key ->
                Metadata.getGlobalData(key.toString())
            })
            setup.defineFunction("setGlobalData", ScriptHandler2 { key, value ->
                Metadata.setGlobalData(key.toString(), value)
                null
            })
        }
    }
}
