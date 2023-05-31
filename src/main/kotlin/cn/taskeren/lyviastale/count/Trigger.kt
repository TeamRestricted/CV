package cn.taskeren.lyviastale.count

import cn.taskeren.lyviastale.LyviasTale
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Trigger(
	val condition: String,
	val commands: List<String>
) {

	fun onValueUpdate(value: Int, player: Player?) {
		fun executeCommand() {
			commands.forEach {
				Bukkit.getScheduler().callSyncMethod(LyviasTale) {
					val parsedCommand = PlaceholderAPI.setPlaceholders(player, it)
					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand)
				}
			}
		}

		val cond = condition.trim()
		when {
			cond.startsWith(">") -> {
				val condValue = cond.removePrefix(">").trim().toIntOrNull()
					?: error("Invalid condition value $cond")
				if(value > condValue) {
					executeCommand()
				}
			}

			cond.startsWith("<") -> {
				val condValue = cond.removePrefix("<").trim().toIntOrNull()
					?: error("Invalid condition value $cond")
				if(value < condValue) {
					executeCommand()
				}
			}

			cond.startsWith(">=") -> {
				val condValue = cond.removePrefix(">=").trim().toIntOrNull()
					?: error("Invalid condition value $cond")
				if(value >= condValue) {
					executeCommand()
				}
			}

			cond.startsWith("<=") -> {
				val condValue = cond.removePrefix("<=").trim().toIntOrNull()
					?: error("Invalid condition value $cond")
				if(value <= condValue) {
					executeCommand()
				}
			}

			else -> {
				val condValue = cond.toIntOrNull() ?: error("Invalid condition value $cond")
				if(value == condValue) {
					executeCommand()
				}
			}

		}
	}
}