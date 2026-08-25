package me.lukiiy.fortunePillars

import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.GameEntry
import me.lukiiy.flow.setting.BooleanSetting
import me.lukiiy.flow.setting.DoubleSetting
import java.util.function.Supplier

class Entry : GameEntry("fortunepillars", "Fortune Pillars", Supplier { Game() }, "Fortune Pillars".asMini().color(FDefaults.ORANGE)) {
    val itemTimer: DoubleSetting = setting(DoubleSetting("itemTimer", "Random Item Period", "", 1.0, 20.0, 3.0, 1.0)) // TODO
    val radius = setting(DoubleSetting("radius", "Radius", "", 2.0, 24.0, 12.0, 1.0))
    val same4all = setting(BooleanSetting("same", "Same for everyone", "", false))
    val ablockalypse = setting(BooleanSetting("ablockalypse", "Ablockalypse", "", false))
}