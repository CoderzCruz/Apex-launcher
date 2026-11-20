"""Vortex Client Mobile: separate app module with real persisted behaviour."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOD = ROOT / "vortexclientapp"

gradle = (MOD / "build.gradle").read_text()
assert "com.vortex.clientapp" in gradle and "com.android.application" in gradle, "separate app module"

manifest = (MOD / "src/main/AndroidManifest.xml").read_text()
assert "com.vortex.clientapp.ui.MainActivity" in manifest or ".ui.MainActivity" in manifest
assert "Vortex Client" in manifest

main = (MOD / "src/main/java/com/vortex/clientapp/ui/MainActivity.kt").read_text()
for screen in ["SINGLEPLAYER", "MULTIPLAYER", "MODS", "SETTINGS", "COSMETICS", "PERFORMANCE", "ACCOUNTS"]:
    assert screen in main, f"menu button {screen}"
assert "0xFFE21F3D" in main, "vortex red"
assert "RedButton" in main, "feather-style buttons"

screens = (MOD / "src/main/java/com/vortex/clientapp/ui/Screens.kt").read_text()
for token in ["writeVarInt", "readVarInt", "ping(", "ClientPrefs.saveServers", "Slider", "Microsoft sign-in"]:
    assert token in screens, f"screens missing {token}"

prefs = (MOD / "src/main/java/com/vortex/clientapp/data/ClientPrefs.kt").read_text()
for token in ["ramMb", "fpsCap", "renderDistance", "cape", "launcherPackage"]:
    assert token in prefs, f"prefs missing {token}"

wf = (ROOT / ".github/workflows/vortex-client-mobile.yml").read_text()
assert ":vortexclientapp:assembleDebug" in wf and "client-mobile-latest" in wf

# The separate app must NOT be referenced by the launcher app
settings = (ROOT / "settings.gradle").read_text()
assert "include ':vortexclientapp'" in settings
app_gradle = (ROOT / "app_pojavlauncher/build.gradle").read_text()
assert "vortexclientapp" not in app_gradle, "separate app must not be a launcher dependency"

print("Vortex Client Mobile smoke test passed")
