package dev.stillya.vpet

interface Animated {
	fun init(params: Params)
	fun onFail()
	fun onSuccess()
	fun onProgress()
	fun onCompleted()
	fun onOccasion()
	fun onStartObserving()
	fun onCursorMove(isOnLeftSide: Boolean)

	data class Params(
		val atlasPath: String,
		val imgPath: String,
	)
}