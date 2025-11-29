package notebad.prabe.sh.core.model

import android.net.Uri

/**
 * Comprehensive metadata about a file, obtained without loading its contents.
 * This allows the UI to make decisions about display mode before any heavy I/O.
 *
 * @property uri The URI pointing to the file (content:// or file://)
 * @property displayName Human-readable filename for display
 * @property mimeType The MIME type as reported by the system
 * @property size Total file size in bytes (-1 if unknown)
 * @property lastModified Timestamp of last modification (0 if unknown)
 * @property isReadOnly Whether the file cannot be written to
 * @property detectedType Our analyzed determination of the file's type
 */
data class FileMetadata(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val size: Long,
    val lastModified: Long,
    val isReadOnly: Boolean,
    val detectedType: DetectedFileType
) {
    /**
     * The file extension, if any
     */
    val extension: String
        get() = displayName.substringAfterLast('.', "").lowercase()

    /**
     * Human-readable file size
     */
    val formattedSize: String
        get() = formatFileSize(size)

    /**
     * Total number of chunks needed to read the entire file
     */
    fun totalChunks(chunkSize: Int): Long {
        if (size <= 0) return 1
        return (size + chunkSize - 1) / chunkSize
    }

    companion object {
        private fun formatFileSize(bytes: Long): String {
            if (bytes < 0) return "Unknown size"
            if (bytes < 1024) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024) return "%.2f KB".format(kb)
            val mb = kb / 1024.0
            if (mb < 1024) return "%.2f MB".format(mb)
            val gb = mb / 1024.0
            return "%.2f GB".format(gb)
        }

        /**
         * Creates a minimal metadata object for error states
         */
        fun unknown(uri: Uri): FileMetadata = FileMetadata(
            uri = uri,
            displayName = "Unknown",
            mimeType = null,
            size = -1,
            lastModified = 0,
            isReadOnly = true,
            detectedType = DetectedFileType.UNKNOWN
        )
    }
}
