/*
 * Apex Service: the assistant that lives inside Apex Launcher's chat.
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

package net.kdt.pojavlaunch.aerix.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * One line of the private conversation with the assistant.
 * Kept in memory only: the assistant is a helper, not a chat archive.
 */
data class ServiceTurn(
    val id: Long,
    val fromUser: Boolean,
    val text: String
)

/**
 * "Apex Service" — the assistant players can talk to, and which answers a mention in any room.
 *
 * The brain is pluggable. With a Google AI key saved in Settings it answers through the Gemini
 * API, so it can hold a real conversation. Without a key it still answers instantly from a small
 * built-in knowledge base about the launcher, so the feature never looks broken or dead.
 */
object ApexService {

    const val NAME = "Apex Service"
    const val TAGLINE = "AI assistant · pooch kuch bhi"

    /** How a player calls the assistant inside a shared room. */
    private val MENTIONS = listOf(
        "@apex service", "@apex", "apex service", "!apex", "/apex", "hey apex", "hi apex"
    )

    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta"

    /**
     * Used only when the live model list cannot be fetched. Google retires model names on a
     * schedule (2.5 Flash and older 404 now), so the real list is discovered at runtime and this
     * is just a safety net, newest first.
     */
    private val FALLBACK_MODELS = listOf(
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-flash-latest",
        "gemini-3.5-flash-lite",
        "gemini-2.5-flash"
    )

    private val NON_CHAT_MARKERS = listOf(
        "tts", "image", "audio", "embedding", "aqa", "live", "native",
        "video", "veo", "imagen", "learnlm", "robotics", "computer"
    )

    /** Models this key can actually call, filled in on first use. */
    private var modelCache: List<String>? = null

    /** The model that answered last, so a busy flagship never costs two tries on every message. */
    private var preferredModel: String? = null

    /** Name of the model currently answering, for the chat header. */
    var lastModel by mutableStateOf<String?>(null)
        private set

    /** Private conversation with the assistant, newest last. */
    var turns by mutableStateOf<List<ServiceTurn>>(emptyList())
        private set

    /** True while a Gemini request is in flight, so the UI can show "typing…". */
    var thinking by mutableStateOf(false)
        private set

    /** Last failure, for the UI to explain itself. */
    var lastError by mutableStateOf<String?>(null)
        private set

    private var nextId = 1L
    private var lastCallMs = 0L

    /** True when the player can get real AI answers instead of the built-in knowledge base. */
    val hasBrain: Boolean get() = AerixPreferences.effectiveAiKey().isNotBlank()

    /** Does this room message call the assistant? */
    fun mentionsUs(text: String): Boolean {
        val clean = text.trim().lowercase()
        if (clean.isEmpty()) return false
        return MENTIONS.any { clean.contains(it) }
    }

    /** Removes the "@apex" style prefix so the model sees a plain question. */
    private fun stripMention(text: String): String {
        var clean = text.trim()
        MENTIONS.forEach { mention ->
            val index = clean.lowercase().indexOf(mention)
            if (index == 0) clean = clean.substring(mention.length).trim()
        }
        return clean.trim().trimStart(',', ':', '-', '—').trim().ifBlank { text.trim() }
    }

    fun resetConversation() {
        turns = emptyList()
        lastError = null
    }

    /**
     * Asks the assistant something and appends both sides to the private conversation.
     * Returns the answer text so a room can show it inline as well.
     */
    suspend fun ask(question: String, roomContext: List<String> = emptyList()): String {
        val clean = question.trim()
        if (clean.isEmpty()) return ""
        turns = turns + ServiceTurn(nextId++, fromUser = true, text = clean)
        thinking = true
        val answer = try {
            reply(clean, roomContext)
        } finally {
            thinking = false
        }
        turns = turns + ServiceTurn(nextId++, fromUser = false, text = answer)
        return answer
    }

