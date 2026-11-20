/*
 * Vortex Client — multiplayer server list for the in-launcher client page.
 *
 * The separate Vortex Client app has a multiplayer tab; this is the same idea inside the
 * launcher: saved servers, a REAL Minecraft status ping (handshake + status request), and a
 * one-tap join that arms GameRunner's pending server and launches the selected instance.
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

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Server bookmarks for the client page, persisted as JSON in [AerixPreferences].
 *
 * Nothing is faked here: an entry is only "online" when a real status handshake answered, and
 * the join button launches the game through the launcher's normal pending-server path.
 */
object AerixClientServers {
    /** One saved server: display name + `host[:port]`. */
    data class Server(val name: String, val address: String)

    /** Outcome of a real Minecraft status ping. */
    data class Ping(val online: Boolean, val players: Int, val latencyMs: Int) {
        /** Human line under the server name; "checking…" is drawn while [Ping] is still null. */
        val label: String
            get() = if (online) "$players online · ${latencyMs}ms" else "offline / not answering"
    }

    private const val DEFAULT_PORT = 25565
    private const val TIMEOUT_MS = 2500
    /** Handshake protocol version (1.6-era value) — every modern server still answers it. */
    private const val PROTOCOL = 79

    /** Live list shown by the client page. */
    var servers by mutableStateOf(listOf<Server>())
        private set

    private var loaded = false

    /**
     * Reads the saved list once. Falls back to the Vortex MC address the launcher already knows
     * (`AerixPreferences.vortexMcServer`) so the list is never empty on a fresh install.
     */
    fun init(context: Context) {
        if (loaded) return
        loaded = true
        // The launcher shell normally initialises AerixPreferences before the UI; guard anyway so
        // a cold render of the client page never reads an unloaded preference file.
        if (AerixPreferences.clientServersJson == null) {
            runCatching { AerixPreferences.init(context.applicationContext) }
        }
        val raw = AerixPreferences.clientServersJson
        servers = if (raw == null) {
            listOf(Server("Vortex MC", AerixPreferences.vortexMcServer))
        } else {
            parse(raw)
        }
    }

    private fun parse(raw: String): List<Server> = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val entry = array.optJSONObject(index) ?: return@mapNotNull null
            val address = normalizeAddress(entry.optString("address")) ?: return@mapNotNull null
            val name = entry.optString("name").trim().ifBlank { address }
            Server(name, address)
        }
    }.getOrDefault(emptyList())

    private fun persist() {
        val array = JSONArray()
        servers.forEach { server ->
            array.put(JSONObject().put("name", server.name).put("address", server.address))
        }
        AerixPreferences.saveClientServersJson(array.toString())
    }

    /** Adds a bookmark; false when the address is unusable or already saved. */
    fun add(name: String, address: String): Boolean {
        val clean = normalizeAddress(address) ?: return false
        if (servers.any { it.address.equals(clean, ignoreCase = true) }) return false
        servers = servers + Server(name.trim().ifBlank { clean }, clean)
        persist()
        return true
    }

    fun remove(address: String) {
        servers = servers.filterNot { it.address == address }
        persist()
    }

    /**
     * `mc.example.net`, `mc.example.net:25566`, `mc://mc.example.net:25566/` and a pasted
     * `https://…` link all end up as `host[:port]`. Null when nothing usable is left.
     */
    fun normalizeAddress(raw: String): String? {
        var value = raw.trim()
        value = value.removePrefix("mc://").removePrefix("https://").removePrefix("http://")
        value = value.substringBefore('/').substringBefore(' ').trim()
        if (value.isBlank() || value.startsWith(":")) return null
        val host = value.substringBefore(':').trim()
        val port = if (value.contains(':')) value.substringAfter(':').toIntOrNull() ?: DEFAULT_PORT else DEFAULT_PORT
        if (host.isBlank() || port !in 1..65535) return null
        return if (port == DEFAULT_PORT) host else "$host:$port"
    }

    /**
     * Real status ping on a background thread: handshake (state 1) → status request → parse the
     * JSON for the online player count, and time the round trip.
     */
    suspend fun ping(address: String): Ping = withContext(Dispatchers.IO) {
        val host = address.substringBefore(':')
        val port = address.substringAfter(':', "").toIntOrNull() ?: DEFAULT_PORT
        val started = System.currentTimeMillis()
        runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
                socket.soTimeout = TIMEOUT_MS
                val out = DataOutputStream(socket.getOutputStream())
                val input = DataInputStream(socket.getInputStream())
                val hostBytes = host.toByteArray(StandardCharsets.UTF_8)
                // Handshake: packet id 0, protocol, host, port, next state = status (1).
                out.writeByte(0x00)
                writeVarInt(out, PROTOCOL)
                writeVarInt(out, hostBytes.size)
                out.write(hostBytes)
                out.writeShort(port)
                writeVarInt(out, 1)
                out.writeByte(0x00) // status request
                out.flush()

                readVarInt(input) // packet length
                val packetId = readVarInt(input)
                if (packetId != 0x00) return@runCatching Ping(false, 0, 0)
                val length = readVarInt(input)
                if (length <= 0 || length > 1 shl 20) return@runCatching Ping(false, 0, 0)
                val payload = ByteArray(length)
                input.readFully(payload)
                val latency = (System.currentTimeMillis() - started).toInt()
                Ping(true, playersFrom(String(payload, StandardCharsets.UTF_8)), latency)
            }
        }.getOrElse { Ping(false, 0, 0) }
    }

    private fun playersFrom(json: String): Int {
        val marker = json.indexOf("\"online\":")
        if (marker < 0) return 0
        var start = marker + "\"online\":".length
        while (start < json.length && json[start] !in '0'..'9') start++
        var end = start
        while (end < json.length && json[end].isDigit()) end++
        return json.substring(start, end).toIntOrNull() ?: 0
    }

    private fun writeVarInt(out: DataOutputStream, value: Int) {
        var remaining = value
        while (true) {
            if (remaining and 0x7F.inv() == 0) {
                out.writeByte(remaining)
                return
            }
            out.writeByte((remaining and 0x7F) or 0x80)
            remaining = remaining ushr 7
        }
    }

    private fun readVarInt(input: DataInputStream): Int {
        var readBytes = 0
        var result = 0
        while (true) {
            val next = input.read()
            if (next < 0) return 0
            result = result or ((next and 0x7F) shl (7 * readBytes))
            readBytes++
            if (readBytes > 5) return 0
            if (next and 0x80 == 0) return result
        }
    }
}
