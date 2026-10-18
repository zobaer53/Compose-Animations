package com.zobaer53.composeanimations.characters.dog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * A dachshund drawn entirely on a [Canvas] whose body grows with [stretch] and whose face morphs
 * to [expression]. The dog draws centred and grows sideways to fill the canvas width, so give it
 * a canvas as wide as it is allowed to get, even if that is wider than its parent.
 *
 * Every animated value is read inside the draw block, so animating only redraws: no recomposition
 * or relayout per frame.
 *
 * @param stretch 0 = short dog, 1 = as long as the canvas allows.
 */
@Composable
fun StretchyDog(
    stretch: Float,
    expression: DogExpression,
    modifier: Modifier = Modifier,
) {
    val length = remember { Animatable(stretch) }
    LaunchedEffect(stretch) {
        length.animateTo(stretch, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow))
    }

    val face by animateDogExpressionAsState(expression)

    // The wag phase advances at the current wag speed instead of looping a fixed animation, so the
    // tail speeds up or slows down smoothly when the mood changes.
    val wagPhase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                wagPhase.floatValue = (wagPhase.floatValue + (now - last) / 1e9f * face.wagSpeed) % (2 * PI.toFloat())
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

    Canvas(modifier) {
        drawDog(
            stretch = length.value,
            targetStretch = stretch,
            face = face,
            wag = sin(wagPhase.floatValue),
            blink = blink.value,
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun StretchyDogPreview() {
    Column {
        listOf(
            0f to DogExpression.Grumpy,
            0.3f to DogExpression.Surprised,
            0.5f to DogExpression.Shocked,
            0.6f to DogExpression.Curious,
            0.75f to DogExpression.Pleased,
            0.85f to DogExpression.Smug,
            1f to DogExpression.TongueOut,
            1f to DogExpression.Content,
        ).forEach { (stretch, expression) ->
            StretchyDog(stretch, expression, Modifier.fillMaxWidth().height(110.dp))
        }
    }
}
