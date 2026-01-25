package trplugins.menu.module.internal.service

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.command.PermissionDefault
import taboolib.common.platform.command.command
import taboolib.common.platform.function.adaptPlayer
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.unregisterCommand
import taboolib.platform.util.onlinePlayers
import trplugins.menu.TrMenu
import trplugins.menu.TrMenu.actionHandle
import trplugins.menu.api.reaction.Reactions
import trplugins.menu.module.display.MenuSession
import trplugins.menu.util.conf.Property

/**
 * @author Arasple
 * @date 2020/7/28 15:46
 */
object RegisterCommands {

    private val registered = mutableSetOf<String>()

    private fun ofReaction(any: Any?): Reactions {
        return Reactions.ofReaction(actionHandle, Property.asAnyList(any))
    }

    fun load() {
        if (!Bukkit.isPrimaryThread()) {
            submit {
                load()
            }
            return
        }
        val unregisterList = registered.toList()
        unregisterList.forEach { unregisterCommand(it) }
        registered.clear()

        val section = TrMenu.SETTINGS.getConfigurationSection("RegisterCommands")
        section?.let { it ->
            for (main in it.getKeys(false)) {
                val section = it.getConfigurationSection(main) ?: continue
                val argument = section.getConfigurationSection("arguments")
                val reactions = ofReaction(section["execute"])
                val permission = section["permission"]?.toString()
                val subReactions = mutableMapOf<String, Reactions>()
                argument?.getKeys(false)?.forEach {
                    subReactions[it] = ofReaction(argument[it])
                }
                registered.add(main)
                command(
                    name = main,
                    permission = permission ?: "",
                    permissionDefault = if (permission == null) PermissionDefault.TRUE else PermissionDefault.FALSE,
                    aliases = section.getStringList("aliases")
                ) {
                    dynamic(optional = true) {
                        suggestion<Player>(uncheck = true) { _, _ ->
                            argument?.getKeys(false)?.toList()
                        }
                        execute<Player> { player, _, argument ->
                            val args = if (argument.contains(" ")) argument.split(" ") else listOf(argument)
                            val session = MenuSession.getSession(player)
                            if (args.isNotEmpty()) {
                                subReactions[args[0]]?.let { it ->
                                    if (args.size > 1) session.arguments = args.toMutableList().also { it.removeAt(0) }.toTypedArray()
                                    it.eval(adaptPlayer(player))
                                }
                            } else {
                                session.arguments = args.toTypedArray()
                                reactions.eval(adaptPlayer(player))
                            }
                        }
                    }
                    execute<Player> { player, _, _ ->
                        val session = MenuSession.getSession(player)
                        session.arguments = arrayOf()
                        reactions.eval(adaptPlayer(player))
                    }
                }
            }
        }

        // 延迟同步命令到所有在线玩家，避免与 Paper 异步命令发送线程冲突
        // Paper 的 sendAsync 会在异步线程遍历命令树，直接调用 updateCommands 可能触发 ConcurrentModificationException
        submit(delay = 1) {
            val players = onlinePlayers
            if (players.isEmpty()) {
                return@submit
            }
            players.forEach { it.updateCommands() }
        }
    }
}
