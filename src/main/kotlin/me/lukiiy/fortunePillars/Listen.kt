package me.lukiiy.fortunePillars

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent

class Listen(private val game: Game) : Listener {
    @EventHandler
    fun death(e: PlayerDeathEvent) {
        game.getFlowPlayer(e.player).orElse(null)?.let(game::kill)
    }

    @EventHandler
    fun move(e: PlayerMoveEvent) {
        val fp = game.getFlowPlayer(e.player)?.get() ?: return

        if (game.freeze) {
            val from = e.from
            val to = e.to

            if (from.x != to.x || from.z != to.z) {
                e.isCancelled = true

                e.to = from.clone().apply {
                    yaw = to.yaw
                    pitch = to.pitch
                }
            }

            return
        }

        if (e.to.y < 28.0 && !game.end) game.kill(fp)
    }

    @EventHandler(ignoreCancelled = true)
    fun blockFromTo(e: BlockFromToEvent) {
        if (e.toBlock.y < 28) e.isCancelled = true
    }
}