# dotMOD Testing

## Automated Tests

Run tests without starting Minecraft:

```bash
./gradlew test
```

Current JUnit coverage:

- missing config category and field restoration;
- invalid config value validation and clamping;
- flat legacy config to the current categorized schema migration;
- failed legacy migration retry without a false commit marker;
- UUID player-color extraction during migration;
- malformed UUID isolation and future-schema rejection;
- separated storage paths;
- migration backup copying;
- JSON creation and atomic replacement;
- previous-file `.bak` preservation;
- malformed JSON `.broken` preservation, backup recovery, and safe defaults;
- ISM 41-slot mapping and deep-copy invariants;
- complete ISM mode capability boundaries;
- local cursor move/split/merge, amount, rollback, and explicit save behavior;
- component-bearing ItemStack JSON round-trip and invalid item rejection;
- ISM draft backup recovery, future-schema/registry write blocking, and
  external-change protection;
- compact/wide ISM layout, local catalog search, and `0..40` snapshot reads.
- preset name normalization and case-insensitive conflicts;
- immutable metadata, tag normalization, and monotonic timestamps;
- preset CRUD, active state, stale revisions, duplicate snapshots, and trash;
- orphan index recovery and future primary/backup write blocking;
- bounded whitelist-based preset JSON import/export with ItemStack components.

Run the complete verification and build:

```bash
./gradlew clean build
```

## Stage 1 Manual Checklist

### Configuration And Migration

- Start once without config files; verify `config.json` and
  `player-colors.json` are created under `config/dotmod/`.
- Start with a valid legacy `config/dotmod.json`; verify all existing settings
  and UUID colors migrate and `dotmod.json.migrated.bak` exists.
- Corrupt each JSON file independently; verify startup continues, `.broken` is
  preserved, defaults are restored, and an error is logged.
- Change values in Mod Menu, restart, and verify they persist.
- Add an unknown JSON field, reload, and verify known settings remain valid.

### Commands And Messages

- Join singleplayer and a normal server without dotMOD installed.
- Verify `/dot` and `/dotmod` expose identical autocomplete trees.
- Execute `help`, `config`, `hud`, `reload`, and every `prefix` variant.
- Verify `/dot prefix custom "My Mod:"` handles quotes and persists.
- Verify clickable help actions and hover text.
- Switch between English and Russian and verify command/config/HUD text.
- Verify no dotMOD custom payload is sent and server chat is not used for local
  command feedback.

### Current GUI, HUD, And Keybinds

- Test survival inventory, crafting table, and creative inventory at GUI scales
  1 through Auto in windowed and fullscreen modes.
- Verify Quick Craft and HUD buttons remain attached when the recipe book moves
  the container.
- Open HUD Editor, drag every element, test grid and magnetic snapping, confirm
  reset, close, restart, and verify offsets persist.
- Rebind each key in Controls and verify the config screen shows the real bound
  key rather than the default.
- Verify player colors remain UUID-based across reconnects and can be disabled
  from persistence.
- Verify Toggle Shift and uniform-name-tag state messages use the selected
  dotMOD prefix.

## Stage 2 Manual Checklist

### ISM Modes And Safety

- Run `/dot ism view`; verify tooltips and copy work while left/right click,
  Delete, amount changes, rollback, and save cannot mutate the snapshot.
- Run `/dot ism edit`; verify first click selects, subsequent left/right clicks
  move, split, merge, and swap only local stacks.
- Verify Delete, amount `0`, valid maximum amount, invalid text, and overstack
  input are handled without a crash.
- Verify Ctrl+C, Ctrl+S, Ctrl+Z, arrows, Enter, Tab, mouse controls, and Escape.
- Leave a local stack on the cursor; verify Save is blocked and Close asks for
  discard confirmation.
- Save an edit and verify the real player inventory and real Minecraft cursor
  remain byte-for-byte unchanged.

### Creative Catalog And Draft

- Run `/dot ism creative`; search by localized name and namespaced item ID,
  scroll the catalog, and select normal/max stacks with left/right click.
