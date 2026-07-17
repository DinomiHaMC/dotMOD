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
  storage/      Paths, JSON documents, backup, and atomic persistence
  hud/          Client-independent HUD element identifiers

src/client/java/
  command/      Fabric client command registration
  config/       Runtime ConfigService, player-color service, config screen
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

`DotModConfig` has schema version `2` and these top-level categories:

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
