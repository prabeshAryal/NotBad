package notebad.prabe.sh.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import notebad.prabe.sh.core.io.HexDataSource
import notebad.prabe.sh.core.io.LargeFileRepository
import notebad.prabe.sh.core.model.DetectedFileType
import notebad.prabe.sh.core.model.FileMetadata
import notebad.prabe.sh.core.model.HexLine
import notebad.prabe.sh.core.usecase.LoadHexPageUseCase
import notebad.prabe.sh.core.usecase.OpenFileUseCase
import notebad.prabe.sh.core.usecase.ReadTextContentUseCase
import notebad.prabe.sh.core.usecase.SaveTextContentUseCase
import notebad.prabe.sh.ui.state.ContentState
import notebad.prabe.sh.ui.state.FileViewerEffect
import notebad.prabe.sh.ui.state.FileViewerEvent
import notebad.prabe.sh.ui.state.FileViewerUiState
import notebad.prabe.sh.ui.state.ViewMode

/**
 * ViewModel for the file viewer/editor screen.
 * 
 * Handles:
 * - Opening files from URIs (content:// and file://)
 * - Determining appropriate view mode based on file type
 * - Managing memory-safe loading of large files
 * - Text editing with modification tracking
 * - Saving changes back to the file
 * 
 * All file I/O is performed on background threads via the repository layer.
 */
