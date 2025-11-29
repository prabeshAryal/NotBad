package notebad.prabe.sh.core.io

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import notebad.prabe.sh.core.model.FileChunk
import notebad.prabe.sh.core.model.HexLine

/**
 * Data source for paginated hex viewing.
 * Provides hex lines on-demand for use with LazyColumn pagination.
 * 
 * This class converts raw file chunks into formatted hex display lines,
 * handling the conversion from bytes to hex strings and ASCII representation.
 */
class HexDataSource(
    private val repository: LargeFileRepository,
    private val uri: Uri,
    private val fileSize: Long
) {
    companion object {
        /**
         * Number of hex lines per page
         * 16 bytes per line, ~50 lines visible, load 100 for smooth scrolling
         */
        const val LINES_PER_PAGE = 100

        /**
         * Bytes per hex line
         */
        const val BYTES_PER_LINE = HexLine.BYTES_PER_LINE
    }

    /**
     * Total number of hex lines in the file
     */
    val totalLines: Long
        get() = if (fileSize <= 0) 0 else (fileSize + BYTES_PER_LINE - 1) / BYTES_PER_LINE

    /**
     * Loads a page of hex lines.
     *
     * @param pageIndex 0-based page index
     * @param linesPerPage Number of lines per page
     * @return List of HexLines for the requested page
     */
    suspend fun loadPage(
        pageIndex: Int,
        linesPerPage: Int = LINES_PER_PAGE
    ): List<HexLine> = withContext(Dispatchers.IO) {
        val startLine = pageIndex.toLong() * linesPerPage
        val startOffset = startLine * BYTES_PER_LINE
        val bytesToRead = linesPerPage * BYTES_PER_LINE

        if (startOffset >= fileSize) {
            return@withContext emptyList()
        }

        val result = repository.readRange(uri, startOffset, bytesToRead)
        result.getOrNull()?.let { data ->
            convertToHexLines(data, startLine, startOffset)
        } ?: emptyList()
    }

    /**
     * Loads hex lines for a specific range.
     *
     * @param startLine Starting line index
     * @param endLine Ending line index (exclusive)
     * @return List of HexLines for the requested range
     */
    suspend fun loadLineRange(
        startLine: Long,
        endLine: Long
    ): List<HexLine> = withContext(Dispatchers.IO) {
        val actualStartLine = maxOf(0, startLine)
        val actualEndLine = minOf(endLine, totalLines)

        if (actualStartLine >= actualEndLine) {
            return@withContext emptyList()
        }

        val startOffset = actualStartLine * BYTES_PER_LINE
        val bytesToRead = ((actualEndLine - actualStartLine) * BYTES_PER_LINE).toInt()

        if (startOffset >= fileSize) {
            return@withContext emptyList()
        }

        val result = repository.readRange(uri, startOffset, bytesToRead)
        result.getOrNull()?.let { data ->
            convertToHexLines(data, actualStartLine, startOffset)
        } ?: emptyList()
    }

    /**
     * Gets a single hex line by index.
     *
     * @param lineIndex The line index to retrieve
     * @return HexLine or null if out of range
     */
    suspend fun getLine(lineIndex: Long): HexLine? = withContext(Dispatchers.IO) {
        if (lineIndex < 0 || lineIndex >= totalLines) {
            return@withContext null
        }

        val offset = lineIndex * BYTES_PER_LINE
        val result = repository.readRange(uri, offset, BYTES_PER_LINE)

        result.getOrNull()?.takeIf { it.isNotEmpty() }?.let { data ->
            HexLine.fromBytes(lineIndex, offset, data)
        }
    }

    /**
     * Converts raw bytes into a list of HexLines.
     */
    private fun convertToHexLines(
        data: ByteArray,
        startLineIndex: Long,
        startOffset: Long
    ): List<HexLine> {
        val lines = mutableListOf<HexLine>()
        var currentOffset = startOffset
        var lineIndex = startLineIndex

        var i = 0
        while (i < data.size) {
            val endIndex = minOf(i + BYTES_PER_LINE, data.size)
            val lineBytes = data.copyOfRange(i, endIndex)
            lines.add(HexLine.fromBytes(lineIndex, currentOffset, lineBytes))

            currentOffset += lineBytes.size
            lineIndex++
            i = endIndex
        }

        return lines
    }

    /**
     * Searches for a byte pattern and returns matching line indices.
     *
     * @param pattern Byte pattern to search for
     * @param maxResults Maximum number of results to return
     * @return List of line indices containing the pattern
     */
    suspend fun searchBytes(
        pattern: ByteArray,
        maxResults: Int = 100
    ): List<Long> = withContext(Dispatchers.IO) {
        if (pattern.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<Long>()
        var offset = 0L
        val chunkSize = LargeFileRepository.DEFAULT_CHUNK_SIZE

        while (offset < fileSize && results.size < maxResults) {
            val result = repository.readRange(uri, offset, chunkSize)
            val data = result.getOrNull() ?: break

            // Search for pattern in this chunk
            var searchIndex = 0
            while (searchIndex <= data.size - pattern.size && results.size < maxResults) {
                if (matchesPattern(data, searchIndex, pattern)) {
                    val matchOffset = offset + searchIndex
                    val lineIndex = matchOffset / BYTES_PER_LINE
                    if (results.isEmpty() || results.last() != lineIndex) {
                        results.add(lineIndex)
                    }
                }
                searchIndex++
            }

            offset += chunkSize - pattern.size + 1 // Overlap to catch patterns spanning chunks
        }

        results
    }

    private fun matchesPattern(data: ByteArray, startIndex: Int, pattern: ByteArray): Boolean {
        for (i in pattern.indices) {
            if (data[startIndex + i] != pattern[i]) {
                return false
            }
        }
        return true
    }
}
