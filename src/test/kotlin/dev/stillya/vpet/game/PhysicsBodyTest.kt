package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class PhysicsBodyTest {

	private lateinit var tileMap: VirtualTileMap

	private val testCharacter = object : Character {
		val walkSpeed = 9.0f
		val jumpVelocity = -15.6f

		override fun id() = EntityID("test")

		override fun collider() = AABB(width = 2, height = 2)

		override fun update(input: InputState, ctx: TickContext, dt: Float): CharacterIntent {
			val effectiveInput = if (ctx.phase == GamePhase.ENTRANCE) InputState() else input

			var vx = ctx.velocity.x
			var vy = ctx.velocity.y
			var direction = ctx.sprite.direction

			if (effectiveInput.moveDirection != 0) {
				direction = if (effectiveInput.moveDirection < 0) Direction.LEFT else Direction.RIGHT
			}

			if (ctx.isOnGround) {
				vx = if (effectiveInput.moveDirection != 0) {
					effectiveInput.moveDirection.toFloat() * walkSpeed
				} else {
					val damped = vx * exp(-GROUND_DAMPING * dt)
					if (abs(damped) < Physics.VELOCITY_EPSILON) 0f else damped
				}
			} else {
				if (effectiveInput.moveDirection != 0) {
					vx += effectiveInput.moveDirection.toFloat() * walkSpeed * AIR_CONTROL * dt
				}
			}

			if (effectiveInput.jumpJustPressed && ctx.isOnGround) {
				vy = jumpVelocity
			}

			val velocity = Velocity(vx, vy)

			var phase = ctx.phase
			if (phase == GamePhase.ENTRANCE && ctx.isOnGround) {
				phase = GamePhase.PLAYING
			}

			return CharacterIntent(velocity, Animation.empty(), direction, phase)
		}
	}

	companion object {
		private const val DT = 0.016f
		private const val GROUND_DAMPING = 18.0f
		private const val AIR_CONTROL = 0.3f
	}

	@Before
	fun setup() {
		tileMap = VirtualTileMap()
	}

	private fun buildMap(vararg lines: String) {
		tileMap.rebuildFromLines(lines.toList())
	}

	private fun tick(
		world: World,
		input: InputState = InputState(),
		dt: Float = DT,
		lastVisibleLine: Int = 20
	): World =
		WorldUpdate.tick(world, input, dt, testCharacter, tileMap, 0..lastVisibleLine).world

	private fun worldAt(
		x: Float = 0f,
		y: Float = 0f,
		vx: Float = 0f,
		vy: Float = 0f,
		isOnGround: Boolean = true,
		tag: String = "Idle"
	) = World(
		transform = Transform(x, y),
		velocity = Velocity(vx, vy),
		isOnGround = isOnGround,
		sprite = SpriteState(tag = tag)
	)

	@Test
	fun `standing on code line stays grounded`() {
		buildMap("", "    code here", "")
		val w = worldAt(x = 5f, y = 1f)
		val result = tick(w)

		assertEquals(1f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
	}

	@Test
	fun `walking across spaces between tokens stays grounded`() {
		buildMap("fun  foo()  {")
		val w = worldAt(x = 3f, y = 0f)
		val result = tick(w)

		assertEquals(0f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
	}

	@Test
	fun `gravity zeroes vy on grounded entity`() {
		buildMap("xxxxxxxxxxxxxxxxxxxx")
		val w = worldAt(x = 5f, y = 0f)
		val result = tick(w)

		assertEquals(0f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
		assertEquals(0f, result.velocity.y, 0.001f)
	}

	@Test
	fun `grounded re-derived each frame from collision`() {
		buildMap("ground", "")
		var w = worldAt(x = 0f, y = 0f)

		w = tick(w)
		assertTrue(w.isOnGround)

		w = w.copy(transform = Transform(10f, 0f))
		w = tick(w)
		assertFalse(w.isOnGround)
	}

	@Test
	fun `falls when ground ends`() {
		buildMap("short", "          long line here", "")
		var w = worldAt(x = 10f, y = 0f)

		for (i in 0..50) {
			w = tick(w)
			if (w.isOnGround) break
		}

		assertEquals(1f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
	}

	@Test
	fun `falls and lands on platform below`() {
		buildMap("", "", "platform", "")
		var w = worldAt(x = 3f, y = 0f, isOnGround = false)

		for (i in 0..200) {
			w = tick(w)
			if (w.isOnGround) break
		}

		assertEquals(2f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
		assertEquals(0f, w.velocity.y, 0.001f)
	}

	@Test
	fun `jump sets upward velocity and leaves ground`() {
		buildMap("", "", "", "", "", "platform")
		val w = worldAt(x = 0f, y = 5f)
		val input = InputState(jumpJustPressed = true)
		val result = tick(w, input)

		assertFalse(result.isOnGround)
		assertTrue(result.velocity.y < 0)
	}

	@Test
	fun `jump preserves horizontal momentum`() {
		buildMap("", "", "", "", "", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 5f, y = 5f)
		val walkInput = InputState(moveDirection = 1)
		w = tick(w, walkInput)
		assertTrue(w.velocity.x > 0)

		val jumpInput = InputState(moveDirection = 0, jumpJustPressed = true)
		w = tick(w, jumpInput)
		assertFalse(w.isOnGround)
		assertTrue(w.velocity.x > testCharacter.walkSpeed * 0.5f)
	}

	@Test
	fun `wall blocks from left`() {
		buildMap("", "          code here", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 8f, y = 2f)
		val input = InputState(moveDirection = 1)

		for (i in 0..50) {
			w = tick(w, input)
		}

		val catRight = kotlin.math.floor(w.transform.x).toInt() + 2 - 1
		assertTrue(catRight < 10)
	}

	@Test
	fun `wall blocks from right`() {
		buildMap("", "code here", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 10f, y = 2f)
		val input = InputState(moveDirection = -1)

		for (i in 0..50) {
			w = tick(w, input)
		}

		val catLeft = kotlin.math.floor(w.transform.x).toInt()
		assertTrue(catLeft > 8)
	}

	@Test
	fun `no wall block when already inside body extent`() {
		buildMap("xxxxxxxxxxxxxxxxxxxx", "    code")
		val w = worldAt(x = 5f, y = 1f)
		val input = InputState(moveDirection = 1)
		val result = tick(w, input)

		assertTrue(result.transform.x > 5f)
	}

	@Test
	fun `x does not go below zero`() {
		buildMap("code")
		val w = worldAt(x = 0f, y = 0f)
		val input = InputState(moveDirection = -1)
		val result = tick(w, input)

		assertEquals(0f, result.transform.x, 0.001f)
	}

	@Test
	fun `y clamped to top boundary`() {
		buildMap("", "", "")
		val w = worldAt(x = 0f, y = 0f, isOnGround = false, vy = -20f)
		val result = tick(w, lastVisibleLine = 2)

		assertEquals(0f, result.transform.y, 0.001f)
		assertFalse(result.isOnGround)
	}

	@Test
	fun `y clamped to bottom boundary grounds entity`() {
		buildMap("", "", "")
		val w = worldAt(x = 0f, y = 2f, isOnGround = false, vy = 20f)
		val result = tick(w, lastVisibleLine = 2)

		assertEquals(2f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
	}
}
