// https://kotlinlang.org/docs/get-started-with-jvm-gradle-project.html#explore-the-build-script
import net.edmacdonald.craftinginterpreters.gradle.ProductionClassGeneratorExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import net.edmacdonald.craftinginterpreters.gradle.ProductionClass
import net.edmacdonald.craftinginterpreters.gradle.Field
import net.edmacdonald.craftinginterpreters.gradle.Production

plugins {
    kotlin("jvm") version "1.7.21"
    application
    distribution
    id("net.edmacdonald.craftingInterpreters.gradle.productionClassGenerator")
}

repositories {
    mavenCentral()
}

the<ProductionClassGeneratorExtension>().apply {
    val expBaseClassName = "Expr"
    val stmtBaseClassName = "Stmt"

    imports.set(
        listOf("net.edmacdonald.craftinginterpreters.scanner.Token")
    )

    srcPackage.set("net.edmacdonald.craftinginterpreters.parser")

    productions.set(
        listOf(
            Production(
                stmtBaseClassName,
                listOf(
                    ProductionClass(
                        "Expression",
                        listOf(
                            Field(expBaseClassName, "expression")
                        )
                    ),
                    ProductionClass(
                        "Print",
                        listOf(
                            Field(expBaseClassName, "expression")
                        )
                    ),
                    ProductionClass(
                        "Var",
                        listOf(
                            Field("Token", "name"),
                            Field("Expr?", "initializer")
                        )
                    )
                )
            ),
            Production(
                expBaseClassName,
                listOf(
                    ProductionClass(
                        "Assign",
                        listOf(
                            Field("Token", "name"),
                            Field(expBaseClassName, "value")
                        )
                    ),
                    ProductionClass(
                        "Binary",
                        listOf(
                            Field(expBaseClassName, "left"),
                            Field("Token", "operator"),
                            Field(expBaseClassName, "right")
                        )
                    ),
                    ProductionClass(
                        "Grouping",
                        listOf(
                            Field(expBaseClassName, "expression")
                        )
                    ),
                    ProductionClass(
                        "Literal",
                        listOf(
                            Field("Any?", "value")
                        )
                    ),
                    ProductionClass(
                        "Unary",
                        listOf(
                            Field("Token", "operator"),
                            Field(expBaseClassName, "right")
                        )
                    ),
                    ProductionClass(
                        "Variable",
                        listOf(
                            Field("Token", "name")
                        )
                    )
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
    dependsOn("generateProductionClasses")
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
