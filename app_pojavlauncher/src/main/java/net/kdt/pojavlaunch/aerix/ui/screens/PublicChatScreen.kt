/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Public Chat: one shared room every Apex Launcher player lands in, hosted on the same social
 * backend the friends list and party chat already use.
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.social.SocialBackend
import net.kdt.pojavlaunch.aerix.social.SocialMessage
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface

/** How often the room is polled while it is on screen. */
private const val PUBLIC_CHAT_POLL_MS = 4_000L

/**
 * The shared room. Everyone signed in to Apex Socials lands in the same conversation, and the
 * author line prefers the in-game name so players recognise each other from the game.
 */
@Composable
fun PublicChatScreen(onOpenSocials: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf<List<SocialMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Opening the room…") }
    var sending by remember { mutableStateOf(false) }
    var joined by remember { mutableStateOf(false) }

    LaunchedEffect(SocialBackend.signedIn) {
        if (!SocialBackend.signedIn) {
            status = "Sign in to Apex Socials to join the public chat"
            return@LaunchedEffect
        }
        status = "Opening the room…"
        val room = SocialBackend.openPublicChat()
        joined = room != null
        status = if (room == null) {
            "Could not open the room. Check your connection."
        } else {
            "Room: ${room.title} · ${room.memberCount.coerceAtLeast(1)} online"
        }
        while (joined) {
            val latest = SocialBackend.loadPublicChatMessages()
            if (latest.isNotEmpty()) {
                messages = latest
                status = "Room: ${SocialBackend.PUBLIC_CHAT_TITLE} · ${latest.size} messages"
                if (listState.layoutInfo.totalItemsCount == 0) listState.scrollToItem(0)
            }
            delay(PUBLIC_CHAT_POLL_MS)
        }
    }

    AerixPage(title = "Public Chat", subtitle = "Everyone using Apex Launcher, one room") {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = status,
                color = AerixSurface.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = AerixSpacing.sm)
            )

            if (!SocialBackend.signedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
                        .padding(AerixSpacing.lg)
                ) {
                    Column {
                        Text(
                            text = "Sign in first",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "The public chat lives on the Apex social backend, so you need an " +
                                "account before you can read or post.",
                            color = AerixSurface.textSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(AerixSpacing.md))
                        AerixGhostButton(
                            text = "Open Socials",
                            iconRes = null,
                            onClick = onOpenSocials,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                items(messages, key = { it.id }) { message ->
                    PublicChatRow(message)
                }
            }

            Spacer(Modifier.height(AerixSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(2000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message the room…", color = AerixSurface.textMuted, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        val text = draft.trim()
                        if (text.isEmpty() || sending) return@KeyboardActions
                        sending = true
                        scope.launch {
                            val sent = SocialBackend.sendPublicChatMessage(text)
                            if (sent) draft = ""
                            sending = false
                        }
                    })
                )
                AerixGhostButton(
                    text = if (sending) "…" else "Send",
                    iconRes = null,
                    onClick = {
                        val text = draft.trim()
                        if (text.isEmpty() || sending) return@AerixGhostButton
                        sending = true
                        scope.launch {
                            val sent = SocialBackend.sendPublicChatMessage(text)
                            if (sent) draft = ""
                            sending = false
                        }
                    }
                )
            }
        }
    }
}

/** One message: the in-game name above the text, own messages tinted with the accent. */
@Composable
private fun PublicChatRow(message: SocialMessage) {
    val mine = message.sender == SocialBackend.myUserId()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (mine) AerixSurface.accent.copy(alpha = 0.18f) else AerixSurface.panelRaised,
                RoundedCornerShape(AerixRadii.card)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Column {
            Text(
                text = if (mine) "You" else SocialBackend.publicChatAuthorName(message),
                color = AerixSurface.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = message.body,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 13.sp
            )
        }
    }
}
