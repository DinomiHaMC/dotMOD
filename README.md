# dotMOD

dotMOD is a client-side Fabric utility mod for Minecraft 1.21.11. It combines
compact crafting controls, a draggable vanilla HUD editor, local player colors,
uniform player name tags, captured Toggle Walk movement, Full Brightness, and
packet-free Freelook without requiring a server plugin or server installation.

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

The `HUD` button opens a visual editor for vanilla HUD sections and dotMOD
widgets:

- hearts;
- armor;
- hunger;
- hotbar;
- experience bar;
- experience level;
- status effects;
- boss bars;
- scoreboard sidebar;
- equipped armor icons;
- colored online players;
- compact durability readings.
- Toggle Walk/Shift runtime state;
- Freelook activity and camera return state.

Each element can be dragged independently. Positions use a nine-point screen
anchor plus scaled GUI-pixel offsets and are clamped to the visible screen.

The editor supports:

- configurable grid snapping;
- grid snapping with matching visible spacing;
- magnetic snapping to screen edges and centers;
- edge-to-edge alignment with other HUD elements;
- matching left, center, right, top, middle, and bottom coordinates;
- visible cyan alignment guides;
- configurable magnetic snap distance;
- per-widget visibility, scale, anchor, and reset from a right-click menu;
- opacity for custom widget surfaces and text;
- real custom-widget previews, including hidden-widget ghost previews;
- reset to registered defaults.

The editor is available from the survival inventory, crafting table, and
creative inventory. HUD positions are local and never affect the server.

The Armor widget reads the four equipped armor slots. Colored Online includes
only UUIDs currently listed in the client tab list and only displays entries
that have a local player color. The Durability widget reads main hand, offhand,
and armor stacks, uses a red-yellow-green interpolated scale, and can issue
tick-based low-durability warnings with a monotonic cooldown.

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

### Toggle Walk And Shift

Toggle Walk snapshots the physically held forward, sprint, and jump bindings,
then forces exactly that combination until toggled off. If none are held it
defaults to forward. Sprint is captured only when retention is enabled; observed
vanilla sprint can still arm retention while forward is active. It never calls
`setSprinting`. Toggle Shift shares the lifecycle-safe input owner for sneak.

- Toggle Walk and emergency release are unbound by default; bind them in Controls.
- Toggle Shift defaults to `Right Shift`.
- Ordinary physically held keys are restored after user toggle-off.
- Configured chat/screens, focus loss, death, disconnect, world/player changes,
  and client shutdown hard-release every key dotMOD changed.
- Vanilla Hold and Toggle sneak accessibility modes are supported.
- Active movement state is runtime-only and always starts inactive.

### Freelook

Freelook orbits the local camera without changing player yaw/pitch or sending
look packets. Its key is unbound by default and supports Hold or Toggle mode.
Sensitivity, X/Y inversion, pitch bounds, smooth return duration, and indicator
are configurable. Activation always switches to third-person back; normal
release keeps it through camera return, then restores the exact prior
perspective. Screens, focus or cursor loss, death, disconnect, owner changes,
and disabling hard-reset immediately. Manual F5 changes remain user-owned.

### Full Brightness

Full Brightness is an unbound local toggle that feeds full vanilla night-vision
strength into the lightmap. It is stronger than maximum gamma, does not apply a
status effect or icon, sends no packet, and never changes the gamma slider or
`options.txt`. Runtime active state is never persisted and resets on disconnect.

### InvSeeMenu (ISM)

ISM is a standalone local virtual-inventory screen. It never uses a
`ScreenHandler`, changes the player's real slots, or sends inventory packets.

| Mode | Command | Behavior |
| --- | --- | --- |
| `V` View | `/dot ism view` | Frozen read-only snapshot of the current inventory |
| `E` Editor | `/dot ism edit` | Edit a local copy of the current inventory |
| `C` Creative editor | `/dot ism creative` | Edit the local draft using the client item catalog |

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

### Inventory Presets

Inventory presets store a versioned 41-slot ISM snapshot together with:

- UUID identity;
- case-insensitively unique name;
- creation and update timestamps;
- optional description and tags;
- hotbar, main inventory, armor, and offhand contents.

New presets start from a local copy of the current player inventory. Selecting
a preset marks it active and makes a best-effort rearrangement of available
exact whole stacks through normal synchronized inventory clicks; unavailable or
blocked slots are left unchanged and reported as unresolved.

Each preset is stored as `config/dotmod/presets/<uuid>.json`; user names never
become filesystem paths. `index.json` stores only ordering and the active UUID.
The repository recovers orphan files, validates revisions before edits, keeps
backups, moves deleted files to local trash, and blocks writes when a primary or
backup uses a newer schema.

