package me.lukiiy.fortunePillars

import me.lukiiy.flow.FDefaults
import me.lukiiy.flow.FUtils.asMini
import me.lukiiy.flow.GameEntry
import java.util.function.Supplier

class Entry : GameEntry("fortunepillars", "Fortune Pillars", Supplier { Game() }, "Fortune Pillars".asMini().color(FDefaults.ORANGE))