class FileViewerViewModel(
    private val repository: LargeFileRepository
) : ViewModel() {

    // Use cases
    private val openFileUseCase = OpenFileUseCase(repository)
    private val readTextContentUseCase = ReadTextContentUseCase(repository)
    private val saveTextContentUseCase = SaveTextContentUseCase(repository)
    private val loadHexPageUseCase = LoadHexPageUseCase(repository)

    // UI State
    private val _uiState = MutableStateFlow<FileViewerUiState>(FileViewerUiState.Idle)
    val uiState: StateFlow<FileViewerUiState> = _uiState.asStateFlow()

    // One-time effects (toasts, navigation, etc.)
    private val _effects = MutableSharedFlow<FileViewerEffect>()
    val effects: SharedFlow<FileViewerEffect> = _effects.asSharedFlow()

    // Current file metadata
    private var currentMetadata: FileMetadata? = null

    // Current file URI
    private var currentUri: Uri? = null

    // Original text content (for detecting modifications)
    private var originalTextContent: String? = null

    // Hex data source for paginated viewing
    private var hexDataSource: HexDataSource? = null

    // Loaded hex lines cache
    private val loadedHexLines = mutableListOf<HexLine>()

    /**
     * Opens a file from the given URI.
     * This is the main entry point when receiving an intent.
     *
     * @param uri The file URI (content:// or file://)
     */
    fun openFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = FileViewerUiState.Loading("Analyzing file...")

            currentUri = uri

            openFileUseCase(uri)
                .onSuccess { metadata ->
                    currentMetadata = metadata
                    loadContentForType(metadata)
                }
                .onFailure { error ->
                    _uiState.value = FileViewerUiState.Error(
                        message = "Failed to open file: ${error.message}",
                        exception = error
                    )
                }
        }
    }

    /**
     * Handles UI events from the Compose layer.
     */
    fun onEvent(event: FileViewerEvent) {
        when (event) {
            is FileViewerEvent.ChangeViewMode -> changeViewMode(event.mode)
            is FileViewerEvent.LoadMoreHexLines -> loadMoreHexLines(event.startLine, event.endLine)
            is FileViewerEvent.TextChanged -> onTextChanged(event.newText)
            is FileViewerEvent.SaveFile -> saveFile()
            is FileViewerEvent.ReloadFile -> reloadFile()
            is FileViewerEvent.ToggleMarkdownPreview -> toggleMarkdownPreview()
            is FileViewerEvent.ToggleWordWrap -> toggleWordWrap()
            is FileViewerEvent.ToggleSearch -> toggleSearch()
            is FileViewerEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is FileViewerEvent.Search -> performSearch(event.query)
            is FileViewerEvent.CloseFile -> closeFile()
        }
    }

    /**
     * Loads content based on the detected file type.
     */
    private suspend fun loadContentForType(metadata: FileMetadata) {
        val viewMode = determineViewMode(metadata.detectedType)

        when (viewMode) {
            ViewMode.HEX -> loadHexContent(metadata)
            ViewMode.TEXT, ViewMode.CODE, ViewMode.MARKDOWN_SOURCE, ViewMode.MARKDOWN_PREVIEW -> {
                loadTextContent(metadata, viewMode)
            }
        }
    }

    /**
     * Determines the initial view mode based on file type.
     */
    private fun determineViewMode(fileType: DetectedFileType): ViewMode {
        return when (fileType) {
            DetectedFileType.BINARY, DetectedFileType.UNKNOWN -> ViewMode.HEX
            DetectedFileType.TEXT -> ViewMode.TEXT
            DetectedFileType.SOURCE_CODE -> ViewMode.CODE
            DetectedFileType.MARKDOWN -> ViewMode.MARKDOWN_PREVIEW
        }
    }

    /**
     * Loads text content from the file.
     */
    private suspend fun loadTextContent(metadata: FileMetadata, viewMode: ViewMode) {
        _uiState.value = FileViewerUiState.Loaded(
            metadata = metadata,
            viewMode = viewMode,
            contentState = ContentState.Loading
        )

        currentUri?.let { uri ->
            readTextContentUseCase(uri)
                .onSuccess { text ->
                    originalTextContent = text
                    val isTruncated = metadata.size > LargeFileRepository.MAX_FULL_TEXT_LOAD_SIZE

                    val language = determineLanguage(metadata)

                    _uiState.value = FileViewerUiState.Loaded(
                        metadata = metadata,
                        viewMode = viewMode,
                        contentState = ContentState.TextContent(
                            text = text,
                            isModified = false,
                            language = language,
                            isTruncated = isTruncated,
                            totalSize = metadata.size
                        )
                    )
                }
                .onFailure { error ->
                    _uiState.value = FileViewerUiState.Loaded(
                        metadata = metadata,
                        viewMode = viewMode,
                        contentState = ContentState.Error("Failed to load content: ${error.message}")
                    )
                }
        }
    }

    /**
     * Loads initial hex content from the file.
     */
    private suspend fun loadHexContent(metadata: FileMetadata) {
        _uiState.value = FileViewerUiState.Loaded(
            metadata = metadata,
            viewMode = ViewMode.HEX,
            contentState = ContentState.Loading
        )

        currentUri?.let { uri ->
            // Create hex data source
            val dataSource = loadHexPageUseCase.createDataSource(uri, metadata.size)
            hexDataSource = dataSource

            // Load initial page
            val initialLines = withContext(Dispatchers.IO) {
                dataSource.loadPage(0)
            }

            loadedHexLines.clear()
            loadedHexLines.addAll(initialLines)

            _uiState.value = FileViewerUiState.Loaded(
                metadata = metadata,
                viewMode = ViewMode.HEX,
                contentState = ContentState.HexContent(
                    lines = loadedHexLines.toList(),
                    totalLines = dataSource.totalLines,
                    currentPage = 0
                )
            )
        }
    }

    /**
     * Loads more hex lines for pagination.
     */
    private fun loadMoreHexLines(startLine: Long, endLine: Long) {
        val dataSource = hexDataSource ?: return
        val state = _uiState.value

        if (state !is FileViewerUiState.Loaded) return
        if (state.contentState !is ContentState.HexContent) return

        // Check if lines are already loaded
        val existingLineIndices = loadedHexLines.map { it.lineIndex }.toSet()
        val needToLoad = (startLine until endLine).any { it !in existingLineIndices }

        if (!needToLoad) return

        viewModelScope.launch {
            // Update state to show loading indicator
            _uiState.value = state.copy(
                contentState = (state.contentState as ContentState.HexContent).copy(
                    isLoadingMore = true
                )
            )

            val newLines = withContext(Dispatchers.IO) {
                dataSource.loadLineRange(startLine, endLine)
            }

            // Merge new lines with existing, avoiding duplicates
            newLines.forEach { line ->
                if (loadedHexLines.none { it.lineIndex == line.lineIndex }) {
                    loadedHexLines.add(line)
                }
            }

            // Sort by line index
            loadedHexLines.sortBy { it.lineIndex }

            _uiState.value = state.copy(
                contentState = ContentState.HexContent(
                    lines = loadedHexLines.toList(),
                    totalLines = dataSource.totalLines,
                    currentPage = (endLine / HexDataSource.LINES_PER_PAGE).toInt(),
                    isLoadingMore = false
                )
            )
        }
    }

    /**
     * Handles text content changes.
     */
    private fun onTextChanged(newText: String) {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return
        if (state.contentState !is ContentState.TextContent) return

        val isModified = newText != originalTextContent

        _uiState.value = state.copy(
            contentState = (state.contentState as ContentState.TextContent).copy(
                text = newText,
                isModified = isModified
            )
        )
    }

    /**
     * Changes the view mode.
     */
    private fun changeViewMode(mode: ViewMode) {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return

        val metadata = state.metadata

        viewModelScope.launch {
            when (mode) {
                ViewMode.HEX -> {
                    loadHexContent(metadata)
                }
                ViewMode.TEXT, ViewMode.CODE, ViewMode.MARKDOWN_SOURCE, ViewMode.MARKDOWN_PREVIEW -> {
                    // If switching from hex to text, reload text content
                    if (state.contentState is ContentState.HexContent) {
                        loadTextContent(metadata, mode)
                    } else {
                        // Just switch the mode, keep the content
                        _uiState.value = state.copy(viewMode = mode)
                    }
                }
            }
        }
    }

    /**
     * Toggles between markdown preview and source view.
     */
    private fun toggleMarkdownPreview() {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return

        val newMode = when (state.viewMode) {
            ViewMode.MARKDOWN_PREVIEW -> ViewMode.MARKDOWN_SOURCE
            ViewMode.MARKDOWN_SOURCE -> ViewMode.MARKDOWN_PREVIEW
            else -> return
        }

        _uiState.value = state.copy(viewMode = newMode)
    }

    /**
     * Toggles word wrap on/off for text content.
     */
    private fun toggleWordWrap() {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return
        if (state.contentState !is ContentState.TextContent) return

        val textContent = state.contentState as ContentState.TextContent
        _uiState.value = state.copy(
            contentState = textContent.copy(
                isWordWrapEnabled = !textContent.isWordWrapEnabled
            )
        )
    }

    /**
     * Toggles search visibility.
     */
    private fun toggleSearch() {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return
        if (state.contentState !is ContentState.TextContent) return

        val textContent = state.contentState as ContentState.TextContent
        _uiState.value = state.copy(
            contentState = textContent.copy(
                isSearchVisible = !textContent.isSearchVisible,
                searchQuery = if (textContent.isSearchVisible) "" else textContent.searchQuery
            )
        )
    }

    /**
     * Updates the search query.
     */
    private fun updateSearchQuery(query: String) {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return
        if (state.contentState !is ContentState.TextContent) return

        val textContent = state.contentState as ContentState.TextContent
        _uiState.value = state.copy(
            contentState = textContent.copy(
                searchQuery = query
            )
        )
    }

    /**
     * Saves the current text content to the file.
     */
    private fun saveFile() {
        val state = _uiState.value
        if (state !is FileViewerUiState.Loaded) return
        if (state.contentState !is ContentState.TextContent) return

        val uri = currentUri ?: return
        val content = (state.contentState as ContentState.TextContent).text

        viewModelScope.launch {
            saveTextContentUseCase(uri, content)
                .onSuccess {
                    originalTextContent = content
                    _uiState.value = state.copy(
                        contentState = (state.contentState as ContentState.TextContent).copy(
                            isModified = false
                        )
                    )
                    _effects.emit(FileViewerEffect.FileSaved)
                    _effects.emit(FileViewerEffect.ShowMessage("File saved successfully"))
                }
                .onFailure { error ->
                    _effects.emit(FileViewerEffect.ShowMessage("Failed to save: ${error.message}"))
                }
        }
    }

    /**
     * Reloads the current file.
     */
    private fun reloadFile() {
        currentUri?.let { openFile(it) }
    }

    /**
     * Performs a search in the current content.
     */
    private fun performSearch(query: String) {
        // TODO: Implement search functionality
        viewModelScope.launch {
            _effects.emit(FileViewerEffect.ShowMessage("Search functionality coming soon"))
        }
    }

    /**
     * Closes the current file.
     */
    private fun closeFile() {
        val state = _uiState.value
        if (state is FileViewerUiState.Loaded) {
            val contentState = state.contentState
            if (contentState is ContentState.TextContent && contentState.isModified) {
                viewModelScope.launch {
                    _effects.emit(FileViewerEffect.ShowUnsavedChangesDialog)
                }
                return
            }
        }

        viewModelScope.launch {
            _effects.emit(FileViewerEffect.NavigateBack)
        }
    }

    /**
     * Forces close without saving.
     */
    fun forceClose() {
        viewModelScope.launch {
            _effects.emit(FileViewerEffect.NavigateBack)
        }
    }

    /**
     * Checks if there are unsaved changes.
     */
    fun hasUnsavedChanges(): Boolean {
        val state = _uiState.value
        if (state is FileViewerUiState.Loaded) {
            val contentState = state.contentState
            if (contentState is ContentState.TextContent) {
                return contentState.isModified
            }
        }
        return false
    }

    /**
     * Determines the programming language for syntax highlighting.
     */
    private fun determineLanguage(metadata: FileMetadata): String? {
        return when (metadata.extension) {
            "kt", "kts" -> "kotlin"
            "java" -> "java"
            "js", "jsx" -> "javascript"
            "ts", "tsx" -> "typescript"
            "py" -> "python"
            "rb" -> "ruby"
            "go" -> "go"
            "rs" -> "rust"
            "c", "h" -> "c"
            "cpp", "cc", "cxx", "hpp" -> "cpp"
            "swift" -> "swift"
            "php" -> "php"
            "html", "htm" -> "html"
            "css" -> "css"
            "scss", "sass" -> "scss"
            "json" -> "json"
            "xml" -> "xml"
            "yaml", "yml" -> "yaml"
            "sql" -> "sql"
            "sh", "bash" -> "bash"
            "ps1" -> "powershell"
            "md", "markdown" -> "markdown"
            "gradle" -> "groovy"
            else -> null
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadedHexLines.clear()
        hexDataSource = null
    }
}
