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
		isOnGround: Boolean,
		tileMap: VirtualTileMap,
		visibleRange: IntRange,
		dt: Float
	): PhysicsResult {
		val displacement = sqrt(velocity.x * velocity.x + velocity.y * velocity.y) * dt
		val steps = max(1, ceil(displacement / MAX_STEP_DISPLACEMENT).toInt())

		if (steps == 1) {
			return singleStep(transform, velocity, isOnGround, tileMap, visibleRange, dt)
		}

		val subDt = dt / steps
		var result = PhysicsResult(transform, velocity, isOnGround)
		for (i in 0 until steps) {
			result = singleStep(result.transform, result.velocity, result.isOnGround, tileMap, visibleRange, subDt)
		}
		return result
	}

	fun isOnFloor(transform: Transform, tileMap: VirtualTileMap): Boolean {
		val displayLine = floor(transform.y).toInt()
		val catLeft = floor(transform.x).toInt()
		val catRight = catLeft + collider.width - 1
		return tileMap.hasGroundAt(displayLine, catLeft, catRight)
	}

	fun boundsAt(transform: Transform): IntRange {
		val left = floor(transform.x).toInt()
		return left..(left + collider.width - 1)
	}

	private fun singleStep(
		transform: Transform,
		velocity: Velocity,
		isOnGround: Boolean,
		tileMap: VirtualTileMap,
		visibleRange: IntRange,
		dt: Float
	): PhysicsResult {
		var t = transform
		var v = velocity
		var grounded = isOnGround

		val xResult = moveAndResolveX(t, v, dt, tileMap)
		t = xResult.first
		v = xResult.second

		val bodyLineBeforeY = floor(t.y).toInt() - 1
		val yResult = moveAndResolveY(t, v, grounded, tileMap, visibleRange.last, dt)
		t = yResult.transform
		v = yResult.velocity
		grounded = yResult.isOnGround

		val pushResult1 = resolveNewBodyLine(t, v, bodyLineBeforeY, tileMap)
		t = pushResult1.first
		v = pushResult1.second

		val bodyLineBeforeGround = floor(t.y).toInt() - 1
		val groundResult = groundMaintenance(t, v, grounded, tileMap, visibleRange.last)
		t = groundResult.transform
		v = groundResult.velocity
		grounded = groundResult.isOnGround

		val pushResult2 = resolveNewBodyLine(t, v, bodyLineBeforeGround, tileMap)
		t = pushResult2.first
		v = pushResult2.second

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
		isOnGround: Boolean,
		tileMap: VirtualTileMap,
		lastVisibleLine: Int,
		dt: Float
	): PhysicsResult {
		if (isOnGround) return PhysicsResult(transform, velocity, true)

		var vy = velocity.y + Physics.GRAVITY * dt
		vy = vy.coerceAtMost(Physics.MAX_FALL_SPEED)

		val oldLineY = transform.y
		val newLineY = oldLineY + vy * dt

		val catLeft = floor(transform.x).toInt()
		val catRight = catLeft + collider.width - 1

		if (vy > 0) {
			val scanStart = (floor(oldLineY).toInt() + 1).coerceAtLeast(0)
			val landLine = tileMap.findGroundBelow(scanStart, catLeft, catRight, lastVisibleLine)

			if (landLine != null && newLineY >= landLine.toFloat()) {
				return PhysicsResult(
					Transform(transform.x, landLine.toFloat()),
					Velocity(velocity.x, 0f),
					true
				)
			}
		} else if (vy < 0) {
			val ceilingLine = floor(newLineY).toInt() - 1
			if (tileMap.hasCeilingAt(ceilingLine, catLeft, catRight)) {
				return PhysicsResult(transform, Velocity(velocity.x, 0f), false)
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

		// Check per-cell: is any solid cell overlapping the cat's horizontal range?
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

	private fun groundMaintenance(
		transform: Transform,
		velocity: Velocity,
		isOnGround: Boolean,
		tileMap: VirtualTileMap,
		lastVisibleLine: Int
	): PhysicsResult {
		if (!isOnGround) return PhysicsResult(transform, velocity, false)

		val displayLine = floor(transform.y).toInt()
		val catLeft = floor(transform.x).toInt()
		val catRight = catLeft + collider.width - 1

		if (!tileMap.hasGroundAt(displayLine, catLeft, catRight)) {
			val nextLine = displayLine + 1
			if (nextLine <= lastVisibleLine && tileMap.hasGroundAt(nextLine, catLeft, catRight)) {
				return PhysicsResult(
					Transform(transform.x, nextLine.toFloat()),
					velocity,
					true
				)
			}
			return PhysicsResult(transform, velocity, false)
		}

		return PhysicsResult(transform, velocity, true)
	}

	private fun clampToVisibleArea(
		transform: Transform,
		velocity: Velocity,
		isOnGround: Boolean,
		visibleRange: IntRange
	): PhysicsResult {
		val clampedY = transform.y.coerceIn(visibleRange.first.toFloat(), visibleRange.last.toFloat())
		if (clampedY != transform.y) {
			return PhysicsResult(
				Transform(transform.x, clampedY),
				Velocity(velocity.x, 0f),
				true
			)
		}
		return PhysicsResult(transform, velocity, isOnGround)
	}

	companion object {
		private const val MAX_STEP_DISPLACEMENT = 0.5f
	}
}
