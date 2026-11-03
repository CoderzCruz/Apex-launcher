/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Owner-only access to the fields that belong to the launcher itself.
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

package net.kdt.pojavlaunch.aerix.bridge

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import net.kdt.pojavlaunch.aerix.discord.AerixDiscordPresence

/**
 * Who may edit the launcher's own identity: the card image, the invite, the website and the API
 * keys. Everyone else keeps the card and every other setting, but those five fields are read-only.
 *
 * Two ways to be the owner:
 *  - Discord is connected and the signed-in account is the owner id (automatic);
 *  - the owner typed their Discord user id once on this device ([AerixPreferences.ownerVerified]).
 *
 * This is a soft client-side lock. It stops casual edits, not somebody who rebuilds the APK.
 */
object ApexOwner {

    /** Bumped whenever ownership changes so every locked field recomposes. */
    var revision by mutableIntStateOf(0)
        private set

    fun isOwner(): Boolean =
        AerixPreferences.ownerVerified || AerixDiscordPresence.isOwnerAccount()

    /**
     * Checks a typed Discord user id. Digits only, so pasting a mention like <@123> still works.
     * Returns true when the id belongs to the owner.
     */
    fun verify(candidate: String): Boolean {
        val id = candidate.trim().filter { it.isDigit() }
        if (id.length < 17) return false
        val owner = AerixPreferences.discordOwnerId.trim()
            .ifBlank { AerixDiscordPresence.OWNER_DISCORD_ID }
        if (id != owner) return false
        AerixPreferences.saveOwnerVerified(true)
        revision++
        return true
    }

    /** Drops the remembered owner on this device. */
    fun forget() {
        AerixPreferences.saveOwnerVerified(false)
        revision++
    }

    /** Message shown under a locked field instead of hiding it. */
    const val LOCK_HINT = "\uD83D\uDD12 Locked to the Apex owner \u2014 only the owner account can change this."
}
