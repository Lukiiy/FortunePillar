package me.lukiiy.fortunePillars

import org.bukkit.*
import org.bukkit.generator.ChunkGenerator
import java.util.*

class MapMaker {
    fun create(): World? {
        val creator = WorldCreator("pillars_" + UUID.randomUUID()).generator(VoidGen()).generateStructures(false).environment(World.Environment.NORMAL).type(WorldType.FLAT)

        val world = creator.createWorld()?.apply {
            setGameRule(GameRules.SPAWN_MOBS, false)
            setGameRule(GameRules.ADVANCE_TIME, false)
            setGameRule(GameRules.RANDOM_TICK_SPEED, 0)
            setGameRule(GameRules.ADVANCE_WEATHER, false)
            setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false)
            isAutoSave = false
            viewDistance = 2
            simulationDistance = 2
            spawnLocation = Location(this, 0.0, 64.0, 0.0).toCenterLocation()
        } ?: return null

        return world
    }

    class VoidGen : ChunkGenerator()
}