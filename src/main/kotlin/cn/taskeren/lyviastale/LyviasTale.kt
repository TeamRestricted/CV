package cn.taskeren.lyviastale

import cn.taskeren.lyviastale.command.CPOPCommand

val LyviasTale: LyviasTalePlugin get() = LyviasTalePlugin.instance

object LyviasTaleInitializer {

	fun init() {
		LyviasTale.getCommand("cpop")?.apply {
			setExecutor(CPOPCommand)
			tabCompleter = CPOPCommand
		}
	}
}