package com.zobaer53.composeanimations.characters.sprout

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
 * Every number Sprout's face, flower and fidgeting are drawn from. Moods are presets of these
 * numbers, so switching mood morphs the face instead of swapping pictures.
 */
@Immutable
data class SproutExpression(
    /** 0 = eyes wide open, 1 = upper lids fully shut. */
    val lidCover: Float,
    /** > 0 inner brow ends raised (worried), < 0 lowered (determined). */
    val browTilt: Float,
    /** 0 = brows resting, 1 = raised high (surprise, hope). */
    val browLift: Float,
    val pupilScale: Float,
    /** -1 = looking left, 1 = looking right. */
    val gazeX: Float,
    /** -1 = looking up, 1 = looking down. */
    val gazeY: Float,
    /** -1 = frown, 1 = smile. */
    val smile: Float,
    /** 0 = closed, 1 = round "o" mouth. */
    val mouthOpen: Float,
    /** 0 = closed, 1 = wide open grin. */
    val grin: Float,
    /** 0 = normal eyes, 1 = closed "^ ^" happy eyes. */
    val happyEyes: Float,
    /** Cheek blush, 0..1. */
    val blush: Float,
    /** 0 = closed bud, 1 = flower fully open. */
    val bloom: Float,
    /** 0 = calm, 1 = glances around and taps a foot now and then. */
    val restless: Float,
    /** Degrees the stem and leaves sway either side. */
    val swayAmplitude: Float,
    /** Sway speed in radians per second. */
    val swaySpeed: Float,
) {
    companion object {
        /** Wilting and a little impatient: looks around, taps a foot, sighs at you. */
        val Thirsty = SproutExpression(
            lidCover = 0.38f, browTilt = 1f, browLift = 0f, pupilScale = 1f, gazeX = 0f, gazeY = 0.5f,
            smile = -0.6f, mouthOpen = 0f, grin = 0f, happyEyes = 0f, blush = 0f, bloom = 0f,
            restless = 1f, swayAmplitude = 2f, swaySpeed = 1.2f,
        )
        /** Looking up into the rain with its mouth open to catch it. */
        val Drinking = SproutExpression(
            lidCover = 0f, browTilt = 0.3f, browLift = 0.8f, pupilScale = 0.9f, gazeX = -0.2f, gazeY = -1f,
            smile = 0f, mouthOpen = 0.8f, grin = 0f, happyEyes = 0f, blush = 0.25f, bloom = 0f,
            restless = 0f, swayAmplitude = 3f, swaySpeed = 3f,
        )
        /** Let go too early: a pout. */
        val Sulky = SproutExpression(
            lidCover = 0.5f, browTilt = 0.6f, browLift = 0f, pupilScale = 1f, gazeX = 0f, gazeY = 0.2f,
            smile = -1f, mouthOpen = 0f, grin = 0f, happyEyes = 0f, blush = 0f, bloom = 0f,
            restless = 0.4f, swayAmplitude = 1f, swaySpeed = 1f,
        )
        val Overjoyed = SproutExpression(
            lidCover = 0f, browTilt = 0f, browLift = 0.5f, pupilScale = 1f, gazeX = 0f, gazeY = 0f,
            smile = 1f, mouthOpen = 0f, grin = 1f, happyEyes = 1f, blush = 1f, bloom = 1f,
            restless = 0f, swayAmplitude = 7f, swaySpeed = 7f,
        )
        val Content = SproutExpression(
            lidCover = 0.12f, browTilt = 0f, browLift = 0.1f, pupilScale = 1f, gazeX = 0f, gazeY = 0f,
            smile = 0.8f, mouthOpen = 0f, grin = 0f, happyEyes = 0f, blush = 0.6f, bloom = 1f,
            restless = 0f, swayAmplitude = 3f, swaySpeed = 1.6f,
        )
    }
}

fun lerp(start: SproutExpression, stop: SproutExpression, fraction: Float) = SproutExpression(
    lidCover = lerp(start.lidCover, stop.lidCover, fraction),
    browTilt = lerp(start.browTilt, stop.browTilt, fraction),
    browLift = lerp(start.browLift, stop.browLift, fraction),
    pupilScale = lerp(start.pupilScale, stop.pupilScale, fraction),
    gazeX = lerp(start.gazeX, stop.gazeX, fraction),
    gazeY = lerp(start.gazeY, stop.gazeY, fraction),
    smile = lerp(start.smile, stop.smile, fraction),
    mouthOpen = lerp(start.mouthOpen, stop.mouthOpen, fraction),
    grin = lerp(start.grin, stop.grin, fraction),
    happyEyes = lerp(start.happyEyes, stop.happyEyes, fraction),
    blush = lerp(start.blush, stop.blush, fraction),
    bloom = lerp(start.bloom, stop.bloom, fraction),
    restless = lerp(start.restless, stop.restless, fraction),
    swayAmplitude = lerp(start.swayAmplitude, stop.swayAmplitude, fraction),
    swaySpeed = lerp(start.swaySpeed, stop.swaySpeed, fraction),
)

private fun lerp(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

/**
 * Morphs toward [target]. A new target mid-morph starts from wherever the face currently is, so
 * the face never jumps.
 */
@Composable
fun animateSproutExpressionAsState(
    target: SproutExpression,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 400),
): State<SproutExpression> {
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
