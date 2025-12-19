package trplugins.menu.module.internal.data

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import taboolib.common.LifeCycle
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.Schedule
import taboolib.common.platform.function.submitAsync
import taboolib.common5.cdouble
import taboolib.common5.cint
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.util.sendLang
import trplugins.menu.TrMenu
import trplugins.menu.TrMenu.SETTINGS
import trplugins.menu.api.event.CustomDatabaseEvent
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.internal.database.DatabaseSQL
import trplugins.menu.module.internal.database.DatabaseSQLite
import trplugins.menu.module.internal.database.GlobalDataDao
import trplugins.menu.module.internal.database.MetaDataDao
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Arasple
 * @date 2021/1/27 11:46
 *
 * differences:
 *
 * <arguments> -> for each trplugins.menu session
 * <meta> -> only lost when the server is shut down
 * <data> -> storable, (support MongoDB)
 */
@RuntimeDependency(
    value = "!org.slf4j:slf4j-jdk14:2.0.8",
    test = "!org.slf4j_2_0_8.jul.JULServiceProvider",
    relocate = ["!org.slf4j", "!org.slf4j_2_0_8"],
    transitive = false
)
object Metadata {

    internal val meta = mutableMapOf<String, DataMap>()
    internal val data = mutableMapOf<String, DataMap>()
    internal val global = ConcurrentHashMap<String, Any?>()

    @Config("data/globalData.yml")
    lateinit var globalData: Configuration

    val isUseLegacy by lazy {
        SETTINGS.getBoolean("Database.Use-Legacy-Database", def = false)
    }

    // Copy in the Adyeshach
    val database by lazy {
        if (!isUseLegacy) return@lazy null
        when (val db = SETTINGS.getString("Database.Method")?.uppercase()) {
            "LOCAL", "SQLITE", null -> DatabaseSQLite()
            "SQL" -> DatabaseSQL()
            "MONGODB" -> TODO("Deprecated")
            else -> {
                val event = CustomDatabaseEvent(db)
                event.call()
                event.database ?: error("\"${SETTINGS.getString("Database.Method")}\" not supported.")
            }
        }
    }

    @Awake(LifeCycle.ENABLE)
    fun init() {
        submitAsync(period = SETTINGS.getLong("Database.Global-Data-Sync")) {
            loadGlobalData()
        }
    }

    //    @TFunction.Cancel 暂不处理
    @Awake(LifeCycle.DISABLE)
    @Schedule(delay = 100, period = 20 * 30, async = true)
    fun save() {
        if (!isUseLegacy) {
            return
        }
        data.forEach { (playerName, dataMap) ->
            val player = Bukkit.getPlayerExact(playerName) ?: return@forEach
            pushData(player, dataMap)
        }
        globalData.saveToFile()
    }

    fun saveData(player: Player, key: String) {
        submitAsync {
            MetaDataDao.door.update(DataEntity.constructor(player, key, getData(player)[key]?.toString() ?: ""))
        }
    }

    fun pushData(player: Player, dataMap: DataMap = getData(player)) {
        if (isUseLegacy) {
            getLocalePlayer(player)?.let {
                it.getConfigurationSection("TrMenu.Data")?.getKeys(true)?.forEach { key ->
                    if (!dataMap.data.containsKey(key)) {
                        it["TrMenu.Data.$key"] = null
                    }
                }
                dataMap.data.forEach { (key, value) -> it["TrMenu.Data.$key"] = value }
            }
            database?.push(player)
        } else {
            dataMap.data.forEach { (key, value) ->
                MetaDataDao.door.update(DataEntity.constructor(player, key, value?.toString() ?: ""))
            }
        }
    }

    private fun getLocalePlayer(player: Player): Configuration? {
        return database?.pull(player)
    }

    fun loadData(player: Player) {
        val map: MutableMap<String, Any?> = mutableMapOf()

        if (isUseLegacy) {
            getLocalePlayer(player)?.getConfigurationSection("TrMenu.Data")?.let { section ->
                section.getKeys(true).forEach { key -> map[key] = section[key] }
            }
        } else {
            MetaDataDao.door.get(player.uniqueId).forEach {
                map[it.key] = it.data
            }
        }

        data[player.name] = DataMap(map)
    }

    fun <T> getData(target: T): DataMap {
        return data.computeIfAbsent(getPlayerName(target)) { DataMap() }
    }

    fun <T> getMeta(target: T): DataMap {
        return meta.computeIfAbsent(getPlayerName(target)) { DataMap() }
    }

    fun loadGlobalData() {
        if (isUseLegacy) {
            return
        }
        GlobalDataDao.door.get().forEach {
            global[it.key] = it.data
        }
    }

    fun getGlobalData(key: String): Any? {
        return if (isUseLegacy) globalData[key] else global[key]
    }

    fun getGlobalDataKeys(): Set<String> {
        return if (isUseLegacy) globalData.getKeys(true) else global.keys
    }

    fun setGlobalData(key: String, value: Any?) {
        if (isUseLegacy) {
            globalData[key] = value
        } else {
            global[key] = value
            submitAsync {
                GlobalDataDao.door.update(key, value)
            }
        }
    }

    fun setBukkitMeta(player: Player, key: String, value: String = "") {
        player.setMetadata(key, FixedMetadataValue(TrMenu.plugin, value))
    }

    fun byBukkit(player: Player, key: String): Boolean {
        return player.hasMetadata(key).also {
            if (it) player.removeMetadata(key, TrMenu.plugin)
        }
    }

    private fun <T> getPlayerName(target: T): String {
        return when (target) {
            is Player -> target.name
            is ProxyPlayer -> target.name
            is MenuSession -> target.placeholderPlayer.name
            else -> throw Exception("Unknown target type.")
        }
    }


    fun modifyData(
        player: Player,
        modifyType: ModifyType,
        dataType: DataType,
        dataName: String,
        value: String,
        sender: CommandSender
    ) {
        val data = getData(player, dataType, dataName)
        when (modifyType) {
            ModifyType.ADD -> {
                setData(player, dataType, dataName, calculate(data, value))
            }

            ModifyType.REMOVE -> {
                setData(player, dataType, dataName, null)
            }

            ModifyType.SET -> {
                setData(player, dataType, dataName, value)
            }

            ModifyType.GET -> {
                sender.sendLang(
                    "Command-Data-Get",
                    player.name,
                    dataType,
                    dataName,
                    data.toString()
                )
            }
        }
    }

    fun setData(player: Player, dataType: DataType, dataName: String, value: Any?) {
        when (dataType) {
            DataType.DATA -> {
                getData(player)[dataName] = value
                if (!isUseLegacy) {
                    saveData(player, dataName)
                }
            }

            DataType.META -> {
                getMeta(player)[dataName] = value
            }

            DataType.GLOBAL -> setGlobalData(dataName, value)
        }
    }

    fun getData(player: Player, dataType: DataType, dataName: String): Any? {
        return when (dataType) {
            DataType.DATA -> getData(player)[dataName]
            DataType.META -> getMeta(player)[dataName]
            DataType.GLOBAL -> getGlobalData(dataName)
        }
    }

    fun calculate(preValue: Any?, value: String): Any {
        val valueIsInt = value.toIntOrNull() != null
        return if ((preValue?.toString()?.toIntOrNull() != null || preValue == null) && valueIsInt) {
            preValue.cint + value.cint
        } else {
            preValue.cdouble + value.cdouble
        }
    }

    enum class DataType {
        DATA, META, GLOBAL
    }

    enum class ModifyType {
        ADD, REMOVE, SET, GET
    }

}