/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Floating-chrome Home, built from the owner's reference screenshot: the wallpaper runs edge to
 * edge, every control is a translucent glass pill floating over it, the player model stands in
 * the middle, and the actions live in one dock at the bottom.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package net.kdt.pojavlaunch.aerix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.BuildConfig
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixInstance
import net.kdt.pojavlaunch.aerix.bridge.GameDownloadTarget
import net.kdt.pojavlaunch.aerix.bridge.PojavAccountBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavCreateInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.components.AerixImageWell
import net.kdt.pojavlaunch.aerix.components.AerixSkinPreview3D
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.prefs.LauncherPreferences

/**
 * The floating Home: wallpaper visible everywhere, glass chrome on top.
 *
 * Everything here is presentation only — the actions come from the same HomeActions object the
 * rest of Home uses, so no feature exists in one layout and not in the other.
 */
@Composable
internal fun FloatingHome(
    selected: AerixInstance?,
    accent: Color,
    actions: HomeActions
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // ── Top-left: the launcher plate, then the instance picker ───────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = AerixSpacing.lg, top = AerixSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
        ) {
            GlassPlate(radius = AerixRadii.panelSmall) {
                Row(
                    modifier = Modifier.padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
                ) {
                    // The raven mark, straight from the owner's artwork: dark plate so the
                    // magenta bolt and the charcoal feathers both stay visible.
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(AerixRadii.control))
                            .background(Color(0xFF0F0F11)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_vortex_raven),
                            contentDescription = "Vortex",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_short_name),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // "Plus" mark on the card only: the app label stays "Vortex Launcher v1"
                            // so shortcuts, the About page and the updater keep pointing at the
                            // same installed identity.
                            Spacer(Modifier.width(AerixSpacing.xs))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(AerixRadii.pill))
                                    .background(Color(0xFFDC2626))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "PLUS",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                        Text(
                            text = "Android ${BuildConfig.VERSION_NAME} \u00b7 build ${BuildConfig.VERSION_CODE}",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.5.sp
                        )
                    }
                }
            }
            InstancePickerPlate(selected = selected, accent = accent, onOpenLibrary = actions.library)
        }

        // ── Top-right: account, notifications, settings ──────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = AerixSpacing.lg, top = AerixSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val account = PojavAccountBridge.current
            GlassPlate(radius = AerixRadii.pill) {
                Row(
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = actions.accounts)
                        .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.smCompact),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
                ) {
                    AerixImageWell(
                        image = account?.let { PojavAccountBridge.loadAccountFacePreview(context, it) },
                        fallback = R.drawable.aerix_ic_people,
                        size = 24.dp,
                        radius = AerixRadii.pill
                    )
                    Text(
                        text = account?.username ?: "Sign in",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            FloatingIconButton(R.drawable.aerix_ic_search, "Discover", actions.explore)
            FloatingIconButton(R.drawable.aerix_ic_settings_filled, "Settings", actions.settings)
        }

        // ── Middle: the player model, standing on the wallpaper ──────────────────────────
        var azimuth by remember { mutableFloatStateOf(20f) }
        val stageWidth = if (landscape) 260.dp else 190.dp
        val stageHeight = if (landscape) 340.dp else 260.dp
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = if (landscape) 0.dp else (-24).dp)
                .width(stageWidth)
                .height(stageHeight)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        azimuth = (azimuth + dragAmount.x * 0.55f).mod(360f)
                    }
                }
        ) {
            AerixSkinPreview3D(
                account = PojavAccountBridge.current,
                azimuth = azimuth.toInt(),
                pitch = 6,
                interactionEnabled = false,
                isVisible = true,
                showOfficialCape = true,
                cameraDistance = 46,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Bottom-left quick pills ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = AerixSpacing.lg, bottom = if (landscape) 84.dp else 128.dp),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)) {
                FloatingTextPill("Touch layout", actions.controls)
                FloatingTextPill("Profile 1", actions.accounts)
            }
            FloatingTextPill(
                text = "Renderer: ${rendererLabel()}",
                onClick = actions.settings,
                wide = false
            )
        }

        // ── Bottom centre: the one download CTA ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (landscape) 84.dp else 128.dp)
        ) {
            FloatingTextPill(
                text = "Download Minecraft ${latestReleaseLabel()}",
                onClick = actions.create,
                leadingIcon = R.drawable.aerix_ic_download,
                emphasised = true
            )
        }

        // ── Bottom: the action dock ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = AerixSpacing.md)
                .widthIn(max = if (landscape) 620.dp else 340.dp)
                .fillMaxWidth()
                .padding(horizontal = AerixSpacing.md)
                .clip(RoundedCornerShape(AerixRadii.pill))
                .background(Brush.verticalGradient(listOf(Color(0x730A1520), Color(0x8C07131D))))
                .border(AerixSpacing.hairline, Color.White.copy(alpha = 0.16f), RoundedCornerShape(AerixRadii.pill))
                .padding(horizontal = AerixSpacing.sm, vertical = AerixSpacing.smCompact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // PLAY stays the loudest control in the dock, in the accent colour.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(AerixRadii.pill))
                    .background(accent)
                    .clickable(role = Role.Button, onClick = if (selected != null) actions.play else actions.create)
                    .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_play_arrow_filled),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text("Play", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            DockEntry(R.drawable.aerix_ic_extension_outlined, "Mods", actions.mods)
            DockEntry(R.drawable.aerix_ic_folder_zip_outlined, "Packs", actions.explore)
            DockEntry(R.drawable.aerix_ic_style_outlined, "Shaders", actions.explore)
            DockEntry(R.drawable.aerix_ic_dashboard_filled, "Installed", actions.library)
            DockEntry(R.drawable.aerix_ic_people, "Skins", actions.accounts)
            DockEntry(R.drawable.aerix_ic_settings_filled, "Settings", actions.settings)
        }
    }
}

