package dev.stillya.vpet.pet

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import dev.stillya.vpet.Animated
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.animation.*
import dev.stillya.vpet.config.SpriteSheetAtlas
import dev.stillya.vpet.game.AABB
import dev.stillya.vpet.game.Character
import dev.stillya.vpet.game.CharacterIntent
import dev.stillya.vpet.game.EntityID
import dev.stillya.vpet.game.GamePhase
import dev.stillya.vpet.game.InputState
import dev.stillya.vpet.game.Physics
import dev.stillya.vpet.game.TickContext
import dev.stillya.vpet.game.Velocity
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationTrigger
import dev.stillya.vpet.graphics.create
import dev.stillya.vpet.service.ActivityTracker
import java.awt.Image
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.exp
import kotlin.random.Random

class PetAnimated(
	private val project: Project,
) : Animated, Character {
	private val atlasLoader: AtlasLoader
		get() = service<AtlasLoader>()
	private val renderer: IconRenderer
		get() = project.service<IconRenderer>()

	var random: Random = Random
		set(value) {
			field = value
			transitionMatrix = buildTransitionMatrix()
		}

	private lateinit var atlas: SpriteSheetAtlas
	private lateinit var image: Image

	private val bridges: List<Bridge> = buildBridges()
	private var transitionMatrix: TransitionMatrix = buildTransitionMatrix()
	private var animationPlayer: AnimationPlayer = AnimationPlayer(bridges)

	private var isObserving = AtomicBoolean(false)

	@Volatile
	private var currentState: AnimationState = AnimationState.IDLE

	@Volatile
	private var lastPivotTimeMs: Long = 0L

	@Volatile
	private var observingStartTimeMs: Long = 0L

	companion object {
		private val log = logger<PetAnimated>()
		private const val PIVOT_INTERVAL_MS = 30_000L
		private const val OBSERVING_DURATION_MS = 120_000L

		private const val WALK_SPEED = 9.0f
		private const val JUMP_VELOCITY = -15.6f
		private const val GROUND_DAMPING = 18.0f
		private const val AIR_DAMPING = 3.0f
		private const val AIR_CONTROL = 0.3f
	}

	override fun init(params: Animated.Params) {
		log.trace("Initializing PetAnimated with atlas: ${params.atlasPath}, image: ${params.imgPath}")
		atlas = atlasLoader.load(params.atlasPath)
			?: throw IllegalArgumentException("Atlas not found")
		image = loadImage(params.imgPath)

		val context = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
		log.trace("Starting initial transition to IDLE state")
		val idleSequence = transitionMatrix.transitionTo(currentState, AnimationState.IDLE)

		animationPlayer.setInitialEffect(idleSequence.first.requirement.toEffect())

		playTransition(idleSequence, context)
	}

	private fun playTransition(
		sequence: Pair<AnimationSequenceWithRequirement, AnimationState>,
		context: AnimationContext? = null
	) {
		val steps = animationPlayer.buildPlaylist(sequence.first)

		if (steps.isEmpty()) {
			log.trace("Empty transition steps, skipping")
			return
		}

		log.trace(
			"Playing transition sequence [${sequence.second}]: ${
				steps.mapIndexed { idx, step ->
					val tag =
						if (step.variants.isNotEmpty()) "${step.variants}" else step.animationTag
					"[$idx]$tag(loops=${step.loops})"
				}.joinToString(" → ")
			}"
		)

		currentState = sequence.second

		val animations = steps.mapIndexed { index, step ->
			val tag = if (step.variants.isNotEmpty()) {
				step.variants[random.nextInt(step.variants.size)].also {
					log.trace("Step $index: Selected '$it' from variants ${step.variants}")
				}
			} else {
				step.animationTag.also {
					log.trace("Step $index: Animation '$it'")
				}
			}

			val onFinish = {
				if ((index == steps.size - 1) && step.loops != INFINITE) {
					log.trace("Animation sequence completed, returning to stable idle state")
					val idleContext = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
					playTransition(
						transitionMatrix.transitionTo(currentState, AnimationState.IDLE),
						idleContext
					)
				}
			}

			runCatching {
				Animation(
					name = tag,
					loop = step.loops,
					sheet = atlas.create(image, tag),
					onFinish = onFinish,
					context = context,
					guard = step.guard,
					state = sequence.second
				)
			}.onFailure {
				log.warn("Failed to create animation for tag '$tag': ${it.message}", it)
			}.getOrDefault(Animation.empty(onFinish = onFinish))
		}

		animations.forEachIndexed { index, animation ->
			if (index < animations.size - 1) {
				animation.nextAnimation = animations[index + 1]
			}
		}

		log.trace("Enqueueing first animation: ${animations.first().name}")
		renderer.enqueue(animations.first())
	}

	override fun onFail() {
		log.trace("BUILD FAILED - Transitioning to FAILED")
		exitObservingMode()
		val sequence = transitionMatrix.transitionTo(currentState, AnimationState.FAILED)
		if (sequence.first.steps.isNotEmpty()) {
			val context = renderer.createAnimationContext(AnimationTrigger.BUILD_FAIL)
			playTransition(sequence, context)
		} else {
			log.trace("No transition available from $currentState to FAILED, ignoring")
		}
	}

	override fun onSuccess() {
		log.trace("BUILD SUCCESS - Transitioning to CELEBRATING")
		exitObservingMode()
		val sequence = transitionMatrix.transitionTo(currentState, AnimationState.CELEBRATING)
		if (sequence.first.steps.isNotEmpty()) {
			val context = renderer.createAnimationContext(AnimationTrigger.BUILD_SUCCESS)
			playTransition(sequence, context)
		} else {
			log.trace("No transition available from $currentState to CELEBRATING, ignoring")
		}
	}

	override fun onProgress() {
		log.trace("BUILD START - Transitioning to RUNNING")
		exitObservingMode()
		val sequence = transitionMatrix.transitionTo(currentState, AnimationState.RUNNING)
		if (sequence.first.steps.isNotEmpty()) {
			val context = renderer.createAnimationContext(AnimationTrigger.BUILD_START)
			playTransition(sequence, context)
		} else {
			log.trace("No transition available from $currentState to RUNNING, ignoring")
		}
	}

	override fun onCompleted() {
		log.trace("BUILD COMPLETED - Transitioning to CELEBRATING")
		exitObservingMode()
		val sequence = transitionMatrix.transitionTo(currentState, AnimationState.CELEBRATING)
		if (sequence.first.steps.isNotEmpty()) {
			val context = renderer.createAnimationContext(AnimationTrigger.BUILD_SUCCESS)
			playTransition(sequence, context)
		} else {
			log.trace("No transition available from $currentState to CELEBRATING, ignoring")
		}
	}

	override fun onOccasion() {
		log.trace("USER CLICK - Transitioning to OCCASION")
		exitObservingMode()
		val sequence = transitionMatrix.transitionTo(currentState, AnimationState.OCCASION)
		if (sequence.first.steps.isNotEmpty()) {
			val context = renderer.createAnimationContext(AnimationTrigger.USER_CLICK)
			playTransition(sequence, context)
		} else {
			log.trace("No transition available from $currentState to OCCASION, ignoring")
		}
	}

	private fun exitObservingMode() {
		if (isObserving.compareAndSet(true, false)) {
			log.trace("Exiting OBSERVING mode")
			observingStartTimeMs = 0L
			renderer.setFlipped(false)
			ActivityTracker.getInstance(project).notifyActivity()
		}
	}

	override fun onStartObserving() {
		if (currentState == AnimationState.IDLE && isObserving.compareAndSet(false, true)) {
			log.trace("INACTIVITY - Starting OBSERVING mode")
			observingStartTimeMs = System.currentTimeMillis()
			lastPivotTimeMs = observingStartTimeMs
			val sequence = transitionMatrix.transitionTo(currentState, AnimationState.OBSERVING)
			if (sequence.first.steps.isNotEmpty()) {
				val context = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
				playTransition(sequence, context)
			}
		}
	}

	override fun onCursorMove(isOnLeftSide: Boolean) {
		if (currentState != AnimationState.OBSERVING) {
			return
		}

		val now = System.currentTimeMillis()
		val observingDuration = now - observingStartTimeMs

		if (observingDuration >= OBSERVING_DURATION_MS) {
			log.trace("Observing duration exceeded (${observingDuration}ms), returning to IDLE")
			exitObservingMode()
			val sequence = transitionMatrix.transitionTo(currentState, AnimationState.IDLE)
			if (sequence.first.steps.isNotEmpty()) {
				val context = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
				playTransition(sequence, context)
			}
			return
		}

		renderer.setFlipped(isOnLeftSide)

		val timeSinceLastPivot = now - lastPivotTimeMs
		if (timeSinceLastPivot >= PIVOT_INTERVAL_MS) {
			log.trace("Pivot timer triggered - transitioning to front stare")
			lastPivotTimeMs = now
			playPivotSequence()
		}
	}

	private fun playPivotSequence() {
		val pivotSequence = sequence {
			require { pose = Pose.STAND }
			play("R_A_1")
			play("R_A_2", loops = SHORT_LOOP)
			play("R_A_3")
			play("R_A_4")
			play("R_A_5", loops = INFINITE)
		}

		val context = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
		playTransition(pivotSequence to AnimationState.OBSERVING, context)
	}

	override fun id() = EntityID("cat")

	override fun collider() = AABB(width = 2, height = 2)

	override fun update(input: InputState, ctx: TickContext, dt: Float): CharacterIntent {
		val effectiveInput = if (ctx.phase == GamePhase.ENTRANCE) InputState() else input

		val velocity = processMovement(effectiveInput, ctx, dt)
		val jumped = effectiveInput.jumpJustPressed && ctx.isOnGround
		val isGrounded = if (jumped) false else ctx.isOnGround

		val direction = if (velocity.x > Physics.VELOCITY_EPSILON) Direction.RIGHT
		else if (velocity.x < -Physics.VELOCITY_EPSILON) Direction.LEFT
		else ctx.sprite.direction

		var tag = resolveAnimationTag(ctx.isOnGround, ctx.velocity.x, ctx.velocity.y, ctx.sprite.tag)
		var phase = ctx.phase

		if (tag == "Stop" && ctx.isOnGround) {
			val anim = createAnimation("Stop")
			if (anim != null && ctx.sprite.frameIndex >= anim.frameCount) {
				tag = "Idle"
			}
		}

		if (phase == GamePhase.ENTRANCE && ctx.isOnGround) {
			phase = GamePhase.PLAYING
			tag = "Idle"
		}

		val loop = if (tag == "Idle" || tag == "Walk") INFINITE else 0
		val animation = createAnimation(tag, loop) ?: Animation.empty()

		return CharacterIntent(velocity, isGrounded, animation, direction, phase)
	}

	private fun createAnimation(tag: String, loop: Int = 0): Animation? {
		return runCatching {
			Animation(
				name = tag,
				loop = loop,
				sheet = atlas.create(image, tag),
				onFinish = {},
				state = AnimationState.IDLE
			)
		}.getOrNull()
	}

	private fun processMovement(input: InputState, ctx: TickContext, dt: Float): Velocity {
		var vx = ctx.velocity.x
		var vy = ctx.velocity.y

		if (ctx.isOnGround) {
			vx = if (input.moveDirection != 0) {
				input.moveDirection.toFloat() * WALK_SPEED
			} else {
				val damped = vx * exp(-GROUND_DAMPING * dt)
				if (abs(damped) < Physics.VELOCITY_EPSILON) 0f else damped
			}
		} else {
			if (input.moveDirection != 0) {
				vx += input.moveDirection.toFloat() * WALK_SPEED * AIR_CONTROL * dt
			}
		}

		if (input.jumpJustPressed && ctx.isOnGround) {
			vy = JUMP_VELOCITY
		}

		return Velocity(vx, vy)
	}

	private fun resolveAnimationTag(isOnGround: Boolean, vx: Float, vy: Float, currentTag: String) = when {
		!isOnGround && vy < 0 -> "J_1"
		!isOnGround && vy >= 0 -> "J_U_D"
		currentTag == "J_U_D" && isOnGround -> "Stop"
		abs(vx) > Physics.VELOCITY_EPSILON -> "Walk"
		else -> "Idle"
	}

	private fun buildTransitionMatrix(): TransitionMatrix = transitions(random) {
		idle(
			sequence {
				require { pose = Pose.LIE }
				playRandom("Dream", "Rest", loops = INFINITE)
			},
			sequence {
				require { pose = Pose.SIT }
				playRandom("Sit", loops = INFINITE)
			},
			sequence {
				require { pose = Pose.STAND }
				playRandom("Idle", loops = INFINITE)
			}
		)

		from(AnimationState.IDLE) to AnimationState.RUNNING via sequence {
			require {
				pose = Pose.STAND
				speed = WALKING_SPEED
			}
			playInfinite(
				"Run",
				guard = AnimationGuard.buildGuard(),
				effect = StateEffect(pose = Pose.STAND, speed = RUNNING_SPEED)
			)
		}

		from(AnimationState.IDLE) to AnimationState.OCCASION via sequence {
			require { pose = Pose.STAND }
			playRandom("Paws", "Pac-Cat", "Goomba", "rook_around", loops = SHORT_LOOP)
		}

		from(AnimationState.RUNNING) to AnimationState.CELEBRATING via sequence {
			require {
				pose = Pose.STAND
				speed = WALKING_SPEED
			}
			transition("Stop", effect = StateEffect.stop())
			play("J_1", loops = SHORT_LOOP, effect = StateEffect.setPose(Pose.STAND))
		}

		from(AnimationState.RUNNING) to AnimationState.CELEBRATING via sequence {
			require {
				pose = Pose.STAND
				speed = WALKING_SPEED
			}
			transition("Stop", effect = StateEffect.stop())
			play("Paws", loops = SHORT_LOOP)
			playRandom("Attack_1", "Attack_2", "Attack_3", "Attack_4", "Attack_5", loops = SHORT_LOOP)
			play("Walk", loops = SHORT_LOOP, effect = StateEffect.setPose(Pose.STAND))
		}

		from(AnimationState.RUNNING) to AnimationState.FAILED via sequence {
			play("Dmg")
			playRandom("Death_1", "Death_2")
			play("Death_End", loops = MEDIUM_LOOP)
			play("Spawn_2", effect = StateEffect(pose = Pose.STAND, speed = 0f))
		}

		from(AnimationState.RUNNING) to AnimationState.FAILED via sequence {
			require {
				speed = NO_SPEED
			}
			play("Pooping")
			play("Dig", loops = MEDIUM_LOOP)
		}

		from(AnimationState.IDLE) to AnimationState.OBSERVING via sequence {
			require { pose = Pose.STAND }
			play("R_A_4")
			play("R_A_5", loops = INFINITE)
		}

		from(AnimationState.OBSERVING) to AnimationState.IDLE via sequence {
			require { pose = Pose.STAND }
			play("R_A_6")
		}

		from(AnimationState.OBSERVING) to AnimationState.RUNNING via sequence {
			require { pose = Pose.STAND }
			play("R_A_6")
			play("Walk")
			transition("Walk_Run", effect = StateEffect.setSpeed(WALKING_SPEED))
			playInfinite(
				"Run",
				guard = AnimationGuard.buildGuard(),
				effect = StateEffect(pose = Pose.STAND, speed = RUNNING_SPEED)
			)
		}

		from(AnimationState.OBSERVING) to AnimationState.CELEBRATING via sequence {
			require { pose = Pose.STAND }
			play("R_A_6")
			play("J_1", loops = SHORT_LOOP, effect = StateEffect.setPose(Pose.STAND))
		}

		from(AnimationState.OBSERVING) to AnimationState.FAILED via sequence {
			require { pose = Pose.STAND }
			play("R_A_6")
			play("Dmg")
			playRandom("Death_1", "Death_2")
			play("Death_End", loops = MEDIUM_LOOP)
			play("Spawn_2", effect = StateEffect(pose = Pose.STAND, speed = 0f))
		}

		from(AnimationState.OBSERVING) to AnimationState.OCCASION via sequence {
			require { pose = Pose.STAND }
			play("R_A_6")
			playRandom("Paws", "Pac-Cat", "Goomba", "rook_around", loops = SHORT_LOOP)
		}

	}

	private fun buildBridges(): List<Bridge> = listOf(
		Bridge(
			name = "Lie_to_Stand",
			animationTag = "J_1",
			guard = BridgeGuard.requirePose(Pose.LIE),
			effect = StateEffect.setPose(Pose.STAND)
		),
		Bridge(
			name = "Stand_to_Lie",
			animationTag = "Sit_Rest",
			guard = BridgeGuard.requirePose(Pose.STAND),
			effect = StateEffect.setPose(Pose.LIE)
		),
		Bridge(
			name = "Sit_to_Stand",
			animationTag = "Sit_Up",
			guard = BridgeGuard.requirePose(Pose.SIT),
			effect = StateEffect.setPose(Pose.STAND)
		),
		Bridge(
			name = "Stand_to_Sit",
			animationTag = "Sit_Down",
			guard = BridgeGuard.requirePose(Pose.STAND),
			effect = StateEffect.setPose(Pose.SIT)
		),
		Bridge(
			name = "NoSpeed_to_Walk",
			animationTag = "Walk",
			guard = BridgeGuard.requireSpeed(NO_SPEED),
			effect = StateEffect.setSpeed(WALKING_SPEED)
		),
		Bridge(
			name = "Walk_to_Run",
			animationTag = "Walk_Run",
			guard = BridgeGuard.requireSpeed(WALKING_SPEED),
			effect = StateEffect.setSpeed(RUNNING_SPEED)
		),
		Bridge(
			name = "Run_to_Walk",
			animationTag = "Stop",
			guard = BridgeGuard.requireSpeed(RUNNING_SPEED),
			effect = StateEffect.setSpeed(WALKING_SPEED)
		),
		Bridge(
			name = "Walk_to_NoSpeed",
			animationTag = "Stop",
			guard = BridgeGuard.requireSpeed(WALKING_SPEED),
			effect = StateEffect.setSpeed(NO_SPEED)
		),
		Bridge(
			name = "Run_to_NoSpeed",
			animationTag = "Stop",
			guard = BridgeGuard.requireSpeed(RUNNING_SPEED),
			effect = StateEffect.setSpeed(NO_SPEED)
		)
	)

	private fun loadImage(path: String): Image = ImageIO.read(javaClass.getResourceAsStream(path))

	private fun SequenceRequirement.toEffect(): StateEffect {
		return StateEffect(
			pose = this.pose,
			speed = this.speed,
			direction = this.direction
		)
	}
}
