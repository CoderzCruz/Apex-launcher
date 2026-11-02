/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Chats: direct messages and group rooms in one place, with the tailed bubbles Discord uses.
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

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.aerix.components.AerixGhostButton
import net.kdt.pojavlaunch.aerix.social.ApexBadge
import net.kdt.pojavlaunch.aerix.social.ApexBadges
import net.kdt.pojavlaunch.aerix.social.ApexService
import net.kdt.pojavlaunch.aerix.social.SocialBackend
import net.kdt.pojavlaunch.aerix.social.SocialMessage
import net.kdt.pojavlaunch.aerix.social.SocialParty
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import java.io.File

private const val CHAT_POLL_MS = 3_500L

/** Which list the left column shows. */
private enum class ChatTab(val label: String) {
    DMS("Direct messages"),
    GROUPS("Groups"),
    PUBLIC("Public")
}

/** Where the conversation is currently pointing. */
private sealed interface ChatTarget {
    data class Dm(val peerId: String, val name: String) : ChatTarget
    data class Group(val partyId: String, val name: String) : ChatTarget
    data object Public : ChatTarget

    /** The assistant's own thread. */
    data object Service : ChatTarget
}

/**
 * One screen for every conversation: direct messages, group rooms and the shared public room.
 * The list column stays on the left, the thread on the right, exactly like a desktop chat client;
 * on a phone the thread takes over the screen once a chat is opened.
 */
@Composable
fun ChatsScreen(onOpenSocials: () -> Unit) {
    var tab by remember { mutableStateOf(ChatTab.DMS) }
    var target by remember { mutableStateOf<ChatTarget?>(null) }
    val signedIn = SocialBackend.signedIn

    AerixPage(
        title = "Chats",
        subtitle = when (val current = target) {
            is ChatTarget.Dm -> current.name
            is ChatTarget.Group -> current.name
            ChatTarget.Public -> "Apex Public Chat"
            ChatTarget.Service -> ApexService.TAGLINE
            null -> "Direct messages and groups"
        }
    ) {
        if (!signedIn) {
            SignInPrompt(onOpenSocials)
            return@AerixPage
        }

        val open = target
        if (open is ChatTarget.Service) {
            ServiceThread(onBack = { target = null })
        } else if (open != null) {
            ChatThread(
                target = open,
                onBack = { target = null }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
            ) {
                ChatTab.entries.forEach { entry ->
                    ChatPill(
                        label = entry.label,
                        selected = tab == entry,
                        modifier = Modifier.weight(1f)
                    ) { tab = entry }
                }
            }
            Spacer(Modifier.height(AerixSpacing.md))
            when (tab) {
                ChatTab.DMS -> DmList(
                    onOpen = { friend -> target = friend },
                    onOpenService = { target = ChatTarget.Service }
                )
                ChatTab.GROUPS -> GroupList { group -> target = group }
                ChatTab.PUBLIC -> PublicRoomCard { target = ChatTarget.Public }
            }
        }
    }
}

/* ── list column ───────────────────────────────────────────────────────── */

@Composable
private fun ChatPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AerixRadii.control))
            .background(if (selected) AerixSurface.accent.copy(alpha = 0.22f) else AerixSurface.panelRaised)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else AerixSurface.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun DmList(onOpen: (ChatTarget.Dm) -> Unit, onOpenService: () -> Unit) {
    val friends = SocialBackend.friends
    LazyColumn(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
        // The assistant is pinned above real friends, the way Discord keeps its helper on top.
        item(key = "apex-service") {
            ChatListRow(
                name = ApexService.NAME,
                subtitle = if (ApexService.hasBrain) ApexService.TAGLINE else "Knowledge base · add an AI key",
                badge = ApexBadge.SERVICE,
                onClick = onOpenService
            )
        }
        if (friends.isEmpty()) {
            item(key = "no-friends") {
                EmptyCard(
                    title = "No friends yet",
                    body = "Add someone from Socials and your conversation shows up here."
                )
            }
        }
        items(friends, key = { it.id }) { friend ->
            ChatListRow(
                name = friend.displayName.ifBlank { friend.mcUsername },
                subtitle = friend.statusLine?.takeIf { it.isNotBlank() } ?: friend.mcUsername,
                badge = ApexBadges.forName(friend.displayName.ifBlank { friend.mcUsername })
            ) { onOpen(ChatTarget.Dm(friend.id, friend.displayName.ifBlank { friend.mcUsername })) }
        }
    }
}

