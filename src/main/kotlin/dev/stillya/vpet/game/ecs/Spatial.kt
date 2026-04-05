package dev.stillya.vpet.game.ecs

import dev.stillya.vpet.game.physics.AABB

interface Spatial {
	fun id(): EntityID
	fun collider(): AABB
}

@JvmInline
value class EntityID(val id: String)
