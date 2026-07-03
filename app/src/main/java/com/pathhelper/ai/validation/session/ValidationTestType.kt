package com.pathhelper.ai.validation.session

/**
 * Enumeration of all physical validation tests defined in the Sarathi test suite.
 *
 * Explain:
 * * Purpose of the component: Specifies the different types of validation tests supported by the system.
 * * Role within the Sarathi architecture: Serves as the configuration definition for active validation sessions and CSV output naming.
 * * Major inputs and outputs: Not applicable (enumeration of test modes).
 */
enum
class ValidationTestType(
    val label: String,
    val targetDurationSeconds: Int = 0
) {
    STATIC_STABILITY(
        label = "Static Stability Test",
        targetDurationSeconds = 60
    ),
    ANNOUNCEMENT_SUPPRESSION(
        label = "Announcement Suppression Test",
        targetDurationSeconds = 120
    ),
    DARK_ROOM(
        label = "Dark Room Test",
        targetDurationSeconds = 0
    ),
    TORCH_RECOVERY(
        label = "Torch Recovery Test",
        targetDurationSeconds = 0
    ),
    MIXED_LIGHTING(
        label = "Mixed Lighting Test",
        targetDurationSeconds = 0
    ),
    HALLWAY_WALK(
        label = "Hallway Walk Test",
        targetDurationSeconds = 0
    ),
    LANDMARK_RECOGNITION(
        label = "Landmark Recognition Test",
        targetDurationSeconds = 0
    ),
    LOOP_CLOSURE(
        label = "Loop Closure Test",
        targetDurationSeconds = 0
    ),
    STABILITY_30_MIN(
        label = "30-Minute Stability Test",
        targetDurationSeconds = 1800
    ),
    ENDURANCE_60_MIN(
        label = "60-Minute Endurance Test",
        targetDurationSeconds = 3600
    );

    /** Safe filename prefix (no spaces or special chars). */
    val filePrefix: String get() = name.lowercase().replace('_', '-')
}
