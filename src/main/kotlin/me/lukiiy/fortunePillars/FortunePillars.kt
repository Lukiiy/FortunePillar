package me.lukiiy.fortunePillars

import me.lukiiy.flow.Flow
import org.bukkit.plugin.java.JavaPlugin

class FortunePillars : JavaPlugin() {
    val mapMaker = MapMaker()

    override fun onEnable() {
        Flow.getInstance().manager.register(Entry())
    }

    companion object {
        fun getInstance(): FortunePillars = getPlugin(FortunePillars::class.java)
    }
}
