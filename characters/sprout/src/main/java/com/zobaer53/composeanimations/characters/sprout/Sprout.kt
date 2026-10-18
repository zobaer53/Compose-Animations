package com.zobaer53.composeanimations.characters.sprout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sprout, a potted seedling with a face, drawn entirely on a [Canvas]. Its stem and leaves stand
 * up as [hydration] rises and its face morphs to [expression]. Bump [cheers] to make it jump for
 * joy and throw petals.
 *
 * When [SproutExpression.restless] is high it glances around and taps a foot now and then; the
 * idle motion (sway, breathing, blinking) is always on but kept small.
 *
 * Every animated value is read inside the draw block, so animating only redraws: no recomposition
 * or relayout per frame.
 *
 * @param hydration 0 = wilted, 1 = standing tall.
 */
@Composable
fun Sprout(
    hydration: Float,
    expression: SproutExpression,
    modifier: Modifier = Modifier,
    cheers: Int = 0,
) {
    // Bouncy, so the leaves overshoot a little as they perk up.
    val water = remember { Animatable(hydration) }
    LaunchedEffect(hydration) {
        water.animateTo(hydration, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }

    val face by animateSproutExpressionAsState(expression)

    // Phases advance at the current speeds instead of looping fixed animations, so a mood change
    // speeds the sway up or slows it down smoothly.
    val swayPhase = remember { mutableFloatStateOf(0f) }
    val breathPhase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        val turn = 2 * PI.toFloat()
        while (true) {
            withFrameNanos { now ->
                val seconds = (now - last) / 1e9f
                swayPhase.floatValue = (swayPhase.floatValue + seconds * face.swaySpeed) % turn
                breathPhase.floatValue = (breathPhase.floatValue + seconds * BreathSpeed) % turn
                last = now
            }
        }
    }

    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2_000, 5_000))
            blink.animateTo(1f, tween(durationMillis = 70))
            blink.animateTo(0f, tween(durationMillis = 120))
        }
    }

    // Fidgeting: every few seconds a glance to one side, and every other time a few impatient
    // foot taps. Both are scaled by how restless the current mood is.
    val glance = remember { Animatable(0f) }
    val tap = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        var impatient = false
        while (true) {
            delay(Random.nextLong(1_800, 3_200))
            val side = if (glance.value > 0f) -1f else 1f
            glance.animateTo(side * Random.nextFloat().coerceAtLeast(0.6f), tween(durationMillis = 220))
            impatient = !impatient
            if (impatient && face.restless > 0.5f) {
                repeat(3) {
                    tap.animateTo(1f, tween(durationMillis = 90))
                    tap.animateTo(0f, tween(durationMillis = 130))
                }
            }
            delay(Random.nextLong(600, 1_200))
            glance.animateTo(0f, tween(durationMillis = 260))
        }
    }

    // The cheer: crouch, spring up stretched, land hard, wobble flat-wide back to shape.
    val hop = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val initialCheers = remember { cheers }
    LaunchedEffect(cheers) {
        if (cheers == initialCheers) return@LaunchedEffect
        squash.animateTo(-0.18f, tween(durationMillis = 110, easing = LinearOutSlowInEasing))
        launch {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(durationMillis = 1_100, easing = LinearEasing))
            burst.snapTo(0f)
        }
        launch { squash.animateTo(0.14f, tween(durationMillis = 120)) }
        hop.animateTo(HopHeight, tween(durationMillis = 260, easing = LinearOutSlowInEasing))
        launch { squash.animateTo(0.08f, tween(durationMillis = 200)) }
        hop.animateTo(0f, tween(durationMillis = 200, easing = FastOutLinearInEasing))
        squash.snapTo(-0.22f)
        squash.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
    }

    Canvas(modifier) {
        drawSprout(
            hydration = water.value,
            face = face,
            sway = sin(swayPhase.floatValue),
            breath = sin(breathPhase.floatValue),
            blink = blink.value,
            glance = glance.value,
            tap = tap.value * face.restless,
            hop = hop.value,
            squash = squash.value,
            burst = burst.value,
        )
    }
}

/**
 * Where the flower sits within a Sprout canvas of [canvasSize] while Sprout stands upright on the
 * ground, for effects that start from it.
 */
fun sproutFlower(canvasSize: Size): Offset = Offset(canvasSize.width / 2, canvasSize.height * FlowerY / 100f)

/** Radians per second: about one slow breath every four seconds. */
private const val BreathSpeed = 1.6f

@Preview(showBackground = true, widthDp = 640)
@Composable
private fun SproutPreview() {
    Row {
        listOf(
            0f to SproutExpression.Thirsty,
            0.5f to SproutExpression.Drinking,
            0f to SproutExpression.Sulky,
            1f to SproutExpression.Overjoyed,
            1f to SproutExpression.Content,
        ).forEach { (hydration, expression) ->
            Sprout(hydration, expression, Modifier.size(128.dp))
        }
    }
}
