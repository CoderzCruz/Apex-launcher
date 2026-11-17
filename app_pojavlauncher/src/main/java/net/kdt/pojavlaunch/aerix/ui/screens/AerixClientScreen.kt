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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLaunchBridge
import net.kdt.pojavlaunch.aerix.client.AerixClientInstaller
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.client.AerixClientCosmetics
import net.kdt.pojavlaunch.aerix.client.ClientCapeSpec
import net.kdt.pojavlaunch.aerix.client.ClientConfigProfiles
import net.kdt.pojavlaunch.aerix.client.ClientVersionSpec
import net.kdt.pojavlaunch.aerix.client.AerixClientStore
import net.kdt.pojavlaunch.aerix.components.GlassPanel
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.instances.Instances

/**
 * Minimal Liquid Glass `Aerix Client` Sidebar Tab:
 * Transparent backdrop over the launcher's live wallpaper, showing ONLY `Aerix Client`
 * and Liquid Glass client version download / launch cards below it.
 */
/** One showcase tile: crimson icon square + title + one-liner. */
@Composable
private fun ClientFeatureTile(
    iconRes: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AerixRadii.cardSmall))
            .background(AerixSurface.panelRaised)
            .border(AerixSpacing.hairline, AerixSurface.borderSoft, RoundedCornerShape(AerixRadii.cardSmall))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = body,
                    color = Color.White.copy(alpha = 0.64f),
                    fontSize = 9.sp,
                    maxLines = 2
                )
            }
        }
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

/** Cape gallery tile: real generated cape preview over the cape's own gradient. */
@Composable
private fun ClientCapeTile(
    spec: ClientCapeSpec,
    equipped: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.card)
    val capeBitmap = remember(spec.id) {
        runCatching { AerixClientCosmetics.buildCapeAndElytraBitmap(spec).asImageBitmap() }.getOrNull()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Color(spec.topHex), Color(spec.bottomHex)))
            )
            .border(
                if (equipped) 2.dp else AerixSpacing.hairline,
                if (equipped) AerixSurface.borderHighlight else AerixSurface.borderSoft,
                shape
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spec.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "LOCKED",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            } else if (equipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "ON",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
        capeBitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = spec.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(52.dp)
            )
        }
        Text(
            text = spec.subtitle,
            fontSize = 8.sp,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    // Equip a cape: store the choice, then push animated frames into the selected instance.
    val equipCape: (ClientCapeSpec) -> Unit = { spec ->
        if (!AerixClientCosmetics.canEquipCape(spec)) {
            Toast.makeText(activity, "Ye cape locked hai — specific account se unlock hota hai", Toast.LENGTH_LONG).show()
        } else if (selectedInstalled == null) {
            Toast.makeText(activity, "Pehle Vortex Client instance install karo", Toast.LENGTH_LONG).show()
        } else {
            AerixClientStore.equippedCapeId = spec.id
            AerixClientStore.persist()
            scope.launch {
                val synced = runCatching {
                    withContext(Dispatchers.IO) {
                        AerixClientCosmetics.syncCapeAssetsToInstance(activity, selectedInstalled.gameDirectory)
                    }
                }.isSuccess
                val msg = if (synced) "${spec.title} equipped — game me next launch se active"
                    else "${spec.title} selected — sync next launch par hoga"
                Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
    // One-tap Vortex mod packs: install into the client instance, then refresh + toast counts.
    val startPackInstall: (String, List<String>, List<String>) -> Unit = { title, slugs, purgePrefixes ->
        scope.launch {
            val res = AerixClientInstaller.installVortexPack(title, slugs, purgePrefixes)
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
                .verticalScroll(rememberScrollState()),
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

        // Feature showcase — 2 × 3 tiles.
        Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientFeatureTile(R.drawable.ic_px_fps, "FPS Boost", "Optimised frames", Modifier.weight(1f))
                ClientFeatureTile(R.drawable.aerix_ic_person_outlined, "Capes & Cosmetics", "WaveyCapes in-game", Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientFeatureTile(R.drawable.aerix_ic_dashboard_outlined, "Custom HUD", "CPS, coords & armor", Modifier.weight(1f))
                ClientFeatureTile(R.drawable.aerix_ic_touch_app_outlined, "Gyro Aim", "Motion controls", Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ClientFeatureTile(R.drawable.aerix_ic_update, "One-tap Sync", "Updates before launch", Modifier.weight(1f))
                ClientFeatureTile(R.drawable.aerix_ic_format_paint_outlined, "Dark Theme", "Custom menu & panorama", Modifier.weight(1f))
            }
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
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
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
                // ── CAPE SHOP: browse and equip animated capes from the launcher ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAPE SHOP",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                    Text(
                        text = "Equipped: ${AerixClientCosmetics.effectiveCapeSpec().title}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B75)
                    )
                }
                AerixClientCosmetics.capes.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
                    ) {
                        pair.forEach { spec ->
                            ClientCapeTile(
                                spec = spec,
                                equipped = AerixClientStore.equippedCapeId == spec.id,
                                unlocked = AerixClientCosmetics.canEquipCape(spec),
                                onClick = { equipCape(spec) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
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
