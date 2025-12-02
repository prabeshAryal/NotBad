package notbad.prabe.sh.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.collectLatest
import notbad.prabe.sh.core.model.FileMetadata
import notbad.prabe.sh.ui.components.EmptyState
import notbad.prabe.sh.ui.components.ErrorScreen
import notbad.prabe.sh.ui.components.FileInfoDialog
import notbad.prabe.sh.ui.components.FileViewerTopBar
import notbad.prabe.sh.ui.components.HexViewer
import notbad.prabe.sh.ui.components.LoadingScreen
import notbad.prabe.sh.ui.components.LoadingScreenWithProgress
import notbad.prabe.sh.ui.components.MarkdownPreview
import notbad.prabe.sh.ui.components.TextEditor
import notbad.prabe.sh.ui.components.TruncationBanner
import notbad.prabe.sh.ui.state.ContentState
import notbad.prabe.sh.ui.state.FileViewerEffect
import notbad.prabe.sh.ui.state.FileViewerEvent
import notbad.prabe.sh.ui.state.FileViewerUiState
import notbad.prabe.sh.ui.state.ViewMode
import notbad.prabe.sh.ui.viewmodel.FileViewerViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Main file viewer/editor screen.
 * Handles all view modes and displays appropriate content based on file type.
 *
 * @param viewModel The FileViewerViewModel instance
 * @param onNavigateBack Callback for back navigation
 * @param onOpenFile Callback to open a file picker
 */
@Composable
fun FileViewerScreen(
    viewModel: FileViewerViewModel,
    onNavigateBack: () -> Unit,
    onOpenFile: () -> Unit,
    onCreateFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showFileInfoDialog by remember { mutableStateOf<FileMetadata?>(null) }

    // Handle back press with unsaved changes check
    BackHandler {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Collect side effects
    LaunchedEffect(Unit) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is FileViewerEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is FileViewerEffect.FileSaved -> {
                    // Already handled by ShowMessage
                }
                is FileViewerEffect.NavigateBack -> {
                    onNavigateBack()
                }
                is FileViewerEffect.ShowUnsavedChangesDialog -> {
                    showUnsavedChangesDialog = true
                }
                is FileViewerEffect.ShowFileInfo -> {
                    showFileInfoDialog = effect.metadata
                }
            }
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedChangesDialog = false },
            onDiscard = {
                showUnsavedChangesDialog = false
                viewModel.forceClose()
            },
            onSave = {
                showUnsavedChangesDialog = false
                viewModel.onEvent(FileViewerEvent.SaveFile)
            }
        )
    }
    
    // File info dialog
    showFileInfoDialog?.let { metadata ->
        FileInfoDialog(
            metadata = metadata,
            onDismiss = { showFileInfoDialog = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is FileViewerUiState.Idle -> {
                    EmptyState(
                        onOpenFile = onOpenFile,
                        onCreateFile = onCreateFile,
                        recentFiles = state.recentFiles,
                        onOpenRecentFile = { uriString ->
                            viewModel.onEvent(FileViewerEvent.OpenRecentFile(uriString))
                        }
                    )
                }

                is FileViewerUiState.Loading -> {
                    LoadingScreen(message = state.message)
                }

                is FileViewerUiState.Error -> {
                    ErrorScreen(
                        message = state.message,
                        onRetry = { viewModel.onEvent(FileViewerEvent.ReloadFile) }
                    )
                }

                is FileViewerUiState.Loaded -> {
                    FileViewerContent(
                        state = state,
                        onEvent = viewModel::onEvent,
                        onNavigateBack = {
                            if (viewModel.hasUnsavedChanges()) {
                                showUnsavedChangesDialog = true
                            } else {
                                onNavigateBack()
                            }
                        },
                        onShowFileInfo = { viewModel.onEvent(FileViewerEvent.ShowFileInfo) }
                    )
                }
            }
        }
    }
}

/**
 * Content for the loaded file state.
 */
