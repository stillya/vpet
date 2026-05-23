package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.Physics
import dev.stillya.vpet.game.ecs.components.AnimationComponent
import dev.stillya.vpet.game.resources.AnimationCache

object AnimationSystem {
	fun updateAnimations(registry: EntityRegistry, dt: Float) {
		for (entityId in registry.allWith(AnimationComponent::class)) {
			val component = registry.get<AnimationComponent>(entityId) ?: continue
			val resource = AnimationCache.get(component.resourceId) ?: continue

			val frameCount = resource.animation.frameCount
			if (frameCount == 0) continue

			component.elapsed += dt
			if (component.elapsed >= Physics.FRAME_ADVANCE_INTERVAL) {
				component.currentFrame = (component.currentFrame + 1) % frameCount
				component.elapsed -= Physics.FRAME_ADVANCE_INTERVAL
			}
		}
	}
}
