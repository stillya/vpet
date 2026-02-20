package dev.stillya.vpet.game

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor

object GameUpdate {

	fun update(
		state: GameState,
		input: InputState,
		dt: Float,
		tileMap: GameTileMap,
		firstVisibleLine: Int,
		lastVisibleLine: Int
	): GameState {
		var s = applyHorizontalInput(state, input, dt)
		s = moveAndResolveX(s, dt, tileMap)
		val bodyLineBeforeY = s.displayLine - 1
		s = moveAndResolveY(s, dt, tileMap, lastVisibleLine)
		s = resolveNewBodyLine(s, bodyLineBeforeY, tileMap)
		val bodyLineBeforeGround = s.displayLine - 1
		s = groundMaintenance(s, tileMap, lastVisibleLine)
		s = resolveNewBodyLine(s, bodyLineBeforeGround, tileMap)
		s = clampToVisibleArea(s, firstVisibleLine, lastVisibleLine)
		s = updateAnimation(s, input, dt)
		return s
	}

	private fun applyHorizontalInput(state: GameState, input: InputState, dt: Float): GameState {
		var vx = state.velocityX
		var facingLeft = state.facingLeft

		if (input.moveDirection != 0) {
			facingLeft = input.moveDirection < 0
		}

		if (state.isOnGround) {
			vx = if (input.moveDirection != 0) {
				input.moveDirection * Physics.WALK_SPEED
			} else {
				val damped = vx * exp(-Physics.GROUND_DAMPING * dt)
				if (abs(damped) < Physics.VELOCITY_EPSILON) 0f else damped
			}
		} else {
			if (input.moveDirection != 0) {
				vx += input.moveDirection * Physics.WALK_SPEED * Physics.AIR_CONTROL * dt
			} else {
				val damped = vx * exp(-Physics.AIR_DAMPING * dt)
				if (abs(damped) < Physics.VELOCITY_EPSILON) 0f else damped
			}
		}

		var s = state
		if (input.jumpJustPressed && state.isOnGround) {
			s = s.copy(
				velocityY = Physics.JUMP_VELOCITY,
				isOnGround = false
			)
		}

		return s.copy(velocityX = vx, facingLeft = facingLeft)
	}

	private fun moveAndResolveX(state: GameState, dt: Float, tileMap: GameTileMap): GameState {
		val newColX = state.colX + state.velocityX * dt

		val prevCatLeft = state.catLeft
		val prevCatRight = state.catRight
		val newCatLeft = floor(newColX).toInt()
		val newCatRight = newCatLeft + Physics.CAT_WIDTH - 1

		val bodyLine = state.displayLine - 1
		val bodyExtent = tileMap.getExtent(bodyLine)

		if (bodyExtent != null) {
			val prevOverlaps = prevCatLeft <= bodyExtent.last && prevCatRight >= bodyExtent.first
			val nowOverlaps = newCatLeft <= bodyExtent.last && newCatRight >= bodyExtent.first

			if (nowOverlaps && !prevOverlaps) {
				val resolvedColX = if (state.velocityX > 0) {
					(bodyExtent.first - Physics.CAT_WIDTH).toFloat()
				} else {
					(bodyExtent.last + 1).toFloat()
				}
				return state.copy(colX = resolvedColX.coerceAtLeast(0f), velocityX = 0f)
			}
		}

		return state.copy(colX = newColX.coerceAtLeast(0f))
	}

