package dev.stillya.vpet.game.rendering

import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.graphics.create
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class GameRendererTest {

	private lateinit var atlasLoader: AsepriteJsonAtlasLoader

	@Before
	fun setup() {
		atlasLoader = AsepriteJsonAtlasLoader()
	}

	@Test
	fun `coinFrames loads successfully from atlas`() {
		val atlas = atlasLoader.load("/META-INF/spritesheets/coin/atlas.json")
		val imgStream = javaClass.getResourceAsStream("/META-INF/spritesheets/coin/sprite.png")
		val image = imgStream?.use { ImageIO.read(it) }

		assertNotNull("Atlas should be loaded", atlas)
		assertNotNull("Coin sprite image should be loaded", image)

		if (atlas != null && image != null) {
			val spriteSheet = atlas.create(image, "coin")
			val frames = spriteSheet.frames.map { frame ->
				val f = frame.frame
				image.getSubimage(f.x, f.y, f.width, f.height)
			}

			assertNotNull("Frames should not be null", frames)
			assertTrue("Frames should not be empty", frames.isNotEmpty())
			assertEquals("Should have exactly 1 frame", 1, frames.size)
			assertTrue("Frame should be a BufferedImage", frames[0] is BufferedImage)
		}
	}

	@Test
	fun `coin atlas has correct frame dimensions`() {
		val atlas = atlasLoader.load("/META-INF/spritesheets/coin/atlas.json")

		assertNotNull("Coin atlas should be loaded", atlas)

		if (atlas != null) {
			val coinTag = atlas.meta.frameTags.find { it.name == "coin" }
			assertNotNull("Coin tag should exist", coinTag)
			assertEquals("Coin tag should start at frame 0", 0, coinTag?.from)
			assertEquals("Coin tag should end at frame 0", 0, coinTag?.to)

			val frame = atlas.frames[0]
			assertEquals("Frame width should be 512", 512, frame.frame.width)
			assertEquals("Frame height should be 512", 512, frame.frame.height)
		}
	}
}