/** The translucent plate the floating chrome sits on: bright rim, dark glass, no solid fill. */
@Composable
private fun GlassPlate(
    radius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x590A1520), Color(0x7307131D))))
            .border(AerixSpacing.hairline, Color.White.copy(alpha = 0.22f), shape)
    ) {
        content()
    }
}

/** One square glass icon button of the top-right cluster. */
@Composable
private fun FloatingIconButton(iconRes: Int, label: String, onClick: () -> Unit) {
    GlassPlate(radius = AerixRadii.control) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A wide glass pill with a label, used for the quick settings under the model. */
@Composable
private fun FloatingTextPill(
    text: String,
    onClick: () -> Unit,
    leadingIcon: Int? = null,
    emphasised: Boolean = false,
    wide: Boolean = true
) {
    val shape = RoundedCornerShape(AerixRadii.pill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(
                if (emphasised) Brush.verticalGradient(listOf(Color(0x8CD42A44), Color(0x8C7E1626)))
                else Brush.verticalGradient(listOf(Color(0x590A1520), Color(0x7307131D)))
            )
            .border(
                AerixSpacing.hairline,
                Color.White.copy(alpha = if (emphasised) 0.34f else 0.22f),
                shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AerixSpacing.mdPlus, vertical = AerixSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
    ) {
        if (leadingIcon != null) {
            Icon(
                painter = painterResource(leadingIcon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = if (wide) Modifier.widthIn(min = 132.dp, max = 240.dp) else Modifier
        )
    }
}

/** One label + icon entry inside the bottom dock. */
@Composable
private fun DockEntry(iconRes: Int, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(AerixRadii.control))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AerixSpacing.xs, vertical = AerixSpacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** The selected instance as the reference's second plate: its name over a dropdown chevron. */
@Composable
private fun InstancePickerPlate(selected: AerixInstance?, accent: Color, onOpenLibrary: () -> Unit) {
    val vanilla = stringResource(R.string.aerix_home_vanilla)
    GlassPlate(radius = AerixRadii.cardSmall) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onOpenLibrary)
                .padding(horizontal = AerixSpacing.smPlus, vertical = AerixSpacing.smCompact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smCompact)
        ) {
            AerixImageWell(
                image = selected?.icon,
                fallback = R.drawable.aerix_loader_vanilla,
                size = 30.dp,
                radius = AerixRadii.controlSmall
            )
            Column {
                Text(
                    text = selected?.name ?: "Select instance",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = selected?.let {
                        "${GameDownloadTarget.minecraftVersionIn(it.versionId) ?: it.versionId} \u00b7 ${loaderLabel(it.versionId, vanilla)}"
                    } ?: "No version installed",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 9.5.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(AerixSpacing.xxs))
            Text("\u25BE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        }
    }
}

/** Renderer id turned into the plain name the reference pill shows. */
private fun rendererLabel(): String {
    val id = LauncherPreferences.PREF_RENDERER ?: return "Auto"
    return when {
        id.contains("zink", ignoreCase = true) -> "Vulkan / Zink"
        id.contains("cosmic", ignoreCase = true) -> "Cosmic RenderX"
        id.contains("mobileglues", ignoreCase = true) -> "MobileGlues"
        id.contains("ltw", ignoreCase = true) -> "LTW"
        id.contains("freedreno", ignoreCase = true) -> "Freedreno"
        id.contains("mesa", ignoreCase = true) -> "Mesa"
        else -> "OpenGL / GL4ES"
    }
}

/**
 * The newest stable Minecraft the launcher knows, as shown on the download pill. The cached list is
 * used when it exists, the bundled fallback otherwise; nothing is fetched from this composable.
 */
private fun latestReleaseLabel(): String = runCatching {
    PojavCreateInstanceBridge.gameVersions()
        .firstOrNull { it.id.matches(Regex("""\d+\.\d+(\.\d+)?""")) }
        ?.id
}.getOrNull() ?: "1.21"
