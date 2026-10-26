package com.aethone.client.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import com.aethone.client.core.VpnStatus

/**
 * The AETHONE brand mark: two interlocking angular polygons, pixel-extracted
 * from the original launcher artwork (100x100 grid, flat isometric look).
 * Red when disconnected, green/teal when connected, pulsing amber while
 * connecting, with an ambient glow behind it in the connected state.
 */
@Composable
fun AethoneMark(
    status: VpnStatus,
    color: Color,
    glowColor: Color,
    modifier: Modifier = Modifier,
    glow: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "mark")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val connecting = status == VpnStatus.CONNECTING
    val scale = if (connecting) pulse else 1f

    Canvas(modifier = modifier) {
        val side = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f

        // ambient glow halo
        if (glow && (status == VpnStatus.CONNECTED || connecting)) {
            val radius = side * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (connecting) 0.35f else 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }

        scale(scale = scale, pivot = Offset(cx, cy)) {
            fun drawPoly(points: List<Pair<Float, Float>>, c: Color, shrink: Float) {
                val path = Path()
                points.forEachIndexed { i, (x, y) ->
                    // normalize 0..100 grid into the canvas, centered
                    val px = cx + (x - 50f) / 100f * side * shrink
                    val py = cy + (y - 50f) / 100f * side * shrink
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, c, style = Fill)
            }
            // upper banner + lower ribbon
            drawPoly(
                listOf(
                    44.9f to 14.6f, 80.9f to 32.6f, 80.9f to 72.5f, 60.7f to 84.8f,
                    60.7f to 45.5f, 25.3f to 27.5f, 44.4f to 15.2f
                ),
                c = color, shrink = 1f
            )
            drawPoly(
                listOf(
                    38.8f to 41.0f, 38.8f to 58.4f, 53.4f to 66.9f, 34.8f to 79.2f,
                    18.5f to 71.3f, 18.5f to 53.4f, 38.2f to 41.6f
                ),
                c = color.copy(alpha = 0.92f), shrink = 1f
            )
        }
    }
}
