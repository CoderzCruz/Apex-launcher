/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Landscape overhaul shell: the mockup layout shared by Home, Library and Discover.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package net.kdt.pojavlaunch.aerix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.aerix.components.apexPress
import net.kdt.pojavlaunch.aerix.components.liquidGlass
import net.kdt.pojavlaunch.aerix.theme.AerixMetrics
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface

/** True on a wide screen; the mockup layout is the landscape one. */
@Composable
fun isLandscapeLayout(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp >= configuration.screenHeightDp &&
        configuration.screenWidthDp >= 720
}

/**
 * A page in the overhauled shell: a large title, optional trailing content (search field, primary
 * button) and the page body underneath. Same typography as the landscape mockups.
 */
@Composable
fun AerixWidePage(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AerixSpacing.xl, vertical = AerixSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = AerixSurface.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
            trailing?.invoke()
        }
        content()
    }
}

/** Small letter-spaced crimson caption that opens each panel, as in the mockups. */
@Composable
fun PanelLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text.uppercase(),
            color = AerixSurface.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.6.sp
        )
        trailing?.invoke()
    }
}

/**
 * Frosted glass card with the crimson hairline rim from the mockups. [scrollable] makes the body
 * scroll on its own, so a tall column never pushes the next card off screen.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = AerixSpacing.mdPlus,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AerixRadii.panelSmall)
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                tint = AerixSurface.glassRose,
                strength = 0.72f,
                elevation = AerixMetrics.glassFloatingElevation
            )
            .background(AerixSurface.panel.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            content()
        }
    }
}

/** Glass card with a crimson border emphasised like the selected card in the mockups. */
@Composable
fun HighlightGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = AerixSpacing.mdPlus,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AerixRadii.panelSmall)
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                tint = AerixSurface.accent,
                strength = 0.9f,
                elevation = AerixMetrics.glassFloatingElevation
            )
            .background(AerixSurface.panelRaised)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            content()
        }
    }
}

/** Crimson gradient pill used for the page's primary action (LAUNCH, NEW INSTANCE, INSTALL). */
@Composable
fun CrimsonPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    val accent = AerixSurface.accent
    val shape = RoundedCornerShape(AerixRadii.pill)
    Box(
        modifier = modifier
            .height(height)
            .liquidGlass(shape = shape, tint = accent, strength = 0.5f)
            .background(Brush.horizontalGradient(listOf(accent, Color(0xFF9E1728))))
            .apexPress(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp
        )
    }
}

/** Outline counterpart of [CrimsonPill] for secondary actions. */
@Composable
fun OutlinePill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 38.dp
) {
    val accent = AerixSurface.accent
    val shape = RoundedCornerShape(AerixRadii.control)
    Box(
        modifier = modifier
            .height(height)
            .liquidGlass(shape = shape, tint = accent, strength = 0.45f)
            .background(AerixSurface.panelRaised.copy(alpha = 0.6f))
            .apexPress(onClick)
            .padding(horizontal = AerixSpacing.mdPlus),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

/** Vertical hairline divider between two columns of the landscape layout. */
@Composable
fun ColumnGap(width: Dp = AerixSpacing.mdPlus) {
    Spacer(modifier = Modifier.width(width).fillMaxHeight())
}

/**
 * Landscape body: three columns with fixed proportions, each able to scroll on its own. On a
 * portrait phone the same content falls back to one scrolling column (see the call sites).
 */
@Composable
fun LandscapeColumns(
    modifier: Modifier = Modifier,
    leftWeight: Float = 1.6f,
    middleWeight: Float = 1.15f,
    rightWeight: Float = 1.25f,
    left: @Composable BoxScope.() -> Unit,
    middle: @Composable BoxScope.() -> Unit,
    right: @Composable BoxScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        Box(modifier = Modifier.weight(leftWeight).fillMaxHeight()) { left() }
        Box(modifier = Modifier.weight(middleWeight).fillMaxHeight()) { middle() }
        Box(modifier = Modifier.weight(rightWeight).fillMaxHeight()) { right() }
    }
}

/** Compact two-line tile from the QUICK ACTIONS block of the mockup. */
@Composable
fun QuickActionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = AerixSurface.accent
    val shape = RoundedCornerShape(AerixRadii.card)
    Column(
        modifier = modifier
            .liquidGlass(shape = shape, tint = AerixSurface.glassRose, strength = 0.6f)
            .background(AerixSurface.panel.copy(alpha = 0.5f))
            .apexPress(onClick)
            .padding(AerixSpacing.mdPlus),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(38.dp)
                .liquidGlass(shape = RoundedCornerShape(AerixRadii.control), tint = accent, strength = 0.7f)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.width(20.dp).height(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accent)
            )
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = AerixSurface.textMuted,
            fontSize = 10.5.sp,
            maxLines = 1
        )
    }
}

/** Border stroke used by the shell's quiet panels (search fields, chips). */
val shellHairline: BorderStroke
    @Composable get() = BorderStroke(AerixSpacing.hairline, AerixSurface.borderSoft)

/** Press effect with an explicit button role, used by the pill-shaped controls. */
@Composable
internal fun Modifier.apexPressClickable(onClick: () -> Unit): Modifier =
    apexPress(onClick = onClick, role = Role.Button)
