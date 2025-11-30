package notbad.prabe.sh.core.model

/**
 * Represents a chunk of data read from a file.
 * This is the fundamental unit of data transfer in our memory-safe I/O system.
 *
 * @property offset The byte offset in the file where this chunk starts
 * @property data The actual byte data of this chunk
 * @property chunkIndex The sequential index of this chunk (0-based)
 * @property isLastChunk Whether this is the final chunk of the file
 */
data class FileChunk(
    val offset: Long,
    val data: ByteArray,
    val chunkIndex: Long,
    val isLastChunk: Boolean
) {
    /**
     * The actual size of data in this chunk (may be less than standard chunk size for last chunk)
     */
    val size: Int get() = data.size

    /**
     * The ending offset (exclusive) of this chunk
     */
    val endOffset: Long get() = offset + size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileChunk

        if (offset != other.offset) return false
        if (!data.contentEquals(other.data)) return false
        if (chunkIndex != other.chunkIndex) return false
        if (isLastChunk != other.isLastChunk) return false

        return true
    }

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + chunkIndex.hashCode()
        result = 31 * result + isLastChunk.hashCode()
        return result
    }

    companion object {
        /**
         * Creates an empty chunk, typically used for error states
         */
        fun empty(chunkIndex: Long = 0): FileChunk = FileChunk(
            offset = 0,
            data = ByteArray(0),
            chunkIndex = chunkIndex,
            isLastChunk = true
        )
    }
}
