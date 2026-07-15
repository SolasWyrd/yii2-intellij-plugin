import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.yii2storm"
version = "1.0.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        phpstorm("2025.1.7.1")
        bundledPlugin("com.jetbrains.php")
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild.set("251")
        }
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.PhpStorm, "2025.1.7.1")
            create(IntelliJPlatformType.PhpStorm, "2026.1")
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    prepareJarSearchableOptions {
        enabled = false
    }

    runIde {
        jvmArgs = listOf("-Xmx2048m")
    }
}
