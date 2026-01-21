package trplugins.menu.api.receptacle.vanilla.window

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.PacketReceiveEvent
import taboolib.module.nms.nmsProxy
import trplugins.menu.api.receptacle.ReceptacleClickType
import trplugins.menu.api.receptacle.ReceptacleCloseEvent
import trplugins.menu.api.receptacle.ReceptacleInteractEvent
import trplugins.menu.api.receptacle.getViewingReceptacle
import trplugins.menu.api.receptacle.vanilla.window.NMS.Companion.useStaticInventory
import trplugins.menu.api.receptacle.vanilla.window.StaticInventory.staticInventory

@PlatformSide(Platform.BUKKIT)
object WindowListener {

    @SubscribeEvent
    fun onPacket(e: PacketReceiveEvent) {
        if (e.player.useStaticInventory()) return
        val receptacle = e.player.getViewingReceptacle() as? WindowReceptacle ?: return
        if (e.packet.name == "PacketPlayInWindowClick" || e.packet.name == "ServerboundContainerClickPacket") {
            val id = if (MinecraftVersion.isUniversal) {
                e.packet.read<Int>("containerId")
            } else {
                e.packet.read<Int>("a")
            }
            if (id == nmsProxy<NMS>().windowId(e.player)) {
                e.isCancelled = true
                val slot: Int
                val clickType: ReceptacleClickType
                val button: Int
                if (MinecraftVersion.isUniversal) {
                    slot = e.packet.read<Int>("slotNum")!!
                    button = e.packet.read<Int>("buttonNum")!!
                    clickType = ReceptacleClickType.from(e.packet.read<Any>("clickType").toString(), button, slot) ?: return
                } else if (MinecraftVersion.majorLegacy >= 10900) {
                    slot = e.packet.read<Int>("slot")!!
                    button = e.packet.read<Int>("button")!!
                    clickType = ReceptacleClickType.from(e.packet.read<Any>("shift").toString(), button, slot) ?: return
                } else {
                    slot = e.packet.read<Int>("slot")!!
                    button = e.packet.read<Int>("button")!!
                    clickType = ReceptacleClickType.from(e.packet.read<Int>("shift")!!, button, slot) ?: return
                }
                clicked(e.player, receptacle, clickType, slot)
            }
        } else if (e.packet.name == "PacketPlayInCloseWindow" || e.packet.name == "ServerboundContainerClosePacket") {
            val id = if (MinecraftVersion.isUniversal) {
                e.packet.read<Int>("containerId")
            } else {
                e.packet.read<Int>("id")
            }
            if (id == nmsProxy<NMS>().windowId(e.player)) {
                close(e.player, receptacle)
            }
            e.isCancelled = true
            ReceptacleCloseEvent(e.player, receptacle).call()
        }
    }

    @SubscribeEvent
    fun onClick(e: InventoryClickEvent) {
        (e.whoClicked as? Player)?.staticInventory ?: return
        if (e.inventory.holder is StaticInventory.Holder) {
            e.isCancelled = true

            val player = e.whoClicked as? Player ?: return
            val receptacle = player.getViewingReceptacle() as? WindowReceptacle ?: return
            e.click.ordinal
            val clickType = ReceptacleClickType.from(e.click, e.action, if (e.click == ClickType.NUMBER_KEY) e.hotbarButton else e.slot) ?: return

            clicked(player, receptacle, clickType, e.slot)
        }
    }

    @SubscribeEvent
    fun onClose(e: InventoryCloseEvent) {
        (e.player as? Player)?.staticInventory ?: return
        if (e.inventory.holder is StaticInventory.Holder) {
            val player = e.player as? Player ?: return
            val receptacle = player.getViewingReceptacle() as? WindowReceptacle ?: return

            close(player, receptacle)
        }
    }

    @SubscribeEvent
    fun onDrag(e: InventoryDragEvent) {
        (e.whoClicked as? Player)?.staticInventory ?: return
        if (e.inventory.holder is StaticInventory.Holder) {
            e.isCancelled = true
        }
    }

    private fun clicked(player: Player, receptacle: WindowReceptacle, clickType: ReceptacleClickType, slot: Int) {
        val evt = ReceptacleInteractEvent(player, receptacle, clickType, slot)
        evt.call()
        receptacle.callEventClick(evt)
        if (evt.isCancelled) {
            if (clickType == ReceptacleClickType.OFFHAND) {
                if (MinecraftVersion.majorLegacy < 12111) {
                    nmsProxy<NMS>().sendWindowsSetSlot(player, windowId = 0, slot = 45)
                } else {
                    nmsProxy<NMS>().sendWindowsSetSlot(player, slot = -1, windowId = -1)
                }
            } else {
                nmsProxy<NMS>().sendWindowsSetSlot(player, slot = -1, windowId = -1)
            }
        }
    }

    private fun close(player: Player, receptacle: WindowReceptacle) {
        receptacle.close(false)
        // 防止关闭菜单后, 动态标题频率过快出现的卡假容器
        submit(delay = 1, async = true) {
            val viewingReceptacle = player.getViewingReceptacle()
            if (viewingReceptacle == null) {
                player.updateInventory()
            }
        }
        submit(delay = 4, async = true) {
            val viewingReceptacle = player.getViewingReceptacle()
            if (viewingReceptacle == receptacle) {
                nmsProxy<NMS>().sendWindowsClose(player)
            }
        }
    }
}
