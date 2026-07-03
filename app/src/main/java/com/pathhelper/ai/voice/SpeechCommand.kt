package com.pathhelper.ai.voice

import com.pathhelper.ai.navigation.GuidanceAction
/**
* Represents the data structures or state of Speech Command.
*/
data
class SpeechCommand(
    val text: String,
    val action: GuidanceAction,
    val priority: Int,
    val timestamp: Long
)
