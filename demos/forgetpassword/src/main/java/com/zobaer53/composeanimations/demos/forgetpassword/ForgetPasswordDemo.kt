package com.zobaer53.composeanimations.demos.forgetpassword

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A reset-password card with a dachshund that stretches as the password grows and judges how
 * strong it is. Brings its own theme, so it looks the same whatever app hosts it.
 */
@Composable
fun ForgetPasswordDemo() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color.Black)) {
        ResetPasswordScreen()
    }
}
