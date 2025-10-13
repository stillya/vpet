package dev.stillya.vpet.graphics

data class Animation(
	val name: String,
	val sheet: SpriteSheet,
	val loop: Int = 0,
	val onFinish: () -> Unit = {},
	var nextAnimation: Animation? = null,
)

// TODO: Animation sequencing: Add animations to sequence instead of using onFinish callbacks.
//  Infinite animations must be only the last one in a sequence

fun Animation.onNext(animation: Animation): Animation {
	this.nextAnimation = animation
	return animation
}

const val INFINITE = -1