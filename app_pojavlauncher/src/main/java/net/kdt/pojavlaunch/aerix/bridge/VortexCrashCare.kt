package net.kdt.pojavlaunch.aerix.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Crash care: the crash screen speaks an offline Hindi fix through Android TextToSpeech.
 *
 * The message is chosen from the crash text (RAM, network, renderer, Java version,
 * permissions, missing files) with a generic Hindi fallback, so it works with no
 * network at the moment of the crash. Speech runs in the FatalErrorActivity process
 * (the crash handler kills the app process right after starting that activity).
 */
object VortexCrashCare {
    private const val UTTERANCE_ID = "vortex-crash-fix"
    private const val SHUTDOWN_MS = 30_000L
    @Volatile private var spoken = false

    /** Hindi fix text for a crash report / stack trace. English tech terms stay as-is. */
    @JvmStatic
    fun fixTextFor(errorText: String): String {
        val t = errorText.lowercase(Locale.ROOT)
        return when {
            "outofmemory" in t || "out of memory" in t || "gc overhead" in t ->
                "\u0930\u0948\u092e \u0915\u092e \u092a\u0921\u093c \u0917\u0908 \u0939\u0948\u0964 Settings \u092e\u0947\u0902 \u091c\u093e\u0915\u0930 RAM Allocation \u092c\u0922\u093c\u093e\u090f\u0901, \u0926\u0942\u0938\u0930\u0947 \u0906\u092a\u094d\u0938 \u092c\u0902\u0926 \u0915\u0930\u0947\u0902, \u092b\u093c\u093f\u0930 \u0917\u0947\u092e \u0926\u094b\u092c\u093e\u0930\u093e \u091a\u0932\u093e\u090f\u0901\u0964"
            "connect" in t || "unknownhost" in t || "sockettimeout" in t || "ssl" in t || "network" in t || "timed out" in t ->
                "\u092f\u0939 \u0907\u0902\u091f\u0930\u0928\u0947\u091f \u0915\u0940 \u0938\u092e\u0938\u094d\u092f\u093e \u0939\u0948\u0964 WiFi \u092f\u093e \u092e\u094b\u092c\u093e\u0907\u0932 \u0921\u0947\u091f\u093e \u091a\u093e\u0932\u0942 \u0930\u0916\u0947\u0902, VPN \u092c\u0902\u0926 \u0915\u0930\u0947\u0902, \u092b\u093c\u093f\u0930 \u0926\u094b\u092c\u093e\u0930\u093e \u0915\u094b\u0936\u093f\u0936 \u0915\u0930\u0947\u0902\u0964"
            "renderer" in t || "opengl" in t || "egl" in t || "vulkan" in t || "libgl" in t || "gl_init" in t ->
                "\u0917\u094d\u0930\u093e\u092b\u093c\u093f\u0915\u094d\u0938 \u0930\u0947\u0902\u0921\u0930\u0930 \u0915\u0940 \u0938\u092e\u0938\u094d\u092f\u093e \u0939\u0948\u0964 Settings \u092e\u0947\u0902 \u091c\u093e\u0915\u0930 Renderer \u092c\u0926\u0932\u0947\u0902 \u2014 Holy GL, VirGL \u092f\u093e Zink \u2014 \u092b\u093c\u093f\u0930 \u0917\u0947\u092e \u0930\u0940\u0938\u094d\u091f\u093e\u0930\u094d\u091f \u0915\u0930\u0947\u0902\u0964"
            "classnotfound" in t || "nosuchmethod" in t || "classformat" in t || "unsupportedclassversion" in t ->
                "Java \u0935\u0930\u094d\u091c\u0928 \u092e\u0948\u091a \u0928\u0939\u0940\u0902 \u0915\u0930 \u0930\u0939\u093e\u0964 Settings \u0915\u0947 Java \u0938\u0947\u0915\u094d\u0936\u0928 \u092e\u0947\u0902 \u0938\u0939\u0940 JRE \u091a\u0941\u0928\u0947\u0902 \u0914\u0930 \u0917\u0947\u092e \u092b\u093f\u0930 \u0938\u0947 \u091a\u0932\u093e\u090f\u0901\u0964"
            "permission" in t || "securityexception" in t ->
                "\u092a\u0930\u092e\u093f\u0936\u0928 \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u0940\u0964 Settings \u092e\u0947\u0902 Storage \u0914\u0930 Install unknown apps \u0915\u0940 \u0905\u0928\u0941\u092e\u0924\u093f \u0926\u0947\u0902, \u092b\u093c\u093f\u0930 \u0926\u094b\u092c\u093e\u0930\u093e \u091a\u0932\u093e\u090f\u0901\u0964"
            "filenotfound" in t || "no such file" in t || "enoent" in t ->
                "\u0915\u094b\u0908 \u092b\u093c\u093e\u0907\u0932 \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u0940\u0964 Library \u092e\u0947\u0902 \u091c\u093e\u0915\u0930 \u0917\u0947\u092e \u0915\u094b \u090f\u0915 \u092c\u093e\u0930 \u0905\u092a\u0921\u0947\u091f \u092f\u093e \u092b\u093f\u0930 \u0938\u0947 \u0907\u0902\u0938\u094d\u091f\u0949\u0932 \u0915\u0930\u0947\u0902\u0964"
            else ->
                "Vortex Launcher \u0915\u094d\u0930\u0948\u0936 \u0939\u094b \u0917\u092f\u0964 \u0906\u092a \u0905\u092a\u094d\u092a \u092c\u0902\u0926 \u0915\u0930\u0915\u0947 \u0926\u094b\u092c\u093e\u0930\u093e \u0916\u094b\u0932\u0947\u0902\u0964 \u092b\u093c\u093f\u0930 \u092d\u0940 \u0939\u094b \u0924\u094b RAM \u092c\u0922\u093c\u093e\u090f\u0901, mods \u0939\u091f\u093e\u090f\u0901, \u0914\u0930 \u0917\u0947\u092e \u0930\u0940\u0907\u0902\u0938\u094d\u091f\u0949\u0932 \u0915\u0930\u0947\u0901\u0964"
        }
    }

