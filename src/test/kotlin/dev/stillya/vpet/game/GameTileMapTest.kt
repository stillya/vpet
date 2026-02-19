package dev.stillya.vpet.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GameTileMapTest {

	private lateinit var tileMap: GameTileMap

	@Before
	fun setup() {
		tileMap = GameTileMap()
	}

	// --- computeExtent / hasGroundAt ---

	@Test
	fun `hasGroundAt returns false for empty line`() {
		tileMap.rebuildFromLines(listOf(""))
		assertFalse(tileMap.hasGroundAt(0, 0, 1))
	}

	@Test
	fun `hasGroundAt returns false for whitespace-only line`() {
		tileMap.rebuildFromLines(listOf("     "))
		assertFalse(tileMap.hasGroundAt(0, 0, 4))
	}

	@Test
	fun `hasGroundAt returns true when cat overlaps code extent`() {
		//                           0123456789...
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		// extent = 4..14
		// cat at cols 5..6 overlaps extent 4..14
		assertTrue(tileMap.hasGroundAt(0, 5, 6))
	}

	@Test
	fun `hasGroundAt returns true when cat overlaps left edge of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		// extent = 4..14, cat at cols 3..4, catRight(4) >= extent.first(4) => true
		assertTrue(tileMap.hasGroundAt(0, 3, 4))
	}

	@Test
	fun `hasGroundAt returns true when cat overlaps right edge of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		// extent = 4..14, cat at cols 14..15, catLeft(14) <= extent.last(14) => true
		assertTrue(tileMap.hasGroundAt(0, 14, 15))
	}

	@Test
	fun `hasGroundAt returns false when cat is entirely left of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		// extent = 4..14, cat at cols 1..2, catRight(2) >= extent.first(4) => false
		assertFalse(tileMap.hasGroundAt(0, 1, 2))
	}

	@Test
	fun `hasGroundAt returns false when cat is entirely right of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		// extent = 4..14, cat at cols 15..16, catLeft(15) <= extent.last(14) => false
		assertFalse(tileMap.hasGroundAt(0, 15, 16))
	}

	@Test
	fun `hasGroundAt bridges spaces between tokens`() {
		tileMap.rebuildFromLines(listOf("fun  foo()  {"))
		// extent = 0..12 (first 'f' to last '{')
		// cat at cols 4..5 is between 'fun' and 'foo' (spaces), but within extent
		assertTrue(tileMap.hasGroundAt(0, 4, 5))
	}

	@Test
	fun `hasGroundAt returns false for line not in map`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.hasGroundAt(5, 0, 1))
	}

	// --- isCharSolid (per-character, for wall collision) ---

	@Test
	fun `isCharSolid returns true for non-whitespace character`() {
		tileMap.rebuildFromLines(listOf("    fun foo()"))
		assertTrue(tileMap.isCharSolid(0, 4))  // 'f'
		assertTrue(tileMap.isCharSolid(0, 5))  // 'u'
		assertTrue(tileMap.isCharSolid(0, 6))  // 'n'
	}

	@Test
	fun `isCharSolid returns false for whitespace character`() {
		tileMap.rebuildFromLines(listOf("    fun foo()"))
		assertFalse(tileMap.isCharSolid(0, 0))  // space
		assertFalse(tileMap.isCharSolid(0, 3))  // space
		assertFalse(tileMap.isCharSolid(0, 7))  // space between 'fun' and 'foo'
	}

	@Test
	fun `isCharSolid returns false for column past end of line`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.isCharSolid(0, 5))
		assertFalse(tileMap.isCharSolid(0, 100))
	}

	@Test
	fun `isCharSolid returns false for negative column`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.isCharSolid(0, -1))
	}

	@Test
	fun `isCharSolid returns false for line not in map`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.isCharSolid(5, 0))
	}

	@Test
	fun `isCharSolid returns false for empty line`() {
		tileMap.rebuildFromLines(listOf(""))
		assertFalse(tileMap.isCharSolid(0, 0))
	}

	// --- findGroundBelow ---

	@Test
	fun `findGroundBelow finds first solid line scanning downward`() {
		tileMap.rebuildFromLines(listOf(
			"",               // 0 - empty
			"",               // 1 - empty
			"    code here",  // 2 - solid at 4..12
			"more code"       // 3 - solid at 0..8
		))
		// cat at cols 5..6, searching from line 0
		assertEquals(2, tileMap.findGroundBelow(0, 5, 6, 3))
	}

	@Test
	fun `findGroundBelow skips lines where cat doesn't overlap`() {
		tileMap.rebuildFromLines(listOf(
			"",                       // 0
			"ab",                     // 1 - extent 0..1
			"                  xy",   // 2 - extent 18..19
			"          code"          // 3 - extent 10..13
		))
		// cat at cols 10..11, searching from line 0
		// line 1: extent 0..1, catLeft(10) <= 1 => false → skip
		// line 2: extent 18..19, catRight(11) >= 18 => false → skip
		// line 3: extent 10..13, catLeft(10) <= 13 && catRight(11) >= 10 → hit
		assertEquals(3, tileMap.findGroundBelow(0, 10, 11, 3))
	}

	@Test
	fun `findGroundBelow returns null when no ground exists`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"",
			""
		))
		assertNull(tileMap.findGroundBelow(0, 0, 1, 2))
	}

	@Test
	fun `findGroundBelow respects startLine parameter`() {
		tileMap.rebuildFromLines(listOf(
			"code",   // 0 - solid
			"",       // 1
			"code"    // 2 - solid
		))
		// starting from line 1, should skip line 0
		assertEquals(2, tileMap.findGroundBelow(1, 0, 1, 2))
	}

	@Test
	fun `findGroundBelow respects maxLine parameter`() {
		tileMap.rebuildFromLines(listOf(
			"",       // 0
			"",       // 1
			"code"    // 2 - solid
		))
		// maxLine=1 means we stop before line 2
		assertNull(tileMap.findGroundBelow(0, 0, 1, 1))
	}

	// --- hasCeilingAt ---

	@Test
	fun `hasCeilingAt detects solid line above`() {
		tileMap.rebuildFromLines(listOf(
			"    code here",  // 0 - solid
			""                // 1
		))
		assertTrue(tileMap.hasCeilingAt(0, 5, 6))
	}

	@Test
	fun `hasCeilingAt returns false for empty line`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"code"
		))
		assertFalse(tileMap.hasCeilingAt(0, 0, 1))
	}

	// --- Multi-line realistic scenarios ---

	@Test
	fun `realistic code file - platform detection`() {
		tileMap.rebuildFromLines(listOf(
			"package dev.example",          // 0: extent 0..18
			"",                             // 1: empty
			"class Foo {",                  // 2: extent 0..10
			"    fun bar() {",              // 3: extent 4..15
			"        val x = 1",            // 4: extent 8..17
			"    }",                         // 5: extent 4..4
			"}"                              // 6: extent 0..0
		))

		// Cat at col 10..11 can stand on lines 0, 2, 3, 4
		assertTrue(tileMap.hasGroundAt(0, 10, 11))
		assertFalse(tileMap.hasGroundAt(1, 10, 11))
		assertTrue(tileMap.hasGroundAt(2, 10, 11))
		assertTrue(tileMap.hasGroundAt(3, 10, 11))
		assertTrue(tileMap.hasGroundAt(4, 10, 11))
		assertFalse(tileMap.hasGroundAt(5, 10, 11)) // extent 4..4, cat 10..11 doesn't overlap
		assertFalse(tileMap.hasGroundAt(6, 10, 11)) // extent 0..0, cat 10..11 doesn't overlap
	}

	@Test
	fun `realistic code file - wall detection per character`() {
		tileMap.rebuildFromLines(listOf(
			"    fun bar() {",  // extent 4..15
		))

		// 'f' at col 4 is solid
		assertTrue(tileMap.isCharSolid(0, 4))
		// space at col 7 (between 'fun' and 'bar') is NOT solid
		assertFalse(tileMap.isCharSolid(0, 7))
		// '{' at col 14 is solid
		assertTrue(tileMap.isCharSolid(0, 14))
		// space at col 15
		assertFalse(tileMap.isCharSolid(0, 15))
		// past end of line
		assertFalse(tileMap.isCharSolid(0, 16))
	}

	@Test
	fun `key difference between hasGroundAt and isCharSolid for spaces between tokens`() {
		tileMap.rebuildFromLines(listOf("fun  foo()"))
		// hasGroundAt treats the whole range 0..9 as solid platform (spaces bridged)
		assertTrue(tileMap.hasGroundAt(0, 3, 4))  // cols 3-4 is "  " between fun and foo

		// isCharSolid checks individual characters
		assertFalse(tileMap.isCharSolid(0, 3))  // space between 'fun' and 'foo'
		assertFalse(tileMap.isCharSolid(0, 4))  // space between 'fun' and 'foo'
		assertTrue(tileMap.isCharSolid(0, 5))   // 'f' of 'foo'
	}

	@Test
	fun `tab characters are treated as whitespace`() {
		tileMap.rebuildFromLines(listOf("\t\tcode"))
		// tabs at 0, 1 are whitespace
		assertFalse(tileMap.isCharSolid(0, 0))
		assertFalse(tileMap.isCharSolid(0, 1))
		assertTrue(tileMap.isCharSolid(0, 2)) // 'c'

		// extent is 2..5
		assertFalse(tileMap.hasGroundAt(0, 0, 1)) // cat at 0..1, extent starts at 2
		assertTrue(tileMap.hasGroundAt(0, 1, 2))   // cat at 1..2 overlaps extent.first(2)
	}
}
