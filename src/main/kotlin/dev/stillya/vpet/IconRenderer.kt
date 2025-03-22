package dev.stillya.vpet

import dev.stillya.vpet.graphics.Animation
import javax.swing.Icon

interface IconRenderer {
	fun enqueue(animation: Animation)
	fun render(): List<Icon>
}