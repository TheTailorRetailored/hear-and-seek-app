package com.bridgesdigital.hearandseek.game

enum class GameMode(
    val title: String,
    val durationLabel: String,
    val durationMillis: Long,
    val hideSeconds: Int,
) {
    QUICK(
        title = "Quick",
        durationLabel = "45 sec",
        durationMillis = 45_000L,
        hideSeconds = 10,
    ),
    CLASSIC(
        title = "Classic",
        durationLabel = "1 min 30",
        durationMillis = 90_000L,
        hideSeconds = 15,
    ),
    LONG(
        title = "Long",
        durationLabel = "3 min",
        durationMillis = 180_000L,
        hideSeconds = 20,
    ),
    MARATHON(
        title = "Marathon",
        durationLabel = "5 min",
        durationMillis = 300_000L,
        hideSeconds = 30,
    ),
}
