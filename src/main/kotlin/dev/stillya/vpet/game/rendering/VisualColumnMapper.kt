package dev.stillya.vpet.game.rendering

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import kotlin.math.ceil

class VisualColumnMapper(editor: Editor) {

	val leftMargin: Int = editor.logicalPositionToXY(LogicalPosition(0, 0)).x

	val charWidth: Int = editor.logicalPositionToXY(LogicalPosition(0, 1)).x - leftMargin

	fun toPixelX(col: Int): Int = leftMargin + col * charWidth

	fun toPixelX(col: Float): Int = leftMargin + (col * charWidth).toInt()

	fun toVisualCol(pixelX: Int): Int = (pixelX - leftMargin) / charWidth

	fun toVisualColF(pixelX: Int): Float = (pixelX - leftMargin).toFloat() / charWidth

	fun toVisualColCeil(pixelX: Int): Int = ceil(toVisualColF(pixelX).toDouble()).toInt()
}
