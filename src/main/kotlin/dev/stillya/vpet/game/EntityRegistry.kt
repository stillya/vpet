package dev.stillya.vpet.game

import kotlin.reflect.KClass

class EntityRegistry {

	private var nextId = 0
	private val components = mutableMapOf<EntityID, MutableMap<KClass<*>, Any>>()
	private val pendingRemovals = mutableSetOf<EntityID>()

	fun create(): EntityID {
		val id = EntityID("entity_${nextId++}")
		components[id] = mutableMapOf()
		return id
	}

	fun destroy(id: EntityID) {
		components.remove(id)
	}

	fun add(id: EntityID, component: Any) {
		require(id in components) { "Entity $id does not exist" }
		components.getValue(id)[component::class] = component
	}

	inline fun <reified T : Any> get(id: EntityID): T? =
		get(id, T::class)

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> get(id: EntityID, type: KClass<T>): T? =
		components[id]?.get(type) as? T

	inline fun <reified T : Any> has(id: EntityID): Boolean =
		has(id, T::class)

	fun <T : Any> has(id: EntityID, type: KClass<T>): Boolean =
		components[id]?.containsKey(type) == true

	fun allWith(vararg types: KClass<*>): List<EntityID> =
		components.entries
			.filter { (_, comps) -> types.all { it in comps } }
			.map { it.key }

	fun markForRemoval(id: EntityID) {
		pendingRemovals.add(id)
	}

	fun flushRemovals() {
		pendingRemovals.forEach { destroy(it) }
		pendingRemovals.clear()
	}

	fun exists(id: EntityID): Boolean = id in components
}
