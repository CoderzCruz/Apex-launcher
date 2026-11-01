"""Guard the Crimson Noir palette and the three Home shortcut cards."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
aerix = root / "app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/aerix"
surface = (aerix / "theme/AerixSurface.kt").read_text()
theme = (aerix / "theme/AerixTheme.kt").read_text()
home = (aerix / "ui/screens/HomeScreen.kt").read_text()
strings = (root / "app_pojavlauncher/src/main/res/values/aerix_strings.xml").read_text()

# Palette: graphite canvas, crimson accent, white text on the accent.
assert "val canvas = Color(0xFF08090B)" in surface
assert "val accentDefault = Color(0xFFE11D48)" in surface
assert "val onAccentDefault = Color(0xFF1B0508)" in surface
assert "val textPrimary = Color(0xFFF4F5F7)" in surface
assert "onPrimary = Color.White" in theme

# The wallpaper preset keeps its own teal so it does not repaint the crimson theme.
assert '"Lush Teal", Color(0xFF8DEFE0)' in (aerix / "theme/AerixThemeManager.kt").read_text()

# Home opens with Backups, Accounts and Contacts.
assert "R.string.aerix_home_quick_backups" in home
assert "R.string.aerix_home_quick_accounts" in home
assert "R.string.aerix_home_quick_contacts" in home
assert 'name="aerix_home_quick_backups">Backups<' in strings
assert 'name="aerix_home_quick_accounts">Accounts<' in strings
assert 'name="aerix_home_quick_contacts">Contacts<' in strings

# The dashboards dropped the instance count argument when the old tiles went away.
assert "private fun QuickTiles(accent: Color, actions: HomeActions" in home
assert "QuickTiles(count" not in home
print("Crimson Noir home smoke test passed")
