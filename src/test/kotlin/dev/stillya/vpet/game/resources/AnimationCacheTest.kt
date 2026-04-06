package dev.stillya.vpet.game.resources

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimationCacheTest {

	private lateinit var loader: AtlasLoader

	@Before
	fun setup() {
		loader = AsepriteJsonAtlasLoader()
		AnimationCache.clear()
	}

	@After
	fun cleanup() {
		AnimationCache.clear()
	}

	@Test
	fun testLoadAnimationCreatesAndCachesResource() {
		val resource = AnimationCache.loadAnimation(
			atlasLoader = loader,
			imagePath = "/META-INF/spritesheets/coin/sprite.png",
			atlasPath = "/META-INF/spritesheets/coin/atlas.json",
			animationTag = "coin",
			resourceId = "coin_idle"
		)

		assertEquals("coin_idle", resource.id)
		assertEquals("coin", resource.animation.name)
		assertTrue("Frames should be extracted", resource.frames.isNotEmpty())
	}

	@Test
	fun testLoadAnimationReturnsCachedResourceOnSecondCall() {
		val resource1 = AnimationCache.loadAnimation(
			atlasLoader = loader,
			imagePath = "/META-INF/spritesheets/coin/sprite.png",
			atlasPath = "/META-INF/spritesheets/coin/atlas.json",
			animationTag = "coin",
			resourceId = "coin_idle"
		)

		val resource2 = AnimationCache.loadAnimation(
			atlasLoader = loader,
			imagePath = "/META-INF/spritesheets/coin/sprite.png",
			atlasPath = "/META-INF/spritesheets/coin/atlas.json",
			animationTag = "coin",
			resourceId = "coin_idle"
		)

		assertSame("Should return the same instance", resource1, resource2)
	}

	@Test
	fun testGetReturnsNullForNonExistentResource() {
		val result = AnimationCache.get("non_existent")
		assertNull("Should return null for non-existent resource", result)
	}

	@Test
	fun testGetReturnsCachedResource() {
		AnimationCache.loadAnimation(
			atlasLoader = loader,
			imagePath = "/META-INF/spritesheets/coin/sprite.png",
			atlasPath = "/META-INF/spritesheets/coin/atlas.json",
			animationTag = "coin",
			resourceId = "coin_idle"
		)

		val cached = AnimationCache.get("coin_idle")
		assertNotNull("Should retrieve cached resource", cached)
		assertEquals("coin_idle", cached?.id)
	}

	@Test
	fun testClearRemovesAllCachedResources() {
		AnimationCache.loadAnimation(
			atlasLoader = loader,
			imagePath = "/META-INF/spritesheets/coin/sprite.png",
			atlasPath = "/META-INF/spritesheets/coin/atlas.json",
			animationTag = "coin",
			resourceId = "coin_idle"
		)

		AnimationCache.clear()

		val cached = AnimationCache.get("coin_idle")
		assertNull("Cache should be empty after clear", cached)
	}

	@Test
	fun testLoadCoinAnimationConvenienceMethod() {
		val resource = AnimationCache.loadCoinAnimation(loader)

		assertEquals(AnimationCache.COIN_IDLE, resource.id)
		assertEquals("coin", resource.animation.name)
		assertTrue("Coin frames should be extracted", resource.frames.isNotEmpty())
	}

	@Test
	fun testLoadCoinAnimationCachesResult() {
		val resource1 = AnimationCache.loadCoinAnimation(loader)
		val resource2 = AnimationCache.loadCoinAnimation(loader)

		assertSame("Should return cached coin resource", resource1, resource2)
	}
}
