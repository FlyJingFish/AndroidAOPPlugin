plugins {
    id("java-library")
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
    implementation("org.javassist:javassist:3.25.0-GA")
    intellijPlatform {
        intellijIdeaCommunity("2024.1.4")
        bundledPlugins("ByteCodeViewer","com.intellij.java")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
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