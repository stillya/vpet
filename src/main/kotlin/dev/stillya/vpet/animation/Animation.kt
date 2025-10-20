package dev.stillya.vpet.animation

import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.SpriteSheet

data class Animation(
	val name: String,
	val sheet: SpriteSheet,
	val loop: Int = 0,
	val onFinish: () -> Unit = {},
	var nextAnimation: Animation? = null,
	val context: AnimationContext? = null,
	val guard: AnimationGuard = AnimationGuard.ALWAYS_VALID
)

enum class AnimationState {
	IDLE,
	WALKING,
	RUNNING,
	CELEBRATING,
	FAILED,
	OCCASION,
	OBSERVING
}