@Composable
private fun FileViewerContent(
    state: FileViewerUiState.Loaded,
    onEvent: (FileViewerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onShowFileInfo: () -> Unit
) {
    val isModified = when (val content = state.contentState) {
        is ContentState.TextContent -> content.isModified
        else -> false
    }
    
    val wordWrapEnabled = when (val content = state.contentState) {
        is ContentState.TextContent -> content.isWordWrapEnabled
        else -> true
    }
    
    val showLineNumbers = when (val content = state.contentState) {
        is ContentState.TextContent -> content.showLineNumbers
        else -> false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar
        FileViewerTopBar(
            metadata = state.metadata,
            viewMode = state.viewMode,
            isModified = isModified,
            wordWrapEnabled = wordWrapEnabled,
            showLineNumbers = showLineNumbers,
            onNavigateBack = onNavigateBack,
            onSave = { onEvent(FileViewerEvent.SaveFile) },
            onReload = { onEvent(FileViewerEvent.ReloadFile) },
            onViewModeChange = { onEvent(FileViewerEvent.ChangeViewMode(it)) },
            onToggleWordWrap = { onEvent(FileViewerEvent.ToggleWordWrap) },
            onToggleSearch = { onEvent(FileViewerEvent.ToggleSearch) },
            onToggleLineNumbers = { onEvent(FileViewerEvent.ToggleLineNumbers) },
            onShowFileInfo = onShowFileInfo
        )

        // Truncation warning if applicable
        when (val content = state.contentState) {
            is ContentState.TextContent -> {
                if (content.isTruncated) {
                    TruncationBanner(
                        totalSize = content.totalSize,
                        loadedSize = content.text.length.toLong(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            else -> {}
        }

        // Main content area
        Box(modifier = Modifier.weight(1f)) {
            when (val content = state.contentState) {
                is ContentState.Loading -> {
                    LoadingScreenWithProgress(
                        progress = content.progress,
                        loadedBytes = content.loadedBytes,
                        totalBytes = content.totalBytes
                    )
                }

                is ContentState.Error -> {
                    ErrorScreen(
                        message = content.message,
                        onRetry = { onEvent(FileViewerEvent.ReloadFile) }
                    )
                }

                is ContentState.TextContent -> {
                    TextContentView(
                        content = content,
                        viewMode = state.viewMode,
                        isReadOnly = state.metadata.isReadOnly,
                        wordWrapEnabled = wordWrapEnabled,
                        onTextChange = { onEvent(FileViewerEvent.TextChanged(it)) },
                        onTogglePreview = { onEvent(FileViewerEvent.ToggleMarkdownPreview) },
                        onSearchQueryChange = { onEvent(FileViewerEvent.UpdateSearchQuery(it)) },
                        onToggleSearch = { onEvent(FileViewerEvent.ToggleSearch) }
                    )
                }

                is ContentState.HexContent -> {
                    HexViewer(
                        lines = content.lines,
                        totalLines = content.totalLines,
                        isLoadingMore = content.isLoadingMore,
                        onLoadMore = { start, end ->
                            onEvent(FileViewerEvent.LoadMoreHexLines(start, end))
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * View for text-based content (text, code, markdown).
 */
@Composable
private fun TextContentView(
    content: ContentState.TextContent,
    viewMode: ViewMode,
    isReadOnly: Boolean,
    wordWrapEnabled: Boolean,
    onTextChange: (String) -> Unit,
    onTogglePreview: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit
) {
    when (viewMode) {
        ViewMode.HEX -> {
            // This shouldn't happen, but handle gracefully
            Text(
                text = "Unexpected state: Text content in Hex view mode",
                color = MaterialTheme.colorScheme.error
            )
        }

        ViewMode.TEXT -> {
            TextEditor(
                text = content.text,
                onTextChange = onTextChange,
                isReadOnly = isReadOnly,
                language = null,
                showLineNumbers = content.showLineNumbers,
                wordWrapEnabled = wordWrapEnabled,
                searchQuery = content.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                showSearch = content.isSearchVisible,
                onToggleSearch = onToggleSearch,
                modifier = Modifier.fillMaxSize()
            )
        }

        ViewMode.CODE -> {
            // Auto-format JSON if in Code mode
            val displayText = remember(content.text, content.language) {
                if (content.language?.equals("json", ignoreCase = true) == true) {
                    try {
                        val tokener = JSONTokener(content.text)
                        val value = tokener.nextValue()
                        when (value) {
                            is JSONObject -> value.toString(4)
                            is JSONArray -> value.toString(4)
                            else -> content.text
                        }
                    } catch (e: Exception) {
                        content.text
                    }
                } else {
                    content.text
                }
            }

            TextEditor(
                text = displayText,
                onTextChange = onTextChange,
                isReadOnly = isReadOnly,
                language = content.language,
                showLineNumbers = content.showLineNumbers,
                wordWrapEnabled = wordWrapEnabled,
                searchQuery = content.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                showSearch = content.isSearchVisible,
                onToggleSearch = onToggleSearch,
                modifier = Modifier.fillMaxSize()
            )
        }

        ViewMode.MARKDOWN_PREVIEW -> {
            MarkdownPreview(
                markdown = content.text,
                modifier = Modifier.fillMaxSize()
            )
        }

        ViewMode.MARKDOWN_SOURCE -> {
            TextEditor(
                text = content.text,
                onTextChange = onTextChange,
                isReadOnly = isReadOnly,
                language = "markdown",
                showLineNumbers = content.showLineNumbers,
                wordWrapEnabled = wordWrapEnabled,
                searchQuery = content.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                showSearch = content.isSearchVisible,
                onToggleSearch = onToggleSearch,
                modifier = Modifier.fillMaxSize()
            )
        }


    }
}

/**
 * Dialog shown when user tries to leave with unsaved changes.
 */
@Composable
private fun UnsavedChangesDialog(
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unsaved Changes") },
        text = { Text("You have unsaved changes. What would you like to do?") },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard")
            }
        }
    )
}
