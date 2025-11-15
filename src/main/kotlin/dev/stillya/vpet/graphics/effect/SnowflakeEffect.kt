package dev.stillya.vpet.graphics.effect

import com.intellij.ui.JBColor
import dev.stillya.vpet.graphics.Effect
import java.awt.Color
import java.awt.Graphics2D
import kotlin.random.Random

class SnowflakeEffect(
	private val width: Int,
	private val height: Int,
	private val snowflakeCount: Int = 6
) : Effect {
	private class Particles(count: Int) {
		val xPositions = FloatArray(count)
		val yPositions = FloatArray(count)
		val speeds = FloatArray(count)
		val drifts = FloatArray(count)
	}

	private val particles = Particles(snowflakeCount)
	private val size = 1

	companion object {
		// NOTE: Don't actually care about light mode :)
		private val SNOWFLAKE_COLOR = JBColor(
			Color(173, 216, 230, 200),
			Color(255, 255, 255, 200)
		)
		private val SNOWDRIFT_COLOR = JBColor(
			Color(173, 216, 230, 150),
			Color(255, 255, 255, 150)
		)
	}

	init {
		for (i in 0 until snowflakeCount) {
			resetSnowflake(i, Random.nextFloat() * height)
		}
	}

	private fun resetSnowflake(index: Int, yPosition: Float = 0f) {
		particles.xPositions[index] = Random.nextFloat() * width
		particles.yPositions[index] = yPosition
		particles.speeds[index] = Random.nextFloat() * 0.5f + 0.3f
		particles.drifts[index] = Random.nextFloat() * 0.3f - 0.15f
	}

	override fun apply(g: Graphics2D) {
		updatePositions()
		drawSnowdrifts(g)
		drawSnowflakes(g)
	}

	private fun updatePositions() {
		for (i in 0 until snowflakeCount) {
			particles.yPositions[i] += particles.speeds[i]
			particles.xPositions[i] += particles.drifts[i]

			if (particles.yPositions[i] > height) {
				resetSnowflake(i)
			}

			if (particles.xPositions[i] < 0) {
				particles.xPositions[i] = width.toFloat()
			} else if (particles.xPositions[i] > width) {
				particles.xPositions[i] = 0f
			}
		}
	}

	private fun drawSnowflakes(g: Graphics2D) {
		val originalColor = g.color
		g.color = SNOWFLAKE_COLOR

		for (i in 0 until snowflakeCount) {
			g.fillOval(
				particles.xPositions[i].toInt(),
				particles.yPositions[i].toInt(),
				size,
				size
			)
		}

		g.color = originalColor
	}

	private fun drawSnowdrifts(g: Graphics2D) {
		g.color = SNOWDRIFT_COLOR

		val driftHeight = 6
		val driftBaseY = height - driftHeight
		val edgeSlopeWidth = width / 4
		val edgeSlopeHeight = driftHeight * 2

		g.fillOval(-edgeSlopeWidth, driftBaseY, edgeSlopeWidth * 2, edgeSlopeHeight)
		g.fillOval(width - edgeSlopeWidth, driftBaseY, edgeSlopeWidth * 2, edgeSlopeHeight)

		g.fillRect(0, driftBaseY, width, driftHeight)

		val moundCount = 3
		val moundSpacing = width / (moundCount * 2)
		val moundHeight = 3
		for (i in 0 until moundCount) {
			val moundX = (i * 2 + 1) * moundSpacing - moundSpacing / 2
			g.fillOval(moundX, driftBaseY - 1, moundSpacing, moundHeight)
		}
	}
}
