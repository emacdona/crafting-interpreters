plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
//    `maven-publish`
//    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.arrow-kt:arrow-core:0.7.3")
}

gradlePlugin {
    plugins {
        create("expressionClassGenerator") {
            id = "net.edmacdonald.craftingInterpreters.gradle.expressionClassGenerator"
            implementationClass = "net.edmacdonald.craftinginterpreters.gradle.ExpressionClassGeneratorPlugin"
        }
    }
}