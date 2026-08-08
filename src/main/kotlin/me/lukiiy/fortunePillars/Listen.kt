package me.lukiiy.fortunePillars

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent

class Listen(private val game: Game) : Listener {
    @EventHandler
    fun death(e: PlayerDeathEvent) {
        game.getFlowPlayer(e.player).orElse(null)?.let(game::kill)
    }

    @EventHandler
    fun move(e: PlayerMoveEvent) {
        if (game.freeze) {
            val from = e.from
            val to = e.to

            if (from.x != to.x || from.y != to.y || from.z != to.z) {
                e.isCancelled = true

                e.to = from.clone().apply {
                    yaw = to.yaw
                    pitch = to.pitch
                }
            }

            return
        }
    }
}