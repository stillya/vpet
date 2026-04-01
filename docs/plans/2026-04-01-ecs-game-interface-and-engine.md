# VPet ECS Rework: Introduce Game Interface and Engine Without Touching Status Bar

## Overview

Introduce a `Game` interface that `PetAnimated` implements, making the pet a first-class
participant in a unified game concept. Alongside this, introduce a `GameEngine` — a clean,
readable coordinator that owns the game loop, world tick, input, and rendering — replacing the
ad-hoc logic scattered across `GameController`. The status bar plugin (widget, icon renderer,
`Animated` interface, `AnimationEventService`) is left completely untouched. The ECS world and
physics remain as-is; the new layer just organizes them behind a cleaner OO structure.

Key constraint: The `Game` interface and `GameEngine` are additive. `PetAnimated` gains one
more interface but loses nothing. `GameController` is replaced internally (or becomes a thin
adapter), but nothing in the status bar path changes.

## Context

- Files involved (read-only, no changes):
  - `AnimatedStatusBarWidget.kt`
  - `graphics/DefaultIconRenderer.kt`
  - `Animated.kt`
  - `service/AnimationEventService.kt`
  - `service/ActivityTracker.kt`
- Files involved (modified):
  - `pet/PetAnimated.kt` — implement `Game` interface, add `onGameStart()` / `onGameStop()` hooks
  - `game/GameController.kt` — replaced internally by `GameEngine`, kept as thin plugin.xml adapter
  - `game/WorldUpdate.kt` — minor: remove `animation: Animation` from `GameFrame` (decouple physics from animation concern)
  - `game/World.kt` — no structural change, potentially minor
- Files created:
  - `game/Game.kt` — the `Game` interface
  - `game/GameEngine.kt` — owns the game loop, world tick, input, rendering coordination
- Related patterns:
  - Existing `javax.swing.Timer` in `GameController` (moves to `GameEngine`)
  - Existing `IdeEventQueue.EventDispatcher` key handling (moves to `GameEngine`)
  - Existing `TileMapSyncer`, `BugSpawner` (unchanged, used by `GameEngine`)
  - Existing `GameRenderer` (unchanged, driven by `GameEngine`)
  - Existing `World`, `WorldUpdate`, `EntityRegistry`, `Character`, `CharacterIntent` (unchanged)

## Development Approach

- **Testing approach**: Regular (code first, then tests)
- Complete each task fully before moving to the next
- **CRITICAL: every task MUST include new/updated tests**
- **CRITICAL: all tests must pass before starting next task**
- No new dependencies: use only what's already in the project
- Status bar plugin is a no-go zone: zero changes to `AnimatedStatusBarWidget`, `DefaultIconRenderer`, `Animated`, or `AnimationEventService`

## Implementation Steps

### Task 1: Define the `Game` interface

**Files:**
- Create: `src/main/kotlin/dev/stillya/vpet/game/Game.kt`
- Modify: `src/main/kotlin/dev/stillya/vpet/pet/PetAnimated.kt`

The `Game` interface is the single contract that formalizes what it means to "be a game
participant." `PetAnimated` (currently `Animated` + `Character`) will implement this too.
The interface is minimal — it describes lifecycle hooks from the game perspective, not the
status bar perspective.

```kotlin
interface Game {
    fun onGameStart()
    fun onGameStop()
}
```

`onGameStart()` is called when game mode is entered — the pet can react (e.g., suspend
status-bar animation scheduling). `onGameStop()` is called on exit. The status bar flow is
untouched; these are purely additive hooks.

- [x] Create `Game.kt` with the interface definition
- [x] Add `onGameStart()` / `onGameStop()` no-op implementations to `PetAnimated` (implements `Game`)
- [x] Write tests: `GameInterfaceTest` — verify `PetAnimated` can be cast to `Game`, both methods callable without error
- [x] Run test suite - must pass before Task 2

### Task 2: Introduce `GameEngine` — clean coordinator replacing `GameController`'s internals

