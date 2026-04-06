package dev.stillya.vpet.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VirtualTileMapTest {

	private lateinit var tileMap: VirtualTileMap

	@Before
	fun setup() {
		tileMap = VirtualTileMap()
	}

	@Test
	fun `hasGroundAt handles overlap and missing-row cases`() {
		tileMap.rebuildFromLines(listOf("    fun foo() {", "     "))

		assertTrue(tileMap.hasGroundAt(0, 5, 6))
		assertTrue(tileMap.hasGroundAt(0, 3, 4))
		assertTrue(tileMap.hasGroundAt(0, 14, 15))
		assertFalse(tileMap.hasGroundAt(0, 1, 2))
		assertFalse(tileMap.hasGroundAt(0, 15, 16))
		assertFalse(tileMap.hasGroundAt(1, 0, 4))
		assertFalse(tileMap.hasGroundAt(5, 0, 1))
	}

	@Test
	fun `hasGroundAt bridges spaces between tokens`() {
		tileMap.rebuildFromLines(listOf("fun  foo()  {"))
		assertTrue(tileMap.hasGroundAt(0, 4, 5))
	}

	@Test
	fun `isSolid distinguishes solid cells from whitespace and bounds`() {
		tileMap.rebuildFromLines(listOf("    fun foo()", ""))

		assertTrue(tileMap.isSolid(0, 4))
		assertTrue(tileMap.isSolid(0, 5))
		assertTrue(tileMap.isSolid(0, 6))
		assertFalse(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 3))
		assertFalse(tileMap.isSolid(0, 7))
		assertFalse(tileMap.isSolid(0, 13))
		assertFalse(tileMap.isSolid(0, 100))
		assertFalse(tileMap.isSolid(0, -1))
		assertFalse(tileMap.isSolid(1, 0))
		assertFalse(tileMap.isSolid(5, 0))
	}

	@Test
	fun `findGroundBelow finds first solid line scanning downward`() {
		tileMap.rebuildFromLines(
			listOf(
				"",
				"",
				"    code here",
				"more code"
			)
		)
		assertEquals(2, tileMap.findGroundBelow(0, 5, 6, 3))
	}

	@Test
	fun `findGroundBelow skips lines where cat doesn't overlap`() {
		tileMap.rebuildFromLines(
			listOf(
				"",
				"ab",
				"                  xy",
				"          code"
			)
		)
		assertEquals(3, tileMap.findGroundBelow(0, 10, 11, 3))
	}

	@Test
	fun `findGroundBelow respects search bounds`() {
		tileMap.rebuildFromLines(listOf("code", "", "code"))
		assertEquals(2, tileMap.findGroundBelow(1, 0, 1, 2))

		tileMap.rebuildFromLines(listOf("", "", ""))
		assertNull(tileMap.findGroundBelow(0, 0, 1, 2))

		tileMap.rebuildFromLines(listOf("", "", "code"))
		assertNull(tileMap.findGroundBelow(0, 0, 1, 1))
	}

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

	@Test
	fun `realistic code file - platform detection`() {
		tileMap.rebuildFromLines(
			listOf(
				"package dev.example",
				"",
				"class Foo {",
				"    fun bar() {",
				"        val x = 1",
				"    }",
				"}"
			)
		)

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

	@Test
	fun `rebuildFromDocument respects visual columns for tab-indented code`() {
		tileMap.rebuildFromDocument(
			lineCount = 1,
			lineText = { "\tfoo" },
			spanMapper = { _, col ->
				when (col) {
					0 -> VisualSpan(0, 4)
					1 -> VisualSpan(4, 5)
					2 -> VisualSpan(5, 6)
					3 -> VisualSpan(6, 7)
					else -> error("unexpected column $col")
				}
			}
		)

		assertFalse(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 3))
		assertTrue(tileMap.isSolid(0, 4))
		assertTrue(tileMap.isSolid(0, 6))
		assertEquals(4..6, tileMap.getExtent(0))
		assertTrue(tileMap.hasGroundAt(0, 4, 5))
	}

	@Test
	fun `rebuildFromDocument handles mixed spaces and tabs`() {
		tileMap.rebuildFromDocument(
			lineCount = 1,
			lineText = { " \t x" },
			spanMapper = { _, col ->
				when (col) {
					0 -> VisualSpan(0, 1)
					1 -> VisualSpan(1, 5)
					2 -> VisualSpan(5, 6)
					3 -> VisualSpan(6, 7)
					else -> error("unexpected column $col")
				}
			}
		)

		assertFalse(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 4))
		assertFalse(tileMap.isSolid(0, 5))
		assertTrue(tileMap.isSolid(0, 6))
		assertEquals(6..6, tileMap.getExtent(0))
	}

	@Test
	fun `rebuildFromDocument supports tabs with non four-space visual width`() {
		tileMap.rebuildFromDocument(
			lineCount = 1,
			lineText = { "\tfoo" },
			spanMapper = { _, col ->
				when (col) {
					0 -> VisualSpan(0, 8)
					1 -> VisualSpan(8, 9)
					2 -> VisualSpan(9, 10)
					3 -> VisualSpan(10, 11)
					else -> error("unexpected column $col")
				}
			}
		)

		assertFalse(tileMap.isSolid(0, 7))
		assertTrue(tileMap.isSolid(0, 8))
		assertTrue(tileMap.isSolid(0, 10))
		assertEquals(8..10, tileMap.getExtent(0))
	}

	@Test
	fun `rebuildFromDocument fills every visual cell occupied by widened character spans`() {
		tileMap.rebuildFromDocument(
			lineCount = 1,
			lineText = { "ab" },
			spanMapper = { _, col ->
				when (col) {
					0 -> VisualSpan(0, 1)
					1 -> VisualSpan(4, 7)
					else -> error("unexpected column $col")
				}
			}
		)

		assertTrue(tileMap.isSolid(0, 0))
		assertFalse(tileMap.isSolid(0, 1))
		assertFalse(tileMap.isSolid(0, 3))
		assertTrue(tileMap.isSolid(0, 4))
		assertTrue(tileMap.isSolid(0, 5))
		assertTrue(tileMap.isSolid(0, 6))
		assertFalse(tileMap.isSolid(0, 7))
		assertEquals(0..6, tileMap.getExtent(0))
	}

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
