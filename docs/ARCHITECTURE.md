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
  hud/          Client-independent widget settings, placement, and snapping

src/client/java/
  command/      Fabric client command registration
  config/       Runtime ConfigService, player-color service, config screen
  feature/      ISM screen, local catalog, launcher, and player snapshot source
  gui/          Inventory buttons, Quick Craft, and HUD editor
  hud/          Fabric HUD layer wrappers, widgets, renderer, and editor control
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
2. `DeathClientService` initializes after config and acquires the active registry
   manager lazily, replacing its repository when the connection changes.
3. `PlayerColorService` loads UUID color data according to the persistence
   setting and subscribes to config saves.
4. `CommandClientService` loads aliases and 100-entry command history, then the
   outgoing command interceptor registers.
5. Vanilla HUD wrappers, custom widgets, and durability tick warnings register.
6. `/dot` and `/dotmod` client commands are registered.
7. Keybinds and handled-screen button hooks are registered.

This order prevents mixins, keybinds, and commands from observing an
uninitialized configuration.

## Configuration

`DotModConfig` has schema version `7` and these top-level categories:

```text
general, commands, hud, quickCraft, inventoryPresets, inventorySearch,
durability, screenshots, deathHistory, toggleWalk, freelook, playerColors,
commandAliases, keybinds, interface
```

`quickCraft` remains explicit because it is an existing independent feature.
The Commands category exposes the existing `commandAliases.enabled` global
switch; command history has no additional schema fields.

`ConfigValidator` restores missing categories and fields, deduplicates and
checks inventory slots, clamps numeric values, validates RGB colors, restores
all registered HUD settings, and advances the schema version. Schema v3 HUD
delta offsets migrate to stable ID-keyed anchor placements without deleting
unknown add-on widget records.

Schema 7 replaces the generic Freelook placeholder with bounded activation,
perspective, sensitivity, inversion, return, and indicator settings. It enables
Toggle Walk and Freelook during migration while preserving explicit schema-7
feature choices. Movement and camera active bits are never serialized, and the
legacy Toggle Shift active bit is ignored.

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

Aliases and command history have independent versioned documents. History keeps
at most 100 recent and 100 pinned entries, deduplicates by normalized
slash-prefixed command, and excludes sensitive roots. Last-known player names
are optional metadata in the UUID-keyed player-color document.

## Death Capture And Images

The `ClientPlayNetworkHandler.onDeathMessage` mixin injects immediately after
vanilla's main-thread handoff and accepts only the local packet entity. The
service deduplicates until the player is alive, replaced, or disconnected. It
copies packet text, identity, world position, XP, 41 inventory slots, bounded
effects, and best-effort recent damage data, then creates the record before
screenshot work can begin.

`DeathScreenshotQueue` stores the record UUID plus expected network and world
identity. The `MinecraftClient.render` mixin runs immediately after
`GameRenderer.render`, starts at most one capture per frame, and hands the
`NativeImage` to the client IO executor. PNG writing is constrained to
`deaths/images/<uuid>.png`; the image is always closed and the saved/failed
repository update returns to the client executor. Records survive capture
failures.

Image viewing loads and validates a non-symlink PNG off-thread, registers its
texture on the client thread, aspect-fits it, and destroys the texture on close.
Desktop open uses `PlatformCommandFactory` and `ProcessBuilder(List<String>)`
without a shell or client-thread wait. Vanilla F2 capture is not mixed into.

## HUD Widgets And Durability

The pure layout chain is:

```text
HudWidgetDefaults -> HudWidgetSettings -> HudPlacementResolver -> HudPlacement
                                      -> HudSnapper
```

Settings contain visibility, nine-point screen anchor, X/Y offset, scale, and
alpha. Resolution happens in scaled GUI pixels, uses scaled bounds, and always
clamps to the current viewport. The editor and runtime renderer share this
resolver. Dragging applies grid snap, widget/screen-edge magnetic snap, clamp,
then converts the absolute result back to anchor-relative offsets.

`VanillaHudWidgetRenderer` replaces Fabric vanilla HUD elements with
exception-safe wrappers. Matrix transforms are restored in `finally`; the old
paired HEAD/RETURN HUD mixins are no longer used. Fixed editor bounds remain
approximations for dynamic vanilla content. Vanilla fractional alpha is not
exposed because Minecraft 1.21.11 has no safe global DrawContext alpha.

Custom widgets implement one local-coordinate `HudWidget` render contract and
register before chat through `HudElementRegistry`. Armor reads equipped armor;
Durability reads copied hand/armor stacks; Colored Online starts exclusively
from `getListedPlayerListEntries()` and resolves colors by UUID from the existing
in-memory player-color service.

`DurabilityReading`, `DurabilityColorInterpolator`, and
`DurabilityWarningService` are pure testable models. The client adapter runs
warnings on `END_CLIENT_TICK` with `System.nanoTime()`, does no render-time IO,
re-arms after repair, and limits all currently-low items before choosing one
overlay message.

## Movement And Freelook

`ToggleWalkController`, `ForcedKeyState`, and movement snapshots are pure state.
The client `MovementLifecycle` is the only owner of forward, sprint, and sneak
bindings it actually changes. It restores ordinary physical input only after a
user toggle-off and hard-releases on configured screens, focus/death/identity
boundaries, disconnect, shutdown, or the emergency key. It never calls the
global `KeyBinding.unpressAll` or mutates player sprint state.

