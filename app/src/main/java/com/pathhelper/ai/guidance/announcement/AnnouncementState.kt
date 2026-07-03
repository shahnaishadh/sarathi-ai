package com.pathhelper.ai.guidance.announcement

/**
 * Mutable counters tracking announcement metrics at runtime.
 *
 * Exposed via [AnnouncementManager.snapshot] as an immutable copy.
 */
data
class AnnouncementState(
    var generated: Int = 0,
    var spoken: Int = 0,
    var suppressed: Int = 0
) {
    val suppressionRate: Float
        get() = if (generated > 0) suppressed.toFloat() / generated else 0f
}
