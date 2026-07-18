# dotMOD 2.0.0

dotMOD is a client-side Fabric utility mod for Minecraft 1.21.11. It requires no
server plugin, custom protocol, or server-side dotMOD installation.

## Features

- Quick Craft through ordinary vanilla inventory clicks.
- Draggable anchored HUD editor with snapping, scale, alpha, and custom widgets.
- Local UUID-based player colors and uniform name tags.
- Local InvSeeMenu drafts that never modify the real inventory.
- Inventory presets, best-effort vanilla-click rearrangement, and material helper.
- Parsed inventory search with item, lore, enchantment, count, and durability filters.
- Fast local command history, aliases, recolor tools, and `/dot` commands.
- Death History with inventory/effect snapshots and optional deferred screenshots.
- Toggle Walk/Shift with captured forward/sprint/jump combinations and safe release.
- Hold/toggle Freelook with enforced third-person orbit and exact restoration.
- Local Full Brightness toggle with Video Settings suspension and gamma restoration.

All user-facing configuration and messages are available in English and Russian.
Configuration and versioned feature data are stored below `config/dotmod/` with
atomic writes, backups, recovery, and future-schema write protection.

## Safety Boundaries

- ISM, search, HUD, helper, death history, Freelook, and Full Brightness are local-only.
- Quick Craft and preset rearrangement use normal synchronized vanilla clicks.
- Freelook does not mutate player yaw/pitch or add look packets.
- Desktop image actions use argument arrays, never shell command strings.
- No custom payload or server-required feature is registered.

## Requirements

- Minecraft **1.21.11**
- Java **21+**
- Fabric Loader **0.19.3+**
- Fabric API **0.140.0+1.21.11+**
- Mod Menu **17.0.0-beta.1+**
- Cloth Config **21.11.153+**

Install the dependencies and dotMOD JAR in the client `mods` folder, then use
Mod Menu and Minecraft Controls to configure features and unbound keybinds.

## Compatibility

dotMOD is built for Minecraft 1.21.11 only. Rendering, camera, or HUD mods that
replace the same internals may conflict. Server use remains subject to each
server's rules. Automated builds do not imply untested Windows, macOS, Realms,
Paper/Spigot, or graphics-mod compatibility.

## Links

- Source and issues: <https://github.com/DinomiHaMC/dotMOD>
- Modrinth: <https://modrinth.com/mod/dotmod>
- License: GNU GPL v3.0
- Author: **DinoMiHa**
