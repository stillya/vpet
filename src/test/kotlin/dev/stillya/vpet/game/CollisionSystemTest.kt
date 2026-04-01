package dev.stillya.vpet.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionSystemTest {

	private fun setupRegistry(): Pair<EntityRegistry, EntityID> {
		val reg = EntityRegistry()
		val player = reg.create()
		reg.add(player, Transform(5f, 5f))
		reg.add(player, AABB(2, 2))
		return reg to player
	}

	@Test
	fun `detects overlapping collectible`() {
		val (reg, player) = setupRegistry()
		val bug = reg.create()
		reg.add(bug, Transform(6f, 5f))
		reg.add(bug, AABB(1, 1))
		reg.add(bug, Collectible())

		val grid = SpatialGrid()
		grid.rebuild(reg)

		val collected = CollisionSystem.detectCollections(reg, player, grid)
		assertEquals(1, collected.size)
		assertEquals(bug, collected[0])
	}

	@Test
	fun `ignores non-overlapping collectible`() {
		val (reg, player) = setupRegistry()
		val bug = reg.create()
		reg.add(bug, Transform(20f, 20f))
		reg.add(bug, AABB(1, 1))
		reg.add(bug, Collectible())

		val grid = SpatialGrid()
		grid.rebuild(reg)

		val collected = CollisionSystem.detectCollections(reg, player, grid)
		assertTrue(collected.isEmpty())
	}

	@Test
	fun `ignores overlapping non-collectible`() {
		val (reg, player) = setupRegistry()
		val other = reg.create()
		reg.add(other, Transform(5f, 5f))
		reg.add(other, AABB(1, 1))

		val grid = SpatialGrid()
		grid.rebuild(reg)

		val collected = CollisionSystem.detectCollections(reg, player, grid)
		assertTrue(collected.isEmpty())
	}

	@Test
	fun `detects multiple bugs`() {
		val (reg, player) = setupRegistry()
		for (i in 0..1) {
			val bug = reg.create()
			reg.add(bug, Transform(5f + i, 5f))
			reg.add(bug, AABB(1, 1))
			reg.add(bug, Collectible())
		}

		val grid = SpatialGrid()
		grid.rebuild(reg)

		val collected = CollisionSystem.detectCollections(reg, player, grid)
		assertEquals(2, collected.size)
	}
}
