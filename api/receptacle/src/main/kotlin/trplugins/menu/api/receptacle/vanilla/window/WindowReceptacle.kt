package trplugins.menu.api.receptacle.vanilla.window

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.function.submit
import taboolib.module.chat.component
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.nmsProxy
import trplugins.menu.api.receptacle.Receptacle
import trplugins.menu.api.receptacle.ReceptacleInteractEvent
import trplugins.menu.api.receptacle.getViewingReceptacle
import trplugins.menu.api.receptacle.setViewingReceptacle

/**
 * @author Arasple
 * @date 2020/11/29 10:38
 */
open class WindowReceptacle(var type: WindowLayout, override var title: String = type.toBukkitType().defaultTitle) : Receptacle<ItemStack>(type) {

    private var viewer: Player? = null

    override var onOpen: ((player: Player, receptacle: Receptacle<ItemStack>) -> Unit) = { _, _ -> }

    override var onClose: ((player: Player, receptacle: Receptacle<ItemStack>) -> Unit) = { _, _ -> }

    override var onClick: ((player: Player, event: ReceptacleInteractEvent<ItemStack>) -> Unit) = { _, _ -> }

    private val contents by lazy { arrayOfNulls<ItemStack?>(type.totalSize) }

    private var hidePlayerInventory = false

    private var stateId = 1
        get() {
            return field++
        }

    fun hidePlayerInventory(hidePlayerInventory: Boolean) {
        this.hidePlayerInventory = hidePlayerInventory
    }

    override fun getElement(slot: Int): ItemStack? {
        setupPlayerInventorySlots()
        return contents.getOrNull(slot)
    }

    override fun hasElement(slot: Int): Boolean {
        return getElement(slot) != null
    }

    override fun setElement(element: ItemStack?, slot: Int, display: Boolean) {
        contents[slot] = element
        val player = viewer ?: return
        if (!display || player.getViewingReceptacle() != this) return
        nmsProxy<NMS>().sendWindowsSetSlot(player, slot = slot, itemStack = element, stateId = stateId)
    }

    override fun clear(display: Boolean) {
        contents.indices.forEach { contents[it] = null }
        if (display) {
            refresh()
        }
    }

    override fun refresh(slot: Int) {
        val player = viewer ?: return
        if (player.getViewingReceptacle() != this) return
        setupPlayerInventorySlots()
        runCatching {
            if (slot >= 0) {
                nmsProxy<NMS>().sendWindowsSetSlot(player, slot = slot, itemStack = contents[slot], stateId = stateId)
            } else {
                nmsProxy<NMS>().sendWindowsItems(player, items = contents)
            }
        }
    }

    override fun open(player: Player) {
        viewer = player
        player.setViewingReceptacle(this)
        initializationPackets()
        onOpen(player, this)
    }

    override fun close(sendPacket: Boolean) {
        if (viewer != null) {
            if (sendPacket) {
                nmsProxy<NMS>().sendWindowsClose(viewer!!)
            }
            onClose(viewer!!, this)
            viewer!!.setViewingReceptacle(null)
            viewer = null
        }
    }

    override fun title(value: String, update: Boolean) {
        title = value
        if (update) {
            submit(delay = 3, async = true) {
                initializationPackets()
            }
        }
    }

    override fun property(id: Int, value: Int) {
        val player = viewer ?: return
        if (player.getViewingReceptacle() != this) return
        nmsProxy<NMS>().sendWindowsUpdateData(player, id = id, value = value)
    }

    override fun callEventClick(event: ReceptacleInteractEvent<ItemStack>) {
        if (viewer != null) {
            onClick(viewer!!, event)
        }
    }

    private fun initializationPackets() {
        if (viewer != null) {
            nmsProxy<NMS>().sendWindowsOpen(viewer!!, title = title, type = type)
            if (MinecraftVersion.majorLegacy < 12111) {
                // 1.21.11+ can crash on offhand slot update during menu switch
                nmsProxy<NMS>().sendWindowsSetSlot(viewer!!, windowId = 0, slot = 45)
            }
            refresh()
        }
    }

    private fun setupPlayerInventorySlots() {
        if (hidePlayerInventory || viewer == null) {
            return
        }
        viewer!!.inventory.contents.forEachIndexed { index, itemStack ->
            if (itemStack != null) {
                val slot = when (index) {
                    in 0..8 -> type.hotBarSlots[index]
                    in 9..35 -> type.mainInvSlots[index - 9]
                    else -> -1
                }
                if (slot > 0) {
                    contents[slot] = itemStack
                }
            }
        }
    }
}
