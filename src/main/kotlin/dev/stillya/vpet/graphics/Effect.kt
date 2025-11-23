package dev.stillya.vpet.graphics

import dev.stillya.vpet.animation.AnimationState
import java.awt.Graphics2D

interface Effect {
	fun apply(g: Graphics2D, state: AnimationState)
}
