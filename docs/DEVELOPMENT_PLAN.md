# dotMOD Development Plan

This document records the Stage 0 audit and the implementation boundaries for
Stages 1-10. It describes the current working tree, including changes that were
not committed when the audit started.

## Implementation Status

| Stage | Status |
| --- | --- |
| Stage 0 - Audit | Complete |
| Stage 1 - Foundation | Complete |
| Stage 2 - InvSeeMenu | Complete |
| Stage 3 - Inventory Presets | Complete |
| Stage 4 - Preset Helper | Complete |
| Stage 5 - HUD Widgets and Durability | Complete |
| Stage 6 - Inventory Search | Complete |
| Stages 7-10 | Not started |

The source-layout section below is the Stage 0 baseline. The current post-Stage
1 architecture is documented in [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Current Baseline

### Toolchain

| Component | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.140.0+1.21.11 |
| Yarn mappings | 1.21.11+build.6 |
| Java | 21 |
| Gradle wrapper | 9.6.0 |
| Fabric Loom | 1.17.12 |
| Cloth Config | 21.11.153 |
| Mod Menu | 17.0.0-beta.1 |

The project is Java-only and client-only. Minecraft is pinned to exactly
1.21.11 because the current rendering mixins target version-specific methods
and internal render command classes.

### Current Source Layout

```text
src/client/java/com/dinomiha/dotmod/
  DotModClient.java
  config/
    DotModConfig.java
    DotModConfigScreen.java
    DotModModMenu.java
  gui/
    HudEditorScreen.java
    InventoryButtons.java
    QuickCraft.java
  hud/
    HudElement.java
    HudLayout.java
    HudTransform.java
  keybind/
    DotModKeybinds.java
  mixin/
    ExperienceBarMixin.java
    HandledScreenAccessor.java
    InGameHudMixin.java
    LabelCommandRendererCommandsMixin.java
    PlayerEntityRendererMixin.java
    PlayerListHudMixin.java
  util/
    ColorUtil.java
    NameColorManager.java
    SlotListParser.java
    UniformNameTagRenderer.java

src/main/resources/
  fabric.mod.json
  dotmod.client.mixins.json
  assets/dotmod/icon.png
  assets/dotmod/lang/en_us.json
  assets/dotmod/lang/ru_ru.json
```

There is no command subsystem, README, automated test source set, CI workflow,
release automation, or persisted-data schema version.

### Current Features

- Quick Craft fills the vanilla 2x2 or 3x3 crafting grid with configured
  inventory slots by issuing ordinary client inventory clicks.
- HUD Editor moves selected vanilla HUD elements, including the experience bar
  and level, and supports grid and magnetic snapping.
- Player Colors assigns green, red, or reset actions to the targeted player and
  stores colors by UUID.
- Uniform Name Tags applies screen-space scaling and a configurable opaque
  background to visible player name tags.
- Toggle Shift controls the vanilla sneak key binding and restores its state
  after a GUI resets key bindings.
- Cloth Config and Mod Menu expose the current settings.

### Current Entrypoints

- Fabric client: `com.dinomiha.dotmod.DotModClient`
- Mod Menu: `com.dinomiha.dotmod.config.DotModModMenu`

`DotModClient` loads config, registers key bindings, and attaches handled-screen
buttons. There is no server or common entrypoint.

### Current Mixins

| Mixin | Target | Purpose |
| --- | --- | --- |
| `HandledScreenAccessor` | `HandledScreen` | Read GUI position and width |
| `InGameHudMixin` | `InGameHud` | Move vanilla HUD sections |
| `ExperienceBarMixin` | `ExperienceBar` | Move the experience bar |
| `PlayerEntityRendererMixin` | `PlayerEntityRenderer` | Color and restyle name tags |
| `PlayerListHudMixin` | `PlayerListHud` | Color tab-list names |
| `LabelCommandRendererCommandsMixin` | `LabelCommandRenderer$Commands` | Scale labels and change background color |

All mixins are required and use `defaultRequire: 1`. The label command mixin is
the most version-sensitive because it targets an internal nested class and an
exact constructor descriptor.

## Audit Findings

### Critical

1. `DotModConfig.load()` catches only `IOException`. Malformed JSON and invalid
   enum or UUID values can crash startup.
2. Config writes truncate the destination directly. There is no temporary file,
   atomic move, backup, schema version, or migration transaction.
3. The working tree contains required untracked mixin and renderer classes. A
   partial commit would produce a source tree that cannot compile or start.
4. Quick Craft does not reject a non-empty cursor stack and has no validated
   click plan or recovery path.
5. Required version-specific mixins can stop client startup after a Minecraft
   update or a conflicting rendering mod.

### High Priority

1. All settings and UUID color data share one mutable singleton and one JSON
   file. This is not suitable for presets, deaths, aliases, or history.
2. Config and storage errors are silently ignored.
3. There are no unit tests, client smoke tests, or CI checks.
4. Most user-visible text is hardcoded English.
5. Runtime state, such as active toggle modes, is mixed with long-lived user
   preferences.
6. Key binding registration, action dispatch, notifications, and movement state
   are coupled in one class.
7. HUD placement uses pixel offsets without screen-edge constraints or anchors.
8. Reinitializing a handled screen can register additional render callbacks that
   retain obsolete button instances.

### Medium Priority

1. The config screen displays default key names instead of actual bindings.
2. HUD hit boxes approximate dynamic vanilla elements.
3. The visible grid spacing differs from the actual grid snap spacing.
4. Player targeting can select a player behind a block in its fallback raycast.
5. Color parsing accepts malformed and ambiguous values.
6. Name-tag rendering depends on synchronous use of a `ThreadLocal` render
   context.
7. Mod Menu is a hard dependency even though it is only an integration entry.
8. Gradle uses deprecated behavior that will be incompatible with Gradle 10.

## Architecture Direction

The project will evolve incrementally. Existing working features must remain in
place until their owning stage has tests and a replacement. Mixins stay thin:
they collect Minecraft state, call a service, and apply a result. Domain logic,
serialization, parsing, and validation must not live in mixins or screens.

### Composition Root

```text
com.dinomiha.dotmod.DotModClient
com.dinomiha.dotmod.core.DotModContext
com.dinomiha.dotmod.core.ServiceRegistry
```

`DotModClient` remains the Fabric entrypoint. `DotModContext` owns initialized
services and gives mixin bridges a stable access point. This avoids a rewrite
while removing direct dependencies on a growing set of static singletons.

### Storage

```text
config/dotmod/config.json
config/dotmod/presets/
config/dotmod/player-colors.json
config/dotmod/command-aliases.json
config/dotmod/command-history.json
config/dotmod/deaths/
```

Planned shared classes:

```text
storage/AtomicJsonStore.java
storage/JsonStoreException.java
storage/StoragePaths.java
storage/BackupService.java
config/ConfigService.java
config/ConfigMigrator.java
config/ConfigValidator.java
config/model/DotModConfig.java
```

Every write uses a same-directory temporary file, flush, atomic move where the
platform supports it, and a safe fallback move. Migrations create a backup
before changing data. Corrupt files are quarantined and replaced with defaults
without deleting the recoverable source.

The old `config/dotmod.json` is persisted user data and therefore requires a
one-time migration. Compatibility code is limited to that concrete migration.

### Messages and Localization

```text
message/MessageService.java
message/MessagePrefix.java
message/MessageType.java
message/InteractiveMessageBuilder.java
```

All features use `MessageService`; they never embed `dotMod:` themselves.
Message text and GUI labels use translation keys in both `en_us.json` and
`ru_ru.json`.

### Commands

```text
command/DotModCommands.java
command/CommandContext.java
command/argument/QuotedStringReader.java
command/suggestion/DotSuggestionProviders.java
```

`/dot` and `/dotmod` register the same client-side command tree. They do not send
custom packets and are not registered on the server. Parsing uses Brigadier and
feature-specific argument types rather than manual string splitting.

### Reusable UI

The UI layer composes vanilla widgets instead of replacing Minecraft rendering.
Each component must be functional when introduced; unused placeholder widgets
are not added.

```text
ui/component/DotButton.java
ui/component/DotIconButton.java
ui/component/DotTextField.java
ui/component/DotList.java
ui/component/DotScrollPanel.java
ui/component/DotTabs.java
ui/component/DotTooltip.java
ui/component/DotColorPicker.java
ui/component/DotContextMenu.java
ui/component/DotConfirmationDialog.java
ui/layout/ScreenBounds.java
ui/theme/DotTheme.java
```

### Feature Packages

```text
feature/preset/
feature/invsee/
feature/inventorysearch/
feature/durability/
feature/death/
feature/screenshot/
feature/togglewalk/
feature/freelook/
feature/playercolor/
feature/commandalias/
feature/commandlist/
hud/widget/
```

## Stage Plan

### Stage 1 - Foundation

Goals:

- Introduce schema-versioned config categories requested in the specification.
- Migrate the legacy config without losing current settings.
- Add atomic JSON storage and backups.
- Add `MessageService` with built-in and custom prefixes.
- Add the reusable UI components as they are consumed by the new config screen.
- Register synonymous `/dot` and `/dotmod` roots with help, reload, and config
  commands.
- Move every touched user string into both language files.
- Add JUnit and initial migration/storage tests.

Planned files:

```text
core/DotModContext.java
config/ConfigService.java
config/ConfigMigrator.java
config/ConfigValidator.java
config/model/DotModConfig.java
config/model/GeneralConfig.java
config/model/CommandsConfig.java
config/model/HudConfig.java
config/model/InventoryPresetsConfig.java
config/model/InventorySearchConfig.java
config/model/DurabilityConfig.java
config/model/ScreenshotsConfig.java
config/model/DeathHistoryConfig.java
config/model/ToggleWalkConfig.java
config/model/FreelookConfig.java
config/model/PlayerColorsConfig.java
config/model/CommandAliasesConfig.java
config/model/KeybindsConfig.java
config/model/InterfaceConfig.java
storage/AtomicJsonStore.java
storage/StoragePaths.java
storage/BackupService.java
message/MessageService.java
message/MessagePrefix.java
command/DotModCommands.java
ui/component/*
src/test/java/com/dinomiha/dotmod/config/*
src/test/java/com/dinomiha/dotmod/storage/*
```

Exit criteria:

- Legacy config migrates with a backup.
- Corrupt config does not crash startup.
- `/dot` and `/dotmod` show identical localized help.
- Config reload is safe and reports errors through `MessageService`.
- `./gradlew test` and `./gradlew build` pass.

Suggested commit: `feat: add configuration and command foundation`

### Stage 2 - InvSeeMenu

Goals:

- Implement a local virtual inventory model independent of `ScreenHandler`.
- Implement view, edit, and creative-preset modes with explicit permissions.
- Prevent every ISM operation from mutating the real player inventory.
- Support tooltips, keyboard access, scrolling, save, cancel, validation, search,
  and local item catalog selection.

Planned files:

```text
feature/invsee/model/VirtualInventory.java
feature/invsee/model/VirtualSlot.java
feature/invsee/model/VirtualInventorySnapshot.java
feature/invsee/InvSeeMode.java
feature/invsee/InvSeeSession.java
feature/invsee/VirtualInventorySerializer.java
feature/invsee/VirtualInventoryValidator.java
feature/invsee/screen/InvSeeMenu.java
feature/invsee/screen/InvSeeInputController.java
feature/invsee/screen/VirtualSlotWidget.java
feature/invsee/catalog/LocalItemCatalog.java
src/test/java/com/dinomiha/dotmod/feature/invsee/*
```

Exit criteria:

- View mode cannot mutate its model.
- Edit mode supports save and rollback.
- Creative mode copies stacks into local state without touching real slots.
- Serialization round-trips valid component-bearing item stacks.

Suggested commit: `feat: add local virtual inventory screen`

### Stage 3 - Inventory Presets

Goals:

- Add versioned preset CRUD, import, and export.
- Store UUID, timestamps, inventory, hotbar, armor, offhand, description, and
  tags.
- Add `pst` commands and confirmation for deletion.
- Add a bounded, collapsible, scrollable preset panel to the inventory screen.
- Open ISM for create, edit, and view actions.

Planned files:

```text
feature/preset/model/InventoryPreset.java
feature/preset/model/PresetId.java
feature/preset/PresetRepository.java
feature/preset/PresetService.java
feature/preset/PresetImportExportService.java
feature/preset/command/PresetCommands.java
feature/preset/screen/PresetPanel.java
feature/preset/screen/PresetContextMenu.java
feature/preset/screen/PresetPanelController.java
mixin/InventoryScreenMixin.java or a Fabric screen-event adapter
src/test/java/com/dinomiha/dotmod/feature/preset/*
```

Exit criteria:

- Names are validated case-insensitively for conflicts.
- Import never executes code and never writes outside the preset directory.
- Panel stays inside the screen for supported GUI scales.

Suggested commit: `feat: add inventory preset manager`

### Stage 4 - Preset Helper

Goals:

- Compare preset requirements with player and visible container contents.
- Calculate required, available, missing, complete, and progress values.
- Add list filters and the `pst hlp` key binding.
- Inspect client-known recipes without automatic crafting.
- Build cycle-safe recipe dependency trees.

Planned files:

```text
feature/preset/helper/InventoryCounter.java
feature/preset/helper/PresetComparisonService.java
feature/preset/helper/PresetRequirement.java
feature/preset/helper/PresetProgress.java
feature/preset/helper/RequirementFilter.java
feature/preset/helper/RecipeAvailabilityService.java
feature/preset/helper/RecipeDependencyTree.java
feature/preset/helper/RecipeCycleGuard.java
feature/preset/screen/PresetHelperPanel.java
src/test/java/com/dinomiha/dotmod/feature/preset/helper/*
```

Exit criteria:

- Comparison tests cover player-only and player-plus-container input.
- Recipe recursion terminates for cyclic recipe graphs.
- No click or craft packet is sent by the helper.

Suggested commit: `feat: add preset material helper`

### Stage 5 - HUD Widgets and Durability

Goals:

- Generalize HUD Editor placements to include scale, alpha, visibility, anchor,
  edge snapping, preview, reset, and right-click context menus.
- Add Armor, Colored Online, and Durability widgets.
- Add a centralized durability service with interpolated colors and warning
  cooldowns.
- Use only players present in the client tab list.

Implementation note: Minecraft 1.21.11 exposes no safe global fractional-alpha
state for an already-rendering vanilla HUD layer. Fractional alpha therefore
applies to custom widget surfaces/text; vanilla wrappers support placement,
scale, visibility, anchors, snapping, preview, and reset. Alpha zero is treated
as hidden when supplied programmatically.

Planned files:

```text
hud/widget/HudWidget.java
hud/widget/HudWidgetRegistry.java
hud/widget/HudWidgetSettings.java
hud/widget/HudWidgetRenderer.java
hud/widget/ArmorWidget.java
hud/widget/ColoredOnlineWidget.java
hud/widget/DurabilityWidget.java
hud/editor/HudEditorController.java
hud/editor/HudContextMenu.java
feature/durability/DurabilityService.java
feature/durability/DurabilityReading.java
feature/durability/DurabilityColorInterpolator.java
feature/durability/DurabilityWarningService.java
src/test/java/com/dinomiha/dotmod/feature/durability/*
```

Exit criteria:

- Widgets remain within screen bounds at supported scales.
- Color interpolation tests cover boundaries and intermediate values.
- Warning cooldown survives normal tick variation without render-tick IO.

Suggested commit: `feat: add configurable HUD widgets`

### Stage 6 - Inventory Search

Goals:

- Implement tokenizer, parser, AST, validation, and evaluator.
- Support text, durability, enchantment, lore, item ID, count, all-text, and
  logical AND filters.
- Integrate a compact search field and help button with handled screens.
- Dim or visually hide non-matches without changing slot layout or inventory.

Implemented query contract:

```text
diamond sword
text:"Diamond Sword"
id=minecraft:diamond_sword
enchantment:sharpness & durability<=25%
lore:"Quest item" & count>=2
```

Bare text means `all-text:`. String fields accept `:`, `=`, and `!=`; numeric
`count` and remaining-percent `durability` also accept `<`, `<=`, `>`, and `>=`.
Only explicit `&` is logical AND. Invalid syntax is fail-open and reports a
typed source position.

Planned files:

```text
feature/inventorysearch/query/QueryTokenizer.java
feature/inventorysearch/query/QueryParser.java
feature/inventorysearch/query/QueryNode.java
feature/inventorysearch/query/AndNode.java
feature/inventorysearch/query/FilterNode.java
feature/inventorysearch/query/ComparisonOperator.java
feature/inventorysearch/query/QueryParseException.java
feature/inventorysearch/InventorySearchEvaluator.java
feature/inventorysearch/ItemSearchDocument.java
feature/inventorysearch/screen/InventorySearchController.java
feature/inventorysearch/screen/SearchHelpDialog.java
mixin/HandledScreenSearchMixin.java
src/test/java/com/dinomiha/dotmod/feature/inventorysearch/query/*
```

Exit criteria:

- Parser tests cover all documented examples and malformed input.
- Search never suppresses slot input or changes the server inventory.
- Query explanation tooltip identifies parsed filters and errors.

Suggested commit: `feat: add parsed inventory search`

### Stage 7 - Commands, History, Aliases, and Recolor

Status: **Complete**

Goals:

- Add Fast Command List and recent-command history.
- Add safe client-side aliases with arguments and placeholders.
- Prevent recursion, cycles, real-command conflicts, dotMOD conflicts, and
  duplicate sends.
- Add `/dot recolor` commands and a compact color picker.
- Persist aliases, history, pinned entries, and last-known player names in
  separate stores.

Implemented files:

```text
feature/commandlist/CommandEntry.java
feature/commandlist/CommandHistoryService.java
feature/commandlist/SensitiveCommandFilter.java
feature/commandlist/screen/FastCommandListScreen.java
feature/commandalias/CommandAlias.java
feature/commandalias/AliasRepository.java
feature/commandalias/AliasExpander.java
feature/commandalias/AliasCycleDetector.java
feature/commandalias/OutgoingCommandInterceptor.java
feature/commandalias/CommandClientService.java
feature/commandalias/AliasCommands.java
config/PlayerColorService.java
feature/playercolor/PlayerLookup.java
feature/playercolor/StrictHexColor.java
feature/playercolor/RecolorCommands.java
feature/playercolor/screen/RecolorPickerScreen.java
ui/component/DotColorPicker.java
src/test/java/com/dinomiha/dotmod/feature/commandalias/*
src/test/java/com/dinomiha/dotmod/feature/commandlist/*
src/test/java/com/dinomiha/dotmod/feature/playercolor/*
```

Exit criteria:

- Alias expansion and cycle tests pass.
- A command is sent only after explicit user action and at most once.
- Sensitive patterns prevent selected history entries from being persisted.
- Colors remain keyed by UUID with last-known names as metadata only.

Actual behavior:

- One singleton owns pretty-printed alias and command-history stores; reload
  includes both, and recent and pinned history are each capped at 100.
- Fabric allow/modify/accepted/canceled events share one pending expansion plan.
  Typed expansion failures are blocked with localized feedback; accepted final
  commands are recorded once without cancel-and-resend.
- Alias mutations and sends reject reserved and currently active dispatcher
  roots. Alias templates use Brigadier `greedyString`.
- Fast Command List selection only edits its field. Execute/Enter is the sole
  send path, with confirmation for dangerous roots.
- Recolor resolves tab-list profiles by UUID, accepts only six-digit RGB, and
  keeps picker changes local until Apply.

Suggested commit: `feat: add command tools and aliases`

### Stage 8 - Death History and Screenshot+

Status: **Implemented**. Death History and deferred death screenshots meet the
exit criteria. Ordinary F2 remains vanilla; Screenshot+ enhancement is currently
limited to death captures.

Goals:

- Capture death metadata and a local inventory/effects snapshot.
- Queue screenshot creation for a later client/render tick without blocking.
- Add interactive death messages and death-history commands.
- Add Screenshot+ messages and a safe cross-platform service.
- Provide image/path copy fallback and a built-in image viewer.

Implemented components:

```text
feature/death/model/DeathRecord.java
feature/death/model/DeathSnapshot.java
feature/death/DeathClientService.java
feature/death/DeathRepository.java
feature/death/DeathScreenshotQueue.java
feature/death/command/DeathCommands.java
feature/death/screen/DeathHistoryScreen.java
feature/screenshot/ClientIoExecutor.java
feature/screenshot/ScreenshotResult.java
feature/screenshot/DesktopPlatformService.java
feature/screenshot/screen/ImageViewerScreen.java
mixin/ClientPlayNetworkHandlerDeathMixin.java
mixin/MinecraftClientRenderMixin.java
src/test/java/com/dinomiha/dotmod/feature/death/*
```

Exit criteria:

- Screenshot failure does not discard the death record.
- Paths are normalized and constrained to configured directories.
- Platform processes use argument arrays, never concatenated shell strings.
- Clear and delete operations require confirmation.

Suggested commit: `feat: add death history and screenshot tools`

### Stage 9 - Toggle Walk and Freelook

Goals:

- Add a tested movement state controller for Toggle Walk and sprint retention.
- Disable movement toggles on configured GUI types, death, disconnect, and world
  changes.
- Add an emergency key binding that releases every forced input.
- Add hold/toggle Freelook without changing server-facing player rotation solely
  because the camera moved.
- Add camera return behavior, sensitivity, inversion, perspective policies, and
  HUD indicators.

Planned files:

```text
feature/togglewalk/ToggleWalkController.java
feature/togglewalk/ForcedKeyState.java
feature/togglewalk/MovementLifecycle.java
feature/freelook/FreelookController.java
feature/freelook/FreelookCameraState.java
feature/freelook/CameraReturnAnimation.java
input/EmergencyToggleController.java
hud/widget/ToggleWalkIndicator.java
hud/widget/FreelookIndicator.java
mixin/CameraMixin.java
mixin/MouseMixin.java
mixin/ClientPlayerEntityMixin.java only if a narrower adapter is insufficient
src/test/java/com/dinomiha/dotmod/feature/togglewalk/*
src/test/java/com/dinomiha/dotmod/feature/freelook/*
```

Exit criteria:

- Rebound movement keys work through `KeyBinding`, not fixed GLFW codes.
- Emergency release restores all forced keys.
- Opening configured screens, death, and disconnect cannot leave movement stuck.
- Freelook pitch is clamped and normal player rotation resumes correctly.

Suggested commit: `feat: add toggle walk and freelook`

### Stage 10 - Completion

Goals:

- Run regression, compatibility, localization, migration, and performance checks.
- Remove duplication discovered during feature integration.
- Finish README and architecture/testing documentation.
- Add CI and release checks if not already introduced in Stage 1.
- Verify no render-tick storage writes and no unsafe server interaction.

Planned files:

```text
README.md
docs/ARCHITECTURE.md
docs/TESTING.md
.github/workflows/build.yml
src/test/java/com/dinomiha/dotmod/*
```

Exit criteria:

- `./gradlew clean test build` passes.
- Translation key parity passes for English and Russian.
- Legacy and current config migration tests pass.
- The manual checklist is completed on each actually available platform and
  server type; untested combinations are documented rather than promised.

Suggested commit: `docs: finalize dotMOD release documentation`

## Verification Policy

After every stage:

1. Run focused unit tests.
2. Run `./gradlew clean build`.
3. Inspect `git diff --check`, `git status`, and the full intended diff.
4. Perform the stage-specific in-game checklist when a graphical client is
   available.
5. Commit only files owned by that stage with a concise message.
6. Report unverified platform or server combinations explicitly.

## Manual Test Matrix

The detailed checklist will live in `docs/TESTING.md` by Stage 10. At minimum it
will cover:

- ISM modes and real-inventory isolation;
- HUD movement, snapping, scaling, alpha, context menus, and resolution changes;
- preset CRUD, import/export, helper counts, and recipes;
- inventory search syntax and handled-screen interactions;
- commands, suggestions, quoting, aliases, recursion protection, and key binds;
- death snapshots, deferred screenshots, failed screenshots, and image actions;
- Screenshot+ on supported desktop environments;
- Toggle Shift, Toggle Walk, emergency release, GUI transitions, death, and
  disconnect;
- Freelook in first/third person and return behavior;
- singleplayer, a vanilla-compatible Fabric server, Paper/Spigot, and Realms
  where accounts and test environments are actually available;
- GUI scale, windowed/fullscreen, English/Russian, Linux/Windows/macOS where
  those platforms are actually available.

## Stage 0 Build Result

The audited working tree completed:

```text
./gradlew clean build
BUILD SUCCESSFUL
```

There are no test sources, so Gradle reported `test NO-SOURCE`. Existing Gradle
deprecation warnings remain and must be addressed before Gradle 10.
