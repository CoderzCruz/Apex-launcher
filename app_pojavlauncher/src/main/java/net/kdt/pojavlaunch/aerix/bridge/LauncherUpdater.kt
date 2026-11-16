package net.kdt.pojavlaunch.aerix.bridge

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import net.kdt.pojavlaunch.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal enum class LauncherUpdatePhase { CHECKING, CURRENT, AVAILABLE, DOWNLOADING, READY, ERROR }

/** Process-lifetime download: closing/minimizing the dialog does not cancel an accepted update. */
internal object LauncherUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    @Volatile private var connection: HttpURLConnection? = null
    /** The connections of the download in progress, so cancelling closes every one of them. */
    @Volatile private var downloadConnections: DownloadConnections? = null
    var phase by mutableStateOf(LauncherUpdatePhase.CHECKING); private set
    var release by mutableStateOf<LauncherRelease?>(null); private set
    var received by mutableLongStateOf(0L); private set
    var error by mutableStateOf(""); private set
    var awaitingPermission by mutableStateOf(false); private set
    var installing by mutableStateOf(false); private set
    /** Result of the last download speed test, for the debug build only. */
    var speedTestResult by mutableStateOf(""); private set
    var speedTestRunning by mutableStateOf(false); private set
    /** Bytes read by the speed test so far, so its progress is visible while it samples. */
    var speedTestReceived by mutableLongStateOf(0L); private set
    private var speedTestJob: Job? = null
    /** Whole-file test: the real update path, so the number it prints is the update's number. */
    var fullTestRunning by mutableStateOf(false); private set
    var fullTestResult by mutableStateOf(""); private set
    var fullTestReceived by mutableLongStateOf(0L); private set
    var fullTestSize by mutableLongStateOf(0L); private set
    private var fullTestJob: Job? = null
    private var apk: File? = null

    private const val VORTEX_APK_NAME = "Vortex.Launcher.v1.apk"
    private const val VORTEX_APK_URL =
        "https://github.com/CoderzCruz/Apex-launcher/releases/download/debug-latest/Vortex.Launcher.v1.apk"

    /**
     * This launcher's own auto-update: the debug-latest release the CI publishes. Returns a
     * release only when the published versionCode is newer than this install, so every build
     * offers itself exactly once. Null (current or unreadable) falls through to the upstream
     * check below, which keeps running unchanged.
     */
    private suspend fun fetchVortexDebugRelease(): LauncherRelease? {
        val response = open("https://api.github.com/repos/CoderzCruz/Apex-launcher/releases/tags/debug-latest")
        val body = try {
            if (response.responseCode == 404) return null
            require(response.responseCode == 200) { "GitHub returned HTTP ${response.responseCode}. Please try again later." }
            response.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (out.size() <= 1024 * 1024) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    out.write(buffer, 0, count)
                }
                out.toByteArray().toString(Charsets.UTF_8)
            }
        } finally { response.disconnect(); connection = null }
        val json = try { org.json.JSONObject(body) } catch (e: Exception) { return null }
        val releaseBody = json.optString("body")
        // Owner gate: no auto-update until UPDATE_ROLLOUT is flipped to "on" in the repo.
        val rollout = Regex("Update rollout:\\*\\*\\s*`?(on|off)", RegexOption.IGNORE_CASE)
            .find(releaseBody)?.groupValues?.get(1)?.lowercase()
        if (rollout != "on") return null
        val released = Regex("versionCode[^0-9]{0,8}(\\d+)")
            .find(releaseBody)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        if (released <= BuildConfig.VERSION_CODE) return null
        val installed = LauncherVersion.parse(BuildConfig.VERSION_NAME) ?: return null

        var apkSize = 0L
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == VORTEX_APK_NAME) {
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }
        if (apkSize <= 0L || apkSize > LauncherUpdatePolicy.MAX_APK_BYTES) return null

        // Encode the versionCode as the offer version so each new build is a fresh, higher offer
        // (versionName stays 2.0.0 across builds); tag stays the clean versionName so validate()'s
        // expectedVersion matches the downloaded noruntime APK.
        val version = LauncherVersion(
            (released / 1_000_000L).toInt(),
            ((released / 1_000L) % 1_000L).toInt(),
            (released % 1_000L).toInt()
        )
        return LauncherRelease(
            tag = "v${installed.major}.${installed.minor}.${installed.patch}",
            version = version,
            notes = json.optString("name").ifBlank { "Vortex Launcher update $released" },
            asset = LauncherUpdateAsset(
                name = VORTEX_APK_NAME,
                url = VORTEX_APK_URL,
                size = apkSize,
                sha256 = fetchVortexSha256()
            ),
            vortex = true
        )
    }

    /** The CI-published `<apk>.sha256` sidecar: first hex token of the sha256sum file. */
    private fun fetchVortexSha256(): String? {
        val response = open(VORTEX_APK_URL + ".sha256")
        try {
            if (response.responseCode != 200) return null
            val text = response.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                val count = input.read(buffer)
                if (count <= 0) "" else String(buffer, 0, count, Charsets.UTF_8)
            }
            return text.trim().substringBefore(' ').lowercase()
                .takeIf { it.length == 64 && it.all { ch -> ch in "0123456789abcdef" } }
        } finally { response.disconnect(); connection = null }
    }

    fun check() {
        if (job?.isActive == true || (phase == LauncherUpdatePhase.READY && apk?.isFile == true)) return
        phase = LauncherUpdatePhase.CHECKING; error = ""
        job = scope.launch {
            try {
                val vortex = fetchVortexDebugRelease()
                if (vortex != null) {
                    withContext(Dispatchers.Main) {
                        release = vortex
                        phase = LauncherUpdatePhase.AVAILABLE
                    }
                    return@launch
                }
                val response = open("https://api.github.com/repos/entitybrian69-bit/Aerix-launcher/releases/latest")
                val result = try {
                    if (response.responseCode == 404) null else {
                        require(response.responseCode == 200) { "GitHub returned HTTP ${response.responseCode}. Please try again later." }
                        val bytes = response.inputStream.use { input ->
                            val out = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            while (out.size() <= 256 * 1024) {
                                val count = input.read(buffer)
                                if (count == -1) break
                                out.write(buffer, 0, count)
                            }
                            out.toByteArray()
                        }
                        require(bytes.size <= 256 * 1024) { "Release response is too large" }
                        LauncherUpdatePolicy.release(
                            bytes.toString(Charsets.UTF_8),
                            BuildConfig.FLAVOR == "noruntime",
                            // The build for this device's own CPU: arm64 where it exists, arm32 on
                            // the 32-bit phones, the universal APK where neither applies.
                            LauncherUpdatePolicy.preferredAbi(Build.SUPPORTED_64_BIT_ABIS, Build.SUPPORTED_32_BIT_ABIS)
                        )
                    }
                } finally { response.disconnect(); connection = null }
                withContext(Dispatchers.Main) {
                    release = result
                    val installed = LauncherVersion.parse(BuildConfig.VERSION_NAME)
                    require(installed != null) { "This build does not have a stable release version" }
                    phase = if (result != null && result.version > installed) LauncherUpdatePhase.AVAILABLE else LauncherUpdatePhase.CURRENT
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                fail(e)
            }
        }
    }
    fun download(context: Context) {
        if (phase == LauncherUpdatePhase.DOWNLOADING || phase == LauncherUpdatePhase.CHECKING) return
        val update = release ?: return
        val app = context.applicationContext
        phase = LauncherUpdatePhase.DOWNLOADING; error = ""; received = 0
        val connections = DownloadConnections()
        downloadConnections = connections
        job = scope.launch {
            val directory = File(app.cacheDir, "updates")
            val target = File(directory, update.asset.name)
            try {
                require(directory.isDirectory || directory.mkdirs()) { "Cannot create the update cache" }
                // Six connections instead of one: a single stream on a phone's link spends most of
                // its time waiting, which is what made a hundred-megabyte update crawl. The parts
                // land straight in the file and the checksum is verified once it is whole.
                val result = FastDownload.download(
                    url = update.asset.url,
                    target = target,
                    expectedSize = update.asset.size,
                    userAgent = "AerixLauncher/${BuildConfig.VERSION_NAME}",
                    connections = connections,
                    onProgress = { bytes -> received = bytes }
                )
                withContext(Dispatchers.Main) { received = result.bytes }
                require(update.asset.sha256 == null || result.sha256 == update.asset.sha256) {
                    "Update checksum verification failed"
                }
                validate(app, target, update)
                withContext(Dispatchers.Main) { apk = target; phase = LauncherUpdatePhase.READY }
            } catch (e: Exception) {
                target.delete()
                if (e is CancellationException || !currentCoroutineContext().isActive) {
                    withContext(NonCancellable + Dispatchers.Main) { phase = LauncherUpdatePhase.AVAILABLE; received = 0 }
                } else fail(e)
            } finally {
                downloadConnections = null
            }
        }
    }
    /**
     * Measures how fast this release downloads: once over a single connection, then over the whole
     * pool the updater uses. Debug builds only. It reads a few megabytes of the published APK and
     * installs nothing, so it answers "is my link slow, or was the download slow?" in seconds.
     *
     * Every phase is capped, so the dialog always ends with a number or a plain reason - it can
     * never sit on "Measuring" while a shaped connection dribbles.
     */
    fun measureDownloadSpeed(context: Context) {
        if (speedTestRunning) return
        val update = release ?: return
        speedTestRunning = true
        speedTestResult = ""
        speedTestReceived = 0
        speedTestJob = scope.launch {
            val userAgent = "AerixLauncher/${BuildConfig.VERSION_NAME}"
            val active = DownloadConnections()
            try {
                val single = FastDownload.measureRate(
                    url = update.asset.url,
                    userAgent = userAgent,
                    connections = active,
                    parallel = 1,
                    sampleBytes = 3L * 1024 * 1024,
                    timeoutMs = 25_000
                ) { bytes -> speedTestReceived = bytes }
                withContext(Dispatchers.Main) {
                    speedTestResult = if (single == null) {
                        "1 connection: no data came back in 25 s."
                    } else {
                        "1 connection: %.2f MB/s".format(single / 1048576.0)
                    }
                    speedTestReceived = 0
                }
                val pooled = FastDownload.measureRate(
                    url = update.asset.url,
                    userAgent = userAgent,
                    connections = active,
                    parallel = FastDownload.MAX_CONNECTIONS,
                    sampleBytes = 12L * 1024 * 1024,
                    timeoutMs = 30_000
                ) { bytes -> speedTestReceived = bytes }
                withContext(Dispatchers.Main) {
                    val firstLine = if (single == null) "1 connection: no data in 25 s"
                    else "1 connection: %.2f MB/s".format(single / 1048576.0)
                    speedTestResult = if (pooled == null) {
                        firstLine + "\n%d connections: no data in 30 s".format(FastDownload.MAX_CONNECTIONS)
                    } else if (single != null && single > 0) {
                        "%s\n%d connections: %.2f MB/s (%.1f× faster)".format(
                            firstLine, FastDownload.MAX_CONNECTIONS,
                            pooled / 1048576.0, pooled / single
                        )
                    } else {
                        "%s\n%d connections: %.2f MB/s".format(
                            firstLine, FastDownload.MAX_CONNECTIONS, pooled / 1048576.0
                        )
                    }
                    speedTestReceived = 0
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    withContext(NonCancellable + Dispatchers.Main) {
                        speedTestResult = "Speed test cancelled."
                        speedTestReceived = 0
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        speedTestResult = e.message ?: "The speed test failed"
                        speedTestReceived = 0
                    }
                }
            } finally {
                // Close whatever is still open, so a capped phase cannot leave a lane reading.
                runCatching { active.disconnectAll() }
                withContext(NonCancellable + Dispatchers.Main) { speedTestRunning = false }
            }
        }
    }

    /**
     * Downloads the whole published APK through the exact code path an update takes - same pool,
     * same adaptive growth, same checksum - then throws the file away and reports the average
     * rate. Debug builds only. This is the honest answer to "will my next update be fast?".
     */
    fun measureFullDownload(context: Context) {
        if (fullTestRunning) return
        val update = release ?: return
        val app = context.applicationContext
        fullTestRunning = true
        fullTestResult = ""
        fullTestReceived = 0
        fullTestSize = update.asset.size
        fullTestJob = scope.launch {
            val directory = File(app.cacheDir, "updates")
            val target = File(directory, "speedtest-full.apk")
            val connections = DownloadConnections()
            try {
                require(directory.isDirectory || directory.mkdirs()) { "Cannot create the update cache" }
                val started = System.nanoTime()
                val result = FastDownload.download(
                    url = update.asset.url,
                    target = target,
                    expectedSize = update.asset.size,
                    userAgent = "AerixLauncher/${BuildConfig.VERSION_NAME}",
                    connections = connections,
                    onProgress = { bytes -> fullTestReceived = bytes }
                )
                val seconds = (System.nanoTime() - started) / 1_000_000_000.0
                val megabytes = result.bytes / 1048576.0
                val rate = if (seconds > 0) megabytes / seconds else 0.0
                withContext(Dispatchers.Main) {
                    fullTestResult = "%.1f MB in %.1f s · average %.2f MB/s".format(megabytes, seconds, rate)
                    fullTestReceived = 0
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    withContext(NonCancellable + Dispatchers.Main) { fullTestResult = "Test cancelled."; fullTestReceived = 0 }
                } else {
                    withContext(Dispatchers.Main) { fullTestResult = e.message ?: "The test download failed"; fullTestReceived = 0 }
                }
            } finally {
                runCatching { connections.disconnectAll() }
                runCatching { target.delete() }
                withContext(NonCancellable + Dispatchers.Main) { fullTestRunning = false }
            }
        }
    }

    /** Stops a running whole-file test. */
    fun cancelFullTest() {
        fullTestJob?.cancel()
    }

    /** Stops a running speed test, leaving its result on screen. */
    fun cancelSpeedTest() {
        speedTestJob?.cancel()
    }

    fun cancelDownload() {
        job?.cancel()
        val active = connection
        val parts = downloadConnections
        scope.launch {
            runCatching { active?.disconnect() }
            // Closes every part's connection, not just the one the last request used, so a
            // cancelled download stops pushing bytes into the cache immediately.
            runCatching { parts?.disconnectAll() }
        }
    }
    private fun open(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000; readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "AerixLauncher/${BuildConfig.VERSION_NAME}")
            instanceFollowRedirects = true
            connection = this
        }
    }
    private suspend fun fail(e: Exception) = withContext(Dispatchers.Main) {
        error = e.message ?: "The update could not be completed. Please try again."
        phase = LauncherUpdatePhase.ERROR
    }
    @Suppress("DEPRECATION")
    private fun validate(context: Context, file: File, update: LauncherRelease) {
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val pm = context.packageManager
        val next = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("Downloaded file is not a valid APK")
        val current = pm.getPackageInfo(context.packageName, flags)
        require(next.packageName == context.packageName) { "This APK is for a different launcher edition" }
        val code = if (Build.VERSION.SDK_INT >= 28) next.longVersionCode else next.versionCode.toLong()
        val currentCode = if (Build.VERSION.SDK_INT >= 28) current.longVersionCode else current.versionCode.toLong()
        val expectedVersion = update.tag.removePrefix("v") + if (BuildConfig.FLAVOR == "noruntime") "-noruntime" else ""
        require(code > currentCode && next.versionName == expectedVersion) { "APK version or runtime edition does not match a newer release" }
        if (Build.VERSION.SDK_INT >= 24) require((next.applicationInfo?.minSdkVersion ?: 23) <= Build.VERSION.SDK_INT) { "This update requires a newer Android version" }
        val installedSignatures = if (Build.VERSION.SDK_INT >= 28) current.signingInfo?.apkContentsSigners else current.signatures
        val newSignatures = if (Build.VERSION.SDK_INT >= 28) next.signingInfo?.apkContentsSigners else next.signatures
        require(!installedSignatures.isNullOrEmpty() && !newSignatures.isNullOrEmpty() &&
            installedSignatures.map { it.toCharsString() }.toSet() == newSignatures.map { it.toCharsString() }.toSet()) { "Update signing certificate does not match the installed launcher" }
    }
    fun install(context: Context) {
        if (installing) return
        val file = apk ?: return
        val update = release ?: return
        installing = true
        scope.launch {
            try {
                validate(context.applicationContext, file, update)
                withContext(Dispatchers.Main) {
                    if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                        awaitingPermission = true
                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                    } else {
                        awaitingPermission = false
                        error = ""
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            clipData = ClipData.newRawUri("Vortex update", uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    awaitingPermission = false
                    error = e.message ?: "Android could not open the installer"
                }
            } finally { withContext(Dispatchers.Main) { installing = false } }
        }
    }
    fun permissionReturned(context: Context) {
        if (!awaitingPermission) return
        awaitingPermission = false
        if (Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()) install(context)
        else error = "Allow Vortex to install app updates, then tap Install again."
    }
}
