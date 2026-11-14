/*
 * Vortex MC quick-join page — the rail entry below Rules.
 *
 * Shows the owner's server art with "Sooth Hosting" beneath it, the saved server address
 * (editable right there, persisted in AerixPreferences), and the animated "Click to Play"
 * button. Tapping it arms GameRunner's pending-server address and launches the selected
 * instance — Minecraft joins the server by itself (quickPlay on 1.20+, --server/--port on
 * older versions). With no instance selected the button walks to Create Instance instead,
 * leaving no pending address behind.
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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.bridge.PojavInstanceBridge
import net.kdt.pojavlaunch.aerix.bridge.PojavLaunchBridge
import net.kdt.pojavlaunch.aerix.components.apexPress
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager
import net.kdt.pojavlaunch.aerix.theme.ThemeMotion
import net.kdt.pojavlaunch.aerix.theme.accentGlow
import net.kdt.pojavlaunch.utils.jre.GameRunner

/**
 * The Vortex MC page: server art, "Sooth Hosting", the editable server address and the
 * animated "Click to Play" button that launches straight into the server.
 */
@Composable
fun VortexMcScreen(
    activity: FragmentActivity,
    onClose: () -> Unit,
    onNeedInstance: () -> Unit
) {
    val accent = AerixThemeManager.currentAccent()
    val serverAddress = AerixPreferences.vortexMcServer
    var editingServerIp by remember { mutableStateOf(false) }

    // The IP option lives right here: pencil opens a small dialog, Save persists the address.
    if (editingServerIp) {
        var draft by remember { mutableStateOf(serverAddress) }
        AlertDialog(
            onDismissRequest = { editingServerIp = false },
            containerColor = AerixSurface.panelRaised,
            title = {
                Text("Server IP", color = AerixSurface.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("host:port", color = AerixSurface.textMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = AerixSurface.borderSoft,
                        focusedTextColor = AerixSurface.textPrimary,
                        unfocusedTextColor = AerixSurface.textPrimary,
                        cursorColor = accent
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AerixPreferences.saveVortexMcServer(draft)
                    editingServerIp = false
                }) {
                    Text("Save", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingServerIp = false }) {
                    Text("Cancel", color = AerixSurface.textSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AerixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.md)
    ) {
        // The owner's server art.
        Image(
            painter = painterResource(R.drawable.vortex_mc_card),
            contentDescription = "Vortex Mc ip",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1280f / 575f)
                .clip(RoundedCornerShape(AerixRadii.card))
        )

        Text(
            text = "Vortex Mc ip",
            fontSize = 26.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AerixSurface.textPrimary
        )
        Text(
            text = "Sooth Hosting",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )

        // Address row with the edit option.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = serverAddress,
                style = MaterialTheme.typography.labelLarge,
                color = AerixSurface.textSecondary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { editingServerIp = true },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_edit_outlined),
                    contentDescription = "Edit server IP",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = "Launches your selected instance and joins this server automatically.",
            style = MaterialTheme.typography.labelSmall,
            color = AerixSurface.textMuted
        )

        // Click to Play: dip-and-spring press plus the breathing accent glow.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AerixRadii.panel))
                .apexPress(onClick = {
                    if (PojavInstanceBridge.selected != null) {
                        GameRunner.setPendingServerAddress(serverAddress)
                        PojavLaunchBridge.play(activity)
                    } else {
                        onNeedInstance()
                    }
                })
                .accentGlow(accent, active = true, enabled = ThemeMotion.rememberEnabled())
                .height(48.dp),
            shape = RoundedCornerShape(AerixRadii.panel),
            color = accent,
            contentColor = AerixSurface.onAccent
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.aerix_ic_play_arrow_filled),
                    contentDescription = null,
                    tint = AerixSurface.onAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(AerixSpacing.xs))
                Text(
                    text = "Click to Play",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AerixSurface.onAccent
                )
            }
        }

        // Back affordance for the rail page.
        TextButton(onClick = onClose) {
            Text("Back to Home", color = AerixSurface.textSecondary)
        }
    }
}
