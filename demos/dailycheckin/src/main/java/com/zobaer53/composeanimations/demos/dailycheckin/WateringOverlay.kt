package com.zobaer53.composeanimations.demos.dailycheckin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

private val Water = Color(0xFF4AA8F0)
private val WaterLight = Color(0xFFA8DBFF)
private val Flash = Color(0xFFF7F5FF)
private val Petal = Color(0xFFFF8FAB)
private val FlowerMiddle = Color(0xFFFFC93C)
private val Leaf = Color(0xFF5DBB63)

/** Samples along the jet; enough for the wobble to look smooth. */
private const val Samples = 64
/** Seconds for the bloom wave's front to reach the far corner. */
private const val WaveSeconds = 1.1f
/** Seconds each little flower on the wave lives. */
private const val PopSeconds = 0.75f
private const val BloomMillis = ((WaveSeconds + PopSeconds) * 1000).toInt()

/**
 * Screen-wide effects on top of everything: the water jet from the hold button up into the storm
 * cloud (with ripples off the button and spray off the jet), a white flash each time [strikes]
 * goes up, and the bloom wave across the whole screen each time [cheers] goes up.
 *
 * Positions are in root coordinates.
 *
 * @param source centre of the hold button.
 * @param sourceRadius the button's radius; ripples start at its edge.
 * @param target where the jet enters the cloud.
 * @param bloomFrom where the bloom wave starts: the flower.
 */
@Composable
internal fun WateringOverlay(
    source: Offset,
    sourceRadius: Float,
    target: Offset,
    bloomFrom: Offset,
    flowing: Boolean,
    strikes: Int,
    cheers: Int,
    modifier: Modifier = Modifier,
) {
    // The jet's head shoots from the button to the cloud; when the flow stops its tail follows
    // it up, as if the cloud slurps the last of it.
    val head = remember { Animatable(0f) }
    val tail = remember { Animatable(0f) }
    LaunchedEffect(flowing) {
        if (flowing) {
            tail.snapTo(0f)
            head.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
        } else if (head.value > 0f) {
            tail.animateTo(head.value, tween(durationMillis = 280, easing = FastOutSlowInEasing))
            head.snapTo(0f)
            tail.snapTo(0f)
        }
    }

    val flash = remember { Animatable(0f) }
    val initialStrikes = remember { strikes }
    LaunchedEffect(strikes) {
        if (strikes == initialStrikes) return@LaunchedEffect
        // Soft and short: it sits on top of everything, so a strong flash would bleach the bolt.
        flash.snapTo(0.28f)
        flash.animateTo(0f, tween(durationMillis = 220))
    }

    val bloom = remember { Animatable(0f) }
    val initialCheers = remember { cheers }
    LaunchedEffect(cheers) {
        if (cheers == initialCheers) return@LaunchedEffect
        bloom.snapTo(0f)
        bloom.animateTo(1f, tween(durationMillis = BloomMillis, easing = LinearEasing))
        bloom.snapTo(0f)
    }

    // A clock that only ticks while something is on screen.
    val time = remember { mutableFloatStateOf(0f) }
    val active by remember { derivedStateOf { head.value > 0f || bloom.value > 0f } }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                time.floatValue += (now - last) / 1e9f
                last = now
            }
        }
    }

    var origin by remember { mutableStateOf(Offset.Zero) }
    Canvas(modifier.onGloballyPositioned { origin = it.positionInRoot() }) {
        if (bloom.value > 0f && bloomFrom.isSpecified) {
            drawBloomWave(bloomFrom - origin, bloom.value * BloomMillis / 1000f)
        }
        if (head.value > 0f && source.isSpecified && target.isSpecified) {
            val t = time.floatValue
            val visible = (1f - (tail.value / head.value.coerceAtLeast(0.01f))).coerceIn(0f, 1f)
            drawRipples(source - origin, sourceRadius, t, visible)
            // The jet comes out of the top of the ring, leaving the button's icon visible.
            val nozzle = source - origin - Offset(0f, sourceRadius * 0.85f)
            drawJet(nozzle, target - origin, tail.value, head.value, t)
        }
        if (flash.value > 0f) drawRect(Flash, alpha = flash.value)
    }
}

/**
 * The jet leaves the top of the button, swings out to the left past Sprout, and curls up into the
 * cloud's underside from below.
 */
