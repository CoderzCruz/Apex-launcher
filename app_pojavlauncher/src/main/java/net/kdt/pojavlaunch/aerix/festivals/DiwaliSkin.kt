/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Temporary Diwali skin: the icon and the accent follow the calendar and turn themselves
 * back off after Diwali, with nothing persisted and no update required.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.festivals

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import java.util.Calendar

/**
 * The Diwali skin window: 4 November 2026 only — the event was ended early on 5 November
 * (it was meant to run through Diwali, 8 November). Closing the window is still just the
 * clock: every check reads it, so the skin switches off by itself with no preference, no
 * toggle and no update. Set DAY_AFTER_LAST back to 9 to re-arm it for Diwali day.
 */
object DiwaliSkin {
    /** Marigold gold — the Diwali accent everywhere in the shell. */
    val GOLD = Color(0xFFFFC24B)

    private const val YEAR = 2026
    private const val FIRST_DAY = 4
    private const val DAY_AFTER_LAST = 5

    /** Component names of the two launcher-icon aliases declared in the manifest. */
    private const val ICON_NORMAL = "net.kdt.pojavlaunch.ApexLauncherIcon"
    private const val ICON_DIWALI = "net.kdt.pojavlaunch.ApexLauncherIconDiwali"

    private val startBoundary by lazy { dayBoundary(FIRST_DAY) }
    private val endBoundary by lazy { dayBoundary(DAY_AFTER_LAST) }

    /** True while the Diwali skin should be showing. */
    fun active(now: Long = System.currentTimeMillis()): Boolean =
        now >= startBoundary && now < endBoundary

    /** Milliseconds until the window opens or closes, whichever is next; 0 when nothing is pending. */
    fun millisToNextBoundary(now: Long = System.currentTimeMillis()): Long = when {
        active(now) -> (endBoundary - now).coerceAtLeast(0L)
        now < startBoundary -> startBoundary - now
        else -> 0L
    }

    /**
     * Points the launcher icon at the Diwali alias while the window is open and at the normal
     * alias otherwise. The wanted alias is always enabled before the other one is disabled, so
     * there is never a moment with no icon, and DONT_KILL_APP keeps the app alive while the
     * launcher re-resolves the home-screen entry.
     */
    @JvmStatic
    fun syncIcon(context: Context) {
        val pm = context.packageManager
        if (active()) {
            setAliasEnabled(pm, context, ICON_DIWALI, true)
            setAliasEnabled(pm, context, ICON_NORMAL, false)
        } else {
            setAliasEnabled(pm, context, ICON_NORMAL, true)
            setAliasEnabled(pm, context, ICON_DIWALI, false)
        }
    }

    private fun setAliasEnabled(pm: PackageManager, context: Context, cls: String, want: Boolean) {
        val component = ComponentName(context.packageName, cls)
        val shown = when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            // DEFAULT follows the manifest: normal icon on, Diwali icon off.
            else -> cls == ICON_NORMAL
        }
        if (shown == want) return
        val state = if (want) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
    }

    private fun dayBoundary(day: Int): Long = Calendar.getInstance().apply {
        set(YEAR, Calendar.NOVEMBER, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/**
 * Brings the icon back in line after a reboot or an app update, so the window also closes
 * for people who never reopened the launcher.
 */
class DiwaliIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching { DiwaliSkin.syncIcon(context) }
    }
}
