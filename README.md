# dotMOD

dotMOD is a client-side Fabric utility mod for Minecraft 1.21.11. It combines
compact crafting controls, a draggable vanilla HUD editor, local player colors,
uniform player name tags, and Toggle Shift without requiring a server plugin or
a server-side installation.

- Current version: **2.0.0**
- Author: **DinoMiHa**
- License: **GNU GPL v3.0**
- Modrinth: <https://modrinth.com/mod/dotmod>

## Features

### Quick Craft

Quick Craft adds a configurable `Craft` button to the survival inventory and
crafting-table screens.

- The 2x2 and 3x3 layouts have separate source-slot presets.
- Source slots use logical player inventory indices from `0` to `35`.
- The button follows the container when the recipe book changes its position.
- Button text and X/Y offsets are configurable.
- The implementation uses ordinary vanilla `PICKUP` inventory clicks.
- The cursor must be empty before Quick Craft starts; otherwise the action is
  safely ignored.
- Existing crafting-grid stacks are not replaced.
- The recipe output is not collected automatically.

Default source slots:

```text
2x2: 9,10,18,19
3x3: 9,10,11,18,19,20,27,28,29
```

Quick Craft is intentionally not an automatic crafting system and does not use
custom packets.

### HUD Editor

The `HUD` button opens a visual editor for selected vanilla HUD elements:

- hearts;
- armor;
- hunger;
- hotbar;
- experience bar;
- experience level;
- status effects;
- boss bars;
- scoreboard sidebar.

Each element can be dragged independently. Positions are stored as pixel
offsets from the element's vanilla position.

The editor supports:

- configurable grid snapping;
- magnetic snapping to the original `dx = 0` and `dy = 0` positions;
- edge-to-edge alignment with other HUD elements;
- matching left, center, right, top, middle, and bottom coordinates;
- visible cyan alignment guides;
- configurable magnetic snap distance;
- reset to vanilla offsets.

The editor is available from the survival inventory, crafting table, and
creative inventory. HUD positions are local and never affect the server.

### Player Name Colors

dotMOD can assign local colors to players selected with the crosshair.

| Default key | Action |
| --- | --- |
| `G` | Set the targeted player to the configured green color |
| `R` | Set the targeted player to the configured red color |
| `V` | Reset the targeted player's color |

Colors are:

- stored by player UUID;
- shown above player models;
- shown in the vanilla tab list;
- visible only on the local client;
- optionally persisted between restarts.

The green, red, and fallback colors accept `#RRGGBB` values. Chat notifications
for color changes can be disabled.

### Uniform Name Tags

Uniform Name Tags keeps visible player labels at a consistent screen-space size
and adds an opaque configurable background.

- Default toggle key: `N`.
- Size multiplier: `0.1` to `5.0`.
- Background color: configurable `#RRGGBB`, black by default.
- State is saved between restarts.
- Existing player-name colors are preserved.
- Vanilla name-tag visibility and render distance still apply.

This feature changes only player labels rendered by the client. It does not make
hidden or otherwise unavailable players visible.

### Toggle Shift

Toggle Shift keeps the configured Minecraft sneak action active until the
toggle is pressed again.

- Default key: `Right Shift`.
- The player stands immediately when the mode is disabled.
- A physically held normal sneak key remains respected.
- Opening a GUI does not cause a one-tick stand/crouch flicker.
- Vanilla Hold and Toggle sneak accessibility modes are supported.
- The active state is saved between restarts.

### Configuration

The configuration screen is available through Mod Menu. Current categories are:

- General;
- Quick Craft;
- HUD Editor;
- Name Colors;
- Uniform Name Tags;
- Toggle Shift;
- Keybind information.

Settings are stored in:

```text
config/dotmod.json
```

Writes use a temporary file and atomic replacement where supported. If the JSON
cannot be read, dotMOD preserves it as `config/dotmod.json.broken`, restores safe
defaults, and continues startup.

Minecraft stores key rebindings in its normal `options.txt`. Every dotMOD key
can be changed under Minecraft's Controls screen.

## Requirements

| Dependency | Required version |
| --- | --- |
| Minecraft | 1.21.11 |
| Java | 21 or newer |
| Fabric Loader | 0.19.3 or newer |
| Fabric API | 0.140.0+1.21.11 or newer for Minecraft 1.21.11 |
| Mod Menu | 17.0.0-beta.1 or newer |
| Cloth Config | 21.11.153 or newer |

Mod Menu and Cloth Config are currently required dependencies.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install Fabric API, Mod Menu, and Cloth Config.
3. Place the dotMOD JAR in the instance's `mods` directory.
4. Start Minecraft and open Mod Menu to configure dotMOD.

dotMOD is client-only. It does not need to be installed on a dedicated server,
Fabric server, Paper/Spigot server, or Realm.

## Compatibility and Limitations

- Minecraft 1.21.11 is the only supported game version for this release.
- The mod uses version-specific client rendering mixins. Other mods that replace
  the same HUD or player-label render paths can conflict.
- HUD positions are pixel offsets and are not automatically clamped to the
  screen after changing resolution or GUI scale.
- HUD editor rectangles approximate dynamic vanilla elements such as multi-row
  hearts, multiple boss bars, and variable-size scoreboards.
- Player colors depend on client-known entities and tab-list data.
- Most configuration UI text is currently English; keybind names and selected
  messages include English and Russian translations.
- There are currently no `/dot` or `/dotmod` in-game commands.
- Quick Craft sends only normal inventory interactions, so server latency or
  server-side inventory restrictions can affect the result.

## Development

Clone the repository and use the included Gradle wrapper with Java 21.

```bash
./gradlew runClient
./gradlew clean build
```

Built artifacts are written to:

```text
build/libs/
```

The current architecture and staged development plan are documented in
[`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md).

Automated tests are not yet present; `./gradlew test` currently reports
`NO-SOURCE`.

## License

dotMOD is available under the [GNU General Public License v3.0](LICENSE).

Copyright (c) 2026 DinoMiHa.
