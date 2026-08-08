package me.lukiiy.fortunePillars

import io.papermc.paper.scoreboard.numbers.NumberFormat
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Team

// A very small hardcoded in-lib to do the scoreboard
class OnlyBoard(val player: Player, title: Component) {
    private val scoreboard = Bukkit.getScoreboardManager().newScoreboard
    private val objective = scoreboard.registerNewObjective("pillar_sb", Criteria.DUMMY, title).apply {
        displaySlot = DisplaySlot.SIDEBAR

        numberFormat(NumberFormat.blank())
    }

    private val teams = HashMap<Int, Team>()

    init {
        player.scoreboard = scoreboard
    }

    fun setTitle(title: Component) = objective.displayName(title)

    fun updateLines(vararg lines: Component) {
        val totalLines = lines.size

        lines.forEachIndexed { idx, component ->
            val scoreIndex = totalLines - idx // Scores are ordered descending on sidebars

            val team = teams.computeIfAbsent(scoreIndex) { idx ->
                val entryAnchor = "§" + idx.toString(16) // Invisible color code anchor
                val newTeam = scoreboard.registerNewTeam("line_$idx")

                newTeam.addEntry(entryAnchor)
                objective.getScore(entryAnchor).score = idx
                newTeam
            }

            team.prefix(component)
        }

        val iterator = teams.iterator() // Clean up excess lines if line count shrank

        while (iterator.hasNext()) {
            val (scoreIdx, team) = iterator.next()

            if (scoreIdx > totalLines) {
                val entry = team.entries.firstOrNull() ?: continue

                scoreboard.resetScores(entry)
                team.unregister()
                iterator.remove()
            }
        }
    }

    fun destroy() {
        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }
}