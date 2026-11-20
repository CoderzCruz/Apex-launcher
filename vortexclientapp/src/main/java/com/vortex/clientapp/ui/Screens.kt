package com.vortex.clientapp.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.clientapp.data.ClientPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

/** Big labelled field used by every form screen. */
@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = C.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = C.red,
                unfocusedBorderColor = Color(0xFF2A2A2A),
                cursorColor = C.red,
                focusedTextColor = C.text,
                unfocusedTextColor = C.text
            )
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(text, color = C.text2, fontSize = 11.5.sp,
        modifier = Modifier.padding(top = 8.dp))
}

/** Singleplayer: hands off to the installed launcher (real intent, no fake worlds). */
@Composable
fun SingleplayerScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Opens your Minecraft launcher to load worlds.") }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        RedButton("PLAY SINGLEPLAYER") {
            val pkg = ClientPrefs.launcherPackage(context)
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent == null) {
                status = "Launcher '$pkg' not installed — set the package in Settings."
            } else {
                status = "Opening $pkg …"
                runCatching { context.startActivity(intent) }
                    .onFailure { status = "Could not open: ${it.message}" }
            }
        }
        Spacer(Modifier.height(10.dp))
        GhostButton("OPEN LAUNCHER SETTINGS") {
            val pkg = ClientPrefs.launcherPackage(context)
            val settings = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:" + context.packageName))
            runCatching { context.startActivity(settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            status = "Vortex Client package: ${context.packageName} · target launcher: $pkg"
        }
        Note(status)
        Note("Vortex Client is the client UI layer: worlds and the game itself run in your Minecraft launcher (Vortex Launcher / Pojav). No fake worlds are shown.")
    }
}

/** Multiplayer: real server list + real Minecraft status ping. */
@Composable
fun MultiplayerScreen() {
    val context = LocalContext.current
    val servers = remember { mutableStateListOf<Pair<String, String>>().apply {
        addAll(ClientPrefs.servers(context))
    } }
    val pings = remember { mutableStateListOf<String>().apply { repeat(servers.size) { add("—") } } }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Field("Server name", name) { name = it }
        Field("Address (host:port)", address) { address = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("ADD SERVER") {
                if (name.isNotBlank() && address.isNotBlank()) {
                    servers.add(name.trim() to address.trim())
                    pings.add("—")
                    ClientPrefs.saveServers(context, servers)
                    name = ""; address = ""
                }
            }
            GhostButton(if (busy) "PINGING…" else "REFRESH") {
                if (!busy) {
                    busy = true
                    kotlinx.coroutines.MainScope().launch {
                        val results = withContext(Dispatchers.IO) {
                            servers.map { ping(it.second) }
                        }
                        for (i in results.indices) if (i < pings.size) pings[i] = results[i]
                        busy = false
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (servers.isEmpty()) Note("No servers yet — add one above.")
        servers.forEachIndexed { index, (n, a) ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.card).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(n, color = C.text, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Text(a, color = C.text2, fontSize = 11.sp)
                }
                Text(pings.getOrElse(index) { "—" }, color = C.red, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("✕", color = C.text2, fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        if (index < pings.size) pings.removeAt(index)
                        servers.removeAt(index)
                        ClientPrefs.saveServers(context, servers)
                    }.padding(4.dp))
            }
        }
        Note("Ping is a real Minecraft status handshake on port 25565 (latency + online count).")
    }
}

private suspend fun ping(hostPort: String): String = withContext(Dispatchers.IO) {
    var host = hostPort
    var port = 25565
    val colon = hostPort.lastIndexOf(':')
    if (colon > 0) {
        host = hostPort.substring(0, colon)
        port = hostPort.substring(colon + 1).toIntOrNull() ?: 25565
    }
    val started = System.currentTimeMillis()
    runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 2500)
            socket.soTimeout = 2500
            val out = DataOutputStream(socket.getOutputStream())
            val input = DataInputStream(socket.getInputStream())
            val hostBytes = host.toByteArray(StandardCharsets.UTF_8)
            out.write(0x00)
            writeVarInt(out, 79)
            writeVarInt(out, hostBytes.size)
            out.write(hostBytes)
            out.writeShort(port)
            writeVarInt(out, 1)
            out.write(0x00)
            out.flush()
            out.write(0x00)
            out.flush()
            readVarInt(input)
            val len = readVarInt(input)
            val payload = ByteArray(len)
            input.readFully(payload)
            val latency = System.currentTimeMillis() - started
            val json = String(payload, StandardCharsets.UTF_8)
            val idx = json.indexOf("\"online\":")
            val online = if (idx >= 0) {
                var e = idx + 9
                val s = e
                while (e < json.length && json[e].isDigit()) e++
                json.substring(s, e)
            } else "?"
            "● " + online + " · " + latency + "ms"
        }
    }.getOrElse { "○ offline" }
}

