package notbad.prabe.sh.core.model

/**
 * Represents a line of hex data for display in the hex viewer.
 * Each line shows 16 bytes in hex format with ASCII representation.
 *
 * @property lineIndex The sequential line number (0-based)
 * @property offset The byte offset at the start of this line
 * @property hexBytes Array of hex string representations (e.g., "0A", "FF")
 * @property asciiRepresentation Printable ASCII representation of the bytes
 */
data class HexLine(
    val lineIndex: Long,
    val offset: Long,
    val hexBytes: List<String>,
    val asciiRepresentation: String
) {
    /**
     * The formatted offset string (8 characters, zero-padded hex)
     */
    val formattedOffset: String
        get() = "%08X".format(offset)

    /**
     * Number of actual bytes in this line (may be less than 16 for last line)
     */
    val byteCount: Int
        get() = hexBytes.size

    companion object {
        /**
         * Standard number of bytes per hex line
         */
        const val BYTES_PER_LINE = 16

        /**
         * Creates a HexLine from raw byte data
         *
         * @param lineIndex The line number
         * @param offset The byte offset
         * @param bytes The raw bytes for this line
         */
        fun fromBytes(lineIndex: Long, offset: Long, bytes: ByteArray): HexLine {
            val hexBytes = bytes.map { byte -> "%02X".format(byte) }
            val ascii = bytes.map { byte ->
                val char = byte.toInt() and 0xFF
                if (char in 32..126) char.toChar() else '.'
            }.joinToString("")

            return HexLine(
                lineIndex = lineIndex,
                offset = offset,
                hexBytes = hexBytes,
                asciiRepresentation = ascii
            )
        }

        /**
         * Converts a chunk of data into multiple HexLines
         *
         * @param chunk The file chunk to convert
         * @param startLineIndex The starting line index for this batch
         */
        fun fromChunk(chunk: FileChunk, startLineIndex: Long): List<HexLine> {
            val lines = mutableListOf<HexLine>()
            var currentOffset = chunk.offset
            var lineIndex = startLineIndex

            val data = chunk.data
            var i = 0
            while (i < data.size) {
                val endIndex = minOf(i + BYTES_PER_LINE, data.size)
                val lineBytes = data.copyOfRange(i, endIndex)
                lines.add(fromBytes(lineIndex, currentOffset, lineBytes))

                currentOffset += lineBytes.size
                lineIndex++
                i = endIndex
            }

            return lines
        }
    }
}
