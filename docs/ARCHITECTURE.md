# dotMOD Architecture

## Runtime Boundary

dotMOD is a client-only Fabric mod. Its only entrypoint is `DotModClient`, and
all Minecraft screens, commands, keybinds, render hooks, and mixins remain in
the client source set. It does not register a server entrypoint, custom payload,
or server-required feature.

Quick Craft uses ordinary vanilla inventory click actions. Other current
features affect only rendering, local key state, local files, or client chat.

## Source Sets

```text
src/main/java/
  config/       Pure versioned models, validation, and legacy migration
  feature/      Testable ISM model, session, layout, and serialization
  storage/      Paths, JSON documents, backup, and atomic persistence
  hud/          Client-independent HUD element identifiers

src/client/java/
  command/      Fabric client command registration
  config/       Runtime ConfigService, player-color service, config screen
  feature/      ISM screen, local catalog, launcher, and player snapshot source
  gui/          Inventory buttons, Quick Craft, and HUD editor
  hud/          Runtime HUD layout and transforms
  keybind/      Key registration and local toggle behavior
  message/      Centralized localized message formatting
  mixin/        Version-specific Minecraft render and screen hooks
  ui/component/ Reused functional UI adapters
  util/         Color, slot, name-color, and rendering helpers
```

Pure code is in `src/main/java` so Gradle can test it with JUnit without
starting Fabric Loader or Minecraft. Client classes may depend on the main
source set, but the main source set does not depend on client classes.

## Startup Order

`DotModClient.onInitializeClient()` performs initialization in this order:

1. `ConfigService` creates storage paths, migrates legacy data if needed, and
   loads validated config.
2. `PlayerColorService` loads UUID color data according to the persistence
   setting and subscribes to config saves.
3. `/dot` and `/dotmod` client commands are registered.
4. Keybinds and handled-screen button hooks are registered.

This order prevents mixins, keybinds, and commands from observing an
uninitialized configuration.

## Configuration

`DotModConfig` has schema version `3` and these top-level categories:

```text
general, commands, hud, quickCraft, inventoryPresets, inventorySearch,
durability, screenshots, deathHistory, toggleWalk, freelook, playerColors,
commandAliases, keybinds, interface
```

`quickCraft` remains explicit because it is an existing independent feature.
Categories for later stages contain only their global disabled feature switch;
they are not exposed as controls until the corresponding feature exists.

`ConfigValidator` restores missing categories and fields, deduplicates and
checks inventory slots, clamps numeric values, validates RGB colors, restores
all HUD offsets, and advances the schema version. Gson field initializers supply
defaults for fields added in later compatible revisions.

The legacy flat `config/dotmod.json` migration maps every existing field into
the categorized model and moves UUID colors into their own document. Migration
backs up the source before writing colors first and `config.json` last; the
config file is therefore the migration commit marker.

## Storage

`StoragePaths` reserves separate locations:

```text
config/dotmod/config.json
config/dotmod/player-colors.json
config/dotmod/presets/
config/dotmod/command-aliases.json
config/dotmod/command-history.json
config/dotmod/deaths/
```

`AtomicJsonStore` validates data before writing, serializes and flushes a
temporary file, copies the previous valid file to `.bak`, and atomically
replaces the target when the filesystem supports it. A malformed document is
moved to `.broken`; a valid `.bak` is restored before falling back to validated
defaults. IO is performed only on initialization, explicit save/reload, or user
actions, never per render tick.

Player colors are keyed by UUID. Nicknames are not used as identity keys.
Malformed serialized UUID keys are rejected individually so one bad entry does
not discard other players' colors.

## InvSeeMenu

ISM follows this dependency chain:

```text
InvSeeMenu -> InvSeeInputController -> InvSeeSession
           -> VirtualInventory -> VirtualInventorySnapshot
           -> InvSeeSaveTarget
```

`VirtualInventory` owns exactly 41 copied stacks: hotbar `0..8`, main inventory
`9..35`, armor `36..39`, and offhand `40`. It never holds `PlayerInventory`,
`ScreenHandler`, vanilla `Slot`, or the real cursor stack. All getters and
snapshots return deep copies.

`InvSeeMode` grants explicit capabilities. View mode has tooltip/copy access but
no mutation or save capability. Edit mode adds local mutation, amount changes,
rollback, and save. Creative mode additionally allows copying default stacks
from the local item registry into the ISM-owned cursor.

