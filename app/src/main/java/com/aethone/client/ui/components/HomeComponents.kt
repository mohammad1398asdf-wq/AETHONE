package com.aethone.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.ui.theme.Aethone

/** Rounded-square icon chip used for the theme toggle and the gear button. */
@Composable
fun IconChip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Aethone.extras.text,
    bg: Color = Aethone.extras.surfaceVariant,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = tint, modifier = Modifier.size(22.dp)
        )
    }
}

/** White/elevated card with the app's generous corner radius. */
@Composable
fun AethoneCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Aethone.extras.surface, RoundedCornerShape(22.dp))
            .shadow(
                elevation = if (Aethone.extras.background == com.aethone.client.ui.theme.LBackground) 6.dp else 0.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x14000000),
            )
    ) { content() }
}

/** The location card of the home screen (flag + COUNTRY / CITY). */
@Composable
fun LocationCard(
    flag: String,
    label: String,
    sublabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    AethoneCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(flag, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Aethone.extras.text,
                    maxLines = 1,
                )
                Text(
                    sublabel,
                    fontSize = 11.sp,
                    color = Aethone.extras.textDim,
                    maxLines = 1,
                )
            }
        }
    }
}

/** The ping card of the home screen (dot + value). */
@Composable
fun PingCard(
    pingMs: Int,
    dotColor: Color,
    modifier: Modifier = Modifier,
) {
    AethoneCard(modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "PING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Aethone.extras.textDim,
                )
                Text(
                    if (pingMs >= 0) "$pingMs" else "—",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Aethone.extras.text,
                )
            }
        }
    }
}

/** DOWN / UP stat card with the live rate underneath the volume. */
@Composable
fun StatCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    volume: String,
    rate: String,
    modifier: Modifier = Modifier,
) {
    AethoneCard(modifier = modifier) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Aethone.extras.textDim,
                )
                Text(
                    volume,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Aethone.extras.text,
                )
                Text(
                    rate,
                    fontSize = 11.sp,
                    color = Aethone.extras.textDim,
                )
            }
        }
    }
}

/** Capsule showing the active protocol; opens the protocol picker. */
@Composable
fun ProtocolPill(
    protocol: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AethoneCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AethoneMark(
                status = com.aethone.client.core.VpnStatus.CONNECTED,
                color = accent,
                glowColor = accent,
                modifier = Modifier.size(16.dp),
                glow = false,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                protocol,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Aethone.extras.text,
            )
        }
    }
}

/** Round connect/disconnect orb next to the protocol pill. */
@Composable
fun ToggleOrb(
    accent: Color,
    wash: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(wash, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AethoneMark(
            status = com.aethone.client.core.VpnStatus.CONNECTED,
            color = accent,
            glowColor = accent,
            modifier = Modifier.size(26.dp),
            glow = false,
        )
    }
}
