package dev.stillya.vpet.game

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VirtualTileMapTest {

	private lateinit var tileMap: VirtualTileMap

	@Before
	fun setup() {
		tileMap = VirtualTileMap()
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
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		assertTrue(tileMap.hasGroundAt(0, 5, 6))
	}

	@Test
	fun `hasGroundAt returns true when cat overlaps left edge of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		assertTrue(tileMap.hasGroundAt(0, 3, 4))
	}

	@Test
	fun `hasGroundAt returns true when cat overlaps right edge of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		assertTrue(tileMap.hasGroundAt(0, 14, 15))
	}

	@Test
	fun `hasGroundAt returns false when cat is entirely left of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		assertFalse(tileMap.hasGroundAt(0, 1, 2))
	}

	@Test
	fun `hasGroundAt returns false when cat is entirely right of extent`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {"))
		assertFalse(tileMap.hasGroundAt(0, 15, 16))
	}

	@Test
	fun `hasGroundAt bridges spaces between tokens`() {
		tileMap.rebuildFromLines(listOf("fun  foo()  {"))
		assertTrue(tileMap.hasGroundAt(0, 4, 5))
	}

	@Test
	fun `hasGroundAt returns false for line not in map`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.hasGroundAt(5, 0, 1))
	}

	// --- isSolid (per-cell wall collision) ---

	@Test
	fun `isSolid returns true for non-whitespace character`() {
		tileMap.rebuildFromLines(listOf("    fun foo()"))
		assertTrue(tileMap.isSolid(0, 4))
		assertTrue(tileMap.isSolid(0, 5))
		assertTrue(tileMap.isSolid(0, 6))
	}

	@Test
	fun `isSolid returns false for whitespace character`() {
		tileMap.rebuildFromLines(listOf("    fun foo()"))
		assertFalse(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 3))
		assertFalse(tileMap.isSolid(0, 7))
	}

	@Test
	fun `isSolid returns false for column past end of line`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.isSolid(0, 5))
		assertFalse(tileMap.isSolid(0, 100))
	}

	@Test
	fun `isSolid returns false for negative column`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.isSolid(0, -1))
	}

	@Test
	fun `isSolid returns false for line not in map`() {
		tileMap.rebuildFromLines(listOf("hello"))
		assertFalse(tileMap.isSolid(5, 0))
	}

	@Test
	fun `isSolid returns false for empty line`() {
		tileMap.rebuildFromLines(listOf(""))
		assertFalse(tileMap.isSolid(0, 0))
	}

	// --- findGroundBelow ---

	@Test
	fun `findGroundBelow finds first solid line scanning downward`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"",
			"    code here",
			"more code"
		))
		assertEquals(2, tileMap.findGroundBelow(0, 5, 6, 3))
	}

	@Test
	fun `findGroundBelow skips lines where cat doesn't overlap`() {
		tileMap.rebuildFromLines(listOf(
			"",
			"ab",
			"                  xy",
			"          code"
		))
		assertEquals(3, tileMap.findGroundBelow(0, 10, 11, 3))
	}

	@Test
	fun `findGroundBelow returns null when no ground exists`() {
		tileMap.rebuildFromLines(listOf("", "", ""))
		assertNull(tileMap.findGroundBelow(0, 0, 1, 2))
	}

	@Test
	fun `findGroundBelow respects startLine parameter`() {
		tileMap.rebuildFromLines(listOf("code", "", "code"))
		assertEquals(2, tileMap.findGroundBelow(1, 0, 1, 2))
	}

	@Test
	fun `findGroundBelow respects maxLine parameter`() {
		tileMap.rebuildFromLines(listOf("", "", "code"))
		assertNull(tileMap.findGroundBelow(0, 0, 1, 1))
	}

	// --- hasCeilingAt ---

	@Test
	fun `hasCeilingAt detects solid line above`() {
		tileMap.rebuildFromLines(listOf("    code here", ""))
		assertTrue(tileMap.hasCeilingAt(0, 5, 6))
	}

	@Test
	fun `hasCeilingAt returns false for empty line`() {
		tileMap.rebuildFromLines(listOf("", "code"))
		assertFalse(tileMap.hasCeilingAt(0, 0, 1))
	}

	// --- Multi-line realistic scenarios ---

	@Test
	fun `realistic code file - platform detection`() {
		tileMap.rebuildFromLines(listOf(
			"package dev.example",
			"",
			"class Foo {",
			"    fun bar() {",
			"        val x = 1",
			"    }",
			"}"
		))

		assertTrue(tileMap.hasGroundAt(0, 10, 11))
		assertFalse(tileMap.hasGroundAt(1, 10, 11))
		assertTrue(tileMap.hasGroundAt(2, 10, 11))
		assertTrue(tileMap.hasGroundAt(3, 10, 11))
		assertTrue(tileMap.hasGroundAt(4, 10, 11))
		assertFalse(tileMap.hasGroundAt(5, 10, 11))
		assertFalse(tileMap.hasGroundAt(6, 10, 11))
	}

	@Test
	fun `realistic code file - wall detection per cell`() {
		tileMap.rebuildFromLines(listOf("    fun bar() {"))

		assertTrue(tileMap.isSolid(0, 4))   // 'f'
		assertFalse(tileMap.isSolid(0, 7))  // space between 'fun' and 'bar'
		assertTrue(tileMap.isSolid(0, 14))  // '{'
		assertFalse(tileMap.isSolid(0, 15)) // past end
	}

	@Test
	fun `key difference between hasGroundAt and isSolid for spaces between tokens`() {
		tileMap.rebuildFromLines(listOf("fun  foo()"))
		// hasGroundAt treats the whole range 0..9 as solid platform (spaces bridged)
		assertTrue(tileMap.hasGroundAt(0, 3, 4))

		// isSolid checks individual cells
		assertFalse(tileMap.isSolid(0, 3))
		assertFalse(tileMap.isSolid(0, 4))
		assertTrue(tileMap.isSolid(0, 5))
	}

	@Test
	fun `tab characters are treated as whitespace`() {
		tileMap.rebuildFromLines(listOf("\t\tcode"))
		assertFalse(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 1))
		assertTrue(tileMap.isSolid(0, 2))

		assertFalse(tileMap.hasGroundAt(0, 0, 1))
		assertTrue(tileMap.hasGroundAt(0, 1, 2))
	}

	// --- Per-cell wall tests ---

	@Test
	fun `isSolid detects wall at exact column in sparse line`() {
		tileMap.rebuildFromLines(listOf("a     b"))
		assertTrue(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 3))
		assertTrue(tileMap.isSolid(0, 6))
	}

	@Test
	fun `getExtent spans first to last solid cell`() {
		tileMap.rebuildFromLines(listOf("  ab  cd  "))
		val extent = tileMap.getExtent(0)
		assertNotNull(extent)
		assertEquals(2, extent!!.first)
		assertEquals(7, extent.last)
	}
}
