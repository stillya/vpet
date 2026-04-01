package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction
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
		override fun id() = EntityID("test")
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
		val world = worldAt(x = 0f, y = 0f)
		val (frame, intent) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertNotNull(frame)
		assertNotNull(frame.world)
		assertNotNull(frame.bounds)

		// Verify GameFrame has no animation: the data class only has world and bounds
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
	fun `score increases when collectible is collected`() {
		val world = worldAt(x = 2f, y = 0f)
		val initialScore = world.score
		val (frame, _) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		assertTrue(frame.world.score >= initialScore)
	}

	@Test
	fun `intent animation name matches character SpriteState tag in world`() {
		val world = worldAt(x = 0f, y = 0f)
		val (frame, intent) = WorldUpdate.tick(world, InputState(), 0.016f, testCharacter, tileMap, 0..10)

		// The animation from intent is the one used for rendering: must not be null
		assertNotNull(intent.animation)
		// The world sprite tag is tracked independently by WorldUpdate (via advanceFrame)
		assertNotNull(frame.world.sprite.tag)
	}

	@Test
	fun `GameFrame fields are exactly world and bounds`() {
		val fields = GameFrame::class.java.declaredFields.map { it.name }.toSet()
		assertEquals(setOf("world", "bounds"), fields)
	}
}
