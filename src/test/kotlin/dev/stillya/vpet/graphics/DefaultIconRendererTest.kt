package dev.stillya.vpet.graphics

import com.intellij.testFramework.LightPlatform4TestCase
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.AnimationState
import dev.stillya.vpet.animation.INFINITE
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.config.SpriteSheetAtlas
import org.junit.Test
import java.awt.Image
import javax.imageio.ImageIO

class DefaultIconRendererTest : LightPlatform4TestCase() {

	private companion object {
		const val WALK = "Walk" // 8 frames
		const val DMG = "Dmg"   // 2 frames
	}

	private lateinit var renderer: DefaultIconRenderer
	private lateinit var atlas: SpriteSheetAtlas
	private lateinit var image: Image

	override fun setUp() {
		super.setUp()
		renderer = DefaultIconRenderer(project)
		atlas = AsepriteJsonAtlasLoader().load("/META-INF/spritesheets/cat/atlas.json")!!
		image = javaClass.getResourceAsStream("/META-INF/spritesheets/cat/sprite.png")!!
			.use { ImageIO.read(it) }
	}

	private fun animation(tag: String, loop: Int = 1) = Animation(
		name = tag,
		sheet = atlas.create(image, tag),
		loop = loop,
		onFinish = {},
		state = AnimationState.IDLE
	)

	@Test
	fun testLockCollapsesToASingleFrame() {
		renderer.enqueue(animation(WALK))
		assertEquals(8, renderer.render().size)

		renderer.lockFrame()

		assertEquals(1, renderer.render().size)
	}

	@Test
	fun testLockedFramesAreDistinctInstances() {
		renderer.enqueue(animation(WALK))
		renderer.render()
		renderer.lockFrame()

		val first = renderer.render().single()
		val second = renderer.render().single()

		assertNotSame("Locked frames must be fresh instances", first, second)
		assertFalse("Locked frames must not be equal", first == second)
	}

	@Test
	fun testWithoutAnyEffectFramesAreReusedAsIs() {
		renderer.enqueue(animation(WALK, loop = INFINITE))
		val first = renderer.render()
		val second = renderer.render()

		first.forEachIndexed { i, icon ->
			assertSame("No effect is active, frames should come straight from the cache", icon, second[i])
		}
	}

	@Test
	fun testLockDoesNotAdvanceTheAnimationQueue() {
		renderer.enqueue(animation(WALK, loop = 5))
		renderer.enqueue(animation(DMG))
		assertEquals(8, renderer.render().size)

		renderer.lockFrame()
		repeat(20) { assertEquals(1, renderer.render().size) }
		renderer.unlockFrame()

		repeat(4) { assertEquals("Loop $it was consumed while locked", 8, renderer.render().size) }
		assertEquals("Only then move on to the queued animation", 2, renderer.render().size)
	}

	@Test
	fun testLockBeforeAnyAnimationIsRendered() {
		renderer.lockFrame()
		renderer.enqueue(animation(WALK))

		assertEquals(1, renderer.render().size)
	}
}
