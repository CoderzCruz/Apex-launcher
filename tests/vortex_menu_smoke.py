#!/usr/bin/env python3
"""VORTEX CLIENT — VORTEX RED FOREST main menu.

The in-game title screen is a red-forest Minecraft menu: crimson VORTEX logo, red autumn
panorama, dark-red nine-slice buttons. Everything the player clicks is a real vanilla screen
(Singleplayer / Multiplayer / Realms / Mods / Options / Quit) — this test guards both the art
wiring and the "no fake buttons" rule.
"""
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app_pojavlauncher/src/main/assets/vortex_menu"
CLIENT = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/client"
pack = (CLIENT / "AerixClientPackGenerator.kt").read_text()
store = (CLIENT / "AerixClientStore.kt").read_text()
installer = (CLIENT / "AerixClientInstaller.kt").read_text()
screen = (ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix/ui/screens/"
          "AerixClientScreen.kt").read_text()
mod = (ROOT / "aerix_client_mod/src/main/java/aerix/client/mod/VortexMenuHooks.java").read_text()


def png_size(path: Path):
    data = path.read_bytes()[:33]
    assert data[:8] == b"\x89PNG\r\n\x1a\n", f"{path.name} is not a PNG"
    return struct.unpack(">II", data[16:24])


# ── Shipped art ───────────────────────────────────────────────────────────
scene = ASSETS / "menu_red_forest.webp"
raw = scene.read_bytes()
assert raw[:4] == b"RIFF" and raw[8:12] == b"WEBP", "red forest scene must be a webp"
assert len(raw) < 400_000, "menu scene stays small enough for a phone APK"

# Logo sheets: modern = 256x64 logical at 4x, legacy = 256x256 split at 4x.
assert png_size(ASSETS / "logo_modern.png") == (1024, 256)
assert png_size(ASSETS / "logo_legacy.png") == (1024, 1024)
for name in ("button.png", "button_highlighted.png", "button_disabled.png"):
    assert png_size(ASSETS / name) == (800, 80), name
assert png_size(ASSETS / "widgets_legacy.png") == (1024, 1024)
assert png_size(ASSETS / "panorama_overlay.png") == (16, 128)
assert "VORTEX CLIENT!" in (ASSETS / "splashes.txt").read_text()

# ── Pack generator writes them into the version-correct paths ────────────
assert 'internal const val MENU_ASSET_DIR = "vortex_menu"' in pack
assert "v7_vortex_red_forest_menu_2026" in pack, "new art invalidates old packs"
assert "private fun appendVortexRedForestMenu(context: Context, zos: ZipOutputStream, packFmt: Int)" in pack
assert 'if (packFmt >= 13) "logo_modern.png" else "logo_legacy.png"' in pack
assert '"assets/minecraft/textures/gui/title/minecraft.png"' in pack
assert '"assets/minecraft/textures/gui/title/edition.png"' in pack
assert 'menuBitmap(context, "menu_red_forest.webp", inSampleSize = 2)' in pack, "red forest panorama"
assert 'buildPanoramaFacesFrom(scene)' in pack
assert '"assets/minecraft/textures/gui/title/background/panorama_$idx.png"' in pack
assert 'menuBitmap(context, "panorama_overlay.png")' in pack
assert 'assets/minecraft/textures/gui/sprites/widget/$sprite.png' in pack, "1.20.2+ buttons"
assert '"nine_slice"' in pack and '"width":200,"height":20,"border":$border' in pack
assert 'menuBitmap(context, "widgets_legacy.png")' in pack, "legacy widget sheet"
assert 'menuText(context, "splashes.txt")' in pack
# Missing art must degrade to the old renderer, never to a missing texture / crash.
assert "if (!appendVortexRedForestMenu(context, zos, packFmt))" in pack
assert "buildAerixTitleLogoModern(context)" in pack and "buildAerixTitleLogoLegacySplit(context)" in pack

# ── options.txt: readability, parallax, pack enabled ─────────────────────
assert 'map["panoramaScrollSpeed"] =' in pack and 'packMenuParallax) "1.0" else "0.0"' in pack
assert 'map["monochromeLogo"] = "false"' in pack, "red wordmark must never render grey"
assert 'map["resourcePacks"]' in pack and '"file/$PACK_FILE_NAME"' in pack
assert "var packMenuParallax by mutableStateOf(true)" in store
assert 'putBoolean("packMenuParallax", packMenuParallax)' in store

# ── In-launcher control on the client page ───────────────────────────────
assert '"CLIENT OPTIONS"' in screen and '"VORTEX RED FOREST"' in screen
assert '"REBUILD MENU PACK"' in screen
# Full-width option rows with real switches beat the cramped half-width chips.
assert "private fun ClientOptionRow(" in screen and "Switch(" in screen
assert 'label = "Crimson menu"' in screen and 'label = "Menu parallax"' in screen
assert ".apexPress(onClick = onClick)" in screen, "menu buttons press with motion"
assert "AerixClientPackGenerator.buildAndApplyToInstance(activity, target, selectedVersion)" in screen
assert "AerixClientPackGenerator.applyClientOptionsTxt(target, selectedVersion)" in screen
assert "packMenuParallax = !AerixClientStore.packMenuParallax" in screen
assert "onChange = {" in screen, "option lambda is passed by name"
assert "onCheckedChange = { onChange() }" in screen

# ── Real functionality: no re-implemented or fake menu screens ───────────
assert "class " not in mod.replace("public final class VortexMenuHooks", ""), "mod adds no screen class"
assert "override" not in mod or "InvocationHandler" in mod
assert "method_37063" in mod, "only adds a widget to the real title screen"
# The MODS button only exists when Mod Menu is installed - the launcher guarantees it.
assert 'ClientModEntry("modmenu", "Mod Menu", "Client UI")' in installer
assert "private fun ensureModMenu(modsDir: File, mcVersion: String)" in installer
assert "ensureModMenu(modsDir, mcVersion)" in installer

print("Vortex Red Forest main menu smoke test passed")
