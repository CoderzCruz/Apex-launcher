package com.vortex.clientapp.data

import android.content.Context

/**
 * Real persisted client settings (SharedPreferences). Nothing fake: values are
 * written by the UI and read back on every screen open.
 */
object ClientPrefs {
    private const val FILE = "vortex_client_cfg"

    private fun p(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun username(context: Context): String = p(context).getString("username", "Player") ?: "Player"
    fun setUsername(context: Context, v: String) = p(context).edit().putString("username", v).apply()

    fun ramMb(context: Context): Int = p(context).getInt("ramMb", 2048)
    fun setRamMb(context: Context, v: Int) = p(context).edit().putInt("ramMb", v).apply()

    fun fpsCap(context: Context): Int = p(context).getInt("fpsCap", 120)
    fun setFpsCap(context: Context, v: Int) = p(context).edit().putInt("fpsCap", v).apply()

    fun renderDistance(context: Context): Int = p(context).getInt("renderDistance", 8)
    fun setRenderDistance(context: Context, v: Int) = p(context).edit().putInt("renderDistance", v).apply()

    fun graphics(context: Context): String = p(context).getString("graphics", "Fast") ?: "Fast"
    fun setGraphics(context: Context, v: String) = p(context).edit().putString("graphics", v).apply()

    fun cape(context: Context): String = p(context).getString("cape", "None") ?: "None"
    fun setCape(context: Context, v: String) = p(context).edit().putString("cape", v).apply()

    fun skinStyle(context: Context): String = p(context).getString("skinStyle", "Classic") ?: "Classic"
    fun setSkinStyle(context: Context, v: String) = p(context).edit().putString("skinStyle", v).apply()

    fun launcherPackage(context: Context): String =
        p(context).getString("launcherPackage", "net.kdt.pojavlaunch") ?: "net.kdt.pojavlaunch"
    fun setLauncherPackage(context: Context, v: String) =
        p(context).edit().putString("launcherPackage", v).apply()

    // ── Servers: "name|address" entries ──
    fun servers(context: Context): MutableList<Pair<String, String>> {
        val raw = p(context).getString("servers", "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split("~~").mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMutableList()
    }

    fun saveServers(context: Context, servers: List<Pair<String, String>>) {
        val raw = servers.joinToString("~~") { (n, a) -> n.replace("|", " ").replace("~~", " ") + "|" + a }
        p(context).edit().putString("servers", raw).apply()
    }
}
