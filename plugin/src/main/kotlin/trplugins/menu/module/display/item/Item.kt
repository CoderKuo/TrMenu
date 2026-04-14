package trplugins.menu.module.display.item

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import taboolib.library.xseries.XMaterial
import taboolib.platform.util.buildItem
import taboolib.platform.util.isAir
import trplugins.menu.api.menu.IItem
import trplugins.menu.module.display.MenuSession
import trplugins.menu.module.display.texture.Texture
import trplugins.menu.util.bukkit.ItemHelper.defColorize
import trplugins.menu.util.collections.CycleList

/**
 * @author Arasple
 * @date 2021/1/25 10:48
 */
open class Item(
    val texture: CycleList<Texture>,
    val name: CycleList<String>,
    val lore: CycleList<Lore>,
    val meta: Meta
) : IItem {

    val nameI18n = HashMap<String, CycleList<String>>()

    val loreI18n = HashMap<String, CycleList<Lore>>()

    internal val cache = object: HashMap<Int, ItemStack>() {
        override fun put(key: Int, value: ItemStack): ItemStack? {
            if (value.type == XMaterial.PLAYER_HEAD.get()) {
                return super.put(key, value.clone())
            }
            return super.put(key, value)
        }
    }

    fun addI18nName(locale: String, name: CycleList<String>) {
        nameI18n[locale] = name
    }

    fun addI18nLore(locale: String, lore: CycleList<Lore>) {
        loreI18n[locale] = lore
    }

    fun name(session: MenuSession): CycleList<String> {
        if (nameI18n.isEmpty()) {
            return name
        }
        return nameI18n[session.locale] ?: name
    }

    fun lore(session: MenuSession): CycleList<Lore> {
        if (loreI18n.isEmpty()) {
            return lore
        }
        return loreI18n[session.locale] ?: lore
    }

    private fun parsedName(session: MenuSession) = name(session).current(session.id)?.let { defColorize(session.parse(it)) }

    private fun parsedLore(session: MenuSession) =
        lore(session).current(session.id)?.parse(session)?.map { defColorize(it, true) }

    fun get(session: MenuSession): ItemStack {
        return if (cache.containsKey(session.id)) cache[session.id]!!
        else build(session)
    }

    override fun generate(
        session: MenuSession,
        texture: Texture,
        name: String?,
        lore: List<String>?,
        meta: Meta
    ): ItemStack {
        val item = texture.generate(session)

        if (item.isAir) {
            return item
        }

        val itemStack = buildItem(item) {
            if (item.itemMeta != null) {
                name?.let { this.name = it }
                lore?.let { this.lore.addAll(it) }
            }
            meta.flags(this)
            meta.shiny(session, this)
            meta.tooltipStyle(session, this)
            meta.itemModel(session, this)
            meta.hideTooltip(session, this)
            meta.unbreakable(session, this)
            meta.data(session, this)

            if (meta.hasAmount()) this.amount = meta.amount(session)
        }

        meta.nbt(session, itemStack)?.run {
            itemStack.itemMeta = this
        }
        meta.enchants(session, itemStack)

        return itemStack
    }

    private fun build(
        session: MenuSession,
        name: String? = parsedName(session),
        lore: List<String>? = parsedLore(session)
    ): ItemStack {
        val item = generate(session, texture.current(session.id)!!, name, lore, meta)
        cache[session.id] = item
        return item
    }

    override fun updateTexture(session: MenuSession) {
        texture.cycleIndex(session.id)

        if (!cache.containsKey(session.id))
            build(session)
        else if (cache[session.id]!!.itemMeta == null)
            build(session)
        else
            cache[session.id]!!.itemMeta?.let {
                build(session, it.displayName, it.lore ?: listOf())
            }
    }

    override fun updateName(session: MenuSession) {
        name(session).cycleIndex(session.id)

        if (!cache.containsKey(session.id))
            build(session)
        else {
            val current = cache[session.id]
            try {
                val new = buildItem(current!!) { parsedName(session)?.let { name = it } }
                cache[session.id] = new
            } catch (t: Throwable) {
                t.stackTrace
            }
        }
    }

    override fun updateLore(session: MenuSession) {
        lore(session).cycleIndex(session.id)

        if (!cache.containsKey(session.id)) {
            build(session)
        } else {
            val current = cache[session.id]
            if (current != null && current.type != Material.AIR) {
                val new = buildItem(current) {
                    lore.clear()
                    lore.addAll(parsedLore(session) ?: listOf())
                }
                cache[session.id] = new
            }
        }
    }

}