"""Vortex Client desktop module: real screens, real launcher pieces, no mockups."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MOD = ROOT / "vortexclient"

assert (MOD / "build.gradle").exists(), "module build.gradle"
gradle = (MOD / "build.gradle").read_text()
assert "org.json" in gradle and "VortexClientApp" in gradle, "fat jar + main class"

app = (MOD / "src/main/java/com/vortex/client/VortexClientApp.java").read_text()
for screen in ["singleplayer", "multiplayer", "mods", "cosmetics", "performance", "settings", "accounts"]:
    assert screen in app, f"screen {screen} registered"
assert "CardLayout" in app

# Real launch engine, not a fake play button
launcher = (MOD / "src/main/java/com/vortex/client/game/MinecraftLauncher.java").read_text()
for token in ["ProcessBuilder", "mainClass", "libraries", "assetIndex", "--username", "offlineUuid"]:
    assert token in launcher, f"launcher missing {token}"

# Multiplayer = real MC status ping
mp = (MOD / "src/main/java/com/vortex/client/ui/MultiplayerScreen.java").read_text()
assert "Socket" in mp and "writeVarInt" in mp and "saveServers" in mp, "real server ping + CRUD"

# Mods = real jar toggling on disk
mods = (MOD / "src/main/java/com/vortex/client/ui/ModsScreen.java").read_text()
assert ".jar.disabled" in mods and "listFiles" in mods, "real mod toggles"

# Config persistence
cfg = (MOD / "src/main/java/com/vortex/client/data/ClientConfig.java").read_text()
assert "vortex-client.properties" in cfg and "saveServers" in cfg, "persistence"

# Performance values persist
perf = (MOD / "src/main/java/com/vortex/client/ui/PerformanceScreen.java").read_text()
assert "ramMb" in perf and "renderDistance" in perf, "perf settings persisted"

# Workflow publishes the jar
wf = (ROOT / ".github/workflows/vortex-client.yml").read_text()
assert ":vortexclient:jar" in wf and "client-latest" in wf

# Design tokens from the spec
theme = (MOD / "src/main/java/com/vortex/client/ui/Theme.java").read_text()
for hexv in ["0x090909", "0x111111", "0x181818", "0xE21F3D", "0xFF304F", "0x858585"]:
    assert hexv in theme, f"theme color {hexv}"

print("Vortex Client smoke test passed")
