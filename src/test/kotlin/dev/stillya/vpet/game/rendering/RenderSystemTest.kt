package dev.stillya.vpet.game.rendering

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.game.ecs.World
import dev.stillya.vpet.game.ecs.components.AnimationComponent
import dev.stillya.vpet.game.ecs.components.PhaseState
import dev.stillya.vpet.game.ecs.components.PhysicsState
import dev.stillya.vpet.game.ecs.components.SpriteState
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.ecs.components.Velocity
import dev.stillya.vpet.game.resources.AnimationCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RenderSystemTest {

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
	fun testRenderSystemCreation() {
		val world = World()
		world.registry.add(world.player, Transform(x = 10f, y = 5f))
		world.registry.add(world.player, Velocity())
		world.registry.add(world.player, SpriteState(tag = "test", frameIndex = 0))
		world.registry.add(world.player, PhysicsState(isOnGround = true))
		world.registry.add(world.player, PhaseState())

		assertNotNull("World should exist", world)
		assertEquals("Player should be at x=10", 10f, world.transform.x)
	}

	@Test
	fun testCoinEntitiesHaveAnimationComponent() {
		val world = World()
		val coinEntity = world.registry.create()
		world.registry.add(coinEntity, Transform(x = 15f, y = 5f))
		world.registry.add(coinEntity, AnimationComponent(resourceId = "coin_idle"))

		val component = world.registry.get<AnimationComponent>(coinEntity)
		assertNotNull("Coin should have AnimationComponent", component)
		assertEquals("coin_idle", component?.resourceId)
	}

	@Test
	fun testMultipleCoinEntitiesCanExist() {
		val world = World()

		val coin1 = world.registry.create()
		world.registry.add(coin1, Transform(x = 15f, y = 5f))
		world.registry.add(coin1, AnimationComponent(resourceId = "coin_idle"))

		val coin2 = world.registry.create()
		world.registry.add(coin2, Transform(x = 20f, y = 6f))
		world.registry.add(coin2, AnimationComponent(resourceId = "coin_idle"))

		val coins = world.registry.allWith(AnimationComponent::class, Transform::class)
		assertEquals("Should have 2 coins", 2, coins.size)
	}

	@Test
	fun testAnimationCacheReturnsNullForMissingResource() {
		val result = AnimationCache.get("nonexistent")
		assertNull("Should return null for missing resource", result)
	}

	@Test
	fun testCoinAnimationCanBeLoaded() {
		val resource = AnimationCache.loadCoinAnimation(loader)
		assertNotNull("Coin animation should load", resource)
		assertEquals(AnimationCache.COIN_IDLE, resource.id)
		assertTrue("Frames should be extracted", resource.frames.isNotEmpty())
	}

	@Test
	fun testRenderSystemQueriesEntitiesCorrectly() {
		val world = World()
		val coin1 = world.registry.create()
		world.registry.add(coin1, Transform(x = 15f, y = 5f))
		world.registry.add(coin1, AnimationComponent(resourceId = "coin_idle"))

		val coin2 = world.registry.create()
		world.registry.add(coin2, Transform(x = 20f, y = 6f))
		world.registry.add(coin2, AnimationComponent(resourceId = "coin_idle"))

		val otherEntity = world.registry.create()
		world.registry.add(otherEntity, Transform(x = 25f, y = 7f))

		val coinsWithAnim = world.registry.allWith(AnimationComponent::class, Transform::class)
		assertEquals("Should find only entities with both components", 2, coinsWithAnim.size)
	}
}
