package com.zobaer53.composeanimations.cover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zobaer53.composeanimations.characters.dog.DogExpression
import com.zobaer53.composeanimations.characters.dog.StretchyDog
import com.zobaer53.composeanimations.characters.sprout.Sprout
import com.zobaer53.composeanimations.characters.sprout.SproutExpression
import com.zobaer53.composeanimations.characters.stormcloud.StormCloud

private val Ink = Color(0xFF1E1B2E)
private val Peach = Color(0xFFFFF1DC)
private val Blush = Color(0xFFFFDDE8)
private val Lavender = Color(0xFFDCD6FF)
private val TitleGradient = Brush.linearGradient(listOf(Color(0xFFFF7A3D), Color(0xFFFF4F8B), Color(0xFF7B5CFF)))
private val Sparkle = Color(0xFFFFB020)
private val Swoosh = Color(0xFF7B5CFF)

/** The README cover's size: GitHub's social preview ratio. */
internal val CoverWidth = 640.dp
internal val CoverHeight = 320.dp

/**
 * The README cover: the title on the left, and every character mid-performance. Draw it at
 * [CoverWidth] x [CoverHeight].
 *
 * @param strikes bump to fire the storm cloud's lightning.
 */
@Composable
internal fun Cover(strikes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.linearGradient(listOf(Peach, Blush, Lavender), start = Offset.Zero, end = Offset.Infinite),
        ),
    ) {
        Canvas(Modifier.matchParentSize()) { drawDecorations() }

        Column(Modifier.padding(start = 32.dp, top = 28.dp).width(340.dp)) {
            Text(
                "JETPACK COMPOSE",
                color = Ink.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            val title = TextStyle(fontSize = 46.sp, lineHeight = 46.sp, fontWeight = FontWeight.Black)
            Text("Compose", style = title, color = Ink)
            Text("Animations", style = title.copy(brush = TitleGradient))
            Spacer(Modifier.height(10.dp))
            Text(
                "Hand-drawn Canvas characters that stretch, morph, blink and react.",
                color = Ink.copy(alpha = 0.7f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Canvas", "Springs", "Morphing faces", "Gestures").forEach { Chip(it) }
            }
        }

        // The dog stretches out under the title, toward the others.
        StretchyDog(
            stretch = 0.6f,
            expression = DogExpression.TongueOut,
            modifier = Modifier
                .offset(x = 4.dp, y = 236.dp)
                .size(width = 340.dp, height = 80.dp),
        )
        // The cloud's canvas reaches down to Sprout's feet, so its rain and lightning land there.
        StormCloud(
            charge = 1f,
            raining = true,
            strikes = strikes,
            modifier = Modifier
                .offset(x = 410.dp, y = 8.dp)
                .size(width = 200.dp, height = 285.dp),
        )
        Sprout(
            hydration = 1f,
            expression = SproutExpression.Overjoyed,
            modifier = Modifier
                .offset(x = 435.dp, y = 150.dp)
                .size(150.dp),
        )
    }
}

@Composable
private fun Chip(label: String) {
    Text(
        label,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = Ink,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Sparkles and a dotted motion trail, so the background feels like it's moving too. */
private fun DrawScope.drawDecorations() {
    val u = size.height / 100f
    val trail = Path().apply {
        moveTo(56f * u, 80f * u)
        cubicTo(100f * u, 60f * u, 112f * u, 98f * u, 150f * u, 70f * u)
    }
    drawPath(
        trail, Swoosh, alpha = 0.25f,
        style = Stroke(
            width = 1.2f * u,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, 3f * u)),
        ),
    )
    listOf(
        Offset(118f, 12f) to 3.2f,
        Offset(194f, 16f) to 2.4f,
        Offset(126f, 46f) to 2f,
        Offset(196f, 70f) to 2.8f,
        Offset(8f, 66f) to 1.8f,
    ).forEach { (p, r) -> drawSparkle(p * u, r * u) }
}

private fun DrawScope.drawSparkle(c: Offset, r: Float) {
    val star = Path().apply {
        moveTo(c.x, c.y - r)
        quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r)
        quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r)
        close()
    }
    drawPath(star, Sparkle)
}

@Preview(widthDp = 640, heightDp = 320)
@Composable
private fun CoverPreview() {
    Cover(strikes = 0, Modifier.size(CoverWidth, CoverHeight))
}
