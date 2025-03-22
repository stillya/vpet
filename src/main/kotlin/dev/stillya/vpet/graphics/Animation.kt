package dev.stillya.vpet.graphics

data class Animation(
	val name: String,
	val sheet: SpriteSheet,
	var loop: Int = 0,
	val onFinish: () -> Unit = {},
)

const val INFINITE = -1