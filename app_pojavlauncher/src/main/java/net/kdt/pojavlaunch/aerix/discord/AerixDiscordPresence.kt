/*
 * Aerix Launcher UI layer for PojavLauncher.
 * Discord Rich Presence, published from the phone to the Discord app installed on it.
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

package net.kdt.pojavlaunch.aerix.discord

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.discord.socialsdk.rpc.IDiscordRpcCallback
import com.discord.socialsdk.rpc.IDiscordRpcConnection
import com.discord.socialsdk.rpc.IDiscordRpcService
import net.kdt.pojavlaunch.aerix.bridge.AerixPreferences
import net.kdt.pojavlaunch.aerix.social.SocialBackend
import net.kdt.pojavlaunch.aerix.social.SocialNotificationReceiver
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Publishes rich, clickable Discord Rich Presence cards from Aerix Launcher to the Discord app:
 *
 * 1. **Apex Launcher Card Mode** (always used in launcher, and optionally in-game):
 *    - Published under `Apex Launcher` (`1566402542487736410`) with crisp Apex / modpack cover
 *      art (`assets.large_image`), clickable cover & badge URLs (`large_url`, `small_url`),
 *      clickable headline & status links (`details_url`, `state_url`) opening the official Aerix
 *      Launcher website and Discord community, action buttons (`About Aerix` + `Join Discord`),
 *      and live Aerix Social party size `(current of max)`.
 * 2. **Official Minecraft Game Profile Mode** (Hybrid in-game option, enabled by default):
 *    - While in-game, switches RPC connection to Minecraft's verified Discord catalog ID
 *      (`1402418491272986635`, with automatic cascade fallback to `356875570916753438` and then
 *      the Minecraft catalog ID if a mobile client rejects external catalog IDs) so
 *      tapping the card on Discord opens Minecraft's native full-screen Game Profile sheet while
 *      displaying `"via Apex Launcher"` and the Apex badge.
 * 3. **Exit cleanup**:
 *    - Send an explicit clear before disconnecting, without a delayed alarm that wakes the app.
 *      Android may freeze or kill the process before a one-minute timer can fire.
 */
object AerixDiscordPresence {

    private const val TAG = "AerixDiscordPresence"
    private const val ACTION = "com.discord.socialsdk.rpc.IDiscordRpcService"
    private const val RPC_VERSION = "1"
    private const val HANDLER_PID = 1
    private const val CLOSE_INVALID_CLIENT_ID = 4000
    private const val MAX_ASSET_URL_LEN = 256
    private const val WATCHDOG_INTERVAL_MS = 5_000L

    /**
     * The card belongs to this build's owner. Discord reports the signed-in account in the READY
     * frame; if it is a different account the card is never published, so anyone installing this
     * APK still sees nothing on their own profile.
     */
    const val OWNER_DISCORD_ID = "1385646460774907914"

    /** Official clickable destinations & artwork for Aerix Rich Presence cards. */
    const val AERIX_WEBSITE_URL = "https://entitybrian69-bit.github.io/Aerix-launcher/"
    const val AERIX_DISCORD_URL = "https://discord.gg/RS7q9KaCm6"
    /**
     * Rich Presence asset key uploaded to the Apex Launcher Discord application. Mobile clients only
     * resolve application assets, so the card uses the key rather than a bare image URL.
     */
    const val APEX_LOGO_ASSET = "apex_logo"

    /**
     * One setting covers both cases: a plain value is the art asset name uploaded in the Discord
     * Developer Portal, while an http(s) value is used as the image URL itself.
     */
    private fun assetSetting(): String = AerixPreferences.discordAssetName.trim()

    private fun isImageUrl(value: String): Boolean =
        value.length in 8..MAX_ASSET_URL_LEN &&
            (value.startsWith("https://", true) || value.startsWith("http://", true))

    /** The asset name to publish: whatever is set in settings, else the shipped key. */
    fun logoAssetKey(): String {
        val value = assetSetting()
        return if (value.isEmpty() || isImageUrl(value)) APEX_LOGO_ASSET else value
    }

    /** What goes into `assets.large_image`: the configured name or, when a link is given, the URL. */
    fun logoImageValue(): String {
        val value = assetSetting()
        return if (isImageUrl(value)) value else logoAssetKey()
    }

    /** The fork's own destinations. Blank means the matching button/link is skipped entirely. */
    const val APEX_WEBSITE_URL = "https://github.com/CoderzCruz/Apex-launcher"
    /** Apex Launcher community invite, so every install has a working Join button by default. */
    const val APEX_DISCORD_URL = "https://discord.gg/pERGMvSFw"

