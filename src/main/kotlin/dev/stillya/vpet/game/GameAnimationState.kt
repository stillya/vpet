package dev.stillya.vpet.game

enum class GameAnimationState(val spriteTag: String, val loops: Boolean) {
	IDLE("Idle", true),
	WALK("Walk", true),
	JUMP_UP("J_1", false),
	JUMP_DOWN("J_U_D", false),
	LAND("Stop", false)
}