private class Arc(source: Offset, target: Offset, out: Float) {
    private val p0 = source
    private val p3 = target
    private val span = source.y - target.y
    private val p1 = Offset(source.x - out * 1.1f, source.y - span * 0.5f)
    private val p2 = Offset(target.x - out * 0.9f, target.y + span * 0.35f)

    fun point(t: Float): Offset {
        val m = 1f - t
        return p0 * (m * m * m) + p1 * (3f * m * m * t) + p2 * (3f * m * t * t) + p3 * (t * t * t)
    }

    /** Unit normal, pointing to the right of the direction of travel. */
    fun normal(t: Float): Offset {
        val m = 1f - t
        val d = (p1 - p0) * (3f * m * m) + (p2 - p1) * (6f * m * t) + (p3 - p2) * (3f * t * t)
        val length = hypot(d.x, d.y).coerceAtLeast(0.001f)
        return Offset(-d.y / length, d.x / length)
    }
}

private fun DrawScope.drawJet(source: Offset, target: Offset, from: Float, to: Float, time: Float) {
    if (to - from < 0.005f) return
    val arc = Arc(source, target, out = minOf(140.dp.toPx(), size.width * 0.34f))
    val wobble = 3.5.dp.toPx()

    // The jet wobbles like a hose: a wave runs along it, pinned at both ends.
    fun wobbly(s: Float): Offset {
        val pin = sin(s * PI.toFloat())
        return arc.point(s) + arc.normal(s) * (sin(s * 16f - time * 11f) * wobble * pin)
    }
    val jet = Path()
    for (i in 0..Samples) {
        val s = from + (to - from) * i / Samples
        val p = wobbly(s)
        if (i == 0) jet.moveTo(p.x, p.y) else jet.lineTo(p.x, p.y)
    }

    val round = StrokeCap.Round
    val join = StrokeJoin.Round
    drawPath(jet, Water, alpha = 0.16f, style = Stroke(22.dp.toPx(), cap = round, join = join))
    drawPath(jet, Water, style = Stroke(9.dp.toPx(), cap = round, join = join))
    drawPath(jet, WaterLight, style = Stroke(4.dp.toPx(), cap = round, join = join))
    // Flow marks running from the button up to the cloud.
    val dash = 10.dp.toPx()
    val gap = 22.dp.toPx()
    val travel = (time * 260.dp.toPx()) % (dash + gap)
    drawPath(
        jet, Color.White, alpha = 0.85f,
        style = Stroke(
            2.5.dp.toPx(),
            cap = round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), phase = dash + gap - travel),
        ),
    )

    // Bubbles riding the jet.
    for (i in 0 until 5) {
        val s = (time * 0.9f + i / 5f) % 1f
        if (s < from || s > to) continue
        drawCircle(Color.White, radius = 2.6.dp.toPx(), center = wobbly(s), alpha = 0.9f)
    }

    // Spray: droplets peel off the jet and fall away. Each one restarts at a new spot every cycle.
    for (i in 0 until 9) {
        val cycle = time * 1.4f + i / 9f
        val p = cycle % 1f
        val s = from + (to - from) * (0.15f + 0.7f * hash(floor(cycle) * 7.13f + i * 3.7f))
        val side = if (i % 2 == 0) 1f else -1f
        val drop = wobbly(s) + arc.normal(s) * (side * p * 26.dp.toPx()) + Offset(0f, p * p * 34.dp.toPx())
        drawCircle(Water, radius = 2.4.dp.toPx() * (1f - p * 0.5f), center = drop, alpha = (1f - p) * 0.8f)
    }

    // Where the jet hits the cloud: misty puffs billowing out.
    if (to > 0.97f && from < 0.9f) {
        for (i in 0 until 6) {
            val p = (time * 1.8f + i / 6f) % 1f
            val angle = (-200f + i * 45f) * PI.toFloat() / 180f
            val puff = target + Offset(cos(angle), sin(angle)) * (6.dp.toPx() + p * 18.dp.toPx())
            drawCircle(Color.White, radius = (3f + p * 5f) * 1.dp.toPx(), center = puff, alpha = (1f - p) * 0.8f)
            drawCircle(WaterLight, radius = 1.6.dp.toPx(), center = puff, alpha = (1f - p) * 0.9f)
        }
    }
}

