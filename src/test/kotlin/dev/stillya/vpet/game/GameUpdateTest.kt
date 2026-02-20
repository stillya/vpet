package dev.stillya.vpet.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameUpdateTest {

	private lateinit var tileMap: GameTileMap

	companion object {
		private const val DT = 0.016f
	}

	@Before
	fun setup() {
		tileMap = GameTileMap()
	}

	private fun buildMap(vararg lines: String) {
		tileMap.rebuildFromLines(lines.toList())
	}

	private fun tick(
		state: GameState,
		input: InputState = InputState(),
		dt: Float = DT,
		lastVisibleLine: Int = 20
	): GameState =
		GameUpdate.update(state, input, dt, tileMap, 0, lastVisibleLine)

	private fun ticks(
		initialState: GameState,
		count: Int,
		input: InputState = InputState(),
		dt: Float = DT,
		lastVisibleLine: Int = 20
	): GameState {
		var s = initialState
		for (i in 0 until count) {
			s = tick(s, input, dt, lastVisibleLine)
		}
		return s
	}

	// --- Ground standing ---

	@Test
	fun `cat stays on ground when standing on code line`() {
		buildMap(
			"",
			"    code here",
			""
		)
		val state = GameState(lineY = 1f, colX = 5f, isOnGround = true)
		val result = tick(state)

		assertEquals(1f, result.lineY, 0.001f)
		assertTrue(result.isOnGround)
	}

	@Test
	fun `cat stays on ground walking across spaces between tokens`() {
		buildMap("fun  foo()  {")
		val state = GameState(lineY = 0f, colX = 3f, isOnGround = true)
		val result = tick(state)

		assertEquals(0f, result.lineY, 0.001f)
		assertTrue(result.isOnGround)
	}

	// --- Step-down ---

	@Test
	fun `cat steps down to next line when current ground ends`() {
		buildMap(
			"short",
			"          long line here",
			""
		)
		val state = GameState(lineY = 0f, colX = 10f, isOnGround = true)
		val result = tick(state)

		assertEquals(1f, result.lineY, 0.001f)
		assertTrue("Cat should step down and stay on ground", result.isOnGround)
	}

	@Test
	fun `cat steps down when walking off shorter platform onto longer one below`() {
		buildMap(
			"  code",
			"  code with more stuff here",
			""
		)
		val state = GameState(lineY = 0f, colX = 6f, isOnGround = true)
		val result = tick(state)

		assertEquals(1f, result.lineY, 0.001f)
		assertTrue(result.isOnGround)
	}

	@Test
	fun `cat falls when no ground below either`() {
		buildMap(
			"short",
			"",
			""
		)
		val state = GameState(lineY = 0f, colX = 10f, isOnGround = true)
		val result = tick(state)

		assertFalse("Cat should start falling", result.isOnGround)
	}

	// --- Falling and landing ---

	@Test
	fun `cat falls and lands on platform below`() {
		buildMap(
			"",
			"",
			"platform",
			""
		)
		var state = GameState(lineY = 0f, colX = 3f, isOnGround = false, velocityY = 0f)

		for (i in 0..200) {
			state = tick(state)
			if (state.isOnGround) break
		}

		assertEquals(2f, state.lineY, 0.001f)
		assertTrue(state.isOnGround)
		assertEquals(0f, state.velocityY, 0.001f)
	}

	@Test
	fun `fast fall does not skip platform`() {
		buildMap(
			"",
			"",
			"ground",
			"",
			"",
			""
		)
		var state = GameState(lineY = 0f, colX = 3f, isOnGround = false, velocityY = 0f)

		for (i in 0..200) {
			state = tick(state)
			if (state.isOnGround) break
		}

		assertEquals(2f, state.lineY, 0.001f)
		assertTrue(state.isOnGround)
	}

	@Test
	fun `falling cat lands on first available platform`() {
		buildMap(
			"",
			"ab",
			"",
			"  code here",
			""
		)
		var state = GameState(lineY = 0f, colX = 5f, isOnGround = false, velocityY = 0f)

		for (i in 0..300) {
			state = tick(state)
			if (state.isOnGround) break
		}

		assertEquals(3f, state.lineY, 0.001f)
		assertTrue(state.isOnGround)
	}

	// --- Jump ---

	@Test
	fun `jump sets upward velocity and leaves ground`() {
		buildMap(
			"",
			"",
			"",
			"",
			"",
			"platform"
		)
		val state = GameState(lineY = 5f, colX = 0f, isOnGround = true)
		val input = InputState(jumpJustPressed = true)
		val result = tick(state, input)

		assertFalse(result.isOnGround)
		assertTrue(result.velocityY < 0)
	}

	// --- Wall collision (extent-overlap approach) ---

	@Test
	fun `no wall block when cat already inside body extent`() {
		buildMap(
			"xxxxxxxxxxxxxxxxxxxx",
			"    code",
		)
		val state = GameState(lineY = 1f, colX = 5f, isOnGround = true)
		val input = InputState(moveDirection = 1)
		val result = tick(state, input)

		assertTrue(result.colX > 5f)
	}

	@Test
	fun `wall blocks cat entering body extent from left`() {
		buildMap(
			"",
			"          code here",
			"xxxxxxxxxxxxxxxxxxxx",
		)
		var state = GameState(lineY = 2f, colX = 8f, isOnGround = true)
		val input = InputState(moveDirection = 1)

		for (i in 0..50) {
			state = tick(state, input)
		}

		val catRight = kotlin.math.floor(state.colX).toInt() + Physics.CAT_WIDTH - 1
		assertTrue("Cat should be blocked by wall at body extent", catRight < 10)
	}

	@Test
	fun `wall blocks cat entering body extent from right`() {
		buildMap(
			"",
			"code here",
			"xxxxxxxxxxxxxxxxxxxx",
		)
		var state = GameState(lineY = 2f, colX = 10f, isOnGround = true)
		val input = InputState(moveDirection = -1)

		for (i in 0..50) {
			state = tick(state, input)
		}

		val catLeft = kotlin.math.floor(state.colX).toInt()
		assertTrue("Cat should be blocked by wall at body extent", catLeft > 8)
	}

	@Test
	fun `wall collision is symmetric - left and right speed same`() {
		buildMap(
			"",
			"     code here",
			"xxxxxxxxxxxxxxxxxxxx",
		)

		var state = GameState(lineY = 2f, colX = 3f, isOnGround = true)
		val rightInput = InputState(moveDirection = 1)
		for (i in 0..50) {
			state = tick(state, rightInput)
		}
		val rightBlockedX = state.colX

		state = GameState(lineY = 2f, colX = 16f, isOnGround = true)
		val leftInput = InputState(moveDirection = -1)
		for (i in 0..50) {
			state = tick(state, leftInput)
		}
		val leftBlockedX = state.colX

		assertTrue("Right approach should stop near extent start", rightBlockedX < 5f)
		assertTrue("Left approach should stop near extent end", leftBlockedX > 13f)
	}

	// --- Realistic scenario: Java code with continuation lines ---

	@Test
	fun `cat walks across short line and steps down to longer continuation line`() {
		buildMap(
			"props.put(\"key.serializer\",",
			"        \"org.apache.kafka.common.serialization.StringSerializer\");",
			"props.put(\"value.serializer\",",
		)

		var state = GameState(lineY = 0f, colX = 25f, isOnGround = true)
		val input = InputState(moveDirection = 1)

		for (i in 0..100) {
			state = tick(state, input)
			if (state.displayLine == 1) break
		}

		assertEquals(1f, state.lineY, 0.001f)
		assertTrue("Cat should still be on ground after step-down", state.isOnGround)
	}

	@Test
	fun `cat walks along long continuation line without falling`() {
		buildMap(
			"props.put(\"key.serializer\",",
			"        \"org.apache.kafka.common.serialization.StringSerializer\");",
			""
		)

		var state = GameState(lineY = 1f, colX = 30f, isOnGround = true)
		val input = InputState(moveDirection = 1)

		for (i in 0..50) {
			state = tick(state, input)
		}

		assertEquals(1f, state.lineY, 0.001f)
		assertTrue(state.isOnGround)
		assertTrue("Cat should have moved right", state.colX > 30f)
	}

	// --- Edge cases ---

	@Test
	fun `colX does not go below zero`() {
		buildMap("code")
		val state = GameState(lineY = 0f, colX = 0f, isOnGround = true)
		val input = InputState(moveDirection = -1)
		val result = tick(state, input)

		assertEquals(0f, result.colX, 0.001f)
	}

	@Test
	fun `lineY clamped to visible area`() {
		buildMap("", "", "")
		val state = GameState(lineY = 0f, isOnGround = false, velocityY = -20f)
		val result = tick(state, lastVisibleLine = 2)

		assertEquals(0f, result.lineY, 0.001f)
		assertTrue(result.isOnGround)
	}

	// --- NEW: Jump preserves horizontal momentum ---

	@Test
	fun `jump preserves horizontal momentum`() {
		buildMap(
			"",
			"",
			"",
			"",
			"",
			"xxxxxxxxxxxxxxxxxxxx",
		)
		var state = GameState(lineY = 5f, colX = 5f, isOnGround = true)
		val walkInput = InputState(moveDirection = 1)
		state = tick(state, walkInput)
		assertTrue("Should be walking right", state.velocityX > 0)

		val jumpInput = InputState(moveDirection = 0, jumpJustPressed = true)
		state = tick(state, jumpInput)
		assertFalse(state.isOnGround)
		assertTrue("Horizontal velocity should be preserved in air", state.velocityX > Physics.WALK_SPEED * 0.5f)
	}

	// --- NEW: AABB pushOut to correct edge ---

	@Test
	fun `AABB pushes cat to left edge when approaching wall from left`() {
		buildMap(
			"",
			"          wall",
			"xxxxxxxxxxxxxxxxxxxx",
		)
		var state = GameState(lineY = 2f, colX = 7f, isOnGround = true)
		val input = InputState(moveDirection = 1)

		for (i in 0..50) {
			state = tick(state, input)
		}

		val catRight = kotlin.math.floor(state.colX).toInt() + Physics.CAT_WIDTH - 1
		assertTrue("Cat right edge should stop before wall extent start (10)", catRight < 10)
	}

	@Test
	fun `AABB pushes cat to right edge when approaching wall from right`() {
		buildMap(
			"",
			"code",
			"xxxxxxxxxxxxxxxxxxxx",
		)
		var state = GameState(lineY = 2f, colX = 6f, isOnGround = true)
		val input = InputState(moveDirection = -1)

		for (i in 0..50) {
			state = tick(state, input)
		}

		val catLeft = kotlin.math.floor(state.colX).toInt()
		assertTrue("Cat left edge should stop after wall extent end (3)", catLeft > 3)
	}

	// --- NEW: Can't jump over wall ---

	@Test
	fun `cat cannot jump over wall and land inside solid row`() {
		buildMap(
			"",
			"",
			"",
			"",
			"",
			"     wall",
			"xxxxxxxxxxxxxxxxxxxx",
			"",
			"",
		)
		var state = GameState(lineY = 6f, colX = 3f, isOnGround = true)
		val jumpRight = InputState(moveDirection = 1, jumpJustPressed = true)
		state = tick(state, jumpRight)

		val moveRight = InputState(moveDirection = 1)
		for (i in 0..100) {
			state = tick(state, moveRight)
			val bodyLine = state.displayLine - 1
			val bodyExtent = tileMap.getExtent(bodyLine)
			if (bodyExtent != null) {
				val catLeft = state.catLeft
				val catRight = state.catRight
				val overlapsBody = catLeft <= bodyExtent.last && catRight >= bodyExtent.first
				if (overlapsBody) {
					fail("Cat should never be inside the body extent of the wall")
				}
			}
		}
	}

	// --- NEW: Body line resolution after displayLine change ---

	@Test
	fun `cat pushed out of body extent when jumping changes displayLine`() {
		// Cat jumps from line 6, moves horizontally during jump into body extent of new displayLine
		buildMap(
			"",
			"",
			"",
			"",
			"props.put(\"acks\", \"all\");",       // 4: extent 0..24
			"props.put(\"key.serializer\",",        // 5: extent 0..29
			"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",  // 6: ground
		)
		// Cat at col 26, outside line 4's extent (0..24) but inside line 5's (0..29)
		var state = GameState(lineY = 6f, colX = 26f, isOnGround = true)

		// Jump
		val jumpInput = InputState(jumpJustPressed = true)
		state = tick(state, jumpInput)

		// Continue ticking until displayLine changes to 5 (body line becomes 4)
		val noInput = InputState()
		for (i in 0..200) {
			state = tick(state, noInput)
			// Once displayLine becomes 5, body line is 4 (extent 0..24)
			// Cat at col ~26 should be pushed out of extent 0..24
			if (state.displayLine == 5) {
				val bodyExtent = tileMap.getExtent(4)!!
				val catLeft = state.catLeft
				val catRight = state.catRight
				assertFalse(
					"Cat should be pushed out of body extent when displayLine changes",
					catLeft <= bodyExtent.last && catRight >= bodyExtent.first
				)
				return
			}
		}
		// Cat should have reached displayLine 5 at some point
		fail("Cat never reached displayLine 5 during jump")
	}

	@Test
	fun `cat not pushed when body line unchanged`() {
		// Cat walks normally — body line doesn't change, no push should happen
		buildMap(
			"long code line extending far",  // 0: body line, extent 0..28
			"xxxxxxxxxxxxxxxxxxxx",           // 1: ground
		)
		var state = GameState(lineY = 1f, colX = 5f, isOnGround = true)
		val input = InputState(moveDirection = 1)

		for (i in 0..20) {
			state = tick(state, input)
		}

		assertTrue("Cat should have moved right while walking", state.colX > 5f)
		assertEquals(1f, state.lineY, 0.001f)
	}

	// --- NEW: dt independence ---

	@Test
	fun `dt independence - single large step approximately equals two small steps`() {
		buildMap(
			"",
			"xxxxxxxxxxxxxxxxxxxx",
		)
		val initial = GameState(lineY = 1f, colX = 5f, isOnGround = true)
		val input = InputState(moveDirection = 1)

		val singleStep = tick(initial, input, dt = 0.032f)
		val doubleStep = ticks(initial, count = 2, input = input, dt = 0.016f)

		assertEquals(singleStep.colX, doubleStep.colX, 0.5f)
	}
}
