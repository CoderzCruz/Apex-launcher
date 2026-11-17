package net.kdt.pojavlaunch.aerix.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.aerix.client.AerixClientCosmetics
import net.kdt.pojavlaunch.aerix.client.AerixClientInstaller
import net.kdt.pojavlaunch.aerix.client.AerixClientStore
import net.kdt.pojavlaunch.aerix.client.ClientCapeSpec
import net.kdt.pojavlaunch.aerix.theme.AerixRadii
import net.kdt.pojavlaunch.aerix.theme.AerixSpacing
import net.kdt.pojavlaunch.aerix.theme.AerixSurface
import net.kdt.pojavlaunch.aerix.ui.AerixNav
import net.kdt.pojavlaunch.aerix.ui.LauncherSection

/**
 * Vortex Cape Shop: the full animated-cape gallery with equip + instance sync.
 * Reachable from the rail; keeps a link back to the Vortex Client hub (which the
 * rail no longer lists directly).
 */
@Composable
fun AerixCapeShopScreen(activity: FragmentActivity) {
    val scope = rememberCoroutineScope()
    val equipped = AerixClientCosmetics.effectiveCapeSpec()

    val equipCape: (ClientCapeSpec) -> Unit = { spec ->
        if (!AerixClientCosmetics.canEquipCape(spec)) {
            Toast.makeText(activity, "Ye cape locked hai — specific account se unlock hota hai", Toast.LENGTH_LONG).show()
        } else {
            val instance = AerixClientInstaller.findInstalledClientInstance(AerixClientStore.selectedClientVersion)
            if (instance == null) {
                Toast.makeText(activity, "Pehle Vortex Client instance install karo", Toast.LENGTH_LONG).show()
            } else {
                AerixClientStore.equippedCapeId = spec.id
                AerixClientStore.persist()
                scope.launch {
                    val synced = runCatching {
                        withContext(Dispatchers.IO) {
                            AerixClientCosmetics.syncCapeAssetsToInstance(activity, instance.gameDirectory)
                        }
                    }.isSuccess
                    val msg = if (synced) "${spec.title} equipped — game me next launch se active"
                        else "${spec.title} selected — sync next launch par hoga"
                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    AerixPage(
        title = "Cape Shop",
        subtitle = "15 animated capes · equip karke next launch me game me lagao"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAPE SHOP",
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AerixSurface.accent
                )
                Text(
                    text = "Equipped: ${equipped.title}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B75)
                )
            }
            AerixClientCosmetics.capes.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AerixSpacing.sm)
                ) {
                    pair.forEach { spec ->
                        ClientCapeTile(
                            spec = spec,
                            equipped = AerixClientStore.equippedCapeId == spec.id,
                            unlocked = AerixClientCosmetics.canEquipCape(spec),
                            onClick = { equipCape(spec) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(AerixSpacing.sm))
            // The rail now opens Cape Shop instead of the client hub — keep the hub one tap away.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AerixRadii.card))
                    .background(Brush.horizontalGradient(listOf(Color(0x33DC2626), AerixSurface.panelRaised)))
                    .border(
                        AerixSpacing.hairline,
                        Brush.horizontalGradient(listOf(Color(0xE0DC2626), Color(0x55DC2626))),
                        RoundedCornerShape(AerixRadii.card)
                    )
                    .clickable { AerixNav.navigate(LauncherSection.CLIENT) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open Vortex Client Hub →",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/** Cape gallery tile: real generated cape preview over the cape's own gradient. */
@Composable
private fun ClientCapeTile(
    spec: ClientCapeSpec,
    equipped: Boolean,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AerixRadii.card)
    val capeBitmap = remember(spec.id) {
        runCatching { AerixClientCosmetics.buildCapeAndElytraBitmap(spec).asImageBitmap() }.getOrNull()
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Color(spec.topHex), Color(spec.bottomHex)))
            )
            .border(
                if (equipped) 2.dp else AerixSpacing.hairline,
                if (equipped) AerixSurface.borderHighlight else AerixSurface.borderSoft,
                shape
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = spec.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!unlocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "LOCKED",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            } else if (equipped) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "ON",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
        capeBitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = spec.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(52.dp)
            )
        }
        Text(
            text = spec.subtitle,
            fontSize = 8.sp,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
