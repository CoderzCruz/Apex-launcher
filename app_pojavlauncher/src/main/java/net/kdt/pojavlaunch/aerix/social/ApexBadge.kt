/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Staff badges shown next to a player's name in the shared room and on profiles.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.social

/**
 * A staff label attached to a name. Badges are matched on the name the room shows, so they work
 * without carrying extra columns in the backend; whoever renames themselves loses the badge.
 */
enum class ApexBadge(val label: String, val emoji: String, val colorArgb: Long) {
    CEO("CEO", "\uD83D\uDC51", 0xFFFFB020),
    OWNER("OWNER", "\uD83D\uDC51", 0xFFEF233C),
    DEVELOPER("DEV", "\uD83D\uDEE0", 0xFF38BDF8),
    TEAM("TEAM", "\u2B50", 0xFFB388FF),
    SERVICE("BOT", "\uD83E\uDD16", 0xFF7C5CFF);

    /** "👑 CEO" as shown next to a name. */
    val chip: String get() = "$emoji $label"
}

object ApexBadges {
    /**
     * Names that carry a badge. Add or remove entries here; matching is case-insensitive and
     * ignores the role tags the backend wraps around names like "(OWNER) Cruz".
     */
    private val byName: Map<String, ApexBadge> = mapOf(
        "yadavboy" to ApexBadge.CEO,
        "cruz" to ApexBadge.OWNER,
        "entitybrian" to ApexBadge.DEVELOPER,
        // The in-app assistant always wears its own badge.
        "apex service" to ApexBadge.SERVICE
    )

    fun forName(name: String?): ApexBadge? {
        val clean = (name ?: return null)
            .substringAfterLast(')')
            .trim()
            .lowercase()
            .trim('*', '_', '~')
        if (clean.isEmpty()) return null
        byName[clean]?.let { return it }
        // Fall back to a contains match so decorated names ("Cruz {/}") still resolve.
        return byName.entries.firstOrNull { (key, _) -> clean.contains(key) }?.value
    }
}
