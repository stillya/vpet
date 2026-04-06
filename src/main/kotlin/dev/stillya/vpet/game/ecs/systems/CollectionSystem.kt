package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.components.Collecting
import dev.stillya.vpet.game.ecs.components.Transform
import kotlin.math.pow

object CollectionSystem {
	private const val BOUNCE_DURATION = 0.4f
	private const val BOUNCE_HEIGHT = 2.0f

	fun updateCollecting(registry: EntityRegistry, dt: Float) {
		val collectingEntities = registry.allWith(Collecting::class, Transform::class)

		for (id in collectingEntities) {
			val collecting = registry.get<Collecting>(id) ?: continue
			val transform = registry.get<Transform>(id) ?: continue

			val newElapsed = collecting.elapsed + dt

			if (newElapsed >= BOUNCE_DURATION) {
				registry.markForRemoval(id)
			} else {
				val progress = newElapsed / BOUNCE_DURATION
				val bounceOffset = BOUNCE_HEIGHT * (1 - (2 * progress - 1).pow(2))

				registry.add(id, transform.copy(y = collecting.startY - bounceOffset))
				registry.add(id, collecting.copy(elapsed = newElapsed))
			}
		}
	}
}
