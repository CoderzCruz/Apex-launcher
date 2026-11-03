/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The Diwali Special page: the Apex Launcher 2.0 announcement and its live countdown.
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

package net.kdt.pojavlaunch.aerix.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.provider.CalendarContract
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.sin

/** The announcement shown by the Diwali Special entry on the launcher rail. */
object ApexDiwali {
    const val TITLE = "Diwali Special"
    const val HEADLINE = "\uD83C\uDF87 A NEW BEGINNING IS COMING! \uD83C\uDF87"
    const val SUBHEAD = "\uD83E\uDE94 This Diwali, Apex Launcher has a special surprise for everyone!"
    const val VERSION = "\uD83D\uDE80 APEX LAUNCHER 2.0 IS COMING SOON!"
    const val TAGLINE = "Something bigger. Something better. Something completely NEW! \uD83D\uDD25"
    const val DATE_LABEL = "RELEASE DATE"
    const val DATE_VALUE = "20 NOVEMBER 2026"

    /** The launch moment, in the timezone the announcement is written for. */
    const val RELEASE_YEAR = 2026
    val RELEASE_MONTH = Calendar.NOVEMBER
    const val RELEASE_DAY = 20

    /** Where the "journey to 2.0" progress bar starts counting from. */
    const val BUILD_UP_YEAR = 2026
    val BUILD_UP_MONTH = Calendar.OCTOBER
    const val BUILD_UP_DAY = 1

    /** Everything under the headline, one entry per line of the announcement. */
    val lines = listOf(
        "\uD83D\uDC8E A brand-new experience awaits you.",
        "\u26A1 Next-level improvements.",
        "\uD83C\uDFA8 A fresh premium look.",
        "\uD83D\uDC51 A whole new Apex experience.",
        "\u23F3 The wait is almost over!",
        "\u2764\uFE0F Thank you for being part of our journey. Your support makes Apex Launcher stronger!",
        "\uD83D\uDD25 SAVE THE DATE \u2013 20.11.2026"
    )

    /** The four cards under the countdown: what 2.0 actually brings. */
    val features = listOf(
        Triple("\uD83C\uDF86", "Grand launch", "20 November, for everyone"),
        Triple("\u26A1", "Faster launcher", "Quicker start, smoother screens"),
        Triple("\uD83C\uDFA8", "New look", "A premium crimson-and-gold face"),
        Triple("\uD83D\uDC51", "Founder perks", "Early access for our testers")
    )

    const val FOOTER = "APEX LAUNCHER 2.0"
    const val FOOTER_TAGLINE = "The Next Generation Begins. \uD83D\uDE80"

    /** Text used by the share sheet. */
    fun shareText(): String = buildString {
        append("\uD83C\uDF87 Apex Launcher 2.0 \u2014 Diwali Special \uD83C\uDF87\n\n")
        append("Release: $DATE_VALUE\n\n")
        lines.forEach { append("\u2022 ${it}\n") }
        append("\nGet Apex Launcher: https://github.com/CoderzCruz/Apex-launcher")
    }
}

/** Diwali gold on the launcher's own crimson canvas. */
private val festivalGold = Color(0xFFFFB020)
private val festivalAmber = Color(0xFFFF7A18)
private val festivalDeepRed = Color(0xFF6B0F1A)
private val festivalNight = Color(0xFF2A0A10)

/** Midnight of the release day, or of the build-up start, in the announcement's timezone. */
private fun momentOf(year: Int, month: Int, day: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
        clear()
        set(year, month, day, 0, 0, 0)
    }.timeInMillis

private fun millisUntilRelease(): Long {
    val release = momentOf(ApexDiwali.RELEASE_YEAR, ApexDiwali.RELEASE_MONTH, ApexDiwali.RELEASE_DAY)
    return (release - System.currentTimeMillis()).coerceAtLeast(0L)
}

/**
 * Every block of the page drifts up into place after [delayMs], so the announcement assembles
 * itself instead of popping in as one slab. Applied outermost so the card fades with its own
 * background, not just its text.
 */
