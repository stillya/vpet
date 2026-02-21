package dev.stillya.vpet.game

import com.intellij.openapi.editor.Editor

class GameTileMap {

	private val extents = HashMap<Int, IntRange?>()
	private val lineTexts = HashMap<Int, String>()

	fun rebuild(editor: Editor, firstLine: Int, lastLine: Int) {
		extents.clear()
		lineTexts.clear()
		val doc = editor.document
		val chars = doc.charsSequence
		val maxLine = (lastLine + 2).coerceAtMost(doc.lineCount - 1)
		for (line in (firstLine - 2).coerceAtLeast(0)..maxLine) {
			val start = doc.getLineStartOffset(line)
			val end = doc.getLineEndOffset(line)
			val text = chars.subSequence(start, end).toString()
			lineTexts[line] = text
			extents[line] = computeExtent(text)
		}
	}

	fun rebuildFromLines(lines: List<String>) {
		extents.clear()
		lineTexts.clear()
		lines.forEachIndexed { idx, text ->
			lineTexts[idx] = text
			extents[idx] = computeExtent(text)
		}
	}

	fun isCharSolid(line: Int, col: Int): Boolean {
		val text = lineTexts[line] ?: return false
		if (col < 0 || col >= text.length) return false
		return !text[col].isWhitespace()
	}

	fun hasGroundAt(line: Int, catLeft: Int, catRight: Int): Boolean {
		val extent = extents[line] ?: return false
		return catLeft <= extent.last && catRight >= extent.first
	}

	fun hasCeilingAt(line: Int, catLeft: Int, catRight: Int): Boolean = hasGroundAt(line, catLeft, catRight)

	fun findGroundBelow(startLine: Int, catLeft: Int, catRight: Int, maxLine: Int): Int? {
		for (line in startLine..maxLine) {
			if (hasGroundAt(line, catLeft, catRight)) return line
		}
		return null
	}

	fun getExtent(line: Int): IntRange? = extents[line]

	fun forEachExtent(action: (line: Int, extent: IntRange) -> Unit) {
		for ((line, extent) in extents) {
			if (extent != null) action(line, extent)
		}
	}

	private fun computeExtent(text: String): IntRange? {
		var first = -1
		var last = -1
		for (i in text.indices) {
			if (!text[i].isWhitespace()) {
				if (first == -1) first = i
				last = i
			}
		}
		return if (first == -1) null else first..last
	}
}
