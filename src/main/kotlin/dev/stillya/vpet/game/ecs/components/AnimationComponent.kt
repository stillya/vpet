package dev.stillya.vpet.game.ecs.components

class AnimationComponent(
	val resourceId: String,
	var currentFrame: Int = 0,
	var elapsed: Float = 0f
)
