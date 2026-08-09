package me.lukiiy.fortunePillars

import me.lukiiy.flow.*
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.FUtils.softReset
import me.lukiiy.flow.component.BasePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.util.*

class Game : Minigame() {
    lateinit var world: World

    var freeze = false
    var end = false

    val alive: List<FlowPlayer>
        get() = getPlayers().filterIsInstance<FlowPlayer>().filter { it.state == BasePlayer.State.PLAYING }

    val gameEntry: Entry
        get() = entry as Entry

    private val componentJoinConfig = JoinConfiguration.builder().separator(Component.text(", ")).lastSeparator(Component.text(" and ")).lastSeparatorIfSerial(Component.text(", and ")).build()
    val boards = mutableMapOf<UUID, Board>()

    val timer = Timer(360, TimerDirection.DOWN, onTick = {
        if (it.timeSeconds % gameEntry.itemTimer.value.toInt() == 0) {
            Bukkit.getGlobalRegionScheduler().run(FortunePillars.getInstance()) {
                alive.forEach { a -> a.player.inventory.addItem(Pool.nextItem()) }
            }
        }

        boards.values.forEach { b -> b.update(it.formattedTime, alive.size, getPlayers().size) }
    }, onEnd = {
        end(alive)
    })

    override fun listeners(): List<Listener?> {
        return listOf(Listen(this))
    }

    override fun prepare() {
        world = FortunePillars.getInstance().mapMaker.create() ?: throw MinigameException("An error occurred when creating the world")
    }

    override fun onStart() {
        val players = getPlayers().filterIsInstance<FlowPlayer>()
        val towerSpawn: List<Location> = MapMaker.genPillars(world.spawnLocation, players.size, gameEntry.radius.value.toInt())
        val totalSeconds = 5

        players.zip(towerSpawn).forEach { (it, tower) ->
            val p = it.player

            p.softReset(GameMode.ADVENTURE)
            p.teleportAsync(tower)
            p.setRespawnLocation(world.spawnLocation, true)
            boards[p.uniqueId] = Board(p)
            p.sendMessage(Component.newline().append(" » ".asMini().color(FDefaults.DARK_GRAY)).append("ℹ".asMini().color(FDefaults.YELLOW)).append(" Push your opponents using your random items, but don't fall down!".asMini().color(FDefaults.WHITE)).appendNewline())
        }

        freeze = true

        Countdown(FortunePillars.getInstance(), Duration.ofSeconds(totalSeconds.toLong()), { c ->
            val secondsLeft = (c + 1).toInt()

            val color = when {
                secondsLeft > 3 -> FDefaults.GREEN
                secondsLeft == 2 -> FDefaults.ORANGE
                secondsLeft == 1 -> FDefaults.YELLOW
                else -> FDefaults.RED
            }

            // Action bar countdown :3
            val active = "▌".repeat(secondsLeft)
            val inactive = "▌".repeat((totalSeconds - secondsLeft))

            forEachPlayer {
                it!!.player.apply {
                    sendActionBar("Game Start ".asMini().append("»".asMini().decorate(TextDecoration.BOLD)).appendSpace().append(active.asMini().color(color)).append(inactive.asMini().color(FDefaults.DARK_GRAY)).append(" $secondsLeft".asMini()))
                    playSound(this, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 1f + ((totalSeconds - secondsLeft) * .15f))
                }
            }
        }, {
            freeze = false

            addSystem(timer)

            forEachPlayer {
                it!!.player.apply {
                    sendActionBar(Component.empty())
                    playSound(this, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 2f)

                    gameMode = GameMode.SURVIVAL
                }
            }
        }).start()
    }

    fun kill(player: FlowPlayer) {
        if (player.state != BasePlayer.State.PLAYING) return

        player.state = BasePlayer.State.SPECTATING
        player.player.apply {
            scheduler.runDelayed(FortunePillars.getInstance(), { this.gameMode = GameMode.SPECTATOR }, null, 5L)
        }

        checkWin()
    }

    fun checkWin() {
        val alive: List<FlowPlayer> = alive
        if (alive.size > 1) return

        val winner: FlowPlayer? = if (alive.isEmpty()) null else alive.first()

        end(listOf(winner))
    }

    private fun end(winners: List<FlowPlayer?>?) {
        if (end) return

        removeSystem(timer)
        end = true

        val valid = winners?.filterNotNull() ?: emptyList()

        val winnerComp = if (valid.isEmpty()) "Nobody".asMini() else Component.join(componentJoinConfig, valid.map { it.player.displayName() }.toList())

        Bukkit.getGlobalRegionScheduler().run(FortunePillars.getInstance()) {
            forEachPlayer {
                it.player.sendMessage(Component.newline()
                    .append(" » ".asMini().color(FDefaults.DARK_GRAY))
                    .append("★".asMini().color(FDefaults.LIME)).appendSpace()
                    .append(winnerComp).appendSpace()
                    .append("won!".asMini().color(FDefaults.LIME))
                    .appendNewline())

                boards[it.player.uniqueId]?.destroy()
            }
        }

        Bukkit.getGlobalRegionScheduler().runDelayed(FortunePillars.getInstance(), {
            stop()
        }, 100L)
    }

    override fun onStop() {
        Flow.getInstance().manager.lobby?.let { forEachPlayer { fp -> it.sendToLobby(fp!!) } }

        val wFolder = world.worldFolder

        Bukkit.getServer().unloadWorld(world, false)

        if (wFolder.exists()) {
            try {
                Files.walk(wFolder.toPath()).use {
                    it.sorted(Comparator.reverseOrder()).forEach { p -> p.toFile().delete() }
                }
            } catch (e: IOException) {
                FortunePillars.getInstance().logger.severe("Could not delete instanced world " + wFolder.getName() + "! " + e.message)
            }
        }
    }

    class Board(player: Player) {
        private val board = OnlyBoard(player, "Pillars".asMini().color(FDefaults.YELLOW).decorate(TextDecoration.BOLD))

        fun update(formattedTime: String, alivePlayers: Int, totalPlayers: Int) {
            board.updateLines(
                Component.empty().append("Time: ".asMini().color(FDefaults.DARK_BLUE)).append(formattedTime.asMini()),
                Component.empty().append("Players: ".asMini().color(FDefaults.DARK_BLUE)).append("$alivePlayers/$totalPlayers".asMini())
            )
        }

        fun destroy() = board.destroy()
    }
}