package trplugins.menu.module.crafting

import trplugins.menu.api.reaction.Reactions

data class CraftingSpec(
    val inputSlots: List<Int>,
    val resultSlot: Int,
    val recipes: List<CraftingRecipe>
)

data class CraftingRecipe(
    val id: String,
    val shapeless: Boolean,
    val shape: List<String>,
    val ingredients: Map<Char, String>,
    val result: CraftingResult,
    val actions: Reactions
)

data class CraftingResult(
    val material: String,
    val amount: Int
)
