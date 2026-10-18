package com.zobaer53.composeanimations.characters.sprout

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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val Outline = Color(0xFF3B2418)
private val Pot = Color(0xFFE07A4F)
private val PotShade = Color(0xFFC25E36)
private val Rim = Color(0xFFF09366)
private val SoilDry = Color(0xFFB8916A)
private val SoilWet = Color(0xFF5B3A26)
private val LeafDry = Color(0xFFB5A85C)
private val LeafLush = Color(0xFF5DBB63)
private val StemDry = Color(0xFF9E9550)
private val StemLush = Color(0xFF47A452)
private val Petal = Color(0xFFFF8FAB)
private val FlowerCenter = Color(0xFFFFC93C)
private val Sparkle = Color(0xFFFFC93C)
private val MouthInside = Color(0xFF6B1F2A)
private val Tongue = Color(0xFFFF7E8F)
private val Blush = Color(0xFFFF6F7F)

// All geometry is in "sprout units": 1 unit = 1% of the canvas height, so Sprout scales with it.
private const val Ground = 95f
private const val SoilY = 50.5f
private const val SoilRadius = 18f
private const val EyeY = 70f
private const val MouthY = 79f
/** Where the flower sits when the plant stands up straight. */
internal const val FlowerY = 22f
/** How high, in units, Sprout jumps when it cheers. */
internal const val HopHeight = 14f

/**
 * Draws Sprout centred horizontally, feet on the bottom edge. Needs a canvas at least ~60% as
 * wide as it is tall.
 *
 * @param hydration 0 = wilted, 1 = standing tall; may overshoot a little while springing.
 * @param sway the sway cycle position, -1..1.
 * @param breath the breathing cycle position, -1..1.
 * @param blink 0 = eyes open, 1 = mid-blink.
 * @param glance horizontal look, -1 (left) .. 1 (right).
 * @param tap how far the right foot is lifted, 0..1.
 * @param hop height above the ground, in units.
 * @param squash > 0 stretched tall and thin, < 0 squashed flat and wide.
 * @param burst progress of the petal burst, 0..1; 0 draws nothing.
 */
internal fun DrawScope.drawSprout(
    hydration: Float,
    face: SproutExpression,
    sway: Float,
    breath: Float,
    blink: Float,
    glance: Float,
    tap: Float,
    hop: Float,
    squash: Float,
    burst: Float,
) {
    val u = size.height / 100f
    val outline = 1.6f * u
    val cx = center.x
    val wet = hydration.coerceIn(0f, 1f)

    // Ground shadow, shrinking while Sprout is in the air.
    val shadow = 1f - (hop / (HopHeight * 2.5f)).coerceIn(0f, 0.6f)
    drawOval(
        color = Color.Black.copy(alpha = 0.1f),
        topLeft = Offset(cx - 21f * u * shadow, (Ground - 1.5f) * u),
        size = Size(42f * u * shadow, 4.5f * u * shadow),
    )

    val stretchY = 1f + squash + breath * 0.012f
    val stretchX = 1f - squash * 0.6f - breath * 0.008f
    withTransform({
        translate(top = -hop * u)
        scale(stretchX, stretchY, pivot = Offset(cx, Ground * u))
    }) {
        drawFeet(cx, u, outline, tap)
        drawPlant(cx, u, outline, hydration, wet, face, sway, cheer = (hop / HopHeight).coerceIn(0f, 1f))

        // A mound of soil peeking over the rim; it darkens as it gets wet.
        val soil = Path().apply {
            addOval(Rect(cx - SoilRadius * u, (SoilY - 3.4f) * u, cx + SoilRadius * u, (SoilY + 3.4f) * u))
        }
        drawOutlined(soil, lerp(SoilDry, SoilWet, wet), outline)

        drawPot(cx, u, outline)
        drawFace(cx, u, outline, face, blink, glance * face.restless)
    }

    if (burst > 0f && burst < 1f) drawBurst(Offset(cx, (FlowerY - hop) * u), u, burst)
}

