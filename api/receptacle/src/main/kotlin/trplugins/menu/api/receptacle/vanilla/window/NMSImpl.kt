package trplugins.menu.api.receptacle.vanilla.window

import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket
import net.minecraft.server.v1_16_R3.*
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.library.reflex.Reflex.Companion.unsafeInstance
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.nmsClass
import taboolib.module.nms.sendPacket
import taboolib.platform.util.isAir
import trplugins.menu.api.receptacle.vanilla.window.StaticInventory.inventoryView
import trplugins.menu.api.receptacle.vanilla.window.StaticInventory.staticInventory

/**
 * @author Arasple
 * @date 2020/12/4 21:25
 */
class NMSImpl : NMS() {

    private val version = MinecraftVersion.majorLegacy
    private val isUnobfuscated = MinecraftVersion.isUnobfuscated
    private val windowIds = HashMap<String, Int>()

    private val emptyItemStack: Any? = if (isUnobfuscated) {
        craftItemStackCopy(ItemStack(Material.AIR))
    } else {
        CraftItemStack.asNMSCopy(ItemStack(Material.AIR))
    }

    private val Player.windowId get() = windowIds[this.name] ?: 119

    override fun windowId(player: Player, create: Boolean): Int {
        if (createWindowId() && create) {
            val id = player.getProperty<Int>("entity/containerCounter")!! + 1
            player.setProperty("entity/containerCounter", id)
            windowIds[player.name] = id
        }
        return player.windowId
    }

    override fun sendWindowsClose(player: Player, windowId: Int) {
        if (player.useStaticInventory()) {
            StaticInventory.close(player)
        } else {
            windowIds.remove(player.name)
            if (isUnobfuscated) {
                val packet = nmsClass("network.protocol.game.ClientboundContainerClosePacket")
                    .getDeclaredConstructor(Int::class.java).newInstance(windowId)
                player.sendPacket(packet)
            } else {
                player.sendPacket(PacketPlayOutCloseWindow(windowId))
            }
        }
    }

