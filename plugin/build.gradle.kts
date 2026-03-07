taboolib {
    description {
        name(rootProject.name)
        desc("Modern & Advanced Menu-Plugin for Minecraft Servers")
        contributors {
            name("Arasple")
            name("Score2")
        }
        dependencies {
            name("MagicCosmetics").with("bukkit").optional(true)
            name("PlaceholderAPI").with("bukkit").optional(true)
            name("Zaphkiel").with("bukkit").optional(true)
            name("Skulls").with("bukkit").optional(true)
            name("Vault").with("bukkit").optional(true)
            name("PlayerPoints").with("bukkit").optional(true)
            name("HeadDatabase").with("bukkit").optional(true)
            name("Oraxen").with("bukkit").optional(true)
            name("SkinsRestorer").with("bukkit").optional(true)
            name("ItemsAdder").with("bukkit").optional(true)
            name("floodgate").with("bukkit").optional(true)
            name("FastScript").with("bukkit").optional(true)
            name("Triton").with("bukkit").optional(true)
            name("MMOItems").with("bukkit").optional(true)
            name("MagicGem").with("bukkit").optional(true)
            name("NeigeItems").with("bukkit").optional(true)
            name("EcoItems").with("bukkit").optional(true)
            name("MythicMobs").with("bukkit").optional(true)
            name("HMCCosmetics").with("bukkit").optional(true)
            name("PxRpg").with("bukkit").optional(true)
            name("Nexo").with("bukkit").optional(true)
            name("NBTAPI").with("bukkit").optional(true).loadafter(true)
            name("TrMenu-Graal").with("bukkit").optional(true)
            name("AzureFlow").with("bukkit").optional(true)
            name("MeowEco").with("bukkit").optional(true)
            name("CraftEngine").with("bukkit").optional(true)
            name("SX-Item").with("bukkit").optional(true)
        }
    }
    relocate("trplugins.menu", group.toString())
    relocate("ink.ptms.um", "${group}.um")
    relocate("top.maplex.arim", "${group}.arim")
}

repositories {
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.org/repository/maven-public")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.oraxen.com/releases")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://r.irepo.space/maven/")
    maven("https://repo.auxilor.io/repository/maven-public/")
    maven("https://repo.hibiscusmc.com/releases/")
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    taboo(project(":common"))
    taboo(project(":api:receptacle"))
    taboo(project(":api:action"))
    taboo("ink.ptms:um:1.1.5")
    taboo("top.maplex.arim:Arim:1.2.13")

    // Libraries
    compileOnly("org.apache.commons:commons-lang3:3.17.0")

    // Server Core
    compileOnly("ink.ptms.core:v12005:12005-minimize:mapped")
    compileOnly("ink.ptms.core:v12005:12005-minimize:universal")
    compileOnly("ink.ptms.core:v12002:12002-minimize:mapped")
    compileOnly("ink.ptms.core:v12002:12002-minimize:universal")
    compileOnly("ink.ptms.core:v11904:11904-minimize:mapped")
    compileOnly("ink.ptms.core:v11904:11904-minimize:universal")
    compileOnly("ink.ptms.core:v11701:11701-minimize:mapped")
    compileOnly("ink.ptms.core:v11701:11701-minimize:universal")
    compileOnly("ink.ptms.core:v11605:11605")

    // Hook Plugins

    compileOnly("me.clip:placeholderapi:2.11.6") { isTransitive = false }
    compileOnly("ink.ptms:Zaphkiel:2.0.14") { isTransitive = false }
    compileOnly("ca.tweetzy:skulls:3.10.0") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:8bad2c479f") { isTransitive = false }
    compileOnly("org.black_ixx:playerpoints:3.2.6") { isTransitive = false }
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.2") { isTransitive = false }
    compileOnly("io.th0rgal:oraxen:1.165.0") { isTransitive = false }
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.0.0") { isTransitive = false }
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.3-beta-14") { isTransitive = false }
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT") { isTransitive = false }
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.12.2") { isTransitive = false }
    compileOnly("com.github.FrancoBM12:API-MagicCosmetics:2.2.7") { isTransitive = false }
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT") { isTransitive = false } // Required by MMOItems API
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT") { isTransitive = false }
    compileOnly("pers.neige.neigeitems:NeigeItems:1.17.24") { isTransitive = false }
    compileOnly("com.willfp:eco:6.71.3") { isTransitive = false }
    compileOnly("com.willfp:EcoItems:5.49.1") { isTransitive = false }
    compileOnly("net.momirealms:craft-engine-core:0.0.22") { isTransitive = false }
    compileOnly("net.momirealms:craft-engine-bukkit:0.0.22") { isTransitive = false }
    compileOnly(files("libs/MeowEco-26.7.0.jar"))
    compileOnly(fileTree("libs"))
}