@Composable
private fun GroupList(onOpen: (ChatTarget.Group) -> Unit) {
    val parties = SocialBackend.parties
    LaunchedEffect(Unit) { runCatching { SocialBackend.refreshParties() } }
    if (parties.isEmpty()) {
        EmptyCard(
            title = "No groups right now",
            body = "Create a party from Socials and its chat opens here for every member."
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)) {
        items(parties, key = { it.id }) { party: SocialParty ->
            ChatListRow(
                name = party.title,
                subtitle = "${party.memberCount.coerceAtLeast(1)} members · host ${party.hostName}",
                badge = ApexBadges.forName(party.hostName)
            ) { onOpen(ChatTarget.Group(party.id, party.title)) }
        }
    }
}

@Composable
private fun PublicRoomCard(onOpen: () -> Unit) {
    ChatListRow(
        name = "Apex Public Chat",
        subtitle = "One room every Apex Launcher player shares",
        badge = null,
        onClick = onOpen
    )
}

@Composable
private fun ChatListRow(
    name: String,
    subtitle: String,
    badge: ApexBadge?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AerixRadii.card))
            .background(AerixSurface.panelRaised)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AerixSurface.accent.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().take(1).uppercase().ifBlank { "?" },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                BadgeChip(badge)
            }
            Text(text = subtitle, color = AerixSurface.textSecondary, fontSize = 11.sp)
        }
        Text(text = "\u203A", color = AerixSurface.textMuted, fontSize = 18.sp)
    }
}

/* ── thread ────────────────────────────────────────────────────────────── */

