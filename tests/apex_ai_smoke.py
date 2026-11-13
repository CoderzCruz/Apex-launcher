#!/usr/bin/env python3
"""Smoke checks for the restored Apex Service: the client, key plumbing, AI page and CI bake.

The Gemini key never lives in the repository: CI overwrites the empty
`apex_service_ai_key` string at build time from the APEX_SERVICE_AI_KEY secret, and a blank
key falls back to the built-in knowledge base — the page says which source it will use.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main"
SERVICE = APP / "java/net/kdt/pojavlaunch/aerix/social/ApexService.kt"
PREFS = APP / "java/net/kdt/pojavlaunch/aerix/bridge/AerixPreferences.kt"
AI_SCREEN = APP / "java/net/kdt/pojavlaunch/aerix/ui/screens/AiScreen.kt"
STRINGS = APP / "res/values/strings.xml"
LAUNCHER = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixLauncherScreen.kt"
RAIL = APP / "java/net/kdt/pojavlaunch/aerix/ui/AerixNavigationRail.kt"
WORKFLOW = ROOT / ".github/workflows/aerix-debug.yml"

service = SERVICE.read_text()
assert "object ApexService" in service
assert "suspend fun ask(question: String" in service
assert "fun resetConversation()" in service
assert "localAnswer" in service, "knowledge-base fallback when no key"

prefs = PREFS.read_text()
assert 'KEY_GOOGLE_AI_KEY = "apexServiceAiKey"' in prefs
assert "fun effectiveAiKey(): String = googleAiKey.ifBlank { bakedAiKey }" in prefs
assert "fun saveGoogleAiKey" in prefs
assert "R.string.apex_service_ai_key" in prefs

strings = STRINGS.read_text()
import re
m = re.search(r'<string name="apex_service_ai_key"[^>]*>([^<]*)</string>', strings)
assert m is not None, "placeholder exists for the baked key"
assert m.group(1) == "", "placeholder must stay empty — no secret in the repo"

screen = AI_SCREEN.read_text()
assert "ApexService.ask(question)" in screen
assert "Local knowledge base (no key set)" in screen, "honest source hint"
assert "Gemini key ready" in screen

import re
assert re.search(r"^\s+AI,\s*$", RAIL.read_text(), re.M), "AI enum entry"
launcher = LAUNCHER.read_text()
assert "LauncherSection.AI -> AiScreen(" in launcher

wf = WORKFLOW.read_text()
assert "APEX_SERVICE_AI_KEY: ${{ secrets.APEX_SERVICE_AI_KEY }}" in wf
assert "Bake Apex Service key" in wf

print("Apex Service: client, key plumbing, AI page and CI bake passed")