    /** One answer, without touching the private conversation. */
    suspend fun reply(question: String, roomContext: List<String> = emptyList()): String {
        val prompt = stripMention(question)
        if (prompt.isBlank()) return "Bolo, kya jaanna hai? Apex Launcher, chat, themes — kuch bhi."
        val key = AerixPreferences.effectiveAiKey()
        if (key.isNotBlank()) {
            val remote = askGemini(key, prompt, roomContext)
            if (remote != null) {
                lastError = null
                return remote
            }
        }
        return localAnswer(prompt)
    }

    /* ── Gemini ─────────────────────────────────────────────────────────── */

    private fun systemPrompt(context: List<String>): String = buildString {
        append("You are Apex Service, the friendly in-app assistant of Apex Launcher, ")
        append("a Minecraft: Java Edition launcher for Android (a GPL-3.0 fork of PojavLauncher ")
        append("by the Apex team; the original author credit must always stay). ")
        append("Things Apex Launcher has: a Home screen with Crimson Noir theme, Discover, ")
        append("Instance/Library management, Accounts, Settings, Contribution page with gold-ringed ")
        append("CEO badge, a Diwali special screen, Discord Rich Presence with an editable card image, ")
        append("Socials (friends, parties, profiles) and a Chats section with Direct messages, Groups ")
        append("and one shared Public room. Chats support text and voice notes. ")
        append("Live voice chat (a real voice channel) is NOT available yet — say so honestly and ")
        append("point people to the Discord server instead. ")
        append("Rules: answer in the same language the player used (Hindi, Hinglish or English), ")
        append("stay under 70 words, be warm and concrete, never invent settings that do not exist, ")
        append("never promise features as already done unless listed above.")
        if (context.isNotEmpty()) {
            append(" Recent room messages you can refer to: ")
            append(context.takeLast(6).joinToString(" | "))
        }
    }

