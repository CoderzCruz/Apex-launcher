package net.kdt.pojavlaunch.aerix.ui.screens

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import net.kdt.pojavlaunch.aerix.theme.AerixSurface

/**
 * Owner-only admin panel: the Vortex Admin page bundled as an asset and rendered in a
 * WebView. The page talks to the GitHub API directly (release status, rollout kill-switch,
 * build trigger); the owner's own PAT stays in this WebView's localStorage on this device.
 */
@Composable
fun OwnerPanelScreen(activity: FragmentActivity) {
    AerixPage(
        title = "Owner Panel",
        subtitle = "Vortex Admin · release control (apna GitHub token sirf isi device pe)"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B0D12))
                .border(1.dp, AerixSurface.borderSoft, RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        loadUrl("file:///android_asset/owner_panel.html")
                    }
                }
            )
        }
        Text(
            text = "Token github.com/settings/tokens se lelo (classic · repo scope) — yahin save hota hai.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
