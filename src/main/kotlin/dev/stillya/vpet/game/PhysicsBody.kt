package dev.stillya.vpet.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

data class AABB(val width: Int, val height: Int = 2)

data class PhysicsResult(
	val transform: Transform,
	val velocity: Velocity,
	val isOnGround: Boolean
)

class PhysicsBody(val collider: AABB) {

	fun moveAndSlide(
		transform: Transform,
		velocity: Velocity,
		tileMap: VirtualTileMap,
		visibleRange: IntRange,
		dt: Float
	): PhysicsResult {
		val displacement = sqrt(velocity.x * velocity.x + velocity.y * velocity.y) * dt
		val steps = max(1, ceil(displacement / MAX_STEP_DISPLACEMENT).toInt())

		if (steps == 1) {
			return singleStep(transform, velocity, tileMap, visibleRange, dt)
		}

		val subDt = dt / steps
		var result = PhysicsResult(transform, velocity, false)
		for (i in 0 until steps) {
			result = singleStep(result.transform, result.velocity, tileMap, visibleRange, subDt)
		}
		return result
	}

	fun boundsAt(transform: Transform): IntRange {
		val left = floor(transform.x).toInt()
		return left..<left + collider.width
	}

	private fun singleStep(
		transform: Transform,
		velocity: Velocity,
		tileMap: VirtualTileMap,
		visibleRange: IntRange,
		dt: Float
	): PhysicsResult {
		var t = transform
		var v = velocity

		val xResult = moveAndResolveX(t, v, dt, tileMap)
		t = xResult.first
		v = xResult.second

		val bodyLineBeforeY = floor(t.y).toInt() - 1
		val yResult = moveAndResolveY(t, v, tileMap, dt)
		t = yResult.transform
		v = yResult.velocity
		val grounded = yResult.isOnGround

		val pushResult = resolveNewBodyLine(t, v, bodyLineBeforeY, tileMap)
		t = pushResult.first
		v = pushResult.second

		return clampToVisibleArea(t, v, grounded, visibleRange)
	}

	private fun moveAndResolveX(
		transform: Transform,
		velocity: Velocity,
		dt: Float,
		tileMap: VirtualTileMap
	): Pair<Transform, Velocity> {
		val newX = transform.x + velocity.x * dt

		val prevCatLeft = floor(transform.x).toInt()
		val prevCatRight = prevCatLeft + collider.width - 1
		val newCatLeft = floor(newX).toInt()
		val newCatRight = newCatLeft + collider.width - 1

		val bodyLine = floor(transform.y).toInt() - 1

		// Skip wall collision if cat already overlaps solid cells on body line
		val alreadyOverlaps = (prevCatLeft..prevCatRight).any { tileMap.isSolid(bodyLine, it) }
		if (alreadyOverlaps) {
			return Transform(newX.coerceAtLeast(0f), transform.y) to velocity
		}

		if (velocity.x > 0) {
			for (col in (prevCatRight + 1)..newCatRight) {
				if (tileMap.isSolid(bodyLine, col)) {
					val resolvedX = (col - collider.width).toFloat()
					return Transform(resolvedX.coerceAtLeast(0f), transform.y) to Velocity(0f, velocity.y)
				}
			}
		} else if (velocity.x < 0) {
			for (col in (prevCatLeft - 1) downTo newCatLeft) {
				if (tileMap.isSolid(bodyLine, col)) {
					val resolvedX = (col + 1).toFloat()
					return Transform(resolvedX.coerceAtLeast(0f), transform.y) to Velocity(0f, velocity.y)
				}
			}
		}

		return Transform(newX.coerceAtLeast(0f), transform.y) to velocity
	}

	private fun moveAndResolveY(
		transform: Transform,
		velocity: Velocity,
		tileMap: VirtualTileMap,
		dt: Float
	): PhysicsResult {
		var vy = velocity.y + Physics.GRAVITY * dt
		vy = vy.coerceAtMost(Physics.MAX_FALL_SPEED)

		val newLineY = transform.y + vy * dt

		val catLeft = floor(transform.x).toInt()
		val catRight = catLeft + collider.width - 1

		if (vy >= 0) {
			val sweepStart = ceil(transform.y - SWEEP_EPSILON).toInt()
			val sweepEnd = floor(newLineY).toInt()

			if (sweepStart <= sweepEnd) {
				val landLine = tileMap.findGroundBelow(sweepStart, catLeft, catRight, sweepEnd)
				if (landLine != null) {
					return PhysicsResult(
						Transform(transform.x, landLine.toFloat()),
						Velocity(velocity.x, 0f),
						true
					)
				}
			}
		} else {
			val oldBodyLine = floor(transform.y).toInt() - 1
			val newBodyLine = floor(newLineY).toInt() - 1

			for (line in (oldBodyLine - 1) downTo newBodyLine) {
				if (tileMap.hasCeilingAt(line, catLeft, catRight)) {
					val resolvedY = (line + collider.height).toFloat()
					return PhysicsResult(
						Transform(transform.x, resolvedY),
						Velocity(velocity.x, 0f),
						false
					)
				}
			}
		}

		return PhysicsResult(
			Transform(transform.x, newLineY),
			Velocity(velocity.x, vy),
			false
		)
	}

	private fun resolveNewBodyLine(
		transform: Transform,
		velocity: Velocity,
		prevBodyLine: Int,
		tileMap: VirtualTileMap
	): Pair<Transform, Velocity> {
		val newBodyLine = floor(transform.y).toInt() - 1
		if (newBodyLine == prevBodyLine) return transform to velocity

		val catLeft = floor(transform.x).toInt()
		val catRight = catLeft + collider.width - 1

		var solidLeft = Int.MAX_VALUE
		var solidRight = Int.MIN_VALUE
		for (col in catLeft..catRight) {
			if (tileMap.isSolid(newBodyLine, col)) {
				if (col < solidLeft) solidLeft = col
				if (col > solidRight) solidRight = col
			}
		}
		if (solidLeft > solidRight) return transform to velocity

		val pushLeft = (solidLeft - collider.width).toFloat()
		val pushRight = (solidRight + 1).toFloat()

		val leftDist = abs(transform.x - pushLeft)
		val rightDist = abs(pushRight - transform.x)

		val resolvedX = if (leftDist <= rightDist) pushLeft else pushRight
		return Transform(resolvedX.coerceAtLeast(0f), transform.y) to Velocity(0f, velocity.y)
	}

	private fun clampToVisibleArea(
		transform: Transform,
		velocity: Velocity,
		isOnGround: Boolean,
		visibleRange: IntRange
	): PhysicsResult {
		val clampedY = transform.y.coerceIn(visibleRange.first.toFloat(), visibleRange.last.toFloat())
		if (clampedY != transform.y) {
			val hitBottom = transform.y > visibleRange.last.toFloat()
			return PhysicsResult(
				Transform(transform.x, clampedY),
				Velocity(velocity.x, 0f),
				hitBottom
			)
		}
		return PhysicsResult(transform, velocity, isOnGround)
	}

	companion object {
		private const val MAX_STEP_DISPLACEMENT = 0.5f
		private const val SWEEP_EPSILON = 0.01f
	}
}