    /** Speaks the Hindi fix once per process; no-op when TTS is unavailable. */
    @JvmStatic
    fun speakHindiFix(context: Context, errorText: String) {
        if (spoken) return
        spoken = true
        speakRaw(context, fixTextFor(errorText))
    }

    private const val FREEZE_TEXT =
        "\u0932\u0917 \u092f\u093e \u092b\u094d\u0930\u0940\u091c \u0939\u094b \u0930\u0939\u093e \u0939\u0948\u0964 " +
        "RAM \u092c\u0922\u093c\u093e\u090f\u0901, mods \u0915\u092e \u0915\u0930\u0947\u0902, \u092b\u093c\u094b\u0928 \u0930\u0940\u0938\u094d\u091f\u093e\u0930\u094d\u091f \u0915\u0930\u0947\u0902, \u092b\u093c\u093f\u0930 \u0917\u0947\u092e \u091a\u0932\u093e\u090f\u0901\u0964"
    private const val FREEZE_TOAST = "Vortex: freeze? RAM badhao / mods kam karo"

    @Volatile private var lastTickAt = 0L
    @Volatile private var freezeSpoken = false
    @Volatile private var watchdogStartedAt = 0L
    private var watchdogStarted = false

    /**
     * Freeze watchdog: a main-looper heartbeat every 2s; when it stops for 12s (and the app has
     * been alive long enough to skip startup unpacking), a background thread speaks the Hindi
     * freeze fix and posts a toast. Recovers automatically when the heartbeat resumes.
     * Call once from Application.onCreate.
     */
    @JvmStatic
    fun startFreezeWatchdog(context: Context) {
        if (watchdogStarted) return
        watchdogStarted = true
        val app = context.applicationContext
        val main = Handler(Looper.getMainLooper())
        watchdogStartedAt = SystemClock.elapsedRealtime()
        lastTickAt = watchdogStartedAt
        val ticker = object : Runnable {
            override fun run() {
                lastTickAt = SystemClock.elapsedRealtime()
                main.postDelayed(this, 2000L)
            }
        }
        main.postDelayed(ticker, 2000L)
        Thread({
            while (true) {
                try {
                    Thread.sleep(3000L)
                    val now = SystemClock.elapsedRealtime()
                    val age = now - lastTickAt
                    // >20s old skips the asset-unpack/startup window; one alert until recovery.
                    if (now - watchdogStartedAt > 20_000L && age > 12_000L && !freezeSpoken) {
                        freezeSpoken = true
                        speakRaw(app, FREEZE_TEXT)
                        main.post {
                            runCatching {
                                Toast.makeText(app, FREEZE_TOAST, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else if (age < 4_000L) {
                        freezeSpoken = false
                    }
                } catch (e: InterruptedException) {
                    return@Thread
                } catch (ignored: Throwable) {
                }
            }
        }, "Vortex-Freeze-Watchdog").apply { isDaemon = true }.start()
    }

    private fun speakRaw(context: Context, text: String) {
        runCatching {
            // The init callback fires after the constructor returns, so the var is set by then;
            // inside the callback a local val keeps every reference resolvable.
            var pending: TextToSpeech? = null
            pending = TextToSpeech(context.applicationContext) { status ->
                val tts = pending ?: return@TextToSpeech
                if (status != TextToSpeech.SUCCESS) {
                    runCatching { tts.shutdown() }
                    return@TextToSpeech
                }
                runCatching {
                    tts.setLanguage(Locale("hi", "IN"))
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            runCatching { tts.stop(); tts.shutdown() }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            runCatching { tts.shutdown() }
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            runCatching { tts.shutdown() }
                        }
                    })
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
                    Handler(Looper.getMainLooper()).postDelayed({
                        runCatching { tts.shutdown() }
                    }, SHUTDOWN_MS)
                }
            }
        }
    }
}
