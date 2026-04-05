package dev.stillya.vpet.game.ecs

import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.physics.AABB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialGridTest {

	@Test
	fun `query finds entity in same cell`() {
		val reg = EntityRegistry()
		val id = reg.create()
		reg.add(id, Transform(2f, 2f))
		reg.add(id, AABB(1, 1))

		val grid = SpatialGrid(cellSize = 4)
		grid.rebuild(reg)

		val results = grid.query(Transform(1f, 1f), AABB(1, 1))
		assertTrue(id in results)
	}

	@Test
	fun `query returns empty for distant entities`() {
		val reg = EntityRegistry()
		val id = reg.create()
		reg.add(id, Transform(20f, 20f))
		reg.add(id, AABB(1, 1))

		val grid = SpatialGrid(cellSize = 4)
		grid.rebuild(reg)

		val results = grid.query(Transform(0f, 0f), AABB(1, 1))
		assertTrue(results.isEmpty())
	}

	@Test
	fun `query finds multiple entities`() {
		val reg = EntityRegistry()
		val a = reg.create()
		reg.add(a, Transform(1f, 1f))
		reg.add(a, AABB(1, 1))

		val b = reg.create()
		reg.add(b, Transform(2f, 1f))
		reg.add(b, AABB(1, 1))

		val grid = SpatialGrid(cellSize = 4)
		grid.rebuild(reg)

		val results = grid.query(Transform(1f, 1f), AABB(2, 1))
		assertEquals(2, results.size)
	}

	@Test
	fun `rebuild clears old data`() {
		val reg = EntityRegistry()
		val id = reg.create()
		reg.add(id, Transform(1f, 1f))
		reg.add(id, AABB(1, 1))

		val grid = SpatialGrid(cellSize = 4)
		grid.rebuild(reg)
		assertTrue(grid.query(Transform(1f, 1f), AABB(1, 1)).isNotEmpty())

		reg.destroy(id)
		grid.rebuild(reg)
		assertTrue(grid.query(Transform(1f, 1f), AABB(1, 1)).isEmpty())
	}
}
