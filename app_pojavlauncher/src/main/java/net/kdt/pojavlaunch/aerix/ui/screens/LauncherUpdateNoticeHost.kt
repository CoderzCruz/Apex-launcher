package net.kdt.pojavlaunch.aerix.ui.screens

import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.kdt.pojavlaunch.BuildConfig
import net.kdt.pojavlaunch.aerix.bridge.*

/** Launcher-only: never interrupts Minecraft or polls while this activity is in the background. */
@Composable
internal fun LauncherUpdateNoticeHost(activity: FragmentActivity) {
    var offeredTag by remember { mutableStateOf<String?>(null) }
    val release = LauncherUpdater.release
    val revision = LauncherUpdateNotices.revision
    val manual = LauncherUpdateNotices.manualDialogs
    LaunchedEffect(activity) {
        activity.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                LauncherUpdateNotices.checkIfDue()
                delay(30 * 60_000L)
            }
        }
    }
    // Auto-download the moment an update is offered — only Android's install sheet needs a tap.
    LaunchedEffect(LauncherUpdater.phase, release?.tag) {
        if (LauncherUpdater.phase == LauncherUpdatePhase.AVAILABLE && release != null) {
            LauncherUpdater.download(activity)
        }
    }
    LaunchedEffect(release?.tag, LauncherUpdater.phase, revision, manual) {
        if (manual > 0) offeredTag = null
        else if (offeredTag == null && LauncherUpdater.phase == LauncherUpdatePhase.AVAILABLE &&
            LauncherUpdateNotices.shouldOffer(LauncherVersion.parse(BuildConfig.VERSION_NAME), release?.version,
                LauncherUpdateNotices.acknowledged(activity))) offeredTag = release?.tag
    }
    if (offeredTag != null && manual == 0) {
        LauncherUpdateDialog(activity, automatic = true, onDismiss = { offeredTag = null })
    }
}
