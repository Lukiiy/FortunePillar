package me.lukiiy.fortunePillars

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object Pool {
    private lateinit var validMaterials: List<Material>

    val excluded = setOf(Material.BEDROCK, Material.BARRIER, Material.LIGHT, Material.STRUCTURE_BLOCK, Material.STRUCTURE_VOID, Material.COMMAND_BLOCK, Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK, Material.COMMAND_BLOCK_MINECART, Material.JIGSAW, Material.DEBUG_STICK, Material.KNOWLEDGE_BOOK, Material.SPAWNER)

    fun init() {
        validMaterials = Material.entries.filterNot { !it.isItem || it.isAir || it in excluded }

        FortunePillars.getInstance().logger.info("Setting up item pools...")
    }

    fun nextItem(amount: Int = 1) = ItemStack.of(validMaterials.random(), amount)

    fun nextMaterial(): Material = validMaterials.random()
}