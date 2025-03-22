package dev.stillya.vpet.graphics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.config.SpriteSheetAtlas
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.random.Random

// TODO: It's really hard to understand what's going on here. Need to refactor
@Service
class PetAnimated : Animated {
	private val atlasLoader: AtlasLoader
		get() = service<AsepriteJsonAtlasLoader>()

	private val renderer: IconRenderer
		get() = service<DefaultIconRenderer>()

	private lateinit var atlas: SpriteSheetAtlas
	private lateinit var image: Image

	companion object {
		const val INFINITE = -1
		const val SHORT_LOOP = 5
		const val MEDIUM_LOOP = 8
	}

	override fun init(params: Animated.Params) {
		atlas = atlasLoader.load(params.atlasPath)
			?: throw IllegalArgumentException("Atlas not found")
		image = loadImage(params.imgPath)

		playDefaultAnimation(withRandom = false)
	}

	override fun onFail() {
		when (Random.nextInt(2)) {
			0 -> playDeathAnimation()
			else -> playPoopingDiggingSequence()
		}
	}

	override fun onSuccess() {
		playSuccessAnimation()
	}

	override fun onProgress() {
		playProgressAnimation()
	}

	override fun onCompleted() {
		playCelebrateAnimation()
	}

	override fun onOccasion() {
		playOccasionAnimation()
	}

	private fun getRandomAnimation(variants: List<String>): String {
		return variants[Random.nextInt(variants.size)]
	}

	private fun createSpriteSheet(animationTag: String): SpriteSheet {
		val frameTag = atlas.meta.frameTags.find { it.name == animationTag }
			?: throw IllegalArgumentException("Animation tag not found: $animationTag")

		return SpriteSheet(
			image = image,
			frames = atlas.frames.subList(frameTag.from, frameTag.to + 1)
		)
	}

	private fun playDefaultAnimation(withRandom: Boolean = true) {
		if (withRandom) {
			playRandomIdleBehavior()
			return
		}

		val idleVariant = getRandomAnimation(listOf("Idle", "Sit", "Dream", "Rest"))

		renderer.enqueue(
			Animation(
				name = "default_idle",
				loop = INFINITE,
				sheet = createSpriteSheet(idleVariant),
			)
		)
	}

	private fun playRandomIdleBehavior() {
		when (Random.nextInt(5)) {
			0 -> playGroomingAnimation()
			1 -> playStretchAnimation()
			2 -> playWalkAnimation()
			3 -> playSitThenStandAnimation()
			else -> playDefaultAnimation(withRandom = false)
		}
	}

	private fun playWalkAnimation() {
		val walkVariant = getRandomAnimation(listOf("Walk"))

		renderer.enqueue(
			Animation(
				name = "walking",
				loop = SHORT_LOOP,
				sheet = createSpriteSheet(walkVariant),
				onFinish = {
					if (Random.nextFloat() < 0.3) {
						playRandomIdleBehavior()
					} else {
						playDefaultAnimation(withRandom = false)
					}
				}
			)
		)
	}

	private fun playOccasionAnimation() {
		val occasionVariant = getRandomAnimation(listOf("Pac-Cat", "Goomba"))

		renderer.enqueue(
			Animation(
				name = "occasion",
				loop = MEDIUM_LOOP,
				sheet = createSpriteSheet(occasionVariant),
				onFinish = { playDefaultAnimation() }
			)
		)
	}

	private fun playGroomingAnimation() {
		renderer.enqueue(
			Animation(
				name = "groom_start",
				sheet = createSpriteSheet("Sit_Up"),
				onFinish = {
					renderer.enqueue(
						Animation(
							name = "grooming",
							loop = SHORT_LOOP,
							sheet = createSpriteSheet("Scratching"),
							onFinish = {
								renderer.enqueue(
									Animation(
										name = "groom_end",
										sheet = createSpriteSheet("Sit_Down"),
										onFinish = { playDefaultAnimation(withRandom = false) }
									)
								)
							}
						)
					)
				}
			)
		)
	}

	private fun playStretchAnimation() {
		val stretchVariant = getRandomAnimation(listOf("On_2_Paws", "On_4_Paws"))

		renderer.enqueue(
			Animation(
				name = "stretch",
				sheet = createSpriteSheet(stretchVariant),
				onFinish = { playDefaultAnimation(withRandom = false) }
			)
		)
	}

