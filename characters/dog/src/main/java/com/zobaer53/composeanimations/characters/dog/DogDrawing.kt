package com.zobaer53.composeanimations.characters.dog

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private val Outline = Color(0xFF4A2410)
private val Fur = Color(0xFFE07B2C)
private val FurLight = Color(0xFFF4A65E)
private val FurShade = Color(0xFFBF5E1C)
private val FarLegFur = Color(0xFFB4561A)
private val EarFur = Color(0xFF6B3313)
private val Nose = Color(0xFF2A1810)
private val MouthInside = Color(0xFF5C1D1D)
private val Tongue = Color(0xFFE85D75)
private val TongueShade = Color(0xFFB93A55)
private val Blush = Color(0xFFFF8A80)

// All geometry is in "dog units": 1 unit = 1% of the canvas height, so the dog scales with it.
private const val BaseBodyLength = 80f
// How far the nose reaches left of the body and the tail right of it.
private const val FrontReach = 34f
private const val BackReach = 26f
private const val BodyTop = 40f
private const val BodyBottom = 78f
private val SideMargin = 16.dp

/**
 * Draws the dog facing left, centred in the canvas.
 *
 * @param stretch 0 for the short dog, 1 for the longest one that still fits the canvas width.
 * @param targetStretch where [stretch] is heading; the gap between the two makes the ear lag.
 * @param wag the wag cycle position, -1..1.
 * @param blink 0 = eyes open, 1 = mid-blink.
 */
internal fun DrawScope.drawDog(
    stretch: Float,
    targetStretch: Float,
    face: DogExpression,
    wag: Float,
    blink: Float,
) {
    val u = size.height / 100f
    val outline = 2.2f * u
    val maxExtra = (size.width - (BaseBodyLength + FrontReach + BackReach) * u - SideMargin.toPx() * 2)
        .coerceAtLeast(0f)
    val bodyLength = BaseBodyLength * u + maxExtra * stretch
    val bodyLeft = center.x - bodyLength / 2 + (FrontReach - BackReach) / 2 * u
    val bodyRight = bodyLeft + bodyLength
    val head = Offset(bodyLeft + 6f * u, 36f * u)

    // Ground shadow.
    drawOval(
        color = Color.Black.copy(alpha = 0.08f),
        topLeft = Offset(bodyLeft - 24f * u, 90f * u),
        size = Size(bodyLength + 52f * u, 7f * u),
    )

    // Tail, behind the body. Negative degrees swing it up.
    val tailBase = Offset(bodyRight - 6f * u, 50f * u)
    rotate(degrees = -(face.tailLift + wag * face.wagAmplitude), pivot = tailBase) {
        val tail = Path().apply {
            moveTo(tailBase.x, tailBase.y - 4.5f * u)
            quadraticTo(tailBase.x + 16f * u, tailBase.y - 6f * u, tailBase.x + 31f * u, tailBase.y + 3f * u)
            quadraticTo(tailBase.x + 15f * u, tailBase.y + 3f * u, tailBase.x, tailBase.y + 4.5f * u)
            close()
        }
        drawOutlined(tail, Fur, outline)
    }

    // Far-side legs peek out from behind the body in a darker shade.
    val farLegs = Path().apply {
        addLeg(bodyLeft + 22f * u, bottom = 90f * u, u = u)
        addLeg(bodyRight - 11f * u, bottom = 90f * u, u = u)
    }
    drawOutlined(farLegs, FarLegFur, outline)

    // Near legs, body, head and muzzle share one outline: stroke them all first, then fill them
    // all, so the fills hide the outline wherever the shapes overlap.
    val nearLegs = Path().apply {
        addLeg(bodyLeft + 12f * u, bottom = 92f * u, u = u)
        addLeg(bodyRight - 21f * u, bottom = 92f * u, u = u)
    }
    val body = Path().apply {
        addRoundRect(
            RoundRect(
                left = bodyLeft, top = BodyTop * u, right = bodyRight, bottom = BodyBottom * u,
                cornerRadius = CornerRadius((BodyBottom - BodyTop) / 2 * u),
            ),
        )
    }
    val skull = Path().apply { addOval(Rect(center = head, radius = 21f * u)) }
    val muzzle = Path().apply {
        addOval(Rect(offset = Offset(head.x - 37f * u, head.y - 0.5f * u), size = Size(32f * u, 19f * u)))
    }
    val outlineStroke = Stroke(width = outline * 2, join = StrokeJoin.Round)
    for (part in listOf(nearLegs, body, skull, muzzle)) drawPath(part, Outline, style = outlineStroke)
    drawPath(nearLegs, Fur)
    drawPath(
        body,
        Brush.verticalGradient(
            0f to Fur, 0.55f to Fur, 1f to FurShade,
            startY = BodyTop * u, endY = BodyBottom * u,
        ),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(bodyLeft + 18f * u, 44f * u),
        size = Size((bodyLength - 36f * u).coerceAtLeast(0f), 4f * u),
        cornerRadius = CornerRadius(2f * u),
    )
    drawPath(skull, Fur)
    drawPath(muzzle, FurLight)

    // The ear trails behind the head while the body is still catching up with its target length.
    val lag = (targetStretch - stretch) * maxExtra / u
    val earPivot = Offset(head.x + 9f * u, head.y - 14f * u)
    rotate(degrees = -(lag * 1.4f).coerceIn(-30f, 30f), pivot = earPivot) {
        val ear = Path().apply {
            moveTo(earPivot.x - 6f * u, earPivot.y)
            cubicTo(
                earPivot.x - 12f * u, earPivot.y + 16f * u,
                earPivot.x - 6f * u, earPivot.y + 36f * u,
                earPivot.x + 4f * u, earPivot.y + 35f * u,
            )
            cubicTo(
                earPivot.x + 13f * u, earPivot.y + 34f * u,
                earPivot.x + 12f * u, earPivot.y + 12f * u,
                earPivot.x + 7f * u, earPivot.y - 2f * u,
            )
            close()
        }
        drawOutlined(ear, EarFur, outline)
    }

    // The far eye sits nearer the nose and is a touch smaller; "inner" points at the other eye.
    drawEye(Offset(head.x - 17f * u, head.y - 3f * u), 4.6f * u, 6f * u, inner = 1f, face, blink, u, outline)
    drawEye(Offset(head.x - 4f * u, head.y - 4f * u), 5.6f * u, 7.2f * u, inner = -1f, face, blink, u, outline)

    drawOval(
        color = Blush,
        alpha = ((face.smile - 0.3f) / 0.7f).coerceIn(0f, 1f) * 0.45f,
        topLeft = Offset(head.x - 9f * u, head.y + 7f * u),
        size = Size(9f * u, 4.5f * u),
    )

    drawOval(Nose, topLeft = Offset(head.x - 40f * u, head.y + 0.5f * u), size = Size(11f * u, 8.5f * u))
    drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(head.x - 37.5f * u, head.y + 2f * u),
        size = Size(4f * u, 2.2f * u),
    )

    drawMouth(Offset(head.x - 23f * u, head.y + 13f * u), face, u, outline)
}

