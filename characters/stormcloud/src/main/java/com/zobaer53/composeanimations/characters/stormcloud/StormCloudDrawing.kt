package com.zobaer53.composeanimations.characters.stormcloud

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val Outline = Color(0xFF2F3340)
private val CalmBody = Color(0xFFE3E8EE)
private val StormBody = Color(0xFF5D6473)
private val CalmShade = Color(0xFFC7CFD9)
private val StormShade = Color(0xFF454B58)
private val RelievedBody = Color(0xFFF4F7FA)
private val Lit = Color(0xFFFFF4C2)
private val Flush = Color(0xFFFF6B6B)
private val Rain = Color(0xFF5AAEF0)
private val BoltCore = Color(0xFFFFCC00)
private val BoltHot = Color.White
private val BoltGlow = Color(0xFFFFD60A)

// All geometry is in "cloud units": 1 unit = 1% of the canvas width. The cloud hangs at the top
// of its canvas; rain and lightning fall all the way to the bottom edge, which is the ground.
private const val CenterX = 50f
private const val CenterY = 24f
/** Rain falls from this far below the cloud's centre. */
private const val Underside = 15f

/** The cloud's scale at a given fullness: it swells as it fills up. */
internal fun cloudScale(fullness: Float) = 0.72f + 0.28f * fullness

/** Where the cloud's centre rests, before bobbing and shaking. */
internal fun cloudCenter(u: Float) = Offset(CenterX * u, CenterY * u)

/** The left side of the underside, relative to the centre, in units at scale 1. */
internal val IntakeOffset = Offset(-24f, 14f)

private class Puff(val x: Float, val y: Float, val r: Float)

private val Puffs = listOf(
    Puff(-22f, 3f, 14f),
    Puff(-5f, -7f, 18f),
    Puff(15f, -4f, 16f),
    Puff(29f, 6f, 11f),
    Puff(-34f, 8f, 9f),
)

/**
 * @param fullness 0 = small, pale and grumbling, 1 = big, dark and furious; may overshoot.
 * @param rain 0 = dry, 1 = pouring.
 * @param time seconds, for bobbing, shaking, rain and steam.
 * @param blink 0 = eyes open, 1 = mid-blink.
 * @param bolt life of the current lightning bolt, 0..1; 0 draws none.
 * @param boltSeed picks the bolt's side and shape.
 * @param leave 0 = here, 1 = cheered up and floated away. Fading it out is up to the caller.
 */
internal fun DrawScope.drawStormCloud(
    fullness: Float,
    rain: Float,
    time: Float,
    blink: Float,
    bolt: Float,
    boltSeed: Int,
    leave: Float,
) {
    val u = size.width / 100f
    val outline = 1.4f * u
    val full = fullness.coerceIn(0f, 1f)
    val anger = full * (1f - leave)
    val s = cloudScale(fullness)
    val flash = boltFlicker(bolt)

    // Furious clouds vibrate; a lightning strike adds a big rumble on top.
    val rumble = if (bolt > 0f && bolt < 0.5f) sin(time * 95f) * 1.6f * u * (1f - bolt * 2f) else 0f
    val shake = sin(time * 63f) * 0.45f * u * anger * rain + rumble
    val bob = sin(time * 1.4f) * 1.2f * u
    val c = cloudCenter(u) + Offset(shake, bob - leave * 40f * u)

    if (rain > 0.01f) drawRain(c, s, u, rain, time)
    if (bolt > 0f) drawBolt(c, s, u, outline, bolt, boltSeed)

    val body = lerp(lerp(lerp(CalmBody, StormBody, full), RelievedBody, leave), Lit, flash * 0.65f)
    val shade = lerp(lerp(lerp(CalmShade, StormShade, full), CalmShade, leave), Lit, flash * 0.5f)
    val shape = Path()
    for (p in Puffs) shape.addOval(Rect(center = c + Offset(p.x, p.y) * (u * s), radius = p.r * u * s))
    val base = RoundRect(
        left = c.x - 38f * u * s, top = c.y + 2f * u * s, right = c.x + 37f * u * s, bottom = c.y + 17f * u * s,
        cornerRadius = CornerRadius(7.5f * u * s),
    )
    shape.addRoundRect(base)

    // Every puff shares one outline: stroke them all, then fill them all.
    val stroke = Stroke(width = outline * 2)
    for (p in Puffs) {
        drawCircle(Outline, p.r * u * s, c + Offset(p.x, p.y) * (u * s), style = stroke)
    }
    drawPath(Path().apply { addRoundRect(base) }, Outline, style = stroke)
    drawPath(shape, body)
    clipPath(shape) {
        drawRect(shade, topLeft = Offset(c.x - 45f * u * s, c.y + 10f * u * s), size = Size(90f * u * s, 10f * u * s))
        drawCircle(Color.White, radius = 5f * u * s, center = c + Offset(-10f, -15f) * (u * s), alpha = 0.3f)
    }

    if (anger > 0.6f && rain > 0.01f) drawSteam(c, s, u, time, ((anger - 0.6f) / 0.4f) * rain)
    drawFace(c, s, u, outline, anger, blink, leave, body)
}

