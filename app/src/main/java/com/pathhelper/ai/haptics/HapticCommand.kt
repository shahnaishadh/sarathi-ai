package com.pathhelper.ai.haptics

import com.pathhelper.ai.navigation.GuidanceAction
/**
* Represents the data structures or state of Haptic Command.
*/
data
class HapticCommand(
    val pattern: HapticPattern,
    val action: GuidanceAction,
    val timestamp: Long,
    val priority: Int
)