@Composable
private fun ChatThread(target: ChatTarget, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var messages by remember(target) { mutableStateOf<List<SocialMessage>>(emptyList()) }
    var draft by remember(target) { mutableStateOf("") }
    var sending by remember(target) { mutableStateOf(false) }
    var playingUrl by remember { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var notice by remember(target) { mutableStateOf<String?>(null) }
    // Answers Apex Service gave inside this room, newest last. They live in the app, so a mention
    // gets one answer per player instead of the whole room fighting over who replies.
    var botReplies by remember(target) { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    val answered = remember(target) { mutableSetOf<Long>() }
    val voiceFile = remember(context) { File(context.cacheDir, "apex-voice.m4a") }

    suspend fun load(): List<SocialMessage> = when (target) {
        is ChatTarget.Dm -> SocialBackend.loadMessages(target.peerId)
        is ChatTarget.Group -> SocialBackend.loadPartyMessages(target.partyId)
        ChatTarget.Public -> SocialBackend.loadPublicChatMessages()
        ChatTarget.Service -> emptyList()
    }

    LaunchedEffect(target) {
        if (target == ChatTarget.Public) SocialBackend.openPublicChat()
        while (true) {
            val latest = runCatching { load() }.getOrDefault(emptyList())
            if (latest.isNotEmpty()) messages = latest
            delay(CHAT_POLL_MS)
        }
    }

    LaunchedEffect(target, messages) {
        val asked = messages.filter {
            !answered.contains(it.id) &&
                it.sender != SocialBackend.myUserId() &&
                ApexService.mentionsUs(it.body)
        }
        asked.take(2).forEach { message ->
            answered += message.id
            val recent = messages.takeLast(6).map { "${authorOf(it)}: ${it.body}" }
            val answer = runCatching { ApexService.reply(message.body, recent) }.getOrNull()
            if (!answer.isNullOrBlank()) {
                botReplies = botReplies + (message.id to answer)
                ApexService.lastError?.let { notice = it }
            }
        }
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || sending) return
        sending = true
        scope.launch {
            val ok = when (target) {
                is ChatTarget.Dm -> SocialBackend.sendMessage(target.peerId, clean)
                is ChatTarget.Group -> SocialBackend.sendPartyMessage(target.partyId, clean)
                ChatTarget.Public -> SocialBackend.sendPublicChatMessage(clean)
                ChatTarget.Service -> false
            }
            if (ok) {
                draft = ""
                messages = runCatching { load() }.getOrDefault(messages)
            } else {
                notice = SocialBackend.lastError ?: "Message could not be sent"
            }
            sending = false
        }
    }

    fun stopRecorder(): File? {
        val active = recorder ?: return null
        recorder = null
        recording = false
        runCatching { active.stop() }
        runCatching { active.release() }
        return voiceFile.takeIf { it.isFile && it.length() > 512 }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AerixGhostButton(text = "\u2039  Back", iconRes = null, onClick = onBack)
            Spacer(Modifier.width(AerixSpacing.sm))
            Text(
                text = "${messages.size} messages",
                color = AerixSurface.textMuted,
                fontSize = 10.sp
            )
        }
        notice?.let {
            Text(text = it, color = AerixSurface.errorDarkContainer, fontSize = 10.sp)
        }
        Spacer(Modifier.height(AerixSpacing.sm))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    mine = message.sender == SocialBackend.myUserId(),
                    authorName = authorOf(message),
                    playingUrl = playingUrl,
                    onPlay = { url ->
                        player?.let { runCatching { it.stop(); it.release() } }
                        player = null
                        if (playingUrl == url) {
                            playingUrl = null
                        } else {
                            playingUrl = url
                            runCatching {
                                val mp = MediaPlayer()
                                mp.setDataSource(url)
                                mp.setOnCompletionListener {
                                    playingUrl = null
                                    runCatching { it.release() }
                                }
                                mp.setOnPreparedListener { it.start() }
                                mp.prepareAsync()
                                player = mp
                            }.onFailure { playingUrl = null }
                        }
                    }
                )
            }
            items(botReplies, key = { "apex-service-" + it.first }) { entry ->
                BotRow(text = entry.second)
            }
        }

        if (target is ChatTarget.Group || target == ChatTarget.Public) {
            Text(
                text = "Tip: message me with @apex — main yahin jawab dunga.",
                color = AerixSurface.textMuted,
                fontSize = 9.5.sp
            )
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
                placeholder = { Text("Message…", color = AerixSurface.textMuted, fontSize = 12.sp) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(draft) })
            )
            AerixGhostButton(
                text = if (recording) "\u23F9" else "\uD83C\uDFA4",
                iconRes = null,
                onClick = {
                    if (recording) {
                        val file = stopRecorder() ?: return@AerixGhostButton
                        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@AerixGhostButton
                        scope.launch {
                            sending = true
                            val ok = when (target) {
                                is ChatTarget.Dm -> SocialBackend.uploadVoice(bytes)?.let { url ->
                                    SocialBackend.sendMessage(target.peerId, "Voice message", attachmentUri = android.net.Uri.parse(url))
                                } ?: false
                                is ChatTarget.Group,
                                ChatTarget.Public -> SocialBackend.sendPublicChatVoice(bytes)
                                ChatTarget.Service -> false
                            }
                            if (!ok) notice = SocialBackend.lastError ?: "Voice message failed"
                            messages = runCatching { load() }.getOrDefault(messages)
                            sending = false
                            runCatching { file.delete() }
                        }
                    } else {
                        runCatching {
                            @Suppress("DEPRECATION")
                            val rec = MediaRecorder()
                            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
                            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                            rec.setAudioEncodingBitRate(64_000)
                            rec.setAudioSamplingRate(44_100)
                            rec.setOutputFile(voiceFile.absolutePath)
                            rec.prepare()
                            rec.start()
                            recorder = rec
                            recording = true
                        }.onFailure {
                            recorder = null
                            recording = false
                            notice = "Microphone unavailable"
                        }
                    }
                }
            )
            AerixGhostButton(
                text = if (sending) "\u2026" else "Send",
                iconRes = null,
                onClick = { send(draft) }
            )
        }
    }
}

