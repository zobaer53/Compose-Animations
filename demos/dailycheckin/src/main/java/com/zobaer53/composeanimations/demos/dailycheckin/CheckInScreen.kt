package com.zobaer53.composeanimations.demos.dailycheckin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.sp
import com.zobaer53.composeanimations.characters.sprout.Sprout
import com.zobaer53.composeanimations.characters.sprout.SproutExpression
import com.zobaer53.composeanimations.characters.sprout.sproutFlower
import com.zobaer53.composeanimations.characters.stormcloud.StormCloud
import com.zobaer53.composeanimations.characters.stormcloud.stormCloudIntake
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random

private val ScreenBackground = Color(0xFFFBF6EE)
private val Ink = Color(0xFF2B2118)
private val Muted = Color(0xFF8C8177)
internal val Green = Color(0xFF47A452)
private val Water = Color(0xFF4AA8F0)
private val Track = Color(0xFFEAE3D8)
private val Sun = Color(0xFFFFD873)

/** How long the button must be held to fill the ring. */
private const val FillMillis = 2_200
/** How long a full ring takes to drain after letting go early. */
private const val DrainMillis = 700
/** Sprout perks up in this many gulps rather than continuously. */
private const val Gulps = 4
private const val SulkMillis = 1_800L
private const val SettleDelayMillis = 2_200L
/** Time for the jet to reach the cloud and the rain to start before the first bolt. */
private const val FirstStrikeMillis = 500L
/** Sprout's canvas size, and how far above its bottom edge it stands. */
private val SproutSize = 240.dp
private val SproutGround = SproutSize * 0.05f

