package me.lukiiy.fortunePillars.arena

import org.bukkit.Location
import kotlin.math.absoluteValue

data class MapBounds(val center: Location, val radius: Int, val maxY: Int) {
    operator fun contains(location: Location): Boolean = (location.blockX - center.blockX).absoluteValue <= radius && (location.blockZ - center.blockZ).absoluteValue <= radius && location.blockY <= maxY
}