private fun authorOf(message: SocialMessage): String {
    val mine = message.sender == SocialBackend.myUserId()
    if (mine) return SocialBackend.myEffectiveDisplayName()
    SocialBackend.friends.firstOrNull { it.id == message.sender }?.let { friend ->
        return friend.displayName.ifBlank { friend.mcUsername }
    }
    return message.senderName?.takeIf { it.isNotBlank() } ?: "Player"
}

/* ── bubbles ───────────────────────────────────────────────────────────── */

/**
 * A rounded bubble with a tail in one bottom corner, the shape Discord draws around a message.
 * The tail is a small triangle cut into the outline rather than an extra drawn shape, so it scales
 * with the bubble and never overlaps the text.
 */
private class TailBubbleShape(
    private val corner: Float,
    private val tail: Float,
    private val onRight: Boolean
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val c = corner.coerceAtMost(size.minDimension / 2f)
        val t = tail.coerceAtMost(size.width / 3f)

        path.moveTo(c, 0f)
        path.lineTo(size.width - c, 0f)
        path.quadraticBezierTo(size.width, 0f, size.width, c)
        path.lineTo(size.width, size.height - c)
        path.quadraticBezierTo(size.width, size.height, size.width - c, size.height)

        if (onRight) {
            // Tail hangs off the bottom-right corner, pointing down to the author.
            path.lineTo(size.width - t, size.height)
            path.lineTo(size.width - t * 0.15f, size.height + tail * 0.9f)
            path.lineTo(size.width - t * 0.95f, size.height)
        } else {
            path.lineTo(c, size.height)
        }

        if (!onRight) {
            // Tail hangs off the bottom-left corner for everyone else.
            path.lineTo(t * 0.95f, size.height)
            path.lineTo(t * 0.15f, size.height + tail * 0.9f)
            path.lineTo(t, size.height)
        }

        path.quadraticBezierTo(0f, size.height, 0f, size.height - c)
        path.lineTo(0f, c)
        path.quadraticBezierTo(0f, 0f, c, 0f)
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is TailBubbleShape && other.corner == corner && other.tail == tail && other.onRight == onRight

    override fun hashCode(): Int = (corner.hashCode() * 31 + tail.hashCode()) * 31 + onRight.hashCode()
}

/**
 * Shared shell of every message: an optional author line, then the tailed bubble.
 * [onClick] makes the whole bubble tappable, which voice notes use for playback.
 */
