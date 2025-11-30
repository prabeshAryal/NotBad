package notbad.prabe.sh.core.io

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import notbad.prabe.sh.core.model.DetectedFileType
import notbad.prabe.sh.core.model.FileChunk
import notbad.prabe.sh.core.model.FileMetadata
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Memory-safe file I/O repository that uses NIO FileChannel for random access.
 * 
 * CRITICAL DESIGN PRINCIPLE: This class NEVER loads an entire file into memory.
 * All reads are performed in fixed-size chunks using memory-mapped or direct buffer access.
 * 
 * This enables handling files of arbitrary size (5GB+) without OutOfMemoryError.
 * All I/O operations are performed on Dispatchers.IO for main-thread safety.
 */
class LargeFileRepository(
    private val context: Context
) {
    companion object {
        /**
         * Standard chunk size for reading files (8KB)
         * Chosen as a balance between:
         * - Memory efficiency (small enough to not stress heap)
         * - I/O efficiency (large enough to minimize syscall overhead)
         * - UI responsiveness (small enough for quick loading)
         */
        const val DEFAULT_CHUNK_SIZE = 8 * 1024 // 8KB

        /**
         * Size of the buffer used for binary detection
         */
        const val BINARY_DETECTION_BUFFER_SIZE = 8 * 1024 // 8KB

        /**
         * Threshold ratio of non-printable characters to classify as binary
         * If more than 30% of characters are non-printable, treat as binary
         */
        const val BINARY_THRESHOLD_RATIO = 0.30f

        /**
         * Threshold for null byte detection (if any null bytes in first 8KB, likely binary)
         */
        const val NULL_BYTE_THRESHOLD = 1

        /**
         * Maximum file size to attempt full text loading (500KB)
         * Files larger than this will be truncated with a warning
         * Reduced from 5MB to prevent OOM during Compose text rendering
         */
        const val MAX_FULL_TEXT_LOAD_SIZE = 500 * 1024L // 500KB
        
        /**
         * Maximum file size for ANY text loading - no limit for JSON/text files
         * We load ALL text files, but disable syntax highlighting for large ones
         */
        const val ABSOLUTE_MAX_TEXT_SIZE = 50 * 1024 * 1024L // 50MB
        
        /**
         * Chunk size for progressive loading
         */
        const val PROGRESSIVE_CHUNK_SIZE = 64 * 1024 // 64KB
    }

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * Retrieves comprehensive metadata about a file without loading its contents.
     * This is the first call that should be made when opening any file.
     *
     * @param uri The URI of the file (content:// or file://)
     * @return FileMetadata containing size, type, name, and detected content type
     */
    suspend fun getFileMetadata(uri: Uri): Result<FileMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = getDisplayName(uri)
            val mimeType = contentResolver.getType(uri)
            val size = getFileSize(uri)
            val isReadOnly = !isWritable(uri)

            // Detect file type based on extension, mime type, and content analysis
            val detectedType = detectFileType(uri, displayName, mimeType)

            FileMetadata(
                uri = uri,
                displayName = displayName,
                mimeType = mimeType,
                size = size,
                lastModified = System.currentTimeMillis(), // ContentResolver doesn't always provide this
                isReadOnly = isReadOnly,
                detectedType = detectedType
            )
        }
    }

    /**
     * Reads a single chunk of data from the file at the specified index.
     * Uses NIO FileChannel for efficient random access.
     *
     * @param uri The file URI
     * @param chunkIndex The 0-based index of the chunk to read
     * @param chunkSize The size of each chunk (default 8KB)
     * @return Result containing the FileChunk or an error
     */
    suspend fun readChunk(
        uri: Uri,
        chunkIndex: Long,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Result<FileChunk> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    fis.channel.use { channel ->
                        val fileSize = channel.size()
                        val offset = chunkIndex * chunkSize

                        // Check if offset is beyond file size
                        if (offset >= fileSize) {
                            return@runCatching FileChunk.empty(chunkIndex)
                        }

                        // Calculate actual bytes to read (may be less for last chunk)
                        val bytesToRead = minOf(chunkSize.toLong(), fileSize - offset).toInt()
                        val buffer = ByteBuffer.allocate(bytesToRead)

                        // Position channel and read
                        channel.position(offset)
                        val bytesRead = channel.read(buffer)

                        if (bytesRead <= 0) {
                            return@runCatching FileChunk.empty(chunkIndex)
                        }

                        buffer.flip()
                        val data = ByteArray(bytesRead)
                        buffer.get(data)

                        val isLastChunk = (offset + bytesRead) >= fileSize

                        FileChunk(
                            offset = offset,
                            data = data,
                            chunkIndex = chunkIndex,
                            isLastChunk = isLastChunk
                        )
                    }
                }
            } ?: throw IllegalStateException("Unable to open file descriptor for URI: $uri")
        }
    }

    /**
     * Reads a range of bytes from the file.
     * Useful for reading arbitrary sections without chunk alignment.
     *
     * @param uri The file URI
     * @param offset Starting byte offset
     * @param length Number of bytes to read
     * @return Result containing the byte array or an error
     */
    suspend fun readRange(
        uri: Uri,
        offset: Long,
        length: Int
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    fis.channel.use { channel ->
                        val fileSize = channel.size()

                        if (offset >= fileSize) {
                            return@runCatching ByteArray(0)
                        }

                        val bytesToRead = minOf(length.toLong(), fileSize - offset).toInt()
                        val buffer = ByteBuffer.allocate(bytesToRead)

                        channel.position(offset)
                        val bytesRead = channel.read(buffer)

                        if (bytesRead <= 0) {
                            return@runCatching ByteArray(0)
                        }

                        buffer.flip()
                        val data = ByteArray(bytesRead)
                        buffer.get(data)
                        data
                    }
                }
            } ?: throw IllegalStateException("Unable to open file descriptor for URI: $uri")
        }
    }

    /**
     * Creates a Flow that emits chunks sequentially from start to end.
     * Ideal for streaming reads or building up a text buffer progressively.
     *
     * @param uri The file URI
     * @param startChunk Starting chunk index (0-based)
     * @param endChunk Ending chunk index (exclusive), null for EOF
     * @param chunkSize Size of each chunk
     * @return Flow emitting FileChunks
     */
    fun readChunksFlow(
        uri: Uri,
        startChunk: Long = 0,
        endChunk: Long? = null,
        chunkSize: Int = DEFAULT_CHUNK_SIZE
    ): Flow<FileChunk> = flow {
        var currentChunk = startChunk
        var reachedEnd = false

        while (!reachedEnd) {
            if (endChunk != null && currentChunk >= endChunk) {
                break
            }

            val result = readChunk(uri, currentChunk, chunkSize)
            result.onSuccess { chunk ->
                if (chunk.size > 0) {
                    emit(chunk)
                }
                reachedEnd = chunk.isLastChunk || chunk.size == 0
            }.onFailure {
                reachedEnd = true
            }

            currentChunk++
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Reads text content from a file with size limits for safety.
     * For files under MAX_FULL_TEXT_LOAD_SIZE, loads the full content.
     * For larger files, loads only the first portion.
     *
     * @param uri The file URI
     * @param maxSize Maximum bytes to load (default 1MB)
     * @param charset Character encoding (default UTF-8)
     * @return Result containing the text content or an error
     */
    suspend fun readTextContent(
        uri: Uri,
        maxSize: Long = MAX_FULL_TEXT_LOAD_SIZE,
        charset: java.nio.charset.Charset = Charsets.UTF_8
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileSize = getFileSize(uri)
            val bytesToRead = minOf(fileSize, maxSize).toInt()

            val result = readRange(uri, 0, bytesToRead)
            result.getOrThrow().toString(charset)
        }
    }
    
    /**
     * Reads text content progressively with progress callback.
     * Loads full file content without size limits, suitable for JSON and text files.
     * Reports progress via callback for UI updates.
     *
     * @param uri The file URI
     * @param onProgress Callback with (loadedBytes, totalBytes, progress 0.0-1.0)
     * @param charset Character encoding (default UTF-8)
     * @return Result containing the full text content or an error
     */
    suspend fun readTextContentProgressive(
        uri: Uri,
        onProgress: suspend (loadedBytes: Long, totalBytes: Long, progress: Float) -> Unit,
        charset: java.nio.charset.Charset = Charsets.UTF_8
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fileSize = getFileSize(uri)
            val useChunked = fileSize <= 0 || fileSize == -1L || fileSize > PROGRESSIVE_CHUNK_SIZE
            val maxBytes = if (fileSize > 0) minOf(fileSize, ABSOLUTE_MAX_TEXT_SIZE) else ABSOLUTE_MAX_TEXT_SIZE
            val stringBuilder = StringBuilder((maxBytes / 2).toInt())
            var bytesLoaded = 0L
            var offset = 0L

            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    fis.channel.use { channel ->
                        while (offset < maxBytes) {
                            val bytesToRead = minOf(PROGRESSIVE_CHUNK_SIZE.toLong(), maxBytes - offset).toInt()
                            val buffer = ByteBuffer.allocate(bytesToRead)
                            channel.position(offset)
                            val bytesRead = channel.read(buffer)
                            if (bytesRead <= 0) break
                            buffer.flip()
                            val data = ByteArray(bytesRead)
                            buffer.get(data)
                            stringBuilder.append(data.toString(charset))
                            bytesLoaded += bytesRead
                            offset += bytesRead
                            // Estimate progress: if fileSize is unknown, use offset/maxBytes
                            val progress = if (fileSize > 0) (bytesLoaded.toFloat() / maxBytes).coerceIn(0f, 1f) else (offset.toFloat() / maxBytes).coerceIn(0f, 1f)
                            onProgress(bytesLoaded, maxBytes, progress)
                        }
                    }
                }
            } ?: throw IllegalStateException("Unable to open file descriptor for URI: $uri")

            // If file is very small and we only did one chunk, ensure progress is set to 100%
            onProgress(bytesLoaded, maxBytes, 1f)
            stringBuilder.toString()
        }
    }

    /**
     * Writes text content to a file.
     * 
     * @param uri The file URI
     * @param content The text content to write
     * @param charset Character encoding (default UTF-8)
     * @return Result indicating success or failure
     */
    suspend fun writeTextContent(
        uri: Uri,
        content: String,
        charset: java.nio.charset.Charset = Charsets.UTF_8
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                outputStream.write(content.toByteArray(charset))
            } ?: throw IllegalStateException("Unable to open output stream for URI: $uri")
        }
    }

    /**
     * Detects whether a file is binary by analyzing its content.
     * Scans the first 8KB for null bytes and non-printable characters.
     *
     * @param uri The file URI
     * @return true if the file is likely binary, false if likely text
     */
    suspend fun isBinaryFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val chunk = readChunk(uri, 0, BINARY_DETECTION_BUFFER_SIZE).getOrNull()
                ?: return@withContext true // Can't read = treat as binary

            val data = chunk.data
            if (data.isEmpty()) return@withContext false

            // Count null bytes and non-printable characters
            var nullCount = 0
            var nonPrintableCount = 0

            for (byte in data) {
                val value = byte.toInt() and 0xFF

                if (value == 0) {
                    nullCount++
                    // Early exit: null bytes strongly indicate binary
                    if (nullCount >= NULL_BYTE_THRESHOLD) {
                        return@withContext true
                    }
                }

                // Non-printable: not tab(9), newline(10), carriage return(13), or printable ASCII (32-126)
                if (value !in 9..13 && value !in 32..126) {
                    // Allow some high-byte characters for UTF-8
                    if (value < 128) {
                        nonPrintableCount++
                    }
                }
            }

            // Calculate ratio of non-printable characters
            val ratio = nonPrintableCount.toFloat() / data.size
            ratio > BINARY_THRESHOLD_RATIO
        } catch (e: Exception) {
            true // Error reading = treat as binary for safety
        }
    }

    /**
     * Determines the file type based on multiple signals.
     */
    private suspend fun detectFileType(
        uri: Uri,
        displayName: String,
        mimeType: String?
    ): DetectedFileType {
        val extension = displayName.substringAfterLast('.', "").lowercase()

        // First, check file extension (most reliable for text files)
        when {
            extension in DetectedFileType.BINARY_EXTENSIONS -> return DetectedFileType.BINARY
            extension in DetectedFileType.MARKDOWN_EXTENSIONS -> return DetectedFileType.MARKDOWN
            extension in DetectedFileType.SOURCE_CODE_EXTENSIONS -> return DetectedFileType.SOURCE_CODE
            extension in DetectedFileType.TEXT_EXTENSIONS -> return DetectedFileType.TEXT
        }

        // Check MIME type
        if (DetectedFileType.isTextMimeType(mimeType)) {
            return when {
                mimeType?.contains("markdown") == true -> DetectedFileType.MARKDOWN
                extension.isNotEmpty() -> DetectedFileType.SOURCE_CODE
                else -> DetectedFileType.TEXT
            }
        }

        // Finally, perform content analysis
        return if (isBinaryFile(uri)) {
            DetectedFileType.BINARY
        } else {
            // It's text, but what kind?
            when {
                extension.isNotEmpty() -> DetectedFileType.SOURCE_CODE
                else -> DetectedFileType.TEXT
            }
        }
    }

    /**
     * Gets the display name of the file from the URI.
     */
    private fun getDisplayName(uri: Uri): String {
        // Try content resolver first (for content:// URIs)
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        return it.getString(nameIndex) ?: uri.lastPathSegment ?: "Unknown"
                    }
                }
            }
        }

        // Fallback to path segment
        return uri.lastPathSegment ?: "Unknown"
    }

    /**
     * Gets the file size from the URI.
     */
    private fun getFileSize(uri: Uri): Long {
        // Try content resolver first
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !it.isNull(sizeIndex)) {
                        return it.getLong(sizeIndex)
                    }
                }
            }
        }

        // Fallback to FileChannel
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { fis ->
                    fis.channel.size()
                }
            } ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Checks if the file is writable.
     */
    private fun isWritable(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "w")?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