private fun DrawScope.drawFace(
    c: Offset,
    s: Float,
    u: Float,
    outline: Float,
    anger: Float,
    blink: Float,
    leave: Float,
    lidColor: Color,
) {
    val k = u * s
    for (side in listOf(-1f, 1f)) {
        // "inner" points toward the other eye.
        drawEye(c + Offset(side * 8.5f, 5f) * k, inner = -side, k, outline, anger, blink, leave, lidColor)
        drawOval(
            Flush,
            topLeft = c + Offset(side * 14.5f - 3f, 9f) * k,
            size = Size(6f * k, 3f * k),
            alpha = 0.45f * anger,
        )
    }

    val mouth = c + Offset(0f, 13.5f) * k
    val teeth = ((anger - 0.45f) / 0.35f).coerceIn(0f, 1f)
    val grumble = (1f - teeth) * (1f - leave)
    if (grumble > 0.01f) {
        val wavy = Path().apply {
            moveTo(mouth.x - 4f * k, mouth.y + 0.5f * k)
            quadraticTo(mouth.x - 2f * k, mouth.y - 1.5f * k, mouth.x, mouth.y)
            quadraticTo(mouth.x + 2f * k, mouth.y + 1.5f * k, mouth.x + 4f * k, mouth.y - 0.5f * k)
        }
        drawPath(wavy, Outline, alpha = grumble, style = Stroke(outline * 1.1f, cap = StrokeCap.Round))
    }
    if (teeth > 0.01f) {
        // Gritted teeth.
        val grit = RoundRect(
            left = mouth.x - 6f * k, top = mouth.y - 2.5f * k, right = mouth.x + 6f * k, bottom = mouth.y + 2.5f * k,
            cornerRadius = CornerRadius(2f * k),
        )
        val gritPath = Path().apply { addRoundRect(grit) }
        val a = teeth
        drawPath(gritPath, Outline, alpha = a, style = Stroke(outline * 1.6f))
        drawPath(gritPath, Color.White, alpha = a)
        drawLine(Outline, Offset(grit.left, mouth.y), Offset(grit.right, mouth.y), strokeWidth = outline * 0.6f, alpha = a)
        for (x in listOf(-3f, 0f, 3f)) {
            drawLine(
                Outline,
                Offset(mouth.x + x * k, grit.top),
                Offset(mouth.x + x * k, grit.bottom),
                strokeWidth = outline * 0.6f,
                alpha = a,
            )
        }
    }
    if (leave > 0.01f) {
        val smile = Path().apply {
            moveTo(mouth.x - 4f * k, mouth.y - 1f * k)
            quadraticTo(mouth.x, mouth.y + 3.5f * k, mouth.x + 4f * k, mouth.y - 1f * k)
        }
        drawPath(smile, Outline, alpha = leave, style = Stroke(outline * 1.1f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawEye(
    center: Offset,
    inner: Float,
    k: Float,
    outline: Float,
    anger: Float,
    blink: Float,
    leave: Float,
    lidColor: Color,
) {
    val rx = 4f * k
    val ry = 4.4f * k
    val bounds = Rect(center.x - rx, center.y - ry, center.x + rx, center.y + ry)
    val open = 1f - leave
    if (open > 0.01f) {
        val eye = Path().apply { addOval(bounds) }
        clipPath(eye) {
            drawRect(Color.White, bounds.topLeft, bounds.size, alpha = open)
            drawCircle(Outline, radius = 2.2f * k * (1f - 0.3f * anger), center = center + Offset(0f, 0.6f * k), alpha = open)
            // Heavy lids, sloping down toward the nose: sulky when calm, fierce when full.
            val cover = max(lerp(0.5f, 0.3f, anger), blink).coerceIn(0f, 1f)
            val edge = bounds.top + bounds.height * cover
            val tilt = lerp(0.35f, 1.2f, anger) * bounds.height * 0.4f * (1f - cover)
            val innerX = if (inner > 0f) bounds.right else bounds.left
            val outerX = if (inner > 0f) bounds.left else bounds.right
            val lid = Path().apply {
                moveTo(bounds.left, bounds.top - bounds.height)
                lineTo(bounds.right, bounds.top - bounds.height)
                lineTo(bounds.right, if (inner > 0f) edge + tilt else edge - tilt)
                lineTo(bounds.left, if (inner > 0f) edge - tilt else edge + tilt)
                close()
            }
            drawPath(lid, lidColor, alpha = open)
            drawLine(Outline, Offset(outerX, edge - tilt), Offset(innerX, edge + tilt), strokeWidth = outline, alpha = open)
        }
        drawPath(eye, Outline, alpha = open, style = Stroke(outline))

        // A thick brow diving toward the nose.
        val browY = bounds.top - 2.5f * k
        drawLine(
            Outline,
            start = Offset(center.x - inner * rx * 1.1f, browY - 0.5f * k),
            end = Offset(center.x + inner * rx * 1.1f, browY + anger * 3f * k),
            strokeWidth = 2f * k,
            cap = StrokeCap.Round,
            alpha = open,
        )
    }
    if (leave > 0.01f) {
        val arc = Path().apply {
            moveTo(center.x - rx, center.y + 1f * k)
            quadraticTo(center.x, center.y - ry * 1.3f, center.x + rx, center.y + 1f * k)
        }
        drawPath(arc, Outline, alpha = leave, style = Stroke(outline * 1.2f, cap = StrokeCap.Round))
    }
}

/** Puffs of steam shooting out of both sides when the cloud is really angry. */
private fun DrawScope.drawSteam(c: Offset, s: Float, u: Float, time: Float, strength: Float) {
    val k = u * s
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until 3) {
            val p = (time * 1.3f + i / 3f) % 1f
            drawCircle(
                Color.White,
                radius = (2f + p * 3f) * k,
                center = c + Offset(side * (31f + p * 8f), -12f - p * 12f) * k,
                alpha = (1f - p) * 0.75f * strength,
            )
        }
    }
}

private const val Drops = 18
private const val FallSeconds = 0.42f
/** Wind: how far a drop drifts left for every unit it falls. */
private const val Slant = 0.18f

/** Slanted streaks falling to the ground, with a tiny splash each. */
private fun DrawScope.drawRain(c: Offset, s: Float, u: Float, rain: Float, time: Float) {
    val top = c.y + Underside * u * s
    val ground = size.height
    val fall = ground - top
    val streak = 5f * u
    for (i in 0 until Drops) {
        val x0 = c.x + (-32f + 64f * i / (Drops - 1) + sin(i * 12.9f) * 2f) * u * s
        val p = (time / FallSeconds + fract(sin(i * 7.7f) * 91.3f)) % 1f
        val head = Offset(x0 - p * fall * Slant, top + p * fall)
        if (p < 0.94f) {
            val tail = head - Offset(-Slant, 1f) * streak
            drawLine(Rain, tail, head, strokeWidth = 1.1f * u, cap = StrokeCap.Round, alpha = rain * 0.85f)
        } else {
            val q = (p - 0.94f) / 0.06f
            val g = Offset(head.x, ground)
            for (dir in listOf(-1f, 1f)) {
                drawLine(
                    Rain,
                    g + Offset(dir * 0.6f * u, -0.3f * u),
                    g + Offset(dir * (0.8f + q * 1.6f) * u, -(0.6f + q * 1.4f) * u),
                    strokeWidth = 0.8f * u,
                    cap = StrokeCap.Round,
                    alpha = rain * (1f - q),
                )
            }
        }
    }
}

/** On, off, on again, then a fade: how a bolt flickers over its life. */
private fun boltFlicker(bolt: Float) = when {
    bolt <= 0f -> 0f
    bolt < 0.08f -> 1f
    bolt < 0.16f -> 0.25f
    bolt < 0.3f -> 1f
    else -> (1f - (bolt - 0.3f) / 0.7f).coerceIn(0f, 1f)
}

/** A jagged bolt from under the cloud to the ground, alternating sides so it never hits the middle. */
private fun DrawScope.drawBolt(c: Offset, s: Float, u: Float, outline: Float, bolt: Float, seed: Int) {
    val a = boltFlicker(bolt)
    if (a <= 0f) return
    val rng = Random(seed * 7919 + 13)
    val side = if (seed % 2 == 0) -1f else 1f
    val start = c + Offset(side * (8f + rng.nextFloat() * 10f), Underside - 2f) * (u * s)
    val end = Offset(CenterX * u + side * (30f + rng.nextFloat() * 12f) * u, size.height)
    val segments = 7
    val points = List(segments + 1) { i ->
        val p = lerp(start, end, i / segments.toFloat())
        if (i == 0 || i == segments) p else p + Offset((rng.nextFloat() * 2f - 1f) * 5f * u, 0f)
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (p in points.drop(1)) lineTo(p.x, p.y)
        // A short fork off the middle.
        var b = points[3]
        moveTo(b.x, b.y)
        repeat(3) {
            b += Offset(side * (3f + rng.nextFloat() * 3f) * u, (5f + rng.nextFloat() * 4f) * u)
            lineTo(b.x, b.y)
        }
    }
    val join = StrokeJoin.Round
    // Wide glow, dark outline, yellow body, white-hot core.
    drawPath(path, BoltGlow, alpha = 0.55f * a, style = Stroke(10f * u, cap = StrokeCap.Round, join = join))
    drawPath(path, Outline, alpha = a, style = Stroke(3f * u + outline * 2f, cap = StrokeCap.Round, join = join))
    drawPath(path, BoltCore, alpha = a, style = Stroke(3f * u, cap = StrokeCap.Round, join = join))
    drawPath(path, BoltHot, alpha = a, style = Stroke(1.1f * u, cap = StrokeCap.Round, join = join))

    // Impact: a glow on the ground and sparks flying out.
    drawCircle(
        Brush.radialGradient(listOf(BoltGlow.copy(alpha = 0.6f * a), Color.Transparent), center = end, radius = 14f * u),
        radius = 14f * u,
        center = end,
    )
    val reach = (2f + bolt * 7f) * u
    for (i in 0 until 6) {
        val angle = (-170f + i * 32f) * PI.toFloat() / 180f
        val dir = Offset(cos(angle), sin(angle))
        drawLine(BoltCore, end + dir * (reach * 0.5f), end + dir * reach, strokeWidth = 0.9f * u, cap = StrokeCap.Round, alpha = a)
    }
}

private fun fract(x: Float) = x - kotlin.math.floor(x)

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction
