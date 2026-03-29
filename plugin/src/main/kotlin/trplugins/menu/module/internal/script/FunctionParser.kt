package trplugins.menu.module.internal.script

import org.bukkit.entity.Player
import taboolib.common.util.subList
import trplugins.menu.api.TrMenuAPI
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.data.Metadata
import trplugins.menu.module.internal.hook.HookPlugin
import trplugins.menu.module.internal.script.jexl.JexlAgent
import trplugins.menu.module.internal.script.js.JavaScriptAgent
import trplugins.menu.module.internal.script.nova.NovaScriptAgent
import trplugins.menu.util.Regexs
import trplugins.menu.util.collections.Variables
import trplugins.menu.util.print

/**
 * @author Arasple
 * @date 2021/1/31 17:17
 */
object FunctionParser {

    private val internalFunctionPattern = "\\$\\{([^0-9].+?[^\\\\}])}".toRegex()

    fun parse(
        player: Player,
        input: String,
        block: (type: String, value: String) -> String? = { _, value -> "{$value}" }
    ): String {
        return runCatching {
            if (!Regexs.containsPlaceholder(input)) return input
            val session = MenuSession.getSession(player)

            val functionParsed = parseFunctionExpressions(input).joinToString("") {
                if (it.isVariable) {
                    val split = it.value.split(":", limit = 2)
                    if (split.size < 2) return@joinToString it.value
                    val value = split[1]

                    when (val type = split[0].removePrefix(" ").lowercase()) {
                        "kether", "ke" -> parseKetherFunction(player, value)
                        "javascript", "js" -> parseJavaScript(session, value)
                        "jexl" -> parseJexlScript(session, value)
                        "nova", "novalang","novascript" -> parseNovaScript(session, value)
                        "meta", "m" -> Metadata.getMeta(player)[value].toString()
                        "data", "d" -> Metadata.getData(player)[value].toString()
                        "globaldata", "gdata", "g" -> Metadata.getGlobalData(value)?.toString() ?: "null"
                        "triton" -> parseLangText(player, value)

                        else -> block(type, value) ?: "{${it.value}}"
                    }
                } else it.value
            }

            return Variables(functionParsed, internalFunctionPattern) {
                it[1]
            }.element.joinToString("") {
                if (it.isVariable) parseInternalFunction(session, it.value)
                else it.value
            }
        }.onFailure {
            it.print("Error occured when parsing the string for player ${player.name}: $input")
        }.getOrElse { input }
    }

    /**
     * 使用括号平衡扫描解析 {type: content} 或 ${type: content} 表达式，
     * 替代原先的正则匹配，以支持内容中包含嵌套 {} 的脚本语言（如 Lambda、when 块等）。
     */
    private fun parseFunctionExpressions(input: String): List<Variables.Element> {
        val elements = mutableListOf<Variables.Element>()
        var lastEnd = 0
        var i = 0

        while (i < input.length) {
            val matchStart = i

            // 检测 ${ 或 {
            if (input[i] == '$' && i + 1 < input.length && input[i + 1] == '{') {
                i += 2
            } else if (input[i] == '{') {
                i++
            } else {
                i++
                continue
            }

            // 匹配类型名: \w+ (字母、数字、下划线)
            val typeStart = i
            while (i < input.length && (input[i].isLetterOrDigit() || input[i] == '_')) {
                i++
            }
            if (i == typeStart) {
                i = matchStart + 1
                continue
            }
            val type = input.substring(typeStart, i)

            // 要求 ': ' (冒号 + 可选空格)
            if (i >= input.length || input[i] != ':') {
                i = matchStart + 1
                continue
            }
            i++ // 跳过 ':'
            if (i < input.length && input[i] == ' ') {
                i++ // 跳过可选空格
            }

            val contentStart = i

            // 括号平衡扫描
            var depth = 1
            var found = false
            while (i < input.length) {
                val ch = input[i]

                // 字符串字面量: 跳过直到匹配的引号
                if (ch == '"' || ch == '\'') {
                    val quote = ch
                    i++
                    while (i < input.length) {
                        if (input[i] == '\\' && i + 1 < input.length) {
                            i += 2
                        } else if (input[i] == quote) {
                            i++
                            break
                        } else {
                            i++
                        }
                    }
                    continue
                }

                // 转义字符: 跳过下一个字符 (兼容旧版 \} 转义)
                if (ch == '\\' && i + 1 < input.length) {
                    i += 2
                    continue
                }

                when (ch) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            found = true
                            break
                        }
                    }
                }
                i++
            }

            if (!found) {
                i = matchStart + 1
                continue
            }

            // 提取内容
            val content = input.substring(contentStart, i)

            // 添加前置的非变量文本
            if (matchStart > lastEnd) {
                elements.add(Variables.Element(input.substring(lastEnd, matchStart)))
            }

            // 添加变量元素 (格式与原 Variables 回调一致: "type:content")
            elements.add(Variables.Element("$type:$content", true))

            i++ // 跳过闭合 '}'
            lastEnd = i
        }

        // 添加剩余文本
        if (lastEnd < input.length) {
            elements.add(Variables.Element(input.substring(lastEnd)))
        }

        return elements
    }

    private fun parseInternalFunction(session: MenuSession, input: String): String {
        val func = input.split("_")

        session.menu?.settings?.internalFunctions?.forEach {
            if (it.id == func[0]) {
                val args = subList(func, 1, func.size)
                return it.compile(session, args).asString()
            }
        }
        return "___ UNKNOWN_FUNCTION_$input ___"
    }

    private fun parseKetherFunction(player: Player, input: String): String {
        return TrMenuAPI.instantKether(player, input).asString()
    }

    private fun parseJavaScript(session: MenuSession, input: String): String {
        return JavaScriptAgent.eval(session, input).asString()
    }

    private fun parseJexlScript(session: MenuSession, input: String): String {
        return JexlAgent.eval(session, input).asString()
    }

    private fun parseNovaScript(session: MenuSession, input: String): String {
        return NovaScriptAgent.eval(session, input).asString()
    }

    private fun parseLangText(player: Player, text: String): String {
        val split = text.split("=", limit = 2)
        return HookPlugin.getTriton()
            .getText(player, split[0], if (split.size < 2) emptyArray() else split[1].split("_||_").toTypedArray())
            .toString()
    }

}
