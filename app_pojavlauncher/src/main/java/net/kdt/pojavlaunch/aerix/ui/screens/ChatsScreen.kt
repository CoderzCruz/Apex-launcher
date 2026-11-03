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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
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
            ChatTarget.Service -> "Ask me anything \u00B7 Apex assistant"
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
                subtitle = if (ApexService.hasBrain) {
                    "AI assistant \u00B7 always here"
                } else {
                    "Knowledge base \u00B7 offline answers"
                },
                badge = ApexBadge.SERVICE,
                avatarText = "\u2728",
                avatarBrush = Brush.linearGradient(
                    listOf(serviceViolet, Color(0xFFB388FF))
                ),
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
    onClick: () -> Unit,
    avatarText: String? = null,
    avatarBrush: Brush? = null
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
                .background(
                    avatarBrush ?: Brush.linearGradient(
                        listOf(
                            AerixSurface.accent.copy(alpha = 0.25f),
                            AerixSurface.accent.copy(alpha = 0.25f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarText ?: name.trim().take(1).uppercase().ifBlank { "?" },
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

/** Apex Service wears violet, so it never looks like one of the player's conversations. */
private val serviceViolet = Color(0xFF7C5CFF)
private val serviceVioletDeep = Color(0xFF2A1B5E)
private val serviceInk = Color(0xFF120B2B)

private const val SERVICE_WELCOME =
    "Namaste! Main Apex Service hoon \u2014 Apex Launcher ka assistant.\n\n" +
        "Chat, voice notes, themes, Discord card, badges, install, Diwali \u2014 jo poochna hai " +
        "poocho. Kisi bhi room me @apex likhoge to wahin jawab dunga."

/** Tappable questions, so the first message is never a blank stare. */
private val SERVICE_PROMPTS = listOf(
    "Chat kaise use karu?",
    "Voice note kaise bhejein?",
    "2.0 me kya naya hai?",
    "Discord card set kaise karu?",
    "Badges kaise milte hain?",
    "APK kahan se download karu?"
)

@Composable
private fun ServiceThread(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val turns = ApexService.turns
    val thinking = ApexService.thinking
    val connected = ApexService.hasBrain

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || thinking) return
        draft = ""
        scope.launch { ApexService.ask(clean) }
    }

    LaunchedEffect(turns.size, thinking) {
        val count = turns.size + if (thinking) 1 else 0
        if (count > 0) runCatching { listState.animateScrollToItem(count - 1) }
    }

    // The whole page sits on a violet wash instead of the launcher's crimson.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(AerixRadii.card))
            .background(
                Brush.verticalGradient(
                    listOf(serviceInk, serviceVioletDeep.copy(alpha = 0.55f), serviceInk)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(AerixSpacing.sm)) {
            ServiceHero(
                connected = connected,
                model = ApexService.lastModel,
                onBack = onBack,
                onReset = { ApexService.resetConversation() }
            )

            ApexService.lastError?.let { error ->
                Spacer(Modifier.height(AerixSpacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AerixRadii.control))
                        .background(Color(0x33EF233C))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "\u26A0\uFE0F", fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = error,
                        color = Color(0xFFFFC9D2),
                        fontSize = 9.5.sp
                    )
                }
            }

            Spacer(Modifier.height(AerixSpacing.sm))

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (turns.isEmpty()) {
                    item(key = "welcome") {
                        ServiceWelcomeCard(prompts = SERVICE_PROMPTS, onPick = ::send)
                    }
                }
                items(turns, key = { it.id }) { turn ->
                    if (turn.fromUser) {
                        UserTurn(text = turn.text)
                    } else {
                        ServiceTurn(text = turn.text)
                    }
                }
                if (thinking) {
                    item(key = "thinking") { ServiceTyping() }
                }
            }

            Spacer(Modifier.height(AerixSpacing.sm))
            ServicePromptStrip(
                prompts = SERVICE_PROMPTS.take(4),
                enabled = !thinking,
                onPick = ::send
            )
            Spacer(Modifier.height(AerixSpacing.xs))
            ServiceComposer(
                value = draft,
                onValueChange = { draft = it.take(1000) },
                busy = thinking,
                onSend = { send(draft) }
            )
        }
    }
}

/* ── header ────────────────────────────────────────────────────────────── */

