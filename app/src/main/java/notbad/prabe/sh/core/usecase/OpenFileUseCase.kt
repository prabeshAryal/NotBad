package notbad.prabe.sh.core.usecase

import android.net.Uri
import notbad.prabe.sh.core.io.LargeFileRepository
import notbad.prabe.sh.core.model.FileMetadata

/**
 * Use case for opening a file and retrieving its metadata.
 * This is the entry point for all file operations.
 */
class OpenFileUseCase(
    private val repository: LargeFileRepository
) {
    /**
     * Opens a file and returns its metadata.
     * Does not load file content - only retrieves metadata for UI decisions.
     *
     * @param uri The file URI to open
     * @return Result containing FileMetadata or error
     */
    suspend operator fun invoke(uri: Uri): Result<FileMetadata> {
        return repository.getFileMetadata(uri)
    }
}
