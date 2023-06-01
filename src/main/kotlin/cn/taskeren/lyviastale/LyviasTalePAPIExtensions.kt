package cn.taskeren.lyviastale

import cn.taskeren.lyviastale.count.CVFactory
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

fun initPlaceholderExpansions() {
	LyviasTalePlaceholderExpansion.register()
	LyviasTaleGlobalPlaceholderExpansion.register()
}

private const val Author = "Taskeren"
private const val Version = "1.0.0"

internal object LyviasTalePlaceholderExpansion : PlaceholderExpansion() {

	override fun getIdentifier(): String = "cv"
	override fun getAuthor(): String = Author
	override fun getVersion(): String = Version

	override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
		if(identifier in CVFactory.listPlayerCV()) {
			val cv = CVFactory.getPlayerCV(identifier) ?: return null
			val uuid = player?.uniqueId ?: return null
			return cv.getValue(uuid).toString()
		}
		return null
	}
}

internal object LyviasTaleGlobalPlaceholderExpansion : PlaceholderExpansion() {

	override fun getIdentifier(): String = "gcv"
	override fun getAuthor(): String = Author
	override fun getVersion(): String = Version

	override fun onPlaceholderRequest(player: Player?, identifier: String): String? {
		if(identifier in CVFactory.listGlobalCV()) {
			val cv = CVFactory.getGlobalCV(identifier) ?: return null
			return cv.getValue().toString()
		}
		return null
	}
}