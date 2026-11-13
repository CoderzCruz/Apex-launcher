/*
 * Rules of Vortex Launcher — the page every player reads before using v1.
 *
 * Carries the player-provided rules banner art, the big heading, a short plain-language rule
 * list and a Discord invite button. The rule text is the launcher's default set; the owner can
 * replace it with their exact server wording any time.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package net.kdt.pojavlaunch.aerix.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager

private const val DISCORD_INVITE = "https://discord.gg/wpjAVkf2V"

/** The rules a player reads before using Vortex Launcher v1, in the owner's wording order. */
private val VortexRules = listOf(
    "Be respectful to every player — no hate speech, harassment or bullying.",
    "No cheating, hacked clients or unfair advantages on any server.",
    "No spam, advertising, scam or phishing links anywhere.",
    "Follow the Discord server rules — the invite link is below.",
    "Keep your account safe — never share your login with anyone.",
    "Report bugs and give feedback in Discord instead of misusing features.",
    "Downloads and modpacks are for personal use — respect the creators."
)

/**
 * The Rules page: the player-provided banner art, a big "Rules Of Vortex Launcher" heading,
 * the rule list, and a Discord invite button that opens the server link.
 */
@Composable
fun RulesScreen(onClose: () -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AerixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // The owner's pixel-art rules banner.
        Image(
            painter = painterResource(R.drawable.rules_banner),
            contentDescription = "Rules banner",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2170f / 725f)
                .clip(RoundedCornerShape(AerixRadii.card))
        )

        // The big heading the owner asked for.
        Text(
            text = "Rules Of Vortex Launcher",
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AerixSurface.textPrimary
        )
        Text(
            text = "Read these before you play Vortex Launcher v1.",
            style = MaterialTheme.typography.bodyMedium,
            color = AerixSurface.textSecondary
        )

        // The rules themselves, numbered.
        VortexRules.forEachIndexed { index, rule ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
                Text(
                    text = rule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AerixSurface.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(AerixSpacing.xs))

        // Discord invite — opens the server link in the browser/Discord app.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AerixRadii.panel))
                .background(accent),
            shape = RoundedCornerShape(AerixRadii.panel),
            color = Color.Transparent,
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DISCORD_INVITE)))
                }
            }
        ) {
            Row(
                modifier = Modifier.padding(vertical = AerixSpacing.md),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_people),
                    contentDescription = null,
                    tint = AerixSurface.onAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(AerixSpacing.sm))
                Text(
                    text = "Join the Discord server",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AerixSurface.onAccent
                )
            }
        }
    }
}
