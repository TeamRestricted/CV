// transformed from https://github.com/PaperMC/paperweight-test-plugin/blob/master/build.gradle.kts

plugins {
	java
	kotlin("jvm") version "1.8.20"
	id("io.papermc.paperweight.userdev") version "1.5.5"
	id("xyz.jpenilla.run-paper") version "2.0.1"
}

group = "cn.taskeren"
version = "1.1"
description = "Lyvia's Tale"

repositories {
	mavenCentral()
	maven("https://libraries.minecraft.net")
	maven("https://papermc.io/repo/repository/maven-public/")
	maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
	paperweight.paperDevBundle("1.18.2-R0.1-SNAPSHOT")

	implementation("org.litote.kmongo:kmongo:4.9.0")
	compileOnly("me.clip:placeholderapi:2.11.3")
}

java {
	toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
	assemble {
		dependsOn(reobfJar)
	}

	compileJava {
		options.encoding = Charsets.UTF_8.name()
		options.release.set(17)
	}

	javadoc {
		options.encoding = Charsets.UTF_8.name()
	}

	processResources {
		filteringCharset = Charsets.UTF_8.name()
		val props = mapOf(
			"name" to project.name,
			"version" to project.version,
			"description" to project.description,
			"apiVersion" to "1.18"
		)
		inputs.properties(props)
		filesMatching("plugin.yml") {
			expand(props)
		}
	}
}