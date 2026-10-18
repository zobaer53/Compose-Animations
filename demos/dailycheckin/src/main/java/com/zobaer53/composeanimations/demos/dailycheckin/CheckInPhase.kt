package com.zobaer53.composeanimations.demos.dailycheckin

import com.zobaer53.composeanimations.characters.sprout.SproutExpression

/** Where the check-in is, and what the headline says and Sprout looks like there. */
internal enum class CheckInPhase(val title: String, val subtitle: String, val expression: SproutExpression) {
    Thirsty(
        "Sprout is thirsty",
        "The cloud is slacking off. Hold the button to fill it up and make it rain.",
        SproutExpression.Thirsty,
    ),
    Watering("Here comes the storm!", "Keep holding, almost there.", SproutExpression.Drinking),
    /** Let go before the ring filled. */
    GaveUp("Hey, I was drinking that!", "Hold until the ring fills up.", SproutExpression.Sulky),
    /** Overjoyed first; the screen settles it into [SproutExpression.Content] after a moment. */
    Bloomed(
        "Back in full bloom!",
        "That's ${StartStreak + 1} days in a row. See you tomorrow.",
        SproutExpression.Overjoyed,
    ),
}

/** The streak before today's check-in. */
internal const val StartStreak = 7
