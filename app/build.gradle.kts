// https://kotlinlang.org/docs/get-started-with-jvm-gradle-project.html#explore-the-build-script
import net.edmacdonald.craftinginterpreters.gradle.ExpressionClassGeneratorExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import net.edmacdonald.craftinginterpreters.gradle.ExprClass
import net.edmacdonald.craftinginterpreters.gradle.Field

plugins {
    kotlin("jvm") version "1.7.21"
    application
    distribution
    id("net.edmacdonald.craftingInterpreters.gradle.expressionClassGenerator")
}

repositories {
    mavenCentral()
}

the<ExpressionClassGeneratorExtension>().apply {
    val expBaseClassName = "Expr"

    imports.set(
        listOf("net.edmacdonald.craftinginterpreters.scanner.Token")
    )

    srcPackage.set("net.edmacdonald.craftinginterpreters.parser")

    this.expBaseClassName.set(expBaseClassName)

    definitions.set(
        listOf(
            ExprClass(
                "Binary", listOf(
                    Field(expBaseClassName, "left"),
                    Field("Token", "operator"),
                    Field(expBaseClassName, "right")
                )
            ),
            ExprClass(
                "Grouping", listOf(
                    Field(expBaseClassName, "expression")
                )
            ),
            ExprClass(
                "Literal", listOf(
                    Field("Any?", "value")
                )
            ),
            ExprClass(
                "Unary", listOf(
                    Field("Token", "operator"),
                    Field(expBaseClassName, "right")
                )
            )
        )
    )
}

dependencies {
    implementation("com.google.guava:guava:30.1.1-jre")

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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("net.edmacdonald.craftinginterpreters.LoxKt")
}