@Composable
private fun Modifier.entrance(delayMs: Long): Modifier {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }
    val fade by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "diwali-fade"
    )
    val rise by animateFloatAsState(
        targetValue = if (visible) 0f else 34f,
        animationSpec = tween(620),
        label = "diwali-rise"
    )
    return this.graphicsLayer {
        alpha = fade
        translationY = rise * density
    }
}

@Composable
fun DiwaliSpecialScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var remaining by remember { mutableLongStateOf(millisUntilRelease()) }

    // One tick a second keeps the countdown honest without waking anything else up.
    LaunchedEffect(Unit) {
        while (true) {
            remaining = millisUntilRelease()
            delay(1_000L)
        }
    }

    val days = remaining / 86_400_000L
    val hours = remaining % 86_400_000L / 3_600_000L
    val minutes = remaining % 3_600_000L / 60_000L
    val seconds = remaining % 60_000L / 1_000L

    val start = momentOf(
        ApexDiwali.BUILD_UP_YEAR,
        ApexDiwali.BUILD_UP_MONTH,
        ApexDiwali.BUILD_UP_DAY
    )
    val release = momentOf(ApexDiwali.RELEASE_YEAR, ApexDiwali.RELEASE_MONTH, ApexDiwali.RELEASE_DAY)
    val progress = remember(start, release) {
        val span = (release - start).coerceAtLeast(1L)
        ((System.currentTimeMillis() - start).toFloat() / span).coerceIn(0f, 1f)
    }

    AerixPage(title = ApexDiwali.TITLE, subtitle = "Apex Launcher 2.0 announcement") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
        ) {
            DiwaliHero(remaining = remaining, days = days, hours = hours, minutes = minutes, seconds = seconds)

            JourneyCard(progress = progress, days = days)

            FeatureGrid()

            ApexBulletCard(ApexDiwali.lines, festivalGold)

            DiwaliDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                AerixGhostButton(
                    text = "\uD83D\uDD14 Remind me",
                    iconRes = null,
                    onClick = { addToCalendar(context) },
                    modifier = Modifier.entrance(620).weight(1f)
                )
                AerixGhostButton(
                    text = "\uD83D\uDCE4 Share",
                    iconRes = null,
                    onClick = { shareAnnouncement(context) },
                    modifier = Modifier.entrance(700).weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                AerixGhostButton(
                    text = "\uD83D\uDCCB Copy text",
                    iconRes = null,
                    onClick = { copyAnnouncement(context) },
                    modifier = Modifier.entrance(780).weight(1f)
                )
                AerixGhostButton(
                    text = "\uD83D\uDD17 GitHub",
                    iconRes = null,
                    onClick = { openGitHub(context) },
                    modifier = Modifier.entrance(860).weight(1f)
                )
            }

            // Closing card, on the plain panel so the gold hero keeps the emphasis.
            val footerGlow = rememberInfiniteTransition(label = "diwali-footer")
            val glow by footerGlow.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1_900, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "diwali-footer-glow"
            )
            Box(
                modifier = Modifier.entrance(940)
                    .fillMaxWidth()
                    .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                festivalGold.copy(alpha = 0.45f + 0.40f * glow),
                                festivalGold.copy(alpha = 0.10f + 0.16f * glow)
                            )
                        ),
                        shape = RoundedCornerShape(AerixRadii.card)
                    )
                    .padding(AerixSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\uD83E\uDE94",
                        fontSize = 18.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 0.92f + 0.14f * glow
                            scaleY = 0.92f + 0.14f * glow
                        }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = ApexDiwali.FOOTER,
                        color = festivalGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = ApexDiwali.FOOTER_TAGLINE,
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            AerixGhostButton(
                text = "\u2039  Back to launcher",
                iconRes = null,
                onClick = onClose,
                modifier = Modifier.entrance(1020).fillMaxWidth()
            )
            Spacer(Modifier.height(AerixSpacing.sm))
        }
    }
}

/* ── hero ──────────────────────────────────────────────────────────────── */

