package net.edmacdonald.craftinginterpreters.gradle

import arrow.core.Option
import arrow.core.getOrElse
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import java.io.File

private const val TASK_NAME = "generateExpressionClasses"
data class Field(
    val type: String,
    val name: String)
data class ExprClass(
    val name: String,
    val fields: List<Field>,
    val base: Option<String> = Option.empty())
interface ExpressionClassGeneratorExtension {
    val imports: ListProperty<String>
    val srcPackage: Property<String>
    val expBaseClassName: Property<String>
    val definitions: ListProperty<ExprClass>
}

abstract class ExpressionClassGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val outputDir = File("${project.buildDir}/generated/main/kotlin")
        val sourceFile = File(outputDir, "Expr.kt")
        val extension = project.extensions.create("expressionClasses", ExpressionClassGeneratorExtension::class.java)

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
                val classes = extension.definitions.get()
                val imports = extension.imports.get()
                val srcPackage = extension.srcPackage.get()
                val expBaseClassName = extension.expBaseClassName.get()
//@formatter:off
                sourceFile.writeText(
                    """
                    package $srcPackage
                    
                    ${
                        imports.map { 
                            """
                            import $it
                            """.trimIndent()
                        }.joinToString("")
                    }
                        
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
                        }.joinToString("\n")
                        }
                    }
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