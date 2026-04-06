package dev.stillya.vpet.game.resources

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.AnimationState
import dev.stillya.vpet.config.AtlasFrame
import dev.stillya.vpet.config.FrameRect
import dev.stillya.vpet.graphics.SpriteSheet
import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.image.BufferedImage

class AnimationResourceTest {

	@Test
	fun testAnimationResourceHoldsIdAnimationAndFrames() {
		val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
		val frames = listOf(
			AtlasFrame(
				frame = FrameRect(0, 0, 16, 16)
			)
		)
		val sheet = SpriteSheet(image, frames)
		val animation = Animation(
			name = "test",
			sheet = sheet,
			loop = 0,
			onFinish = {},
			state = AnimationState.IDLE
		)
		val extractedFrames = animation.extractFrames()

		val resource = AnimationResource(
			id = "test_anim",
			animation = animation,
			frames = extractedFrames
		)

		assertEquals("test_anim", resource.id)
		assertEquals(animation, resource.animation)
		assertEquals(1, resource.frames.size)
	}
}
