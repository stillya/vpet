package dev.stillya.vpet

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationTrigger
import javax.swing.Icon

interface IconRenderer {
	fun enqueue(animation: Animation)
	fun render(): List<Icon>
	fun createAnimationContext(
		trigger: AnimationTrigger
	): AnimationContext
}