@Composable
internal fun CheckInScreen(modifier: Modifier = Modifier) {
    var done by rememberSaveable { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var gaveUp by remember { mutableStateOf(false) }
    // Not saveable on purpose: restoring the screen shouldn't replay the celebration or thunder.
    var cheers by remember { mutableIntStateOf(0) }
    var strikes by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(if (done) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Holding fills the ring at a steady rate; letting go early drains it back. A filled ring
    // checks in. Releasing restarts this effect, which cancels the fill mid-way.
    LaunchedEffect(holding) {
        if (done) return@LaunchedEffect
        if (holding) {
            gaveUp = false
            val remaining = ((1f - progress.value) * FillMillis).roundToInt()
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            done = true
            // One last thunderclap as the cloud empties, and Sprout blooms.
            strikes++
            cheers++
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        } else if (progress.value > 0f) {
            gaveUp = true
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            progress.animateTo(0f, tween((progress.value * DrainMillis).roundToInt()))
        }
    }
    LaunchedEffect(gaveUp) {
        if (gaveUp) {
            delay(SulkMillis)
            gaveUp = false
        }
    }
    // While the storm rages, lightning strikes every so often.
    LaunchedEffect(holding, done) {
        if (holding && !done) {
            delay(FirstStrikeMillis)
            while (true) {
                strikes++
                delay(Random.nextLong(550, 950))
            }
        }
    }
    // Thunder: a buzz and a quick, dying side-to-side rumble of the whole screen.
    val rumble = remember { Animatable(0f) }
    LaunchedEffect(strikes) {
        if (strikes == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        for (i in 0 until 6) {
            rumble.animateTo((if (i % 2 == 0) 1f else -1f) * (6 - i), tween(durationMillis = 35))
        }
        rumble.animateTo(0f, tween(durationMillis = 40))
    }

    // Like the dog's tongue-out moment: overjoyed first, then a calm, content smile.
    var settled by remember(done) { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (done) {
            delay(SettleDelayMillis)
            settled = true
        }
    }

    // Stepped, so Sprout recomposes once per gulp instead of every frame, and each step gets
    // its own springy perk-up.
    val hydration by remember { derivedStateOf { floor(progress.value * Gulps) / Gulps } }
    LaunchedEffect(hydration) {
        if (holding && hydration in 0.01f..0.99f) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    val phase = when {
        done -> CheckInPhase.Bloomed
        holding -> CheckInPhase.Watering
        gaveUp -> CheckInPhase.GaveUp
        else -> CheckInPhase.Thirsty
    }
    val expression = if (done && settled) SproutExpression.Content else phase.expression

    // Where the jet starts and ends and the bloom wave starts, in root coordinates.
    var buttonCenter by remember { mutableStateOf(Offset.Unspecified) }
    var buttonRadius by remember { mutableStateOf(0f) }
    var intake by remember { mutableStateOf(Offset.Unspecified) }
    var flower by remember { mutableStateOf(Offset.Unspecified) }

    // Background outside the rumbling layer, so shaking never shows the window behind it.
    Box(modifier.fillMaxSize().background(ScreenBackground)) {
        Box(Modifier.matchParentSize().graphicsLayer { translationX = rumble.value.dp.toPx() }) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily check-in", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    StreakChip(if (done) StartStreak + 1 else StartStreak)
                }

                // The stage: the storm cloud hangs at the top, Sprout stands at the bottom, and the
                // cloud's rain and lightning reach down to Sprout's feet.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .drawBehind {
                            // A patch of sunshine behind Sprout that warms up as it drinks.
                            val warmth = 0.25f + 0.75f * progress.value
                            val radius = 140.dp.toPx()
                            val center = Offset(size.width / 2, size.height - SproutSize.toPx() / 2)
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(Sun.copy(alpha = 0.6f * warmth), Sun.copy(alpha = 0f)),
                                    center = center,
                                    radius = radius,
                                ),
                                radius = radius,
                                center = center,
                            )
                        },
                ) {
                    StormCloud(
                        charge = if (done) 1f else hydration,
                        raining = holding && !done,
                        strikes = strikes,
                        // Emptied out by the check-in, it cheers up and drifts off.
                        present = !done,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .requiredWidth(300.dp)
                            .fillMaxHeight()
                            .padding(bottom = SproutGround)
                            .onGloballyPositioned { intake = it.positionInRoot() + stormCloudIntake(it.size.toSize()) },
                    )
                    Sprout(
                        hydration = if (done) 1f else hydration,
                        expression = expression,
                        cheers = cheers,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(SproutSize)
                            .onGloballyPositioned { flower = it.positionInRoot() + sproutFlower(it.size.toSize()) },
                    )
                }

                Spacer(Modifier.height(8.dp))
                AnimatedText(phase.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(6.dp))
                // Always two lines tall, so Sprout and the button don't shift (and drag the jet) as it changes.
                AnimatedText(phase.subtitle, fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Muted, minLines = 2)

                Spacer(Modifier.height(24.dp))
                WeekStrip(progress = { progress.value }, done = done, cheers = cheers)

                Spacer(Modifier.height(28.dp))
                HoldToWaterButton(
                    modifier = Modifier.onGloballyPositioned {
                        buttonRadius = it.size.width / 2f
                        buttonCenter = it.positionInRoot() + Offset(buttonRadius, it.size.height / 2f)
                    },
                    progress = { progress.value },
                    done = done,
                    onHoldChange = { holding = it },
                    onAccessibilityWater = {
                        // No holding needed with a screen reader: one activation waters Sprout.
                        scope.launch {
                            progress.animateTo(1f, tween(600))
                            done = true
                            cheers++
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    if (done) "Watered for today" else "Hold to water",
                    color = Muted,
                    fontSize = 13.sp,
                )

                Spacer(Modifier.height(24.dp))
                Button(
                    // A demo has nowhere to continue to, so it starts the check-in over: Sprout wilts again.
                    onClick = {
                        done = false
                        scope.launch { progress.animateTo(0f, tween(900)) }
                    },
                    enabled = done,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        disabledContainerColor = Track,
                        disabledContentColor = Muted,
                    ),
                ) {
                    Text("Continue", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            // On top of everything, like the video's bolt: the jet crosses the text on its way up.
            WateringOverlay(
                source = buttonCenter,
                sourceRadius = buttonRadius,
                target = intake,
                bloomFrom = flower,
                flowing = holding && !done,
                strikes = strikes,
                cheers = cheers,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun AnimatedText(text: String, fontSize: TextUnit, fontWeight: FontWeight, color: Color, minLines: Int = 1) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn()) togetherWith
                (slideOutVertically { -it / 2 } + fadeOut()) using
                SizeTransform(clip = false)
        },
        label = "checkInText",
    ) { target ->
        Text(
            target,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = TextAlign.Center,
            minLines = minLines,
        )
    }
}

@Composable
private fun StreakChip(days: Int) {
    Row(
        Modifier
            .background(Color.White, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(14.dp)) { drawWaterDrop(Water) }
        Spacer(Modifier.width(6.dp))
        // The number rolls up when the streak grows.
        AnimatedContent(
            targetState = days,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInVertically { it * direction } + fadeIn()) togetherWith
                    (slideOutVertically { -it * direction } + fadeOut())
            },
            label = "streak",
        ) { target ->
            Text("$target", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(" day streak", color = Ink, fontSize = 14.sp)
    }
}

/** This week, Monday first. Earlier days are checked; today's circle fills while watering. */
@Composable
private fun WeekStrip(progress: () -> Float, done: Boolean, cheers: Int) {
    val today = remember { (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(cheers) {
        if (cheers > 0) {
            pop.snapTo(1.35f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        "MTWTFSS".forEachIndexed { index, letter ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    letter.toString(),
                    color = if (index == today) Ink else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (index == today) FontWeight.Bold else FontWeight.Medium,
                )
                Spacer(Modifier.height(6.dp))
                Canvas(
                    Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            if (index == today) {
                                scaleX = pop.value
                                scaleY = pop.value
                            }
                        },
                ) {
                    when {
                        index < today || (index == today && done) -> {
                            drawCircle(Green)
                            drawCheck(Color.White, strokeWidth = 2.5.dp.toPx())
                        }
                        index == today -> {
                            val stroke = 3.dp.toPx()
                            val inset = stroke / 2
                            drawCircle(Track, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
                            drawArc(
                                Water,
                                startAngle = -90f,
                                sweepAngle = 360f * progress(),
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke),
                                style = Stroke(stroke, cap = StrokeCap.Round),
                            )
                        }
                        else -> drawCircle(Track)
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldToWaterButton(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    done: Boolean,
    onHoldChange: (Boolean) -> Unit,
    onAccessibilityWater: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val press = animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val fill by animateFloatAsState(if (done) 1f else 0f, tween(300), label = "doneFill")
    Canvas(
        modifier
            .size(96.dp)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .semantics {
                role = Role.Button
                contentDescription = if (done) "Sprout is watered" else "Water Sprout"
                if (!done) {
                    onClick {
                        onAccessibilityWater()
                        true
                    }
                }
            }
            .pointerInput(done) {
                if (done) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    onHoldChange(true)
                    // finally: this also runs if the gesture is cancelled because the ring
                    // filled (done changes, restarting pointerInput), so holding never sticks.
                    try {
                        waitForUpOrCancellation()
                    } finally {
                        pressed = false
                        onHoldChange(false)
                    }
                }
            },
    ) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2
        val radius = size.minDimension / 2 - inset
        drawCircle(Color.White, radius)
        drawCircle(Green, radius * fill)
        drawCircle(Track, radius, style = Stroke(stroke))
        drawArc(
            if (done) Green else Water,
            startAngle = -90f,
            sweepAngle = 360f * progress(),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        val icon = size.minDimension * 0.34f
        if (fill < 0.5f) {
            translate(left = (size.width - icon) / 2, top = (size.height - icon) / 2) {
                drawWaterDrop(Water, Size(icon, icon), alpha = 1f - fill * 2f)
            }
        } else {
            drawCheck(Color.White, strokeWidth = 4.dp.toPx(), scale = 0.4f, alpha = fill * 2f - 1f)
        }
    }
}

private fun DrawScope.drawWaterDrop(color: Color, area: Size = size, alpha: Float = 1f) {
    val w = area.width
    val h = area.height
    val drop = Path().apply {
        moveTo(w / 2, 0f)
        cubicTo(w * 0.62f, h * 0.22f, w * 0.92f, h * 0.45f, w * 0.92f, h * 0.64f)
        cubicTo(w * 0.92f, h * 0.86f, w * 0.72f, h, w / 2, h)
        cubicTo(w * 0.28f, h, w * 0.08f, h * 0.86f, w * 0.08f, h * 0.64f)
        cubicTo(w * 0.08f, h * 0.45f, w * 0.38f, h * 0.22f, w / 2, 0f)
        close()
    }
    drawPath(drop, color, alpha = alpha)
}

/** A check mark centred in the canvas, [scale] of its size. */
private fun DrawScope.drawCheck(color: Color, strokeWidth: Float, scale: Float = 0.5f, alpha: Float = 1f) {
    val s = size.minDimension * scale
    val c = center
    val check = Path().apply {
        moveTo(c.x - s * 0.5f, c.y)
        lineTo(c.x - s * 0.12f, c.y + s * 0.36f)
        lineTo(c.x + s * 0.5f, c.y - s * 0.32f)
    }
    drawPath(
        check, color, alpha = alpha.coerceIn(0f, 1f),
        style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun CheckInScreenPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green)) {
        CheckInScreen()
    }
}
