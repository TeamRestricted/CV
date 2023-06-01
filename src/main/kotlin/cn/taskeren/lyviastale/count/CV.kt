package cn.taskeren.lyviastale.count

import cn.taskeren.lyviastale.LTLog
import cn.taskeren.lyviastale.LyviasTale
import cn.taskeren.lyviastale.LyviasTaleDatabase
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Updates
import org.bson.Document
import org.bson.conversions.Bson
import org.bukkit.Bukkit
import org.litote.kmongo.*
import java.io.File
import java.util.*

object CVFactory {

	// [{ _id: <identifier>; defaultValue: <default_value>; <player_1>: <player_1_value> }]
	internal val playerDoc: MongoCollection<Document> = LyviasTaleDatabase.getCollection("PlayerSpecValue")
	internal val globalDoc: MongoCollection<Document> = LyviasTaleDatabase.getCollection("GlobalValue")

	fun createPlayerCV(identifier: String, defaultValue: Int): PlayerCV {
		val existed = getPlayerCV(identifier)
		if(existed != null) {
			return existed
		}

		return PlayerCV.create(identifier, defaultValue)
	}

	fun getPlayerCV(identifier: String): PlayerCV? {
		return PlayerCV.Cached[identifier] ?: PlayerCV.find(identifier)
	}

	fun removePlayerCV(identifier: String): Boolean {
		return getPlayerCV(identifier)?.remove() ?: false
	}

	fun listPlayerCV(): Iterable<String> {
		return playerDoc.find().map { it.getString("_id") }
	}

	fun createGlobalCV(identifier: String, defaultValue: Int): GlobalCV {
		val existed = getGlobalCV(identifier)
		if(existed != null) {
			return existed
		}

		return GlobalCV.create(identifier, defaultValue)
	}

	fun getGlobalCV(identifier: String): GlobalCV? {
		return GlobalCV.Cached[identifier] ?: GlobalCV.find(identifier)
	}

	fun removeGlobalCV(identifier: String): Boolean {
		return getGlobalCV(identifier)?.remove() ?: false
	}

	fun listGlobalCV(): Iterable<String> {
		return globalDoc.find().map { it.getString("_id") }
	}

	fun reload() {
		PlayerCV.reloadAll()
		GlobalCV.reloadAll()
	}

}

// CV objects in JVM

val G = Gson()

abstract class CV(val identifier: String) {

	internal val triggers = mutableListOf<Trigger>()

	fun addTrigger(trigger: Trigger) {
		triggers.add(trigger)
	}

	abstract fun getConfigFile(): File

	abstract fun syncRead()
	abstract fun syncWrite()

	// should be called in the init of children classes, to keep the order that will cause the file is null.
	internal fun ensureCVConfigFilePrepared() {
		// initial
		if(!getConfigFile().parentFile.exists()) {
			getConfigFile().parentFile.mkdirs()
		}
		if(!getConfigFile().exists()) {
			getConfigFile().createNewFile()
			saveToFile()
		}
	}

	abstract fun remove(): Boolean

	internal fun onRemove() {
		// rename the config to cv_${identifier}_removed.json. delete the old one if exists
		val renamedFile = getConfigFile().parentFile.resolve(getConfigFile().nameWithoutExtension + "_removed" + getConfigFile().extension)
		if(renamedFile.exists()) {
			renamedFile.delete()
		}
		getConfigFile().renameTo(renamedFile)
	}

	fun readFromFile() {
		runCatching {
			this.triggers.clear()
			val cfg = G.fromJson(getConfigFile().bufferedReader(), JsonElement::class.java)
			val triggers = cfg.asJsonObject.getAsJsonObject("triggers")
			triggers.entrySet().forEach { (condition, element) ->
				val commands = element.asJsonArray.map { it.asJsonPrimitive.asString }
				val trigger = Trigger(condition, commands)
				this.triggers.add(trigger)
			}
		}.onFailure {
			LTLog.info("Exception occurred when loading CV configuration $identifier", it)
		}
	}

	fun saveToFile() {
		val json = JsonObject()
		val triggers = JsonObject()
		json.add("triggers", triggers)
		this.triggers.forEach { t ->
			triggers.add(
				t.condition,
				JsonArray(t.commands.size).apply { t.commands.forEach { add(it) } }
			)
		}
		getConfigFile().writeText(G.toJson(json))
	}

}


class GlobalCV(identifier: String) : CV(identifier) {

	init {
		if(this.identifier in Cached) {
			error("Duplicated Reference of $identifier!")
		}
		Cached[identifier] = this

		ensureCVConfigFilePrepared()
		readFromFile()
	}

	private var value: Int? = null
	private var defaultValue: Int = 0

	override fun getConfigFile(): File =
		LyviasTale.dataFolder.resolve("cv_global_${identifier}.json")

	override fun syncRead() {
		val inDB = CVFactory.globalDoc.findOneById(identifier)
		if(inDB != null) {
			value = inDB.getInteger("value")
			defaultValue = inDB.getInteger("defaultValue")
		}
	}

