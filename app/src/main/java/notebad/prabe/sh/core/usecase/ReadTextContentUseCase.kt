package notebad.prabe.sh.core.usecase

import android.net.Uri
import notebad.prabe.sh.core.io.LargeFileRepository

/**
 * Use case for reading text content from a file.
 * Respects maximum size limits to prevent OOM.
 */
class ReadTextContentUseCase(
    private val repository: LargeFileRepository
) {
    /**
     * Reads text content from a file.
     *
     * @param uri The file URI
     * @param maxSize Maximum bytes to read (default 1MB)
     * @return Result containing text content or error
     */
    suspend operator fun invoke(
        uri: Uri,
        maxSize: Long = LargeFileRepository.MAX_FULL_TEXT_LOAD_SIZE
    ): Result<String> {
        return repository.readTextContent(uri, maxSize)
    }
}
