# Collectable Coins with Sprite Rendering and Status Bar Score

## Overview

Replace the existing diamond-placeholder collectible bugs with animated coin collectibles using the pixel art coin sprite from glycin/intellij25 (MIT licensed). Rename the entity from "bug" to "coin" throughout, embed the coin sprite, and display the total coins collected across all game sessions as a persistent stat via a dedicated message bus topic in the status bar widget.

## Context

- Files involved:
  - `src/main/kotlin/dev/stillya/vpet/game/CollectibleComponents.kt` — rename BugVisual/BugColor to CoinVisual
  - `src/main/kotlin/dev/stillya/vpet/game/BugSpawner.kt` — rename to CoinSpawner
  - `src/main/kotlin/dev/stillya/vpet/game/GameRenderer.kt` — replace diamond renderBugs() with coin sprite rendering
  - `src/main/kotlin/dev/stillya/vpet/game/GameEngine.kt` — expose finalScore, update spawner reference
  - `src/main/kotlin/dev/stillya/vpet/game/GameController.kt` — publish coin count event on game exit
  - New: `src/main/kotlin/dev/stillya/vpet/game/CoinSpawner.kt` — replaces BugSpawner.kt
  - New: `src/main/kotlin/dev/stillya/vpet/game/CoinCollectedListener.kt` — topic interface + companion for custom message bus topic
  - `src/main/kotlin/dev/stillya/vpet/AnimatedStatusBarWidget.kt` — subscribe to coin topic, show count in tooltip
  - `src/main/resources/META-INF/spritesheets/coin/sprite.png` — coin sprite from glycin/intellij25 (MIT)
  - `src/main/resources/META-INF/spritesheets/coin/atlas.json` — atlas for coin sprite
  - `README.md` — add coin sprite attribution to existing Credits section
- Related patterns:
  - `VPetSettings.TOPIC` / `VPetSettingsListener` — message bus topic pattern to replicate
  - `AsepriteJsonAtlasLoader` / `SpriteSheetAtlas` for atlas loading
  - `BugSpawner` / `BugVisual` / `GameRenderer.renderBugs()` — existing pattern to replace
- Dependencies: coin.png from glycin/intellij25 (MIT license, attribution required)

## Development Approach

- Testing approach: Regular (code first, then tests)
- Complete each task fully before moving to the next
- CRITICAL: every task MUST include new/updated tests
- CRITICAL: all tests must pass before starting next task

## Implementation Steps

### Task 1: Download Coin Sprite and Create Atlas Asset

**Files:**
- Create: `src/main/resources/META-INF/spritesheets/coin/sprite.png`
- Create: `src/main/resources/META-INF/spritesheets/coin/atlas.json`
- Modify: `README.md`

- [x] Download coin.png from https://raw.githubusercontent.com/glycin/intellij25/main/src/main/resources/sprites/coin.png and save to `src/main/resources/META-INF/spritesheets/coin/sprite.png`
- [x] Inspect the PNG to determine frame dimensions
- [x] Write a minimal Aseprite-compatible atlas.json for the coin sprite with a single frame tag `"coin"` covering the full image
- [x] Add coin sprite attribution to the existing Credits section in `README.md` (glycin/intellij25, MIT license)
- [x] Verify atlas can be parsed by `AsepriteJsonAtlasLoader`

### Task 2: Rename Bug Entities to Coin Entities

**Files:**
- Modify: `src/main/kotlin/dev/stillya/vpet/game/CollectibleComponents.kt`
- Create: `src/main/kotlin/dev/stillya/vpet/game/CoinSpawner.kt`
- Delete: `src/main/kotlin/dev/stillya/vpet/game/BugSpawner.kt`

- [x] In `CollectibleComponents.kt`: remove `BugVisual` and `BugColor`; add `data class CoinVisual()`
- [x] Create `CoinSpawner.kt` mirroring `BugSpawner` logic but spawning `CoinVisual()` instead of `BugVisual(BugColor.*)`
- [x] Delete `BugSpawner.kt`
- [x] Update `GameEngine.kt`: replace `BugSpawner` reference with `CoinSpawner`
- [x] Write unit tests for `CoinSpawner` (coins spawn on solid tiles, count matches requested count)
- [x] Run `./gradlew test` — must pass

### Task 3: Render Coins as Sprites in GameRenderer

**Files:**
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameRenderer.kt`

- [x] Add lazily-initialized `coinFrames: List<BufferedImage>` that loads coin sprite via `AsepriteJsonAtlasLoader` + `SpriteSheet`, extracting the `"coin"` tag frames
- [x] Replace `renderBugs()` with `renderCoins()`: draw coin sprite frame scaled to `lineHeight` at each coin's world position
- [x] Use a simple frame counter for animation cycling if multiple frames; otherwise use frame 0
- [x] Remove all `BugVisual`, `BugColor`, and diamond polygon rendering code
- [x] Keep a fallback colored square if `coinFrames` is empty (defensive)
- [x] Write test verifying `coinFrames` loads non-empty from the atlas
- [x] Run `./gradlew test` — must pass

### Task 4: Create CoinCollectedListener Topic and Expose Score on Game Exit

**Files:**
- Create: `src/main/kotlin/dev/stillya/vpet/game/CoinCollectedListener.kt`
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameEngine.kt`
- Modify: `src/main/kotlin/dev/stillya/vpet/game/GameController.kt`

- [x] Create `CoinCollectedListener.kt`: define `interface CoinCollectedListener { fun onCoinsCollected(count: Int) }` and a companion object with `val TOPIC = Topic.create("CoinCollected", CoinCollectedListener::class.java)`
- [x] In `GameEngine`, add `val finalScore: Int get() = world.score`
- [x] In `GameController.exitGameMode()`, after `engineToStop?.stop()`, publish to `CoinCollectedListener.TOPIC` via `ApplicationManager.getApplication().messageBus.syncPublisher(CoinCollectedListener.TOPIC).onCoinsCollected(engineToStop?.finalScore ?: 0)`
- [x] Write unit test verifying topic fires with the correct score on game exit
- [x] Run `./gradlew test` — must pass

### Task 5: Display Coin Count in Status Bar Widget

**Files:**
- Modify: `src/main/kotlin/dev/stillya/vpet/AnimatedStatusBarWidget.kt`

- [ ] Subscribe `AnimatedStatusBarWidget` to `CoinCollectedListener.TOPIC` via `project.messageBus.connect(this).subscribe(CoinCollectedListener.TOPIC, ...)`
- [ ] In the listener callback: accumulate a local `totalCoinsCollected` counter and trigger a status bar update
- [ ] Override `getTooltipText()` on `AnimatedStatusBarWidget` to return `"Coins collected: $totalCoinsCollected"`
- [ ] Write test verifying tooltip text updates after receiving a topic event
- [ ] Run `./gradlew test` — must pass

### Task 6: Verify Acceptance Criteria

- [ ] Run full test suite: `./gradlew test`
- [ ] Run build: `./gradlew buildPlugin`
- [ ] Verify coins render as sprites (not diamonds) in game mode via `runIde`
- [ ] Verify coin count increments after exiting game
- [ ] Verify status bar tooltip shows coin count outside game mode

### Task 7: Update Documentation

- [ ] Update `CLAUDE.md` if new patterns introduced (CoinCollectedListener topic pattern)
- [ ] Move this plan to `docs/plans/completed/`
