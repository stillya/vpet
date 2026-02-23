package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction
import org.junit.Assert.*
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
				} else {
					val damped = vx * exp(-AIR_DAMPING * dt)
					if (abs(damped) < Physics.VELOCITY_EPSILON) 0f else damped
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
		private const val AIR_DAMPING = 3.0f
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

	private fun ticks(
		initial: World,
		count: Int,
		input: InputState = InputState(),
		dt: Float = DT,
		lastVisibleLine: Int = 20
	): World {
		var w = initial
		for (i in 0 until count) {
			w = tick(w, input, dt, lastVisibleLine)
		}
		return w
	}

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

	// --- Ground standing ---

	@Test
	fun `cat stays on ground when standing on code line`() {
		buildMap("", "    code here", "")
		val w = worldAt(x = 5f, y = 1f)
		val result = tick(w)

		assertEquals(1f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
	}

	@Test
	fun `cat stays on ground walking across spaces between tokens`() {
		buildMap("fun  foo()  {")
		val w = worldAt(x = 3f, y = 0f)
		val result = tick(w)

		assertEquals(0f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
	}

	// --- Falling to next platform ---

	@Test
	fun `cat falls to next line when current ground ends`() {
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
	fun `cat falls to longer platform below when walking off shorter one`() {
		buildMap("  code", "  code with more stuff here", "")
		var w = worldAt(x = 6f, y = 0f)

		for (i in 0..50) {
			w = tick(w)
			if (w.isOnGround) break
		}

		assertEquals(1f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
	}

	@Test
	fun `cat falls when no ground below either`() {
		buildMap("short", "", "")
		val w = worldAt(x = 10f, y = 0f)
		val result = tick(w)

		assertFalse(result.isOnGround)
	}

	// --- Falling and landing ---

	@Test
	fun `cat falls and lands on platform below`() {
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
	fun `fast fall does not skip platform`() {
		buildMap("", "", "ground", "", "", "")
		var w = worldAt(x = 3f, y = 0f, isOnGround = false)

		for (i in 0..200) {
			w = tick(w)
			if (w.isOnGround) break
		}

		assertEquals(2f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
	}

	@Test
	fun `falling cat lands on first available platform`() {
		buildMap("", "ab", "", "  code here", "")
		var w = worldAt(x = 5f, y = 0f, isOnGround = false)

		for (i in 0..300) {
			w = tick(w)
			if (w.isOnGround) break
		}

		assertEquals(3f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
	}

	// --- Jump ---

	@Test
	fun `jump sets upward velocity and leaves ground`() {
		buildMap("", "", "", "", "", "platform")
		val w = worldAt(x = 0f, y = 5f)
		val input = InputState(jumpJustPressed = true)
		val result = tick(w, input)

		assertFalse(result.isOnGround)
		assertTrue(result.velocity.y < 0)
	}

	// --- Wall collision ---

	@Test
	fun `no wall block when cat already inside body extent`() {
		buildMap("xxxxxxxxxxxxxxxxxxxx", "    code")
		val w = worldAt(x = 5f, y = 1f)
		val input = InputState(moveDirection = 1)
		val result = tick(w, input)

		assertTrue(result.transform.x > 5f)
	}

	@Test
	fun `wall blocks cat entering body extent from left`() {
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
	fun `wall blocks cat entering body extent from right`() {
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
	fun `wall collision is symmetric - left and right speed same`() {
		buildMap("", "     code here", "xxxxxxxxxxxxxxxxxxxx")

		var w = worldAt(x = 3f, y = 2f)
		val rightInput = InputState(moveDirection = 1)
		for (i in 0..50) {
			w = tick(w, rightInput)
		}
		val rightBlockedX = w.transform.x

		w = worldAt(x = 16f, y = 2f)
		val leftInput = InputState(moveDirection = -1)
		for (i in 0..50) {
			w = tick(w, leftInput)
		}
		val leftBlockedX = w.transform.x

		assertTrue(rightBlockedX < 5f)
		assertTrue(leftBlockedX > 13f)
	}

	// --- Realistic scenario ---

	@Test
	fun `cat walks across short line and falls to longer continuation line`() {
		buildMap(
			"props.put(\"key.serializer\",",
			"        \"org.apache.kafka.common.serialization.StringSerializer\");",
			"props.put(\"value.serializer\","
		)

		var w = worldAt(x = 25f, y = 0f)
		val input = InputState(moveDirection = 1)

		for (i in 0..100) {
			w = tick(w, input)
			if (w.displayLine == 1) break
		}

		assertEquals(1f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
	}

	@Test
	fun `cat walks along long continuation line without falling`() {
		buildMap(
			"props.put(\"key.serializer\",",
			"        \"org.apache.kafka.common.serialization.StringSerializer\");",
			""
		)

		var w = worldAt(x = 30f, y = 1f)
		val input = InputState(moveDirection = 1)

		for (i in 0..50) {
			w = tick(w, input)
		}

		assertEquals(1f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
		assertTrue(w.transform.x > 30f)
	}

	// --- Edge cases ---

	@Test
	fun `colX does not go below zero`() {
		buildMap("code")
		val w = worldAt(x = 0f, y = 0f)
		val input = InputState(moveDirection = -1)
		val result = tick(w, input)

		assertEquals(0f, result.transform.x, 0.001f)
	}

	@Test
	fun `lineY clamped to visible area top boundary`() {
		buildMap("", "", "")
		val w = worldAt(x = 0f, y = 0f, isOnGround = false, vy = -20f)
		val result = tick(w, lastVisibleLine = 2)

		assertEquals(0f, result.transform.y, 0.001f)
		assertFalse(result.isOnGround)
	}

	@Test
	fun `lineY clamped to visible area bottom boundary grounds entity`() {
		buildMap("", "", "")
		val w = worldAt(x = 0f, y = 2f, isOnGround = false, vy = 20f)
		val result = tick(w, lastVisibleLine = 2)

		assertEquals(2f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
	}

	// --- Jump preserves horizontal momentum ---

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

	// --- AABB push ---

	@Test
	fun `AABB pushes cat to left edge when approaching wall from left`() {
		buildMap("", "          wall", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 7f, y = 2f)
		val input = InputState(moveDirection = 1)

		for (i in 0..50) {
			w = tick(w, input)
		}

		val catRight = kotlin.math.floor(w.transform.x).toInt() + 2 - 1
		assertTrue(catRight < 10)
	}

	@Test
	fun `AABB pushes cat to right edge when approaching wall from right`() {
		buildMap("", "code", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 6f, y = 2f)
		val input = InputState(moveDirection = -1)

		for (i in 0..50) {
			w = tick(w, input)
		}

		val catLeft = kotlin.math.floor(w.transform.x).toInt()
		assertTrue(catLeft > 3)
	}

	// --- Can't jump over wall ---

	@Test
	fun `cat cannot jump over wall and land inside solid row`() {
		buildMap("", "", "", "", "", "     wall", "xxxxxxxxxxxxxxxxxxxx", "", "")
		var w = worldAt(x = 3f, y = 6f)
		val jumpRight = InputState(moveDirection = 1, jumpJustPressed = true)
		w = tick(w, jumpRight)

		val moveRight = InputState(moveDirection = 1)
		for (i in 0..100) {
			w = tick(w, moveRight)
			val bodyLine = w.displayLine - 1
			val bodyExtent = tileMap.getExtent(bodyLine)
			if (bodyExtent != null) {
				val catLeft = kotlin.math.floor(w.transform.x).toInt()
				val catRight = catLeft + 2 - 1
				val overlapsBody = catLeft <= bodyExtent.last && catRight >= bodyExtent.first
				if (overlapsBody) {
					fail("Cat should never be inside the body extent of the wall")
				}
			}
		}
	}

	// --- Body line resolution after displayLine change ---

	@Test
	fun `cat pushed out of body extent when jumping changes displayLine`() {
		buildMap(
			"",
			"",
			"",
			"",
			"props.put(\"acks\", \"all\");",
			"props.put(\"key.serializer\",",
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
		)
		var w = worldAt(x = 26f, y = 6f)

		val jumpInput = InputState(jumpJustPressed = true)
		w = tick(w, jumpInput)

		val noInput = InputState()
		for (i in 0..200) {
			w = tick(w, noInput)
			if (w.displayLine == 5) {
				val bodyExtent = tileMap.getExtent(4)!!
				val catLeft = kotlin.math.floor(w.transform.x).toInt()
				val catRight = catLeft + 2 - 1
				assertFalse(
					"Cat should be pushed out of body extent when displayLine changes",
					catLeft <= bodyExtent.last && catRight >= bodyExtent.first
				)
				return
			}
		}
		fail("Cat never reached displayLine 5 during jump")
	}

	@Test
	fun `cat not pushed when body line unchanged`() {
		buildMap("long code line extending far", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 5f, y = 1f)
		val input = InputState(moveDirection = 1)

		for (i in 0..20) {
			w = tick(w, input)
		}

		assertTrue(w.transform.x > 5f)
		assertEquals(1f, w.transform.y, 0.001f)
	}

	// --- dt independence ---

	@Test
	fun `dt independence - single large step approximately equals two small steps`() {
		buildMap("", "xxxxxxxxxxxxxxxxxxxx")
		val initial = worldAt(x = 5f, y = 1f)
		val input = InputState(moveDirection = 1)

		val singleStep = tick(initial, input, dt = 0.032f)
		val doubleStep = ticks(initial, count = 2, input = input, dt = 0.016f)

		assertEquals(singleStep.transform.x, doubleStep.transform.x, 0.5f)
	}

	// --- Tunneling regression ---

	@Test
	fun `cat falling 20+ lines lands on platform without tunneling`() {
		val lines = mutableListOf<String>()
		for (i in 0..24) lines.add("")
		lines.add("xxxxxxxxxxxxxxxxxxxx") // line 25
		lines.add("")
		buildMap(*lines.toTypedArray())

		var w = worldAt(x = 5f, y = 0f, isOnGround = false)

		for (i in 0..500) {
			w = tick(w, lastVisibleLine = 26)
			if (w.isOnGround) break
		}

		assertEquals(25f, w.transform.y, 0.001f)
		assertTrue(w.isOnGround)
	}

	@Test
	fun `per-cell wall collision blocks cat at isolated solid column`() {
		buildMap("", "          code", "xxxxxxxxxxxxxxxxxxxx")
		var w = worldAt(x = 7f, y = 2f)
		val input = InputState(moveDirection = 1)

		for (i in 0..100) {
			w = tick(w, input)
		}

		val catRight = kotlin.math.floor(w.transform.x).toInt() + 2 - 1
		assertTrue("Cat should be blocked by solid at col 10, catRight=$catRight", catRight < 10)
	}

	// --- Grounded derived from collision ---

	@Test
	fun `grounded state re-derived each frame from collision not carried over`() {
		buildMap("ground", "")
		var w = worldAt(x = 0f, y = 0f)

		// Confirm grounded via collision on first tick
		w = tick(w)
		assertTrue(w.isOnGround)
		assertEquals(0f, w.transform.y, 0.001f)

		// Move cat off ground
		w = w.copy(transform = Transform(10f, 0f))
		w = tick(w)
		assertFalse(w.isOnGround)
	}

	@Test
	fun `gravity always applied - grounded entity has zero vy after collision`() {
		buildMap("xxxxxxxxxxxxxxxxxxxx")
		val w = worldAt(x = 5f, y = 0f)
		val result = tick(w)

		assertEquals(0f, result.transform.y, 0.001f)
		assertTrue(result.isOnGround)
		assertEquals(0f, result.velocity.y, 0.001f)
	}
}