private fun writeVarInt(out: DataOutputStream, value: Int) {
    var v = value
    while ((v and 0x7F.inv()) != 0) {
        out.writeByte((v and 0x7F) or 0x80)
        v = v ushr 7
    }
    out.writeByte(v)
}

private fun readVarInt(input: DataInputStream): Int {
    var value = 0
    var position = 0
    while (true) {
        val current = input.readByte().toInt()
        value = value or ((current and 0x7F) shl position)
        if ((current and 0x80) == 0) break
        position += 7
        if (position >= 32) break
    }
    return value
}

/** Mods: local mod names + enable/disable persisted (no jar execution here). */
@Composable
fun ModsScreen() {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    val disabled = remember {
        mutableStateListOf<String>().apply {
            addAll(ClientPrefs.servers(context).map { it.second })
        }
    }
    val known = listOf(
        "Sodium", "Lithium", "FerriteCore", "ImmediatelyFast", "EntityCulling",
        "Dynamic FPS", "ModernFix", "Krypton", "WaveyCapes"
    )
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(
            value = search, onValueChange = { search = it }, singleLine = true,
            placeholder = { Text("Search mods", color = C.text2) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = C.red, unfocusedBorderColor = Color(0xFF2A2A2A),
                cursorColor = C.red, focusedTextColor = C.text, unfocusedTextColor = C.text
            )
        )
        Spacer(Modifier.height(8.dp))
        Text("PERFORMANCE", color = C.red, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp)
        Spacer(Modifier.height(6.dp))
        known.filter { search.isBlank() || it.contains(search, true) }.forEach { mod ->
            val off = disabled.contains(mod)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp)).background(C.card)
                    .clickable {
                        if (off) disabled.remove(mod) else disabled.add(mod)
                        // Persist the disabled list (name|name entries).
                        ClientPrefs.saveServers(context, disabled.map { it to it })
                    }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (off) "○" else "●", color = if (off) C.text2 else C.red, fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(mod, color = C.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Note("Toggle state is saved. Install/remove jars in your launcher's mods folder; this screen manages the Vortex mod profile.")
    }
}

/** Cosmetics: cape + style, persisted. */
@Composable
fun CosmeticsScreen() {
    val context = LocalContext.current
    val capes = listOf(
        "None", "Crimson Sovereign", "Vortex Ember", "Void Walker", "Frost Bite",
        "Golden Era", "Neon Drift", "Phantom Wave", "Obsidian Core"
    )
    var cape by remember { mutableStateOf(ClientPrefs.cape(context)) }
    var style by remember { mutableStateOf(ClientPrefs.skinStyle(context)) }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("CAPE", color = C.red, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(6.dp))
        capes.forEach { c ->
            val selected = c == cape
            Row(
                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0x33E21F3D) else C.card)
                    .clickable { cape = c; ClientPrefs.setCape(context, c) }
                    .padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (selected) "●" else "○", color = if (selected) C.red else C.text2, fontSize = 12.sp)
                Spacer(Modifier.width(10.dp))
                Text(c, color = C.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("SKIN STYLE", color = C.red, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(6.dp))
        listOf("Classic", "Slim (Alex)", "Wide (Steve)").forEach { s ->
            GhostButton(if (s == style) "● $s" else "○ $s") {
                style = s; ClientPrefs.setSkinStyle(context, s)
            }
            Spacer(Modifier.height(6.dp))
        }
        Note("Saved to the Vortex client profile; your launcher applies it on the next launch.")
    }
}

