package com.smartlease.app

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val labelStyle = TextStyle(fontSize = 10.sp, color = Col.mutedFg)

private fun DrawScope.centered(tm: TextMeasurer, text: String, cx: Float, y: Float) {
    if (text.isEmpty()) return
    val r = tm.measure(text, labelStyle)
    drawText(r, topLeft = Offset(cx - r.size.width / 2f, y))
}

@Composable
fun AreaChart(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val h = size.height - 16.dp.toPx()
        val top = 4.dp.toPx()
        val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        val slot = size.width / values.size
        val pts = values.mapIndexed { i, v -> Offset(slot * (i + 0.5f), top + (h - top) * (1f - (v / max).toFloat())) }
        fun line(): Path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val a = pts[i - 1]; val b = pts[i]; val mx = (a.x + b.x) / 2f
                cubicTo(mx, a.y, mx, b.y, b.x, b.y)
            }
        }
        val fill = line().apply { lineTo(pts.last().x, h); lineTo(pts.first().x, h); close() }
        drawPath(fill, Brush.verticalGradient(listOf(Col.chart1.copy(alpha = 0.35f), Color.Transparent), startY = 0f, endY = h))
        drawPath(line(), Col.chart1, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        labels.forEachIndexed { i, l -> centered(tm, l, slot * (i + 0.5f), h + 2.dp.toPx()) }
    }
}

@Composable
fun BarChart(series: List<List<Double>>, colors: List<Color>, labels: List<String>, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val h = size.height - 16.dp.toPx()
        val max = (series.flatten().maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        val slot = size.width / labels.size
        val gap = 2.dp.toPx()
        val group = slot * 0.7f
        val bw = (group - gap * (series.size - 1)) / series.size
        for (i in labels.indices) {
            val x0 = slot * i + (slot - group) / 2f
            series.forEachIndexed { s, vals ->
                val bh = (h * (vals[i] / max)).toFloat()
                if (bh > 0f) drawRoundRect(colors[s], Offset(x0 + s * (bw + gap), h - bh), Size(bw, bh), CornerRadius(2.dp.toPx()))
            }
            centered(tm, labels[i], slot * (i + 0.5f), h + 2.dp.toPx())
        }
    }
}

@Composable
fun Donut(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier, stroke: Dp = 16.dp) {
    Canvas(modifier) {
        val total = slices.sumOf { it.first.toDouble() }.toFloat()
        if (total <= 0f) return@Canvas
        val sw = stroke.toPx()
        val d = minOf(size.width, size.height) - sw
        val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        val gap = if (slices.size > 1) 3f else 0f
        var start = -90f
        slices.forEach { (v, c) ->
            val sweep = 360f * v / total
            drawArc(c, start + gap / 2f, (sweep - gap).coerceAtLeast(0.5f), false, tl, Size(d, d), style = Stroke(sw))
            start += sweep
        }
    }
}
