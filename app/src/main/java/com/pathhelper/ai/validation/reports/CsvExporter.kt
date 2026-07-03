package com.pathhelper.ai.validation.reports

import android.content.Context
import android.util.Log
import com.pathhelper.ai.validation.logging.ValidationLogger
import com.pathhelper.ai.validation.session.ValidationSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports a [ValidationSession] to a CSV file in the app's external files directory.
 *
 * ## Output directory
 * `<externalFilesDir>/validation/logs/<testPrefix>-<timestamp>.csv`
 *
 * Pull from device with:
 * ```
 * adb pull /sdcard/Android/data/com.pathhelper.ai/files/validation/logs/
 * ```
 *
 * If external storage is unavailable, falls back to internal files dir.
 */
class CsvExporter(private val context: Context) {

    companion
object {
        private const val TAG = "PathHelper.CsvExporter"
        private const val DIR = "validation/logs"
        private val TIMESTAMP_FMT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    }

    /**
     * Writes the session to a CSV file.
     *
     * Safe to call from any thread; does synchronous file IO.
     *
     * @return The [File] written, or null if an error occurred.
     */
    fun export(session: ValidationSession): File? {
        return try {
            val dir = (context.getExternalFilesDir(null)
                ?: context.filesDir)
                .resolve(DIR)
                .also { it.mkdirs() }

            val timestamp = TIMESTAMP_FMT.format(Date(session.startTimeMs))
            val filename = "${session.testType.filePrefix}-$timestamp.csv"
            val file = File(dir, filename)

            file.bufferedWriter().use { writer ->
                writer.write(ValidationLogger.csvHeader())
                writer.newLine()
                for (sample in session.samples) {
                    writer.write(ValidationLogger.toCsvRow(sample))
                    writer.newLine()
                }
            }

            Log.i(TAG, "CSV exported: ${file.absolutePath} (${session.samples.size} rows)")
            file
        } catch (e: Exception) {
            Log.e(TAG, "CSV export failed: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns all CSV files previously exported.
     */
    fun listExports(): List<File> {
        val dir = (context.getExternalFilesDir(null) ?: context.filesDir).resolve(DIR)
        return dir.listFiles()?.filter { it.extension == "csv" }?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