    override fun sendWindowsItems(player: Player, windowId: Int, items: Array<ItemStack?>) {
        when {
            player.useStaticInventory() -> {
                val inventory = player.staticInventory!!
                items.forEachIndexed { index, item ->
                    if (index >= inventory.size) {
                        return
                    }
                    inventory.setItem(index, item)
                }
            }
            isUnobfuscated -> {
                sendPacket(
                    player,
                    nmsClass("network.protocol.game.ClientboundContainerSetContentPacket").unsafeInstance(),
                    "containerId" to windowId,
                    "stateId" to 1,
                    "items" to items.map { i -> toNMSCopy(i) }.toList(),
                    "carriedItem" to emptyItemStack
                )
            }
            version >= 11701 -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "stateId" to 1,
                    "items" to items.map { i -> toNMSCopy(i) }.toList(),
                    "carriedItem" to emptyItemStack
                )
            }
            version >= 11700 -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "items" to items.map { i -> toNMSCopy(i) }.toList()
                )
            }
            version >= 11000 -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "a" to windowId,
                    "b" to items.map { i -> toNMSCopy(i) }.toList()
                )
            }
            else -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "a" to windowId,
                    "b" to items.map { i -> toNMSCopy(i) }.toTypedArray()
                )
            }
        }
    }

    override fun sendWindowsOpen(player: Player, windowId: Int, type: WindowLayout, title: String) {
        when {
            player.useStaticInventory() -> {
                StaticInventory.open(player, type, title)
            }
            isUnobfuscated -> {
                val menuType = nmsClass("world.inventory.MenuType").getProperty<Any>(type.vanillaId, isStatic = true)
                val chatComponent = craftChatMessageComponent(title)
                sendPacket(
                    player,
                    nmsClass("network.protocol.game.ClientboundOpenScreenPacket").unsafeInstance(),
                    "containerId" to windowId,
                    "type" to menuType,
                    "title" to chatComponent
                )
            }
            version >= 11900 -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "type" to Containers::class.java.getProperty(type.vanillaId, true),
                    "title" to CraftChatMessage.fromJSONOrString(title)
                )
            }
            MinecraftVersion.isUniversal -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "type" to type.serialId,
                    "title" to CraftChatMessage.fromJSONOrString(title)
                )
            }
            version >= 11400 -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow(),
                    "a" to windowId,
                    "b" to type.serialId,
                    "c" to CraftChatMessage.fromStringOrNull(title)
                )
            }
            else -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow(),
                    "a" to windowId,
                    "b" to type.id,
                    "c" to ChatComponentText(title),
                    "d" to type.containerSize - 1 // Fixed ViaVersion can not view 6x9 menu bug.
                )
            }
        }
    }

    override fun sendWindowsSetSlot(player: Player, windowId: Int, slot: Int, itemStack: ItemStack?, stateId: Int) {
        when {
            player.useStaticInventory() -> {
                if (windowId == -1 && slot == -1) {
                    player.itemOnCursor.type = Material.AIR
                } else {
                    val inventory = player.staticInventory!!
                    if (slot >= 0 && slot < inventory.size) {
                        inventory.setItem(slot, itemStack)
                    }
                }
            }
            isUnobfuscated -> {
                sendPacket(
                    player,
                    nmsClass("network.protocol.game.ClientboundContainerSetSlotPacket").unsafeInstance(),
                    "containerId" to windowId,
                    "stateId" to -1,
                    "slot" to slot,
                    "itemStack" to toNMSCopy(itemStack)
                )
            }
            version >= 11701 -> {
                sendPacket(
                    player,
                    PacketPlayOutSetSlot::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "stateId" to -1,
                    "slot" to slot,
                    "itemStack" to toNMSCopy(itemStack)
                )
            }
            else -> {
                player.sendPacket(PacketPlayOutSetSlot(windowId, slot, toNMSCopy(itemStack) as? net.minecraft.server.v1_16_R3.ItemStack))
            }
        }
        if (version >= 12104) {
            try {
                sendPacket(
                    player,
                    ClientboundSetCursorItemPacket::class.java.unsafeInstance(),
                    "contents" to toNMSCopy(null)
                )
            } catch (_: Throwable) {}
        }
    }

    override fun sendWindowsUpdateData(player: Player, windowId: Int, id: Int, value: Int) {
        when {
            player.useStaticInventory() -> {
                val inventory = player.staticInventory!!
                val view = player.inventoryView!!
                val property = getInventoryProperty(inventory.type, id) ?: return
                view.setProperty(property, value)
            }
            isUnobfuscated -> {
                sendPacket(
                    player,
                    nmsClass("network.protocol.game.ClientboundContainerSetDataPacket").unsafeInstance(),
                    "containerId" to windowId,
                    "id" to id,
                    "value" to value
                )
            }
            MinecraftVersion.isUniversal -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowData::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "id" to id,
                    "value" to value
                )
            }
            else -> {
                player.sendPacket(PacketPlayOutWindowData(windowId, id, value))
            }
        }
    }

    override fun toNMSCopy(itemStack: ItemStack?): Any? {
        if (itemStack.isAir()) return emptyItemStack
        return if (isUnobfuscated) {
            craftItemStackCopy(itemStack)
        } else {
            CraftItemStack.asNMSCopy(itemStack)
        }
    }

    private fun sendPacket(player: Player, packet: Any, vararg fields: Pair<String, Any?>) {
        fields.forEach { packet.setProperty(it.first, it.second) }
        player.sendPacket(packet)
    }

    private fun getInventoryProperty(type: InventoryType, id: Int): InventoryView.Property? {
        return InventoryView.Property.entries.find { (it.type == type || (it.type == InventoryType.FURNACE && type == InventoryType.BLAST_FURNACE)) && it.id == id }
    }

    private fun craftItemStackCopy(itemStack: ItemStack): net.minecraft.world.item.ItemStack? {
        return org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(itemStack)
    }

    private fun craftChatMessageComponent(text: String): Any {
        val clazz = Class.forName("org.bukkit.craftbukkit.util.CraftChatMessage")
        return if (text.startsWith('{') && text.endsWith('}')) {
            clazz.invokeMethod<Any>("fromJSON", text, isStatic = true)!!
        } else {
            (clazz.invokeMethod<Array<Any>>("fromString", text, isStatic = true))!![0]
        }
    }
}