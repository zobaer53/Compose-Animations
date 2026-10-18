package com.zobaer53.composeanimations.characters.dog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Every number the dog's face and tail are drawn from. Moods are just presets of these numbers,
 * so switching mood morphs the face instead of swapping pictures.
 */
@Immutable
data class DogExpression(
    /** 0 = eyes wide open, 1 = upper lids fully shut. */
    val lidCover: Float,
    /** > 0 lids and brows slope down toward the other eye (angry), < 0 away from it (smug). */
    val lidSlant: Float,
    /** 0 = brows resting, 1 = raised high with slightly bigger eyes (surprise). */
    val browLift: Float,
    val pupilScale: Float,
    /** -1 = frown, 1 = big grin. */
    val smile: Float,
    /** 0 = closed, 1 = round "O" mouth. */
    val mouthOpen: Float,
    /** 0 = hidden, 1 = hanging all the way out. */
    val tongue: Float,
    /** 0 = normal eyes, 1 = closed "^ ^" happy eyes. */
    val happyEyes: Float,
    /** Degrees the tail is raised above its droop. */
    val tailLift: Float,
    /** Degrees the tail swings either side while wagging. */
    val wagAmplitude: Float,
    /** Wag speed in radians per second. */
    val wagSpeed: Float,
) {
    companion object {
        val Grumpy = DogExpression(
            lidCover = 0.42f, lidSlant = 1f, browLift = 0f, pupilScale = 1f,
            smile = -0.7f, mouthOpen = 0f, tongue = 0f, happyEyes = 0f,
            tailLift = 0f, wagAmplitude = 4f, wagSpeed = 2f,
        )
        val Surprised = DogExpression(
            lidCover = 0f, lidSlant = 0f, browLift = 0.6f, pupilScale = 0.75f,
            smile = 0f, mouthOpen = 0.55f, tongue = 0f, happyEyes = 0f,
            tailLift = 10f, wagAmplitude = 6f, wagSpeed = 4f,
        )
        val Shocked = DogExpression(
            lidCover = 0f, lidSlant = 0f, browLift = 1f, pupilScale = 0.55f,
            smile = 0f, mouthOpen = 1f, tongue = 0f, happyEyes = 0f,
            tailLift = 15f, wagAmplitude = 4f, wagSpeed = 3f,
        )
        val Curious = DogExpression(
            lidCover = 0.05f, lidSlant = 0f, browLift = 0.3f, pupilScale = 1f,
            smile = 0.35f, mouthOpen = 0f, tongue = 0f, happyEyes = 0f,
            tailLift = 22f, wagAmplitude = 10f, wagSpeed = 7f,
        )
        val Pleased = DogExpression(
            lidCover = 0.15f, lidSlant = -0.2f, browLift = 0.1f, pupilScale = 1f,
            smile = 0.75f, mouthOpen = 0f, tongue = 0f, happyEyes = 0f,
            tailLift = 28f, wagAmplitude = 14f, wagSpeed = 10f,
        )
        val Smug = DogExpression(
            lidCover = 0.5f, lidSlant = -0.4f, browLift = 0f, pupilScale = 1f,
            smile = 1f, mouthOpen = 0f, tongue = 0f, happyEyes = 0f,
            tailLift = 32f, wagAmplitude = 18f, wagSpeed = 13f,
        )
        /** Grumpy eyes with a grin: the "told you so" face after a reset. */
        val Smirk = DogExpression(
            lidCover = 0.45f, lidSlant = 0.8f, browLift = 0f, pupilScale = 1f,
            smile = 1f, mouthOpen = 0f, tongue = 0f, happyEyes = 0f,
            tailLift = 20f, wagAmplitude = 8f, wagSpeed = 5f,
        )
        val TongueOut = DogExpression(
            lidCover = 0.4f, lidSlant = -0.3f, browLift = 0f, pupilScale = 1f,
            smile = 1f, mouthOpen = 0f, tongue = 1f, happyEyes = 0f,
            tailLift = 38f, wagAmplitude = 24f, wagSpeed = 18f,
        )
        val Content = DogExpression(
            lidCover = 1f, lidSlant = 0f, browLift = 0f, pupilScale = 1f,
            smile = 0.9f, mouthOpen = 0f, tongue = 0f, happyEyes = 1f,
            tailLift = 34f, wagAmplitude = 16f, wagSpeed = 9f,
        )
    }
}

fun lerp(start: DogExpression, stop: DogExpression, fraction: Float) = DogExpression(
    lidCover = lerp(start.lidCover, stop.lidCover, fraction),
    lidSlant = lerp(start.lidSlant, stop.lidSlant, fraction),
    browLift = lerp(start.browLift, stop.browLift, fraction),
    pupilScale = lerp(start.pupilScale, stop.pupilScale, fraction),
    smile = lerp(start.smile, stop.smile, fraction),
    mouthOpen = lerp(start.mouthOpen, stop.mouthOpen, fraction),
    tongue = lerp(start.tongue, stop.tongue, fraction),
    happyEyes = lerp(start.happyEyes, stop.happyEyes, fraction),
    tailLift = lerp(start.tailLift, stop.tailLift, fraction),
    wagAmplitude = lerp(start.wagAmplitude, stop.wagAmplitude, fraction),
    wagSpeed = lerp(start.wagSpeed, stop.wagSpeed, fraction),
)

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

/**
 * Morphs toward [target]. A new target mid-morph starts from wherever the face currently is,
 * so fast typing never makes the face jump.
 */
@Composable
fun animateDogExpressionAsState(
    target: DogExpression,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 350),
): State<DogExpression> {
    var from by remember { mutableStateOf(target) }
    var to by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target == to) return@LaunchedEffect
        from = lerp(from, to, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec)
    }
    return remember { derivedStateOf { lerp(from, to, progress.value) } }
}
