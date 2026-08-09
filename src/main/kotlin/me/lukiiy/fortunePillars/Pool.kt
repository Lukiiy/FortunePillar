package me.lukiiy.fortunePillars

import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack

object Pool {
    val validMaterials: List<Material> by lazy {
        Registry.MATERIAL.filter { it.isItem && !it.isAir && it !in excluded }.toList()
    }

    val excluded = setOf(Material.BEDROCK, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART, Material.JIGSAW, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK, Material.SPAWNER)

    fun init() {
        FortunePillars.getInstance().logger.info("Setting up item pools...")
    }

    fun nextItem(amount: Int = 1) = ItemStack.of(validMaterials.random(), amount)

    fun nextMaterial(): Material = validMaterials.random()
}