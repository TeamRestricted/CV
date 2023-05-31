package cn.taskeren.lyviastale

import cn.taskeren.lyviastale.command.CPOPCommand
import cn.taskeren.lyviastale.command.CVCommand
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.litote.kmongo.KMongo

val LyviasTale: LyviasTalePlugin get() = LyviasTalePlugin.instance
val LyviasTaleDatabase: MongoDatabase get() = lyviasTaleDatabase

val LTLog: Logger = LogManager.getLogger("Lyvia'sTale")

private lateinit var lyviasTaleClient: MongoClient
private lateinit var lyviasTaleDatabase: MongoDatabase

object LyviasTaleInitializer {

	private lateinit var mongoDBUrl: String
	private lateinit var mongoDBName: String

	fun init() {
		LTLog.info("Loading Configurations")
		loadConfig()

		LTLog.info("Connecting Database")
		// fix "Service ClassMappingTypeService not found"
		System.setProperty("org.litote.mongo.test.mapping.service", "org.litote.kmongo.jackson.JacksonClassMappingTypeService")
		lyviasTaleClient = KMongo.createClient(mongoDBUrl)
		lyviasTaleDatabase = lyviasTaleClient.getDatabase(mongoDBName)

		LTLog.info("Registering Commands")
		LyviasTale.getCommand("cpop")?.apply {
			setExecutor(CPOPCommand)
			tabCompleter = CPOPCommand
		}
		LyviasTale.getCommand("cv")?.apply {
			setExecutor(CVCommand)
			tabCompleter = CVCommand
		}

		LTLog.info("Registering PAPI Extension")
		LyviasTalePlaceholderExpansion().register()
	}

	private fun loadConfig() {
		LyviasTale.saveDefaultConfig()
		val config = LyviasTale.config
		mongoDBUrl = config.getString("database.mongodb.url") ?: "mongodb://localhost"
		mongoDBName = config.getString("database.mongodb.name") ?: "LyviasTale"
	}
}