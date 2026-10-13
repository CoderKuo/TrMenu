package trplugins.menu.module.internal.database

import taboolib.expansion.db
import taboolib.expansion.persistentContainer
import trplugins.menu.TrMenu
import trplugins.menu.module.internal.data.DataEntity
import java.util.*

class MetaDataDao {

    private val table = "${TrMenu.SETTINGS.getString("Database.SQL.prefix", "trmenu")}_data".lowercase()

    private val container by lazy {
        persistentContainer(type = db("settings.yml", "Database.SQL")) { new<DataEntity>(table) }
    }

    fun update(entity: DataEntity) {
        val containerx = container[table]
        val old = get(entity.user, entity.key)
        if (old == null) {
            containerx.insert(listOf(entity))
        } else {
            val tablex = containerx.table
            val dataSource = containerx.dataSource
            tablex.update(dataSource) {
                where("user" eq entity.user.toString())
                where("key" eq entity.key)
                set("data", entity.data)
            }

        }
    }

    fun get(uuid: UUID): List<DataEntity> {
        return container[table].get<DataEntity?> {
            "user" eq uuid.toString()
        }.filter { it?.data?.isNotEmpty() == true }.mapNotNull { it }
    }

    fun delete(uuid: UUID, key: String) {
        val containerx = container[table]
        containerx.table.delete(containerx.dataSource) {
            where {
                "user" eq uuid.toString()
                "key" eq key
            }
        }
    }

    fun get(uuid: UUID, key: String): DataEntity? {
        return container[table].getOne<DataEntity?> {
            "user" eq uuid.toString()
            "key" eq key
        }
    }

    companion object {
        val door by lazy { MetaDataDao() }
    }
}