@Composable
private fun BubbleBox(
    mine: Boolean,
    authorName: String?,
    badge: ApexBadge?,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val bubbleShape = remember(mine, density) {
        TailBubbleShape(
            corner = with(density) { 16.dp.toPx() },
            tail = with(density) { 9.dp.toPx() },
            onRight = mine
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start
    ) {
        if (!mine && authorName != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            ) {
                Text(
                    text = authorName,
                    color = AerixSurface.accent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                BadgeChip(badge)
            }
        }
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .padding(bottom = 6.dp)
                .background(
                    color = if (mine) AerixSurface.accent.copy(alpha = 0.28f) else AerixSurface.panelRaised,
                    shape = bubbleShape
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MessageRow(
    message: SocialMessage,
    mine: Boolean,
    authorName: String,
    playingUrl: String?,
    onPlay: (String) -> Unit
) {
    val audioUrl = message.attachmentUrl?.takeIf {
        it.startsWith("http") && (it.endsWith(".m4a") || it.contains("voice-"))
    }
    val playAction: (() -> Unit)? = if (audioUrl != null) {
        { onPlay(audioUrl) }
    } else {
        null
    }
    BubbleBox(
        mine = mine,
        authorName = if (mine) null else authorName,
        badge = if (mine) null else ApexBadges.forName(authorName),
        onClick = playAction
    ) {
        if (audioUrl != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = if (playingUrl == audioUrl) "\u23F8" else "\u25B6\uFE0F", fontSize = 14.sp)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Voice message",
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Text(text = message.body, color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp)
        }
    }
}

/** An answer Apex Service posted inside a room. */
@Composable
private fun BotRow(text: String) {
    BubbleBox(mine = false, authorName = ApexService.NAME, badge = ApexBadge.SERVICE) {
        Text(text = text, color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp)
    }
}

/* ── the assistant's own thread ────────────────────────────────────────── */

private const val SERVICE_WELCOME =
    "Namaste! Main Apex Service hoon — Apex Launcher ka assistant.\n\n" +
        "Pooch sakte ho: chat kaise use karein, voice note kaise bhejein, themes, Discord card, " +
        "badges, install, ya koi bhi sawaal. Kisi bhi room me @apex likhoge to wahin jawab dunga."

@Composable
private fun ServiceThread(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val turns = ApexService.turns
    val thinking = ApexService.thinking

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || thinking) return
        draft = ""
        scope.launch { ApexService.ask(clean) }
    }

    LaunchedEffect(turns.size, thinking) {
        val count = turns.size + if (thinking) 1 else 0
        if (count > 0) listState.scrollToItem(count - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AerixGhostButton(text = "\u2039  Back", iconRes = null, onClick = onBack)
            Spacer(Modifier.width(AerixSpacing.sm))
            Column {
                Text(
                    text = ApexService.NAME,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (ApexService.hasBrain) "AI connected · Gemini" else "Built-in answers · AI key Settings me daalo",
                    color = AerixSurface.textMuted,
                    fontSize = 9.5.sp
                )
            }
        }
        ApexService.lastError?.let { error ->
            Text(text = error, color = AerixSurface.errorDarkContainer, fontSize = 9.5.sp)
        }
        Spacer(Modifier.height(AerixSpacing.sm))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (turns.isEmpty()) {
                item(key = "welcome") {
                    BubbleBox(mine = false, authorName = ApexService.NAME, badge = ApexBadge.SERVICE) {
                        Text(
                            text = SERVICE_WELCOME,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            items(turns, key = { it.id }) { turn ->
                BubbleBox(
                    mine = turn.fromUser,
                    authorName = if (turn.fromUser) null else ApexService.NAME,
                    badge = if (turn.fromUser) null else ApexBadge.SERVICE
                ) {
                    Text(text = turn.text, color = Color.White.copy(alpha = 0.95f), fontSize = 13.sp)
                }
            }
            if (thinking) {
                item(key = "thinking") {
                    BubbleBox(mine = false, authorName = ApexService.NAME, badge = ApexBadge.SERVICE) {
                        Text(text = "…", color = Color.White, fontSize = 14.sp)
                    }
                }
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
                onValueChange = { draft = it.take(1000) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask Apex Service…", color = AerixSurface.textMuted, fontSize = 12.sp) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(draft) })
            )
            AerixGhostButton(text = "Send", iconRes = null, onClick = { send(draft) })
        }
    }
}

/* ── small pieces ──────────────────────────────────────────────────────── */

@Composable
private fun BadgeChip(badge: ApexBadge?) {
    if (badge == null) return
    Spacer(Modifier.width(6.dp))
    Box(
        modifier = Modifier
            .background(Color(badge.colorArgb).copy(alpha = 0.20f), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = badge.chip,
            color = Color(badge.colorArgb),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.lg)
    ) {
        Column {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(text = body, color = AerixSurface.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SignInPrompt(onOpenSocials: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AerixSurface.panelRaised, RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.lg)
    ) {
        Column {
            Text(text = "Sign in first", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Chats run on the Apex social backend, so an account is needed before " +
                    "messages can be read or sent.",
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
}
