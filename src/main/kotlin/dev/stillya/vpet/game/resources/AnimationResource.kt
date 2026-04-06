package dev.stillya.vpet.game.resources

import dev.stillya.vpet.animation.Animation
import java.awt.image.BufferedImage

data class AnimationResource(
	val id: String,
	val animation: Animation,
	val frames: List<BufferedImage>
)