/** Rings spreading off the button while it's held. */
private fun DrawScope.drawRipples(center: Offset, radius: Float, time: Float, visible: Float) {
    for (i in 0 until 3) {
        val p = (time / 1.2f + i / 3f) % 1f
        drawCircle(
            Water,
            radius = radius + p * 64.dp.toPx(),
            center = center,
            alpha = (1f - p) * 0.4f * visible,
            style = Stroke(3.dp.toPx() * (1f - p * 0.6f)),
        )
    }
}

private const val WaveColumns = 6
private const val WaveRows = 12

/**
 * A ring rolls out from the flower to past the screen's corners, and everywhere it passes a little
 * flower, leaf or sparkle pops up, spins and fades.
 *
 * @param elapsed seconds since the bloom.
 */
private fun DrawScope.drawBloomWave(origin: Offset, elapsed: Float) {
    val far = maxOf(
        hypot(origin.x, origin.y),
        hypot(size.width - origin.x, origin.y),
        hypot(origin.x, size.height - origin.y),
        hypot(size.width - origin.x, size.height - origin.y),
    )
    // The front decelerates: radius = far * (1 - (1 - x)²).
    val x = (elapsed / WaveSeconds).coerceIn(0f, 1f)
    val front = far * (1f - (1f - x) * (1f - x))
    val fade = (1f - elapsed / (WaveSeconds * 1.2f)).coerceIn(0f, 1f)
    if (fade > 0f) {
        drawCircle(Leaf, radius = front, center = origin, alpha = 0.16f * fade, style = Stroke(44.dp.toPx()))
        drawCircle(Leaf, radius = front, center = origin, alpha = 0.6f * fade, style = Stroke(4.dp.toPx()))
        drawCircle(Petal, radius = front * 0.86f, center = origin, alpha = 0.45f * fade, style = Stroke(3.dp.toPx()))
    }

    val cellW = size.width / WaveColumns
    val cellH = size.height / WaveRows
    val keepClear = 120.dp.toPx()
    for (row in 0 until WaveRows) {
        for (col in 0 until WaveColumns) {
            val i = row * WaveColumns + col
            val p = Offset(
                (col + 0.2f + 0.6f * hash(i * 1.7f)) * cellW,
                (row + 0.2f + 0.6f * hash(i * 2.9f + 5f)) * cellH,
            )
            val d = hypot(p.x - origin.x, p.y - origin.y)
            // Leave Sprout itself uncovered.
            if (d < keepClear) continue
            // When the front reached this spot: invert the front's easing.
            val arrival = WaveSeconds * (1f - sqrt((1f - d / far).coerceAtLeast(0f)))
            val q = (elapsed - arrival) / PopSeconds
            if (q <= 0f || q >= 1f) continue
            val scale = if (q < 0.25f) overshoot(q / 0.25f) else 1f - ((q - 0.25f) / 0.75f).let { it * it }
            val alpha = if (q < 0.6f) 1f else (1f - q) / 0.4f
            val spin = hash(i * 4.3f) * 360f + q * 120f
            rotate(spin, pivot = p) {
                when (i % 3) {
                    0 -> drawMiniFlower(p, scale, alpha)
                    1 -> drawOval(
                        Leaf,
                        topLeft = p - Offset(5f, 2.8f) * (scale * 1.6.dp.toPx()),
                        size = Size(10f, 5.6f) * (scale * 1.6.dp.toPx()),
                        alpha = alpha,
                    )
                    else -> drawSparkle(p, 8.dp.toPx() * scale, alpha)
                }
            }
        }
    }
}

private fun DrawScope.drawMiniFlower(center: Offset, scale: Float, alpha: Float) {
    val k = scale * 1.6.dp.toPx()
    for (j in 0 until 5) {
        val a = j * 72f * PI.toFloat() / 180f
        drawCircle(Petal, radius = 3.2f * k, center = center + Offset(cos(a), sin(a)) * (3.6f * k), alpha = alpha)
    }
    drawCircle(FlowerMiddle, radius = 2.4f * k, center = center, alpha = alpha)
}

private fun DrawScope.drawSparkle(c: Offset, r: Float, alpha: Float) {
    val star = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(star, FlowerMiddle, alpha = alpha)
}

/** 0 → 1 with a bounce past 1 on the way, for things popping into view. */
private fun overshoot(x: Float): Float {
    val c = 2.2f
    val m = x - 1f
    return 1f + (c + 1f) * m * m * m + c * m * m
}

/** A repeatable pseudo-random number in 0..1. */
private fun hash(x: Float): Float {
    val v = sin(x * 12.9898f) * 43758.547f
    return v - floor(v)
}