The survival inventory has a bounded preset panel with collapse, search,
scrolling, active highlight, create/import controls, selection, and a right-click
menu for view, edit, export, and confirmed deletion. Position can be Automatic,
Left, or Right. The panel hides instead of overlapping the recipe book or status
effects.

Preset JSON export/import uses the system clipboard. Import accepts at most
1 MiB, validates a fixed field set and the complete ItemStack payload before any
repository write, and never executes code or accepts a destination path.

### Preset Helper

`/dot pst hlp` opens the active preset in read-only ISM mode. Missing virtual
slots have an orange border, and the Materials action opens exact
item-and-component counts for the frozen player inventory plus any container
that was visible when the helper opened. `/dot pst hlp <name>` inspects a
specific preset. The same action is available in the preset context menu and as
an unbound, configurable keybind.

The material view reports required, available, missing, complete, and overall
progress values. It supports name/ID search and All, Missing, Complete, and
Craftable filters. Client-known recipe options show output count, crafts needed,
crafts possible from the snapshot, immediate ingredient deficits, and a bounded
cycle-safe dependency tree. The helper is informational: it never fills a grid,
moves an item, requests a recipe craft, or changes the real inventory.

### Inventory Search

Every handled inventory can show a compact dotMOD search field. A valid query
visually dims or covers nonmatching stacks while leaving every slot, tooltip,
click, drag, and server-synchronized stack unchanged. Invalid syntax is
fail-open: filtering stops and the field tooltip reports the error position.

Supported fields are `text`, `id`, `lore`, `enchantment`, `durability`, `count`,
and `all-text`. Bare text is shorthand for `all-text:`. String fields support
`:`, `=`, and `!=`; numeric fields additionally support `<`, `<=`, `>`, and
`>=`. Combine up to 16 filters with `&`:

```text
diamond sword
id=minecraft:diamond_sword
enchantment:sharpness & durability<=25%
lore:"Quest item" & count>=2
```

Durability is remaining percent and never matches non-damageable items. Search
uses localized names, explicit lore, normal and stored enchantments, item IDs,
and bounded basic tooltip text. The `?` button exposes the syntax as a nonmodal
tooltip. Search is hidden while a narrow recipe book owns the screen because
vanilla does not render container slots in that state.

### Commands, Aliases, and Recolor

`/dot commands` opens a compact local list of pinned and recent commands. Both
collections are bounded to 100 entries. A row
only fills the editable command field; commands run only from **Execute** or
Enter. Pinning, unpinning, clearing recent history, and confirmation for common
administrative commands are explicit actions. The equivalent keybind is unbound
by default. Sensitive authentication-style commands are excluded from history.

Aliases are managed with `/dot alias`. Templates accept `$1` through `$9`, `$*`,
and `$$`; ordinary extra arguments are appended when a template has no
placeholder. Expansion is bounded and cycle-safe. Alias roots are rejected when
they conflict with `dot`, `dotmod`, or an active client/server command. Fabric's
outgoing command events modify one send in place, never cancel and resend it.

`/dot recolor` lists local UUID colors and can set, reset, or open a compact
picker for an exact tab-list profile. HEX input is strictly `#RRGGBB` or
`RRGGBB`. Picker changes remain a draft until **Apply**; names are stored only as
last-known display metadata and never replace UUID identity.

### Death History and Screenshots

When enabled, dotMOD records one local entry per death before requesting any
screenshot work. Each record includes the plain death message, player and
dimension identity, precise and block coordinates, selected hotbar slot, XP,
active effects, best-effort recent damage information, and a frozen 41-slot
inventory snapshot.

Death screenshots are captured after a later rendered frame and encoded off the
render thread to `config/dotmod/deaths/images/<uuid>.png`. A failed capture is
recorded without deleting the death entry. `/dot deaths` opens the history
screen; commands also provide details, inventory, built-in image viewing, path
copying, desktop opening, confirmed deletion, and confirmed clearing. Desktop
processes use direct argument arrays without a shell. Ordinary vanilla F2
screenshots are unchanged; Screenshot+ currently applies only to death captures.

### Configuration

The configuration screen is available through Mod Menu or `/dot config`.
Current categories are:

