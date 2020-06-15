/*
 * we-are-the-robots - a base for building any art robots - upcycling
 * useful automata into useless experience
 * Copyright (C) 2020  Kazimierz Pogoda
 *
 * This file is part of we-are-the-robots.
 *
 * we-are-the-robots is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * we-are-the-robots is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with we-are-the-robots.
 * If not, see <https://www.gnu.org/licenses/>.
 */

@Suppress("MayBeConstant")
object V {
  val java = JavaVersion.VERSION_1_8
  val kotlin = "1.3.72"
  val kotlinLogging = "1.7.9"
  val rxJava = "3.0.4"
  val rxKotlin = "3.0.0"
  val junit = "5.6.2"
  val atrium = "0.12.0"
  val log4j = "2.13.3"
  val jackson = "2.11.0"
  val jssc = "2.8.0"
  val xemanticState = "1.0-SNAPSHOT"
}

plugins {
  `maven-publish`
  kotlin("jvm") version "1.3.72" apply false
  id("io.spring.dependency-management") version "1.0.9.RELEASE"
  id("org.jetbrains.dokka") version "0.10.1"
}

allprojects {
  repositories {
    jcenter()
    mavenLocal()
  }
}

subprojects {

  group = "com.xemantic.robots"
  version = "1.0-SNAPSHOT"

  apply {
    plugin("io.spring.dependency-management")
    plugin("org.jetbrains.kotlin.jvm")
    plugin("maven-publish")
    plugin("org.jetbrains.dokka")
  }

  configure<JavaPluginExtension> {
    sourceCompatibility = V.java
    targetCompatibility = V.java
    withSourcesJar()
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = V.java.toString()
  }

  dependencyManagement {
    dependencies {

      // implementation dependencies
      dependency("org.jetbrains.kotlin:kotlin-reflect:${V.kotlin}")
      dependency("io.github.microutils:kotlin-logging:${V.kotlinLogging}")
      dependency("com.xemantic.state:xemantic-state-core:${V.xemanticState}")

      dependency("io.reactivex.rxjava3:rxjava:${V.rxJava}")
      dependency("io.reactivex.rxjava3:rxkotlin:${V.rxKotlin}")

      dependency("org.scream3r:jssc:${V.jssc}")

      // test dependencies
      dependency("org.jetbrains.kotlin:kotlin-test-junit5:${V.kotlin}")
      dependency("org.junit.jupiter:junit-jupiter-api:${V.junit}")
      dependency("org.junit.jupiter:junit-jupiter:${V.junit}")
      dependency("ch.tutteli.atrium:atrium-fluent-en_GB:${V.atrium}")
      dependency("ch.tutteli.atrium:atrium-api-fluent-en_GB-kotlin_1_3:${V.atrium}")
      dependency("org.apache.logging.log4j:log4j-api:${V.log4j}")
      dependency("org.apache.logging.log4j:log4j-core:${V.log4j}")
      dependency("org.apache.logging.log4j:log4j-slf4j-impl:${V.log4j}")
      dependency("com.fasterxml.jackson.core:jackson-databind:${V.jackson}")
      dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${V.jackson}")
    }
  }

  dependencies {
    "testImplementation"("org.junit.jupiter:junit-jupiter-api")
    "testImplementation"("ch.tutteli.atrium:atrium-fluent-en_GB")
    "testImplementation"("ch.tutteli.atrium:atrium-api-fluent-en_GB-kotlin_1_3")
    "testRuntimeOnly"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.apache.logging.log4j:log4j-api")
    "testRuntimeOnly"("org.apache.logging.log4j:log4j-core")
    "testRuntimeOnly"("org.apache.logging.log4j:log4j-slf4j-impl")
    "testRuntimeOnly"("com.fasterxml.jackson.core:jackson-databind")
    "testRuntimeOnly"("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
  }

  tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
  }

  val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
  }

  publishing {
    publications {
      create<MavenPublication>("default") {
        from(components["java"])
        artifact(dokkaJar)
      }
    }
  }

}
