# Custom GUI Scales

A client-side **NeoForge** mod for **Minecraft 1.21.1** that lets you render individual HUD
elements at their *own* GUI scale, independent of the global **GUI Scale** video option.

Want your hotbar big at scale **3** but your inventory tooltips smaller at scale **2**? That's
exactly what this does.

## What you can rescale

| Setting | Config key | Default |
|---|---|---|
| Hotbar — the whole bottom HUD (hotbar, xp bar, health, hunger, armor, air, mount health/jump bar), scaled together | `hotbarScale` | **3** |
| Tooltips (inventory, etc.) | `tooltipScale` | **2** |

Each value is an **absolute** GUI scale, just like the vanilla option: `3` means "render this as
if GUI Scale were 3", no matter your global setting. **`0` = leave it at the global scale.**

## Configuring

Three ways:

- **From the settings menu:** `Options` → `Video Settings…` → **Custom GUI Scales…** (a full-width
  button at the top of the options list). The vanilla **GUI Scale** option stays right where it
  normally is. The button opens the customization section.
- **From the Mods list:** `Mods` → *Custom GUI Scales* → **Config** (opens the same section).
- **By file:** edit `config/customguiscales-client.toml` in your game directory.

In the section, each slider goes 0–8, where **0 = "Off (global)"** (that element just uses the
global GUI Scale). Edits preview live and are saved when you close the screen. The config file
additionally accepts fractional values; the in-game sliders round to whole numbers.

## Building

> [!IMPORTANT]
> The NeoForge 1.21.1 toolchain uses **Gradle 8.8**, which only runs on **Java 17–22**.
> You have Java 25 on this machine, which Gradle 8.8 will refuse to start on. Build with a
> **JDK 21** instead. A copy was already downloaded to `.toolchain/jdk-21.0.11+10` while setting
> this up — you can reuse it or install your own.

From the project root:

**PowerShell**
```powershell
$env:JAVA_HOME = "$PWD\.toolchain\jdk-21.0.11+10"
.\gradlew.bat build
```

**Git Bash**
```bash
export JAVA_HOME="$PWD/.toolchain/jdk-21.0.11+10"
./gradlew build
```

The finished mod jar lands in `build/libs/customguiscales-1.0.0.jar`. Drop it in your `mods`
folder (you also need NeoForge for 1.21.1 installed).

### Running it in a dev client

```bash
export JAVA_HOME="$PWD/.toolchain/jdk-21.0.11+10"
./gradlew runClient
```

### Version pins

`gradle.properties` pins `neo_version=21.1.93`. If that exact build is gone from the NeoForge
maven, bump it to any current `21.1.x` from <https://projects.neoforged.net/neoforged/neoforge>.
The Parchment mappings block in `build.gradle` is optional — comment it out if it won't resolve.

## How it works

- **HUD layers (hotbar, etc.)** — handled with NeoForge's `RenderGuiLayerEvent`
  (`HudScaleHandler`). Before a targeted layer draws, we push a pose scaled around the
  bottom-centre anchor (so the element grows/shrinks in place); after it draws, we pop. The scale
  factor is `desiredScale / globalGuiScale`, so the result matches an absolute GUI scale.
- **Tooltips** — there is no event that brackets the tooltip draw, so a small Mixin
  (`GuiGraphicsTooltipMixin`) wraps `GuiGraphics.renderTooltipInternal` with a scaled pose,
  anchored at the cursor.

### Known limitations

- Scaling elements **up** can push them past their neighbours or the screen edge (the game still
  computes layout at the original scale). Scaling **down** is always clean. The defaults
  (hotbar 3 / tooltip 2) assume your global scale is around 3–4.
- Health/hunger/armor/air all anchor to bottom-centre, so scaling only one of them can make it
  overlap the others. Scaling them together (or just the hotbar) looks best.

## Project layout

```
src/main/java/com/saywhat/customguiscales/
├── CustomGuiScales.java              mod entrypoint
├── Config.java                       config spec (per-element scales)
├── client/
│   ├── ClientInit.java               registers the in-game config screen
│   ├── HudScaleHandler.java          rescales HUD layers via RenderGuiLayerEvent
│   └── ScaleUtil.java                scale-factor math
└── mixin/
    └── GuiGraphicsTooltipMixin.java  rescales tooltips
src/main/resources/
├── META-INF/neoforge.mods.toml
├── customguiscales.mixins.json
├── pack.mcmeta
└── assets/customguiscales/lang/en_us.json
```

License: MIT.
