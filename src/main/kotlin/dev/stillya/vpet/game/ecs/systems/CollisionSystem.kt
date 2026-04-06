package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.game.ecs.EntityID
import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.SpatialGrid
import dev.stillya.vpet.game.ecs.components.Collectible
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.physics.AABB
import dev.stillya.vpet.game.utils.toTileInt

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
		val ax = aPos.x.toTileInt()
		val ay = aPos.y.toTileInt() - (a.height - 1)
		val bx = bPos.x.toTileInt()
		val by = bPos.y.toTileInt() - (b.height - 1)

		return ax < bx + b.width &&
				ax + a.width > bx &&
				ay < by + b.height &&
				ay + a.height > by
	}
}
