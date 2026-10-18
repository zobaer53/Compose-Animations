package com.zobaer53.composeanimations.demos.forgetpassword

import androidx.compose.ui.graphics.Color
import com.zobaer53.composeanimations.characters.dog.DogExpression

private val Weak = Color(0xFFE53935)
private val Strong = Color(0xFF2E9E4F)

/** Strength is just length here, because the whole joke is "make it long". */
internal enum class PasswordStrength(val label: String, val color: Color, val expression: DogExpression) {
    Pathetic("Password strength: pathetic", Weak, DogExpression.Grumpy),
    StillWeak("still weak", Weak, DogExpression.Surprised),
    Meh("meh, longer", Weak, DogExpression.Shocked),
    Noob("ok for a noob", Weak, DogExpression.Curious),
    NotBad("not bad, weakling", Weak, DogExpression.Pleased),
    Solid("damn, solid", Strong, DogExpression.Smug),
    /** Tongue out first; the screen settles it into [DogExpression.Content] after a moment. */
    GoodEnough("Password strength: finally good enough", Strong, DogExpression.TongueOut);

    companion object {
        /** Length at which the dog is fully stretched. */
        const val MaxLength = 16

        fun of(password: String): PasswordStrength = when (password.length) {
            in 0..3 -> Pathetic
            in 4..6 -> StillWeak
            in 7..8 -> Meh
            in 9..10 -> Noob
            in 11..12 -> NotBad
            in 13..14 -> Solid
            else -> GoodEnough
        }
    }
}
