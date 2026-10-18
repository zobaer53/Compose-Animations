package com.zobaer53.composeanimations

import androidx.compose.runtime.Composable
import com.zobaer53.composeanimations.demos.dailycheckin.DailyCheckInDemo
import com.zobaer53.composeanimations.demos.forgetpassword.ForgetPasswordDemo

internal class Demo(
    val title: String,
    val description: String,
    val content: @Composable () -> Unit,
)

/** Every demo the showcase lists, in order. Add a new demo module here. */
internal val Demos = listOf(
    Demo(
        title = "Forget password",
        description = "A dachshund that stretches with your password and judges how strong it is.",
        content = { ForgetPasswordDemo() },
    ),
    Demo(
        title = "Daily check-in",
        description = "Sprout, a thirsty potted seedling. Hold to water it and watch it bloom.",
        content = { DailyCheckInDemo() },
    ),
)
