package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.Physics
import dev.stillya.vpet.game.ecs.components.AnimationComponent
import dev.stillya.vpet.game.resources.AnimationCache

object AnimationSystem {
	fun updateAnimations(registry: EntityRegistry, dt: Float) {
		val entities = registry.allWith(AnimationComponent::class)

		for (entityId in entities) {
			val component = registry.get<AnimationComponent>(entityId) ?: continue
			val resource = AnimationCache.get(component.resourceId) ?: continue

			val frameCount = resource.animation.frameCount
			if (frameCount == 0) continue

			val newElapsed = component.elapsed + dt

			if (newElapsed >= Physics.FRAME_ADVANCE_INTERVAL) {
				val nextFrame = (component.currentFrame + 1) % frameCount
				val updatedComponent = AnimationComponent(
					resourceId = component.resourceId,
					currentFrame = nextFrame,
					elapsed = newElapsed - Physics.FRAME_ADVANCE_INTERVAL
				)
				registry.add(entityId, updatedComponent)
			} else {
				val updatedComponent = component.copy(elapsed = newElapsed)
				registry.add(entityId, updatedComponent)
			}
		}
	}
}
