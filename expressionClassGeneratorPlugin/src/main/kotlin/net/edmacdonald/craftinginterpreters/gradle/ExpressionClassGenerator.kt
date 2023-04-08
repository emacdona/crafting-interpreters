package net.edmacdonald.craftinginterpreters.gradle

import arrow.core.Option
import arrow.core.getOrElse
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

private const val TASK_NAME = "generateProductionClasses"

data class Field(
    val type: String,
    val name: String
)

data class ProductionClass(
    val name: String,
    val fields: List<Field>,
    val base: Option<String> = Option.empty()
)

data class Production(
    val expBaseClassName: String,
    val definitions: List<ProductionClass>
)

interface ProductionClassGeneratorExtension {
    val imports: ListProperty<String>
    val srcPackage: Property<String>
    val productions: ListProperty<Production>
}

abstract class ProductionClassGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val outputDir = File("${project.buildDir}/generated/main/kotlin")
        val sourceFile = File(outputDir, "Grammar.kt")
        val extension = project.extensions.create("productionClasses", ProductionClassGeneratorExtension::class.java)

        val sourceSets = project.getProperties().get("sourceSets") as SourceSetContainer
        sourceSets
            .getByName("main")
            .java
            .srcDirs(outputDir)

        project.task(TASK_NAME) {
            doLast {
                println("OutputDir: ${outputDir}")
                println("SourceFile: ${sourceFile}")
                outputDir.mkdirs()

                val imports = extension.imports.get()
                val srcPackage = extension.srcPackage.get()

//@formatter:off
                sourceFile.writeText(
                    """
                        package $srcPackage
                    
                    ${
                        imports.map {
                        """
                        import $it
                        """
                        }.joinToString("")
                    }
                ${extension.productions.get().map { production ->
                    val expBaseClassName = production.expBaseClassName
                    val classes = production.definitions
                        """
                        abstract class $expBaseClassName {
                            interface Visitor<R> {${
                                classes.map{
                                """
                                fun visit${it.name}(it: ${it.name}): R
                                """.trimEnd()
                                }.joinToString("")
                            }
                            }
                            
                            abstract fun <R> accept(visitor: Visitor<R>): R
                            ${
                            classes.map {
                            """
                            data class ${it.name} (${
                                it.fields.map {
                                """
                                val ${it.name}: ${it.type}
                                """.trimEnd()
                                }.joinToString(",")
                            }
                            ) : ${it.base.getOrElse { expBaseClassName }}() {
                                override fun <R> accept(visitor: Visitor<R>): R = visitor.visit${it.name}(this)
                            }
                            """
                            .trimEnd()
                            }.joinToString("")
                            }
                        }
                        """
                }.joinToString("\n")}
                """.trimIndent()
                )
//@formatter:on
            }
        }
        project.tasks.named("compileKotlin") {
            dependsOn(TASK_NAME)
        }
    }
}