    /** Accepts "discord.gg/apex" as well as "https://discord.gg/apex"; drops anything unusable. */
    private fun normalizeLink(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty() || value.contains(' ')) return ""
        if (value.startsWith("http://", true) || value.startsWith("https://", true)) return value
        return if (value.contains('.')) "https://$value" else ""
    }

    /** Community links typed in settings; blank until the fork has destinations of its own. */
    fun websiteUrl(): String =
        normalizeLink(AerixPreferences.discordWebsiteUrl).ifBlank { APEX_WEBSITE_URL }

    fun inviteUrl(): String =
        normalizeLink(AerixPreferences.discordInviteUrl).ifBlank { APEX_DISCORD_URL }

    /** Verified Discord catalog IDs for Minecraft's native Game Profile sheet. */
    const val MINECRAFT_PRIMARY_APP_ID = 1402418491272986635L
    const val MINECRAFT_LEGACY_APP_ID = 356875570916753438L

    private const val CLEAR_FLUSH_DELAY_MS = 400L

    /**
     * How long the card stays up after the launcher goes to the background. Opening Discord to look
     * at your own profile backgrounds the launcher, so an immediate clear made the card invisible.
     */
    private const val BACKGROUND_GRACE_MS = 120_000L
    private const val CLEAR_BIND_TIMEOUT_MS = 5_500L
    private const val PENDING_INTENT_REQUEST_CLEAR = 94120
    private const val STATE_FILE_NAME = "aerix_discord_presence.json"

    const val ACTION_DISCORD_GAME_STARTED = "net.kdt.pojavlaunch.aerix.ACTION_DISCORD_GAME_STARTED"
    const val ACTION_DISCORD_SCHEDULE_CLEAR = "net.kdt.pojavlaunch.aerix.ACTION_DISCORD_SCHEDULE_CLEAR"
    const val ACTION_DISCORD_CLEAR_NOW = "net.kdt.pojavlaunch.aerix.ACTION_DISCORD_CLEAR_NOW"

    const val EXTRA_INSTANCE_NAME = "extra_instance_name"
    const val EXTRA_MC_VERSION = "extra_mc_version"
    const val EXTRA_MODPACK_ICON_URL = "extra_modpack_icon_url"
    const val EXTRA_STARTED_AT = "extra_started_at"
    const val EXTRA_DELAY_MS = "extra_delay_ms"

    /** Official app first, then the main client mods that keep the same service. */
    private val DISCORD_PACKAGES = listOf("com.discord", "cocobo1.pupu.app")

    private val main = Handler(Looper.getMainLooper())

    private var appContext: Context? = null
    private var service: IDiscordRpcService? = null
    private var connection: IDiscordRpcConnection? = null
    @Volatile private var connectedAppId: Long = 0L
    @Volatile private var bound = false
    @Volatile private var ready = false

    /** Last reason binding failed, shown in Settings so a failure is not a bare "not reachable". */
    @Volatile private var bindingNote: String = ""

    /** What the settings card shows: whether Discord is bound, connected and on which app id. */
    /** The last activity error Discord reported, for the settings status card. */
    fun lastError(): String = lastRpcError

    /** True when the signed-in Discord account is not this build's owner. */
    fun isForeignAccount(): Boolean = foreignAccount

    /** The owner id the card is locked to: the setting, else the built-in owner. */
    private fun ownerId(): String =
        AerixPreferences.discordOwnerId.trim().ifBlank { OWNER_DISCORD_ID }

    /** Pulls `data.user.id` out of a READY frame, so the card can stay with its owner. */
    private fun readAccountFromFrame(frame: String) {
        val id = runCatching {
            val user = JSONObject(frame).optJSONObject("data")?.optJSONObject("user")
            user?.optString("id", "")?.filter { it.isDigit() }
        }.getOrNull().orEmpty()
        if (id.isBlank()) return
        reportedUserId = id
        val owner = ownerId()
        foreignAccount = owner.isNotBlank() && id != owner
        if (foreignAccount) {
            Log.i(TAG, "Signed-in Discord account does not own this card; staying silent")
        }
    }

    /** What the card currently says, e.g. "In the launcher · In settings". */
    fun publishedSummary(): String = publishedSummary

    /**
     * Re-aligns the in-memory card with the shared state file. Called by the watchdog so the card
     * follows the game even when the start broadcast is missed or arrives late.
     */
    fun syncFromDisk(context: Context?) {
        val app = context?.applicationContext ?: appContext ?: return
        appContext = app
        AerixPreferences.init(app)
        if (!AerixPreferences.discordPresenceEnabled) return
        val disk = readDiskState(app) ?: return
        when (disk.owner) {
            "game" -> {
                if (!inGameMode || details != disk.details || state != disk.state) {
                    inGameMode = true
                    activeModpackIconUrl = disk.modpackIconUrl
                    details = disk.details
                    state = disk.state
                    if (disk.startedAt > 0L) startedAt = disk.startedAt
                    main.post { switchConnectionIfNeededOrSend() }
                }
            }
            "launcher" -> {
                if (inGameMode) {
                    inGameMode = false
                    activeModpackIconUrl = null
                    details = disk.details
                    state = disk.state
                    if (disk.startedAt > 0L) startedAt = disk.startedAt
                    main.post { switchConnectionIfNeededOrSend() }
                }
            }
        }
    }

    private fun startWatchdog(app: Context) {
        if (watchdog != null) return
        val runnable = object : Runnable {
            private var tick = 0
            override fun run() {
                runCatching { syncFromDisk(appContext ?: app) }
                // Every third tick also refreshes the card, rebuilding a dropped connection and
                // keeping the elapsed timer alive for whoever is looking at the profile.
                if (++tick % 3 == 0) {
                    runCatching {
                        if (AerixPreferences.discordPresenceEnabled) switchConnectionIfNeededOrSend()
                    }
                }
                main.postDelayed(this, WATCHDOG_INTERVAL_MS)
            }
        }
        watchdog = runnable
        main.postDelayed(runnable, WATCHDOG_INTERVAL_MS)
    }

    private fun stopWatchdog() {
        watchdog?.let { main.removeCallbacks(it) }
        watchdog = null
    }

    fun connectionStatus(): String {
        if (!AerixPreferences.discordPresenceEnabled) return "Presence off"
        val appId = DiscordAppId.effective()
        if (appId.isBlank()) return "No application id"
        return when {
            foreignAccount -> "Hidden: this Discord account is not the owner"
            !bound -> bindingNote.ifBlank { "Discord app not reachable" }
            !ready -> "Connecting to Discord…"
            connectedAppId > 0L -> "Connected · app $connectedAppId"
            else -> "Connecting…"
        }
    }

    /** True when the installed Discord client accepted the connection. */
    fun isConnected(): Boolean = ready && connection != null

    /** Catalog IDs rejected by the installed Discord app during this session (e.g. close 4000). */
    private val rejectedAppIds = HashSet<Long>()

    /**
     * Automatically set to false if the installed Discord Android client returns an RPC ERROR frame
     * for `buttons` / `party`, preserving `assets` and clickable `details_url` / `state_url`.
     */
    @Volatile
    private var supportsExtendedFields = true

    /**
     * Discord answers an unknown or rejected activity field with an ERROR frame. Instead of throwing
     * every optional field away at once, each error drops just one group and the card is resent, so a
     * client that dislikes `details_url` still gets its buttons.
     */
    private var extendedDropLevel = 0
    @Volatile private var lastRpcError: String = ""

    /** Previous app ID whose card must be cleared before switching to a different app ID. */
    @Volatile
    private var staleAppIdToClearFirst: Long = 0L

    /** Pending clear scheduled when the launcher went to the background. */
    private var exitClearRunnable: Runnable? = null

    /** True when binding solely to clear the active card and disconnect. */
    @Volatile
    private var clearOnlyMode = false
    @Volatile
    private var pendingClearAppId: Long = 0L
    private var clearOnlyRequiresExit = false
    private val pendingClearCallbacks = mutableListOf<() -> Unit>()
    private val clearGeneration = java.util.concurrent.atomic.AtomicLong()

    /** Set right before LauncherActivity kills itself to hand off to GameActivity. */
    @Volatile
    private var handoffToGameInProgress = false

    /** Cached in `:game` process so returning to GameActivity from background restores game card. */
    private var gameInstanceCache: String? = null
    private var gameVersionCache: String? = null
    private var gameModpackIconCache: String? = null
    private var gameStartedAtCache: Long = 0L

    /** Current state published to Discord. */
    @Volatile private var inGameMode: Boolean = false

    /** What the last published card said, for the settings status card. */
    @Volatile private var publishedSummary: String = ""

    /** The Discord account reported by the client, empty until the READY frame arrives. */
    @Volatile private var reportedUserId: String = ""

    /** True when the signed-in Discord account is not the one this card belongs to. */
    @Volatile private var foreignAccount: Boolean = false

    /**
     * `:game` owns the screen while a game runs, and the launcher process can be reclaimed while it
     * is in the background. A watchdog keeps the card in step with the shared state file, so a
     * missed or late handoff is corrected within a few seconds instead of leaving a stale card.
     */
    private var watchdog: Runnable? = null
    private var activeModpackIconUrl: String? = null
    private var details: String = InLauncher.DETAILS
    private var state: String = InLauncher.HOME
    private var startedAt: Long = 0L

    private data class PresenceDiskState(
        val owner: String, // "launcher" | "game" | "none"
        val exitedAtElapsedMs: Long,
        val details: String,
        val state: String,
        val startedAt: Long,
        val activeAppId: Long = 0L,
        val modpackIconUrl: String? = null
    )

    private val clearBindTimeoutRunnable = Runnable {
        if (clearOnlyMode) {
            clearOnlyMode = false
            pendingClearAppId = 0L
            detach()
            finishPendingClearCallbacks()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val rpc = IDiscordRpcService.Stub.asInterface(binder)
            service = rpc
            main.post {
                val initialId = when {
                    clearOnlyMode -> resolveClearTargetAppId()
                    staleAppIdToClearFirst > 0L -> staleAppIdToClearFirst
                    else -> resolveDesiredAppId()
                }
                if (initialId == null || initialId <= 0L) {
                    Log.i(TAG, "No Discord application id set; presence stays off")
                    clearOnlyMode = false
                    pendingClearAppId = 0L
                    staleAppIdToClearFirst = 0L
                    unbind()
                    finishPendingClearCallbacks()
                    return@post
                }
                connectServiceWithId(initialId)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connection = null
            connectedAppId = 0L
            ready = false
            if (clearOnlyMode) {
                clearOnlyMode = false
                pendingClearAppId = 0L
                finishPendingClearCallbacks()
            }
        }
    }

    private val callback = object : IDiscordRpcCallback.Stub() {
        override fun onFrame(frame: String?) {
            if (frame == null) return
            if (frame.contains("\"READY\"")) {
                readAccountFromFrame(frame)
                main.post {
                    ready = true
                    if (foreignAccount) {
                        // Someone else's Discord account: publish nothing at all, and make sure a
                        // card left over from an earlier account does not stay on the profile.
                        sendClearFrameImmediate()
                        return@post
                    }
                    if (staleAppIdToClearFirst > 0L && connectedAppId == staleAppIdToClearFirst && !clearOnlyMode) {
                        // Flush clear on the previous app ID before connecting the new app ID!
                        sendClearFrameImmediate()
                        val generation = clearGeneration.get()
                        main.postDelayed({
                            if (generation != clearGeneration.get()) return@postDelayed
                            staleAppIdToClearFirst = 0L
                            ready = false
                            runCatching { connection?.disconnect() }
                            connection = null
                            connectedAppId = 0L
                            val nextId = resolveDesiredAppId()
                            if (nextId != null && service != null) {
                                connectServiceWithId(nextId)
                            }
                        }, CLEAR_FLUSH_DELAY_MS)
                        return@post
                    }

                    if (clearOnlyMode) {
                        main.removeCallbacks(clearBindTimeoutRunnable)
                        if (clearOnlyRequiresExit && appContext?.let { readDiskState(it)?.owner } != "none") {
                            clearOnlyMode = false
                            pendingClearAppId = 0L
                            finishPendingClearCallbacks()
                            return@post
                        }
                        sendClearFrameImmediate()
                        val generation = clearGeneration.get()
                        main.postDelayed({
                            if (generation != clearGeneration.get()) return@postDelayed
                            if (clearOnlyRequiresExit && appContext?.let { readDiskState(it)?.owner } != "none") {
                                clearOnlyMode = false
                                finishPendingClearCallbacks()
                                return@postDelayed
                            }
                            clearOnlyMode = false
                            pendingClearAppId = 0L
                            appContext?.let { app ->
                                readDiskState(app)?.let { d ->
                                    writeDiskState(app, d.copy(activeAppId = 0L))
                                }
                            }
                            detach()
                            finishPendingClearCallbacks()
                        }, CLEAR_FLUSH_DELAY_MS)
                    } else {
                        appContext?.let { app ->
                            val d = readDiskState(app)
                            if (d != null && d.owner != "none" && d.activeAppId != connectedAppId) {
                                writeDiskState(app, d.copy(activeAppId = connectedAppId))
                            }
                        }
                        sendActivity()
                    }
                }
            } else if (frame.contains("\"ERROR\"")) {
                Log.w(TAG, "Discord RPC error frame: $frame")
                lastRpcError = frame.take(200)
                if (!clearOnlyMode && supportsExtendedFields) {
                    if (extendedDropLevel < 3) extendedDropLevel++ else supportsExtendedFields = false
                    main.post { sendActivity() }
                }
            }
        }

        override fun onClose(code: Int, message: String?) {
            main.post {
                val closedId = connectedAppId
                ready = false
                connection = null
                connectedAppId = 0L

                if (staleAppIdToClearFirst > 0L && closedId == staleAppIdToClearFirst && !clearOnlyMode) {
                    staleAppIdToClearFirst = 0L
                    val nextId = resolveDesiredAppId()
                    if (nextId != null && service != null) {
                        connectServiceWithId(nextId)
                        return@post
                    }
                }

                if (clearOnlyMode) {
                    clearOnlyMode = false
                    pendingClearAppId = 0L
                    finishPendingClearCallbacks()
                    return@post
                }

                if (isMinecraftCatalogId(closedId) &&
                    (code == CLOSE_INVALID_CLIENT_ID ||
                        message?.contains("client", ignoreCase = true) == true ||
                        code != 1000)
                ) {
                    Log.i(TAG, "Discord closed catalog id $closedId (code=$code: $message); falling back")
                    rejectedAppIds.add(closedId)
                    val fallbackId = resolveDesiredAppId()
                    if (fallbackId != null && service != null) {
                        connectServiceWithId(fallbackId)
                    } else {
                        attach(appContext)
                    }
                }
            }
        }
    }

    private fun isMinecraftCatalogId(id: Long): Boolean =
        id == MINECRAFT_PRIMARY_APP_ID || id == MINECRAFT_LEGACY_APP_ID

    private fun resolveDesiredAppId(): Long? {
        val baseId = DiscordAppId.asLong(appContext) ?: return null
        if (inGameMode && AerixPreferences.discordGameProfileMode && !DiscordAppId.hasCustomOverride()) {
            val candidates = listOf(MINECRAFT_PRIMARY_APP_ID, MINECRAFT_LEGACY_APP_ID, baseId)
            return candidates.firstOrNull { it !in rejectedAppIds } ?: baseId
        }
        return baseId
    }

    private fun resolveClearTargetAppId(): Long? {
        if (pendingClearAppId > 0L) return pendingClearAppId
        if (connectedAppId > 0L) return connectedAppId
        val diskId = appContext?.let { readDiskState(it)?.activeAppId } ?: 0L
        if (diskId > 0L) return diskId
        return DiscordAppId.asLong(appContext)
    }

    private fun connectServiceWithId(id: Long) {
        val rpc = service ?: return
        ready = false
        val conn = runCatching { rpc.connect(id, RPC_VERSION, callback) }.getOrNull()
        if (conn != null) {
            connection = conn
            connectedAppId = id
            return
        }
        Log.w(TAG, "Discord refused RPC connection for id $id")
        if (staleAppIdToClearFirst == id) {
            staleAppIdToClearFirst = 0L
            val next = resolveDesiredAppId()
            if (next != null && next != id) {
                connectServiceWithId(next)
            }
            return
        }
        if (!clearOnlyMode && isMinecraftCatalogId(id)) {
            rejectedAppIds.add(id)
            val fallback = resolveDesiredAppId()
            if (fallback != null && fallback != id) {
                connectServiceWithId(fallback)
                return
            }
        }
        if (clearOnlyMode) {
            clearOnlyMode = false
            pendingClearAppId = 0L
            unbind()
            finishPendingClearCallbacks()
        }
    }

    private fun switchConnectionIfNeededOrSend() {
        if (!AerixPreferences.discordPresenceEnabled) return
        val desiredId = resolveDesiredAppId() ?: return
        val ctx = appContext ?: return
        if (readDiskState(ctx)?.owner == "none") return

        if (!bound || service == null) {
            attach(ctx)
            return
        }

        if (connection != null && connectedAppId == desiredId && staleAppIdToClearFirst == 0L) {
            if (ready) sendActivity()
            return
        }

        if (connection != null && connectedAppId != 0L && connectedAppId != desiredId) {
            // Clear the currently active app ID card before switching so Discord never leaves it stuck!
            if (ready) {
                sendClearFrameImmediate()
                val targetAfterClear = desiredId
                val generation = clearGeneration.get()
                main.postDelayed({
                    if (generation != clearGeneration.get()) return@postDelayed
                    ready = false
                    runCatching { connection?.disconnect() }
                    connection = null
                    connectedAppId = 0L
                    staleAppIdToClearFirst = 0L
                    if (service != null) {
                        connectServiceWithId(targetAfterClear)
                    } else {
                        attach(ctx)
                    }
                }, CLEAR_FLUSH_DELAY_MS)
            } else {
                ready = false
                runCatching { connection?.disconnect() }
                connection = null
                connectedAppId = 0L
                connectServiceWithId(desiredId)
            }
            return
        }

        if (connection == null) {
            val target = if (staleAppIdToClearFirst > 0L && staleAppIdToClearFirst != desiredId) {
                staleAppIdToClearFirst
            } else {
                staleAppIdToClearFirst = 0L
                desiredId
            }
            connectServiceWithId(target)
        }
    }

    /* ── cross-process lifecycle hooks ─────────────────────────────────────── */

    /** Captures application Context early in every process (`:launcher` and `:game`). */
    @JvmStatic
    fun captureApplicationContext(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext
        }
    }

    /** Called from LauncherActivity.onStart() in `:launcher`. */
    @JvmStatic
    fun onLauncherStarted(context: Context?) {
        val app = context?.applicationContext ?: return
        appContext = app
        handoffToGameInProgress = false
        AerixPreferences.init(app)
        cancelPendingExitClear()
        invalidatePendingClear()
        cancelClearAlarm(app)

        val previousDisk = readDiskState(app)
        inGameMode = false
        activeModpackIconUrl = null
        if (details.startsWith("Playing Minecraft")) {
            details = InLauncher.DETAILS
            state = InLauncher.HOME
            startedAt = System.currentTimeMillis()
        } else if (startedAt == 0L) {
            startedAt = System.currentTimeMillis()
        }

        val desired = resolveDesiredAppId() ?: 0L
        if (previousDisk != null &&
            previousDisk.activeAppId > 0L &&
            desired > 0L &&
            previousDisk.activeAppId != desired &&
            connectedAppId != previousDisk.activeAppId
        ) {
            staleAppIdToClearFirst = previousDisk.activeAppId
        }

        writeDiskState(
            app,
            PresenceDiskState(
                owner = "launcher",
                exitedAtElapsedMs = 0L,
                details = details,
                state = state,
                startedAt = startedAt,
                activeAppId = if (connectedAppId > 0L) connectedAppId else desired,
                modpackIconUrl = null
            )
        )
        main.post { switchConnectionIfNeededOrSend() }
    }

    /** Called from LauncherActivity.onStop() in `:launcher`. */
    @JvmStatic
    fun onLauncherStopped(context: Context?) {
        val app = context?.applicationContext ?: appContext ?: return
        appContext = app
        if (handoffToGameInProgress) return
        val disk = readDiskState(app)
        // If `:game` already took ownership, do not schedule an exit from `:launcher`.
        if (disk?.owner == "game") return
        val nowElapsed = SystemClock.elapsedRealtime()
        writeDiskState(
            app,
            PresenceDiskState(
                owner = "none",
                exitedAtElapsedMs = nowElapsed,
                details = details,
                state = state,
                startedAt = startedAt,
                activeAppId = if (connectedAppId > 0L) connectedAppId else (disk?.activeAppId ?: 0L),
                modpackIconUrl = activeModpackIconUrl
            )
        )
        // Keep the card up for a grace period: the launcher is backgrounded every time Discord is
        // opened, and a card cleared on that first tap is a card nobody ever sees.
        scheduleExitClear(app)
    }

    private fun scheduleExitClear(app: Context) {
        cancelPendingExitClear()
        val runnable = Runnable {
            exitClearRunnable = null
            if (readDiskState(app)?.owner != "none") return@Runnable
            clearAndDetach(app, onlyIfExited = true)
        }
        exitClearRunnable = runnable
        main.postDelayed(runnable, BACKGROUND_GRACE_MS)
    }

    private fun cancelPendingExitClear() {
        exitClearRunnable?.let { main.removeCallbacks(it) }
        exitClearRunnable = null
    }

    /**
     * Called right before `:launcher` kills its own PID in ContextAwareDoneListener to launch
     * GameActivity in `:game`. If Hybrid Game Profile mode will switch to Minecraft's catalog ID,
     * sends an immediate clear on the Apex Launcher card before detaching.
     */
    @JvmStatic
    fun prepareHandoffToGame(context: Context?) {
        handoffToGameInProgress = true
        val app = context?.applicationContext ?: appContext
        if (app != null) {
            AerixPreferences.init(app)
            cancelClearAlarm(app)
            if (AerixPreferences.discordGameProfileMode && !DiscordAppId.hasCustomOverride() && ready) {
                sendClearFrameImmediate()
                readDiskState(app)?.let { d ->
                    writeDiskState(app, d.copy(activeAppId = 0L))
                }
            }
        }
        detach()
    }

    /** Called from GameActivity (`initLayout` and `onStart`) in `:game`. */
    @JvmStatic
    @JvmOverloads
    fun onGameStarted(
        context: Context?,
        instanceName: String?,
        minecraftVersion: String?,
        modpackIconUrl: String? = null
    ) {
        val app = context?.applicationContext ?: return
        appContext = app
        if (!instanceName.isNullOrBlank()) gameInstanceCache = instanceName
        if (!minecraftVersion.isNullOrBlank()) gameVersionCache = minecraftVersion
        if (!modpackIconUrl.isNullOrBlank()) gameModpackIconCache = modpackIconUrl
        if (gameStartedAtCache == 0L) gameStartedAtCache = System.currentTimeMillis()

        val effInstance = gameInstanceCache
        val effVer = gameVersionCache
        val effIcon = gameModpackIconCache
        val gameDetails = "Playing Minecraft${if (effVer.isNullOrBlank()) "" else " $effVer"}"
        val gameState = effInstance?.takeIf { it.isNotBlank() } ?: "In game"
        val prevActiveId = readDiskState(app)?.activeAppId ?: 0L

        writeDiskState(
            app,
            PresenceDiskState(
                owner = "game",
                exitedAtElapsedMs = 0L,
                details = gameDetails,
                state = gameState,
                startedAt = gameStartedAtCache,
                activeAppId = prevActiveId,
                modpackIconUrl = effIcon
            )
        )
        cancelClearAlarm(app)

        // Delegate RPC connection ownership to `:launcher` so `:game` Process.killProcess()
        // never orphans a dead Binder inside Discord.
        runCatching {
            val intent = Intent(app, SocialNotificationReceiver::class.java).apply {
                action = ACTION_DISCORD_GAME_STARTED
                putExtra(EXTRA_INSTANCE_NAME, effInstance)
                putExtra(EXTRA_MC_VERSION, effVer)
                putExtra(EXTRA_MODPACK_ICON_URL, effIcon)
                putExtra(EXTRA_STARTED_AT, gameStartedAtCache)
            }
            app.sendBroadcast(intent)
        }
    }

    /** Called from GameActivity.onStart() when resuming an already-running game session. */
    @JvmStatic
    fun onGameResumed(context: Context?) {
        if (gameInstanceCache != null || gameVersionCache != null || gameStartedAtCache != 0L) {
            onGameStarted(context, gameInstanceCache, gameVersionCache, gameModpackIconCache)
        }
        // Coming back from Discord: republish instead of waiting for the watchdog.
        if (context != null) {
            captureApplicationContext(context)
            playing(gameInstanceCache, gameVersionCache, gameModpackIconCache)
        }
    }

    /** Called from GameActivity.onStop() / onDestroy() in `:game`. */
    @JvmStatic
    fun onGameStopped(context: Context?) {
        val app = context?.applicationContext ?: appContext ?: resolveAnyContext() ?: return
        appContext = app
        val disk = readDiskState(app)
        if (disk?.owner == "launcher") return

        if (disk?.owner != "game") return
        writeDiskState(app, disk.copy(owner = "none", exitedAtElapsedMs = SystemClock.elapsedRealtime()))
        // Opening Discord mid-game stops the game activity: keep the card for the same grace window
        // as the launcher instead of wiping it the moment the player looks at their profile.
        scheduleExitClear(app)
    }

    /**
     * Called right before Process.killProcess() in Tools.fullyExit() or GameService exit paths,
     * attempting exit cleanup even when onStop/onDestroy are skipped.
     */
    @JvmStatic
    fun onProcessExiting(context: Context? = null) {
        val app = context?.applicationContext ?: appContext ?: resolveAnyContext() ?: return
        onGameStopped(app)
    }

    /**
     * Invoked when the user explicitly swipes/removes Aerix from Recent Apps so Discord Rich
     * Presence clears immediately and no delayed wakeup alarm brings the process back later.
     */
    @JvmStatic
    fun onAppSwipedFromRecents(context: Context?, onComplete: (() -> Unit)? = null) {
        val app = context?.applicationContext ?: appContext ?: resolveAnyContext() ?: return
        appContext = app
        cancelClearAlarm(app)
        // Preserve the ID that actually published the card for a cold Discord bind.
        val disk = readDiskState(app)
        if (disk != null) writeDiskState(app, disk.copy(owner = "none", exitedAtElapsedMs = SystemClock.elapsedRealtime()))
        clearAndDetach(app, onComplete)
    }

    /** Invoked in `:launcher` when SocialNotificationReceiver receives ACTION_DISCORD_GAME_STARTED. */
    @JvmStatic
    @JvmOverloads
    fun handleGameStartedBroadcast(
        context: Context,
        instanceName: String?,
        minecraftVersion: String?,
        gameStartedAt: Long,
        modpackIconUrl: String? = null
    ) {
        val app = context.applicationContext
        appContext = app
        AerixPreferences.init(app)
        val prevDisk = readDiskState(app) ?: return
        // A queued start broadcast must not revive a game that has already exited.
        if (prevDisk.owner != "game" || prevDisk.startedAt != gameStartedAt) return
        invalidatePendingClear()
        cancelClearAlarm(app)
        inGameMode = true
        activeModpackIconUrl = modpackIconUrl?.takeIf { it.isNotBlank() } ?: prevDisk?.modpackIconUrl
        details = "Playing Minecraft${if (minecraftVersion.isNullOrBlank()) "" else " $minecraftVersion"}"
        state = instanceName?.takeIf { it.isNotBlank() } ?: "In game"
        startedAt = if (gameStartedAt > 0L) gameStartedAt else System.currentTimeMillis()

        val desired = resolveDesiredAppId() ?: 0L
        val prevActive = prevDisk?.activeAppId ?: 0L
        if (prevActive > 0L && desired > 0L && prevActive != desired && connectedAppId != prevActive) {
            staleAppIdToClearFirst = prevActive
        }

        writeDiskState(
            app,
            PresenceDiskState(
                owner = "game",
                exitedAtElapsedMs = 0L,
                details = details,
                state = state,
                startedAt = startedAt,
                activeAppId = if (connectedAppId > 0L) connectedAppId else desired,
                modpackIconUrl = activeModpackIconUrl
            )
        )

        if (!AerixPreferences.discordPresenceEnabled) return
        main.post { switchConnectionIfNeededOrSend() }
    }

    /** The game has exited; ignore stale broadcasts if a new session has taken over. */
    fun handleScheduleClearBroadcast(context: Context, onComplete: () -> Unit) {
        val app = context.applicationContext
        appContext = app
        if (readDiskState(app)?.owner != "none") {
            onComplete()
            return
        }
        clearAndDetach(app, onComplete, onlyIfExited = true)
    }

    /** Handles an alarm from an older build without scheduling another background wake. */
    fun performScheduledClear(context: Context?, onComplete: (() -> Unit)?) {
        val app = context?.applicationContext ?: appContext
        if (app == null || readDiskState(app)?.owner != "none") {
            onComplete?.invoke()
            return
        }
        clearAndDetach(app, onComplete, onlyIfExited = true)
    }

    private fun invalidatePendingClear() {
        clearGeneration.incrementAndGet()
        clearOnlyMode = false
        pendingClearAppId = 0L
        main.removeCallbacks(clearBindTimeoutRunnable)
        main.post { finishPendingClearCallbacks() }
    }

    /* ── core RPC lifecycle ────────────────────────────────────────────────── */

    /** Bind once, when the launcher becomes visible. Safe to call repeatedly. */
    @JvmStatic
    fun attach(context: Context?) {
        if (context == null) return
        appContext = context.applicationContext
        if (!AerixPreferences.discordPresenceEnabled) return
        if (resolveDesiredAppId() == null) return
        clearOnlyMode = false
        pendingClearAppId = 0L

        if (bound && service != null) {
            main.post { switchConnectionIfNeededOrSend() }
            return
        }
        if (bound && service == null) {
            unbind()
        }

        val intent = resolveService() ?: run {
            Log.i(TAG, "Discord is not installed on this device")
            if (bindingNote.isBlank()) bindingNote = "Discord app not installed"
            return
        }
        bound = bindRpcService(appContext!!, intent)
        if (!bound) Log.w(TAG, "Could not bind to Discord's RPC service: $bindingNote")
        else startWatchdog(appContext!!)
    }

    /** Drop the connection immediately (internal helper; use [clearAndDetach] to clear profile first). */
    fun detach() {
        ready = false
        runCatching { connection?.disconnect() }
        connection = null
        connectedAppId = 0L
        service = null
        stopWatchdog()
        unbind()
    }

    /**
     * Explicitly sends a Social SDK clear `SET_ACTIVITY` frame on the active application ID,
     * flushes over Binder, and then disconnects and unbinds so Discord removes the Rich Presence
     * card cleanly instead of keeping an orphaned session alive for ~20 minutes.
     */
    fun clearAndDetach(context: Context? = appContext, onComplete: (() -> Unit)? = null, onlyIfExited: Boolean = false) {
        val app = context?.applicationContext ?: appContext
        if (app != null) {
            appContext = app
            cancelClearAlarm(app)
        }
        startedAt = 0L
        val generation = clearGeneration.incrementAndGet()

        main.post {
            if (generation != clearGeneration.get() ||
                (onlyIfExited && appContext?.let { readDiskState(it)?.owner } != "none")) {
                finishPendingClearCallbacks()
                onComplete?.invoke()
                return@post
            }
            if (onComplete != null) {
                pendingClearCallbacks.add(onComplete)
            }
            if (connection != null && ready) {
                sendClearFrameImmediate()
                main.postDelayed({
                    if (generation != clearGeneration.get()) return@postDelayed
                    if (onlyIfExited && appContext?.let { readDiskState(it)?.owner } != "none") {
                        finishPendingClearCallbacks()
                        return@postDelayed
                    }
                    clearOnlyMode = false
                    pendingClearAppId = 0L
                    appContext?.let { a ->
                        readDiskState(a)?.let { d ->
                            writeDiskState(a, d.copy(activeAppId = 0L))
                        }
                    }
                    detach()
                    finishPendingClearCallbacks()
                }, CLEAR_FLUSH_DELAY_MS)
                return@post
            }

            // Bind briefly to the app ID that published the card when no connection is ready.
            val ctx = appContext
            val targetId = resolveClearTargetAppId()
            if (ctx == null || targetId == null || targetId <= 0L) {
                detach()
                finishPendingClearCallbacks()
                return@post
            }
            val intent = resolveService()
            if (intent == null) {
                detach()
                finishPendingClearCallbacks()
                return@post
            }
            if (bound) unbind()
            clearOnlyMode = true
            clearOnlyRequiresExit = onlyIfExited
            pendingClearAppId = targetId
            bound = bindRpcService(ctx, intent)
            if (!bound) {
                clearOnlyMode = false
                pendingClearAppId = 0L
                finishPendingClearCallbacks()
            } else {
                main.removeCallbacks(clearBindTimeoutRunnable)
                main.postDelayed(clearBindTimeoutRunnable, CLEAR_BIND_TIMEOUT_MS)
            }
        }
    }

    /** Re-evaluates desired app ID & card fields immediately when Settings toggles change. */
    fun syncSettingsChanged(context: Context?) {
        val app = context?.applicationContext ?: appContext ?: return
        appContext = app
        AerixPreferences.init(app)
        rejectedAppIds.clear()
        supportsExtendedFields = true
        extendedDropLevel = 0
        lastRpcError = ""
        if (!AerixPreferences.discordPresenceEnabled) {
            clearAndDetach(app)
            return
        }
        main.post { switchConnectionIfNeededOrSend() }
    }

    fun refresh(context: Context?) {
        clearOnlyMode = false
        pendingClearAppId = 0L
        rejectedAppIds.clear()
        supportsExtendedFields = true
        extendedDropLevel = 0
        lastRpcError = ""
        syncSettingsChanged(context)
    }

    /* ── what the profile says ─────────────────────────────────────────────── */

    /** Launcher pages: the same two lines for every screen, with the section in the second one. */
    fun inLauncher(section: String?) {
        // Ignore late Compose updates after leaving, which would otherwise republish the card.
        val diskOwner = appContext?.let { readDiskState(it)?.owner }
        if (diskOwner != null && diskOwner != "launcher") return
        clearOnlyMode = false
        inGameMode = false
        activeModpackIconUrl = null
        details = InLauncher.DETAILS
        state = InLauncher.stateFor(section)
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
        appContext?.let { app ->
            val disk = readDiskState(app)
            if (disk == null || disk.owner == "launcher") {
                writeDiskState(
                    app,
                    PresenceDiskState(
                        owner = "launcher",
                        exitedAtElapsedMs = 0L,
                        details = details,
                        state = state,
                        startedAt = startedAt,
                        activeAppId = if (connectedAppId > 0L) connectedAppId else (resolveDesiredAppId() ?: 0L),
                        modpackIconUrl = null
                    )
                )
            }
        }
        main.post { switchConnectionIfNeededOrSend() }
    }

    /** A game is starting or running: the instance takes over the card. */
    @JvmStatic
    @JvmOverloads
    fun playing(instance: String?, minecraftVersion: String?, modpackIconUrl: String? = null) {
        clearOnlyMode = false
        inGameMode = true
        activeModpackIconUrl = modpackIconUrl?.takeIf { it.isNotBlank() }
        details = "Playing Minecraft${if (minecraftVersion.isNullOrBlank()) "" else " $minecraftVersion"}"
        state = instance?.takeIf { it.isNotBlank() } ?: "In game"
        // Keep the session's own start time so the elapsed timer never jumps back to zero.
        val diskStarted = appContext?.let { readDiskState(it) }
            ?.takeIf { it.owner == "game" }?.startedAt ?: 0L
        startedAt = if (diskStarted > 0L) diskStarted else System.currentTimeMillis()
        appContext?.let { app ->
            cancelClearAlarm(app)
            cancelPendingExitClear()
        }
        main.post { switchConnectionIfNeededOrSend() }
    }

    /** Remove the card, e.g. when the setting is switched off. */
    fun clear() {
        send(buildClearFrame())
    }

    /* ── the RPC frames ────────────────────────────────────────────────────── */

    /**
     * Discord's Social SDK clear format: `args` contains only `pid` with `"activity"` omitted
     * (`{"cmd":"SET_ACTIVITY","nonce":"...","args":{"pid":1}}`).
     */
    private fun buildClearFrame(): String =
        JSONObject()
            .put("cmd", "SET_ACTIVITY")
            .put("nonce", UUID.randomUUID().toString())
            .put("args", JSONObject().put("pid", HANDLER_PID))
            .toString()

    private fun sendClearFrameImmediate() {
        val target = connection ?: return
        val frame = buildClearFrame()
        runCatching { target.sendFrame(frame) }
    }

    private fun isValidHttpAssetUrl(url: String?): Boolean {
        if (url.isNullOrBlank() || url.length > MAX_ASSET_URL_LEN) return false
        return url.startsWith("https://", ignoreCase = true) ||
            url.startsWith("http://", ignoreCase = true)
    }

    private fun sendActivity() {
        if (foreignAccount) return
        if (clearOnlyMode || !ready || connection == null) return
        if (appContext?.let { readDiskState(it)?.owner } == "none") return
        val usingMinecraftProfile = isMinecraftCatalogId(connectedAppId)
        val clickable = AerixPreferences.discordClickableLinks

        val cardName = if (usingMinecraftProfile) "Minecraft" else "Apex Launcher"
        // status_display_type: 0 = Name ("Playing Minecraft"), 2 = Details ("In the launcher" / "Playing Minecraft 1.21")
        val statusDisplayType = if (usingMinecraftProfile) 0 else 2
        val effectiveDetails = if (usingMinecraftProfile && !details.contains("Apex", ignoreCase = true)) {
            "$details · via Apex Launcher"
        } else {
            details
        }

        val activity = JSONObject()
            .put("name", cardName)
            .put("type", 0) // PLAYING
            .put("status_display_type", statusDisplayType)
            .put("details", effectiveDetails)
            .put("state", state)
            .put("timestamps", JSONObject().put("start", startedAt.coerceAtLeast(1L)))

        // Live Aerix Social Party size `(current of max)` when in an active party!
        if (supportsExtendedFields && extendedDropLevel < 2) {
            val activeParty = runCatching {
                SocialBackend.parties.firstOrNull { !it.isExpired() }
            }.getOrNull()
            if (activeParty != null && activeParty.id.isNotBlank()) {
                val currentSize = activeParty.memberCount.coerceAtLeast(1)
                val maxSize = activeParty.maxMembers.coerceAtLeast(currentSize).coerceAtLeast(2)
                val sizeArr = JSONArray().put(currentSize).put(maxSize)
                activity.put(
                    "party",
                    JSONObject()
                        .put("id", "apex_${activeParty.id.take(48)}")
                        .put("size", sizeArr)
                )
            }
        }

        val assets = JSONObject()
        val validPackIcon = activeModpackIconUrl?.takeIf { clickable && inGameMode && isValidHttpAssetUrl(it) }
        if (validPackIcon != null) {
            assets.put("large_image", validPackIcon)
            assets.put("large_text", "$state · Apex Launcher")
            assets.put("small_image", logoImageValue())
            assets.put("small_text", "Apex Launcher")
        } else {
            assets.put("large_image", logoImageValue())
            assets.put("large_text", if (usingMinecraftProfile)
                "Apex Launcher — Minecraft Java Edition on Android" else "Apex Launcher")
            // Never repeat the same key as small_image: some clients drop a duplicate pair.
            val badge = logoAssetKey()
            if (usingMinecraftProfile && badge != logoImageValue()) {
                assets.put("small_image", badge)
                assets.put("small_text", "Powered by Apex Launcher")
            }
        }
        // Links are added only once the fork has destinations of its own. Empty strings keep the
        // card from pointing at the upstream project's website and Discord server.
        if (clickable) {
            val site = websiteUrl()
            val invite = inviteUrl()
            if (extendedDropLevel < 1) {
                // An image slot only ever carries an image; a plain web page would blank the cover.
                if (isImageUrl(site)) assets.put("large_url", site)
                if (isImageUrl(invite)) assets.put("small_url", invite)
                if (site.isNotBlank()) activity.put("details_url", site)
                if (invite.isNotBlank()) activity.put("state_url", invite)
            }
            if (supportsExtendedFields && extendedDropLevel < 3) {
                val buttons = JSONArray()
                if (site.isNotBlank()) {
                    buttons.put(JSONObject().put("label", "About Apex").put("url", site))
                }
                if (invite.isNotBlank()) {
                    buttons.put(JSONObject().put("label", "Join Apex Discord").put("url", invite))
                }
                if (buttons.length() > 0) activity.put("buttons", buttons)
            }
        }
        activity.put("assets", assets)

        val args = JSONObject()
            .put("pid", HANDLER_PID)
            .put("activity", activity)

        publishedSummary = "$details · $state"
        send(
            JSONObject()
                .put("cmd", "SET_ACTIVITY")
                .put("nonce", UUID.randomUUID().toString())
                .put("args", args)
                .toString()
        )
    }

    private fun send(frame: String) {
        val target = connection ?: return
        val generation = clearGeneration.get()
        main.post {
            if (generation == clearGeneration.get() && target === connection) {
                runCatching { target.sendFrame(frame) }
            }
        }
    }

    /* ── alarm & cross-process state helpers ───────────────────────────────── */

    private fun buildClearAlarmPendingIntent(app: Context, flagsExtra: Int = 0): PendingIntent? {
        val intent = Intent(app, SocialNotificationReceiver::class.java).apply {
            action = ACTION_DISCORD_CLEAR_NOW
        }
        val baseFlags = if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return runCatching {
            PendingIntent.getBroadcast(app, PENDING_INTENT_REQUEST_CLEAR, intent, baseFlags or flagsExtra)
        }.getOrNull()
    }

    private fun cancelClearAlarm(app: Context) {
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = buildClearAlarmPendingIntent(app, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun stateFile(app: Context): File = File(app.filesDir, STATE_FILE_NAME)

    private fun writeDiskState(app: Context, stateObj: PresenceDiskState) {
        runCatching {
            val json = JSONObject()
                .put("owner", stateObj.owner)
                .put("exitedAtElapsedMs", stateObj.exitedAtElapsedMs)
                .put("details", stateObj.details)
                .put("state", stateObj.state)
                .put("startedAt", stateObj.startedAt)
                .put("activeAppId", stateObj.activeAppId)
                .put("modpackIconUrl", stateObj.modpackIconUrl ?: "")
                .toString()
            stateFile(app).writeText(json)
        }
    }

    private fun readDiskState(app: Context): PresenceDiskState? = runCatching {
        val file = stateFile(app)
        if (!file.isFile) return null
        val json = JSONObject(file.readText())
        PresenceDiskState(
            owner = json.optString("owner", "none"),
            exitedAtElapsedMs = json.optLong("exitedAtElapsedMs", 0L),
            details = json.optString("details", InLauncher.DETAILS),
            state = json.optString("state", InLauncher.HOME),
            startedAt = json.optLong("startedAt", 0L),
            activeAppId = json.optLong("activeAppId", 0L),
            modpackIconUrl = json.optString("modpackIconUrl", "").takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    private fun resolveAnyContext(): Context? = appContext

    private fun finishPendingClearCallbacks() {
        val copy = ArrayList(pendingClearCallbacks)
        pendingClearCallbacks.clear()
        copy.forEach { cb -> runCatching { cb.invoke() } }
    }

    /* ── plumbing ──────────────────────────────────────────────────────────── */

    /**
     * Discord's service is queried first, then addressed by its own component. An implicit
     * package intent is only used when the platform will not report the service, because some
     * Discord builds export the service under an explicit component only.
     */
    private fun resolveService(): Intent? {
        val ctx = appContext ?: return null
        for (pkg in DISCORD_PACKAGES) {
            val probe = Intent(ACTION).setPackage(pkg)
            val services = runCatching {
                ctx.packageManager.queryIntentServices(probe, PackageManager.MATCH_ALL)
            }.getOrNull()
            val info = services?.firstOrNull { it.serviceInfo?.exported == true }
            if (info?.serviceInfo != null) {
                bindingNote = ""
                return Intent(ACTION).setComponent(
                    ComponentName(info.serviceInfo.packageName, info.serviceInfo.name)
                )
            }
            if (ctx.packageManager.resolveService(probe, 0) != null) {
                bindingNote = ""
                return probe
            }
        }
        bindingNote = "Discord RPC service not found"
        return null
    }

    /** Binds to Discord's service, recording the failure reason for the settings status line. */
    private fun bindRpcService(ctx: Context, intent: Intent): Boolean {
        return runCatching {
            ctx.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }.getOrElse { error ->
            bindingNote = "Bind failed: ${error.javaClass.simpleName}"
            Log.w(TAG, "bindService threw", error)
            false
        }.also { ok ->
            if (ok) bindingNote = "" else if (bindingNote.isBlank()) bindingNote = "Bind refused by Discord"
        }
    }

    private fun unbind() {
        if (!bound) return
        runCatching { appContext?.unbindService(serviceConnection) }
        bound = false
    }

    /** The strings the card uses, in one place. */
    object InLauncher {
        const val DETAILS = "In the launcher"

        const val HOME = "Browsing the dashboard"
        const val LIBRARY = "Browsing instances"
        const val DISCOVER = "Browsing versions"
        const val CREATE_INSTANCE = "Creating an instance"
        const val WALLPAPERS = "Picking a wallpaper"
        const val SETTINGS = "In settings"
        const val ACCOUNTS = "Managing accounts"
        const val FILES = "In the file browser"
        const val ABOUT = "Reading about the launcher"
        const val SOCIALS = "Chatting in Socials"
        const val AERIX_CLIENT = "Configuring Apex Client 1.16.5"
        const val GENERIC = "Browsing"

        fun stateFor(section: String?): String = when (section?.uppercase()) {
            null, "HOME" -> HOME
            "AERIX_CLIENT" -> AERIX_CLIENT
            "LIBRARY" -> LIBRARY
            "DISCOVER", "DOWNLOADS" -> DISCOVER
            "CREATE_INSTANCE" -> CREATE_INSTANCE
            "WALLPAPERS" -> WALLPAPERS
            "SETTINGS", "LOGS", "CONTRIBUTION", "CONTRIBUTION_DETAIL" -> SETTINGS
            "ACCOUNTS", "ADD_ACCOUNT", "OFFLINE_ACCOUNT" -> ACCOUNTS
            "FILES" -> FILES
            "SOCIALS" -> SOCIALS
            "ABOUT", "OTHER_RELEASES" -> ABOUT
            else -> GENERIC
        }
    }
}

/**
 * The Discord application the presence is published under, registered once for the project as
 * "Apex Launcher" (Developer Portal → New Application). Its name is the title on the card, so it
 * has to be this application and not a per-user one: every install shows the same card with no
 * setup. A user can still override it in Settings → Launcher; blank means "use this".
 */
object AerixDiscordApp {
    const val DEFAULT_APPLICATION_ID = "1566402542487736410"
}

/**
 * Discord application ids are 17–20 digit numbers. Anything else (an empty field, a client id, a
 * public key) is rejected before it can reach Discord, which would otherwise drop the connection.
 */
object DiscordAppId {
    /** A snowflake is 17-20 digits; anything else is a typo and Discord would reject it. */
    private fun overrideOrNull(): String? {
        val raw = AerixPreferences.discordAppId.trim().trim('"', '\'')
        return raw.takeIf { it.length in 17..20 && it.all(Char::isDigit) }
    }

    fun hasCustomOverride(): Boolean = overrideOrNull() != null

    /** The override from settings, or the application id this build ships with. */
    fun effective(): String = overrideOrNull() ?: AerixDiscordApp.DEFAULT_APPLICATION_ID

    fun asLong(context: Context?): Long? {
        val raw = effective()
        val id = raw.toLongOrNull() ?: return null
        return id.takeIf { it > 0 }
    }
}