	private fun moveAndResolveY(
		state: GameState,
		dt: Float,
		tileMap: GameTileMap,
		lastVisibleLine: Int
	): GameState {
		if (state.isOnGround) return state

		var vy = state.velocityY + Physics.GRAVITY * dt
		vy = vy.coerceAtMost(Physics.MAX_FALL_SPEED)

		val oldLineY = state.lineY
		val newLineY = oldLineY + vy * dt

		val catLeft = floor(state.colX).toInt()
		val catRight = catLeft + Physics.CAT_WIDTH - 1

		if (vy > 0) {
			val scanStart = (floor(oldLineY).toInt() + 1).coerceAtLeast(0)
			val landLine = tileMap.findGroundBelow(scanStart, catLeft, catRight, lastVisibleLine)

			if (landLine != null && newLineY >= landLine.toFloat()) {
				return state.copy(
					lineY = landLine.toFloat(),
					velocityY = 0f,
					isOnGround = true
				)
			}
		} else if (vy < 0) {
			val ceilingLine = floor(newLineY).toInt() - 1
			if (tileMap.hasCeilingAt(ceilingLine, catLeft, catRight)) {
				return state.copy(
					lineY = oldLineY,
					velocityY = 0f
				)
			}
		}

		return state.copy(lineY = newLineY, velocityY = vy)
	}

	private fun resolveNewBodyLine(state: GameState, prevBodyLine: Int, tileMap: GameTileMap): GameState {
		val newBodyLine = state.displayLine - 1
		if (newBodyLine == prevBodyLine) return state

		val bodyExtent = tileMap.getExtent(newBodyLine) ?: return state

		val catLeft = state.catLeft
		val catRight = state.catRight

		if (catLeft > bodyExtent.last || catRight < bodyExtent.first) return state

		val pushLeft = (bodyExtent.first - Physics.CAT_WIDTH).toFloat()
		val pushRight = (bodyExtent.last + 1).toFloat()

		val leftDist = abs(state.colX - pushLeft)
		val rightDist = abs(pushRight - state.colX)

		val resolvedColX = if (leftDist <= rightDist) pushLeft else pushRight
		return state.copy(colX = resolvedColX.coerceAtLeast(0f), velocityX = 0f)
	}

	private fun groundMaintenance(
		state: GameState,
		tileMap: GameTileMap,
		lastVisibleLine: Int
	): GameState {
		if (!state.isOnGround) return state

		val catLeft = state.catLeft
		val catRight = state.catRight

		if (!tileMap.hasGroundAt(state.displayLine, catLeft, catRight)) {
			val nextLine = state.displayLine + 1
			if (nextLine <= lastVisibleLine && tileMap.hasGroundAt(nextLine, catLeft, catRight)) {
				return state.copy(lineY = nextLine.toFloat())
			}
			return state.copy(isOnGround = false)
		}

		return state
	}

	private fun clampToVisibleArea(
		state: GameState,
		firstVisibleLine: Int,
		lastVisibleLine: Int
	): GameState {
		val clampedLine = state.lineY.coerceIn(firstVisibleLine.toFloat(), lastVisibleLine.toFloat())
		if (clampedLine != state.lineY) {
			return state.copy(
				lineY = clampedLine,
				velocityY = 0f,
				isOnGround = true
			)
		}
		return state
	}

	private fun updateAnimation(state: GameState, input: InputState, dt: Float): GameState {
		val newAnim = when {
			!state.isOnGround && state.velocityY < 0 -> GameAnimationState.JUMP_UP
			!state.isOnGround && state.velocityY >= 0 -> GameAnimationState.JUMP_DOWN
			state.animState == GameAnimationState.JUMP_DOWN && state.isOnGround -> GameAnimationState.LAND
			abs(state.velocityX) > Physics.VELOCITY_EPSILON -> GameAnimationState.WALK
			else -> GameAnimationState.IDLE
		}

		val animChanged = newAnim != state.animState
		val frameIndex = if (animChanged) 0 else state.frameIndex
		val frameTimer = if (animChanged) 0f else state.frameTimer

		val newTimer = frameTimer + dt
		return if (newTimer >= Physics.FRAME_ADVANCE_INTERVAL) {
			state.copy(
				animState = newAnim,
				frameIndex = frameIndex + 1,
				frameTimer = newTimer - Physics.FRAME_ADVANCE_INTERVAL
			)
		} else {
			state.copy(
				animState = newAnim,
				frameIndex = frameIndex,
				frameTimer = newTimer
			)
		}
	}
}
