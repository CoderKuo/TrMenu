package trplugins.menu.module.internal.hook.impl

import net.skinsrestorer.api.SkinsRestorer
import net.skinsrestorer.api.SkinsRestorerProvider
import taboolib.common.platform.function.submit
import trplugins.menu.module.internal.hook.HookAbstract
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Arasple
 * @date 2021/1/27 14:12
 */
class HookSkinsRestorer : HookAbstract() {

    private val skinsRestorer: SkinsRestorer? =
        if (plugin != null && plugin!!.isEnabled) {
            runCatching { SkinsRestorerProvider.get() }.getOrNull()
        } else {
            null
        }

    override val isHooked by lazy {
        if (plugin?.isEnabled == false) return@lazy false
        return@lazy skinsRestorer != null
    }

    private val textureCache = ConcurrentHashMap<String, String>()
    private val loading = ConcurrentHashMap.newKeySet<String>()

    fun getPlayerSkinTexture(name: String): String? {
        val key = name.lowercase()
        textureCache[key]?.let {
            return it
        }

        skinsRestorer?.let {
            if (loading.add(key)) {
                submit(async = true) {
                    try {
                        val skinData = it.skinStorage.findOrCreateSkinData(name)
                        if (skinData.isPresent) {
                            textureCache[key] = skinData.get().property.value
                        }
                    } catch (_: Throwable) {
                    } finally {
                        loading.remove(key)
                    }
                }
            }
        }
        return null
    }

}
