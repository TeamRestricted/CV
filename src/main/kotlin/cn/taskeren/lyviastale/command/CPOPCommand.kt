package cn.taskeren.lyviastale.command

import cn.taskeren.lyviastale.LyviasTale
import net.kyori.adventure.text.Component
import org.apache.logging.log4j.LogManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.util.*

object CPOPCommand : CommandExecutor, TabCompleter {

	private val LOGGER = LogManager.getLogger()

	override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
		val commandArgs = listOf(*args).toMutableList()
		if(commandArgs.isEmpty()) {
			sender.sendMessage(Component.text("/cpop <player> <...command args> - let the player execute the command as operator permission."))
			return true
		}

		val playerName = commandArgs.removeAt(0)
		val player = Bukkit.getServer().getPlayer(playerName)
		if(player == null) {
			sender.sendMessage("Player is not online!")
			return true
		}

		Bukkit.getScheduler().callSyncMethod(LyviasTale) {
			val wasOp = player.isOp
			LOGGER.debug("Performing command on {}, and he/she {} OP.", player.name(), if(wasOp) "was" else "was not")
			player.isOp = true
			val flag = player.performCommand(java.lang.String.join(" ", commandArgs))
			player.isOp = wasOp
			flag
		}

		sender.sendMessage("Success!")
		return true
	}

	override fun onTabComplete(
		sender: CommandSender,
		command: Command,
		label: String,
		args: Array<out String>
	): List<String> {
		return if(args.isEmpty()) {
			Bukkit.getOnlinePlayers().map { it.name }
		} else if(args.size > 1) {
			val toExecCmdAndArgs = args.toMutableList().apply { removeAt(0) }.joinToString(separator = " ")
			Bukkit.getCommandMap().tabComplete(
				sender,
				toExecCmdAndArgs
			) ?: listOf()
		} else {
			listOf()
		}
	}
}