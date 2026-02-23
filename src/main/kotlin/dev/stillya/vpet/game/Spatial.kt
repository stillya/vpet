package dev.stillya.vpet.game

interface Spatial {
	fun id(): EntityID
	fun collider(): AABB
}

@JvmInline
value class EntityID(val id: String)