<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# vpet Changelog

## [Unreleased]

### Added

- **Debug render mode**: Added an optional in-editor debug overlay for game rendering
- **FPS counter**: Show current renderer FPS inside debug mode

### Fixed

- **Editor tile map alignment**: Improved tile-map reconstruction for editor rendering so
  tab-indented code and widened visual spans map more accurately to the game collision
  grid
- **Kotlin runtime compatibility**: Bundle the matching Kotlin standard library with the
  plugin to avoid missing runtime helpers on older supported IDE baselines

## [0.2.1] - 2026-04-06

### Added

- **Build for 2026.1+**: Updated plugin to be compatible with IntelliJ IDEA 2026.1 and
  later versions

## [0.2.0] - 2026-04-06

### Added

- **Inline editor game mode**: The pet can jump into the editor and run as an inline
  platformer using your code as the level
- **Coin collection gameplay**: Collect coins in the editor and publish the final score on
  game exit

## [0.1.0] - 2025-12-08

### Added

- **Christmas Mode**: Disable default festive snowflakes and snowdrifts effect via
  settings

## [0.0.9] - 2025-12-08

### Added

- **Build for 2025.3+**: Updated plugin to be compatible with IntelliJ IDEA 2025.3 and
  later versions

## [0.0.8] - 2025-11-30

### Added

- **Cat Variant Selection**: Choose between pet variants in plugin settings
    - Default Cat and Alternative Cat variants available
- **Dynamic Snowflake effect**: Snowflakes and snowdrifts reacts on pet speed and state

## [0.0.7] - 2025-11-16

### Added

- **Christmas Mode**: Festive snowflakes and snowdrifts effect overlay. Currently, is
  enabled by default.
- **Plugin Settings**: New settings panel under `Settings > Tools > VPet`
    - Toggle to enable/disable Christmas mode
- **Effect System**: New system to provide visual effects (e.g., snowflakes) over the pet
  companion

## [0.0.6] - 2025-10-23

### Fixes

- **Turn off hot install** Disable hot install due to problem with dynamic plugin widgets

## [0.0.5] - 2025-10-21

### Initial Release

Animated pixel art companion for IntelliJ IDEA that lives in your status bar and reacts to
your development activities.

#### Added

- **Animated pet companion** in status bar with pixel art sprite animations
- **Build event reactions**: Pet responds to build start, success, failure, and completion
    - Running animation during builds
    - Celebration animations on success
    - Failure animations (death or digging) on build errors
- **Interactive behaviors**: Click the pet 10+ times to trigger special occasion
  animations
- **IDLE state variations**: Multiple idle behaviors (sitting, lying down, standing)
- **OBSERVING mode**:
    - Pet displays back view and follows cursor position across screen
    - Automatically flips sprite to face left or right based on cursor position
    - Pivots to stare at user with front-facing animation
- **State machine architecture**: Clean transition system between animation states
    - IDLE → RUNNING → CELEBRATING/FAILED → IDLE
    - IDLE → OBSERVING → IDLE
    - IDLE → OCCASION → IDLE
- **Guard system**: Epoch-based animation validation to prevent stale animations
- **Bridge animations**: Automatic pose/speed transitions between incompatible states
