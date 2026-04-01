package dev.stillya.vpet.game

interface Spatial {
	fun collider(): AABB
}

@JvmInline
value class EntityID(val id: String)