private fun DrawScope.drawFeet(cx: Float, u: Float, outline: Float, tap: Float) {
    val feet = Path().apply {
        addOval(Rect(cx - 13.5f * u, 89f * u, cx - 2.5f * u, 95.5f * u))
        addOval(Rect(cx + 2.5f * u, (89f - tap * 3f) * u, cx + 13.5f * u, (95.5f - tap * 3f) * u))
    }
    drawOutlined(feet, PotShade, outline)
}

private fun DrawScope.drawPlant(
    cx: Float,
    u: Float,
    outline: Float,
    hydration: Float,
    wet: Float,
    face: SproutExpression,
    sway: Float,
    cheer: Float,
) {
    // A wilted stem rises, then flops over to the right with the bud hanging down.
    val droop = 1f - hydration
    val base = Offset(cx, 51f * u)
    val c1 = lerp(Offset(cx, 42f * u), Offset(cx - 1f * u, 40f * u), droop)
    val c2 = lerp(Offset(cx, 31f * u), Offset(cx + 5f * u, 23f * u), droop)
    val tip = lerp(Offset(cx, FlowerY * u), Offset(cx + 14f * u, 31f * u), droop)
    val leafColor = lerp(LeafDry, LeafLush, wet)
    val stemColor = lerp(StemDry, StemLush, wet)

    rotate(degrees = sway * face.swayAmplitude, pivot = base) {
        // Leaves work as arms: up and out when perky, hanging when wilted, flung up mid-jump.
        val attach = cubicPoint(base, c1, c2, tip, 0.42f)
        // Wilted leaves only sag to ~30°; any lower and they hide behind the rim.
        val leafAngle = lerp(-32f, 30f, droop) - cheer * 40f
        val flutter = sway * 5f
        drawLeaf(attach, leafAngle + flutter, leafColor, u, outline)
        drawLeaf(attach, 180f - leafAngle + flutter, leafColor, u, outline)

        val stem = Path().apply {
            moveTo(base.x, base.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, tip.x, tip.y)
        }
        drawPath(stem, Outline, style = Stroke(width = 3f * u + outline * 2, cap = StrokeCap.Round))
        drawPath(stem, stemColor, style = Stroke(width = 3f * u, cap = StrokeCap.Round))

        val heading = atan2(tip.y - c2.y, tip.x - c2.x) * 180f / PI.toFloat()
        if (face.bloom < 0.98f) drawBud(tip, heading, 1f - face.bloom, stemColor, u, outline)
        if (face.bloom > 0.02f) drawFlower(tip, face.bloom, sway, u, outline)
    }
}