- Verify catalog stacks are copied into local state and are never granted to the
  player or sent to the server.
- Save, reopen, and verify counts and component-bearing items round-trip.
- Corrupt the primary draft and verify `.broken` plus valid `.bak` recovery.
- Open a future-schema draft and a draft with world-unavailable registry data;
  verify each remains untouched and Save fails safely.
- Resize between `320x240`, standard, fullscreen, and multiple GUI scales;
  verify compact panel switching, restored search text, wrapped status, and no
  control outside the screen.
- Disconnect or change worlds while ISM is open; verify no packet, crash, or
  unintended save occurs.

## Future Stage Checklists

The following checks become active only after their implementation stage. They
are retained here so features are not considered complete without in-game
coverage.

### Presets

Preset CRUD is implemented in Stage 3; helper-specific checks remain below for
Stage 4.

## Stage 3 Manual Checklist

### Commands And ISM Integration

- Test every `pst` command under both `/dot` and `/dotmod`, including quoted
  names, spaces, escaped quotes, invalid names, case-only conflicts, and dynamic
  suggestions.
- Create a preset, cancel ISM, and verify no file is created; repeat and Save,
  then verify UUID metadata and all 41 slots.
- View and edit presets through ISM; modify the file externally while editing
  and verify stale Save is rejected.
- Rename, duplicate, select, export, import, and confirmed delete; verify active
  state, timestamps, independent snapshots, clipboard JSON, backup, and trash.
- Import malformed, oversized, future-schema, duplicate UUID/name, unknown item,
  and unavailable-registry JSON; verify no existing preset is overwritten.

### Inventory Panel

- Test collapse, search, wheel scrolling, active highlight, create/import,
  selection, right-click view/edit/export/delete, Escape, and keyboard focus.
- Test panel sides Automatic/Left/Right at GUI scales 1 through Auto and
  `320x240`, windowed, fullscreen, recipe book open/closed, and multiple status
  effects.
- Hold a real item on the Minecraft cursor and click/drag/release every empty
  part of the panel and context menu; verify no item is dropped or moved.
- Resize with search/scroll state and name dialog text entered; verify state is
  retained and no duplicate widgets/callbacks appear.

### Preset Helper

- Test inventory panel helper counts, recipes, and cycle protection.

### Inventory Search

- Test player inventory and every available handled container.
- Test all operators, combined `&` filters, localized names, invalid syntax,
  tooltip explanation, dim/hide modes, and empty queries.

### Command Features

- Test Fast Command List with keyboard and mouse, history limits, pinned items,
  sensitive exclusions, dangerous-command confirmation, and argument prompts.
- Test aliases with quotes, additional arguments, placeholders, duplicate and
  real-command conflicts, direct and indirect cycles, and disabled aliases.
- Test recolor against tab-list players, unknown players, reset/list, picker,
  UUID identity, and invalid HEX values.

### Durability And HUD Widgets

- Test every armor/durability mode, orientation, scale, opacity, snapping,
  context menu, warning threshold, interpolation endpoints, and alert cooldown.
- Test Colored Online sorting/filtering and confirm only tab-list data appears.

### Death And Screenshot+

- Die in each dimension and after changing servers/worlds; verify coordinates,
  cause, effects, inventory, armor, offhand, screenshot, and interactive links.
- Force screenshot failure and verify the remaining death record survives.
- Test history list/show/delete/clear confirmation and missing files.
- Test image/path copy, open, show, and built-in viewing on Linux, Windows, and
  macOS when those platforms are available.

### Toggle Walk And Freelook

- Test rebound movement/sprint keys, Hold/Toggle accessibility modes, chat,
  menus, death, disconnect, emergency stop, and HUD indicators.
- Test Freelook hold/toggle, first/third person, pitch bounds, inversions,
  sensitivity, smooth return, movement direction, server-visible rotation, and
  compatibility with the current camera/render mixins.

### Server Compatibility

- Repeat relevant checks in singleplayer, a plain Fabric server without dotMOD,
  Paper/Spigot, and Realms where access is available.
- Record the exact client/server/mod versions and never claim an untested
  platform result.
