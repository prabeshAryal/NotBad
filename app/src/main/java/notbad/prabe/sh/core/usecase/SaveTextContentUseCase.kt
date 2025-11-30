package notbad.prabe.sh.core.usecase

import android.net.Uri
import notbad.prabe.sh.core.io.LargeFileRepository

/**
 * Use case for saving text content to a file.
 */
class SaveTextContentUseCase(
    private val repository: LargeFileRepository
) {
    /**
     * Saves text content to a file.
     *
     * @param uri The file URI
     * @param content The text content to save
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(uri: Uri, content: String): Result<Unit> {
        return repository.writeTextContent(uri, content)
    }
}
