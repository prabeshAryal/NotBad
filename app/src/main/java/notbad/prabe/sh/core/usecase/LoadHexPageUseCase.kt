package notbad.prabe.sh.core.usecase

import android.net.Uri
import notbad.prabe.sh.core.io.HexDataSource
import notbad.prabe.sh.core.io.LargeFileRepository
import notbad.prabe.sh.core.model.HexLine

/**
 * Use case for loading paginated hex data.
 * Provides on-demand loading of hex lines for large file viewing.
 */
class LoadHexPageUseCase(
    private val repository: LargeFileRepository
) {
    /**
     * Creates a HexDataSource for the given file.
     *
     * @param uri The file URI
     * @param fileSize The total file size
     * @return HexDataSource for paginated access
     */
    fun createDataSource(uri: Uri, fileSize: Long): HexDataSource {
        return HexDataSource(repository, uri, fileSize)
    }

    /**
     * Loads a page of hex lines.
     *
     * @param dataSource The hex data source
     * @param pageIndex The page index to load
     * @return List of HexLines
     */
    suspend operator fun invoke(
        dataSource: HexDataSource,
        pageIndex: Int
    ): List<HexLine> {
        return dataSource.loadPage(pageIndex)
    }

    /**
     * Loads a range of hex lines.
     *
     * @param dataSource The hex data source
     * @param startLine Starting line index
     * @param endLine Ending line index (exclusive)
     * @return List of HexLines
     */
    suspend fun loadRange(
        dataSource: HexDataSource,
        startLine: Long,
        endLine: Long
    ): List<HexLine> {
        return dataSource.loadLineRange(startLine, endLine)
    }
}
