package cn.taskeren.lyviastale.value

import cn.taskeren.lyviastale.LyviasTalePlugin
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.file.YamlConfiguration

private val pluginDataFolder get() = LyviasTalePlugin.dataFolder

fun getLTYamlConfigForPlayer(player: OfflinePlayer) =
	getLTYamlConfigForUUID(player.uniqueId.toString())

fun getLTYamlConfigForUUID(uuid: String): YamlConfiguration {
	val file = pluginDataFolder.resolve("${uuid}.yml")
	return YamlConfiguration().apply { load(file) }
}
