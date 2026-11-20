package net.kdt.pojavlaunch.aerix.client

import android.content.Context
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.game.renderer.def.Renderers
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.prefs.LauncherPreferences

/**
 * Runtime settings a modpack expects, applied right after its install:
 *
 * - RAM: every pack raises the allocation (mod-heavy loads) but never above
 *   60% of the device's RAM, and never downwards — the user's higher choice wins.
 * - Renderer: the Vulkan pack ships VulkanMod (an in-game renderer), so the
 *   launcher-side renderer is forced to plain GL4ES — two GL-to-Vulkan layers
 *   at once means black screens. Sodium-based packs keep the current renderer.
 *
 * The instance-level renderer tag is cleared so the instance inherits the new
 * global choice (getLaunchRenderer prefers a saved instance tag).
 */
object PackRuntimeProfiles {

    const val PACK_RAM_MB = 2048

    fun applyForPack(context: Context, packTitle: String, instance: Instance?) {
        runCatching {
            val prefs = LauncherPreferences.DEFAULT_PREF

            val totalMb = Tools.getTotalDeviceMemory(context)
            val cap = (totalMb * 0.6f).toInt().coerceAtLeast(1024)
            val target = PACK_RAM_MB.coerceAtMost(cap)
            val current = prefs.getInt("allocation", LauncherPreferences.PREF_RAM_ALLOCATION)
            if (current < target) {
                prefs.edit().putInt("allocation", target).apply()
                LauncherPreferences.PREF_RAM_ALLOCATION = target
            }

            if (packTitle.contains("Vulkan", ignoreCase = true)) {
                prefs.edit()
                    .putString("renderer", Renderers.GL4ES_RENDERER)
                    .putBoolean("cosmic_confirmed", false)
                    .apply()
                LauncherPreferences.PREF_RENDERER = Renderers.GL4ES_RENDERER
            }

            // Inherit the global renderer from now on.
            instance?.let {
                it.renderer = null
                it.maybeWrite()
            }
        }
    }
}
