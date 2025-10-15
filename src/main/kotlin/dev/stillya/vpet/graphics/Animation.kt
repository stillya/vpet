package dev.stillya.vpet.graphics

data class Animation(
	val name: String,
	val sheet: SpriteSheet,
	val loop: Int = 0,
	val onFinish: () -> Unit = {},
	var nextAnimation: Animation? = null,
	val context: AnimationContext? = null,
	val guard: AnimationGuard = AnimationGuard.ALWAYS_VALID
)

const val INFINITE = -1