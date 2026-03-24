package trplugins.menu.util

import taboolib.common.io.runningClasses
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import java.util.function.Consumer

object ClassUtils {
    @JvmStatic
    val staticClass: Class<*> by lazy {
        val className = if (System.getProperty("java.version").contains("1.8.")) {
            "jdk.internal.dynalink.beans.StaticClass"
        } else {
            "jdk.dynalink.beans.StaticClass"
        }
        ReflexHelper.requireClass(className)
    }

    @JvmStatic
    fun staticClass(className: String): Any? {
        val owner = ReflexHelper.classOrNull(className) ?: return null
        return runCatching {
            staticClass.invokeMethod<Any?>("forClass", owner, isStatic = true)
        }.getOrNull()
    }

    @JvmStatic
    fun <T> subClasses(`super`: Class<T>, consumer: Consumer<Class<out T>>) {
        runningClasses.forEach { `class` ->
            if (`class`.structure.isAbstract) return@forEach
            if (`class`.structure.superclass?.name != `super`.name) return@forEach

            consumer.accept(`class`.structure.owner.instance!!.asSubclass(`super`))
        }
    }
}