plugins {
    id("java-library")
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jetbrains.changelog") version "2.2.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}
val asm: Configuration by configurations.creating
val asmVersion = "9.7"

val shadowAsmJar = tasks.create("shadowAsmJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    group = "shadow"
    relocate("org.objectweb.asm", "dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm")
    configurations = listOf(asm)
    archiveClassifier.set("asm")
    exclude { file -> file.name == "module-info.class" }
    manifest {
        attributes("Asm-Version" to asmVersion)
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        kotlin {
            setSrcDirs(listOf("src/main/kotlin")) // 指定 Kotlin 文件的目录
        }
    }
}


dependencies {
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("org.javassist:javassist:3.25.0-GA")
    intellijPlatform {
        intellijIdeaCommunity("2024.1.4")
        bundledPlugins("ByteCodeViewer","com.intellij.java","org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
    api(shadowAsmJar.outputs.files)
    asm("org.ow2.asm:asm:$asmVersion")
    asm("org.ow2.asm:asm-analysis:$asmVersion")
    asm("org.ow2.asm:asm-util:$asmVersion")
    asm("org.ow2.asm:asm-commons:$asmVersion")

    implementation("org.apache.commons:commons-text:1.11.0")

    testImplementation("org.assertj:assertj-core:3.24.2")

    val jUnit5Version = "5.10.1"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnit5Version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$jUnit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnit5Version")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$jUnit5Version")
    testImplementation("junit:junit:4.13.2")

    // Used for test data
    testImplementation("org.codehaus.groovy:groovy:3.0.16")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    testImplementation("org.apache.commons:commons-lang3:3.12.0")
}

intellijPlatform {
    sandboxContainer = rootProject.layout.buildDirectory.dir("../idea_sandbox")
}

tasks {
    publishPlugin {
        token = project.findProperty("intellij.publish.token")?.toString()
    }

    runIde {
        jvmArgs("-Xmx1g")
    }
}