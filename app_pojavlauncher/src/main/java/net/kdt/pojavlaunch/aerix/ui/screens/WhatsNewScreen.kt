/*
 * Aerix Launcher UI layer for PojavLauncher.
 * The "What's New" page for Apex Launcher 2.0: the real, shipped features of this release.
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Diwali gold on the launcher's own crimson canvas — the same face as the announcement page. */
private val whatsGold = Color(0xFFFFB020)
private val whatsAmber = Color(0xFFFF7A18)
private val whatsDeepRed = Color(0xFF6B0F1A)
private val whatsNight = Color(0xFF2A0A10)

/**
 * Everything 2.0 actually ships, one card per feature. Every line here describes something a
 * player can open and use — the page never sells a promise as if it were finished.
 */
private data class WhatsEntry(val emoji: String, val title: String, val body: String)

private val WHATS_ENTRIES = listOf(
    WhatsEntry(
        "✨",
        "Apex Service — your AI assistant",
        "It now lives in Chats as a pinned conversation with its own emblem. Real Gemini answers " +
            "when an AI key is set, an honest knowledge base when it isn't, starter prompts, " +
            "breathing typing dots and a New chat reset. Ask it about chat, themes, badges — anything."
    ),
    WhatsEntry(
        "🎨",
        "A brand-new look",
        "Crimson Noir on the home screen, glass panels everywhere, and every page now follows your " +
            "accent theme — Chats, the assistant, settings, all of it."
    ),
    WhatsEntry(
        "👥",
        "Socials that feel alive",
        "Friends, groups and the shared public room with voice notes, badges, and player avatars " +
            "taken straight from their Minecraft profile — on profiles, rows and chats alike."
    ),
    WhatsEntry(
        "🪔",
        "Diwali Special",
        "A countdown to launch day with a live timer, a calendar reminder, share and copy actions, " +
            "and premium gold animations across the whole page."
    ),
    WhatsEntry(
        "🔒",
        "Honest by design",
        "Branding fields stay owner-locked, errors are shown instead of hidden, and the About tab " +
            "keeps the real credits: a GPL-3.0 fork of PojavLauncher, built by entitybrian."
    ),
    WhatsEntry(
        "⚡",
        "Under the hood",
        "Live update checks, safer chat polling, quieter battery use, and dozens of fixes across " +
            "the launcher since 1.0."
    )
)

/** The 2.0 changelog, opened from the Diwali announcement page. */
@Composable
fun WhatsNewScreen(onClose: () -> Unit) {
    val live = ApexDiwali.millisUntilRelease() <= 0L

    AerixPage(title = "What's New", subtitle = "Apex Launcher 2.0 · version 2.0.0") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            // The release card: LIVE from the first moment of launch day, countdown until then.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.card))
                    .background(
                        Brush.verticalGradient(listOf(whatsDeepRed, whatsNight, Color(0xFF14040A)))
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(whatsGold.copy(alpha = 0.75f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(AerixRadii.card)
                    )
                    .padding(AerixSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (live) "\uD83C\uDF86 IT IS LIVE \uD83C\uDF86" else "\uD83D\uDE80 APEX LAUNCHER 2.0",
                    color = whatsGold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (live) {
                        "Shipped on ${ApexDiwali.DATE_VALUE} — thank you for waiting with us."
                    } else {
                        "Arriving ${ApexDiwali.DATE_VALUE}. Here is everything it brings."
                    },
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ApexDiwali.TAGLINE,
                    color = AerixSurface.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            WHATS_ENTRIES.forEach { entry ->
                FeatureRow(entry)
            }

            // Closing note, echoing the announcement so the two pages feel like one story.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.card))
                    .background(AerixSurface.panelRaised)
                    .border(1.dp, whatsGold.copy(alpha = 0.35f), RoundedCornerShape(AerixRadii.card))
                    .padding(AerixSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Thank you for being part of our journey. Your support makes Apex Launcher stronger! ❤️",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "LAUNCHED \u2013 04.11.2026",
                    color = whatsGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            AerixGhostButton(
                text = "\u2039  Back to announcement",
                iconRes = null,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(AerixSpacing.sm))
        }
    }
}

/** One feature: emoji medallion, title, and the honest description of what shipped. */
@Composable
private fun FeatureRow(entry: WhatsEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AerixRadii.card))
            .background(AerixSurface.panelRaised)
            .border(1.dp, whatsAmber.copy(alpha = 0.22f), RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.md),
        verticalAlignment = Alignment.Top
    ) {
        FeatureMedallion(entry.emoji)
        Spacer(Modifier.width(AerixSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = entry.body,
                color = AerixSurface.textSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun FeatureMedallion(emoji: String) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(whatsGold.copy(alpha = 0.14f))
            .border(1.dp, whatsGold.copy(alpha = 0.38f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 16.sp)
    }
}
