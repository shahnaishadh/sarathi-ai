package com.pathhelper.ai.guidance.announcement

/**
 * Priority levels for voice announcements.
 *
 * - [CRITICAL] – Never suppressed. Reserved for immediate collision risk or stop commands.
 * - [WARNING]  – Cooldown of 3 seconds. Used for nearby obstacles (person, door, stairs).
 * - [INFO]     – Cooldown of 10 seconds. Used for environmental context (elevator, destination).
 */
enum
class AnnouncementPriority {
    CRITICAL,
    WARNING,
    INFO
}
