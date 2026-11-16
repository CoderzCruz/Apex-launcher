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

@Composable
fun AerixClientScreen(activity: FragmentActivity) {
    LaunchedEffect(Unit) {
        AerixClientStore.init(activity)
    }

    var refreshTick by remember { mutableIntStateOf(0) }
    var preparingLaunchVer by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val accent = AerixSurface.accent

    AerixPage(
        title = "Vortex Client",
        subtitle = null
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
                                if (latest) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFDC2626), Color(0xFFB01B2B))
                                    )
                                } else {
                                    AerixSurface.panelRaised
                                }
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

        // Load the instance list once, rather than scanning disk for every version card.
        val installedClients = remember(refreshTick, AerixClientInstaller.isInstalling) {
            runCatching { Instances.loadAllInstances() }.getOrDefault(emptyList())
        }
        // Liquid Glass Version Download & Launch List (Nothing More)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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

            AerixClientInstaller.availableVersions.forEach { spec ->
                val installed = remember(refreshTick, AerixClientInstaller.isInstalling, spec.mcVersion) {
                    AerixClientInstaller.findInstalledClientInstance(spec.mcVersion, installedClients)
                }
                val isSelected = AerixClientStore.selectedClientVersion == spec.mcVersion
                val rowShape = RoundedCornerShape(AerixRadii.card)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = rowShape,
                            tint = if (isSelected) accent else AerixSurface.glassTint,
                            strength = if (isSelected) 1.0f else 0.90f
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isSelected) {
                                    listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        accent.copy(alpha = 0.14f),
                                        AerixSurface.panelRaised
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.12f),
                                        AerixSurface.panel
                                    )
                                }
                            ),
                            shape = rowShape
                        )
                        .clickable {
                            AerixClientStore.selectedClientVersion = spec.mcVersion
                            AerixClientStore.persist()
                            if (installed != null) {
                                Instances.setSelectedInstance(installed)
                                PojavInstanceBridge.reload()
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.aerix_ic_rocket_launch_filled),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Minecraft ${spec.mcVersion}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (installed != null) {
                                val badgeShape = RoundedCornerShape(999.dp)
                                Box(
                                    modifier = Modifier
                                        .clip(badgeShape)
                                        .background(accent.copy(alpha = 0.22f))
                                        .border(0.9.dp, accent.copy(alpha = 0.65f), badgeShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(accent)
                                        )
                                        Text(
                                            text = "INSTALLED",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = spec.summary,
                            color = Color.White.copy(alpha = 0.76f),
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val busy = AerixClientInstaller.isInstalling || preparingLaunchVer != null
                        val pillShape = RoundedCornerShape(999.dp)

                        // Liquid Glass Download / Re-download Pill
                        Box(
                            modifier = Modifier
                                .liquidGlass(
                                    shape = pillShape,
                                    tint = if (installed == null) accent else AerixSurface.glassTint,
                                    strength = 0.95f
                                )
                                .background(
                                    brush = if (installed == null) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                accent.copy(alpha = 0.42f),
                                                AerixSurface.glassBlue.copy(alpha = 0.30f)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.14f),
                                                Color.White.copy(alpha = 0.07f)
                                            )
                                        )
                                    },
                                    shape = pillShape
                                )
                                .clickable(enabled = !busy) {
                                    AerixClientStore.selectedClientVersion = spec.mcVersion
                                    AerixClientStore.persist()
                                    scope.launch {
                                        val res = AerixClientInstaller.installClientVersion(activity, spec.mcVersion)
                                        refreshTick++
                                        PojavInstanceBridge.reload()
                                        res.onSuccess {
                                            Toast.makeText(
                                                activity,
                                                "Vortex Client ${spec.mcVersion} ready!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }.onFailure { err ->
                                            Toast.makeText(
                                                activity,
                                                err.message ?: "Download failed",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val isThisDownloading =
                                AerixClientInstaller.isInstalling && AerixClientInstaller.installingVersion == spec.mcVersion
                            Text(
                                text = when {
                                    isThisDownloading -> "Downloading..."
                                    installed == null -> "\u2193 Download"
                                    else -> "\u21bb Re-download"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Liquid Glass Launch Pill (upgrades existing instance pack + WaveyCapes + FPS before launch!)
                        if (installed != null) {
                            Box(
                                modifier = Modifier
                                    .liquidGlass(
                                        shape = pillShape,
                                        tint = accent,
                                        strength = 1.0f
                                    )
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                accent.copy(alpha = 0.55f),
                                                AerixSurface.glassBlue.copy(alpha = 0.38f)
                                            )
                                        ),
                                        shape = pillShape
                                    )
                                    .clickable(enabled = !busy) {
                                        AerixClientStore.selectedClientVersion = spec.mcVersion
                                        AerixClientStore.persist()
                                        scope.launch {
                                            preparingLaunchVer = spec.mcVersion
                                            val sync = AerixClientInstaller.syncExistingInstanceForLaunch(
                                                activity,
                                                installed,
                                                spec.mcVersion
                                            )
                                            sync.onFailure { error ->
                                                Toast.makeText(activity, "Client update failed: ${error.message}", Toast.LENGTH_LONG).show()
                                            }
                                            preparingLaunchVer = null
                                            Instances.setSelectedInstance(installed)
                                            PojavInstanceBridge.reload()
                                            PojavLaunchBridge.play(activity)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (preparingLaunchVer == spec.mcVersion) "Syncing..." else "\u25b6 Launch",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
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
            }
        }
    }
