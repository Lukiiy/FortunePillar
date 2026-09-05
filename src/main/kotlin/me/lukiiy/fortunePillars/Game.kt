package me.lukiiy.fortunePillars

import io.papermc.paper.entity.LookAnchor
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import me.lukiiy.flow.*
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.FUtils.softReset
import me.lukiiy.flow.component.BasePlayer
import me.lukiiy.fortunePillars.arena.MapBounds
import me.lukiiy.fortunePillars.arena.MapMaker
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.Openable
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.Rotatable
import org.bukkit.block.data.Snowable
import org.bukkit.block.data.Waterlogged
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import java.io.IOException
import java.nio.file.Files
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class Game : Minigame() {
    lateinit var world: World
    var bounds: MapBounds? = null

    var freeze = false
    var end = false

    val alive: List<FlowPlayer>
        get() = getPlayers().filterIsInstance<FlowPlayer>().filter { it.state == BasePlayer.State.PLAYING }

    val gameEntry: Entry
        get() = entry as Entry

    private val random = ThreadLocalRandom.current()

    private val componentJoinConfig = JoinConfiguration.builder().separator(Component.text(", ")).lastSeparator(Component.text(" and ")).lastSeparatorIfSerial(Component.text(", and ")).build()
    val boards = mutableMapOf<UUID, Board>()

    val timer = Timer(360, TimerDirection.DOWN, onTick = {
        if (it.timeSeconds % gameEntry.itemTimer.value.toInt() == 0) {
            Bukkit.getGlobalRegionScheduler().run(FortunePillars.getInstance()) {
                val item = if (gameEntry.same4all.value) Pool.nextItem() else null

                alive.forEach { a ->
                    a.player.inventory.addItem(item ?: Pool.nextItem())
                }
            }
        }

        if (gameEntry.ablockalypse.value) {
            alive.forEach { fp ->
                val player = fp.player
                if (!player.isOnline || player.gameMode == GameMode.SPECTATOR) return@forEach

                val loc = player.location

                // 6x6x6 cube around the player !! !
                val x = loc.blockX + random.nextInt(-3, 3)
                val y = loc.blockY + random.nextInt(-3, 3)
                val z = loc.blockZ + random.nextInt(-3, 3)

                val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                if (bounds?.contains(location) != true || !location.block.isEmpty) return@forEach

                val type = Pool.validBlocks.random()
                val data = type.createBlockData()

                randomizeBlockData(data)

                location.block.apply {
                    this.type = type

                    setBlockData(data, false)
                }
            }
        }

        boards.values.forEach { b -> b.update(it.formattedTime) }
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
        val spawn = world.spawnLocation
        val towerSpawn: List<Location> = MapMaker.genPillars(spawn, players.size, gameEntry.radius.value.toInt())

        bounds = MapMaker.getBounds(spawn, players.size, gameEntry.radius.value.toInt() + 8)

        val totalSeconds = 5

        players.zip(towerSpawn).forEach { (it, tower) ->
            val p = it.player

            p.teleportAsync(tower).thenAccept {
                p.scheduler.run(FortunePillars.getInstance(), { p.softReset(GameMode.ADVENTURE) }, null)
                p.lookAt(spawn, LookAnchor.EYES)
            }

            boards[p.uniqueId] = Board(p)
            p.sendMessage(Component.newline().append(" » ".asMini().color(FDefaults.DARK_GRAY)).append("ℹ".asMini().color(FDefaults.YELLOW)).append(" Push your opponents using your random items, but don't fall down!".asMini().color(FDefaults.WHITE)).appendNewline())
            p.setRespawnLocation(spawn, true)
        }

        freeze = true

        Countdown(FortunePillars.getInstance(), Duration.ofSeconds(totalSeconds.toLong()), { c ->
            val secondsLeft = (c + 1).toInt()

            val color = when {
                secondsLeft > 3 -> FDefaults.GREEN
                secondsLeft == 2 -> FDefaults.ORANGE
                secondsLeft == 1 -> FDefaults.RED
                else -> FDefaults.YELLOW
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
        bounds = null

        val valid = winners?.filterNotNull() ?: emptyList()
        val winnerComp = if (valid.isEmpty()) "Nobody".asMini() else Component.join(componentJoinConfig, valid.map { it.player.displayName() }.toList())

        forEachPlayer { it.player.apply {
            if (valid.contains(it)) {
                allowFlight = true
                isFlying = true

                showTitle(Title.title("★".asMini().color(FDefaults.YELLOW).append(" You won! ".asMini().color(FDefaults.LIME)).append("★".asMini().color(FDefaults.YELLOW)), Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))))
                playSound(this, Sound.UI_TOAST_CHALLENGE_COMPLETE, .75f, 1f)
            } else {
                showTitle(Title.title("Game Over!".asMini().color(FDefaults.RED), Component.empty(), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))))
                playSound(this, Sound.ENTITY_GENERIC_EXPLODE, .75f, .5f)
            }
        } }

        var winnerEffect: ScheduledTask? = Bukkit.getGlobalRegionScheduler().runAtFixedRate(FortunePillars.getInstance(), {
            valid.forEach {
                val player = it.player
                if (!player.isOnline || player.gameMode == GameMode.SPECTATOR) return@forEach

                val loc = player.location

                loc.world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0.0, 1.0, 0.0), 2, 0.5, 1.5, 0.5, 0.25)
            }
        }, 1L, 2L)

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
            winnerEffect?.cancel()
            winnerEffect = null

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

        fun update(formattedTime: String) = board.updateLines(Component.empty().append("Time: ".asMini().color(FDefaults.DARK_BLUE)).append(formattedTime.asMini()))

        fun destroy() = board.destroy()
    }

    private fun randomizeBlockData(data: BlockData) {
        when (data) {
            is Directional -> data.facing = data.faces.random()
            is Rotatable -> data.rotation = BlockFace.entries.random()
            is Ageable -> data.age = random.nextInt(data.maximumAge)
            is Levelled -> data.level = random.nextInt(data.minimumLevel, data.maximumLevel + 1)
            is Powerable -> data.isPowered = random.nextBoolean()
            is Openable -> data.isOpen = random.nextBoolean()
            is Waterlogged -> data.isWaterlogged = random.nextBoolean()
            is Snowable -> data.isSnowy = random.nextBoolean()
            is Bisected -> data.half = Bisected.Half.entries.random()
        }
    }
}