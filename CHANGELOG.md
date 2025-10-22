<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# vpet Changelog

## [Unreleased]

## [0.0.6] - 2025-10-23

### Fixes

- **Turn off hot install** Disable hot install due to problem with dynamic plugin widgets

## [0.0.5] - 2025-10-21

### Initial Release

Animated pixel art companion for IntelliJ IDEA that lives in your status bar and reacts to
your development activities.

#### Features

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
