package dev.stillya.vpet.config

import com.fasterxml.jackson.annotation.JsonProperty

data class SpriteSheetAtlas(
	@JsonProperty("frames")
	val frames: List<AtlasFrame>,
	@JsonProperty("meta")
	val meta: AtlasMeta
)

data class AtlasFrame(
	@JsonProperty("frame")
	val frame: FrameRect,
)

data class FrameRect(
	@JsonProperty("x")
	val x: Int = 0,

	@JsonProperty("y")
	val y: Int = 0,

	@JsonProperty("w")
	val width: Int = 0,

	@JsonProperty("h")
	val height: Int = 0
)

data class AtlasMeta(
	@JsonProperty("frameTags")
	val frameTags: List<FrameTag>
)

data class FrameTag(
	@JsonProperty("name")
	val name: String = "",

	@JsonProperty("from")
	val from: Int = 0,

	@JsonProperty("to")
	val to: Int = 0,

	@JsonProperty("duration")
	val duration: Int = 100,
)