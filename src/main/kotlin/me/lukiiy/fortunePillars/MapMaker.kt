package me.lukiiy.fortunePillars

import org.bukkit.*
import org.bukkit.generator.ChunkGenerator
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class MapMaker {
    fun create(): World? {
        val creator = WorldCreator("pillars_" + UUID.randomUUID()).generator(VoidGen()).generateStructures(false).environment(World.Environment.NORMAL).type(WorldType.FLAT)

        val world = creator.createWorld()?.apply {
            setGameRule(GameRules.SPAWN_MOBS, false)
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

    companion object {
        fun genPillars(center: Location, totalPoints: Int, initialRadius: Int): List<Location> {
            val world = center.world ?: return emptyList()
            val top = ArrayList<Location>(totalPoints)
            var pointsRemaining = totalPoints
            var currentIdx = 0
            val pi2 = PI * 2

            while (pointsRemaining > 0) {
                val ringCapacity = 8 + (currentIdx * 4)
                val ringPoints = minOf(pointsRemaining, ringCapacity)
                val currentRadius = initialRadius + (currentIdx * 8) // ring spacing!
                val angleStep = pi2 / ringPoints

                repeat(ringPoints) {
                    val angle = it * angleStep

                    // calculate coords
                    val x = (center.x + floor(currentRadius * cos(angle))).toInt()
                    val z = (center.z + floor(currentRadius * sin(angle))).toInt()

                    for (y in 64..96) world.getBlockAt(x, y, z).setType(Material.BEDROCK, false)

                    top.add(Location(world, x + .5, 97.0, z + .5))
                }

                pointsRemaining -= ringPoints
                currentIdx++
            }

            return top
        }
    }
}