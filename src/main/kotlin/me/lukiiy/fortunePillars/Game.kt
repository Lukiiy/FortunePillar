package me.lukiiy.fortunePillars

import me.lukiiy.flow.*
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.FUtils.softReset
import me.lukiiy.flow.component.BasePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.event.Listener
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class Game : Minigame() {
    lateinit var world: World

    var freeze = true
    var end = false
    var start = 0

    val alive: List<FlowPlayer>
        get() = getPlayers().filterIsInstance<FlowPlayer>().filter { it.state == BasePlayer.State.PLAYING }

    val gameEntry: Entry
        get() = entry as Entry

    private val componentJoinConfig = JoinConfiguration.builder().separator(Component.text(", ")).lastSeparator(Component.text(" and ")).lastSeparatorIfSerial(Component.text(", and ")).build()

    private val timer = Timer(360, TimerDirection.DOWN, onTick = {
        if (it.timeSeconds % 3 == 0) {
            TODO("item")
        }
    }, onEnd = {
        TODO("end")
    })

    override fun listeners(): List<Listener?> {
        return listOf(Listen(this))
    }

    override fun prepare() {
        world = FortunePillars.getInstance().mapMaker.create() ?: throw MinigameException("An error occurred when creating the world")
    }

    override fun onStart() {
        val players = getPlayers().filterIsInstance<FlowPlayer>()
        val towerSpawn: List<Location> = MapMaker.genPillars(world.spawnLocation, players.size, 12)
        val totalSeconds = 5

        players.zip(towerSpawn).forEach { (it, tower) ->
            val p = it.player

            p.softReset(GameMode.ADVENTURE)
            p.teleportAsync(tower)
            p.setRespawnLocation(world.spawnLocation, true)
        }

        freeze = true

        Countdown(FortunePillars.getInstance(), Duration.ofSeconds(5), { c ->
            var color = FDefaults.GREEN
            if (c < 4) color = FDefaults.YELLOW
            if (c < 2) color = FDefaults.RED

            forEachPlayer { it!!.player.showTitle(Title.title(Component.text("Starting in").color(FDefaults.GRAY), Component.text((c + 1).toString() + " seconds!").color(color), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1)))) }
        }, {
            freeze = false

            forEachPlayer { it!!.player.sendMessage(Component.newline().append(" » ".asMini().color(FDefaults.DARK_GRAY)).append("Push your opponents using your random items, but don't fall down!".asMini().color(FDefaults.TEAL)).appendNewline()) }
        }).start()

        Countdown(FortunePillars.getInstance(), Duration.ofSeconds(totalSeconds.toLong()), { c ->
            val secondsLeft = (c + 1).toInt()

            val color = when {
                secondsLeft > 3 -> FDefaults.GREEN
                secondsLeft > 1 -> FDefaults.YELLOW
                else -> FDefaults.RED
            }

            // Action bar countdown :3
            val active = "▌".repeat(secondsLeft)
            val inactive = "▌".repeat((totalSeconds - secondsLeft))

            forEachPlayer {
                it!!.player.apply {
                    sendActionBar("Game Start ".asMini().append("»".asMini().decorate(TextDecoration.BOLD)).appendSpace().append(active.asMini().color(color)).append(inactive.asMini().color(FDefaults.DARK_GRAY)).append(" $secondsLeft".asMini()))
                    playSound(this, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 1f + ((totalSeconds - secondsLeft) * .15f))

                    if (secondsLeft == 3) sendMessage(Component.newline().append(" » ".asMini().color(FDefaults.DARK_GRAY)).append("Push your opponents using your random items, but don't fall down!".asMini().color(FDefaults.TEAL)).appendNewline())
                }
            }
        }, {
            freeze = false

            addSystem(timer)

            forEachPlayer {
                it!!.player.apply {
                    sendActionBar(Component.empty())
                    playSound(this, Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 2f)
                }
            }
        }).start()
    }

    fun kill(player: FlowPlayer) {
        if (player.state != BasePlayer.State.PLAYING) return

        player.state = BasePlayer.State.SPECTATING

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

        val winnerComp = if (winners.isNullOrEmpty()) Component.text("Nobody") else Component.join(componentJoinConfig, winners.filterIsInstance<FlowPlayer>().map { it.player.displayName() }.toList())

        Bukkit.getGlobalRegionScheduler().run(FortunePillars.getInstance()) {
            forEachPlayer { it.player.sendMessage(Component.newline()
                .append(" » ".asMini().color(FDefaults.DARK_GRAY))
                .append("★".asMini().color(FDefaults.LIME))
                .append(winnerComp).appendSpace()
                .append("won!".asMini().color(FDefaults.LIME))
                .appendNewline()) }
        }
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
}