	private fun playSitThenStandAnimation() {
		renderer.enqueue(
			Animation(
				name = "sit_up",
				sheet = createSpriteSheet("Sit_Up"),
				onFinish = {
					renderer.enqueue(
						Animation(
							name = "look_around",
							loop = SHORT_LOOP,
							sheet = createSpriteSheet("Aggress"),
							onFinish = {
								renderer.enqueue(
									Animation(
										name = "sit_down",
										sheet = createSpriteSheet("Sit_Down"),
										onFinish = { playDefaultAnimation(withRandom = false) }
									)
								)
							}
						)
					)
				}
			)
		)
	}

	private fun playDeathAnimation() {
		val deathVariant = getRandomAnimation(listOf("Death_1", "Death_2"))

		renderer.enqueue(
			Animation(
				name = "fail_death",
				sheet = createSpriteSheet(deathVariant),
				onFinish = {
					renderer.enqueue(
						Animation(
							name = "bleeding",
							loop = 10,
							sheet = createSpriteSheet("Deat_End"),
							onFinish = {
								renderer.enqueue(
									Animation(
										name = "comeback",
										sheet = createSpriteSheet("Spawn_2"),
										onFinish = { playDefaultAnimation() }
									)
								)
							}
						)
					)
				}
			)
		)
	}

	private fun playPoopingDiggingSequence() {
		renderer.enqueue(
			Animation(
				name = "sit_up_for_poop",
				sheet = createSpriteSheet("Sit_Up"),
				onFinish = {
					renderer.enqueue(
						Animation(
							name = "fail_pooping",
							sheet = createSpriteSheet("Pooping"),
							onFinish = {
								val digVariant = getRandomAnimation(listOf("Dig"))
								renderer.enqueue(
									Animation(
										name = "fail_digging",
										loop = MEDIUM_LOOP,
										sheet = createSpriteSheet(digVariant),
										onFinish = {
											renderer.enqueue(
												Animation(
													name = "sit_down_after_poop",
													sheet = createSpriteSheet("Sit_Down"),
													onFinish = { playDefaultAnimation() }
												)
											)
										}
									)
								)
							}
						)
					)
				}
			)
		)
	}

	private fun playSuccessAnimation() {
		val walkVariant = getRandomAnimation(listOf("Walk"))

		renderer.enqueue(
			Animation(
				name = "success_walk",
				loop = SHORT_LOOP,
				sheet = createSpriteSheet(walkVariant),
				onFinish = {
					val jumpVariant = getRandomAnimation(listOf("J_1", "J_2", "J_3"))
					renderer.enqueue(
						Animation(
							name = "success_jump",
							loop = SHORT_LOOP,
							sheet = createSpriteSheet(jumpVariant),
							onFinish = { playDefaultAnimation(withRandom = false) }
						)
					)
				}
			)
		)
	}

	private fun playProgressAnimation() {
		val runVariant = getRandomAnimation(listOf("Run"))

		renderer.enqueue(
			Animation(
				name = "progress_run",
				loop = INFINITE,
				sheet = createSpriteSheet(runVariant)
			)
		)
	}

	private fun playCelebrateAnimation() {
		renderer.enqueue(
			Animation(
				name = "celebrate_start",
				sheet = createSpriteSheet("Sit_Up"),
				onFinish = {
					val attackVariant = getRandomAnimation(
						listOf("Attack_1", "Attack_2", "Attack_3", "Attack_4", "Attack_5")
					)

					renderer.enqueue(
						Animation(
							name = "celebrate_attack",
							loop = MEDIUM_LOOP,
							sheet = createSpriteSheet(attackVariant),
							onFinish = {
								val walkVariant =
									getRandomAnimation(listOf("Walk"))
								renderer.enqueue(
									Animation(
										name = "celebrate_walk",
										loop = MEDIUM_LOOP,
										sheet = createSpriteSheet(walkVariant),
									)
								)
							}
						)
					)
				}
			)
		)
	}

	private fun loadImage(path: String): BufferedImage {
		val inputStream = SpriteSheet::class.java.getResourceAsStream(path)
		return ImageIO.read(inputStream)
	}
}