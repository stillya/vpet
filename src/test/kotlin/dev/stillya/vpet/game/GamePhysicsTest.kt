package dev.stillya.vpet.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GamePhysicsTest {

	private lateinit var tileMap: GameTileMap
	private lateinit var physics: GamePhysics

	@Before
	fun setup() {
		tileMap = GameTileMap()
		physics = GamePhysics(tileMap)
	}

	private fun buildMap(vararg lines: String) {
		tileMap.rebuildFromLines(lines.toList())
	}

	private fun tick(lastVisibleLine: Int = 20) {
		physics.step(0, lastVisibleLine)
	}

	// --- Ground standing ---

	@Test
	fun `cat stays on ground when standing on code line`() {
		buildMap(
			"",                // 0
			"    code here",   // 1: extent 4..12
			""                 // 2
		)
		physics.lineY = 1f
		physics.colX = 5f
		physics.isOnGround = true

		tick()

		assertEquals(1f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
	}

	@Test
	fun `cat stays on ground walking across spaces between tokens`() {
		buildMap("fun  foo()  {")  // extent 0..12, spaces at cols 3-4 and 10-11
		physics.lineY = 0f
		physics.colX = 3f  // on the space between 'fun' and 'foo'
		physics.isOnGround = true

		tick()

		assertEquals(0f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
	}

	// --- Step-down ---

	@Test
	fun `cat steps down to next line when current ground ends`() {
		buildMap(
			"short",                    // 0: extent 0..4
			"          long line here", // 1: extent 10..23
			""                          // 2
		)
		physics.lineY = 0f
		physics.colX = 10f  // past extent of line 0 (0..4), within extent of line 1 (10..23)
		physics.isOnGround = true

		tick()

		assertEquals(1f, physics.lineY, 0.001f)
		assertTrue("Cat should step down and stay on ground", physics.isOnGround)
	}

	@Test
	fun `cat steps down when walking off shorter platform onto longer one below`() {
		buildMap(
			"  code",                        // 0: extent 2..5
			"  code with more stuff here",   // 1: extent 2..27
			""                               // 2
		)
		physics.lineY = 0f
		physics.colX = 6f  // past line 0's extent (2..5), within line 1's (2..27)
		physics.isOnGround = true

		tick()

		assertEquals(1f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
	}

	@Test
	fun `cat falls when no ground below either`() {
		buildMap(
			"short",   // 0: extent 0..4
			"",        // 1: empty
			""         // 2: empty
		)
		physics.lineY = 0f
		physics.colX = 10f  // past extent of line 0, no ground on line 1
		physics.isOnGround = true

		tick()

		assertFalse("Cat should start falling", physics.isOnGround)
	}

	// --- Falling and landing ---

	@Test
	fun `cat falls and lands on platform below`() {
		buildMap(
			"",          // 0: empty
			"",          // 1: empty
			"platform",  // 2: extent 0..7
			""           // 3
		)
		physics.lineY = 0f
		physics.colX = 3f
		physics.isOnGround = false
		physics.velocityY = 0f

		// Run enough ticks to fall from line 0 to line 2
		for (i in 0..200) {
			tick()
			if (physics.isOnGround) break
		}

		assertEquals(2f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
		assertEquals(0f, physics.velocityY, 0.001f)
	}

	@Test
	fun `fast fall does not skip platform - scanStart fix`() {
		buildMap(
			"",          // 0
			"",          // 1
			"ground",    // 2: extent 0..5
			"",          // 3
			"",          // 4
			""           // 5
		)
		physics.lineY = 0f
		physics.colX = 3f
		physics.isOnGround = false
		physics.velocityY = 3f  // very fast fall — would jump from line 0 to line 3+ in one tick

		tick()

		// Should still land on line 2, not skip it
		assertEquals(2f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
	}

	@Test
	fun `falling cat lands on first available platform`() {
		buildMap(
			"",             // 0
			"ab",           // 1: extent 0..1
			"",             // 2
			"  code here",  // 3: extent 2..10
			""              // 4
		)
		physics.lineY = 0f
		physics.colX = 5f  // past line 1's extent (0..1), within line 3's (2..10)
		physics.isOnGround = false
		physics.velocityY = 0f

		for (i in 0..300) {
			tick()
			if (physics.isOnGround) break
		}

		assertEquals(3f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
	}

	// --- Jump ---

	@Test
	fun `jump sets upward velocity and leaves ground`() {
		physics.lineY = 5f
		physics.isOnGround = true

		physics.jump()

		assertFalse(physics.isOnGround)
		assertTrue(physics.velocityY < 0)
	}

	// --- Wall collision (extent-overlap approach) ---

	@Test
	fun `no wall block when cat already inside body extent`() {
		buildMap(
			"xxxxxxxxxxxxxxxxxxxx",  // 0: solid everywhere (bodyLine for line 1)
			"    code",              // 1: extent 4..7
		)
		physics.lineY = 1f
		physics.colX = 5f
		physics.isOnGround = true
		physics.inputDirection = 1  // walk right

		tick()

		// Cat already overlaps body extent, so movement is not blocked
		assertTrue(physics.colX > 5f)
	}

	@Test
	fun `wall blocks cat entering body extent from left`() {
		buildMap(
			"",                      // 0
			"          code here",   // 1: bodyLine, extent 10..18
			"xxxxxxxxxxxxxxxxxxxx",  // 2: ground
		)
		physics.lineY = 2f
		physics.colX = 8f  // catLeft=8, catRight=9, outside extent 10..18
		physics.isOnGround = true
		physics.inputDirection = 1  // walking right toward the wall

		// Walk until blocked or well past where wall should stop us
		for (i in 0..20) {
			tick()
		}

		// Cat should be blocked before entering the body extent
		// catRight should not reach 10 (extent.first)
		val catRight = kotlin.math.floor(physics.colX).toInt() + GamePhysics.CAT_WIDTH - 1
		assertTrue("Cat should be blocked by wall at body extent", catRight < 10)
	}

	@Test
	fun `wall blocks cat entering body extent from right`() {
		buildMap(
			"",                      // 0
			"code here",             // 1: bodyLine, extent 0..8
			"xxxxxxxxxxxxxxxxxxxx",  // 2: ground
		)
		physics.lineY = 2f
		physics.colX = 10f  // catLeft=10, outside extent 0..8
		physics.isOnGround = true
		physics.inputDirection = -1  // walking left toward the wall

		for (i in 0..20) {
			tick()
		}

		// Cat should be blocked before entering the body extent
		val catLeft = kotlin.math.floor(physics.colX).toInt()
		assertTrue("Cat should be blocked by wall at body extent", catLeft > 8)
	}

	@Test
	fun `wall collision is symmetric - left and right speed same`() {
		buildMap(
			"",                      // 0
			"     code here",        // 1: bodyLine, extent 5..13
			"xxxxxxxxxxxxxxxxxxxx",  // 2: ground
		)

		// Test walking right into wall
		physics.lineY = 2f
		physics.colX = 3f
		physics.isOnGround = true
		physics.inputDirection = 1
		for (i in 0..30) tick()
		val rightBlockedX = physics.colX

		// Reset and test walking left into wall
		physics.colX = 16f
		physics.velocityX = 0f
		physics.inputDirection = -1
		for (i in 0..30) tick()
		val leftBlockedX = physics.colX

		// Both sides should be stopped near the extent edges
		assertTrue("Right approach should stop near extent start", rightBlockedX < 5f)
		assertTrue("Left approach should stop near extent end", leftBlockedX > 13f)
	}

	// --- Realistic scenario: Java code with continuation lines ---

	@Test
	fun `cat walks across short line and steps down to longer continuation line`() {
		buildMap(
			"props.put(\"key.serializer\",",                                      // 0: extent 0..30
			"        \"org.apache.kafka.common.serialization.StringSerializer\");", // 1: extent 8..72
			"props.put(\"value.serializer\",",                                     // 2: extent 0..32
		)

		physics.lineY = 0f
		physics.colX = 25f  // on line 0's extent (0..30)
		physics.isOnGround = true
		physics.inputDirection = 1  // walking right

		// Walk until past line 0's extent
		for (i in 0..100) {
			tick()
			if (physics.displayLine == 1) break
		}

		// Cat should have stepped down to line 1, not fallen
		assertEquals(1f, physics.lineY, 0.001f)
		assertTrue("Cat should still be on ground after step-down", physics.isOnGround)
	}

	@Test
	fun `cat walks along long continuation line without falling`() {
		buildMap(
			"props.put(\"key.serializer\",",                                      // 0
			"        \"org.apache.kafka.common.serialization.StringSerializer\");", // 1: extent 8..72
			""                                                                      // 2
		)

		physics.lineY = 1f
		physics.colX = 30f  // middle of line 1
		physics.isOnGround = true
		physics.inputDirection = 1

		// Walk right for many ticks — should stay on line 1 until past column 72
		for (i in 0..50) {
			tick()
		}

		assertEquals(1f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
		assertTrue("Cat should have moved right", physics.colX > 30f)
	}

	// --- Edge cases ---

	@Test
	fun `colX does not go below zero`() {
		buildMap("code")
		physics.lineY = 0f
		physics.colX = 0f
		physics.isOnGround = true
		physics.inputDirection = -1

		tick()

		assertEquals(0f, physics.colX, 0.001f)
	}

	@Test
	fun `lineY clamped to visible area`() {
		buildMap("", "", "")
		physics.lineY = 0f
		physics.isOnGround = false
		physics.velocityY = -1f  // going above visible area

		tick(lastVisibleLine = 2)

		assertEquals(0f, physics.lineY, 0.001f)
		assertTrue(physics.isOnGround)
	}
}
