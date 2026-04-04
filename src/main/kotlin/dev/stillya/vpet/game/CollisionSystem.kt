package dev.stillya.vpet.game

import kotlin.math.floor

object CollisionSystem {

	fun detectCollections(
		registry: EntityRegistry,
		playerEntity: EntityID,
		spatialGrid: SpatialGrid
	): List<EntityID> {
		val playerPos = registry.get<Transform>(playerEntity) ?: return emptyList()
		val playerCol = registry.get<AABB>(playerEntity) ?: return emptyList()

		val candidates = spatialGrid.query(playerPos, playerCol)
		return candidates.filter { id ->
			id != playerEntity &&
				registry.has<Collectible>(id) &&
				run {
					val pos = registry.get<Transform>(id) ?: return@filter false
					val col = registry.get<AABB>(id) ?: return@filter false
					aabbsOverlap(playerPos, playerCol, pos, col)
				}
		}
	}

	private fun aabbsOverlap(aPos: Transform, a: AABB, bPos: Transform, b: AABB): Boolean {
		val ax = floor(aPos.x).toInt()
		val ay = floor(aPos.y).toInt() - (a.height - 1)
		val bx = floor(bPos.x).toInt()
		val by = floor(bPos.y).toInt() - (b.height - 1)

		return ax < bx + b.width &&
			ax + a.width > bx &&
			ay < by + b.height &&
			ay + a.height > by
	}
}
