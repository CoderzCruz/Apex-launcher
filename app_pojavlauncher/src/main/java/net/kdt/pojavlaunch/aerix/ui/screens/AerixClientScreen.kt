package net.kdt.pojavlaunch.aerix.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixClientServers
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLaunchBridge
import net.kdt.pojavlaunch.aerix.client.AerixClientInstaller
import net.kdt.pojavlaunch.aerix.client.AerixClientPackGenerator
import net.kdt.pojavlaunch.aerix.client.ClientConfigProfiles
import net.kdt.pojavlaunch.aerix.client.ClientVersionSpec
import net.kdt.pojavlaunch.aerix.client.AerixClientStore
import net.kdt.pojavlaunch.aerix.components.GlassPanel
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.ui.AerixNav
import net.kdt.pojavlaunch.aerix.ui.LauncherSection
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.utils.jre.GameRunner

/**
 * Minimal Liquid Glass `Aerix Client` Sidebar Tab:
 * Transparent backdrop over the launcher's live wallpaper, showing ONLY `Aerix Client`
 * and Liquid Glass client version download / launch cards below it.
 */
/**
 * One CLIENT MENU button: angular crimson face, icon + label, and a one-line promise of what it
 * opens. Every button is wired to a real destination — no decorative tiles.
 */
@Composable
private fun ClientMenuButton(
    label: String,
    caption: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFE21F3D), Color(0xFF9E1728))))
            .border(AerixSpacing.hairline, Color(0x66FF304F), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = AerixSpacing.mdTight, vertical = AerixSpacing.smPlus)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.5.sp,
                letterSpacing = 0.9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Text(
            text = caption,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 8.5.sp,
            maxLines = 2,
            modifier = Modifier.padding(top = AerixSpacing.xxs)
        )
    }
}

/**
 * MULTIPLAYER for the in-launcher client: saved servers, a REAL status ping and a one-tap join.
 * The join only arms the launcher's normal pending-server path — no fake "connected" state.
 */
@Composable
private fun ClientServersSection(accent: Color, onJoin: (String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { AerixClientServers.init(context) } }
    val servers = AerixClientServers.servers
    val pings = remember { mutableStateMapOf<String, AerixClientServers.Ping?>() }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    // Ping every saved server in parallel, once per list change (real handshake, ~2.5s timeout).
    LaunchedEffect(servers) {
        coroutineScope {
            servers.forEach { server ->
                launch { pings[server.address] = AerixClientServers.ping(server.address) }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MULTIPLAYER",
                fontSize = 9.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = "real ping · one-tap join",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.55f)
            )
        }

        servers.forEach { server ->
            val ping = pings[server.address]
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = AerixRadii.card,
                color = AerixSurface.panelRaised,
                contentPadding = AerixSpacing.mdTight
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = server.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = server.address,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                        Text(
                            text = when {
                                ping == null -> "checking…"
                                ping.online -> "\u25CF " + ping.label
                                else -> "\u25CB " + ping.label
                            },
                            color = if (ping?.online == true) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.45f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(accent)
                            .clickable { onJoin(server.address) }
                            .padding(horizontal = AerixSpacing.mdTight, vertical = AerixSpacing.smCompact)
                    ) {
                        Text(
                            text = "JOIN",
                            color = Color.White,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.aerix_ic_close),
                        contentDescription = "Remove " + server.name,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { AerixClientServers.remove(server.address) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AerixRadii.card))
                .border(AerixSpacing.hairline, accent.copy(alpha = 0.55f), RoundedCornerShape(AerixRadii.card))
                .clickable {
                    newName = ""
                    newAddress = ""
                    addError = null
                    adding = true
                }
                .padding(vertical = AerixSpacing.smPlus),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ ADD SERVER",
                color = accent,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            containerColor = AerixSurface.panelRaised,
            title = { Text("Add server", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        singleLine = true,
                        label = { Text("Server name", color = AerixSurface.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = AerixSurface.borderSoft,
                            cursorColor = accent
                        )
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        singleLine = true,
                        label = { Text("Address (host or host:port)", color = AerixSurface.textSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = AerixSurface.borderSoft,
                            cursorColor = accent
                        )
                    )
                    addError?.let { message ->
                        Text(text = message, color = Color(0xFFFF6B6B), fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when {
                            AerixClientServers.normalizeAddress(newAddress) == null ->
                                addError = "Address galat hai — host ya host:port likho"
                            !AerixClientServers.add(newName, newAddress) ->
                                addError = "Ye server already list me hai"
                            else -> adding = false
                        }
                    }
                ) {
                    Text("ADD", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { adding = false }) {
                    Text("CANCEL", color = AerixSurface.textSecondary)
                }
            }
        )
    }
}

/** One module row: label + live store state + toggle. */
private class ModuleChip(val label: String, val enabled: Boolean, val toggle: () -> Unit)

@Composable
private fun ClientModuleChip(label: String, enabled: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AerixRadii.control)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (enabled) Color(0x33DC2626) else AerixSurface.panelRaised)
            .border(
                AerixSpacing.hairline,
                if (enabled) AerixSurface.borderHighlight else AerixSurface.borderSoft,
                shape
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.72f),
                fontSize = 10.5.sp,
                fontWeight = if (enabled) FontWeight.ExtraBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(width = 30.dp, height = 16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (enabled) AerixSurface.accent else Color.White.copy(alpha = 0.14f))
                    .border(
                        0.8.dp,
                        if (enabled) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.22f),
                        RoundedCornerShape(999.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(if (enabled) Alignment.CenterEnd else Alignment.CenterStart)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientModuleSection(title: String, chips: List<ModuleChip>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White.copy(alpha = 0.6f)
        )
        chips.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pair.forEach { chip ->
                    ClientModuleChip(chip.label, chip.enabled, chip.toggle, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClientPresetPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B))))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** Compact version card for the custom client grid: version, tier, selected ring. */
