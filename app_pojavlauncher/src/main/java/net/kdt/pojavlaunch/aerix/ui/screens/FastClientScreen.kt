package net.kdt.pojavlaunch.aerix.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.bridge.AssetType
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.ui.LauncherSection

/**
 * Fast Client by mxinspire1 — showcase page with the published versions and the
 * official download/community links. Fast Client is a DESKTOP launcher (Windows /
 * macOS / Linux), so the page opens links in the browser instead of installing
 * anything into this Android launcher.
 */
@Composable
fun FastClientScreen(activity: FragmentActivity, navigate: (LauncherSection) -> Unit) {
    val openLink: (String) -> Unit = { url ->
        if (url == "discover") {
            AerixDownloadRequest.open(AssetType.MODPACK, navigate)
        } else {
            runCatching {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    AerixPage(
        title = "Fast Client",
        subtitle = "by MxInspire · Minecraft performance client"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.card))
                    .background(Brush.horizontalGradient(listOf(Color(0x33DC2626), AerixSurface.panelRaised)))
                    .border(
                        1.4.dp,
                        Brush.horizontalGradient(listOf(Color(0xE0DC2626), Color(0x55DC2626))),
                        RoundedCornerShape(AerixRadii.card)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "FAST CLIENT",
                        fontSize = 24.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "BY MXINSPIRE",
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF6B75)
                    )
                    Text(
                        text = "FPS boost, one-click Fabric mods, skins & capes, custom HUD — the fast launcher by mxinspire1.",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Mx Boost — MxInspire's Android-ready FPS modpack (the fast experience on phones)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.card))
                    .background(Brush.horizontalGradient(listOf(Color(0x3DFF3B30), AerixSurface.panelRaised)))
                    .border(
                        1.6.dp,
                        Brush.horizontalGradient(listOf(Color(0xFFFF5A50), Color(0x99FF3B30))),
                        RoundedCornerShape(AerixRadii.card)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        text = "MX BOOST",
                        fontSize = 22.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "BY MXINSPIRE - FPS MODPACK - ANDROID READY",
                        fontSize = 9.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF6B75)
                    )
                    Text(
                        text = "Tumhare phone ke liye ban gaya: Pojav, Mojo aur Zalith launchers + PC. Minecraft 1.21 se 26.2 tak optimized.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    listOf(
                        "Reduced lag on low-end devices",
                        "Higher and stable FPS",
                        "Carefully tested performance mods"
                    ).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = ">", color = Color(0xFFFF6B75), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            Text(text = line, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.78f))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    FastLinkButton("Find in Discover (search: Mx Boost)", "discover") { openLink(it) }
                    FastLinkButton("Open on Modrinth", "https://modrinth.com/project/GGc38iHO") { openLink(it) }
                }
            }

            // Versions (per official sources)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VERSIONS",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AerixSurface.accent
                )
                listOf("26.1.2", "1.20.1").forEachIndexed { index, version ->
                    val shape = RoundedCornerShape(999.dp)
                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .background(
                                if (index == 0) Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B)))
                                else Brush.horizontalGradient(listOf(AerixSurface.panelRaised, AerixSurface.panelRaised))
                            )
                            .border(
                                AerixSpacing.hairline,
                                if (index == 0) AerixSurface.borderHighlight else AerixSurface.borderSoft,
                                shape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = version,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Features
            listOf(
                "FPS boost" to "Performance-focused launch settings",
                "One-click Fabric mods" to "Modrinth mods directly from the client",
                "Skins & capes" to "Built-in skin and cape management",
                "Custom HUD" to "HUD tools, zoom key and profiles"
            ).forEach { (title, description) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.smPlus)
                ) {
                    Text(text = ">", color = AerixSurface.accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Column {
                        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = description, fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            // Honest note: desktop launcher, third-party software
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.card))
                    .background(Color(0x22FFB020))
                    .border(1.dp, Color(0x66FFB020), RoundedCornerShape(AerixRadii.card))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Note: MX BOOST = phone ke liye (Pojav/Mojo/Zalith + PC) - Discover me search karo ya Modrinth se install karo. FAST CLIENT (PC wala) = desktop launcher - Windows/macOS/Linux ke liye; official links se hi download karo aur file scan karo.",
                    fontSize = 10.5.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }

            // Official links
            FastLinkButton("Download - fastclient.net", "https://fastclient.net/tutorial") { openLink(it) }
            FastLinkButton("GitHub - Mxcore (official)", "https://github.com/hanky121211-collab/Mxcore") { openLink(it) }
            FastLinkButton("Discord - Fast Client", "https://discord.com/invite/fastclient") { openLink(it) }
            Spacer(Modifier.height(AerixSpacing.sm))
        }
    }
}

@Composable
private fun FastLinkButton(label: String, url: String, onClick: (String) -> Unit) {
    val shape = RoundedCornerShape(AerixRadii.card)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFB01B2B))))
            .clickable { onClick(url) }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
    }
}
