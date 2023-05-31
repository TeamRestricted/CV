package cn.taskeren.lyviastale.command

import cn.taskeren.lyviastale.LTLog
import cn.taskeren.lyviastale.TextConst
import cn.taskeren.lyviastale.count.CVFactory
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

object CVCommand : CommandExecutor, TabCompleter {

	override fun onCommand(
		sender: CommandSender,
		command: Command,
		label: String,
		argsArr: Array<out String>
	): Boolean {
		val args = listOf(*argsArr).toMutableList()

		if(args.isEmpty()) {
			sender.sendMessage(TextConst.CommandWrongUsageGoToCheckTheManual)
			return true
		}

		val p0 = args.removeAt(0)
		if(p0 == "create") { // create <cv_id> [cv_default]
			val cvIdentifier = args.getOrNull(0)
			val cvDefaultValue = args.getOrNull(1)?.let(String::toIntOrNull) ?: 0
			if(cvIdentifier == null) {
				sender.sendMessage("Missing arguments")
				return true
			}
			CVFactory.createPlayerCV(cvIdentifier, cvDefaultValue)
			sender.sendMessage("Success!")
			return true
		} else if(p0 == "get") { // get <cv_id> <p_id>
			val cvIdentifier = args.getOrNull(0)
			val playerName = args.getOrNull(1)
			if(cvIdentifier == null || playerName == null) {
				sender.sendMessage("Missing arguments")
				return true
			}
			val player = Bukkit.getOfflinePlayer(playerName)
			val cv = CVFactory.getPlayerCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val value = cv.getValue(player.uniqueId)
			sender.sendMessage("CV $cvIdentifier value for player $playerName is ${value}.")
			return true
		} else if(p0 == "set") { // get <cv_id> <p_id> <value>
			val cvIdentifier = args.getOrNull(0)
			val playerName = args.getOrNull(1)
			val newValue = args.getOrNull(2)?.let(String::toIntOrNull)
			if(cvIdentifier == null || playerName == null || newValue == null) {
				sender.sendMessage("Missing/invalid arguments")
				return true
			}
			val player = Bukkit.getOfflinePlayer(playerName)
			val cv = CVFactory.getPlayerCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val oldValue = cv.getValue(player.uniqueId)
			cv.setValue(player.uniqueId, newValue)
			sender.sendMessage("CV $cvIdentifier value for player $playerName was ${oldValue}, and now is ${newValue}.")
			return true
		} else if(p0 == "add") {
			val cvIdentifier = args.getOrNull(0)
			val playerName = args.getOrNull(1)
			val addValue = args.getOrNull(2)?.let(String::toIntOrNull)
			if(cvIdentifier == null || playerName == null || addValue == null) {
				sender.sendMessage("Missing/invalid arguments")
				return true
			}
			val player = Bukkit.getOfflinePlayer(playerName)
			val cv = CVFactory.getPlayerCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val oldValue = cv.getValue(player.uniqueId)
			cv.addValue(player.uniqueId, addValue)
			val newValue = oldValue + addValue
			sender.sendMessage("CV $cvIdentifier value for player $playerName was ${oldValue}, and now is ${newValue}.")
			return true
		} else if(p0 == "reset") {
			val cvIdentifier = args.getOrNull(0)
			val playerName = args.getOrNull(1)
			if(cvIdentifier == null || playerName == null) {
				sender.sendMessage("Missing/invalid arguments")
				return true
			}
			val player = Bukkit.getOfflinePlayer(playerName)
			val cv = CVFactory.getPlayerCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val oldValue = cv.getValue(player.uniqueId)
			cv.resetValue(player.uniqueId)
			val newValue = cv.getValue(player.uniqueId)
			sender.sendMessage("CV $cvIdentifier value for player $playerName was ${oldValue}, and now is ${newValue}.")
			return true
		} else if(p0 == "remove") {
			val cvIdentifier = args.getOrNull(0)
			if(cvIdentifier == null) {
				sender.sendMessage("Missing arguments")
				return true
			}
			val flag = CVFactory.removePlayerCV(cvIdentifier)
			if(flag) {
				sender.sendMessage("Success!")
			} else {
				sender.sendMessage("CV is not found!")
			}
			return true
		} else if(p0 == "list") {
			sender.sendMessage("===[ Available CVs ]===")
			CVFactory.listPlayerCV().forEach {
				sender.sendMessage("- $it")
			}
			return true
		} else if(p0 == "reload") {
			runCatching {
				CVFactory.reload()
				sender.sendMessage("Success!")
			}.onFailure {
				LTLog.error("Exception occurred when reloading CV configurations!", it)
				sender.sendMessage("Something broken!")
				sender.sendMessage("Caused by ${it.javaClass.canonicalName}. Read the log to get more info!")
			}
			return true
		}

		return false
	}

	override fun onTabComplete(
		sender: CommandSender,
		command: Command,
		label: String,
		argsArr: Array<out String>
	): List<String> {
		val args = listOf(*argsArr)

		val subcommandToProcessCV =
			listOf("get", "set", "add", "reset", "remove")

		val arg0 = args.getOrNull(0)

		return when(args.size) {
			1 -> listOf("create", "get", "set", "add", "reset", "remove", "list", "reload")
			2 -> {
				if(arg0 in subcommandToProcessCV) {
					return CVFactory.listPlayerCV().toList()
				} else {
					listOf()
				}
			}
			3 -> {
				if(arg0 in subcommandToProcessCV) {
					return Bukkit.getOnlinePlayers().map { it.name }
				} else {
					listOf()
				}
			}
			4 -> {
				if(arg0 in subcommandToProcessCV) {
					return listOf("0", "1", "100")
				} else {
					return listOf()
				}
			}
			else -> listOf()
		}
	}
}