@Composable
private fun ClientVersionTile(
    spec: ClientVersionSpec,
    installed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.card)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    if (selected) listOf(Color(0x45DC2626), Color(0x2D152536))
                    else listOf(AerixSurface.panelRaised, AerixSurface.panelRaised)
                )
            )
            .border(
                if (selected) 1.5.dp else AerixSpacing.hairline,
                Brush.horizontalGradient(
                    if (selected) listOf(Color(0xE0DC2626), Color(0x73DC2626))
                    else listOf(AerixSurface.borderSoft, AerixSurface.borderSoft)
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = spec.mcVersion,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (spec.mcVersion.startsWith("26.")) "Experimental · Java 25" else "Fabric · optimized",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 8.5.sp,
                    maxLines = 1
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (installed) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x33DC2626))
                            .border(0.8.dp, AerixSurface.borderHighlight, RoundedCornerShape(999.dp))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = "INSTALLED",
                            color = Color.White,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Color(0xFFDC2626) else Color.Transparent)
                        .border(
                            1.4.dp,
                            if (selected) Color(0xFFDC2626) else Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(999.dp)
                        )
                )
            }
        }
    }
}

/** Sticky bottom dock: selected version + primary Download/Launch action. */
@Composable
private fun ClientActionDock(
    selectedVersion: String,
    downloading: Boolean,
    progress: Float,
    installed: Boolean,
    launching: Boolean,
    busy: Boolean,
    onDownload: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.card)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xF2171920), Color(0xF222242C))))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xE0DC2626), Color(0x55DC2626), Color(0xE0DC2626))),
                shape
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SELECTED",
                    fontSize = 7.5.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AerixSurface.accent
                )
                Text(
                    text = "Vortex Client $selectedVersion",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (installed && !downloading) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.9.dp, AerixSurface.borderSoft, RoundedCornerShape(999.dp))
                            .clickable(enabled = !busy) { onDownload() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "\u21bb",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B))))
                        .clickable(enabled = !busy) {
                            if (installed) onLaunch() else onDownload()
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = when {
                            downloading -> "Downloading ${(progress * 100).toInt()}%"
                            launching -> "Syncing..."
                            installed -> "\u25b6 Launch"
                            else -> "\u2193 Download"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/** Curated Vortex pack card: title + details + one gradient install button. */
@Composable
private fun ClientPackCard(
    title: String,
    subtitle: String,
    caption: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.card)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0x33DC2626), AerixSurface.panelRaised)
                )
            )
            .border(
                1.4.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xE0DC2626), Color(0x55DC2626))
                ),
                shape
            )
            .padding(AerixSpacing.smPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.74f)
            )
            Text(
                text = caption,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.52f)
            )
        }
        val btnShape = RoundedCornerShape(999.dp)
        Box(
            modifier = Modifier
                .clip(btnShape)
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(
                            listOf(Color(0xFFDC2626), Color(0xFFB01B2B))
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                AerixSurface.panelRaised,
                                AerixSurface.panelRaised
                            )
                        )
                    }
                )
                .border(
                    AerixSpacing.hairline,
                    if (enabled) AerixSurface.borderHighlight else AerixSurface.borderSoft,
                    btnShape
                )
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = buttonLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun AerixClientScreen(activity: FragmentActivity) {
    LaunchedEffect(Unit) {
        AerixClientStore.init(activity)
    }

    var refreshTick by remember { mutableIntStateOf(0) }
    var preparingLaunchVer by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val accent = AerixSurface.accent
    // The page is one long scroll; the CLIENT MENU jumps to its own sections with real anchors.
    val scrollState = rememberScrollState()
    var viewportTop by remember { mutableIntStateOf(0) }
    var modsTop by remember { mutableIntStateOf(0) }
    var profilesTop by remember { mutableIntStateOf(0) }
    var serversTop by remember { mutableIntStateOf(0) }
    val scrollToAnchor: (Int) -> Unit = { anchorTop ->
        val target = (scrollState.value + (anchorTop - viewportTop) - 90).coerceAtLeast(0)
        scope.launch { scrollState.animateScrollTo(target) }
    }
    var selectedVersion by remember {
        mutableStateOf(
            AerixClientStore.selectedClientVersion
                .takeIf { v -> AerixClientInstaller.availableVersions.any { it.mcVersion == v } }
                ?: AerixClientInstaller.availableVersions.first().mcVersion
        )
    }
    // Load the instance list once, rather than scanning disk for every version card.
    val installedClients = remember(refreshTick, AerixClientInstaller.isInstalling) {
        runCatching { Instances.loadAllInstances() }.getOrDefault(emptyList())
    }
    val selectedInstalled = remember(selectedVersion, installedClients) {
        AerixClientInstaller.findInstalledClientInstance(selectedVersion, installedClients)
    }
    // One-tap config profile: merge preset video settings into the instance's options.txt.
    val applyConfigProfile: (ClientConfigProfiles.Profile) -> Unit = { profile ->
        val target = selectedInstalled?.gameDirectory
        if (target == null) {
            Toast.makeText(activity, "Pehle Vortex Client instance install karo", Toast.LENGTH_LONG).show()
        } else {
            scope.launch {
                val ok = runCatching { ClientConfigProfiles.applyOptions(target, profile.options) }.isSuccess
                if (ok) {
                    Toast.makeText(activity, "${profile.title} profile applied — next launch se active", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(activity, "${profile.title} profile apply failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    // In-game title screen: apply menu options (parallax) or rebuild the whole pack.
    val applyMenuOptions: () -> Unit = {
        val target = selectedInstalled?.gameDirectory
        if (target == null) {
            Toast.makeText(activity, "Pehle Vortex Client instance install karo", Toast.LENGTH_LONG).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching { AerixClientPackGenerator.applyClientOptionsTxt(target, selectedVersion) }.isSuccess
                }
                Toast.makeText(
                    activity,
                    if (ok) "Menu settings saved — next launch se apply honge" else "Menu settings apply failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val rebuildMenuPack: () -> Unit = {
        val target = selectedInstalled?.gameDirectory
        if (target == null) {
            Toast.makeText(activity, "Pehle Vortex Client instance install karo", Toast.LENGTH_LONG).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AerixClientPackGenerator.buildAndApplyToInstance(activity, target, selectedVersion)
                        AerixClientPackGenerator.ensurePackUpToDate(activity, target, selectedVersion)
                    }.isSuccess
                }
                refreshTick++
                Toast.makeText(
                    activity,
                    if (ok) "Vortex Red Forest menu rebuilt" else "Menu pack rebuild failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // One-tap Vortex mod packs: install into the client instance, then refresh + toast counts.
    val startPackInstall: (String, List<String>, List<String>) -> Unit = { title, slugs, purgePrefixes ->
        scope.launch {
            val res = AerixClientInstaller.installVortexPack(activity, title, slugs, purgePrefixes)
            refreshTick++
            PojavInstanceBridge.reload()
            res.onSuccess { r ->
                val failedText = if (r.failed.isEmpty()) "" else " · failed: ${r.failed.joinToString(", ")}"
                val hint = if (r.failed.any { "no build" in it }) " — 1.21.11 instance pe try karo" else ""
                Toast.makeText(activity, "$title: ${r.installed} mods installed$failedText$hint", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(activity, err.message ?: "Pack install failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AerixSpacing.md)
                .onGloballyPositioned { viewportTop = it.positionInWindow().y.toInt() }
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
        ) {
        // Big showcase hero (Feather/RKMC-style client landing).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AerixRadii.card))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0x80DC2626), Color(0x2D152536), Color(0x59B01B2B))
                    )
                )
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0xE0DC2626), Color(0x55DC2626), Color(0xE0DC2626))
                    ),
                    RoundedCornerShape(AerixRadii.card)
                )
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_px_fps),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "VORTEX CLIENT",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = "Java Edition client for Android — free forever",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("FPS Boost", "Capes", "HUD", "Gyro").forEach { chip ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(0.9.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = chip,
                                    color = Color.White,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Supported versions strip — every release the client ships for.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "SUPPORTED",
                fontSize = 8.5.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AerixClientInstaller.availableVersions.forEachIndexed { index, spec ->
                    val latest = index == 0
                    val shape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(
                                Brush.horizontalGradient(
                                    if (latest) listOf(Color(0xFFDC2626), Color(0xFFB01B2B))
                                    else listOf(AerixSurface.panelRaised, AerixSurface.panelRaised)
                                )
                            )
                            .border(
                                AerixSpacing.hairline,
                                if (spec.mcVersion.startsWith("26.") || latest) AerixSurface.borderHighlight
                                else AerixSurface.borderSoft,
                                shape
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = spec.mcVersion,
                            fontSize = 9.sp,
                            fontWeight = if (latest) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (latest) Color.White else Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }
        }

        // ── CLIENT MENU — the Vortex Client, right here in the launcher ──
        // Every button opens something real: Library, the server list below, the mod packs,
        // the cape shop, the config profiles, Settings, Accounts and the music engine.
        Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CLIENT MENU",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Text(
                    text = "Welcome back, " + (PojavAccountBridge.current?.username ?: "Player"),
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientMenuButton(
                    label = "SINGLEPLAYER",
                    caption = "Your worlds · Library",
                    iconRes = R.drawable.aerix_ic_dashboard_outlined,
                    modifier = Modifier.weight(1f)
                ) { AerixNav.navigate(LauncherSection.LIBRARY) }
                ClientMenuButton(
                    label = "MULTIPLAYER",
                    caption = "Ping + one-tap join",
                    iconRes = R.drawable.aerix_ic_style_outlined,
                    modifier = Modifier.weight(1f)
                ) { scrollToAnchor(serversTop) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientMenuButton(
                    label = "MODS",
                    caption = "Vortex Modpack · Vulkan",
                    iconRes = R.drawable.ic_px_fps,
                    modifier = Modifier.weight(1f)
                ) { scrollToAnchor(modsTop) }
                ClientMenuButton(
                    label = "COSMETICS",
                    caption = "Capes & cosmetics",
                    iconRes = R.drawable.aerix_ic_person_outlined,
                    modifier = Modifier.weight(1f)
                ) { AerixNav.navigate(LauncherSection.CAPE_SHOP) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientMenuButton(
                    label = "PERFORMANCE",
                    caption = "PvP · FPS · Builder",
                    iconRes = R.drawable.aerix_ic_video_settings,
                    modifier = Modifier.weight(1f)
                ) { scrollToAnchor(profilesTop) }
                ClientMenuButton(
                    label = "MUSIC",
                    caption = "Play while you play",
                    iconRes = R.drawable.aerix_ic_music_note,
                    modifier = Modifier.weight(1f)
                ) { AerixNav.navigate(LauncherSection.MUSIC) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientMenuButton(
                    label = "ACCOUNTS",
                    caption = "Microsoft · Ely.by",
                    iconRes = R.drawable.aerix_ic_person_outlined,
                    modifier = Modifier.weight(1f)
                ) { AerixNav.navigate(LauncherSection.ACCOUNTS) }
                ClientMenuButton(
                    label = "SETTINGS",
                    caption = "Renderer · Java · Controls",
                    iconRes = R.drawable.aerix_ic_settings_filled,
                    modifier = Modifier.weight(1f)
                ) { AerixNav.navigate(LauncherSection.SETTINGS) }
            }
        }

        // ── MAIN MENU: the in-game title screen (VORTEX RED FOREST) ──
        // Texture work only — every label is still drawn by Minecraft, so SINGLEPLAYER,
        // MULTIPLAYER, REALMS, MODS, OPTIONS and QUIT keep their real vanilla screens.
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            radius = AerixRadii.card,
            color = AerixSurface.panelRaised
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MAIN MENU",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Text(
                    text = "in-game title screen",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Text(
                text = "VORTEX RED FOREST",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Red autumn forest panorama, crimson VORTEX logo and dark-red buttons. " +
                    "Minecraft ke apne screens: Singleplayer, Multiplayer, Realms, Mods, Options, Quit.",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 10.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
                // onToggle is passed by name: the trailing lambda would otherwise bind to modifier.
                ClientModuleChip(
                    label = "Menu parallax",
                    enabled = AerixClientStore.packMenuParallax,
                    onToggle = {
                        AerixClientStore.packMenuParallax = !AerixClientStore.packMenuParallax
                        AerixClientStore.persist()
                        applyMenuOptions()
                    },
                    modifier = Modifier.weight(1f)
                )
                ClientModuleChip(
                    label = "Crimson menu",
                    enabled = AerixClientStore.packCustomMainMenu,
                    onToggle = {
                        AerixClientStore.packCustomMainMenu = !AerixClientStore.packCustomMainMenu
                        AerixClientStore.persist()
                        rebuildMenuPack()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE21F3D), Color(0xFF9E1728))))
                    .clickable { rebuildMenuPack() }
                    .padding(vertical = AerixSpacing.smPlus),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "REBUILD MENU PACK",
                    color = Color.White,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // ── MULTIPLAYER: the client's server list, inside the launcher ──
        Box(modifier = Modifier.onGloballyPositioned { serversTop = it.positionInWindow().y.toInt() }) {
            ClientServersSection(
                accent = accent,
                onJoin = { address ->
                    val installed = selectedInstalled
                    if (installed != null) {
                        Instances.setSelectedInstance(installed)
                        PojavInstanceBridge.reload()
                    }
                    if (PojavInstanceBridge.selected != null) {
                        GameRunner.setPendingServerAddress(address)
                        PojavLaunchBridge.play(activity)
                    } else {
                        Toast.makeText(
                            activity,
                            "Pehle ek instance banao — Library me Add Version se",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }

        // Active download progress banner in Liquid Glass
        if (AerixClientInstaller.isInstalling) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = AerixRadii.card,
                strength = 1.0f,
                color = AerixSurface.panelRaised
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AerixClientInstaller.statusHeadline,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(AerixClientInstaller.progressFraction * 100).toInt()}%",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { AerixClientInstaller.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.16f)
                )
            }
        }

        AerixClientInstaller.lastError?.takeIf { it.isNotBlank() && !AerixClientInstaller.isInstalling }?.let { err ->
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                radius = AerixRadii.card,
                color = AerixSurface.errorDarkContainer.copy(alpha = 0.55f)
            ) {
                Text(text = err, color = Color.White, fontSize = 12.sp)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOWNLOADS",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
                Text(
                    text = "${AerixClientInstaller.availableVersions.size} versions · newest first",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            AerixClientInstaller.availableVersions.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
                ) {
                    pair.forEach { spec ->
                        val isInstalled = remember(spec.mcVersion, installedClients, refreshTick) {
                            AerixClientInstaller.findInstalledClientInstance(spec.mcVersion, installedClients)
                        }
                        ClientVersionTile(
                            spec = spec,
                            installed = isInstalled != null,
                            selected = spec.mcVersion == selectedVersion,
                            onClick = {
                                selectedVersion = spec.mcVersion
                                AerixClientStore.selectedClientVersion = spec.mcVersion
                                AerixClientStore.persist()
                                if (isInstalled != null) {
                                    Instances.setSelectedInstance(isInstalled)
                                    PojavInstanceBridge.reload()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // ── Modules: real client toggles, saved for the next launch ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODULES",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        text = "saved for your next launch",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                // One-tap loadout presets.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ClientPresetPill("PvP") {
                        AerixClientStore.toggleSprintEnabled = true
                        AerixClientStore.toggleSneakEnabled = true
                        AerixClientStore.hitmarkerEnabled = true
                        AerixClientStore.comboCounterEnabled = true
                        AerixClientStore.swordCooldownBarEnabled = true
                        AerixClientStore.autoGgEnabled = true
                        AerixClientStore.quickPvpButtonsEnabled = true
                        AerixClientStore.crosshairEnabled = true
                        AerixClientStore.persist()
                        Toast.makeText(activity, "PvP preset applied", Toast.LENGTH_SHORT).show()
                    }
                    ClientPresetPill("Survival") {
                        AerixClientStore.coordsHudEnabled = true
                        AerixClientStore.armorHudEnabled = true
                        AerixClientStore.effectsHudEnabled = true
                        AerixClientStore.healthHudEnabled = true
                        AerixClientStore.pingHudEnabled = true
                        AerixClientStore.hitmarkerEnabled = false
                        AerixClientStore.comboCounterEnabled = false
                        AerixClientStore.quickPvpButtonsEnabled = false
                        AerixClientStore.persist()
                        Toast.makeText(activity, "Survival preset applied", Toast.LENGTH_SHORT).show()
                    }
                    ClientPresetPill("FPS") {
                        AerixClientStore.perfPillEnabled = true
                        AerixClientStore.adaptiveDistanceEnabled = true
                        AerixClientStore.fullbrightEnabled = true
                        AerixClientStore.wavingCapeEnabled = false
                        AerixClientStore.packEnhancedGlint = false
                        AerixClientStore.persist()
                        Toast.makeText(activity, "FPS preset applied", Toast.LENGTH_SHORT).show()
                    }
                }

                ClientModuleSection(
                    title = "COMBAT & GAMEPLAY",
                    chips = listOf(
                        ModuleChip("Toggle sprint", AerixClientStore.toggleSprintEnabled) {
                            AerixClientStore.toggleSprintEnabled = !AerixClientStore.toggleSprintEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Toggle sneak", AerixClientStore.toggleSneakEnabled) {
                            AerixClientStore.toggleSneakEnabled = !AerixClientStore.toggleSneakEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Hitmarker", AerixClientStore.hitmarkerEnabled) {
                            AerixClientStore.hitmarkerEnabled = !AerixClientStore.hitmarkerEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Combo counter", AerixClientStore.comboCounterEnabled) {
                            AerixClientStore.comboCounterEnabled = !AerixClientStore.comboCounterEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Sword cooldown", AerixClientStore.swordCooldownBarEnabled) {
                            AerixClientStore.swordCooldownBarEnabled = !AerixClientStore.swordCooldownBarEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Auto GG", AerixClientStore.autoGgEnabled) {
                            AerixClientStore.autoGgEnabled = !AerixClientStore.autoGgEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Quick PvP buttons", AerixClientStore.quickPvpButtonsEnabled) {
                            AerixClientStore.quickPvpButtonsEnabled = !AerixClientStore.quickPvpButtonsEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Crosshair", AerixClientStore.crosshairEnabled) {
                            AerixClientStore.crosshairEnabled = !AerixClientStore.crosshairEnabled
                            AerixClientStore.persist()
                        }
                    )
                )

                ClientModuleSection(
                    title = "HUD & STATS",
                    chips = listOf(
                        ModuleChip("Coordinates", AerixClientStore.coordsHudEnabled) {
                            AerixClientStore.coordsHudEnabled = !AerixClientStore.coordsHudEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Armor HUD", AerixClientStore.armorHudEnabled) {
                            AerixClientStore.armorHudEnabled = !AerixClientStore.armorHudEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Effects", AerixClientStore.effectsHudEnabled) {
                            AerixClientStore.effectsHudEnabled = !AerixClientStore.effectsHudEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Health", AerixClientStore.healthHudEnabled) {
                            AerixClientStore.healthHudEnabled = !AerixClientStore.healthHudEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Server & ping", AerixClientStore.pingHudEnabled) {
                            AerixClientStore.pingHudEnabled = !AerixClientStore.pingHudEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Perf pill", AerixClientStore.perfPillEnabled) {
                            AerixClientStore.perfPillEnabled = !AerixClientStore.perfPillEnabled
                            AerixClientStore.persist()
                        }
                    )
                )

                ClientModuleSection(
                    title = "VISUAL",
                    chips = listOf(
                        ModuleChip("Low fire", AerixClientStore.packLowFire) {
                            AerixClientStore.packLowFire = !AerixClientStore.packLowFire
                            AerixClientStore.persist()
                        },
                        ModuleChip("Clear water", AerixClientStore.packClearWater) {
                            AerixClientStore.packClearWater = !AerixClientStore.packClearWater
                            AerixClientStore.persist()
                        },
                        ModuleChip("Enchanted glint", AerixClientStore.packEnhancedGlint) {
                            AerixClientStore.packEnhancedGlint = !AerixClientStore.packEnhancedGlint
                            AerixClientStore.persist()
                        },
                        ModuleChip("Fullbright", AerixClientStore.fullbrightEnabled) {
                            AerixClientStore.fullbrightEnabled = !AerixClientStore.fullbrightEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Wavey cape", AerixClientStore.wavingCapeEnabled) {
                            AerixClientStore.wavingCapeEnabled = !AerixClientStore.wavingCapeEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Streamer mode", AerixClientStore.streamerPrivacyEnabled) {
                            AerixClientStore.streamerPrivacyEnabled = !AerixClientStore.streamerPrivacyEnabled
                            AerixClientStore.persist()
                        }
                    )
                )

                ClientModuleSection(
                    title = "TOUCH & AIM",
                    chips = listOf(
                        ModuleChip("Touch aim", AerixClientStore.touchAimEnabled) {
                            AerixClientStore.touchAimEnabled = !AerixClientStore.touchAimEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Gyro aim", AerixClientStore.gyroAdsEnabled) {
                            AerixClientStore.gyroAdsEnabled = !AerixClientStore.gyroAdsEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Hotbar swipe", AerixClientStore.hotbarSwipeEnabled) {
                            AerixClientStore.hotbarSwipeEnabled = !AerixClientStore.hotbarSwipeEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("Hold zoom", AerixClientStore.quickZoomEnabled) {
                            AerixClientStore.quickZoomEnabled = !AerixClientStore.quickZoomEnabled
                            AerixClientStore.persist()
                        },
                        ModuleChip("180\u00b0 flick", AerixClientStore.quickFlickEnabled) {
                            AerixClientStore.quickFlickEnabled = !AerixClientStore.quickFlickEnabled
                            AerixClientStore.persist()
                        }
                    )
                )
                // ── VORTEX MODS: one-tap curated packs for this instance ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { modsTop = it.positionInWindow().y.toInt() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VORTEX MODS",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        text = "one-tap packs · installs into this instance",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
                ClientPackCard(
                    title = "Vortex Modpack",
                    subtitle = "FPS boost · Sodium, Lithium, Ferritecore, ImmediatelyFast, EntityCulling, Dynamic FPS, ModernFix, Krypton",
                    caption = "All the FPS mods in one tap. Removes the Vulkan renderer if present (renderers conflict). FPS depends on your device.",
                    buttonLabel = "Install FPS Pack",
                    enabled = !AerixClientInstaller.isInstalling && preparingLaunchVer == null,
                    onClick = {
                        startPackInstall(
                            "Vortex Modpack",
                            listOf(
                                "sodium", "lithium", "ferritecore", "immediatelyfast",
                                "entityculling", "dynamic-fps", "modernfix", "krypton"
                            ),
                            listOf("vulkan")
                        )
                    }
                )
                ClientPackCard(
                    title = "Vortex Vulkan",
                    subtitle = "Vorkan · VulkanMod replaces OpenGL with a Vulkan backend (big FPS gains on supported devices)",
                    caption = "Removes Sodium/Indium/Nvidium/Iris first — two renderers cannot run together. VulkanMod supports 26.x / 1.21.x.",
                    buttonLabel = "Install Vulkan",
                    enabled = !AerixClientInstaller.isInstalling && preparingLaunchVer == null,
                    onClick = {
                        startPackInstall(
                            "Vortex Vulkan",
                            listOf("vulkanmod"),
                            listOf("sodium", "indium", "nvidium", "iris")
                        )
                    }
                )
                // ── CONFIG PROFILES: one-tap video settings for this instance ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { profilesTop = it.positionInWindow().y.toInt() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONFIG PROFILES",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        text = "video settings · next launch se active",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
                ClientConfigProfiles.PROFILES.forEach { profile ->
                    ClientPackCard(
                        title = profile.title,
                        subtitle = profile.caption,
                        caption = "options.txt me merge hota hai — baaki sab settings jaisi the waisi rehti hain.",
                        buttonLabel = "Apply",
                        enabled = !AerixClientInstaller.isInstalling && preparingLaunchVer == null && selectedInstalled != null,
                        onClick = { applyConfigProfile(profile) }
                    )
                }
                Spacer(Modifier.height(76.dp))
            }
        }
        ClientActionDock(
            selectedVersion = selectedVersion,
            downloading = AerixClientInstaller.isInstalling && AerixClientInstaller.installingVersion == selectedVersion,
            progress = AerixClientInstaller.progressFraction,
            installed = selectedInstalled != null,
            launching = preparingLaunchVer == selectedVersion,
            busy = AerixClientInstaller.isInstalling || preparingLaunchVer != null,
            onDownload = {
                AerixClientStore.selectedClientVersion = selectedVersion
                AerixClientStore.persist()
                scope.launch {
                    val res = AerixClientInstaller.installClientVersion(activity, selectedVersion)
                    refreshTick++
                    PojavInstanceBridge.reload()
                    res.onSuccess {
                        Toast.makeText(activity, "Vortex Client $selectedVersion ready!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(activity, err.message ?: "Download failed", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onLaunch = {
                if (selectedInstalled != null) {
                    AerixClientStore.selectedClientVersion = selectedVersion
                    AerixClientStore.persist()
                    scope.launch {
                        preparingLaunchVer = selectedVersion
                        val sync = AerixClientInstaller.syncExistingInstanceForLaunch(
                            activity,
                            selectedInstalled,
                            selectedVersion
                        )
                        sync.onFailure { error ->
                            Toast.makeText(activity, "Client update failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                        preparingLaunchVer = null
                        Instances.setSelectedInstance(selectedInstalled)
                        PojavInstanceBridge.reload()
                        PojavLaunchBridge.play(activity)
                    }
                }
            }
        )
    }
}