	override fun syncWrite() {
		val inDB = CVFactory.globalDoc.findOneById(identifier)

		var toUpdate = arrayOf<Bson>()
		if(this.value != inDB?.getInteger("value")) {
			toUpdate += Updates.set("value", this.value)
		}
		if(this.defaultValue != inDB?.getInteger("defaultValue")) {
			toUpdate += Updates.set("defaultValue", this.defaultValue)
		}
		if(toUpdate.isNotEmpty()) {
			CVFactory.globalDoc.updateOneById(identifier, Updates.combine(*toUpdate), upsert())
		}
	}

	override fun remove(): Boolean {
		val flag = CVFactory.globalDoc.deleteOneById(identifier).deletedCount > 0
		onRemove()
		Cached.remove(identifier)
		return flag
	}

	fun getValue(): Int {
		return value ?: defaultValue
	}

	fun setValue(value: Int) {
		this.value = value
		syncWrite()

		this.triggers.forEach {
			it.onValueUpdate(this.getValue(), null)
		}
	}

	fun addValue(value: Int) {
		val setValue = getValue() + value
		setValue(setValue)
	}

	fun resetValue() {
		this.value = null
		syncWrite()

		this.triggers.forEach {
			it.onValueUpdate(this.getValue(), null)
		}
	}

	companion object {
		val Cached = mutableMapOf<String, GlobalCV>()

		fun reloadAll() {
			Cached.forEach { (identifier, cv) ->
				runCatching {
					cv.readFromFile()
				}.onFailure {
					LTLog.warn("Exception occurred when reloading CV configuration $identifier", it)
				}
			}
		}

		fun create(identifier: String, defaultValue: Int): GlobalCV {
			val cv = GlobalCV(identifier)
			cv.defaultValue = defaultValue
			cv.syncWrite()
			return cv
		}

		fun find(identifier: String): GlobalCV? {
			val inDB = CVFactory.globalDoc.findOneById(identifier)
			return if(inDB != null) {
				GlobalCV(identifier).apply(CV::syncRead)
			} else {
				null
			}
		}
	}
}

class PlayerCV(identifier: String) : CV(identifier) {

	init {
		if(this.identifier in Cached) {
			error("Duplicated Reference of $identifier!")
		}
		Cached[identifier] = this

		ensureCVConfigFilePrepared()
		readFromFile()
	}

	private var defaultValue: Int = 0
	private var values: MutableMap<String, Int?> = mutableMapOf()

	override fun getConfigFile(): File =
		LyviasTale.dataFolder.resolve("cv_${identifier}.json")

	private val regexUUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

	override fun syncRead() {
		val inDB = CVFactory.playerDoc.findOneById(identifier)
		if(inDB != null) {
			defaultValue = inDB.getInteger("defaultValue")
			inDB.forEach { key, value ->
				if(regexUUID.matches(key)) {
					values[key] = value as Int?
				}
			}
		}
	}

	override fun syncWrite() {
		val inDB = CVFactory.playerDoc.findOneById(identifier)

		var toUpdate = arrayOf<Bson>()
		if(this.defaultValue != inDB?.getInteger("defaultValue")) {
			toUpdate += Updates.set("defaultValue", this.defaultValue)
		}
		this.values.forEach { (key, value) ->
			if(value != inDB?.getInteger(key)) {
				toUpdate += Updates.set(key, value)
			}
		}
		if(toUpdate.isNotEmpty()) {
			CVFactory.playerDoc.updateOneById(identifier, Updates.combine(*toUpdate), upsert())
		}
	}

	override fun remove(): Boolean {
		val flag = CVFactory.playerDoc.deleteOneById(identifier).deletedCount > 0
		onRemove()
		Cached.remove(identifier)
		return flag
	}

	fun getValue(uuid: UUID): Int {
		return values[uuid.toString()] ?: defaultValue
	}

	fun setValue(uuid: UUID, value: Int) {
		values[uuid.toString()] = value
		syncWrite()

		this.triggers.forEach {
			it.onValueUpdate(getValue(uuid), Bukkit.getPlayer(uuid))
		}
	}

	fun addValue(uuid: UUID, value: Int) {
		val setValue = getValue(uuid) + value
		setValue(uuid, setValue)
	}

	fun resetValue(uuid: UUID) {
		values[uuid.toString()] = null
		syncWrite()

		this.triggers.forEach {
			it.onValueUpdate(getValue(uuid), Bukkit.getPlayer(uuid))
		}
	}

	companion object {
		val Cached = mutableMapOf<String, PlayerCV>()

		fun reloadAll() {
			Cached.forEach { (identifier, cv) ->
				runCatching {
					cv.readFromFile()
				}.onFailure {
					LTLog.warn("Exception occurred when reloading CV configuration $identifier", it)
				}
			}
		}

		fun create(identifier: String, defaultValue: Int): PlayerCV {
			val cv = PlayerCV(identifier)
			cv.defaultValue = defaultValue
			cv.syncWrite()
			return cv
		}

		fun find(identifier: String): PlayerCV? {
			val inDB = CVFactory.playerDoc.findOneById(identifier)
			return if(inDB != null) {
				PlayerCV(identifier).apply(CV::syncRead)
			} else {
				null
			}
		}
	}
}