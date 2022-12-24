// https://kotlinlang.org/docs/get-started-with-jvm-gradle-project.html#explore-the-build-script
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import net.edmacdonald.craftinginterpreters.gradle.ExpressionClassGenerator

plugins {
    kotlin("jvm") version "1.7.21"
    application
    distribution

    // https://plugins.gradle.org/plugin/org.barfuin.gradle.taskinfo
    id("org.barfuin.gradle.taskinfo") version "2.0.0"
}

apply<ExpressionClassGenerator>()

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:30.1.1-jre")
    //testImplementation(kotlin("test-junit5"))
    //testImplementation(kotlin("test-annotations-common"))

    // https://technology.lastminute.com/junit5-kotlin-and-gradle-dsl/
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.2")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
}

task("srcDirs") {
    doLast {
        println("Source Sets:")
        sourceSets.forEach {
            println("\t${it.name}")
            println("\t\tSource: ${it.allSource.srcDirs}")
            println("\t\tOutput: ${it.output.classesDirs.files}")
        }
    }
    dependsOn("generateExpressionClasses")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> { // Settings for `KotlinCompile` tasks
    // Kotlin compiler options
    kotlinOptions.jvmTarget = "1.8" // Target version of generated JVM bytecode
}

application {
    mainClass.set("net.edmacdonald.craftinginterpreters.LoxKt")
}