/** An almond-shaped leaf growing from [attach] in the direction of [degrees] (0 = right, y down). */
private fun DrawScope.drawLeaf(attach: Offset, degrees: Float, color: Color, u: Float, outline: Float) {
    rotate(degrees, pivot = attach) {
        val length = 15f * u
        val width = 6.5f * u
        val leaf = Path().apply {
            moveTo(attach.x, attach.y)
            quadraticTo(attach.x + length * 0.45f, attach.y - width, attach.x + length, attach.y)
            quadraticTo(attach.x + length * 0.55f, attach.y + width, attach.x, attach.y)
            close()
        }
        drawOutlined(leaf, color, outline)
        drawLine(
            Color.White.copy(alpha = 0.35f),
            Offset(attach.x + length * 0.2f, attach.y),
            Offset(attach.x + length * 0.75f, attach.y - 0.4f * u),
            strokeWidth = 0.9f * u,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawBud(tip: Offset, heading: Float, scale: Float, color: Color, u: Float, outline: Float) {
    rotate(heading, pivot = tip) {
        val s = scale * u
        val bud = Path().apply {
            moveTo(tip.x - 1f * s, tip.y)
            cubicTo(tip.x - 1f * s, tip.y - 4.5f * s, tip.x + 4f * s, tip.y - 4f * s, tip.x + 8f * s, tip.y)
            cubicTo(tip.x + 4f * s, tip.y + 4f * s, tip.x - 1f * s, tip.y + 4.5f * s, tip.x - 1f * s, tip.y)
            close()
        }
        drawOutlined(bud, color, outline * scale)
        // A peek of pink at the bud's tip hints at the flower inside.
        drawCircle(Petal, radius = 1.6f * s, center = Offset(tip.x + 6f * s, tip.y))
    }
}

/** Five petals that unfurl with a quarter turn as [bloom] goes from 0 to 1. */
private fun DrawScope.drawFlower(tip: Offset, bloom: Float, sway: Float, u: Float, outline: Float) {
    val s = bloom * u
    val spin = (1f - bloom) * -90f + sway * 6f
    val petals = Path()
    for (i in 0 until 5) {
        val angle = ((i * 72f + spin - 90f) * PI / 180f).toFloat()
        val d = Offset(cos(angle), sin(angle))
        val n = Offset(-d.y, d.x)
        val start = tip + d * (1f * s)
        val end = tip + d * (10f * s)
        petals.moveTo(start.x, start.y)
        val a = tip + d * (3f * s) + n * (5f * s)
        val b = tip + d * (10.5f * s) + n * (4f * s)
        petals.cubicTo(a.x, a.y, b.x, b.y, end.x, end.y)
        val c = tip + d * (10.5f * s) - n * (4f * s)
        val e = tip + d * (3f * s) - n * (5f * s)
        petals.cubicTo(c.x, c.y, e.x, e.y, start.x, start.y)
        petals.close()
    }
    drawOutlined(petals, Petal, outline * bloom)
    val middle = Path().apply { addOval(Rect(center = tip, radius = 3.6f * s)) }
    drawOutlined(middle, FlowerCenter, outline * bloom)
    drawCircle(Color.White.copy(alpha = 0.6f), radius = 1f * s, center = tip + Offset(-1.2f * s, -1.2f * s))
}

private fun DrawScope.drawPot(cx: Float, u: Float, outline: Float) {
    // Slightly bulging sides, narrower at the bottom.
    val body = Path().apply {
        moveTo(cx - 19f * u, 58f * u)
        lineTo(cx + 19f * u, 58f * u)
        quadraticTo(cx + 18.5f * u, 76f * u, cx + 15.5f * u, 87f * u)
        quadraticTo(cx + 14.5f * u, 91.5f * u, cx + 11f * u, 91.5f * u)
        lineTo(cx - 11f * u, 91.5f * u)
        quadraticTo(cx - 14.5f * u, 91.5f * u, cx - 15.5f * u, 87f * u)
        quadraticTo(cx - 18.5f * u, 76f * u, cx - 19f * u, 58f * u)
        close()
    }
    val rim = Path().apply {
        addRoundRect(
            RoundRect(
                left = cx - 22f * u, top = 50f * u, right = cx + 22f * u, bottom = 60f * u,
                cornerRadius = CornerRadius(3.5f * u),
            ),
        )
    }
    // Body and rim share one outline: stroke both, then fill both.
    val outlineStroke = Stroke(width = outline * 2, join = StrokeJoin.Round)
    drawPath(body, Outline, style = outlineStroke)
    drawPath(rim, Outline, style = outlineStroke)
    drawPath(body, Brush.verticalGradient(0f to Pot, 0.6f to Pot, 1f to PotShade, startY = 58f * u, endY = 92f * u))
    drawPath(rim, Rim)
    drawRoundRect(
        Color.White.copy(alpha = 0.3f),
        topLeft = Offset(cx - 18f * u, 52f * u),
        size = Size(13f * u, 2f * u),
        cornerRadius = CornerRadius(1f * u),
    )
    drawLine(
        Color.White.copy(alpha = 0.2f),
        Offset(cx - 14.5f * u, 64f * u),
        Offset(cx - 13.5f * u, 80f * u),
        strokeWidth = 2.2f * u,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawFace(cx: Float, u: Float, outline: Float, face: SproutExpression, blink: Float, gazeX: Float) {
    // "inner" points toward the other eye.
    drawEye(Offset(cx - 7.5f * u, EyeY * u), inner = 1f, face, blink, gazeX, u, outline)
    drawEye(Offset(cx + 7.5f * u, EyeY * u), inner = -1f, face, blink, gazeX, u, outline)

    if (face.blush > 0.01f) {
        for (side in listOf(-1f, 1f)) {
            drawOval(
                Blush,
                alpha = face.blush.coerceIn(0f, 1f) * 0.5f,
                topLeft = Offset(cx + side * 13f * u - 3.2f * u, 75.5f * u),
                size = Size(6.4f * u, 3.4f * u),
            )
        }
    }
    drawMouth(Offset(cx, MouthY * u), face, u, outline)
}

private fun DrawScope.drawEye(
    center: Offset,
    inner: Float,
    face: SproutExpression,
    blink: Float,
    gazeX: Float,
    u: Float,
    outline: Float,
) {
    val radiusX = 4.3f * u
    val radiusY = 5.2f * u
    val bounds = Rect(center.x - radiusX, center.y - radiusY, center.x + radiusX, center.y + radiusY)
    val openAlpha = (1f - face.happyEyes).coerceIn(0f, 1f)

    if (openAlpha > 0f) {
        val eye = Path().apply { addOval(bounds) }
        clipPath(eye) {
            drawRect(Color.White, topLeft = bounds.topLeft, size = bounds.size, alpha = openAlpha)
            val pupilRadius = 2.6f * u * face.pupilScale
            val pupil = center + Offset((face.gazeX + gazeX).coerceIn(-1f, 1f) * 1.6f * u, face.gazeY * 2f * u)
            drawCircle(Outline, pupilRadius, pupil, alpha = openAlpha)
            drawCircle(
                Color.White,
                radius = pupilRadius * 0.35f,
                center = pupil + Offset(-pupilRadius * 0.35f, -pupilRadius * 0.4f),
                alpha = openAlpha,
            )

            // Upper lid. Worried lids droop at the outer corner; looking up lifts them so the
            // pupils never hide under a half-shut lid.
            val lookUp = (-face.gazeY).coerceIn(0f, 1f)
            val cover = max(face.lidCover * (1f - lookUp * 0.8f), blink).coerceIn(0f, 1f)
            if (cover > 0.01f) {
                val edge = bounds.top + bounds.height * cover
                val tilt = face.browTilt * bounds.height * 0.3f * (1f - cover)
                val leftY = edge + tilt * inner
                val rightY = edge - tilt * inner
                val lid = Path().apply {
                    moveTo(bounds.left, bounds.top - bounds.height)
                    lineTo(bounds.right, bounds.top - bounds.height)
                    lineTo(bounds.right, rightY)
                    lineTo(bounds.left, leftY)
                    close()
                }
                drawPath(lid, Pot, alpha = openAlpha)
                drawLine(
                    Outline,
                    Offset(bounds.left, leftY),
                    Offset(bounds.right, rightY),
                    strokeWidth = outline * 1.2f,
                    alpha = openAlpha,
                )
            }
        }
        // A nearly shut eye swaps its outline for a lash line, so a blink reads as closed eyes
        // instead of two empty circles.
        val cover = max(face.lidCover, blink).coerceIn(0f, 1f)
        val shut = ((cover - 0.75f) / 0.25f).coerceIn(0f, 1f)
        drawPath(eye, Outline, alpha = openAlpha * (1f - shut), style = Stroke(width = outline * 1.1f))
        if (shut > 0f) {
            val lash = Path().apply {
                moveTo(center.x - radiusX, center.y + 0.5f * u)
                quadraticTo(center.x, center.y + radiusY * 0.9f, center.x + radiusX, center.y + 0.5f * u)
            }
            drawPath(lash, Outline, alpha = openAlpha * shut, style = Stroke(width = outline * 1.3f, cap = StrokeCap.Round))
        }
    }

    if (face.happyEyes > 0f) {
        val arc = Path().apply {
            moveTo(center.x - radiusX, center.y + 1f * u)
            quadraticTo(center.x, center.y - radiusY * 1.3f, center.x + radiusX, center.y + 1f * u)
        }
        drawPath(
            arc, Outline,
            alpha = face.happyEyes.coerceIn(0f, 1f),
            style = Stroke(width = outline * 1.3f, cap = StrokeCap.Round),
        )
    }

    // Brow: the inner end rises when worried.
    val browY = bounds.top - 2.6f * u - face.browLift * 2.6f * u
    val innerX = center.x + inner * radiusX * 0.9f
    val outerX = center.x - inner * radiusX * 0.9f
    drawLine(
        Outline,
        start = Offset(outerX, browY + face.browTilt * 0.9f * u),
        end = Offset(innerX, browY - face.browTilt * 1.8f * u),
        strokeWidth = 1.8f * u,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMouth(center: Offset, face: SproutExpression, u: Float, outline: Float) {
    val grin = face.grin
    if (grin > 0.02f) {
        val left = Offset(center.x - 5.5f * u * grin, center.y - 1.5f * u)
        val right = Offset(center.x + 5.5f * u * grin, center.y - 1.5f * u)
        val mouth = Path().apply {
            moveTo(left.x, left.y)
            quadraticTo(center.x, center.y - 0.5f * u, right.x, right.y)
            quadraticTo(center.x, center.y + 9f * u * grin, left.x, left.y)
            close()
        }
        drawOutlined(mouth, MouthInside, outline * 0.8f)
        clipPath(mouth) {
            drawCircle(Tongue, radius = 3.4f * u * grin, center = Offset(center.x, center.y + 6f * u * grin))
        }
    }

    val open = face.mouthOpen
    if (open > 0.01f) {
        val alpha = (open * 3f).coerceAtMost(1f)
        val o = Path().apply {
            addOval(Rect(center.x - 2.2f * u * open, center.y - 2.4f * u * open, center.x + 2.2f * u * open, center.y + 3.2f * u * open))
        }
        drawPath(o, Outline, alpha = alpha, style = Stroke(width = outline * 1.3f))
        drawPath(o, MouthInside, alpha = alpha)
    }

    // The resting line fades out as either open mouth takes over.
    val lineAlpha = (1f - max(open, grin) * 2f).coerceIn(0f, 1f)
    if (lineAlpha > 0f) {
        val line = Path().apply {
            moveTo(center.x - 4.5f * u, center.y)
            quadraticTo(center.x, center.y + face.smile * 4f * u, center.x + 4.5f * u, center.y)
        }
        drawPath(line, Outline, alpha = lineAlpha, style = Stroke(width = outline * 1.2f, cap = StrokeCap.Round))
    }
}

/** Petals, leaves and sparkles thrown out of the flower when Sprout cheers, falling as they fade. */
private fun DrawScope.drawBurst(origin: Offset, u: Float, burst: Float) {
    val travel = 1f - (1f - burst) * (1f - burst)
    val alpha = (1f - burst * burst).coerceIn(0f, 1f)
    for (i in 0 until 12) {
        val angle = (i * 30f + sin(i * 2.3f) * 12f) * PI.toFloat() / 180f
        val distance = (16f + (i % 3) * 7f) * u * travel
        val p = origin + Offset(cos(angle), sin(angle)) * distance + Offset(0f, 10f * u * burst * burst)
        val spin = i * 40f + burst * 220f
        when (i % 3) {
            0 -> rotate(spin, pivot = p) {
                drawOval(Petal, topLeft = p - Offset(2f * u, 1.2f * u), size = Size(4f * u, 2.4f * u), alpha = alpha)
            }
            1 -> rotate(spin, pivot = p) {
                drawOval(LeafLush, topLeft = p - Offset(1.8f * u, 1f * u), size = Size(3.6f * u, 2f * u), alpha = alpha)
            }
            else -> {
                val r = 2.4f * u * (1f - burst * 0.4f)
                val star = Path().apply {
                    moveTo(p.x, p.y - r)
                    quadraticTo(p.x, p.y, p.x + r, p.y)
                    quadraticTo(p.x, p.y, p.x, p.y + r)
                    quadraticTo(p.x, p.y, p.x - r, p.y)
                    quadraticTo(p.x, p.y, p.x, p.y - r)
                    close()
                }
                drawPath(star, Sparkle, alpha = alpha)
            }
        }
    }
}

private fun DrawScope.drawOutlined(path: Path, fill: Color, outline: Float) {
    drawPath(path, Outline, style = Stroke(width = outline * 2, join = StrokeJoin.Round))
    drawPath(path, fill)
}

private fun cubicPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val mt = 1f - t
    return p0 * (mt * mt * mt) + p1 * (3f * mt * mt * t) + p2 * (3f * mt * t * t) + p3 * (t * t * t)
}

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction
