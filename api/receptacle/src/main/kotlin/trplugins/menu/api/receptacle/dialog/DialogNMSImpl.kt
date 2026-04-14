package trplugins.menu.api.receptacle.dialog

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket
import trplugins.menu.util.ReflexHelper
import java.util.Locale
import java.util.Optional
import kotlin.math.max
import kotlin.math.min

class DialogNMSImpl : DialogNMS() {

    private val requiredClasses = listOf(
        CLIENTBOUND_SHOW_DIALOG_PACKET_CLASS_NAME,
        CLIENTBOUND_CLEAR_DIALOG_PACKET_CLASS_NAME,
        SERVERBOUND_CUSTOM_CLICK_ACTION_PACKET_CLASS_NAME,
        MULTI_ACTION_DIALOG_CLASS_NAME,
        COMMON_DIALOG_DATA_CLASS_NAME,
        CUSTOM_ALL_ACTION_CLASS_NAME,
        PLAIN_MESSAGE_CLASS_NAME,
        ITEM_BODY_CLASS_NAME,
        INPUT_CLASS_NAME,
        TEXT_INPUT_CLASS_NAME,
        BOOLEAN_INPUT_CLASS_NAME,
        SINGLE_OPTION_INPUT_CLASS_NAME,
        NUMBER_RANGE_INPUT_CLASS_NAME,
        HOLDER_CLASS_NAME
    )

    private val resolvedDialogKeyClass by lazy(LazyThreadSafetyMode.NONE) {
        ReflexHelper.resolveFirstClassOrNull(DIALOG_KEY_CLASS_NAMES)
    }

    private val resolvedChatComponentClass by lazy(LazyThreadSafetyMode.NONE) {
        ReflexHelper.resolveFirstClassOrNull(CHAT_COMPONENT_CLASS_NAMES)
    }

    private val resolvedSingleOptionEntryClass by lazy(LazyThreadSafetyMode.NONE) {
        ReflexHelper.resolveFirstClassOrNull(SINGLE_OPTION_ENTRY_CLASS_NAMES)
    }

    private val resolvedNumberRangeInfoClass by lazy(LazyThreadSafetyMode.NONE) {
        ReflexHelper.resolveFirstClassOrNull(NUMBER_RANGE_INFO_CLASS_NAMES)
    }

    private val clearDialogPacketInstance by lazy(LazyThreadSafetyMode.NONE) {
        resolveClearDialogPacketInstance()
    }

    private val dialogsSupported by lazy(LazyThreadSafetyMode.NONE) {
        MinecraftVersion.majorLegacy >= 12106 &&
            requiredClasses.all(::classExists) &&
            resolvedDialogKeyClass != null &&
            resolvedChatComponentClass != null &&
            canCreateDialogKey()
    }

    override fun supportsDialogs(): Boolean {
        return dialogsSupported
    }

    override fun open(player: Player, payload: DialogPayload) {
        check(supportsDialogs()) { "Dialogs NMS bridge is unavailable on the current runtime." }
        val dialog = buildDialog(payload)
        val holder = holderDirect(dialog)
        val packet = newInstance(CLIENTBOUND_SHOW_DIALOG_PACKET_CLASS_NAME, holder)
        player.sendPacket(packet)
    }

    override fun close(player: Player) {
        if (!supportsDialogs()) {
            return
        }
        val packet = clearDialogPacketInstance ?: return
        player.sendPacket(packet)
    }

    override fun parseResponse(packet: Any): DialogResponseData? {
        if (packet.javaClass.name != SERVERBOUND_CUSTOM_CLICK_ACTION_PACKET_CLASS_NAME) {
            return null
        }
        val id = readRecordComponent(packet, "id", 0) ?: return null
        val namespace = dialogKeyNamespace(id) ?: return null
        val path = dialogKeyPath(id) ?: return null
        if (namespace != ACTION_NAMESPACE || !path.startsWith("$ACTION_PREFIX/")) {
            return null
        }
        val payloadOptional = readRecordComponent(packet, "payload", 1) as? Optional<*>
        val payloadData = payloadOptional?.orElse(null)
        val values = (payloadData?.let(::decodeTag) as? Map<*, *>)
            ?.filterKeys { it is String && !it.startsWith(INTERNAL_PREFIX) }
            ?.mapKeys { it.key.toString() }
            ?: emptyMap()
        return DialogResponseData(
            actionId = path.substringAfterLast('/'),
            closeAction = path.endsWith("/__close__"),
            values = values
        )
    }

    private fun buildDialog(payload: DialogPayload): Any {
        val common = buildCommonDialogData(payload)
        val actions = if (payload.actions.isEmpty() && payload.exitAction == null) {
            listOf(DialogActionPayload("__default_close__", "Close", 120, false))
        } else {
            payload.actions
        }
        val actionButtons = actions.map { buildActionButton(payload, it) }
        val exitAction = payload.exitAction?.let { buildActionButton(payload, it) }
        return when (payload.screenType.uppercase(Locale.ROOT)) {
            "NOTICE" -> buildNoticeDialog(common, actionButtons, exitAction) ?: buildMultiActionDialog(common, actionButtons, exitAction)
            "CONFIRMATION" -> buildConfirmationDialog(common, actionButtons, exitAction) ?: buildMultiActionDialog(common, actionButtons, exitAction)
            else -> buildMultiActionDialog(common, actionButtons, exitAction)
        }
    }

    private fun buildNoticeDialog(common: Any, actions: List<Any>, exitAction: Any?): Any? {
        if (actions.size != 1 || exitAction != null) {
            return null
        }
        return newInstance(
            NOTICE_DIALOG_CLASS_NAME,
            common,
            actions.first()
        )
    }

    private fun buildConfirmationDialog(common: Any, actions: List<Any>, exitAction: Any?): Any? {
        if (actions.size != 2 || exitAction != null) {
            return null
        }
        return newInstance(
            CONFIRMATION_DIALOG_CLASS_NAME,
            common,
            actions[0],
            actions[1]
        )
    }

    private fun buildMultiActionDialog(common: Any, actions: List<Any>, exitAction: Any?): Any {
        val visibleButtons = actions.size + if (exitAction != null) 1 else 0
        val columns = min(3, max(1, if (visibleButtons >= 2) 2 else 1))
        return newInstance(
            MULTI_ACTION_DIALOG_CLASS_NAME,
            common,
            actions,
            Optional.ofNullable(exitAction),
            columns
        )
    }

    private fun buildCommonDialogData(payload: DialogPayload): Any {
        val body = mutableListOf<Any>()
        val inputs = mutableListOf<Any>()
        payload.body.forEach { element ->
            when (element.type.lowercase(Locale.ROOT)) {
                "plain_message", "text", "message" -> body += buildPlainMessage(element)
                "item" -> body += buildItemBody(element)
                "input" -> inputs += buildTextInput(element)
                "boolean" -> inputs += buildBooleanInput(element)
                "single_option", "single-option", "select" -> inputs += buildSingleOptionInput(element)
                "number_range", "number-range", "range" -> inputs += buildNumberRangeInput(element)
                "multi_option", "multi-option" -> throw UnsupportedOperationException("Dialogs multi_option is not supported by the current NMS bridge.")
                else -> throw UnsupportedOperationException("Unsupported dialogs element type: ${element.type}")
            }
        }
        val effectiveAllowEscClose = payload.allowEscClose && payload.exitAction == null
        return newInstance(
            COMMON_DIALOG_DATA_CLASS_NAME,
            component(payload.title ?: payload.externalTitle ?: payload.menuId),
            optionalComponent(payload.externalTitle),
            effectiveAllowEscClose,
            false,
            dialogAction("NONE"),
            body,
            inputs
        )
    }

    private fun buildActionButton(payload: DialogPayload, action: DialogActionPayload): Any {
        val button = newInstance(
            COMMON_BUTTON_DATA_CLASS_NAME,
            component(action.label),
            Optional.empty<Any>(),
            action.width ?: DEFAULT_BUTTON_WIDTH
        )
        val customAction = newInstance(
            CUSTOM_ALL_ACTION_CLASS_NAME,
            actionKey(payload, action.id),
            Optional.empty<Any>()
        )
        return newInstance(
            ACTION_BUTTON_CLASS_NAME,
            button,
            Optional.of(customAction)
        )
    }

    private fun buildPlainMessage(element: DialogElementPayload): Any {
        val text = if (element.text.isEmpty()) element.label.orEmpty() else element.text.joinToString("\n")
        return newInstance(
            PLAIN_MESSAGE_CLASS_NAME,
            component(text),
            element.width ?: DEFAULT_TEXT_WIDTH
        )
    }

    private fun buildItemBody(element: DialogElementPayload): Any {
        val item = toNmsItem(element.item ?: ItemStack(org.bukkit.Material.AIR))
        return newInstance(
            ITEM_BODY_CLASS_NAME,
            item,
            Optional.empty<Any>(),
            true,
            true,
            element.width ?: DEFAULT_ITEM_SIZE,
            DEFAULT_ITEM_SIZE
        )
    }

    private fun buildTextInput(element: DialogElementPayload): Any {
        val control = newInstance(
            TEXT_INPUT_CLASS_NAME,
            element.width ?: DEFAULT_INPUT_WIDTH,
            component(element.label ?: element.id),
            true,
            element.value ?: "",
            element.maxLength ?: DEFAULT_MAX_LENGTH,
            Optional.empty<Any>()
        )
        return newInstance(
            INPUT_CLASS_NAME,
            element.id,
            control
        )
    }

    private fun buildBooleanInput(element: DialogElementPayload): Any {
        val control = newInstance(
            BOOLEAN_INPUT_CLASS_NAME,
            component(element.label ?: element.id),
            element.boolValue ?: false,
            "true",
            "false"
        )
        return newInstance(
            INPUT_CLASS_NAME,
            element.id,
            control
        )
    }

    private fun buildSingleOptionInput(element: DialogElementPayload): Any {
        require(element.options.isNotEmpty()) { "Dialog single_option ${element.id} requires at least one option." }
        val entryClass = singleOptionEntryClass()
        val defaultValue = element.value
        val entries = element.options.map { option ->
            newInstance(
                entryClass,
                option.id,
                optionalComponent(option.title),
                option.id == defaultValue
            )
        }
        val control = newInstance(
            SINGLE_OPTION_INPUT_CLASS_NAME,
            element.width ?: DEFAULT_INPUT_WIDTH,
            entries,
            component(element.label ?: element.id),
            true
        )
        return newInstance(
            INPUT_CLASS_NAME,
            element.id,
            control
        )
    }

    private fun buildNumberRangeInput(element: DialogElementPayload): Any {
        val initial = element.value?.toFloatOrNull()
        val rangeInfoClass = numberRangeInfoClass()
        val rangeInfo = newInstance(
            rangeInfoClass,
            (element.min ?: 0.0).toFloat(),
            (element.max ?: 1.0).toFloat(),
            Optional.ofNullable(initial),
            Optional.ofNullable(element.step?.toFloat())
        )
        val control = newInstance(
            NUMBER_RANGE_INPUT_CLASS_NAME,
            element.width ?: DEFAULT_INPUT_WIDTH,
            component(element.label ?: element.id),
            "%s",
            rangeInfo
        )
        return newInstance(
            INPUT_CLASS_NAME,
            element.id,
            control
        )
    }

    private fun component(text: String): Any {
        val raw = text.ifBlank { " " }
        val craftChatMessage = runCatching { craftBukkitClass("util.CraftChatMessage") }.getOrNull()
        if (craftChatMessage != null) {
            invokeStaticAliasOrNull(craftChatMessage, CRAFT_CHAT_MESSAGE_PARSE_METHOD_NAMES, raw)
                ?.let { return it }
        }
        return invokeStaticAlias(
            chatComponentClass(),
            COMPONENT_LITERAL_METHOD_NAMES,
            raw
        )
    }

    private fun optionalComponent(text: String?): Optional<Any> {
        return Optional.ofNullable(text?.takeIf { it.isNotBlank() }?.let(::component))
    }

    private fun toNmsItem(item: ItemStack): Any {
        val craftItemStack = craftBukkitClass("inventory.CraftItemStack")
        return invokeStatic(craftItemStack, "asNMSCopy", item)
    }

    private fun holderDirect(value: Any): Any {
        return invokeStatic(requiredClass(HOLDER_CLASS_NAME), "direct", value)
    }

    private fun actionKey(payload: DialogPayload, actionId: String): Any {
        val path = listOf(
            ACTION_PREFIX,
            sanitizeId(payload.menuId),
            sanitizeId(payload.pageId),
            sanitizeId(actionId)
        ).joinToString("/")
        return createDialogKey(ACTION_NAMESPACE, path)
    }

    private fun createDialogKey(namespace: String, path: String): Any {
        val keyClass = dialogKeyClass()
        invokeStaticAliasOrNull(keyClass, DIALOG_KEY_FACTORY_METHOD_NAMES, namespace, path)
            ?.let { return it }
        ReflexHelper.invokeConstructorOrNull(keyClass, namespace, path)
            ?.let { return it }
        val fullPath = "$namespace:$path"
        invokeStaticAliasOrNull(keyClass, DIALOG_KEY_PARSER_METHOD_NAMES, fullPath)
            ?.let { return it }
        throw NoSuchMethodException("Unable to construct dialog key for ${keyClass.name}")
    }

    private fun dialogKeyNamespace(key: Any): String? {
        (ReflexHelper.invokeMethodAliasOrNull(key, DIALOG_KEY_NAMESPACE_METHOD_NAMES) as? String)?.let { return it }
        (ReflexHelper.getPropertyAliasOrNull(key, DIALOG_KEY_NAMESPACE_FIELD_NAMES) as? String)?.let { return it }
        return parseDialogKeyString(key.toString())?.first
    }

    private fun dialogKeyPath(key: Any): String? {
        (ReflexHelper.invokeMethodAliasOrNull(key, DIALOG_KEY_PATH_METHOD_NAMES) as? String)?.let { return it }
        (ReflexHelper.getPropertyAliasOrNull(key, DIALOG_KEY_PATH_FIELD_NAMES) as? String)?.let { return it }
        return parseDialogKeyString(key.toString())?.second
    }

