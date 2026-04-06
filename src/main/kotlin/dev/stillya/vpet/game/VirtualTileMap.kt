package dev.stillya.vpet.game

object Tile {
	const val AIR: Byte = 0x00
	const val SOLID: Byte = 0x01
}

data class VisualSpan(val startCol: Int, val endColExclusive: Int) {
	init {
		require(startCol >= 0) { "startCol must be non-negative" }
		require(endColExclusive > startCol) { "endColExclusive must be greater than startCol" }
	}
}

class VirtualTileMap {

	private val rows = HashMap<Int, ByteArray>()
	private val extentCache = HashMap<Int, IntRange?>()

	fun isSolid(line: Int, col: Int): Boolean {
		val row = rows[line] ?: return false
		if (col < 0 || col >= row.size) return false
		return row[col] == Tile.SOLID
	}

	fun hasGroundAt(line: Int, catLeft: Int, catRight: Int): Boolean {
		val extent = getExtent(line) ?: return false
		return catLeft <= extent.last && catRight >= extent.first
	}

	fun hasCeilingAt(line: Int, catLeft: Int, catRight: Int): Boolean =
		hasGroundAt(line, catLeft, catRight)

	fun findGroundBelow(startLine: Int, catLeft: Int, catRight: Int, maxLine: Int): Int? {
		for (line in startLine..maxLine) {
			if (hasGroundAt(line, catLeft, catRight)) return line
		}
		return null
	}

	fun getExtent(line: Int): IntRange? = extentCache.getOrPut(line) { computeExtent(line) }

	fun forEachExtent(action: (line: Int, extent: IntRange) -> Unit) {
		for (line in rows.keys) {
			val extent = getExtent(line)
			if (extent != null) action(line, extent)
		}
	}

	fun rebuildFromLines(lines: List<String>) {
		rows.clear()
		extentCache.clear()
		lines.forEachIndexed { idx, text ->
			if (text.isNotEmpty()) {
				val cells = ByteArray(text.length)
				for (i in text.indices) {
					cells[i] = if (!text[i].isWhitespace()) Tile.SOLID else Tile.AIR
				}
				rows[idx] = cells
			}
		}
	}

	fun rebuildFromDocument(
		lineCount: Int,
		lineText: (Int) -> String,
		spanMapper: (Int, Int) -> VisualSpan
	) {
		rows.clear()
		extentCache.clear()
		for (line in 0 until lineCount) {
			val text = lineText(line)
			if (text.isEmpty()) continue
			val spans = text.indices.map { spanMapper(line, it) }
			val lineWidth = spans.maxOfOrNull { it.endColExclusive } ?: continue
			val cells = ByteArray(lineWidth)
			for (i in text.indices) {
				if (!text[i].isWhitespace()) {
					val span = spans[i]
					for (col in span.startCol until span.endColExclusive) {
						cells[col] = Tile.SOLID
					}
				}
			}
			rows[line] = cells
		}
	}

	private fun computeExtent(line: Int): IntRange? {
		val row = rows[line] ?: return null
		var first = -1
		var last = -1
		for (i in row.indices) {
			if (row[i] == Tile.SOLID) {
				if (first == -1) first = i
				last = i
			}
		}
		return if (first == -1) null else first..last
	}
}