Freelook keeps relative yaw/pitch offsets in `FreelookCameraState` and uses a
smoothstep `CameraReturnAnimation`. `MouseMixin` wraps the processed local look
call while active, and `CameraMixin` substitutes effective camera yaw/pitch for
orbit and clipping. Player rotation and packets are untouched. The client owner
hard-resets on screen, focus, cursor, player/world/handler/camera, death, and
configuration boundaries. Perspective restoration is conditional on the
perspective still being the one dotMOD selected.

Movement and Freelook indicators are ordinary registered custom HUD widgets.
They perform no IO, hide while idle, and remain visible in editor preview mode.

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

## Preset Helper

The helper core is independent of screens and handlers:

```text
VirtualInventorySnapshot -> PresetComparisonService -> PresetProgress
Client recipe displays   -> RecipeAvailabilityService
                         -> RecipeDependencyTreeBuilder
```

`ExactItemKey` removes count but preserves the item and all data components.
`InventoryCounter` aggregates copied stacks, and comparison totals cap available
amounts at each requirement so excess items cannot inflate overall progress.
Missing virtual slot indices are passed to ISM through an optional read-only
`InvSeeSupplement`; ISM remains unaware of presets and gains no mutation path.

`ClientInventorySnapshot` reads all player inventory slots and, only while a
matching handled screen is visible, non-player container slots. It excludes the
real cursor, duplicate backing slots, creative catalog slots, and computed
crafting results. The copied snapshot is frozen before ISM replaces the vanilla
screen. A non-player handler remains active but inaccessible beneath the plain
read-only screen, avoiding cursor/input return side effects. ISM restores its
parent only while both connection and handler identity still match; otherwise it
closes to a safe null screen instead of reviving a stale handled screen.

Minecraft 1.21.11 does not provide every server recipe definition to a vanilla
client. `ClientRecipeCatalog` therefore consumes only `RecipeDisplayEntry`
objects present in the current recipe book. Ingredient occurrences are retained
instead of deduplicated. A capacity-flow allocation handles overlapping
alternatives; recipes without declarative requirements and component predicates
that cannot be reconstructed from a display are marked unknown. Dependency
expansion follows the recipe selected in the detail view, shares one allocated
inventory budget across sibling branches, and uses a path-local cycle guard plus
depth and node bounds.
No helper class calls an interaction manager, slot click, recipe click, or craft
packet API.

## Inventory Search

The search core is client-independent:

```text
QueryTokenizer -> QueryParser -> QueryNode/AndNode/FilterNode
ItemSearchDocument + QueryNode -> InventorySearchEvaluator
```

Queries are bounded to 512 UTF-16 units and 16 AND clauses. Text uses Unicode
NFKC, collapsed whitespace, and `Locale.ROOT` case normalization. Bare text maps
to `all-text:`; malformed syntax returns a typed error with a source span and is
never partially evaluated. Invalid queries fail open in the UI.

`ItemSearchDocumentFactory` reads only client-known `ItemStack` data: localized
name, item ID, explicit lore, normal/stored enchantments, count, remaining
durability percent, and optionally bounded basic tooltip text. Documents cap
individual and aggregate text, cache by full stack equality, clear on language
replacement, and are built at no more than 64 new entries per frame.

`InventorySearchController` attaches through per-screen Fabric events and keeps
query state across re-init of the same screen. It hides its controls in narrow
recipe-book mode and uses a targeted input bridge for Creative Inventory, whose
native field bypasses normal child char dispatch. The help button is a nonmodal
multiline tooltip and never intercepts slot input.

`HandledScreenSearchMixin` renders DIM/HIDE rectangles immediately after
`drawSlots()` while the matrix is in container-local coordinates. It does not
change slot coordinates, enabled state, stack values, handler lists, focus,
tooltips, clicks, drag handling, or packets.

## Messages And Commands

`MessageService` is the only formatter for dotMOD chat and overlay messages. It
supports `dotMod:`, `[dotMod]`, `.`, and a validated custom prefix, plus info,
warning, error, and success styles. It also creates command click actions with
hover text.

`/dot` and `/dotmod` build the same Brigadier tree. In addition to configuration,
HUD, ISM, presets, reload, and prefix operations, Stage 7 adds alias CRUD,
 Fast Command List, UUID recolor, and Death History operations. These roots are local Fabric
commands.

`OutgoingCommandInterceptor` uses `ALLOW_COMMAND`, `MODIFY_COMMAND`, `COMMAND`,
and `COMMAND_CANCELED`. It computes an alias plan once, returns the final command
from the modify event, records the accepted event once, and clears pending state
on both outcomes. It never cancels and resends. Before expansion and alias
mutation it rejects `dot`, `dotmod`, and roots in the active Fabric client or
current server dispatcher. Expansion failures cancel the original typed command
with localized feedback.

`FastCommandListScreen` has no open/select send path. Execute and Enter call
`sendChatCommand` once without a slash; dangerous administrative roots first use
`DotConfirmationDialog`. `RecolorCommands` resolves exact case-insensitive names
only from tab-list profiles and mutates colors by UUID. `DotColorPicker` edits a
screen-local draft until Apply.

Preset creation seeds its local creative ISM session from a copied 41-slot
player snapshot. Selection uses `PresetInventoryArranger` for conservative
whole-stack swaps through vanilla `PICKUP` interactions only; it never writes a
real `Slot` or `PlayerInventory` directly and may report a partial result.

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
