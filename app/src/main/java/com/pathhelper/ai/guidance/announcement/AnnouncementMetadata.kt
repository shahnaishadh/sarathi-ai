package com.pathhelper.ai.guidance.announcement

/**
 * Immutable metadata attached to each announcement attempt.
 *
 * @param text        The text to be spoken.
 * @param priority    Determines cooldown and suppression rules.
 * @param timestampMs Wall clock when the announcement was requested.
 */
data
class AnnouncementMetadata(
    val text: String,
    val priority: AnnouncementPriority,
    val timestampMs: Long = System.currentTimeMillis()
)
