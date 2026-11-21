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
# Widget sprites must be at NATIVE size: nine-slice UVs are absolute. 4x art rendered as
# magenta missing-texture bars on the title screen (owner screenshot, 1.21.11).
for name in ("button.png", "button_highlighted.png", "button_disabled.png"):
    assert png_size(ASSETS / name) == (200, 20), name
assert png_size(ASSETS / "tab.png") == (130, 24)
assert png_size(ASSETS / "tab_highlighted.png") == (130, 24)
assert png_size(ASSETS / "tab_selected.png") == (130, 24)
assert png_size(ASSETS / "widgets_legacy.png") == (256, 256)

# ── Glass buttons: the panorama must stay visible through every widget ────
# A fully opaque face hides the red forest again (owner asked for transparency).
def _alpha(path: Path, xy) -> int:
    from PIL import Image  # noqa: PLC0415  (test-only dependency)
    return Image.open(path).convert("RGBA").getpixel(xy)[3]


try:
    import PIL  # noqa: F401
except ImportError:  # pragma: no cover - CI installs pillow for this check
    pass
else:
    for name in ("button.png", "button_highlighted.png"):
        face = _alpha(ASSETS / name, (100, 10))
        rim = _alpha(ASSETS / name, (1, 10))
        assert 90 <= face <= 200, f"{name} face alpha {face} must be glass, not solid"
        assert rim >= 200, f"{name} rim must stay readable ({rim})"
    assert _alpha(ASSETS / "button_disabled.png", (100, 10)) < 120, "disabled stays dimmer"
    assert 80 <= _alpha(ASSETS / "tab.png", (60, 12)) <= 160, "tabs are glass too"
assert png_size(ASSETS / "panorama_overlay.png") == (16, 128)
assert "VORTEX CLIENT!" in (ASSETS / "splashes.txt").read_text()

# ── Pack generator writes them into the version-correct paths ────────────
assert 'internal const val MENU_ASSET_DIR = "vortex_menu"' in pack
assert "v9_vortex_glass_buttons" in pack, "new art invalidates old packs"
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
assert '\"border\":{\"left\":2,\"top\":2,\"right\":2,\"bottom\":0}' in pack, "vanilla tab border"
assert 'menuBitmap(context, "$alias.png")' in pack, "tabs ship their own 130x24 sprites"
assert "v9_vortex_glass_buttons" in pack, "old broken packs are rebuilt"
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
assert "AerixClientPackGenerator.applyClientOptionsTxt(clientDir, selectedVersion)" in screen
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

# ── The menu also lands on NON-Client instances (plain, modded, modpacks) ──
assert 'const val MENU_PACK_FILE_NAME = "Vortex-Main-Menu.zip"' in pack
assert "fun ensureMainMenuPackFor(" in pack, "menu-only pack writer"
assert "fun enableMainMenuPackOptions(" in pack, "options.txt merge for normal instances"
assert 'if (!found && enabled) rewritten += "resourcePacks:"' in pack, "adds the pack when missing"
assert "for (i in packs.length() - 1 downTo 0)" in pack, "no duplicate entries"
assert "fun packStampFor(enabled: Boolean)" in pack
assert "if (!enabled) {" in pack and "zipFile.delete()" in pack, "switching off removes the pack"

assert "fun syncMainMenuAssetsAtGameStart(context: Context, instance: Instance)" in installer
assert "if (instance.isAerixClient) return" in installer, "client instances keep their own pack"
assert "AerixClientStore.packCustomMainMenu &&" in installer and "packMenuAllInstances && !shipsOwnMenu" in installer
assert "resourcepacks/.vortex_menu_options" in installer, "player's own pack choice is respected"
assert "val shipsOwnMenu" in installer, "a modpack that ships its own title keeps it"
assert "packCustomMainMenu &&\n                AerixClientStore.packMenuAllInstances && !shipsOwnMenu" in installer

activity = (ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/game/GameActivity.java").read_text()
assert "syncMainMenuAssetsAtGameStart(this, instance)" in activity, "wired into every launch"
assert "syncLocalAssetsAtGameStart(this, instance)" in activity

assert "var packMenuAllInstances by mutableStateOf(true)" in store
assert 'putBoolean("packMenuAllInstances", packMenuAllInstances)' in store
assert 'label = "Har instance pe menu"' in screen

print("Vortex Red Forest main menu smoke test passed")
