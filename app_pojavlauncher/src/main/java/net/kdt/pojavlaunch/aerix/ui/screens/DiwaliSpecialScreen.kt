/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The Diwali Special page: the Apex Launcher 2.0 announcement, opened from the Apex Client screen.
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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface

/** The announcement shown by the Diwali Special entry on the Apex Client screen. */
object ApexDiwali {
    const val TITLE = "Diwali Special"
    const val HEADLINE = "\uD83C\uDF87 A NEW BEGINNING IS COMING! \uD83C\uDF87"
    const val SUBHEAD = "\uD83E\uDE94 This Diwali, Apex Launcher has a special surprise for everyone!"
    const val VERSION = "\uD83D\uDE80 APEX LAUNCHER 2.0 IS COMING SOON!"
    const val TAGLINE = "Something bigger. Something better. Something completely NEW! \uD83D\uDD25"
    const val DATE_LABEL = "RELEASE DATE"
    const val DATE_VALUE = "20 NOVEMBER 2026"

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

    const val FOOTER = "APEX LAUNCHER 2.0"
    const val FOOTER_TAGLINE = "The Next Generation Begins. \uD83D\uDE80"
}

/** Diwali gold on the launcher's own crimson canvas. */
private val festivalGold = Color(0xFFFFB020)
private val festivalDeepRed = Color(0xFF6B0F1A)

@Composable
fun DiwaliSpecialScreen(onClose: () -> Unit) {
    AerixPage(title = ApexDiwali.TITLE, subtitle = "Apex Launcher 2.0 announcement") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
        ) {
            // The hero card: the headline over a warm crimson-to-gold wash.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(festivalDeepRed, AerixSurface.panelRaised, Color(0xFF2A0A10))
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
                    .padding(AerixSpacing.lg)
            ) {
                Column(
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
                                brush = Brush.horizontalGradient(
                                    listOf(festivalGold, Color(0xFFFF7A18))
                                ),
                                shape = CircleShape
                            )
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDCC5 ${ApexDiwali.DATE_LABEL}:",
                            color = Color(0xFF2A0A10),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ApexDiwali.DATE_VALUE,
                            color = Color(0xFF2A0A10),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            ApexBulletCard(ApexDiwali.lines, festivalGold)

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
                text = "Back to Apex Client",
                iconRes = null,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AerixSpacing.sm))
        }
    }
}

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
