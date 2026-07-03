package com.pathhelper.ai.perception.lighting

/**
 * Lighting environment classification.
 *
 * Derived from brightness score (0–100):
 *
 * | State  | Score Range |
 * |--------|-------------|
 * | BRIGHT | >= 70       |
 * | NORMAL | >= 40       |
 * | DIM    | >= 15       |
 * | DARK   | < 15        |
 */
enum
class LowLightState {
    BRIGHT,
    NORMAL,
    DIM,
    DARK;

    companion
object {
        fun fromScore(score: Float): LowLightState = when {
            score >= 70f -> BRIGHT
            score >= 40f -> NORMAL
            score >= 15f -> DIM
            else         -> DARK
        }
    }
}
