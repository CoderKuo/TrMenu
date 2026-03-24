package trplugins.menu.util

import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.ReflexClass
import java.util.concurrent.ConcurrentHashMap

object ReflexHelper {

    private val classCache = ConcurrentHashMap<String, Class<*>>()
    private val reflexClassCache = ConcurrentHashMap<Class<*>, ReflexClass>()

    fun classOrNull(className: String): Class<*>? {
        classCache[className]?.let { return it }
        return runCatching { Class.forName(className) }.getOrNull()?.also {
            classCache.putIfAbsent(className, it)
        }
    }

    fun requireClass(className: String): Class<*> {
        return classOrNull(className) ?: throw ClassNotFoundException(className)
    }

    fun resolveFirstClassOrNull(classNames: Iterable<String>): Class<*>? {
        return classNames.firstNotNullOfOrNull(::classOrNull)
    }

    fun reflexClass(owner: Class<*>): ReflexClass {
        return reflexClassCache.computeIfAbsent(owner) { ReflexClass.of(it) }
    }

    fun hasMethod(owner: Class<*>, methodNames: Iterable<String>, staticOnly: Boolean = false, vararg parameterTypes: Class<*>): Boolean {
        val expectedTypes = parameterTypes.map(::wrap)
        return reflexClass(owner).structure.methods.any { method ->
            method.name in methodNames &&
                (!staticOnly || method.isStatic) &&
                matches(method.parameterTypes, expectedTypes)
        }
    }

    fun hasConstructor(owner: Class<*>, vararg parameterTypes: Class<*>): Boolean {
        val expectedTypes = parameterTypes.map(::wrap)
        return reflexClass(owner).structure.constructors.any { constructor ->
            matches(constructor.parameterTypes, expectedTypes)
        }
    }

    fun hasProperty(owner: Class<*>, propertyNames: Iterable<String>, staticOnly: Boolean = false): Boolean {
        return reflexClass(owner).structure.fields.any { field ->
            field.name in propertyNames && (!staticOnly || field.isStatic)
        }
    }

    fun invokeMethodAliasOrNull(instance: Any, methodNames: Iterable<String>, vararg args: Any?): Any? {
        for (methodName in methodNames) {
            val result = runCatching { instance.invokeMethod<Any?>(methodName, *args) }
            if (result.isSuccess) {
                return result.getOrNull()
            }
        }
        return null
    }

    fun invokeStaticMethodAliasOrNull(owner: Class<*>, methodNames: Iterable<String>, vararg args: Any?): Any? {
        for (methodName in methodNames) {
            val result = runCatching { owner.invokeMethod<Any?>(methodName, *args, isStatic = true) }
            if (result.isSuccess) {
                return result.getOrNull()
            }
        }
        return null
    }

    fun getPropertyAliasOrNull(instance: Any, propertyNames: Iterable<String>): Any? {
        for (propertyName in propertyNames) {
            val result = runCatching { instance.getProperty<Any?>(propertyName) }
            if (result.isSuccess) {
                return result.getOrNull()
            }
        }
        return null
    }

    fun getStaticPropertyAliasOrNull(owner: Class<*>, propertyNames: Iterable<String>): Any? {
        for (propertyName in propertyNames) {
            val result = runCatching { owner.getProperty<Any?>(propertyName, isStatic = true) }
            if (result.isSuccess) {
                return result.getOrNull()
            }
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun invokeConstructorOrNull(owner: Class<*>, vararg args: Any?): Any? {
        return runCatching { (owner as Class<Any>).invokeConstructor(*args) }.getOrNull()
    }

    fun getStaticPropertyByTypeOrNull(owner: Class<*>, propertyType: Class<*>): Any? {
        for (field in reflexClass(owner).structure.fields) {
            if (field.isStatic && propertyType.isAssignableFrom(field.fieldType)) {
                val result = runCatching { field.get(null) }
                if (result.isSuccess) {
                    return result.getOrNull()
                }
            }
        }
        return null
    }


    private fun matches(actualTypes: Array<out Class<*>>, expectedTypes: List<Class<*>>): Boolean {
        if (actualTypes.size != expectedTypes.size) {
            return false
        }
        return actualTypes.indices.all { index ->
            wrap(actualTypes[index]).isAssignableFrom(expectedTypes[index])
        }
    }

    private fun wrap(type: Class<*>): Class<*> {
        return when (type) {
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            java.lang.Byte.TYPE -> java.lang.Byte::class.java
            java.lang.Short.TYPE -> java.lang.Short::class.java
            java.lang.Integer.TYPE -> java.lang.Integer::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            java.lang.Float.TYPE -> java.lang.Float::class.java
            java.lang.Double.TYPE -> java.lang.Double::class.java
            java.lang.Character.TYPE -> java.lang.Character::class.java
            else -> type
        }
    }
}
