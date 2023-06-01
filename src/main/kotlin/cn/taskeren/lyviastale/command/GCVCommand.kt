package cn.taskeren.lyviastale.command

import cn.taskeren.lyviastale.LTLog
import cn.taskeren.lyviastale.TextConst
import cn.taskeren.lyviastale.count.CVFactory
import org.bukkit.ChatColor
import org.bukkit.ChatColor.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

object GCVCommand : CommandExecutor, TabCompleter {

	private val HelpText = """
		${GRAY}${BOLD}===[ ${GOLD}${BOLD}GCV ${GRAY}${BOLD}]===
		${GRAY}${BOLD}- ${GOLD}${BOLD}create ${GRAY}<identifier> [defaultValue] ${WHITE}- To create the CV with default value
		${GRAY}${BOLD}- ${GOLD}${BOLD}remove ${GRAY}<identifier> ${WHITE}- To remove the CV
		${GRAY}${BOLD}- ${GOLD}${BOLD}set ${GRAY}<identifier> <value> ${WHITE}- To set the value of the CV
		${GRAY}${BOLD}- ${GOLD}${BOLD}add ${GRAY}<identifier> <value> ${WHITE}- To add the value to the CV
		${GRAY}${BOLD}- ${GOLD}${BOLD}get ${GRAY}<identifier> ${WHITE}- To get the value of the CV
		${GRAY}${BOLD}- ${GOLD}${BOLD}reset ${GRAY}<identifier> ${WHITE}- To reset the value of the CV to the default
		${GRAY}${BOLD}- ${GOLD}${BOLD}list ${WHITE}- To get the list of the identifiers of the CVs
		${GRAY}${BOLD}- ${GOLD}${BOLD}reload ${WHITE}- To reload the configurations of the CVs ${GRAY}(mostly used to update the triggers)
	""".trimIndent()

	override fun onCommand(
		sender: CommandSender,
		command: Command,
		label: String,
		argsArr: Array<out String>
	): Boolean {
		val args = listOf(*argsArr).toMutableList()

		if(args.isEmpty()) {
			sender.sendMessage(HelpText)
			return true
		}

		val p0 = args.removeAt(0)
		if(p0 == "create") {
			val cvIdentifier = args.getOrNull(0)
			val cvDefaultValue = args.getOrNull(1)?.let(String::toIntOrNull) ?: 0
			if(cvIdentifier == null) {
				sender.sendMessage("Missing arguments")
				return true
			}
			CVFactory.createGlobalCV(cvIdentifier, cvDefaultValue)
			sender.sendMessage("Success!")
			return true
		} else if(p0 == "get") {
			val cvIdentifier = args.getOrNull(0)
			if(cvIdentifier == null) {
				sender.sendMessage("Missing arguments")
				return true
			}
			val cv = CVFactory.getGlobalCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val value = cv.getValue()
			sender.sendMessage("CV $cvIdentifier value is ${value}.")
			return true
		} else if(p0 == "set") {
			val cvIdentifier = args.getOrNull(0)
			val newValue = args.getOrNull(1)?.let(String::toIntOrNull)
			if(cvIdentifier == null || newValue == null) {
				sender.sendMessage("Missing/invalid arguments")
				return true
			}
			val cv = CVFactory.getGlobalCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val oldValue = cv.getValue()
			cv.setValue(newValue)
			sender.sendMessage("CV $cvIdentifier value was ${oldValue}, and now is ${newValue}.")
			return true
		} else if(p0 == "add") {
			val cvIdentifier = args.getOrNull(0)
			val addValue = args.getOrNull(1)?.let(String::toIntOrNull)
			if(cvIdentifier == null || addValue == null) {
				sender.sendMessage("Missing/invalid arguments")
				return true
			}
			val cv = CVFactory.getGlobalCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val oldValue = cv.getValue()
			cv.addValue(addValue)
			val newValue = oldValue + addValue
			sender.sendMessage("CV $cvIdentifier value was ${oldValue}, and now is ${newValue}.")
			return true
		} else if(p0 == "reset") {
			val cvIdentifier = args.getOrNull(0)
			if(cvIdentifier == null) {
				sender.sendMessage("Missing/invalid arguments")
				return true
			}
			val cv = CVFactory.getGlobalCV(cvIdentifier)
			if(cv == null) {
				sender.sendMessage("CV does not exist!")
				return true
			}
			val oldValue = cv.getValue()
			cv.resetValue()
			val newValue = cv.getValue()
			sender.sendMessage("CV $cvIdentifier value was ${oldValue}, and now is ${newValue}.")
			return true
		} else if(p0 == "remove") {
			val cvIdentifier = args.getOrNull(0)
			if(cvIdentifier == null) {
				sender.sendMessage("Missing arguments")
				return true
			}
			val flag = CVFactory.removeGlobalCV(cvIdentifier)
			if(flag) {
				sender.sendMessage("Success!")
			} else {
				sender.sendMessage("CV is not found!")
			}
			return true
		} else if(p0 == "list") {
			sender.sendMessage("===[ Available GCVs ]===")
			CVFactory.listGlobalCV().forEach {
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
					return listOf("0", "1", "100")
				} else {
					return listOf()
				}
			}
			else -> listOf()
		}
	}
}