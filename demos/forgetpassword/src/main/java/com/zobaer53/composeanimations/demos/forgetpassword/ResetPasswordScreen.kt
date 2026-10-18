package com.zobaer53.composeanimations.demos.forgetpassword

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zobaer53.composeanimations.characters.dog.DogExpression
import com.zobaer53.composeanimations.characters.dog.StretchyDog
import kotlinx.coroutines.delay

private val ScreenBackground = Color(0xFFF2F2F4)
private val Muted = Color(0xFF8A8A8E)
private val FieldGreen = Color(0xFF34C759)
private val CardShape = RoundedCornerShape(28.dp)

private const val Bullet = '●'
private const val BulletsAcross = 12
// Large bullets like the video; the tighter line height keeps the field at its normal height.
private val BulletTextStyle = TextStyle(fontSize = 28.sp, lineHeight = 24.sp)
// OutlinedTextField's default horizontal content padding.
private val FieldContentPadding = 16.dp

private const val SettleDelayMillis = 2_000L
private const val SmirkMillis = 2_500L

@Composable
internal fun ResetPasswordScreen(modifier: Modifier = Modifier) {
    var password by rememberSaveable { mutableStateOf("") }
    val strength = PasswordStrength.of(password)
    val focusManager = LocalFocusManager.current

    // Like the video, the dog doesn't just map strength to a face: at full strength it pants
    // with its tongue out, then settles into a content smile; after a reset it smirks at you.
    var settled by remember(strength) { mutableStateOf(false) }
    LaunchedEffect(strength) {
        if (strength == PasswordStrength.GoodEnough) {
            delay(SettleDelayMillis)
            settled = true
        }
    }
    var smirking by remember { mutableStateOf(false) }
    LaunchedEffect(smirking) {
        if (smirking) {
            delay(SmirkMillis)
            smirking = false
        }
    }
    val expression = when {
        smirking -> DogExpression.Smirk
        settled -> DogExpression.Content
        else -> strength.expression
    }

    var fieldWidth by remember { mutableIntStateOf(0) }
    val bulletStyle = rememberBulletStyle(fieldWidth)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        val screenWidth = maxWidth
        // The card must not clip: the dog is allowed to grow past its edges. That rules out
        // Card/Surface (they clip to their shape) and needs clip = false on the shadow.
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .shadow(24.dp, CardShape, clip = false, spotColor = Color.Black.copy(alpha = 0.2f))
                .background(Color.White, CardShape)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Reset password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Make it long, you weakling", fontSize = 14.sp, color = Muted)
            Spacer(Modifier.height(12.dp))

            StretchyDog(
                stretch = password.length.coerceAtMost(PasswordStrength.MaxLength) /
                    PasswordStrength.MaxLength.toFloat(),
                expression = expression,
                // Screen-wide canvas centred on the card, so the dog can outgrow the card.
                modifier = Modifier
                    .requiredWidth(screenWidth)
                    .height(120.dp),
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    smirking = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { fieldWidth = it.width },
                singleLine = true,
                textStyle = bulletStyle,
                placeholder = { Text("New password") },
                visualTransformation = PasswordVisualTransformation(Bullet),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FieldGreen,
                    unfocusedBorderColor = FieldGreen.copy(alpha = 0.6f),
                ),
            )
            Spacer(Modifier.height(6.dp))
            StrengthLabel(strength)
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    password = ""
                    smirking = true
                    focusManager.clearFocus()
                },
                enabled = strength >= PasswordStrength.Solid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    disabledContainerColor = Color.Black.copy(alpha = 0.25f),
                    disabledContentColor = Color.White,
                ),
            ) {
                Text("Reset", fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = {
                password = ""
                focusManager.clearFocus()
            }) {
                Text("Cancel", color = Muted)
            }
        }
    }
}

/**
 * Big bullets spaced so exactly [BulletsAcross] of them fill the field edge to edge; past that the
 * single-line field scrolls.
 */
@Composable
private fun rememberBulletStyle(fieldWidthPx: Int): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(fieldWidthPx, density) {
        if (fieldWidthPx == 0) return@remember BulletTextStyle
        val glyphWidth = measurer.measure(Bullet.toString(), BulletTextStyle).size.width
        // The extra 2dp keeps the cursor visible after the last bullet.
        val textWidth = fieldWidthPx - with(density) { (FieldContentPadding * 2 + 2.dp).toPx() }
        // Android drops the letter spacing at the start and end of a line, so n bullets take
        // n glyphs plus n - 1 gaps.
        val gap = (textWidth - glyphWidth * BulletsAcross) / (BulletsAcross - 1)
        BulletTextStyle.copy(letterSpacing = with(density) { gap.toSp() })
    }
}

@Composable
private fun StrengthLabel(strength: PasswordStrength) {
    val color by animateColorAsState(strength.color, label = "strengthColor")
    AnimatedContent(
        targetState = strength,
        transitionSpec = {
            // Stronger slides up from below, weaker drops in from above.
            val direction = if (targetState > initialState) 1 else -1
            (slideInVertically { it * direction } + fadeIn()) togetherWith
                (slideOutVertically { -it * direction } + fadeOut()) using
                SizeTransform(clip = false)
        },
        label = "strengthLabel",
    ) { target ->
        Text(target.label, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Composable
private fun ResetPasswordScreenPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color.Black)) {
        ResetPasswordScreen()
    }
}
