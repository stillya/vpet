package dev.stillya.vpet.game

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
					overlaps(playerPos, playerCol, pos, col)
				}
		}
	}
}
