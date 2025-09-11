package com.wishring.app.domain.model

/**
 * User profile data class for MRD SDK integration
 */
data class UserProfile(
    val height: Int, // in cm
    val weight: Int, // in kg
    val age: Int,
    val gender: Gender,
    val targetSteps: Int = 10000,
    val skinColor: SkinColor = SkinColor.YELLOW,
    val wearHand: WearHand = WearHand.LEFT
)

/**
 * Gender enum
 */
enum class Gender {
    MALE, FEMALE
}

/**
 * Skin color enum
 */
enum class SkinColor {
    WHITE, WHITE_YELLOW, YELLOW, BROWN_YELLOW, BROWN, BLACK
}

/**
 * Wear hand enum
 */
enum class WearHand {
    LEFT, RIGHT
}