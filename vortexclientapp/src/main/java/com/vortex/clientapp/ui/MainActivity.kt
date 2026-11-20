package com.vortex.clientapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.clientapp.data.ClientPrefs

// VORTEX Premium tokens (same identity as the desktop client).
object C {
    val bg = Color(0xFF090909)
    val secondary = Color(0xFF111111)
    val card = Color(0xFF181818)
    val red = Color(0xFFE21F3D)
    val redHover = Color(0xFF304F)
    val text = Color(0xFFF2F2F4)
    val text2 = Color(0xFF858585)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme(
                    primary = C.red, background = C.bg, surface = C.secondary
                )
            ) { VortexClientApp() }
        }
    }
}

private enum class Screen(val title: String) {
    MENU("Menu"), SINGLEPLAYER("Singleplayer"), MULTIPLAYER("Multiplayer"),
    MODS("Mods"), COSMETICS("Cosmetics"), PERFORMANCE("Performance"),
    SETTINGS("Settings"), ACCOUNTS("Accounts")
}

@Composable
private fun VortexClientApp() {
    var screen by remember { mutableStateOf(Screen.MENU) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(Modifier.fillMaxSize().background(C.bg)) {
        // Header: brand + username + settings shortcut
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("VORTEX", color = C.red, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Text("CLIENT", color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ClientPrefs.username(context), color = C.text, fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text("⚙", color = C.text2, fontSize = 17.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .clickable { screen = Screen.SETTINGS }.padding(6.dp))
            }
        }

        if (screen != Screen.MENU) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("‹ Menu", color = C.text2, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { screen = Screen.MENU }.padding(8.dp))
                Text(screen.title.uppercase(), color = C.red, fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
            }
        }

        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            when (screen) {
                Screen.MENU -> MenuScreen { screen = it }
                Screen.SINGLEPLAYER -> SingleplayerScreen()
                Screen.MULTIPLAYER -> MultiplayerScreen()
                Screen.MODS -> ModsScreen()
                Screen.COSMETICS -> CosmeticsScreen()
                Screen.PERFORMANCE -> PerformanceScreen()
                Screen.SETTINGS -> SettingsPanel()
                Screen.ACCOUNTS -> AccountsScreen()
            }
        }

        // Footer: client identity + ready status
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Minecraft - · Vortex Client 1.0.0", color = C.text2, fontSize = 10.5.sp)
            Text("● CLIENT", color = C.red, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Feather-style menu: big red angular buttons + character pane. */
@Composable
private fun MenuScreen(go: (Screen) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text("VORTEX", color = C.text, fontSize = 44.sp, fontWeight = FontWeight.Black)
        Text("Welcome back, ${ClientPrefs.username(context)}", color = C.text2, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Text("CLIENT MENU", color = C.red, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp)
        Spacer(Modifier.height(8.dp))
        listOf(
            "SINGLEPLAYER" to Screen.SINGLEPLAYER,
            "MULTIPLAYER" to Screen.MULTIPLAYER,
            "MODS" to Screen.MODS,
            "SETTINGS" to Screen.SETTINGS,
            "COSMETICS" to Screen.COSMETICS,
            "PERFORMANCE" to Screen.PERFORMANCE,
            "ACCOUNTS" to Screen.ACCOUNTS
        ).forEach { (label, target) ->
            RedButton(label) { go(target) }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(10.dp))
        // Character pane (blocky silhouette, same art language as the desktop client)
        Box(
            Modifier.fillMaxWidth().height(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF141414), C.bg)))
        ) {
            Text("PLAYER PREVIEW", color = C.text2, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
            Text("V", color = C.red.copy(alpha = 0.55f), fontSize = 90.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun RedButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(listOf(C.red, Color(0xFFB01B2B))))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp)
    }
}

@Composable
fun GhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(C.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = C.text, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}
