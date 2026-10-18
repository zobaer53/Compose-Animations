package com.zobaer53.composeanimations.characters.stormcloud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * A storm cloud with a temper, drawn on a [Canvas]. The fuller it gets ([charge]) the bigger, darker
 * and angrier it is: a sulky grumble when nearly empty, gritted teeth, a flushed face, steam and a
 * constant shake when full. While [raining] it pours; bump [strikes] for a bolt of lightning. When
 * no longer [present] it cheers up and floats away.
 *
 * The cloud hangs at the top of its canvas and its rain and lightning fall all the way to the
 * bottom edge, so make the canvas reach down to the ground.
 *
 * Every animated value is read inside the draw block, so animating only redraws.
 *
 * @param charge 0 = nearly empty, 1 = full to bursting.
 */
@Composable
fun StormCloud(
    charge: Float,
    modifier: Modifier = Modifier,
    raining: Boolean = false,
    strikes: Int = 0,
    present: Boolean = true,
) {
    // Swells with a wobble every time it takes on more water.
    val fullness = remember { Animatable(charge) }
    LaunchedEffect(charge) {
        fullness.animateTo(charge, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
    }

    val rain = remember { Animatable(if (raining) 1f else 0f) }
    LaunchedEffect(raining) { rain.animateTo(if (raining) 1f else 0f, tween(durationMillis = 350)) }

    val leave = remember { Animatable(if (present) 0f else 1f) }
    LaunchedEffect(present) {
        leave.animateTo(
            if (present) 0f else 1f,
            tween(durationMillis = if (present) 700 else 1_400, easing = FastOutSlowInEasing),
        )
    }

    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                time.floatValue += (now - last) / 1e9f
                last = now
            }
        }
    }

    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2_500, 5_500))
            blink.animateTo(1f, tween(durationMillis = 70))
            blink.animateTo(0f, tween(durationMillis = 120))
        }
    }

    val bolt = remember { Animatable(0f) }
    val initialStrikes = remember { strikes }
    LaunchedEffect(strikes) {
        if (strikes == initialStrikes) return@LaunchedEffect
        bolt.snapTo(0.001f)
        bolt.animateTo(1f, tween(durationMillis = 500, easing = LinearEasing))
        bolt.snapTo(0f)
    }

    // Fade as one layer: fading each puff on its own would show every overlap through the others.
    Canvas(modifier.graphicsLayer { alpha = 1f - leave.value }) {
        drawStormCloud(
            fullness = fullness.value,
            rain = rain.value,
            time = time.floatValue,
            blink = blink.value,
            bolt = bolt.value,
            boltSeed = strikes,
            leave = leave.value,
        )
    }
}

/**
 * Where a jet of water should enter the cloud, within a [StormCloud] canvas of [canvasSize]: the
 * left side of its underside, at the cloud's half-full size.
 */
fun stormCloudIntake(canvasSize: Size): Offset {
    val u = canvasSize.width / 100f
    return cloudCenter(u) + IntakeOffset * (u * cloudScale(0.5f))
}

@Preview(showBackground = true, widthDp = 600, heightDp = 200)
@Composable
private fun StormCloudPreview() {
    Row {
        StormCloud(charge = 0f, modifier = Modifier.size(200.dp))
        StormCloud(charge = 0.6f, raining = true, modifier = Modifier.size(200.dp))
        StormCloud(charge = 1f, raining = true, strikes = 1, modifier = Modifier.size(200.dp))
    }
}
