package me.lukiiy.fortunePillars

import me.lukiiy.flow.BaseMinigame
import me.lukiiy.flow.GameHandler

class Timer(var timeSeconds: Int, private val direction: TimerDirection = TimerDirection.DOWN, private val endLimit: Int? = if (direction == TimerDirection.DOWN) 0 else null, private val onTick: (Timer) -> Unit = {}, private val onEnd: () -> Unit = {}) : GameHandler {
    var isPaused = false
    private var internalTickCount = 0
    private val steps = mutableMapOf<Int, () -> Unit>()

    val start = System.currentTimeMillis()

    fun onTime(second: Int, action: () -> Unit): Timer {
        steps[second] = action

        return this
    }

    val formattedTime: String
        get() = String.format("%02d:%02d", timeSeconds / 60, timeSeconds % 60)

    override fun tick(game: BaseMinigame) {
        if (isPaused || ++internalTickCount % 20 != 0) return

        steps[timeSeconds]?.invoke()
        onTick(this)

        if (endLimit != null && timeSeconds == endLimit) {
            onEnd()
            isPaused = true

            return
        }

        if (direction == TimerDirection.DOWN) timeSeconds-- else timeSeconds++
    }
}

enum class TimerDirection { UP, DOWN }