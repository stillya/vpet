package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction
import dev.stillya.vpet.game.ecs.EntityID
import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.GamePhase
import dev.stillya.vpet.game.ecs.Physics
import dev.stillya.vpet.game.ecs.World
import dev.stillya.vpet.game.ecs.components.Collectible
import dev.stillya.vpet.game.ecs.components.PhaseState
import dev.stillya.vpet.game.ecs.components.PhysicsState
import dev.stillya.vpet.game.ecs.components.SpriteState
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.ecs.components.Velocity
import dev.stillya.vpet.game.input.InputState
import dev.stillya.vpet.game.physics.AABB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class WorldUpdateTest {

	private lateinit var tileMap: VirtualTileMap

	private val testCharacter = object : Character {
		override fun id(): EntityID = EntityID("test_character")
		override fun collider() = AABB(width = 2, height = 2)
		override fun update(input: InputState, ctx: TickContext, dt: Float): CharacterIntent {
			val vx = when {
				input.moveDirection != 0 -> input.moveDirection.toFloat() * 9f
				ctx.isOnGround -> {
					val damped = ctx.velocity.x * exp(-18f * dt)
					if (abs(damped) < Physics.VELOCITY_EPSILON) 0f else damped
				}

				else -> ctx.velocity.x
			}
			val vy = if (input.jumpJustPressed && ctx.isOnGround) -15.6f else ctx.velocity.y
			val direction = if (input.moveDirection < 0) Direction.LEFT else Direction.RIGHT
			var phase = ctx.phase
			if (phase == GamePhase.ENTRANCE && ctx.isOnGround) phase = GamePhase.PLAYING
			return CharacterIntent(Velocity(vx, vy), Animation.empty(), direction, phase)
		}
	}

	@Before
	fun setup() {
		tileMap = VirtualTileMap()
		tileMap.rebuildFromLines(listOf("code here", ""))
	}

	private fun worldAt(x: Float = 0f, y: Float = 0f): World {
		val reg = EntityRegistry()
		val player = reg.create()
		reg.add(player, Transform(x, y))
		reg.add(player, Velocity(0f, 0f))
		reg.add(player, PhysicsState(isOnGround = true))
		reg.add(player, SpriteState(tag = "Idle"))
		reg.add(player, PhaseState())
		reg.add(player, AABB(2, 2))
		return World(registry = reg, player = player)
	}

	@Test
	fun `GameFrame does not contain animation field`() {
		val fields = GameFrame::class.java.declaredFields.map { it.name }
		assertTrue("GameFrame should not contain 'animation' field", "animation" !in fields)
	}

	@Test
	fun `tick returns CharacterIntent with animation separately`() {
		val world = worldAt(x = 0f, y = 0f)
		val (frame, intent) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertNotNull(intent)
		assertNotNull(intent.animation)
	}

	@Test
	fun `tick returns updated world state`() {
		val world = worldAt(x = 2f, y = 0f)
		val (frame, _) = WorldUpdate.tick(world, InputState(moveDirection = 1), 0.016f, testCharacter, tileMap, 0..10)

		assertNotNull(frame.world)
		assertTrue(frame.world.transform.x >= 2f)
	}

	@Test
	fun `tick returns non-empty bounds`() {
		val world = worldAt(x = 2f, y = 0f)
		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		val boundsSize = frame.bounds.last - frame.bounds.first
		assertTrue("Bounds should have non-negative size", boundsSize >= 0)
	}

	@Test
	fun `score increases when collectible is at player position`() {
		val world = worldAt(x = 2f, y = 0f)
		val reg = world.registry
		val bug = reg.create()
		reg.add(bug, Transform(2f, 0f))
		reg.add(bug, AABB(1, 1))
		reg.add(bug, Collectible(value = 1))

		val initialScore = world.score
		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertEquals(initialScore + 1, frame.world.score)
		assertTrue("Collected bug should be removed", !frame.world.registry.exists(bug))
	}

	@Test
	fun `score does not increase when no collectible is nearby`() {
		val world = worldAt(x = 2f, y = 0f)
		val initialScore = world.score
		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertEquals(initialScore, frame.world.score)
	}

	@Test
	fun `intent animation name matches character SpriteState tag in world`() {
		val world = worldAt(x = 0f, y = 0f)
		val (frame, intent) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertNotNull(intent.animation)
		assertNotNull(frame.world.sprite.tag)
	}

	@Test
	fun `frame index advances after FRAME_ADVANCE_INTERVAL`() {
		val world = worldAt(x = 0f, y = 0f)
		val dt = Physics.FRAME_ADVANCE_INTERVAL + 0.001f
		val (frame, _) = WorldUpdate.tick(world, InputState(), dt, testCharacter, tileMap, 0..10)

		assertEquals(1, frame.world.sprite.frameIndex)
	}

	@Test
	fun `frame index resets when animation tag changes`() {
		val world = worldAt(x = 0f, y = 0f)
		val reg = world.registry
		val player = world.player
		reg.add(player, SpriteState(tag = "Run", frameIndex = 5))

		val changingCharacter = object : Character by testCharacter {
			override fun update(input: InputState, ctx: TickContext, dt: Float): CharacterIntent {
				val base = testCharacter.update(input, ctx, dt)
				return base.copy(animation = Animation.empty())
			}
		}

		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, changingCharacter, tileMap, 0..10)

		assertEquals(0, frame.world.sprite.frameIndex)
	}

	@Test
	fun `entrance phase transitions to playing when player lands on ground`() {
		val reg = EntityRegistry()
		val player = reg.create()
		reg.add(player, Transform(0f, 0f))
		reg.add(player, Velocity(0f, 0f))
		reg.add(player, PhysicsState(isOnGround = true))
		reg.add(player, SpriteState(tag = "Idle"))
		reg.add(player, PhaseState(GamePhase.ENTRANCE))
		reg.add(player, AABB(2, 2))
		val world = World(registry = reg, player = player)

		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertEquals(GamePhase.PLAYING, frame.world.phase)
	}

	@Test
	fun `entrance phase is preserved while player is airborne`() {
		val reg = EntityRegistry()
		val player = reg.create()
		reg.add(player, Transform(0f, -5f))
		reg.add(player, Velocity(0f, 5f))
		reg.add(player, PhysicsState(isOnGround = false))
		reg.add(player, SpriteState(tag = "Idle"))
		reg.add(player, PhaseState(GamePhase.ENTRANCE))
		reg.add(player, AABB(2, 2))
		val world = World(registry = reg, player = player)

		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertEquals(GamePhase.ENTRANCE, frame.world.phase)
	}
}

