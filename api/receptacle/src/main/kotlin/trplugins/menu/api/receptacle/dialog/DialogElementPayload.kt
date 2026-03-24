package trplugins.menu.api.receptacle.dialog

import org.bukkit.inventory.ItemStack

data class DialogElementPayload(
    val id: String,
    val type: String,
    val label: String? = null,
    val text: List<String> = emptyList(),
    val width: Int? = null,
    val item: ItemStack? = null,
    val placeholder: String? = null,
    val options: List<DialogOptionPayload> = emptyList(),
    val value: String? = null,
    val boolValue: Boolean? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null
)
