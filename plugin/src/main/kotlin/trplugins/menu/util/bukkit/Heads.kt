package trplugins.menu.util.bukkit

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.authlib.GameProfile
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.xseries.XMaterial
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.BukkitSkull
import taboolib.common.platform.function.submit
import trplugins.menu.module.internal.hook.HookPlugin
import trplugins.menu.util.ReflexHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Arasple
 * @date 2021/1/27 14:05
 */
object Heads {

    private const val USER_API = "https://api.mojang.com/users/profiles/minecraft/"
    private const val SESSION_API = "https://sessionserver.mojang.com/session/minecraft/profile/"

    var headConnectTimeout: Int = 500
    var headReadTimeout: Int = 2500

    private val JSON_PARSER = JsonParser()
    private val DEFAULT_HEAD = XMaterial.PLAYER_HEAD.parseItem()!!.apply {
        if (runCatching { Material.PLAYER_HEAD }.isFailure) {
            durability = 3
        }
    }
    private val CACHED_SKULLS = mutableMapOf<String, ItemStack>()
    private val VALUE = if (MinecraftVersion.major >= 1.20) "value" else "getValue"
    private val NAME = if (MinecraftVersion.major >= 1.20) "name" else "getName"
    private val USE_PROFILE = ReflexHelper.hasMethod(OfflinePlayer::class.java, listOf("getPlayerProfile"))

    // 异步材质缓存: 玩家名 -> 材质URL
    private val textureCache = ConcurrentHashMap<String, String>()
    // 正在异步加载中的玩家名集合，防止重复请求
    private val loading = ConcurrentHashMap.newKeySet<String>()
    // 获取失败计数: 玩家名 -> 失败次数
    private val failedCount = ConcurrentHashMap<String, Int>()
    // 失败超过3次后加入黑名单，不再请求
    private val blacklisted = ConcurrentHashMap.newKeySet<String>()
    private const val MAX_RETRIES = 3

    fun cacheSize(): Int {
        return CACHED_SKULLS.size
    }

    fun getHead(id: String): ItemStack {
        return if (id.length <= 20) {
            getPlayerHead(id)
        } else if (id.length == 32) {
            getPlayerHead(UUID.fromString(StringBuilder(id)
                .insert(20, '-').insert(16, '-').insert(12, '-').insert(8, '-')
                .toString()))
        } else if (id.length == 36) {
            getPlayerHead(UUID.fromString(id))
        } else {
            getCustomHead(id)
        }
    }

    private fun getCustomHead(id: String): ItemStack = CACHED_SKULLS.computeIfAbsent(id) {
        if (id.startsWith("http://textures.minecraft.net/texture/")) {
            BukkitSkull.applySkull(id.substring(38))
        } else {
            BukkitSkull.applySkull(id)
        }
    }.clone()

    private fun getPlayerHead(uniqueId: UUID): ItemStack {
        val player = Bukkit.getPlayer(uniqueId)
        if (player != null) {
            return getPlayerHead(player)
        }
        val name = Bukkit.getOfflinePlayer(uniqueId).name
        return if (name == null) DEFAULT_HEAD else getPlayerHead(name)
    }

    private fun getPlayerHead(name: String): ItemStack {
        if (HookPlugin.getSkinsRestorer().isHooked) {
            val texture: String? = HookPlugin.getSkinsRestorer().getPlayerSkinTexture(name)
            return texture?.let { getCustomHead(it) } ?: DEFAULT_HEAD
        }
        val player = Bukkit.getPlayer(name)
        if (player != null) {
            return getPlayerHead(player)
        }
        val texture = seekTextureAsync(name)
        return if (texture == null) DEFAULT_HEAD else getCustomHead(texture)
    }

    private fun getPlayerHead(player: Player): ItemStack {
        if (USE_PROFILE) {
            return getCustomHead(player.playerProfile.textures.skin.toString())
        } else {
            val profile = player.invokeMethod<GameProfile>("getProfile")
            profile?.properties?.get("textures")?.forEach { texture ->
                if (texture != null) {
                    return getCustomHead(texture.getProperty<String>(VALUE)!!)
                }
            }
            val texture = seekTextureAsync(player.name)
            return if (texture == null) DEFAULT_HEAD else getCustomHead(texture)
        }
    }

