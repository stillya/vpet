package dev.stillya.vpet.graphics.effect

import com.intellij.ui.JBColor
import dev.stillya.vpet.animation.AnimationState
import dev.stillya.vpet.graphics.Effect
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import kotlin.math.roundToInt

class SleepEffect(
	private val width: Int,
	private val height: Int,
	private val clock: () -> Long = System::currentTimeMillis
) : Effect {
	override val overSprite = true

	private val startMs = clock()

	companion object {
		internal const val CYCLE_MS = 2700L
		private const val COUNT = 3

		private const val BASE_X = 0.48f
		private const val DRIFT_X = 0.24f
		private const val BASE_Y = 0.68f
		private const val TOP_Y = 0.18f
		private const val MIN_SIZE = 0.16f
		private const val MAX_SIZE = 0.26f

		private const val MAX_ALPHA = 230f
		private const val FADE_IN = 0.2f
		private const val FADE_OUT = 0.45f

		private val Z_LIGHT = Color(0x33, 0x33, 0x4D)
		private val Z_DARK = Color(0xE0, 0xE0, 0xF0)
	}

	override fun apply(g: Graphics2D, state: AnimationState) {
		val elapsed = ((clock() - startMs) % CYCLE_MS) / CYCLE_MS.toFloat()
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

		for (i in 0 until COUNT) {
			drawZ(g, (elapsed + i.toFloat() / COUNT) % 1f)
		}
	}

	private fun drawZ(g: Graphics2D, phase: Float) {
		val fade = minOf(phase / FADE_IN, (1f - phase) / FADE_OUT, 1f)
		if (fade <= 0f) return

		val alpha = (MAX_ALPHA * fade).roundToInt()
		g.color = JBColor(withAlpha(Z_LIGHT, alpha), withAlpha(Z_DARK, alpha))
		g.font = Font(Font.SANS_SERIF, Font.BOLD, (height * (MIN_SIZE + (MAX_SIZE - MIN_SIZE) * phase)).roundToInt())
		g.drawString(
			"z",
			(width * (BASE_X + DRIFT_X * phase)).roundToInt(),
			(height * (BASE_Y - (BASE_Y - TOP_Y) * phase)).roundToInt()
		)
	}

	private fun withAlpha(color: Color, alpha: Int) = Color(color.red, color.green, color.blue, alpha)
}