`InvSeeSession` is the only mutation entrypoint. Save is an explicit callback;
the screen cannot know whether Stage 3 supplies a draft or preset target. A
non-empty local cursor blocks save, and failed targets keep the session open and
dirty.

`VirtualInventorySerializer` uses `ItemStack.VALIDATED_CODEC` with the active
registry lookup. Empty slots are omitted; duplicate/out-of-range slots, unknown
items, invalid components, overstacking, and future schemas are rejected.

The Stage 2 adapter stores `invsee-draft.json` through `AtomicJsonStore`.
Structural corruption can recover from `.bak`. Registry decode failures and
future schemas block writes without quarantining or replacing the original.
The repository fingerprints the file after load and refuses to overwrite an
external change made during an open session.

`InvSeeMenu` extends plain `Screen`, not `HandledScreen`. It has no inventory
interaction manager or packet path. Player inventory is read once into a frozen
copy when the screen opens; all subsequent cursor, catalog, and slot operations
are local Java state.

## Inventory Presets

`InventoryPreset` is immutable and contains UUID, normalized name, optional
description/tags, monotonic timestamps, and a deeply copied
`VirtualInventorySnapshot`. Name conflict keys use Unicode NFKC plus
case-insensitive comparison; names never determine a path.

`PresetRepository` stores one `<uuid>.json` per preset and keeps ordering/active
UUID in `presets/index.json`. All operations use a process lock plus filesystem
lock. Preset writes are atomic and revision-checked; failed index commits roll
back create/delete filesystem changes. Orphan files are discovered and restored
to effective order. Delete moves both primary and backup to collision-free
local trash names.

Future primary or backup schemas block mutation without quarantine. Structural
corruption is moved to a unique `.broken` artifact and can recover from a valid
backup. Registry-dependent decode failure leaves the original file untouched.
Non-canonical or duplicate UUID filenames are never mutated.

`PresetImportExportService` accepts only a bounded JSON object with a fixed root
field whitelist. It reuses `VirtualInventorySerializer`, so metadata and every
component-bearing stack are validated before repository import. Clipboard is a
client adapter only; imported data cannot provide paths, class names, commands,
URLs, or executable behavior.

`PresetClientService` adapts repository records to existing ISM modes. Create
uses creative mode with an empty snapshot, view is read-only, and edit uses the
regular editor. Save callbacks capture connection and file revision, rejecting
world changes or stale external edits.

The inventory panel is attached with Fabric screen events. Mouse press/release
inside its bounded rectangle is consumed before `HandledScreen`, preventing
vanilla outside-click item drops. It hides around recipe-book/status-effect
conflicts and performs IO only on attach, refresh, or explicit user actions.

## Messages And Commands

`MessageService` is the only formatter for dotMOD chat and overlay messages. It
supports `dotMod:`, `[dotMod]`, `.`, and a validated custom prefix, plus info,
warning, error, and success styles. It also creates command click actions with
hover text.

`/dot` and `/dotmod` build the same Brigadier tree. They currently expose help,
config, HUD, reload, and message-prefix operations. They are local Fabric
commands and do not send their execution to the server.

Client-command conflicts with another client mod are logged. Fabric API does
not expose a reliable server-only command tree after joining, so a server root
with the same name cannot be detected safely without an additional packet
mixin. This limitation is documented instead of relying on unstable internals.

## UI Components

Stage 1 introduces only components that have real consumers:

- `DotButton` standardizes compact buttons and tooltips.
- `DotTooltip` applies a shared tooltip delay.
- `DotConfirmationDialog` wraps the accessible vanilla confirmation screen.
- `DotTextField` standardizes validated, narrated single-line input fields.
- `DotContextMenu` provides bounded action menus used by preset rows.

The HUD reset uses confirmation, and inventory/HUD buttons use `DotButton`.
Lists, scrolling panels, tabs, text inputs, color pickers, icon buttons, and
context menus will be added in the first stage that needs each behavior rather
than as inactive placeholder classes.

## Extension Rules

- New persistent subsystems receive a separate model and storage file.
- Data classes and algorithms that do not require Minecraft stay testable in
  `src/main/java`.
- Screens mutate local models; server inventory mutation is never implemented
  as a shortcut.
- Every user-facing string uses a translation key in `en_us` and `ru_ru`.
- New config fields require defaults and validator coverage; format-breaking
  changes require an explicit migration and backup.