@Composable
private fun DiwaliHero(
    remaining: Long,
    days: Long,
    hours: Long,
    minutes: Long,
    seconds: Long
) {
    Box(
        modifier = Modifier.entrance(0)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    listOf(festivalDeepRed, festivalNight, Color(0xFF14040A))
                ),
                shape = RoundedCornerShape(AerixRadii.card)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(festivalGold.copy(alpha = 0.75f), Color.Transparent)
                ),
                shape = RoundedCornerShape(AerixRadii.card)
            )
            .clip(RoundedCornerShape(AerixRadii.card))
    ) {
        DiwaliNightSky()
        DiwaliGoldSheen()

        // The headline breathes between deep gold and candle-light.
        val sheen = rememberInfiniteTransition(label = "diwali-headline")
        val glow by sheen.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2_600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "diwali-headline-glow"
        )
        val headlineColor = lerp(festivalGold, Color(0xFFFFF3C4), glow)

        Column(
            modifier = Modifier.padding(AerixSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Text(
                text = ApexDiwali.HEADLINE,
                color = headlineColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = ApexDiwali.SUBHEAD,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(AerixSpacing.xs))
            Text(
                text = ApexDiwali.VERSION,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = ApexDiwali.TAGLINE,
                color = AerixSurface.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(AerixSpacing.sm))

            // The date chip is the thing people read first, so it gets its own pill.
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 7.dp + 9.dp * glow,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = festivalAmber,
                        spotColor = festivalAmber
                    )
                    .background(
                        brush = Brush.horizontalGradient(listOf(festivalGold, festivalAmber)),
                        shape = CircleShape
                    )
                    .padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "\uD83D\uDCC5 ${ApexDiwali.DATE_LABEL}:",
                    color = festivalNight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ApexDiwali.DATE_VALUE,
                    color = festivalNight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(AerixSpacing.sm))

            if (remaining > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
                ) {
                    CountdownCell(days.toString(), "DAYS", Modifier.weight(1f))
                    CountdownCell(hours.toString().padStart(2, '0'), "HRS", Modifier.weight(1f))
                    CountdownCell(minutes.toString().padStart(2, '0'), "MIN", Modifier.weight(1f))
                    CountdownCell(seconds.toString().padStart(2, '0'), "SEC", Modifier.weight(1f))
                }
            } else {
                Text(
                    text = "\uD83C\uDF86 IT IS HERE \u2014 APEX LAUNCHER 2.0 IS LIVE \uD83C\uDF86",
                    color = festivalGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A twinkling night sky inside the hero: a row of lit diyas along the bottom and sparks that
 * breathe above them. Drawn on a Canvas so it costs one draw call and needs no assets.
 */
@Composable
private fun BoxScope.DiwaliNightSky() {
    val transition = rememberInfiniteTransition(label = "diwali-sky")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diwali-phase"
    )

    Canvas(modifier = Modifier.matchParentSize()) {
        // Sparks: deterministic positions so nothing flickers between frames.
        val sparkCount = 26
        for (i in 0 until sparkCount) {
            val x = ((i * 37) % 100) / 100f * size.width
            val y = ((i * 53) % 55) / 100f * size.height
            val twinkle = 0.25f + 0.55f * ((sin(phase * 2f * PI.toFloat() + i.toFloat()) + 1f) / 2f)
            drawCircle(
                color = festivalGold.copy(alpha = twinkle * 0.55f),
                radius = (1f + (i % 3)) * density,
                center = Offset(x, y)
            )
        }

        // Diyas along the bottom edge: a warm bowl with a flickering flame.
        val diyaCount = 7
        val step = size.width / (diyaCount + 1)
        for (i in 1..diyaCount) {
            val x = step * i
            val y = size.height - 6f * density
            val glow = 0.35f + 0.4f * ((sin(phase * 2f * PI.toFloat() * 2f + i) + 1f) / 2f)

            drawCircle(
                color = festivalAmber.copy(alpha = glow * 0.30f),
                radius = 11f * density,
                center = Offset(x, y)
            )
            drawCircle(color = Color(0xFFFFE9B0).copy(alpha = glow), radius = 2.6f * density, center = Offset(x, y - 1.6f * density))
            drawCircle(color = festivalGold.copy(alpha = 0.85f), radius = 5.2f * density, center = Offset(x, y + 2.2f * density))
        }
    }
}

/**
 * A soft gold band that sweeps across the hero once in a while, the way light runs over foil.
 * Drawn on a Canvas over the night sky, so it costs no extra layer.
 */
@Composable
private fun BoxScope.DiwaliGoldSheen() {
    val transition = rememberInfiniteTransition(label = "diwali-sheen")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "diwali-sheen-phase"
    )

    Canvas(modifier = Modifier.matchParentSize()) {
        val band = size.width * 0.30f
        val travel = size.width + band * 2f
        val left = -band + travel * phase
        drawRect(
            brush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    festivalGold.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                start = Offset(left, 0f),
                end = Offset(left + band, size.height)
            )
        )
    }
}

