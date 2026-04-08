repositories {
    maven("https://repo.aeoliancloud.com/repository/releases") { isAllowInsecureProtocol = true }
}

dependencies {
    compileOnly(project(":common"))
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v260100:260100")
    compileOnly("dependencies.core:paper:12104-min")
    compileOnly("ink.ptms.core:v12002:12002-minimize:universal")
    compileOnly("ink.ptms.core:v12002:12002-minimize:mapped")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")
    compileOnly(fileTree("libs"))
}

taboolib { subproject = true }