- General;
- Commands;
- Quick Craft;
- Inventory Presets;
- Inventory Search;
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
config/dotmod/command-aliases.json
config/dotmod/command-history.json
config/dotmod/deaths/<uuid>.json
config/dotmod/deaths/images/<uuid>.png
```

Each subsystem has its own path under `config/dotmod/`. Writes use a temporary
file, preserve the previous version as `.bak`, and use atomic replacement where
supported. If JSON cannot be read, dotMOD preserves it as `.broken`, restores a
valid `.bak` when available, otherwise restores safe defaults, and continues
startup.

The previous flat `config/dotmod.json` format is migrated automatically. The
original is retained as `config/dotmod.json.migrated.bak` before the new files
are written. `/dot reload` reloads configuration, player colors, aliases, and
command history without restarting Minecraft.

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
| `/dot pst lst` | List presets and active state |
| `/dot pst slc <name>` | Select the active preset |
| `/dot pst crt <name>` | Create a preset through creative ISM |
| `/dot pst dlt <name>` | Delete after confirmation |
| `/dot pst shw <name>` | Open read-only ISM view |
| `/dot pst hlp [name]` | Compare active or named preset materials |
| `/dot pst ren <old> <new>` | Rename a preset |
| `/dot pst dup <source> <new>` | Duplicate a preset |
| `/dot pst exp <name>` | Copy safe preset JSON |
| `/dot pst imp` | Import preset JSON from clipboard |
| `/dot preset list` | Long-form preset list command |
| `/dot preset select <name>` | Select and best-effort arrange a preset |
| `/dot preset create <name>` | Create from the current inventory snapshot |
| `/dot preset delete <name>` | Delete after confirmation |
| `/dot preset show <name>` | Open read-only ISM view |
| `/dot preset helper [name]` | Open the material helper |
| `/dot preset rename <old> <new>` | Rename a preset |
| `/dot preset duplicate <source> <new>` | Duplicate a preset |
| `/dot preset export <name>` | Copy safe preset JSON |
| `/dot preset import` | Import preset JSON from clipboard |
| `/dot alias list` | List aliases and enabled state |
| `/dot alias set <name> <template...>` | Create or update an alias |
| `/dot alias remove\|enable\|disable <name>` | Mutate an existing alias |
| `/dot commands` | Open pinned and recent commands |
| `/dot recolor list` | List UUID-backed player colors |
| `/dot recolor set <player> <hex>` | Color an exact tab-list profile |
| `/dot recolor reset\|pick <player>` | Reset a color or open its picker |
| `/dot deaths [list]` | Open Death History |
| `/dot deaths show\|inventory\|view\|copy\|open <id-prefix>` | Act on a death record |
| `/dot deaths delete <id-prefix>` | Delete a death record after confirmation |
| `/dot deaths clear` | Clear death history after confirmation |
| `/dot reload` | Reload configuration and local service data |
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
- The mod wraps vanilla Fabric HUD layers and uses version-specific player-label
  mixins. Other mods that replace the same layers or label paths can conflict.
- HUD editor rectangles approximate dynamic vanilla elements such as multi-row
  hearts, multiple boss bars, and variable-size scoreboards.
- Fractional opacity applies to custom widget panels, bars, and text. Minecraft
  item icons and wrapped vanilla HUD layers remain opaque; opacity zero hides a
  wrapped vanilla layer.
- The experience-bar placement wraps Minecraft's complete info-bar layer, which
  can also contain mount-related bars.
- Player colors depend on client-known entities and tab-list data.
- ISM intentionally models the requested hotbar, main inventory, armor, and
  offhand slots (`0..40`). Minecraft 1.21.11's special body/saddle slots are not
  part of the current ISM schema.
- The creative ISM catalog contains locally registered default item variants.
  A draft may therefore contain an item unavailable on the current server, but
  ISM never attempts to grant or insert it into the real inventory.
- A preset whose registry data is unavailable in the current world remains
  untouched on disk and reserves its UUID/name, but cannot be opened until the
  required registry data is available again.
- Preset Helper recipes are limited to recipe displays known to the current
  client recipe book. Locked, server-only, or non-declarative special recipes
  may be absent or marked as having unknown ingredients.
- Visible container contents are a frozen client-side snapshot. Player-backed
  duplicate slots, the real cursor stack, and computed crafting-result slots are
  excluded.
- Alias conflict checks use the active Fabric client dispatcher and the current
  server command dispatcher. Commands hidden from the client by a server cannot
  be detected until that server exposes them.
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
component serialization, draft recovery/write protection, layout, catalog,
preset CRUD/import, exact material comparison, recipe allocation, cyclic
dependency termination, alias expansion/persistence, command history policy,
strict HEX parsing, and player-profile lookup.

## License

dotMOD is available under the [GNU General Public License v3.0](LICENSE).

Copyright (c) 2026 DinoMiHa.