@Composable
private fun CountdownCell(value: String, label: String, modifier: Modifier = Modifier) {
    // Every tick lands with a small pop, so the countdown feels alive rather than printed.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(value) {
        pop.snapTo(1.16f)
        pop.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
                ),
                shape = RoundedCornerShape(AerixRadii.control)
            )
            .border(1.dp, festivalGold.copy(alpha = 0.42f), RoundedCornerShape(AerixRadii.control))
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = festivalGold,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            color = AerixSurface.textMuted,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ── journey + features ────────────────────────────────────────────────── */

@Composable
private fun JourneyCard(progress: Float, days: Long) {
    // The bar sweeps from zero to today's mark the first time the card is seen.
    val fill = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        fill.animateTo(progress.coerceAtLeast(0.02f), tween(1_100, easing = FastOutSlowInEasing))
    }
    Column(
        modifier = Modifier.entrance(140)
            .fillMaxWidth()
            .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Journey to 2.0",
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (days > 0) {
                    "${(progress * 100).toInt()}% \u00B7 $days days to go"
                } else {
                    "100% \u00B7 it is here!"
                },
                color = festivalGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AerixSurface.scrimSoft, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.value)
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(festivalDeepRed, festivalGold)),
                        shape = CircleShape
                    )
            ) {
                // The lit tip of the bar: a small lamp burning at today's mark.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(festivalGold)
                        .border(2.dp, Color(0xFFFFE9B0), CircleShape)
                )
            }
        }
        Text(
            text = "Jitna intezaar, utna hi bada surprise 🎁",
            color = AerixSurface.textMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun FeatureGrid() {
    val rows = ApexDiwali.features.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
        rows.forEachIndexed { rowIndex, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                pair.forEachIndexed { columnIndex, (emoji, title, note) ->
                    val order = rowIndex * 2 + columnIndex
                    FeatureTile(
                        emoji = emoji,
                        title = title,
                        note = note,
                        modifier = Modifier.entrance(260L + order * 90L).weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureTile(
    emoji: String,
    title: String,
    note: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(festivalDeepRed.copy(alpha = 0.55f), AerixSurface.panelRaised)
                ),
                shape = RoundedCornerShape(AerixRadii.card)
            )
            .border(1.dp, festivalGold.copy(alpha = 0.22f), RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(festivalGold.copy(alpha = 0.14f))
                .border(1.dp, festivalGold.copy(alpha = 0.38f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = note,
            color = festivalGold.copy(alpha = 0.85f),
            fontSize = 9.5.sp,
            lineHeight = 13.sp
        )
    }
}

/* ── announcement lines ────────────────────────────────────────────────── */

/** One card holding the announcement lines, each with a small lit diya dot. */
@Composable
private fun ApexBulletCard(lines: List<String>, accent: Color) {
    Column(
        modifier = Modifier.entrance(380)
            .fillMaxWidth()
            .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        lines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier.entrance(450L + index * 70L),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp, end = 10.dp)
                        .size(6.dp)
                        .background(accent, CircleShape)
                )
                Text(
                    text = line,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * A foil ornament: two gold lines fading outwards from a small diamond, with a lit dot on
 * either side — the sort of rule a premium invitation card carries.
 */
@Composable
private fun DiwaliDivider() {
    // The rule draws itself outward from the diamond once the page has settled.
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(620L)
        grow.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val g = grow.value
        val cy = size.height / 2f
        val cx = size.width / 2f
        val gap = 30f * density

        // The arms reach from the diamond outwards as g grows; skipped until they have length,
        // so the shader never gets a zero-distance direction.
        if (g > 0.02f) {
            val leftOuter = (cx - gap) * (1f - g)
            drawLine(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, festivalGold.copy(alpha = 0.75f)),
                    start = Offset(leftOuter, cy),
                    end = Offset(cx - gap, cy)
                ),
                start = Offset(leftOuter, cy),
                end = Offset(cx - gap, cy),
                strokeWidth = 1.4f * density
            )
            val rightOuter = (cx + gap) + (size.width - (cx + gap)) * g
            drawLine(
                brush = Brush.linearGradient(
                    listOf(festivalGold.copy(alpha = 0.75f), Color.Transparent),
                    start = Offset(cx + gap, cy),
                    end = Offset(rightOuter, cy)
                ),
                start = Offset(cx + gap, cy),
                end = Offset(rightOuter, cy),
                strokeWidth = 1.4f * density
            )
        }

        drawCircle(
            festivalGold.copy(alpha = 0.55f * g),
            radius = 2.4f * density,
            center = Offset(cx - gap - 8f * density, cy)
        )
        drawCircle(
            festivalGold.copy(alpha = 0.55f * g),
            radius = 2.4f * density,
            center = Offset(cx + gap + 8f * density, cy)
        )

        val diamond = Path().apply {
            moveTo(cx, cy - 7f * density * g)
            lineTo(cx + 7f * density * g, cy)
            lineTo(cx, cy + 7f * density * g)
            lineTo(cx - 7f * density * g, cy)
            close()
        }
        drawPath(diamond, festivalGold.copy(alpha = g))
        val inner = Path().apply {
            moveTo(cx, cy - 3.2f * density * g)
            lineTo(cx + 3.2f * density * g, cy)
            lineTo(cx, cy + 3.2f * density * g)
            lineTo(cx - 3.2f * density * g, cy)
            close()
        }
        drawPath(inner, Color(0xFFFFF3C4).copy(alpha = g))
    }
}

/* ── actions ───────────────────────────────────────────────────────────── */

/** Prefills a calendar event on the release day. No permission, no background work. */
private fun addToCalendar(context: Context) {
    val begin = momentOf(
        ApexDiwali.RELEASE_YEAR,
        ApexDiwali.RELEASE_MONTH,
        ApexDiwali.RELEASE_DAY
    )
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, "\uD83C\uDF86 Apex Launcher 2.0 launch")
        putExtra(
            CalendarContract.Events.DESCRIPTION,
            ApexDiwali.SUBHEAD + "\n\n" + ApexDiwali.TAGLINE
        )
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + 3_600_000L)
        putExtra(CalendarContract.Events.ALL_DAY, true)
        putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
    }
    runCatching { context.startActivity(intent) }
}

/** Puts the whole announcement on the clipboard, ready to paste anywhere. */
private fun copyAnnouncement(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Apex Diwali Special", ApexDiwali.shareText()))
    Toast.makeText(context, "\u2705 Announcement copied", Toast.LENGTH_SHORT).show()
}

/** Opens the project page where every build and release lives. */
private fun openGitHub(context: Context) {
    val page = Uri.parse("https://github.com/CoderzCruz/Apex-launcher")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, page)) }
        .onFailure { Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show() }
}

private fun shareAnnouncement(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Apex Launcher 2.0 \u2014 Diwali Special")
        putExtra(Intent.EXTRA_TEXT, ApexDiwali.shareText())
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Share Diwali Special")) }
}