private fun Path.addLeg(x: Float, bottom: Float, u: Float) {
    addRoundRect(
        RoundRect(
            left = x - 5.5f * u, top = 62f * u, right = x + 5.5f * u, bottom = bottom,
            cornerRadius = CornerRadius(5.5f * u),
        ),
    )
}

private fun ovalAround(center: Offset, radiusX: Float, radiusY: Float) =
    Rect(center.x - radiusX, center.y - radiusY, center.x + radiusX, center.y + radiusY)

private fun DrawScope.drawOutlined(path: Path, fill: Color, outline: Float) {
    drawPath(path, Outline, style = Stroke(width = outline * 2, join = StrokeJoin.Round))
    drawPath(path, fill)
}

private fun DrawScope.drawEye(
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    inner: Float,
    face: DogExpression,
    blink: Float,
    u: Float,
    outline: Float,
) {
    // Happy eyes cross-fade the round eye into a "^" arc.
    val openAlpha = (1f - face.happyEyes).coerceIn(0f, 1f)
    val grow = 1f + face.browLift * 0.15f
    val bounds = ovalAround(center, radiusX * grow, radiusY * grow)
    val eye = Path().apply { addOval(bounds) }

    if (openAlpha > 0f) {
        clipPath(eye) {
            drawRect(Color.White, topLeft = bounds.topLeft, size = bounds.size, alpha = openAlpha)

            val pupilRadius = min(bounds.width, bounds.height) / 2 * 0.62f * face.pupilScale
            val pupil = Offset(center.x - bounds.width * 0.12f, center.y + bounds.height * 0.05f)
            drawCircle(Outline, pupilRadius, pupil, alpha = openAlpha)
            drawCircle(
                Color.White,
                radius = pupilRadius * 0.32f,
                center = pupil + Offset(-pupilRadius * 0.35f, -pupilRadius * 0.35f),
                alpha = openAlpha,
            )

            // Upper lid: a fur-coloured slab whose bottom edge slopes with lidSlant.
            val cover = max(face.lidCover, blink).coerceIn(0f, 1f)
            if (cover > 0.01f) {
                val edge = bounds.top + bounds.height * cover
                val tilt = face.lidSlant * bounds.height * 0.4f * (1f - cover) * inner
                val leftY = edge - tilt
                val rightY = edge + tilt
                val lid = Path().apply {
                    moveTo(bounds.left, bounds.top - bounds.height)
                    lineTo(bounds.right, bounds.top - bounds.height)
                    lineTo(bounds.right, rightY)
                    lineTo(bounds.left, leftY)
                    close()
                }
                drawPath(lid, Fur, alpha = openAlpha)
                drawLine(
                    Outline,
                    Offset(bounds.left, leftY),
                    Offset(bounds.right, rightY),
                    strokeWidth = outline * 1.4f,
                    alpha = openAlpha,
                )
            }
        }
        drawPath(eye, Outline, alpha = openAlpha, style = Stroke(width = outline * 1.2f))
    }

    if (face.happyEyes > 0f) {
        val arc = Path().apply {
            moveTo(center.x - radiusX, center.y + 1f * u)
            quadraticTo(center.x, center.y - radiusY * 1.2f, center.x + radiusX, center.y + 1f * u)
        }
        drawPath(
            arc, Outline,
            alpha = face.happyEyes.coerceIn(0f, 1f),
            style = Stroke(width = outline * 1.5f, cap = StrokeCap.Round),
        )
    }

    // Brow, sloping the same way as the lid.
    val browY = bounds.top - 3f * u - face.browLift * 4f * u
    val browTilt = face.lidSlant * 3f * u * inner
    drawLine(
        Outline,
        start = Offset(center.x - radiusX * 1.1f, browY - browTilt),
        end = Offset(center.x + radiusX * 1.1f, browY + browTilt),
        strokeWidth = 2.4f * u,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMouth(center: Offset, face: DogExpression, u: Float, outline: Float) {
    val left = Offset(center.x - 8f * u, center.y - 1f * u)
    val right = Offset(center.x + 7f * u, center.y - 2f * u)
    // The upper lip's control point drops for a grin and rises for a frown.
    val lip = Offset(center.x - 1f * u, center.y + face.smile * 7f * u)

    // Sticking the tongue out opens the grin: a jaw curve drops below the upper lip and the
    // tongue hangs out over it.
    if (face.tongue > 0.01f) {
        val jaw = lip + Offset(0f, 6f * u * face.tongue)
        val mouth = Path().apply {
            moveTo(left.x, left.y)
            quadraticTo(lip.x, lip.y, right.x, right.y)
            quadraticTo(jaw.x, jaw.y, left.x, left.y)
            close()
        }
        drawOutlined(mouth, MouthInside, outline * 0.7f)

        val lipMid = quadraticMidpoint(left, lip, right)
        val tongueTop = lipMid.y + 1f * u
        val tongueBottom = quadraticMidpoint(left, jaw, right).y + 9f * u * face.tongue
        if (tongueBottom > tongueTop + 1f * u) {
            val tongue = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = lipMid.x - 6f * u, top = tongueTop, right = lipMid.x + 2f * u, bottom = tongueBottom,
                        bottomLeftCornerRadius = CornerRadius(4f * u),
                        bottomRightCornerRadius = CornerRadius(4f * u),
                    ),
                )
            }
            drawOutlined(tongue, Tongue, outline * 0.7f)
            if (tongueBottom - tongueTop > 6f * u) {
                drawLine(
                    TongueShade,
                    Offset(lipMid.x - 2f * u, tongueTop + 2f * u),
                    Offset(lipMid.x - 2f * u, tongueBottom - 3f * u),
                    strokeWidth = 1.2f * u,
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    if (face.mouthOpen > 0.01f) {
        val open = face.mouthOpen
        val o = Path().apply {
            addOval(ovalAround(Offset(center.x - 1f * u, center.y + 2f * u), 3.5f * u * open, 4.5f * u * open))
        }
        drawPath(o, Outline, alpha = (open * 3f).coerceAtMost(1f), style = Stroke(width = outline * 1.4f))
        drawPath(o, MouthInside, alpha = (open * 3f).coerceAtMost(1f))
    }

    // Upper lip goes on top of the tongue. It fades out as the "O" opens, fully gone by half
    // open so it never cuts across a small "O".
    val upperLip = Path().apply {
        moveTo(left.x, left.y)
        quadraticTo(lip.x, lip.y, right.x, right.y)
    }
    drawPath(
        upperLip, Outline,
        alpha = (1f - face.mouthOpen * 2f).coerceIn(0f, 1f),
        style = Stroke(width = outline * 1.3f, cap = StrokeCap.Round),
    )
}

private fun quadraticMidpoint(start: Offset, control: Offset, end: Offset) = (start + control * 2f + end) / 4f
