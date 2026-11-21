#!/usr/bin/env python3
"""Only Vortex updates may ever be offered.

Owner rule: no Aerix / upstream (entitybrian69-bit) launcher update may appear inside
Vortex — no "A new Aerix is ready", no "v2.4.4 / v2.4.5 arm64" prompt. The launcher checks
one place: our own `debug-latest` release on CoderzCruz/Apex-launcher.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
BRIDGE = APP / "java/net/kdt/pojavlaunch/aerix/bridge"
updater = (BRIDGE / "LauncherUpdater.kt").read_text()
policy = (BRIDGE / "LauncherUpdatePolicy.kt").read_text()
notices = (BRIDGE / "LauncherUpdateNotices.kt").read_text()
dialog = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/LauncherUpdateDialog.kt").read_text()
host = (APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/LauncherUpdateNoticeHost.kt").read_text()

# ── One channel, one repo ────────────────────────────────────────────────
assert updater.count("api.github.com") == 1, "exactly one release endpoint"
assert "https://api.github.com/repos/CoderzCruz/Apex-launcher/releases/tags/debug-latest" in updater
assert "entitybrian69-bit/Aerix-launcher/releases" not in updater, "no upstream release URL"
assert "releases/latest" not in updater, "the upstream 'latest' release is never queried"
# Only a comment may mention the banned upstream tag; no code pins it.
for line in (updater + policy).splitlines():
    stripped = line.strip()
    if stripped.startswith(("*", "//", "/*")) or stripped.startswith("/**"):
        continue
    assert "v2.4" not in stripped, stripped

# Trusted assets: only our repo + only debug-latest.
assert 'const val OWNER_REPO = "CoderzCruz/Apex-launcher"' in policy
assert 'const val OWNER_RELEASE_TAG = "debug-latest"' in policy
assert 'startsWith("/$OWNER_REPO/releases/download/$OWNER_RELEASE_TAG/")' in policy
assert "entitybrian69-bit/Aerix-launcher/releases" not in policy

# ── check(): a failed Vortex lookup is "current", never an upstream fallback ──
check_body = updater[updater.index("    fun check() {"):updater.index("    fun download(context: Context)")]
assert "val vortex = fetchVortexDebugRelease()" in check_body
assert check_body.count("phase = LauncherUpdatePhase.CURRENT") == 1
assert "fetchLatest" not in check_body and "LauncherUpdatePolicy.release(" not in check_body
assert "LauncherUpdatePolicy.release(" not in updater, "upstream parser is not wired into the app"

# ── Offer gating: only releases we built (vortex = true) auto-download ────
assert "release?.vortex == true" in host, "only a Vortex release may auto-download"
assert "LauncherUpdateNotices.shouldOffer(" in host and "available > installed" in notices

# ── Copy: the prompt says Vortex, never Aerix ────────────────────────────
assert '"A new Vortex is ready"' in dialog
for banned in ("Aerix is ready", "A new Aerix", "New Aerix update"):
    assert banned not in dialog and banned not in host and banned not in updater, banned

# ── One dialog at a time + honest copy ───────────────────────────────────
assert "val notificationPromptAnswered = AerixPreferences.notificationPromptShown" in host
assert "if (manual > 0 || !notificationPromptAnswered) offeredTag = null" in host, \
    "an update prompt must never stack on the first-run notification card"
assert "launcher\" updates only" in dialog or "Vortex Launcher updates only" in dialog

print("Vortex-only update channel smoke test passed")
