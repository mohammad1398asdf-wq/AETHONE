package com.aethone.client.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aethone.client.MainActivity
import com.aethone.client.R
import com.aethone.client.core.AethoneVpnService
import com.aethone.client.core.ConnectionBus
import com.aethone.client.core.VpnStatus
import com.aethone.client.data.SettingsRepository
import com.aethone.client.data.ThemeMode
import com.aethone.client.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/* ------------------------------------------------------------------ shared */

private object W {
    val statusKey = ActionParameters.Key<String>("status")

    data class Palette(
        val bg: Color,
        val surface: Color,
        val text: Color,
        val dim: Color,
        val green: Color,
        val red: Color,
    )

    fun dark() = Palette(
        bg = Color(0xFF121418), surface = Color(0xFF1C1F26),
        text = Color.White, dim = Color(0xFF8B929E),
        green = Color(0xFF00E676), red = Color(0xFFEF4444),
    )

    fun light() = Palette(
        bg = Color(0xFFF4F7F5), surface = Color.White,
        text = Color(0xFF1E293B), dim = Color(0xFF9CA3AF),
        green = Color(0xFF10B981), red = Color(0xFFEF4444),
    )
}

/** Sends the toggle intent to the VPN service from widget taps. */
class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        AethoneVpnService.start(context, AethoneVpnService.ACTION_TOGGLE)
    }
}

private suspend fun paletteFor(context: Context): W.Palette {
    val mode = SettingsRepository(context).settings.first().themeMode
    val isNight = context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    return when (mode) {
        ThemeMode.DARK -> W.dark()
        ThemeMode.LIGHT -> W.light()
        ThemeMode.SYSTEM -> if (isNight) W.dark() else W.light()
    }
}

@Composable
private fun statusColor(p: W.Palette, status: VpnStatus): Color = when (status) {
    VpnStatus.CONNECTED -> p.green
    VpnStatus.ERROR -> p.red
    VpnStatus.CONNECTING, VpnStatus.DISCONNECTING -> Color(0xFFF5A623)
    VpnStatus.DISCONNECTED -> p.red
}

/* ------------------------------------------------------------ small widget */

class StatusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p = paletteFor(context)
        provideContent {
            val st = ConnectionBus.state.value
            Box(
                GlanceModifier.fillMaxSize().background(p.surface)
                    .clickable(actionRunCallback<ToggleAction>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.glance.Image(
                        provider = androidx.glance.ImageProvider(R.drawable.ic_mark),
                        contentDescription = "AETHONE",
                        modifier = GlanceModifier.size(36.dp),
                        colorFilter = androidx.glance.ColorFilter.tint(
                            ColorProvider(statusColor(p, st.status))
                        )
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        st.status.name,
                        style = TextStyle(
                            color = ColorProvider(p.text),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class StatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StatusWidget()
}

/* ---------------------------------------------------------- medium widget */

class MediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p = paletteFor(context)
        provideContent {
            val st = ConnectionBus.state.value
            Box(GlanceModifier.fillMaxSize().background(p.bg).padding(12.dp)) {
                Column {
                    // header: mark + wordmark + status
                    Row(
                        GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.glance.Image(
                            provider = androidx.glance.ImageProvider(R.drawable.ic_mark),
                            contentDescription = null,
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = androidx.glance.ColorFilter.tint(
                                ColorProvider(statusColor(p, st.status))
                            )
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            "AETHONE",
                            style = TextStyle(
                                color = ColorProvider(p.text),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(GlanceModifier.height(10.dp))
                    // location + ping
                    Row(
                        GlanceModifier.fillMaxWidth().background(p.surface).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(GlanceModifier.defaultWeight()) {
                            Text(
                                st.exitInfo.label,
                                style = TextStyle(color = ColorProvider(p.text), fontSize = 13.sp)
                            )
                            Text(
                                "PING ${if (st.pingMs >= 0) st.pingMs else "—"} ms",
                                style = TextStyle(color = ColorProvider(p.dim), fontSize = 11.sp)
                            )
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        androidx.glance.Image(
                            provider = androidx.glance.ImageProvider(R.drawable.ic_mark),
                            contentDescription = "toggle",
                            modifier = GlanceModifier.size(34.dp)
                                .clickable(actionRunCallback<ToggleAction>()),
                            colorFilter = androidx.glance.ColorFilter.tint(
                                ColorProvider(statusColor(p, st.status))
                            )
                        )
                    }
                }
            }
        }
    }
}

class MediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumWidget()
}

/* ------------------------------------------------------------ large widget */

class LargeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p = paletteFor(context)
        provideContent {
            val st = ConnectionBus.state.value
            Box(GlanceModifier.fillMaxSize().background(p.bg).padding(14.dp)) {
                Column {
                    Row(
                        GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.glance.Image(
                            provider = androidx.glance.ImageProvider(R.drawable.ic_mark),
                            contentDescription = null,
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = androidx.glance.ColorFilter.tint(
                                ColorProvider(statusColor(p, st.status))
                            )
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            "AETHONE",
                            style = TextStyle(
                                color = ColorProvider(p.text),
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        Text(
                            if (st.status == VpnStatus.CONNECTED)
                                Formatters.elapsed(System.currentTimeMillis() - st.connectedAtMillis)
                            else st.status.name,
                            style = TextStyle(color = ColorProvider(p.dim), fontSize = 12.sp)
                        )
                    }
                    Spacer(GlanceModifier.height(10.dp))
                    Row(
                        GlanceModifier.fillMaxWidth().background(p.surface).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(GlanceModifier.defaultWeight()) {
                            Text(
                                st.exitInfo.label,
                                style = TextStyle(color = ColorProvider(p.text), fontSize = 14.sp)
                            )
                            Text(
                                "PING ${if (st.pingMs >= 0) st.pingMs else "—"} ms",
                                style = TextStyle(color = ColorProvider(p.dim), fontSize = 11.sp)
                            )
                        }
                        androidx.glance.Image(
                            provider = androidx.glance.ImageProvider(R.drawable.ic_mark),
                            contentDescription = "toggle",
                            modifier = GlanceModifier.size(38.dp)
                                .clickable(actionRunCallback<ToggleAction>()),
                            colorFilter = androidx.glance.ColorFilter.tint(
                                ColorProvider(statusColor(p, st.status))
                            )
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Row(GlanceModifier.fillMaxWidth()) {
                        Column(
                            GlanceModifier.defaultWeight().background(p.surface).padding(10.dp)
                        ) {
                            Text("DOWN", style = TextStyle(color = ColorProvider(p.green), fontSize = 10.sp))
                            Text(
                                Formatters.bytes(st.downBytes),
                                style = TextStyle(color = ColorProvider(p.text), fontSize = 13.sp)
                            )
                            Text(
                                Formatters.speed(st.downRateBps),
                                style = TextStyle(color = ColorProvider(p.dim), fontSize = 10.sp)
                            )
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column(
                            GlanceModifier.defaultWeight().background(p.surface).padding(10.dp)
                        ) {
                            Text("UP", style = TextStyle(color = ColorProvider(p.red), fontSize = 10.sp))
                            Text(
                                Formatters.bytes(st.upBytes),
                                style = TextStyle(color = ColorProvider(p.text), fontSize = 13.sp)
                            )
                            Text(
                                Formatters.speed(st.upRateBps),
                                style = TextStyle(color = ColorProvider(p.dim), fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class LargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeWidget()
}

/* ---------------------------------------------------------------- updater */

object WidgetUpdater {
    // Glance widget updates are suspend functions; run them off the caller thread.
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
    )

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching { StatusWidget().updateAll(appContext) }
            runCatching { MediumWidget().updateAll(appContext) }
            runCatching { LargeWidget().updateAll(appContext) }
        }
    }
}
