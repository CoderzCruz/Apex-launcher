/*
 * Apex Service chat — the assistant page opened from the Home header.
 *
 * Answers come from ApexService: Gemini when a key is present (the CI-baked
 * APEX_SERVICE_AI_KEY or the player's own), and the built-in local knowledge base otherwise.
 * Nothing here claims an "AI connected" state it cannot verify — the header hint shows which
 * source the next answer will use.
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.social.ApexService
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.theme.AerixThemeManager

/** One line shown in the transcript: the player's prompt or Apex Service's answer. */
private data class AiLine(val fromUser: Boolean, val text: String)

/**
 * The Apex Service page: a plain transcript with a send box. The service itself owns the
 * conversation, key fallback and model discovery — this screen only renders turns.
 */
@Composable
fun AiScreen(onClose: () -> Unit) {
    val accent = AerixThemeManager.currentAccent()
    val scope = rememberCoroutineScope()
    val turns = remember { mutableStateListOf<AiLine>() }
    val listState = rememberLazyListState()
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val hasKey = AerixPreferences.effectiveAiKey().isNotBlank()

    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty()) listState.animateScrollToItem(turns.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AerixSpacing.md)
    ) {
        // Header: back, title, and an honest hint about where the next answer comes from.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        net.kdt.pojavlaunch.R.drawable.aerix_ic_arrow_back
                    ),
                    contentDescription = "Back",
                    tint = AerixSurface.textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Apex Service",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AerixSurface.textPrimary
                )
                Text(
                    text = if (hasKey) "Gemini key ready" else "Local knowledge base (no key set)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasKey) accent else AerixSurface.textMuted
                )
            }
            Text(
                text = if (busy) "thinking…" else "",
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = AerixSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            if (turns.isEmpty()) {
                item {
                    Text(
                        text = "Ask anything about the launcher — installs, renderers, controls, settings. " +
                            "With a key it answers from Gemini; without one it uses the built-in guide.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AerixSurface.textSecondary
                    )
                }
            }
            items(turns) { line -> AiBubble(line, accent) }
            if (busy) {
                item {
                    Text(
                        text = "…",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        modifier = Modifier.padding(start = AerixSpacing.sm)
                    )
                }
            }
        }

        // Send row.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text("Ask Apex Service…", color = AerixSurface.textMuted, fontSize = 13.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = AerixSurface.borderSoft,
                    focusedTextColor = AerixSurface.textPrimary,
                    unfocusedTextColor = AerixSurface.textPrimary,
                    cursorColor = accent
                )
            )
            IconButton(
                enabled = prompt.isNotBlank() && !busy,
                onClick = {
                    val question = prompt.trim()
                    if (question.isEmpty()) return@IconButton
                    prompt = ""
                    busy = true
                    turns.add(AiLine(fromUser = true, text = question))
                    scope.launch {
                        val answer = withContext(Dispatchers.IO) {
                            runCatching { ApexService.ask(question) }
                                .getOrElse { "I could not reach the assistant just now — try again." }
                        }
                        turns.add(AiLine(fromUser = false, text = answer))
                        busy = false
                    }
                }
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        net.kdt.pojavlaunch.R.drawable.aerix_ic_send
                    ),
                    contentDescription = "Send",
                    tint = if (prompt.isNotBlank() && !busy) accent else AerixSurface.textMuted
                )
            }
        }
    }
}

@Composable
private fun AiBubble(line: AiLine, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    if (line.fromUser) accent.copy(alpha = 0.18f) else AerixSurface.panel,
                    RoundedCornerShape(AerixRadii.cardSmall)
                )
                .padding(horizontal = AerixSpacing.md, vertical = AerixSpacing.sm)
        ) {
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (line.fromUser) AerixSurface.textPrimary else AerixSurface.textPrimary
            )
        }
    }
}
