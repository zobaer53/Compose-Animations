package com.zobaer53.composeanimations.demos.dailycheckin

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * A habit app's daily check-in with Sprout, a potted seedling: hold the button to water it, and
 * it perks up, blooms and jumps for joy. Brings its own theme, so it looks the same whatever app
 * hosts it.
 */
@Composable
fun DailyCheckInDemo() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green)) {
        CheckInScreen()
    }
}
