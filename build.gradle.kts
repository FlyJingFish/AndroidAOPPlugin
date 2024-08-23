plugins {
    alias(libs.plugins.kotlin) apply false
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    intellijPlatform {
        intellijIdeaCommunity("2024.1.4")
        bundledPlugins("com.intellij.java", "org.jetbrains.kotlin")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

intellijPlatform {
    sandboxContainer = rootProject.layout.buildDirectory.dir("../idea_sandbox")
}

tasks {
    patchPluginXml {
        changeNotes = """
            <b>Version 1.0.1</b>
            <ul>
                <li>Adapt to the latest version of IC</li>
            </ul>
            <b>Version 1.0.0</b>
            <ul>
                <li>publish plugin</li>
            </ul>
        """.trimIndent()
        version = project.version.toString()
    }

    publishPlugin {
        token = project.findProperty("intellij.publish.token")?.toString()
    }

    runIde {
        jvmArgs("-Xmx1g")
    }
}