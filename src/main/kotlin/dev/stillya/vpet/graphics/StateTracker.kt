package dev.stillya.vpet.graphics

import com.intellij.openapi.diagnostic.Logger

class StateTracker(
	private val config: HysteresisConfig = HysteresisConfig.DEFAULT
) {
	private val log = Logger.getInstance(StateTracker::class.java)
	private var currentState: RuntimeState = RuntimeState.DEFAULT
	private var inWalkBand = false
	private var inRunBand = false

	fun getCurrentState(): RuntimeState = currentState

	fun updateSpeed(rawSpeed: Float) {
		val newSpeed = applyHysteresis(rawSpeed)
		if (newSpeed != currentState.kinematics.speed) {
			log.trace("Speed updated: ${currentState.kinematics.speed} -> $newSpeed (raw: $rawSpeed)")
			currentState = currentState.withSpeed(newSpeed)
		}
	}

	fun updatePose(newPose: Pose) {
		if (newPose != currentState.pose) {
			log.trace("Pose updated: ${currentState.pose} -> $newPose")
			currentState = currentState.withPose(newPose)
		}
	}

	fun updateDirection(newDirection: Direction) {
		if (newDirection.angle != currentState.kinematics.direction.angle) {
			log.trace("Direction updated: ${currentState.kinematics.direction.angle}° -> ${newDirection.angle}°")
			currentState = currentState.withDirection(newDirection)
		}
	}

	fun updateFlags(newFlags: Set<StateFlag>) {
		if (newFlags != currentState.flags) {
			log.trace("Flags updated: ${currentState.flags} -> $newFlags")
			currentState = currentState.withFlags(newFlags)
		}
	}

	fun applyEffect(effect: StateEffect) {
		log.trace("Applying effect: $effect")
		effect.pose?.let { updatePose(it) }
		effect.speedDelta?.let { delta ->
			val newSpeed = (currentState.kinematics.speed + delta).coerceAtLeast(0f)
			updateSpeed(newSpeed)
		}
		effect.speed?.let { updateSpeed(it) }
		effect.direction?.let { updateDirection(it) }
		effect.flagsToAdd.forEach { currentState = currentState.addFlag(it) }
		effect.flagsToRemove.forEach { currentState = currentState.removeFlag(it) }
	}

	fun reset(newState: RuntimeState = RuntimeState.DEFAULT) {
		log.trace("Resetting state to: $newState")
		currentState = newState
		inWalkBand = false
		inRunBand = false
	}

	private fun applyHysteresis(rawSpeed: Float): Float {
		val wasWalking = inWalkBand
		val wasRunning = inRunBand

		when {
			rawSpeed >= config.runOnThreshold -> {
				inRunBand = true
				inWalkBand = true
			}
			rawSpeed <= config.runOffThreshold && rawSpeed >= config.walkOnThreshold -> {
				inRunBand = false
				inWalkBand = true
			}
			rawSpeed <= config.walkOffThreshold -> {
				inRunBand = false
				inWalkBand = false
			}
		}

		return when {
			inRunBand -> rawSpeed.coerceAtLeast(config.runOnThreshold)
			inWalkBand -> rawSpeed.coerceIn(config.walkOnThreshold, config.runOffThreshold)
			else -> 0f
		}
	}
}

data class HysteresisConfig(
	val walkOnThreshold: Float,
	val walkOffThreshold: Float,
	val runOnThreshold: Float,
	val runOffThreshold: Float
) {
	companion object {
		val DEFAULT = HysteresisConfig(
			walkOnThreshold = 0.3f,
			walkOffThreshold = 0.2f,
			runOnThreshold = 0.7f,
			runOffThreshold = 0.6f
		)
	}
}
