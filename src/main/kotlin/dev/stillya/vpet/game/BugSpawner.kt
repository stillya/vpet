package dev.stillya.vpet.game

import kotlin.random.Random

object BugSpawner {

	fun spawnBugs(
		registry: EntityRegistry,
		tileMap: VirtualTileMap,
		visibleRange: IntRange,
		count: Int = 5
	) {
		val candidates = mutableListOf<Pair<Int, Int>>()

		for (groundLine in visibleRange) {
			val extent = tileMap.getExtent(groundLine) ?: continue
			for (col in extent.first..extent.last) {
				if (!tileMap.isSolid(groundLine, col)) continue
				val aboveLine = groundLine - 1
				if (aboveLine < visibleRange.first) continue
				if (tileMap.isSolid(aboveLine, col)) continue
				candidates.add(col to groundLine)
			}
		}

		candidates.shuffled(Random).take(count).forEach { (col, groundLine) ->
			val bug = registry.create()
			registry.add(bug, Transform(col.toFloat(), (groundLine - 1).toFloat()))
			registry.add(bug, AABB(1, 1))
			registry.add(bug, Collectible())
			registry.add(bug, BugVisual(BugColor.entries[Random.nextInt(BugColor.entries.size)]))
		}
	}
}
