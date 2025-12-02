package notbad.prabe.sh.ui.state

import notbad.prabe.sh.core.model.FileMetadata
import notbad.prabe.sh.core.model.HexLine

/**
 * Represents the different viewing modes for the file viewer.
 */
enum class ViewMode {
    /**
     * Hex dump view for binary files
     */
    HEX,

    /**
     * Plain text editor view
     */
    TEXT,

    /**
     * Syntax-highlighted code view
     */
    CODE,

    /**
     * Rendered Markdown preview
     */
    MARKDOWN_PREVIEW,

    /**
     * Raw Markdown editor (source view)
     */
    MARKDOWN_SOURCE,


}

/**
 * Represents the overall UI state for the file viewer.
 */
sealed interface FileViewerUiState {
    /**
     * Initial state - no file loaded
     */
    data class Idle(
        val recentFiles: List<String> = emptyList()
    ) : FileViewerUiState

    /**
     * Loading state - file is being opened/analyzed
     */
    data class Loading(
        val message: String = "Opening file...",
        val progress: Float = 0f // 0.0 to 1.0
    ) : FileViewerUiState

    /**
     * File loaded successfully
     */
    data class Loaded(
        val metadata: FileMetadata,
        val viewMode: ViewMode,
        val contentState: ContentState
    ) : FileViewerUiState

    /**
     * Error state
     */
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : FileViewerUiState
}

/**
 * Represents the state of the actual content being displayed.
 */
sealed interface ContentState {
    /**
     * Content is loading with progress
     */
    data class Loading(
        val progress: Float = 0f, // 0.0 to 1.0
        val loadedBytes: Long = 0,
        val totalBytes: Long = 0
    ) : ContentState

    /**
     * Text content loaded (for text/code/markdown modes)
     */
    data class TextContent(
        val text: String,
        val isModified: Boolean = false,
        val cursorPosition: Int = 0,
        val language: String? = null,
        val isTruncated: Boolean = false,
        val totalSize: Long = 0,
        val isWordWrapEnabled: Boolean = true,
        val showLineNumbers: Boolean = false,
        val isSearchVisible: Boolean = false,
        val searchQuery: String = ""
    ) : ContentState

    /**
     * Hex content loaded (for binary mode)
     */
    data class HexContent(
        val lines: List<HexLine>,
        val totalLines: Long,
        val currentPage: Int,
        val isLoadingMore: Boolean = false
    ) : ContentState

    /**
     * Error loading content
     */
    data class Error(
        val message: String
    ) : ContentState
}

/**
 * UI events that can be triggered by user actions.
 */
sealed interface FileViewerEvent {
    /**
     * User selected a different view mode
     */
    data class ChangeViewMode(val mode: ViewMode) : FileViewerEvent

    /**
     * User scrolled to request more hex lines
     */
    data class LoadMoreHexLines(val startLine: Long, val endLine: Long) : FileViewerEvent

    /**
     * User modified text content
     */
    data class TextChanged(val newText: String) : FileViewerEvent

    /**
     * User requested to save the file
     */
    data object SaveFile : FileViewerEvent

    /**
     * User requested to reload the file
     */
    data object ReloadFile : FileViewerEvent

    /**
     * User requested to toggle between preview and source (for markdown)
     */
    data object ToggleMarkdownPreview : FileViewerEvent

    /**
     * User toggled word wrap
     */
    data object ToggleWordWrap : FileViewerEvent

    /**
     * User toggled search visibility
     */
    data object ToggleSearch : FileViewerEvent

    /**
     * User changed search query
     */
    data class UpdateSearchQuery(val query: String) : FileViewerEvent

    /**
     * User searched for text/hex
     */
    data class Search(val query: String) : FileViewerEvent

    /**
     * User closed the file
     */
    data object CloseFile : FileViewerEvent
    
    /**
     * User wants to see file info
     */
    data object ShowFileInfo : FileViewerEvent

    /**
     * User toggled line numbers
     */
    data object ToggleLineNumbers : FileViewerEvent

    /**
     * User requested to create a new file
     */
    data class CreateFile(val callback: () -> Unit) : FileViewerEvent

    /**
     * User clicked a recent file
     */
    data class OpenRecentFile(val uri: String) : FileViewerEvent
}

/**
 * Side effects that the ViewModel can emit.
 */
sealed interface FileViewerEffect {
    /**
     * Show a toast/snackbar message
     */
    data class ShowMessage(val message: String) : FileViewerEffect

    /**
     * File saved successfully
     */
    data object FileSaved : FileViewerEffect

    /**
     * Navigate back (close activity)
     */
    data object NavigateBack : FileViewerEffect

    /**
     * Show unsaved changes dialog
     */
    data object ShowUnsavedChangesDialog : FileViewerEffect
    
    /**
     * Show file info dialog
     */
    data class ShowFileInfo(val metadata: FileMetadata) : FileViewerEffect
}
