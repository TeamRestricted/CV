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
import org.bukkit.Bukkit
import org.litote.kmongo.*
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

		val document = Document().apply {
			put("_id", identifier)
			put("defaultValue", defaultValue)
		}
		playerDoc.insertOne(document)
		return PlayerCV(identifier, document)
	}

	fun getPlayerCV(identifier: String): PlayerCV? {
		return PlayerCV.Cached[identifier] ?: playerDoc.findOneById(identifier)?.let { PlayerCV(identifier, it) }
	}

	fun removePlayerCV(identifier: String): Boolean {
		return playerDoc.deleteOneById(identifier).deletedCount > 0
	}

	fun listPlayerCV(): Iterable<String> {
		return playerDoc.find().map { it.getString("_id") }
	}

	fun reload() {
		PlayerCV.reloadAll()
	}

}

// CV objects in JVM

val G = Gson()

abstract class CV(private val identifier: String) {

	internal val triggers = mutableListOf<Trigger>()

	fun addTrigger(trigger: Trigger) {
		triggers.add(trigger)
	}

	private val cvConfigFile = LyviasTale.dataFolder.resolve("cv_${identifier}.json")

	// should be called in the init of children classes, to keep the order that will cause the file is null.
	internal fun ensureCVConfigFilePrepared() {
		// initial
		if(!cvConfigFile.parentFile.exists()) {
			cvConfigFile.parentFile.mkdirs()
		}
		if(!cvConfigFile.exists()) {
			cvConfigFile.createNewFile()
			saveToFile()
		}
	}

	fun readFromFile() {
		runCatching {
			this.triggers.clear()
			val cfg = G.fromJson(cvConfigFile.bufferedReader(), JsonElement::class.java)
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
		cvConfigFile.writeText(G.toJson(json))
	}

}


class GlobalCV(identifier: String) : CV(identifier) {
	val value: Int = 0
}

class PlayerCV(identifier: String, private val document: Document) : CV(identifier) {

	init {
		if(this.identifier in Cached) {
			error("Duplicated Reference of $identifier!")
		}
		Cached[identifier] = this

		ensureCVConfigFilePrepared()
		readFromFile()
	}

	private val identifier get() = document["_id"]!!
	private val defaultValue get() = document.getInteger("defaultValue")

	fun getValue(uuid: UUID): Int {
		return document.getInteger(uuid.toString(), defaultValue)
	}

	fun setValue(uuid: UUID, value: Int) {
		val updates = Updates.set(uuid.toString(), value)
		CVFactory.playerDoc.updateOneById(identifier, updates, upsert(), true)
		document[uuid.toString()] = value

		this.triggers.forEach {
			it.onValueUpdate(value, Bukkit.getPlayer(uuid))
		}
	}

	fun addValue(uuid: UUID, value: Int) {
		val setValue = getValue(uuid) + value
		setValue(uuid, setValue)
	}

	fun resetValue(uuid: UUID) {
		val updates = Updates.set(uuid.toString(), null)
		CVFactory.playerDoc.updateOneById(identifier, updates, upsert(), true)
		document[uuid.toString()] = null
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
	}
}