package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.game.VirtualTileMap
import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.components.CoinVisual
import dev.stillya.vpet.game.ecs.components.Collectible
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.physics.AABB
import kotlin.random.Random

object CoinSpawner {

	fun spawnCoins(
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
			val coin = registry.create()
			registry.add(coin, Transform(col.toFloat(), (groundLine - 1).toFloat()))
			registry.add(coin, AABB(1, 1))
			registry.add(coin, Collectible())
			registry.add(coin, CoinVisual())
		}
	}
}
