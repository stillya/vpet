package dev.stillya.vpet.graphics.effect

import dev.stillya.vpet.animation.AnimationState
import dev.stillya.vpet.AnimatedStatusBarWidget
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage

class SleepEffectTest {

	private companion object {
		const val SIZE = 38
		const val PAD = SIZE
		const val FRAME_MS = AnimatedStatusBarWidget.FRAME_RATE_MS
		const val CYCLE_MS = SleepEffect.CYCLE_MS
	}

	private var now = 1_000_000L

	private fun newEffect() = SleepEffect(SIZE, SIZE) { now }

	private fun renderPadded(effect: SleepEffect): BufferedImage {
		val image = BufferedImage(SIZE + PAD * 2, SIZE + PAD * 2, BufferedImage.TYPE_INT_ARGB)
		val g = image.createGraphics()
		g.translate(PAD, PAD)
		effect.apply(g, AnimationState.IDLE)
		g.dispose()
		return image
	}

	private fun renderPixels(effect: SleepEffect): IntArray =
		renderPadded(effect).getRGB(PAD, PAD, SIZE, SIZE, null, 0, SIZE)

	@Test
	fun testDrawsOnlyInsideTheIconBox() {
		val effect = newEffect()
		var visiblePixels = 0

		repeat(9) {
			val image = renderPadded(effect)
			for (y in 0 until image.height) {
				for (x in 0 until image.width) {
					if (image.getRGB(x, y) ushr 24 == 0) continue
					val insideBox = x in PAD until PAD + SIZE && y in PAD until PAD + SIZE
					assertTrue("Glyph drawn outside the icon box at ($x, $y)", insideBox)
					visiblePixels++
				}
			}
			now += CYCLE_MS / 9
		}

		assertTrue("Effect should draw at least one visible glyph", visiblePixels > 0)
	}

	@Test
	fun testAdvancesEveryFrame() {
		val effect = newEffect()
		val first = renderPixels(effect)

		now += FRAME_MS
		val second = renderPixels(effect)

		assertFalse("Phase should advance between two frames", first.contentEquals(second))
	}

	@Test
	fun testIsDrivenByTheClockNotByRepaintCount() {
		val effect = newEffect()
		val first = renderPixels(effect)
		repeat(5) { renderPixels(effect) }
		val afterExtraRepaints = renderPixels(effect)

		assertArrayEquals("Extra repaints must not advance the phase", first, afterExtraRepaints)
	}

	@Test
	fun testLoopsAfterFullCycle() {
		val effect = newEffect()
		val first = renderPixels(effect)

		now += CYCLE_MS
		val afterCycle = renderPixels(effect)

		assertArrayEquals("Effect should repeat after a full cycle", first, afterCycle)
	}
}
