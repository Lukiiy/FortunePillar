package me.lukiiy.fortunePillars

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object Pool {
    private val validMaterials: List<Material>

    val excluded = setOf(Material.BEDROCK, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART, Material.JIGSAW, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK, Material.SPAWNER)

    init {
        validMaterials = Material.entries.filterNot { !it.isItem || it.isAir || it in excluded }
    }

    fun nextItem(amount: Int = 1): ItemStack = ItemStack(validMaterials.random(), amount)
    fun nextMaterial(): Material = validMaterials.random()
}