    private fun parseDialogKeyString(raw: String): Pair<String, String>? {
        val separatorIndex = raw.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex >= raw.lastIndex) {
            return null
        }
        return raw.substring(0, separatorIndex) to raw.substring(separatorIndex + 1)
    }

    private fun dialogAction(name: String): Any {
        return readStaticProperty(requiredClass(DIALOG_ACTION_CLASS_NAME), name)
    }

    private fun decodeTag(tag: Any): Any? {
        callOptional(tag, "asString")?.let { return it }
        callOptional(tag, "asBoolean")?.let { return it }
        callOptional(tag, "asInt")?.let { return it }
        callOptional(tag, "asLong")?.let { return it }
        callOptional(tag, "asFloat")?.let { return it }
        callOptional(tag, "asDouble")?.let { return it }
        callOptional(tag, "asCompound")?.let { compound ->
            val entries = compound.invokeMethod<Set<*>>("entrySet") ?: return emptyMap<String, Any?>()
            return entries.associate { entry ->
                val mapEntry = entry as Map.Entry<*, *>
                mapEntry.key.toString() to decodeTag(mapEntry.value!!)
            }
        }
        callOptional(tag, "asList")?.let { list ->
            val size = list.invokeMethod<Int>("size") ?: 0
            return (0 until size).map { index ->
                decodeTag(list.invokeMethod<Any?>("get", index)!!)
            }
        }
        return tag.toString()
    }

    private fun callOptional(instance: Any, methodName: String): Any? {
        val result = runCatching { instance.invokeMethod<Optional<*>>(methodName) }.getOrNull() ?: return null
        return result.orElse(null)
    }

    private fun readRecordComponent(instance: Any, preferredName: String, index: Int): Any? {
        runCatching { instance.invokeMethod<Any?>(preferredName) }.getOrNull()?.let { return it }
        runCatching { instance.getProperty<Any?>(preferredName) }.getOrNull()?.let { return it }
        val field = ReflexHelper.reflexClass(instance.javaClass).structure.fields
            .filterNot { it.isStatic }
            .getOrNull(index)
            ?: return null
        return runCatching { field.get(instance) }.getOrNull()
    }

    private fun dialogKeyClass(): Class<*> {
        return resolvedDialogKeyClass
            ?: throw ClassNotFoundException("Unable to resolve dialogs key class from: ${DIALOG_KEY_CLASS_NAMES.joinToString()}")
    }

    private fun chatComponentClass(): Class<*> {
        return resolvedChatComponentClass
            ?: throw ClassNotFoundException("Unable to resolve chat component class from: ${CHAT_COMPONENT_CLASS_NAMES.joinToString()}")
    }

    private fun singleOptionEntryClass(): Class<*> {
        return resolvedSingleOptionEntryClass
            ?: throw ClassNotFoundException("Unable to resolve single option entry class from: ${SINGLE_OPTION_ENTRY_CLASS_NAMES.joinToString()}")
    }

    private fun numberRangeInfoClass(): Class<*> {
        return resolvedNumberRangeInfoClass
            ?: throw ClassNotFoundException("Unable to resolve number range info class from: ${NUMBER_RANGE_INFO_CLASS_NAMES.joinToString()}")
    }

    private fun resolveClearDialogPacketInstance(): Any? {
        val packetClass = ReflexHelper.classOrNull(CLIENTBOUND_CLEAR_DIALOG_PACKET_CLASS_NAME) ?: return null
        ReflexHelper.getStaticPropertyByTypeOrNull(packetClass, packetClass)?.let { return it }
        return ReflexHelper.invokeConstructorOrNull(packetClass)
    }

    private fun canCreateDialogKey(): Boolean {
        val keyClass = resolvedDialogKeyClass ?: return false
        return ReflexHelper.hasMethod(keyClass, DIALOG_KEY_FACTORY_METHOD_NAMES, staticOnly = true, String::class.java, String::class.java) ||
            ReflexHelper.hasConstructor(keyClass, String::class.java, String::class.java) ||
            ReflexHelper.hasMethod(keyClass, DIALOG_KEY_PARSER_METHOD_NAMES, staticOnly = true, String::class.java)
    }

    private fun classExists(className: String): Boolean {
        return ReflexHelper.classOrNull(className) != null
    }

    private fun requiredClass(className: String): Class<*> {
        return ReflexHelper.requireClass(className)
    }

    private fun readStaticProperty(owner: Class<*>, propertyName: String): Any {
        return ReflexHelper.getStaticPropertyAliasOrNull(owner, listOf(propertyName))
            ?: throw NoSuchFieldException("Field $propertyName not found in ${owner.name}")
    }

    private fun newInstance(className: String, vararg args: Any?): Any {
        return requiredClass(className).invokeConstructor(*args)
    }

    private fun newInstance(owner: Class<*>, vararg args: Any?): Any {
        return owner.invokeConstructor(*args)
    }

    private fun invokeStatic(owner: Class<*>, methodName: String, vararg args: Any?): Any {
        return owner.invokeMethod<Any?>(methodName, *args, isStatic = true)
            ?: throw NoSuchMethodException("Method $methodName not found in ${owner.name}")
    }

    private fun invokeStaticAlias(owner: Class<*>, methodNames: List<String>, vararg args: Any?): Any {
        return invokeStaticAliasOrNull(owner, methodNames, *args)
            ?: throw NoSuchMethodException("Method ${methodNames.joinToString("/")} not found in ${owner.name}")
    }

    private fun invokeStaticAliasOrNull(owner: Class<*>, methodNames: List<String>, vararg args: Any?): Any? {
        return ReflexHelper.invokeStaticMethodAliasOrNull(owner, methodNames, *args)
    }

    private fun sanitizeId(value: String): String {
        return value.lowercase(Locale.ROOT).replace(ID_SANITIZE, "_").trim('_').ifBlank { "unnamed" }
    }

    private fun craftBukkitClass(path: String): Class<*> {
        val versioned = Bukkit.getServer()::class.java.`package`.name
        return ReflexHelper.classOrNull("org.bukkit.craftbukkit.$path")
            ?: ReflexHelper.classOrNull("$versioned.$path")
            ?: throw ClassNotFoundException("Unable to resolve CraftBukkit class: $path")
    }

    companion object {
        private const val ACTION_NAMESPACE = "trmenu"
        private const val ACTION_PREFIX = "dialog"
        private const val INTERNAL_PREFIX = "_trmenu_"
        private const val DEFAULT_TEXT_WIDTH = 320
        private const val DEFAULT_INPUT_WIDTH = 320
        private const val DEFAULT_BUTTON_WIDTH = 160
        private const val DEFAULT_ITEM_SIZE = 16
        private const val DEFAULT_MAX_LENGTH = 64

        private const val CLIENTBOUND_SHOW_DIALOG_PACKET_CLASS_NAME = "net.minecraft.network.protocol.common.ClientboundShowDialogPacket"
        private const val CLIENTBOUND_CLEAR_DIALOG_PACKET_CLASS_NAME = "net.minecraft.network.protocol.common.ClientboundClearDialogPacket"
        private const val SERVERBOUND_CUSTOM_CLICK_ACTION_PACKET_CLASS_NAME = "net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket"
        private const val HOLDER_CLASS_NAME = "net.minecraft.core.Holder"
        private const val NMS_ITEM_STACK_CLASS_NAME = "net.minecraft.world.item.ItemStack"
        private const val DIALOG_ACTION_CLASS_NAME = "net.minecraft.server.dialog.DialogAction"
        private const val COMMON_DIALOG_DATA_CLASS_NAME = "net.minecraft.server.dialog.CommonDialogData"
        private const val COMMON_BUTTON_DATA_CLASS_NAME = "net.minecraft.server.dialog.CommonButtonData"
        private const val ACTION_BUTTON_CLASS_NAME = "net.minecraft.server.dialog.ActionButton"
        private const val NOTICE_DIALOG_CLASS_NAME = "net.minecraft.server.dialog.NoticeDialog"
        private const val CONFIRMATION_DIALOG_CLASS_NAME = "net.minecraft.server.dialog.ConfirmationDialog"
        private const val MULTI_ACTION_DIALOG_CLASS_NAME = "net.minecraft.server.dialog.MultiActionDialog"
        private const val CUSTOM_ALL_ACTION_CLASS_NAME = "net.minecraft.server.dialog.action.CustomAll"
        private const val PLAIN_MESSAGE_CLASS_NAME = "net.minecraft.server.dialog.body.PlainMessage"
        private const val ITEM_BODY_CLASS_NAME = "net.minecraft.server.dialog.body.ItemBody"
        private const val INPUT_CLASS_NAME = "net.minecraft.server.dialog.Input"
        private const val INPUT_CONTROL_CLASS_NAME = "net.minecraft.server.dialog.input.InputControl"
        private const val TEXT_INPUT_CLASS_NAME = "net.minecraft.server.dialog.input.TextInput"
        private const val BOOLEAN_INPUT_CLASS_NAME = "net.minecraft.server.dialog.input.BooleanInput"
        private const val SINGLE_OPTION_INPUT_CLASS_NAME = "net.minecraft.server.dialog.input.SingleOptionInput"
        private const val NUMBER_RANGE_INPUT_CLASS_NAME = "net.minecraft.server.dialog.input.NumberRangeInput"

        private val CHAT_COMPONENT_CLASS_NAMES = listOf(
            "net.minecraft.network.chat.Component",
            "net.minecraft.network.chat.IChatBaseComponent"
        )

        private val DIALOG_KEY_CLASS_NAMES = listOf(
            "net.minecraft.resources.Identifier",
            "net.minecraft.resources.ResourceLocation",
            "net.minecraft.resources.MinecraftKey"
        )

        private val SINGLE_OPTION_ENTRY_CLASS_NAMES = listOf(
            "net.minecraft.server.dialog.input.SingleOptionInput\$Entry",
            "net.minecraft.server.dialog.input.SingleOptionInput\$a"
        )

        private val NUMBER_RANGE_INFO_CLASS_NAMES = listOf(
            "net.minecraft.server.dialog.input.NumberRangeInput\$RangeInfo",
            "net.minecraft.server.dialog.input.NumberRangeInput\$a"
        )

        private val COMPONENT_LITERAL_METHOD_NAMES = listOf(
            "literal",
            "of"
        )

        private val CRAFT_CHAT_MESSAGE_PARSE_METHOD_NAMES = listOf(
            "fromJSONOrString",
            "fromStringOrNull"
        )

        private val DIALOG_KEY_FACTORY_METHOD_NAMES = listOf(
            "fromNamespaceAndPath",
            "tryBuild"
        )

        private val DIALOG_KEY_PARSER_METHOD_NAMES = listOf(
            "parse",
            "tryParse"
        )

        private val DIALOG_KEY_NAMESPACE_METHOD_NAMES = listOf(
            "getNamespace",
            "namespace"
        )

        private val DIALOG_KEY_PATH_METHOD_NAMES = listOf(
            "getPath",
            "path"
        )

        private val DIALOG_KEY_NAMESPACE_FIELD_NAMES = listOf("namespace")
        private val DIALOG_KEY_PATH_FIELD_NAMES = listOf("path")
        private val ID_SANITIZE = Regex("[^a-z0-9/._-]")
    }
}