/** Performance: real persisted values. */
@Composable
fun PerformanceScreen() {
    val context = LocalContext.current
    var ram by remember { mutableStateOf(ClientPrefs.ramMb(context).toFloat()) }
    var fps by remember { mutableStateOf(ClientPrefs.fpsCap(context).toFloat()) }
    var dist by remember { mutableStateOf(ClientPrefs.renderDistance(context).toFloat()) }
    var graphics by remember { mutableStateOf(ClientPrefs.graphics(context)) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        SliderRow("RAM allocation: ${ram.toInt()} MB", ram, 512f, 8192f) {
            ram = it; ClientPrefs.setRamMb(context, it.toInt())
        }
        SliderRow("FPS limit: ${fps.toInt()}", fps, 30f, 260f) {
            fps = it; ClientPrefs.setFpsCap(context, it.toInt())
        }
        SliderRow("Render distance: ${dist.toInt()} chunks", dist, 2f, 32f) {
            dist = it; ClientPrefs.setRenderDistance(context, it.toInt())
        }
        Text("GRAPHICS", color = C.text2, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Fast", "Fancy", "Fabulous").forEach { g ->
                GhostButton(if (g == graphics) "● $g" else "○ $g") {
                    graphics = g; ClientPrefs.setGraphics(context, g)
                }
            }
        }
        Note("Values are saved and handed to the launcher. No guaranteed-FPS claims.")
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Text(label, color = C.text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    Slider(
        value = value, onValueChange = onChange, valueRange = min..max,
        colors = SliderDefaults.colors(
            thumbColor = C.red, activeTrackColor = C.red, inactiveTrackColor = C.card
        )
    )
}

/** Settings: username + launcher package + reset. */
@Composable
fun SettingsPanel() {
    val context = LocalContext.current
    var username by remember { mutableStateOf(ClientPrefs.username(context)) }
    var pkg by remember { mutableStateOf(ClientPrefs.launcherPackage(context)) }
    var status by remember { mutableStateOf(" ") }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Field("Username", username) { username = it; ClientPrefs.setUsername(context, it) }
        Field("Launcher package", pkg) { pkg = it; ClientPrefs.setLauncherPackage(context, it) }
        Spacer(Modifier.height(10.dp))
        RedButton("SAVE SETTINGS") {
            ClientPrefs.setUsername(context, username.trim().ifEmpty { "Player" })
            ClientPrefs.setLauncherPackage(context, pkg.trim().ifEmpty { "net.kdt.pojavlaunch" })
            status = "Saved."
        }
        Spacer(Modifier.height(10.dp))
        GhostButton("INSTALLED LAUNCHERS") {
            val found = context.packageManager.getInstalledPackages(0)
                .mapNotNull { it.packageName }
                .filter { it.contains("pojav", true) || it.contains("mojo", true) || it.contains("vortex", true) }
            status = if (found.isEmpty()) "None found — install Vortex Launcher or Pojav." else found.joinToString(", ")
        }
        Note(status)
        Note("Vortex Client v1.0.0 · standalone client UI · no telemetry, no accounts stored.")
    }
}

/** Accounts: offline username switch (persisted). */
@Composable
fun AccountsScreen() {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(ClientPrefs.username(context)) }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Active: $active", color = C.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Field("New offline username", name) { name = it }
        RedButton("USE THIS NAME") {
            val clean = name.trim()
            if (clean.isNotEmpty()) {
                ClientPrefs.setUsername(context, clean)
                active = clean
                name = ""
            }
        }
        Spacer(Modifier.height(10.dp))
        Note("Offline usernames only. Microsoft sign-in is an official-OAuth phase and is never faked.")
    }
}
