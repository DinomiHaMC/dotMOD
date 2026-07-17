# dotMOD 2.0.0

dotMOD is a compact client-side Fabric utility mod for Minecraft 1.21.11. It
adds practical inventory, HUD, player-label, and movement tools while remaining
compatible with ordinary servers: no server plugin, custom protocol, or
server-side dotMOD installation is required.

## Quick Craft

Quick Craft adds a configurable button to the survival inventory and crafting
table.

- Separate source-slot layouts for 2x2 and 3x3 crafting grids.
- Configurable logical inventory slots (`0-35`).
- Configurable button text and position.
- The buttons move with the container when the recipe book opens or closes.
- Uses only normal vanilla inventory clicks.
- Never starts while an item is held on the cursor.
- Does not replace occupied crafting slots or automatically collect the output.

Default layouts:

```text
2x2: 9,10,18,19
3x3: 9,10,11,18,19,20,27,28,29
```

## Draggable HUD Editor

Open the editor with the compact `HUD` button and move individual vanilla HUD
elements:

- hearts;
- armor;
- hunger;
- hotbar;
- experience bar;
- experience level number;
- status effects;
- boss bars;
- scoreboard.

The editor includes grid snapping, magnetic snapping, edge and center alignment,
reset-to-zero snapping, cyan alignment guides, configurable sensitivity, and a
full reset button. All positions are stored locally.

## Local Player Colors

Aim at a player and use the configurable keybinds:

| Default key | Action |
| --- | --- |
| `G` | Apply the configured green color |
| `R` | Apply the configured red color |
| `V` | Reset the color |

Assignments are stored by UUID and are displayed above player models and in the
tab list. They are entirely client-side; other players do not see your color
settings. Green, red, and fallback colors are configurable with `#RRGGBB`.

## Uniform Name Tags

Press `N` by default to toggle uniform player name tags.

- Consistent screen-space label size at different distances.
- Configurable size multiplier from `0.1` to `5.0`.
- Opaque configurable `#RRGGBB` background, black by default.
- Compatible with dotMOD player colors.
- Saved toggle state.

Vanilla visibility rules remain active: dotMOD does not reveal hidden players or
labels that Minecraft would not normally render.

## Toggle Shift

Press `Right Shift` by default to keep sneaking until the key is pressed again.

- Immediate release when disabled.
- No stand/crouch flicker when opening inventory or another GUI.
- Respects a physically held normal sneak key.
- Supports Minecraft's Hold and Toggle sneak accessibility modes.
- Saved state between restarts.

All default keys can be changed in Minecraft Controls.

## Configuration

Use Mod Menu to configure:

- global enable/disable;
- Quick Craft slots, labels, and button offsets;
- HUD button position;
- grid and magnetic snapping;
- every HUD element's X/Y offset;
- player colors and persistence;
- Uniform Name Tag size and background;
- Toggle Shift.

Configuration is stored in `config/dotmod.json`. dotMOD writes through a
temporary file. If the config becomes unreadable, it is preserved as
`dotmod.json.broken` and safe defaults are restored instead of crashing the
client.

## Requirements

- Minecraft **1.21.11**
- Java **21+**
- Fabric Loader **0.19.3+**
- Fabric API **0.140.0+1.21.11+**
- Mod Menu **17.0.0-beta.1+**
- Cloth Config **21.11.153+**

Mod Menu and Cloth Config are required for this version.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Add Fabric API, Mod Menu, and Cloth Config.
3. Put the dotMOD JAR in your `mods` folder.
4. Configure the mod through Mod Menu.

Install dotMOD only on the client. It can connect to servers that do not have
the mod installed, including normal Fabric and Paper/Spigot servers, subject to
the server's usual rules.

## Important Limitations

- This release supports Minecraft 1.21.11 only.
- HUD positions use pixel offsets and can require a reset after major GUI-scale
  or resolution changes.
- Dynamic HUD elements use approximate editor rectangles.
- Rendering mods that replace the same HUD or name-tag internals may conflict.
- Quick Craft is an inventory-layout helper, not automatic crafting.
- There are no dotMOD chat commands in version 2.0.0.
- Most configuration screens are currently displayed in English.

## Links

- Source code: <https://github.com/DinomiHaMC/dotMOD>
- Modrinth: <https://modrinth.com/mod/dotmod>
- License: GNU GPL v3.0
- Author: **DinoMiHa**
