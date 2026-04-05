package dev.stillya.vpet.game.ecs

import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.physics.AABB
import kotlin.math.floor

class SpatialGrid(private val cellSize: Int = 4) {

	private val cells = HashMap<Long, MutableSet<EntityID>>()

	fun rebuild(registry: EntityRegistry) {
		cells.clear()
		for (id in registry.allWith(Transform::class, AABB::class)) {
			val t = registry.get<Transform>(id) ?: continue
			val c = registry.get<AABB>(id) ?: continue
			insert(id, t, c)
		}
	}

	fun query(at: Transform, bounds: AABB): Set<EntityID> {
		val x = floor(at.x).toInt()
		val y = floor(at.y).toInt() - (bounds.height - 1)

		val minCx = x / cellSize - (if (x < 0 && x % cellSize != 0) 1 else 0)
		val maxCx = (x + bounds.width - 1) / cellSize
		val minCy = y / cellSize - (if (y < 0 && y % cellSize != 0) 1 else 0)
		val maxCy = (y + bounds.height - 1) / cellSize

		val result = mutableSetOf<EntityID>()
		for (cx in minCx..maxCx) {
			for (cy in minCy..maxCy) {
				cells[key(cx, cy)]?.let { result.addAll(it) }
			}
		}
		return result
	}

	private fun insert(id: EntityID, t: Transform, c: AABB) {
		val x = floor(t.x).toInt()
		val y = floor(t.y).toInt() - (c.height - 1)

		val minCx = x / cellSize - (if (x < 0 && x % cellSize != 0) 1 else 0)
		val maxCx = (x + c.width - 1) / cellSize
		val minCy = y / cellSize - (if (y < 0 && y % cellSize != 0) 1 else 0)
		val maxCy = (y + c.height - 1) / cellSize

		for (cx in minCx..maxCx) {
			for (cy in minCy..maxCy) {
				cells.computeIfAbsent(key(cx, cy)) { mutableSetOf() }.add(id)
			}
		}
	}

	private fun key(cx: Int, cy: Int): Long =
		(cx.toLong() shl 32) or (cy.toLong() and 0xFFFFFFFFL)
}
