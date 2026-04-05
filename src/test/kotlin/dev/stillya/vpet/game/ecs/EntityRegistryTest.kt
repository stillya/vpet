package dev.stillya.vpet.game.ecs

import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.ecs.components.Velocity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityRegistryTest {

	@Test
	fun `create returns unique IDs`() {
		val reg = EntityRegistry()
		val a = reg.create()
		val b = reg.create()
		assertTrue(a != b)
	}

	@Test
	fun `add and get component`() {
		val reg = EntityRegistry()
		val id = reg.create()
		reg.add(id, Transform(1f, 2f))

		val t = reg.get<Transform>(id)
		assertEquals(1f, t!!.x, 0.001f)
		assertEquals(2f, t.y, 0.001f)
	}

	@Test
	fun `get returns null for missing component`() {
		val reg = EntityRegistry()
		val id = reg.create()
		assertNull(reg.get<Transform>(id))
	}

	@Test
	fun `has returns correct values`() {
		val reg = EntityRegistry()
		val id = reg.create()
		assertFalse(reg.has<Transform>(id))
		reg.add(id, Transform())
		assertTrue(reg.has<Transform>(id))
	}

	@Test
	fun `destroy removes entity`() {
		val reg = EntityRegistry()
		val id = reg.create()
		reg.add(id, Transform())
		reg.destroy(id)
		assertFalse(reg.exists(id))
		assertNull(reg.get<Transform>(id))
	}

	@Test
	fun `allWith queries by component types`() {
		val reg = EntityRegistry()
		val a = reg.create()
		reg.add(a, Transform())
		reg.add(a, Velocity())

		val b = reg.create()
		reg.add(b, Transform())

		val both = reg.allWith(Transform::class, Velocity::class)
		assertEquals(1, both.size)
		assertTrue(a in both)

		val justTransform = reg.allWith(Transform::class)
		assertEquals(2, justTransform.size)
	}

	@Test
	fun `deferred removal flushes correctly`() {
		val reg = EntityRegistry()
		val a = reg.create()
		val b = reg.create()
		reg.add(a, Transform())
		reg.add(b, Transform())

		reg.markForRemoval(a)
		assertTrue(reg.exists(a))

		reg.flushRemovals()
		assertFalse(reg.exists(a))
		assertTrue(reg.exists(b))
	}
}
