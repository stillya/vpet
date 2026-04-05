package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.game.VirtualTileMap
import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.components.AnimationComponent
import dev.stillya.vpet.game.ecs.components.Collectible
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.physics.AABB
import dev.stillya.vpet.game.resources.AnimationCache
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CoinSpawnerTest {

	private lateinit var registry: EntityRegistry
	private lateinit var tileMap: VirtualTileMap

	@Before
	fun setup() {
		registry = EntityRegistry()
		tileMap = VirtualTileMap()
	}

	@Test
	fun `spawnCoins creates coins on solid tiles with air above`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"code here"
		))
		CoinSpawner.spawnCoins(registry, tileMap, 0..1, count = 3)

		val coins = registry.allWith(AnimationComponent::class, Transform::class, Collectible::class, AABB::class)
		assertEquals(3, coins.size)

		coins.forEach { id ->
			val transform = registry.get<Transform>(id)!!
			val aabb = registry.get<AABB>(id)!!
			val collectible = registry.get<Collectible>(id)!!
			val animComp = registry.get<AnimationComponent>(id)!!

			assertEquals(0f, transform.y, 0.001f)
			assertEquals(1, aabb.width)
			assertEquals(1, aabb.height)
			assertEquals(1, collectible.value)
			assertEquals(AnimationCache.COIN_IDLE, animComp.resourceId)
		}
	}

	@Test
	fun `spawnCoins respects count parameter`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"code code code code code code code"
		))
		CoinSpawner.spawnCoins(registry, tileMap, 0..1, count = 2)

		val coins = registry.allWith(AnimationComponent::class)
		assertEquals(2, coins.size)
	}

	@Test
	fun `spawnCoins spawns no coins when no valid positions exist`() {
		tileMap.rebuildFromLines(listOf(
			"code",
			"code"
		))
		CoinSpawner.spawnCoins(registry, tileMap, 0..1, count = 5)

		val coins = registry.allWith(AnimationComponent::class)
		assertEquals(0, coins.size)
	}

	@Test
	fun `spawnCoins only spawns on ground with air above`() {
		tileMap.rebuildFromLines(listOf(
			"code",
			"",
			"code"
		))
		CoinSpawner.spawnCoins(registry, tileMap, 0..2, count = 10)

		val coins = registry.allWith(AnimationComponent::class, Transform::class)
		assertTrue(coins.size > 0)

		coins.forEach { id ->
			val transform = registry.get<Transform>(id)!!
			val y = transform.y.toInt()
			assertEquals(1, y)
		}
	}

	@Test
	fun `spawnCoins positions coins one tile above ground`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"",
			"code"
		))
		CoinSpawner.spawnCoins(registry, tileMap, 0..2, count = 1)

		val coins = registry.allWith(AnimationComponent::class, Transform::class)
		assertEquals(1, coins.size)

		val transform = registry.get<Transform>(coins.first())!!
		assertEquals(1f, transform.y, 0.001f)
	}

}
