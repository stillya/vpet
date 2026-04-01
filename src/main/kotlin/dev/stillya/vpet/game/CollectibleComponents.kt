package dev.stillya.vpet.game

data class Collectible(val value: Int = 1)

data class BugVisual(val color: BugColor)

enum class BugColor { RED, BLUE, GREEN }
