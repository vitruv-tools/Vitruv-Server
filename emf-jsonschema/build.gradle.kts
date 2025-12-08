plugins {
    application
    kotlin("jvm") version "2.0.20"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "io.freund.adrian.emfjsonschema"
version = "1.0-SNAPSHOT"

application {
    mainClass = "io.freund.adrian.emfjsonschema.MainKt"
}

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "io.freund.adrian.emfjsonschema.MainKt")
    }
}

dependencies {
    val kotestVersion = "5.9.1"
    implementation(kotlin("reflect"))
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("org.eclipse.emf:org.eclipse.emf.ecore:2.36.0")
    implementation("org.eclipse.emf:org.eclipse.emf.ecore.xmi:2.37.0")
    implementation("com.networknt:json-schema-validator:1.5.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("org.eclipse.emfcloud:emfjson-jackson:2.2.0")
    implementation("com.squareup:kotlinpoet:1.18.1")
    implementation("org.slf4j:slf4j-simple:2.0.16")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
}

tasks
    .withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>()
    .configureEach {
        compilerOptions
            .languageVersion
            .set(
                org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2,
            )
    }

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

spotless {
    val ktlintVersion = "1.3.1"

    kotlin {
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        ktlint(ktlintVersion)
    }
}
