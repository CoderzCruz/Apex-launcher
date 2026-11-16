package net.kdt.pojavlaunch.aerix.client

import java.io.File
import java.util.LinkedHashMap

/**
 * One-tap video-settings presets written into a Vortex Client instance's options.txt.
 * Keys use the modern colon format; every other key is preserved, and a value a game
 * version does not understand simply falls back to its default on load.
 */
object ClientConfigProfiles {

    data class Profile(
        val id: String,
        val title: String,
        val caption: String,
        val options: Map<String, String>
    )

    val PROFILES: List<Profile> = listOf(
        Profile(
            "pvp",
            "PvP",
            "FOV 90 · 8 chunks · fast graphics · no shadows/clouds · 144 FPS cap",
            linkedMapOf(
                "fov" to "90",
                "graphics" to "FAST",
                "renderDistance" to "8",
                "maxFps" to "144",
                "particles" to "MINIMAL",
                "entityShadows" to "false",
                "cloud" to "false",
                "bobView" to "false",
                "enableVsync" to "false"
            )
        ),
        Profile(
            "fps",
            "FPS Boost",
            "6 chunks · fast graphics · minimal FX · no clouds · 120 FPS cap",
            linkedMapOf(
                "graphics" to "FAST",
                "renderDistance" to "6",
                "maxFps" to "120",
                "particles" to "MINIMAL",
                "entityShadows" to "false",
                "cloud" to "false",
                "ao" to "false",
                "enableVsync" to "false",
                "mipmapLevels" to "2"
            )
        ),
        Profile(
            "build",
            "Builder",
            "12 chunks · fancy graphics · all particles · smooth lighting · clouds on",
            linkedMapOf(
                "graphics" to "FANCY",
                "renderDistance" to "12",
                "maxFps" to "120",
                "particles" to "ALL",
                "cloud" to "true",
                "ao" to "true",
                "fov" to "70",
                "entityShadows" to "true"
            )
        )
    )

    /** Merges [options] into the instance's options.txt, preserving every other key. */
    fun applyOptions(instanceGameDir: File, options: Map<String, String>) {
        val optFile = File(instanceGameDir, "options.txt")
        val map = LinkedHashMap<String, String>()
        if (optFile.isFile) {
            for (line in optFile.readLines()) {
                val idx = line.indexOf(':')
                if (idx > 0) map[line.substring(0, idx)] = line.substring(idx + 1)
            }
        }
        map.putAll(options)
        optFile.parentFile?.mkdirs()
        optFile.bufferedWriter().use { writer ->
            for ((key, value) in map) writer.write("$key:$value\n")
        }
    }
}
