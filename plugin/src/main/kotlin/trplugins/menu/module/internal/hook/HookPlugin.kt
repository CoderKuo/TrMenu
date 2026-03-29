package trplugins.menu.module.internal.hook

import taboolib.common.LifeCycle
import taboolib.common.platform.SkipTo
import taboolib.common.platform.function.console
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.module.lang.sendLang
import trplugins.menu.module.internal.hook.impl.*
import trplugins.menu.util.ClassUtils
import kotlin.reflect.KClass

/**
 * @author Arasple
 * @date 2021/1/26 22:04
 */
@SkipTo(LifeCycle.ENABLE)
object HookPlugin {

    fun printInfo() {
        registry.filter { it.isHooked }.forEach {
            console().sendLang("Plugin-Dependency-Hooked", it.name)
        }
    }

    private val registry by lazy {
        mutableListOf<HookAbstract>().also {
            ClassUtils.subClasses(HookAbstract::class.java) { hook ->
                it.add(hook.invokeConstructor())
            }
        }.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(clazz: Class<T>) = registry.find { it.javaClass == clazz } as T

    operator fun <T : Any> get(clazz: KClass<T>) = this[clazz.java]

    fun getHeadDatabase(): HookHeadDatabase {
        return get(HookHeadDatabase::class.java)
    }

    fun getOraxen(): HookOraxen {
        return get(HookOraxen::class.java)
    }

    fun getPlayerPoints(): HookPlayerPoints {
        return get(HookPlayerPoints::class.java)
    }

    fun getSkinsRestorer(): HookSkinsRestorer {
        return get(HookSkinsRestorer::class.java)
    }

    fun getItemsAdder(): HookItemsAdder {
        return get(HookItemsAdder::class.java)
    }

    fun getFloodgate(): HookFloodgate {
        return get(HookFloodgate::class.java)
    }

    fun getVault(): HookVault {
        return get(HookVault::class.java)
    }

    fun getFastScript(): HookFastScript {
        return get(HookFastScript::class.java)
    }

    fun getZaphkiel(): HookZaphkiel {
        return get(HookZaphkiel::class.java)
    }

    fun getSXItem(): HookSXItem {
        return get(HookSXItem::class.java)
    }

    fun getTriton(): HookTriton {
        return get(HookTriton::class.java)
    }

    fun getMMOItems(): HookMMOItems {
        return get(HookMMOItems::class.java)
    }

    fun getNBTAPI(): HookNBTAPI {
        return get(HookNBTAPI::class.java)
    }

    fun getMagicCosmetics(): HookMagicCosmetics {
        return get(HookMagicCosmetics::class.java)
    }

    fun getMagicGem(): HookMagicGem {
        return get(HookMagicGem::class.java)
    }

    fun getNeigeItem(): HookNeigeItems {
        return get(HookNeigeItems::class.java)
    }

    fun getMeowEco(): HookMeowEco {
        return get(HookMeowEco::class.java)
    }

    fun getEcoItem(): HookEcoItems {
        return get(HookEcoItems::class.java)
    }

    fun getHMCCosmetics(): HookHMCCosmetics {
        return get(HookHMCCosmetics::class.java)
    }

    fun getMythicMobs(): HookMythicMobs {
        return get(HookMythicMobs::class.java)
    }

    fun getAzureFlow(): HookAzureFlow {
        return get(HookAzureFlow::class.java)
    }

    fun getGraalvm(): HookGraalJS {
        return HookGraalJS()
    }

    fun getPxRpg(): HookPxRpg {
        return get(HookPxRpg::class.java)
    }

    fun getNexo(): HookNexo {
        return get(HookNexo::class.java)
    }

    fun getCraftEngine(): HookCraftEngine {
        return get(HookCraftEngine::class.java)
    }

    fun getNovaScript(): HookNovaScript {
        return get(HookNovaScript::class.java)
    }

}