    fun seekTexture(itemStack: ItemStack): String? {
        val meta = itemStack.itemMeta ?: return null

        if (meta is SkullMeta) {
            meta.owningPlayer?.name?.let { return it }
        }

        val profileValue = meta.getProperty<Any>("profile") ?: return null

        val gameProfile: GameProfile? = if (profileValue is GameProfile) {
            profileValue
        } else {
            // Minecraft 1.21+ 将 profile 字段改为 ResolvableProfile
            val optional = runCatching { profileValue.invokeMethod<Any>("gameProfile") }.getOrNull()
            runCatching { optional?.invokeMethod<GameProfile>("orElse", null) }.getOrNull()
        }

        gameProfile?.properties?.values()?.forEach {
            if (it.getProperty<String>(NAME) == "textures") return it.getProperty<String>(VALUE)
        }
        return null
    }

    /**
     * 非阻塞获取材质：返回缓存值或 null，并在后台异步请求 Mojang API
     * 首次调用返回 null（显示默认头颅），下次菜单刷新时从缓存获取
     * 失败超过 3 次后将该名称加入黑名单，不再请求
     */
    private fun seekTextureAsync(name: String): String? {
        val key = name.lowercase()
        // 已缓存直接返回
        textureCache[key]?.let { return it }
        // 已被黑名单标记，不再请求
        if (key in blacklisted) return null
        // 未在加载中则发起异步请求
        if (loading.add(key)) {
            submit(async = true) {
                try {
                    val texture = fetchTexture(name)
                    if (texture != null) {
                        textureCache[key] = texture
                        failedCount.remove(key)
                    } else {
                        recordFailure(key)
                    }
                } catch (_: Exception) {
                    recordFailure(key)
                } finally {
                    loading.remove(key)
                }
            }
        }
        return null
    }

    private fun recordFailure(key: String) {
        val count = failedCount.merge(key, 1) { old, inc -> old + inc } ?: 1
        if (count >= MAX_RETRIES) {
            blacklisted.add(key)
            failedCount.remove(key)
        }
    }

    /**
     * 实际执行 HTTP 请求获取材质（仅在异步线程调用）
     */
    private fun fetchTexture(name: String): String? {
        val user = urlJson(USER_API + name)
        if (user != null && user.has("id")) {
            val uuid = user["id"].asString
            val session = urlJson(SESSION_API + uuid)
            if (session != null) {
                for (element in session.getAsJsonArray("properties")) {
                    val property = element.asJsonObject
                    if (property["name"].asString == "textures") {
                        val value = property["value"].asString
                        val texture = JSON_PARSER.parse(String(Base64.getDecoder().decode(value))).asJsonObject
                        if (texture != null) {
                            return texture["textures"].asJsonObject["SKIN"].asJsonObject["url"].asString
                        }
                    }
                }
            }
        }
        return null
    }

    fun seekTexture(name: String): String? {
        return textureCache[name.lowercase()]
    }

    private fun urlJson(url: String): JsonObject? {
        val text = urlText(url)
        return if (text.trim { it <= ' ' }.isEmpty()) {
            null
        } else {
            JSON_PARSER.parse(text).asJsonObject
        }
    }

    private fun urlText(url: String): String {
        try {
            val con = URL(url).openConnection()
            // Java 8 require user agent
            con.connectTimeout = headConnectTimeout
            con.readTimeout = headReadTimeout
            con.addRequestProperty("User-Agent", "Mozilla/5.0")
            con.getInputStream().use { `in` ->
                BufferedReader(InputStreamReader(`in`)).use { reader ->
                    val out = java.lang.StringBuilder()
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        out.append(line)
                    }
                    return out.toString()
                }
            }
        } catch (e: Exception) {
            return ""
        }
    }
}