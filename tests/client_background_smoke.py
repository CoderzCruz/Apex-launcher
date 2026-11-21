"""The VORTEX client skin (in-game HUD, title-screen pack) is gone.

This smoke pins the removal: no client overlay, no generated main-menu pack and no
Client-only activity wiring may creep back into the launcher.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch"

assert not (JAVA / "aerix/client").exists(), "client feature package removed"
assert not (ROOT / "aerix_client_mod").exists(), "client mod removed"
assert not (ROOT / "app_pojavlauncher/src/main/assets/vortex_menu").exists(), "menu art removed"

activity = (JAVA / "game/GameActivity.java").read_text()
for gone in ("AerixClientOverlayView", "AerixClientInstaller", "isAerixClient", "aerix_client_overlay"):
    assert gone not in activity, f"{gone} still wired into the game activity"

layout = (ROOT / "app_pojavlauncher/src/main/res/layout/activity_basemain.xml").read_text()
assert "aerix_client_overlay" not in layout, "overlay view removed from the layout"

pack_json = (ROOT / "settings.gradle").read_text()
assert "aerix_client_mod" not in pack_json, "client mod module unregistered"

print("Client removal: overlay, pack generator, mod and wiring are gone")
