package dev.stillya.vpet.game

import com.intellij.openapi.editor.Editor
import kotlin.math.abs
import kotlin.math.floor

class GamePhysics(internal val tileMap: GameTileMap = GameTileMap()) {

	var colX: Float = 0f
	var lineY: Float = 0f
	var velocityX: Float = 0f
	var velocityY: Float = 0f
	var isOnGround: Boolean = true
	var facingLeft: Boolean = false
	var inputDirection: Int = 0

	val displayLine: Int
		get() = floor(lineY).toInt()

	companion object {
		const val CAT_WIDTH = 2
		const val GRAVITY = 0.02f
		const val JUMP_VELOCITY = -0.25f
		const val WALK_SPEED = 0.15f
		const val GROUND_FRICTION = 0.75f
	}

	fun update(editor: Editor, firstVisibleLine: Int, lastVisibleLine: Int) {
		tileMap.rebuild(editor, firstVisibleLine, lastVisibleLine)
		step(firstVisibleLine, lastVisibleLine)
	}

	internal fun step(firstVisibleLine: Int, lastVisibleLine: Int) {
		if (inputDirection != 0) {
			velocityX = inputDirection * WALK_SPEED
			facingLeft = inputDirection < 0
		} else {
			velocityX *= GROUND_FRICTION
			if (abs(velocityX) < 0.01f) velocityX = 0f
		}

		val prevColX = colX
		val prevCatLeft = floor(prevColX).toInt()
		val prevCatRight = prevCatLeft + CAT_WIDTH - 1

		colX += velocityX

		val catLeft = floor(colX).toInt()
		val catRight = catLeft + CAT_WIDTH - 1
		val bodyLine = displayLine - 1

		val bodyExtent = tileMap.getExtent(bodyLine)
		if (bodyExtent != null) {
			val prevOverlaps = prevCatLeft <= bodyExtent.last && prevCatRight >= bodyExtent.first
			val nowOverlaps = catLeft <= bodyExtent.last && catRight >= bodyExtent.first
			if (nowOverlaps && !prevOverlaps) {
				colX = prevColX
				velocityX = 0f
			}
		}

		if (!isOnGround) {
			velocityY += GRAVITY
			lineY += velocityY

			val curCatLeft = floor(colX).toInt()
			val curCatRight = curCatLeft + CAT_WIDTH - 1

			if (velocityY > 0) {
				val scanStart = (floor(lineY - velocityY).toInt() + 1).coerceAtLeast(0)
				val landLine = tileMap.findGroundBelow(scanStart, curCatLeft, curCatRight, lastVisibleLine)
				if (landLine != null && lineY >= landLine.toFloat()) {
					lineY = landLine.toFloat()
					velocityY = 0f
					isOnGround = true
				}
			} else if (velocityY < 0) {
				val ceilingLine = displayLine - 2
				if (tileMap.hasCeilingAt(ceilingLine, curCatLeft, curCatRight)) {
					velocityY = 0f
				}
			}
		} else {
			val curCatLeft = floor(colX).toInt()
			val curCatRight = curCatLeft + CAT_WIDTH - 1
			if (!tileMap.hasGroundAt(displayLine, curCatLeft, curCatRight)) {
				val nextLine = displayLine + 1
				if (nextLine <= lastVisibleLine && tileMap.hasGroundAt(nextLine, curCatLeft, curCatRight)) {
					lineY = nextLine.toFloat()
				} else {
					isOnGround = false
				}
			}
		}

		val clampedLine = lineY.coerceIn(firstVisibleLine.toFloat(), lastVisibleLine.toFloat())
		if (clampedLine != lineY) {
			lineY = clampedLine
			velocityY = 0f
			isOnGround = true
		}

		colX = colX.coerceAtLeast(0f)
	}

	fun jump() {
		velocityY = JUMP_VELOCITY
		isOnGround = false
	}
}
