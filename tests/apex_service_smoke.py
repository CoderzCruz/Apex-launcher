#!/usr/bin/env python3
"""Guard rails for Apex Service, the in-app assistant.

It must stay a real assistant: a pinned conversation in Chats, mention handling inside shared
rooms, a Gemini brain when a key is saved, and a built-in knowledge base so the feature never
looks dead. The system prompt must keep the honest "no live voice chat" line.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix"

SERVICE = APP / "social/ApexService.kt"
BADGE = APP / "social/ApexBadge.kt"
PREFS = APP / "bridge/AerixPreferences.kt"
SETTINGS = APP / "ui/screens/SettingsScreen.kt"
CHATS = APP / "ui/screens/ChatsScreen.kt"
STRINGS = ROOT / "app_pojavlauncher/src/main/res/values/aerix_strings.xml"

service = SERVICE.read_text()
assert "object ApexService" in service
assert 'const val NAME = "Apex Service"' in service

# Addressable in a room, with the mention stripped before the brain sees it.
assert "fun mentionsUs(" in service
assert '"@apex"' in service and '"apex service"' in service, "the room trigger words must stay"
assert "private fun stripMention(" in service

# Real AI path.
assert "generativelanguage.googleapis.com" in service
assert "gemini-2.5-flash" in service, "the free Gemini flash model must stay the default"
assert 'setRequestProperty("x-goog-api-key"' in service
assert "suspend fun reply(" in service and "suspend fun ask(" in service

# Honest system prompt: never claims live voice chat exists.
assert "Live voice chat (a real voice channel) is NOT available yet" in service
assert "GPL-3.0 fork of PojavLauncher" in service, "author credit must stay in the prompt"

# Offline brain so the assistant answers without a key.
assert "localAnswer(" in service and "knowledge" in service
assert '"voice chat", "vc"' in service, "must answer VC questions honestly"

# Key storage + settings field.
prefs = PREFS.read_text()
assert 'const val KEY_GOOGLE_AI_KEY = "apexServiceAiKey"' in prefs
assert "var googleAiKey by mutableStateOf(" in prefs
assert "fun saveGoogleAiKey(" in prefs
settings = SETTINGS.read_text()
assert "AerixPreferences.googleAiKey" in settings
assert "AerixPreferences.saveGoogleAiKey(it)" in settings
strings = STRINGS.read_text()
assert "aerix_settings_ai_key" in strings and "aerix_settings_ai_key_sub" in strings

# Assistant badge.
badge = BADGE.read_text()
assert 'SERVICE("BOT"' in badge
assert '"apex service" to ApexBadge.SERVICE' in badge

# Chats integration.
chats = CHATS.read_text()
assert "data object Service : ChatTarget" in chats
assert "private fun ServiceThread(" in chats
assert "private fun BotRow(" in chats
assert "ApexService.mentionsUs(" in chats, "rooms must detect the @apex mention"
assert "ApexService.reply(" in chats and "ApexService.ask(" in chats
assert 'items(botReplies, key = { "apex-service-" + it.first })' in chats
assert "ApexService.NAME" in chats and "ApexBadge.SERVICE" in chats

# Ship-with-build brain: the CI secret is baked in as a resource, never committed.
gradle = (ROOT / "app_pojavlauncher/build.gradle").read_text()
assert "def getServiceAiKey = {" in gradle
assert "resValue 'string', 'apex_service_ai_key', getServiceAiKey()" in gradle
assert "System.getenv(\"APEX_SERVICE_AI_KEY\")" in gradle
workflow = (ROOT / ".github/workflows/aerix-debug.yml").read_text()
assert "APEX_SERVICE_AI_KEY: ${{ secrets.APEX_SERVICE_AI_KEY }}" in workflow
assert "fun effectiveAiKey()" in prefs and "googleAiKey.ifBlank { bakedAiKey }" in prefs
assert "AerixPreferences.effectiveAiKey()" in service
# The secret itself must never appear in the tree.
# The prefix is assembled here so this guard file never contains the secret itself.
key_prefix = "AQ.Ab" + "8RN6"
for path in ROOT.rglob("*"):
    if path == Path(__file__) or not path.is_file():
        continue
    if path.suffix in {".kt", ".gradle", ".yml", ".py", ".xml", ".txt", ".properties"}:
        assert key_prefix not in path.read_text(errors="ignore"), f"AI key leaked into {path}"

print("Apex Service wiring passed")
