package notebad.prabe.sh.ui.screens

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
import androidx.compose.material3.Surface
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
import notebad.prabe.sh.core.io.LargeFileRepository
import notebad.prabe.sh.ui.components.CodeViewer
import notebad.prabe.sh.ui.components.EmptyState
import notebad.prabe.sh.ui.components.ErrorScreen
import notebad.prabe.sh.ui.components.FileViewerTopBar
import notebad.prabe.sh.ui.components.HexViewer
import notebad.prabe.sh.ui.components.LoadingScreen
import notebad.prabe.sh.ui.components.MarkdownPreview
import notebad.prabe.sh.ui.components.TextEditor
import notebad.prabe.sh.ui.components.TruncationBanner
import notebad.prabe.sh.ui.state.ContentState
import notebad.prabe.sh.ui.state.FileViewerEffect
import notebad.prabe.sh.ui.state.FileViewerEvent
import notebad.prabe.sh.ui.state.FileViewerUiState
import notebad.prabe.sh.ui.state.ViewMode
import notebad.prabe.sh.ui.viewmodel.FileViewerViewModel

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
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

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
                    EmptyState(onOpenFile = onOpenFile)
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
                        }
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
    onNavigateBack: () -> Unit
) {
    val isModified = when (val content = state.contentState) {
        is ContentState.TextContent -> content.isModified
        else -> false
    }
    
    val wordWrapEnabled = when (val content = state.contentState) {
        is ContentState.TextContent -> content.isWordWrapEnabled
        else -> true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar
        FileViewerTopBar(
            metadata = state.metadata,
            viewMode = state.viewMode,
            isModified = isModified,
            wordWrapEnabled = wordWrapEnabled,
            onNavigateBack = onNavigateBack,
            onSave = { onEvent(FileViewerEvent.SaveFile) },
            onReload = { onEvent(FileViewerEvent.ReloadFile) },
            onViewModeChange = { onEvent(FileViewerEvent.ChangeViewMode(it)) },
            onToggleWordWrap = { onEvent(FileViewerEvent.ToggleWordWrap) }
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
                    LoadingScreen(message = "Loading content...")
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
                        onTogglePreview = { onEvent(FileViewerEvent.ToggleMarkdownPreview) }
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
    onTogglePreview: () -> Unit
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
                language = null, // No syntax highlighting in text mode
                showLineNumbers = false, // No line numbers in text mode
                wordWrapEnabled = wordWrapEnabled,
                modifier = Modifier.fillMaxSize()
            )
        }

        ViewMode.CODE -> {
            TextEditor(
                text = content.text,
                onTextChange = onTextChange,
                isReadOnly = isReadOnly,
                language = content.language,
                showLineNumbers = true, // Line numbers only in code mode
                wordWrapEnabled = wordWrapEnabled,
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
                showLineNumbers = false, // No line numbers for markdown source
                wordWrapEnabled = wordWrapEnabled,
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
