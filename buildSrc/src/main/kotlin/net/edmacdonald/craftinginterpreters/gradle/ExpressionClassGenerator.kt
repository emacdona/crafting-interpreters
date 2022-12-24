package net.edmacdonald.craftinginterpreters.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

abstract class ExpressionClassGenerator : Plugin<Project> {
    companion object {
        val TASK_NAME = "generateExpressionClasses"
    }

    var classes: MutableList<ExprClass> = mutableListOf(
        ExprClass(
            "Binary", listOf(
                Field("Expr", "left"),
                Field("Token", "operator"),
                Field("Expr", "right")
            )
        ),
        ExprClass(
            "Grouping", listOf(
                Field("Expr", "expression")
            )
        ),
        ExprClass(
            "Literal", listOf(
                Field("Any", "value")
            )
        ),
        ExprClass(
            "Unary", listOf(
                Field("Token", "operator"),
                Field("Expr", "right")
            )
        )
    );

    override fun apply(project: Project) {
        val outputDir = File("${project.buildDir}/generated/main/kotlin")
        val sourceFile = File(outputDir, "Expr.kt")

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
                sourceFile.writeText(
                    """
                        package net.edmacdonald.craftinginterpreters
                        
                        abstract class Expr {
                            ${
                        classes.map {
                            """
                                data class ${it.name} (
                                    ${
                                it.fields.map {
                                    """
                                        val ${it.name}: ${it.type}"""
                                        .trimEnd()
                                }.joinToString(",\n")
                            }
                                ) : ${it.base}()
                                """
                                .trimEnd()
                        }.joinToString("\n")
                    }
                        }
                        """.trimIndent()
                )
            }
        }
        project.tasks.named("compileKotlin") {
            dependsOn(TASK_NAME)
        }
    }
}

data class Field(val type: String, val name: String)
data class ExprClass(val name: String, val fields: List<Field>, val base: String = "Expr")
