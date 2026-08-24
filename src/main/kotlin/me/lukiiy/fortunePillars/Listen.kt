package me.lukiiy.fortunePillars

import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.block.BlockPlaceEvent
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
        val p = e.player

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

        if (e.to.y < 28.0 && !game.end) p.damage(p.health, DamageSource.builder(DamageType.OUT_OF_WORLD).build())

        game.bounds?.let { if (!it.isInBoundsHorizontally(e.to)) p.velocity = p.world.spawnLocation.toVector().subtract(p.location.toVector()).normalize().multiply(.15).setY(-1) }
    }

    @EventHandler(ignoreCancelled = true)
    fun blockFromTo(e: BlockFromToEvent) {
        if (e.toBlock.y < 28) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun blockPlace(e: BlockPlaceEvent) {
        if (game.bounds?.isInBoundsVertically(e.block.location) ?: false) return

        e.isCancelled = true
        e.player.sendMessage("⚠ You've reached the height limit!".asMini().color(FDefaults.RED))
    }
}