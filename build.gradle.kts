import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


val starsectorDirectory = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
    "C:/Program Files (x86)/Fractal Softworks/Starsector"
} else {
    "/Applications/Starsector.app"
}

val jarFileName = "automaticorders.jar"

val starsectorCoreDirectory = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
    "$starsectorDirectory/starsector-core"
} else {
    "$starsectorDirectory/Contents/Resources/Java"
}
val starsectorModDirectory = "$starsectorDirectory/mods"

plugins {
    kotlin("jvm") version "1.3.11"
    java
}

version = "0.2.1"

repositories {
    maven(url = uri("$projectDir/libs"))
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("starfarer:starfarer-api:1.0")
    implementation(fileTree(starsectorCoreDirectory) {
        include("*.jar")
        exclude("starfarer.api.jar")
    })
}

tasks {
    named<Jar>("jar")
    {
        destinationDir = file("$rootDir/mod")
        archiveName = jarFileName
    }

    register("run-starsector", Exec::class) {
        println("Starting Starsector...")
        workingDir = file(starsectorCoreDirectory)

        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "starsector.bat")
        } else {
            listOf("$starsectorDirectory/Contents/MacOS/starsector_mac.sh")
        }
    }
}

// Compile to Java 6 bytecode so that Starsector can use it
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}