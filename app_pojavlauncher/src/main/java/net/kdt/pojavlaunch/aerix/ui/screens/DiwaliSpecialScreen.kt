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

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                AerixGhostButton(
                    text = "\uD83D\uDD14 Remind me",
                    iconRes = null,
                    onClick = { addToCalendar(context) },
                    modifier = Modifier.weight(1f)
                )
                AerixGhostButton(
                    text = "\uD83D\uDCE4 Share",
                    iconRes = null,
                    onClick = { shareAnnouncement(context) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Closing card, on the plain panel so the gold hero keeps the emphasis.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
                    .border(
                        width = 1.dp,
                        color = festivalGold.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(AerixRadii.card)
                    )
                    .padding(AerixSpacing.lg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier
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
    ) {
        DiwaliNightSky()

        Column(
            modifier = Modifier.padding(AerixSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Text(
                text = ApexDiwali.HEADLINE,
                color = festivalGold,
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

@Composable
private fun CountdownCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(AerixRadii.control))
            .border(1.dp, festivalGold.copy(alpha = 0.28f), RoundedCornerShape(AerixRadii.control))
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
    Column(
        modifier = Modifier
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
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(festivalDeepRed, festivalGold)),
                        shape = CircleShape
                    )
            )
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
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                pair.forEach { (emoji, title, note) ->
                    FeatureTile(
                        emoji = emoji,
                        title = title,
                        note = note,
                        modifier = Modifier.weight(1f)
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
        Text(text = emoji, fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.Top) {
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

private fun shareAnnouncement(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Apex Launcher 2.0 \u2014 Diwali Special")
        putExtra(Intent.EXTRA_TEXT, ApexDiwali.shareText())
    }
    runCatching { context.startActivity(Intent.createChooser(send, "Share Diwali Special")) }
}
