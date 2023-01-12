plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
//    `maven-publish`
//    `java-library`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("expressionClassGenerator") {
            id = "net.edmacdonald.craftingInterpreters.gradle.expressionClassGenerator"
            implementationClass = "net.edmacdonald.craftinginterpreters.gradle.ExpressionClassGeneratorPlugin"
        }
    }
}