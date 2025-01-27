import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    idea
    kotlin("jvm") version "2.1.10"
    id("io.izzel.taboolib") version "2.0.22"
}

// 这段。一言难尽，但我不想动 (依托)
tasks.build {
    doLast {
        val plugin = project(":plugin")
        val file =
            file("${plugin.layout.buildDirectory.get()}/libs").listFiles()?.find { it.endsWith("plugin-$version.jar") }

        file?.copyTo(file("${project.layout.buildDirectory.get()}/libs/${project.name}-$version.jar"), true)
    }
    dependsOn(project(":plugin").tasks.build)
}

subprojects {

    apply<JavaPlugin>()
    apply(plugin = "idea")
    apply(plugin = "io.izzel.taboolib")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    taboolib {
        env {
            install(
                Basic,
                Bukkit,
                BukkitHook,
                BukkitNMS,
                BukkitNMSUtil,
                BukkitUI,
                BukkitUtil,
                CommandHelper,
                Database,
                AlkaidRedis,
                BukkitFakeOp,
                DatabasePlayer,
                I18n,
                JavaScript,
                Jexl,
                Kether,
                Metrics,
                MinecraftChat,
                XSeries
            )
        }
        version {
//            taboolib = "6.2.0-beta18"
            taboolib = "6.2.2"
            coroutines = null
        }
    }

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("http://sacredcraft.cn:8081/repository/releases") { isAllowInsecureProtocol = true }
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://hub.spigotmc.org/nexus/content/groups/public/")
        maven("https://repo.opencollab.dev/main/")
    }

    dependencies {
        compileOnly(kotlin("stdlib"))
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += listOf("-Xskip-prerelease-check","-Xallow-unstable-dependencies")
        }
    }

    // Java 版本设置
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}
