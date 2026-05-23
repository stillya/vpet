package dev.stillya.vpet.game.input

class InputTracker(private val bindings: Map<Int, GameAction> = KeyBindings.defaults) {
	private var heldActions: Set<GameAction> = emptySet()
	private var prevActions: Set<GameAction> = emptySet()

	fun press(keyCode: Int) {
		heldActions = heldActions + (bindings[keyCode] ?: return)
	}

	fun release(keyCode: Int) {
		heldActions = heldActions - (bindings[keyCode] ?: return)
	}

	fun reset() {
		heldActions = emptySet()
		prevActions = emptySet()
	}

	fun snapshot(): InputState {
		val input = InputState(
			moveDirection = when {
				GameAction.MOVE_LEFT in heldActions && GameAction.MOVE_RIGHT !in heldActions -> -1
				GameAction.MOVE_RIGHT in heldActions && GameAction.MOVE_LEFT !in heldActions -> 1
				else -> 0
			},
			jumpJustPressed = GameAction.JUMP in heldActions && GameAction.JUMP !in prevActions,
		)
		prevActions = heldActions

		return input
	}
}
