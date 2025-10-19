package dev.stillya.vpet.config

import com.fasterxml.jackson.annotation.JsonProperty

data class SpriteSheetAtlas(
	@field:JsonProperty("frames")
	@param:JsonProperty("frames")
	val frames: List<AtlasFrame>,
	@field:JsonProperty("meta")
	@param:JsonProperty("meta")
	val meta: AtlasMeta
)

data class AtlasFrame(
	@field:JsonProperty("frame")
	@param:JsonProperty("frame")
	val frame: FrameRect,
)

data class FrameRect(
	@field:JsonProperty("x")
	@param:JsonProperty("x")
	val x: Int = 0,

	@field:JsonProperty("y")
	@param:JsonProperty("y")
	val y: Int = 0,

	@field:JsonProperty("w")
	@param:JsonProperty("w")
	val width: Int = 0,

	@field:JsonProperty("h")
	@param:JsonProperty("h")
	val height: Int = 0
)

data class AtlasMeta(
	@field:JsonProperty("frameTags")
	@param:JsonProperty("frameTags")
	val frameTags: List<FrameTag>
)

data class FrameTag(
	@field:JsonProperty("name")
	@param:JsonProperty("name")
	val name: String = "",

	@field:JsonProperty("from")
	@param:JsonProperty("from")
	val from: Int = 0,

	@field:JsonProperty("to")
	@param:JsonProperty("to")
	val to: Int = 0,

	@field:JsonProperty("duration")
	@param:JsonProperty("duration")
	val duration: Int = 100,
)