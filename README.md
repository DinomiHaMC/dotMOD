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

### InvSeeMenu (ISM)

ISM is a standalone local virtual-inventory screen. It never uses a
`ScreenHandler`, changes the player's real slots, or sends inventory packets.

| Mode | Command | Behavior |
| --- | --- | --- |
| `П` View | `/dot ism view` | Frozen read-only snapshot of the current inventory |
| `Р` Editor | `/dot ism edit` | Edit a local copy of the current inventory |
| `К` Creative editor | `/dot ism creative` | Edit the local draft using the client item catalog |

The editor supports local cursor movement, swapping and merging stacks,
right-click splitting, deletion, strict amount editing, rollback, tooltips,
keyboard navigation, component-aware item information copying, explicit save,
and discard confirmation. Creative catalog actions copy stacks into local state;
they never grant items to the player.

The serialized draft preserves item IDs, counts, and data components through
Minecraft's registry-aware `ItemStack` codec. Corrupt drafts recover from a
backup where possible. Drafts containing a newer schema or registry data that
is unavailable in the current world are opened read-only and are not
overwritten.

### Configuration

The configuration screen is available through Mod Menu or `/dot config`.
Current categories are:

- General;
- Commands;
- Quick Craft;
- HUD Editor;
- Name Colors;
- Uniform Name Tags;
- Toggle Shift;
- Keybind information.

Settings are stored in:

```text
config/dotmod/config.json
config/dotmod/player-colors.json
config/dotmod/invsee-draft.json
```

Each subsystem has its own path under `config/dotmod/`. Writes use a temporary
file, preserve the previous version as `.bak`, and use atomic replacement where
supported. If JSON cannot be read, dotMOD preserves it as `.broken`, restores a
valid `.bak` when available, otherwise restores safe defaults, and continues
startup.

The previous flat `config/dotmod.json` format is migrated automatically. The
original is retained as `config/dotmod.json.migrated.bak` before the new files
are written. `/dot reload` reloads configuration and player colors without
restarting Minecraft.

Minecraft stores key rebindings in its normal `options.txt`. Every dotMOD key
can be changed under Minecraft's Controls screen.

### Client Commands

`/dot` and `/dotmod` are complete synonyms. They are Fabric client commands and
do not send custom packets or require dotMOD on the server.

| Command | Action |
| --- | --- |
| `/dot` or `/dot help` | Show localized clickable help |
| `/dot config` | Open configuration |
| `/dot hud` | Open the HUD editor |
| `/dot ism [view\|edit\|creative]` | Open a local ISM session |
| `/dot reload` | Reload configuration and player colors |
| `/dot prefix dotmod` | Use `dotMod:` |
| `/dot prefix brackets` | Use `[dotMod]` |
| `/dot prefix dot` | Use `.` |
| `/dot prefix custom "text"` | Use a quoted custom prefix |

Messages are routed through one service and support informational, warning,
error, success, click, and hover content. English and Russian are included.

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
- ISM intentionally models the requested hotbar, main inventory, armor, and
  offhand slots (`0..40`). Minecraft 1.21.11's special body/saddle slots are not
  part of the current ISM schema.
- The creative ISM catalog contains locally registered default item variants.
  A draft may therefore contain an item unavailable on the current server, but
  ISM never attempts to grant or insert it into the real inventory.
- A server command named `/dot` or `/dotmod` may conflict with the client root.
  Fabric's public client-command API does not expose the unmerged server command
  tree, so dotMOD cannot reliably detect this server-side name collision.
- Quick Craft sends only normal inventory interactions, so server latency or
  server-side inventory restrictions can affect the result.

## Development

Clone the repository and use the included Gradle wrapper with Java 21.

```bash
./gradlew runClient
./gradlew test
./gradlew clean build
```

Built artifacts are written to:

```text
build/libs/
```

Project documentation:

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) describes the current modules,
  startup order, storage guarantees, and client/server boundary.
- [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md) records the audit and
  staged implementation plan.
- [`docs/TESTING.md`](docs/TESTING.md) contains automated coverage and the manual
  in-game checklist.

JUnit currently covers config validation, future-schema protection, legacy
migration and retry, malformed UUID isolation, safe JSON/backup recovery,
atomic storage, localization parity, ISM permissions and mutations, ItemStack
component serialization, draft recovery/write protection, layout, catalog, and
player snapshot boundaries.

## License

dotMOD is available under the [GNU General Public License v3.0](LICENSE).

Copyright (c) 2026 DinoMiHa.