    /**
     * Asks the API which models this key may call. Survives model renames, which happen often
     * enough that hard-coding a name would break the assistant every few months.
     */
    private fun discoverModels(key: String): List<String> {
        val conn = runCatching {
            (URL("$ENDPOINT/models?pageSize=200").openConnection() as HttpURLConnection)
        }.getOrNull() ?: return emptyList()
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("x-goog-api-key", key)
            if (conn.responseCode !in 200..299) return emptyList()
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val models = JSONObject(raw).optJSONArray("models") ?: return emptyList()
            val usable = ArrayList<String>()
            for (i in 0 until models.length()) {
                val model = models.optJSONObject(i) ?: continue
                val name = model.optString("name").removePrefix("models/")
                if (name.isBlank()) continue
                val methods = model.optJSONArray("supportedGenerationMethods") ?: continue
                val generates = (0 until methods.length())
                    .any { methods.optString(it) == "generateContent" }
                // Image, speech, audio and embedding models also advertise generateContent, but
                // they cannot hold a conversation.
                val conversational = NON_CHAT_MARKERS.none { name.contains(it) }
                if (generates && conversational) usable.add(name)
            }
            // The API lists models alphabetically, so "gemini-2.5-flash" comes before every 3.x
            // name even though 2.5 is retired and 404s. Rank by version number instead: newest
            // flash first, previews and lite variants after the stable ones.
            usable.sortedWith(
                compareByDescending<String> { if (it.contains("flash")) 1 else 0 }
                    .thenByDescending { versionOf(it) }
                    .thenByDescending { if (it.contains("preview")) 0 else 1 }
                    .thenByDescending { if (it.contains("lite")) 0 else 1 }
            )
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    /** "gemini-3.8-flash" -> 3.8, "gemini-flash-latest" -> 0.0. */
    private fun versionOf(name: String): Double {
        val match = Regex("gemini-(\\d+(?:\\.\\d+)?)").find(name) ?: return 0.0
        return match.groupValues[1].toDoubleOrNull() ?: 0.0
    }

    /** One POST to one model. Returns the HTTP status and, on success, the answer text. */
    private fun postGenerate(
        model: String,
        viaHeader: Boolean,
        key: String,
        body: String
    ): Pair<Int, String?> {
        val url = if (viaHeader) {
            "$ENDPOINT/models/$model:generateContent"
        } else {
            "$ENDPOINT/models/$model:generateContent?key=$key"
        }
        val conn = runCatching {
            (URL(url).openConnection() as HttpURLConnection)
        }.getOrNull() ?: return 0 to null
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 12_000
            conn.readTimeout = 25_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (viaHeader) conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                code to extractText(raw)
            } else {
                code to null
            }
        } catch (_: Exception) {
            0 to null
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun askGemini(key: String, prompt: String, context: List<String>): String? =
        withContext(Dispatchers.IO) {
            // Soft client-side throttle; free API keys have their own quota.
            val now = System.currentTimeMillis()
            if (now - lastCallMs < 2_500L) return@withContext null
            lastCallMs = now

            val body = JSONObject().apply {
                put("systemInstruction", JSONObject().put(
                    "parts", JSONArray().put(JSONObject().put("text", systemPrompt(context)))
                ))
                put("contents", JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }
                ))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 900)
                    // 3.x models think before answering and thinking tokens share the output
                    // budget, so keep the level low: a chat answer needs no deep reasoning, and a
                    // high level can spend the whole budget before a visible word is written.
                    put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                })
            }.toString()

            val discovered = modelCache ?: discoverModels(key).also {
                if (it.isNotEmpty()) modelCache = it
            }
            val ranked = (discovered.ifEmpty { FALLBACK_MODELS }).take(6)
            // A model that just answered goes first: Google often leaves the newest name briefly
            // overloaded while older ones serve fine.
            val models = preferredModel?.let { pinned ->
                listOf(pinned) + ranked.filter { it != pinned }
            } ?: ranked

            // Two rounds: Google answers 503 when a model is briefly overloaded, and that clears up
            // in seconds, so one quick retry is worth more than dropping to the offline answers.
            repeat(2) { round ->
                if (round > 0) delay(1_200L)
                for (model in models) {
                    var modelGone = false
                    for (viaHeader in listOf(true, false)) {
                        // Older AIza keys and newer AQ. keys both accept the header; the query form
                        // is the second chance if a key is rejected that way.
                        val (code, text) = postGenerate(model, viaHeader, key, body)
                        if (code in 200..299 && text != null) {
                            lastError = null
                            preferredModel = model
                            lastModel = model
                            return@withContext text
                        }
                        if (code in 200..299 && text == null) {
                            lastError = "AI ne khali jawab diya"
                            continue
                        }
                        lastError = when {
                            code == 0 -> "AI se connect nahi hua"
                            code == 404 -> "AI model nahi mila"
                            code == 429 -> "AI quota khatam, thodi der baad try karo"
                            code in 400..403 -> "AI key reject ho gayi — nayi key banao"
                            code >= 500 -> "AI server busy hai ($code)"
                            else -> "AI server error ($code)"
                        }
                        // A retired model name 404s and the next name may work; a key problem fails
                        // the same way everywhere, so stop early instead of burning the quota.
                        if (code == 404) {
                            modelGone = true
                            break
                        }
                        if (code in 400..403) return@withContext null
                    }
                    if (modelGone) continue
                }
            }
            null
        }

    private fun extractText(raw: String): String? = runCatching {
        val candidates = JSONObject(raw).optJSONArray("candidates") ?: return@runCatching null
        val parts = candidates.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts") ?: return@runCatching null
        val builder = StringBuilder()
        for (i in 0 until parts.length()) {
            val piece = parts.optJSONObject(i)?.optString("text").orEmpty()
            if (piece.isNotBlank()) builder.append(piece)
        }
        builder.toString().trim().ifBlank { null }
    }.getOrNull()

    /* ── built-in knowledge base ────────────────────────────────────────── */

    private val knowledge: List<Pair<List<String>, String>> = listOf(
        listOf("voice chat", "vc", "call", "mic live", "voice channel") to
            "Live voice chat (VC) abhi nahi hai — uske liye media server chahiye. Filhaal Chats me " +
            "voice notes bhej sakte ho, aur Discord server pe voice channel join kar sakte ho: " +
            "https://discord.gg/pERGMvSFw",
        listOf("voice note", "voice message", "record", "mic") to
            "Kisi bhi chat me 🎤 dabao → bolo → ⏹ dabao. Voice note turant bhej diya jayega, " +
            "doosra player ▶️ se sun lega.",
        listOf("dm", "direct message", "personal chat", "private") to
            "Chats → Direct messages me apne friend pe tap karo. Wo chat sirf tum dono ke beech hai.",
        listOf("group", "party chat", "group chat") to
            "Chats → Groups me apni party ka room hai. Party ke saare members ek saath baat karte hain.",
        listOf("public", "global", "everyone") to
            "Chats → Public ek hi room hai jisme har Apex player baat kar sakta hai. " +
            "Wahan @apex likho to main jawab deta hoon.",
        listOf("chat", "message") to
            "Chats section me teen tab hain: Direct messages, Groups aur Public. Text ke saath " +
            "voice notes bhi jaate hain, har message ke corner me Discord jaisi tail hoti hai.",
        listOf("badge", "ceo", "owner", "crown") to
            "Badges staff ke naam ke saath dikhte hain: 👑 CEO, 👑 OWNER, 🛠 DEVELOPER. " +
            "Sirf asli staff names pe lagte hain.",
        listOf("diwali", "festival", "diya") to
            "Diwali apna alag section hai — rail me Diwali icon pe tap karke special screen kholo.",
        listOf("theme", "crimson", "noir", "colour", "color", "dark") to
            "Settings → launcher color theme se accent badlo; Crimson Noir default home theme hai.",
        listOf("discord", "rich presence", "rpc", "card") to
            "Discord card Settings me hai — ek hi \"Card image\" field hai, aur card in-game state " +
            "dikhata hai. Discord app me Clear cache karke dobara check karo.",
        listOf("install", "apk", "update", "download") to
            "Naya APK hamesha yahan milta hai: " +
            "https://github.com/CoderzCruz/Apex-launcher/releases/download/debug-latest/ApexLauncher1.0.apk " +
            "— purana uninstall karna zaroori nahi, seedha install kar do.",
        listOf("crash", "band ho", "not working", "error", "lag") to
            "Logs share karo: Settings → Share. Usse pata chalta hai app kahan atak rahi hai.",
        listOf("account", "login", "microsoft", "offline") to
            "Accounts section se Microsoft login ya offline account dono add kar sakte ho.",
        listOf("ai", "gemini", "key", "smart", "brain") to
            "Main real AI se chalun iske liye Settings → Apex Service (AI) me apni free Google AI " +
            "key daalo (aistudio.google.com se bina card ke milti hai). Key ke bina bhi main " +
            "launcher ke sawaalon ke jawab de sakta hoon."
    )

    private fun localAnswer(question: String): String {
        val q = question.lowercase()
        var best: Pair<Int, String>? = null
        knowledge.forEach { (keys, answer) ->
            val hits = keys.count { q.contains(it) }
            if (hits > 0 && (best == null || hits > best!!.first)) best = hits to answer
        }
        best?.let { return it.second }
        val greet = listOf("hi", "hello", "hey", "namaste", "hola")
        if (greet.any { q == it || q.startsWith("$it ") }) {
            return "Namaste! Main Apex Service hoon. Apex Launcher, chat, themes, voice notes, " +
                "badges — jo poochna hai poocho."
        }
        if (q.endsWith("?")) {
            return "Ye sawaal mere built-in dimaag se bahar hai 😅 Real AI ke liye Settings → " +
                "Apex Service (AI) me free Google AI key daalo, phir main har cheez ka jawab dunga."
        }
        return "Main Apex Launcher ka assistant hoon — chat, voice notes, themes, Discord card, " +
            "install, badges, Diwali, VC ke baare me poocho. (Real AI chahiye to Settings → " +
            "Apex Service (AI) me key daalo.)"
    }
}