**Files:**
- Create: `src/main/kotlin/dev/stillya/vpet/game/GameEngine.kt`
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameController.kt` (delegate to `GameEngine`)

Extract all game-mode coordination logic from `GameController` into a well-structured
`GameEngine` class. `GameEngine` owns: the game loop (`javax.swing.Timer`), input gathering,
world tick (`WorldUpdate.tick()`), and renderer updates. `GameController` becomes a one-line
delegate.

```kotlin
class GameEngine(
    private val project: Project,
    private val editor: Editor,
    private val character: Character,  // PetAnimated, injected
    private val renderer: GameRenderer,
) {
    private var world: World = World()
    private var timer: Timer? = null
    private var tileMapSyncer: TileMapSyncer? = null
    fun start(disposable: Disposable) { ... }
    fun stop() { ... }
    private fun tick() { ... }           // called by Timer every 16ms
    private fun gatherInput(): InputState { ... }
}
```

`GameEngine.tick()` contains the extracted logic from `GameController.gameTick()`: delta
time, input, `WorldUpdate.tick()`, `panel.update()`, `panel.repaint()`. It is a plain
readable function — no statefulness beyond `world`, `lastTickNanos`, `keysHeld`.

`GameController.enterGameMode()` becomes: instantiate `GameEngine`, call `engine.start()`.
`exitGameMode()` calls `engine.stop()`. All 100+ lines of logic move out of `GameController`.

- [x] Create `GameEngine.kt` with `start()`, `stop()`, `tick()`, `gatherInput()`, key dispatcher setup
- [x] Move `gameTick()` logic verbatim into `GameEngine.tick()` (no behavior change)
- [x] Move `gatherInput()` and key dispatcher into `GameEngine`
- [x] Move `resizeListener`, `TileMapSyncer`, `BugSpawner` usage into `GameEngine`
- [x] `GameController.enterGameMode()` creates and starts a `GameEngine`; `exitGameMode()` stops it
- [x] Write tests: `GameEngineTest` — verify `start()`/`stop()` lifecycle, `gatherInput()` with mocked key state
- [x] Run test suite - must pass before Task 3

### Task 3: Wire `PetAnimated` as `Game` into `GameController`/`GameEngine`

**Files:**
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameController.kt`
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameEngine.kt`

`GameController` currently casts `project.service<Animated>()` to `Character`. Now also cast
to `Game` and call lifecycle hooks. This is the connection that makes `PetAnimated` aware of
game mode transitions without changing anything in the status bar flow.

```kotlin
// GameController.enterGameMode():
val animated = project.service<Animated>()
val character = animated as Character
val game = animated as Game
game.onGameStart()
// ... start GameEngine ...

// GameController.exitGameMode():
engine.stop()
game.onGameStop()
```

`PetAnimated.onGameStart()` can suspend idle animation scheduling if desired (or be a no-op
initially). The key point is the contract exists and is called.

- [x] `GameController` resolves `Game` from the `Animated` service and calls `onGameStart()` / `onGameStop()`
- [x] `PetAnimated.onGameStart()` — initial implementation: no-op (hook exists, behavior can be added later)
- [x] `PetAnimated.onGameStop()` — initial implementation: no-op
- [x] Write tests: `PetAnimatedGameLifecycleTest` — mock setup calling `onGameStart` / `onGameStop`, verify no errors and state is consistent
- [x] Run test suite - must pass before Task 4

### Task 4: Decouple `GameFrame` — remove `Animation` from physics output

**Files:**
- Modify: `src/main/kotlin/dev/stillya/vpet/game/WorldUpdate.kt`
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameEngine.kt`

`GameFrame` currently carries `animation: Animation`, mixing physics concern (`WorldUpdate`)
with animation concern (`Character`). Remove it. The animation is already returned via
`CharacterIntent` from `character.update()` — `GameEngine.tick()` can use it directly without
routing through `GameFrame`.

```kotlin
// Before
data class GameFrame(val world: World, val animation: Animation, val bounds: IntRange)
// After
data class GameFrame(val world: World, val bounds: IntRange)
```

`WorldUpdate.tick()` still returns `GameFrame`. `GameEngine.tick()` gets `animation` from
`character.update()` (via `WorldUpdate`) — or `WorldUpdate` returns both as a pair. Cleanest:
`WorldUpdate.tick()` returns `GameFrame` (no animation), and `GameRenderer.update()` receives
the animation from `GameEngine` which holds the last `CharacterIntent`.

- [x] Remove `animation: Animation` field from `GameFrame`
- [x] Update `WorldUpdate.tick()` return — `GameFrame` no longer carries animation
- [x] Update `GameEngine.tick()` to pass animation from `CharacterIntent` to `GameRenderer.update()`
- [x] Update `GameRenderer.update()` signature if needed to accept animation separately
- [x] Write tests: `WorldUpdateTest` — verify `GameFrame` no longer contains animation, world state transitions correct
- [x] Run test suite - must pass before Task 5

### Task 5: Verify acceptance criteria

- [ ] Run `./gradlew test` — all tests pass
- [ ] Run `./gradlew buildPlugin` — builds without errors
- [ ] Confirm `AnimatedStatusBarWidget`, `DefaultIconRenderer`, `Animated`, `AnimationEventService` have zero changes (diff check)
- [ ] `PetAnimated` implements `Animated`, `Character`, and `Game` — all three interfaces coexist cleanly
- [ ] `GameEngine` is the readable single coordinator for all game-mode logic
- [ ] `GameController` is a thin adapter (plugin.xml entry point only)

### Task 6: Update documentation

**Files:**
- Modify: `CLAUDE.md`
- Modify: `docs/ARCHITECTURE.md`
- Move: this plan to `docs/plans/completed/`

- [ ] Update `CLAUDE.md` Architecture section: `PetAnimated` implements `Animated + Character + Game`, `GameEngine` as coordinator, `Game` interface purpose
- [ ] Update `docs/ARCHITECTURE.md` to reflect `Game` interface and `GameEngine` in the flow diagram
- [ ] Move this plan file to `docs/plans/completed/`
