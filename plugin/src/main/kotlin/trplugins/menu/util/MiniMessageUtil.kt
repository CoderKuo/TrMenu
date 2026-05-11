package trplugins.menu.util

/**
 * @author Anahes37
 * @date 2026/05/11 14:45
 */
object MiniMessageUtil {

    private val MINI_MESSAGE_PATTERN = Regex("<[a-zA-Z#!][^<>]*>")

    fun hasMiniMessageTags(text: String): Boolean = MINI_MESSAGE_PATTERN.containsMatchIn(text)

    fun toJson(text: String): String {
        return runCatching {
            val mm = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage")
                .getMethod("miniMessage")
                .invoke(null)
            val component = mm.javaClass
                .getMethod("deserialize", String::class.java)
                .invoke(mm, text)
            val gson = Class.forName("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer")
                .getMethod("gson")
                .invoke(null)
            val componentClass = Class.forName("net.kyori.adventure.text.Component")
            gson.javaClass
                .getMethod("serialize", componentClass)
                .invoke(gson, component) as String
        }.getOrElse { text.colorify() }
    }
}