@Composable
private fun ServiceHero(
    connected: Boolean,
    model: String?,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\u2039", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(AerixSpacing.sm))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(serviceViolet, Color(0xFFB388FF)))
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\u2728", fontSize = 20.sp)
        }
        Spacer(Modifier.width(AerixSpacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ApexService.NAME,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (connected) Color(0xFF3DDC84) else Color(0xFFFFB020))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (connected) {
                        "Live \u00B7 " + (model ?: "Gemini")
                    } else {
                        "Offline brain \u00B7 knowledge base"
                    },
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 9.5.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(onClick = onReset)
                .padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            Text(
                text = "New chat",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/* ── empty state ───────────────────────────────────────────────────────── */

@Composable
private fun ServiceWelcomeCard(prompts: List<String>, onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AerixRadii.card))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, serviceViolet.copy(alpha = 0.35f), RoundedCornerShape(AerixRadii.card))
            .padding(AerixSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "\uD83E\uDD16", fontSize = 15.sp)
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Apex Service",
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            BadgeChip(ApexBadge.SERVICE)
        }
        Text(
            text = SERVICE_WELCOME,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 12.5.sp,
            lineHeight = 18.sp
        )
        Text(
            text = "Try one of these:",
            color = serviceViolet.copy(alpha = 0.9f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
        prompts.take(4).forEach { prompt ->
            ServiceSuggestionChip(text = prompt) { onPick(prompt) }
        }
    }
}

@Composable
private fun ServiceSuggestionChip(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AerixRadii.control))
            .background(serviceViolet.copy(alpha = 0.16f))
            .border(1.dp, serviceViolet.copy(alpha = 0.30f), RoundedCornerShape(AerixRadii.control))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.92f), fontSize = 11.5.sp)
        Spacer(Modifier.weight(1f))
        Text(text = "\u2192", color = serviceViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/* ── message styles ────────────────────────────────────────────────────── */

/** The player's line: crimson, right aligned, still the familiar chat bubble. */
@Composable
private fun UserTurn(text: String) {
    val density = LocalDensity.current
    val shape = remember(density) {
        TailBubbleShape(
            corner = with(density) { 16.dp.toPx() },
            tail = with(density) { 9.dp.toPx() },
            onRight = true
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .background(AerixSurface.accent.copy(alpha = 0.30f), shape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = text, color = Color.White, fontSize = 13.sp)
        }
    }
}

/**
 * Apex Service's answer: a full-width glass card with its own orb, no bubble tail, violet tint.
 * Deliberately a different shape from every player message so an answer never looks like chat.
 */
@Composable
private fun ServiceTurn(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(serviceViolet, Color(0xFFB388FF)))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\u2728", fontSize = 13.sp)
        }
        Spacer(Modifier.width(AerixSpacing.sm))
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(AerixRadii.card))
                .background(Color.White.copy(alpha = 0.07f))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(serviceViolet.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(AerixRadii.card)
                )
                .padding(horizontal = AerixSpacing.md, vertical = 10.dp)
        ) {
            Text(
                text = ApexService.NAME,
                color = serviceViolet,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.94f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

/** Three dots that breathe while the brain is working. */
@Composable
private fun ServiceTyping() {
    val transition = rememberInfiniteTransition(label = "service-typing")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "service-typing-phase"
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(serviceViolet, Color(0xFFB388FF)))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\u2728", fontSize = 13.sp)
        }
        Spacer(Modifier.width(AerixSpacing.sm))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AerixRadii.pill))
                .background(Color.White.copy(alpha = 0.07f))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(3) { index ->
                val wave = ((phase + index * 0.22f) % 1f)
                val alpha = 0.25f + 0.75f * (1f - kotlin.math.abs(wave * 2f - 1f))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = alpha))
                )
            }
        }
    }
}

/* ── prompt strip + composer ───────────────────────────────────────────── */

@Composable
private fun ServicePromptStrip(prompts: List<String>, enabled: Boolean, onPick: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AerixSpacing.xs),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp)
    ) {
        items(prompts) { prompt ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (enabled) 0.08f else 0.03f))
                    .border(1.dp, serviceViolet.copy(alpha = 0.30f), CircleShape)
                    .then(if (enabled) Modifier.clickable { onPick(prompt) } else Modifier)
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text(
                    text = prompt,
                    color = Color.White.copy(alpha = if (enabled) 0.80f else 0.35f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ServiceComposer(
    value: String,
    onValueChange: (String) -> Unit,
    busy: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, serviceViolet.copy(alpha = 0.40f), CircleShape)
            .padding(start = 14.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(serviceViolet),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = onSend),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = "Ask Apex Service\u2026",
                        color = Color.White.copy(alpha = 0.38f),
                        fontSize = 13.sp
                    )
                }
                inner()
            }
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (busy) {
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.10f)))
                    } else {
                        Brush.linearGradient(listOf(serviceViolet, Color(0xFF9D7BFF)))
                    }
                )
                .clickable(enabled = !busy, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (busy) "\u2026" else "\u2191",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
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
