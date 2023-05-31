package cn.taskeren.lyviastale

import cn.taskeren.lyviastale.count.CVFactory
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class LyviasTalePlaceholderExpansion : PlaceholderExpansion() {

	override fun getIdentifier(): String {
		return "cv"
	}

	override fun getAuthor(): String {
		return "Taskeren"
	}

	override fun getVersion(): String {
		return "1.0.0"
	}

	override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
		if(identifier in CVFactory.listPlayerCV()) {
			val cv = CVFactory.getPlayerCV(identifier) ?: return null
			val uuid = player?.uniqueId ?: return null
			return cv.getValue(uuid).toString()
		}
		return null
	}
}