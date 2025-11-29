package notebad.prabe.sh.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import notebad.prabe.sh.core.model.DetectedFileType
import notebad.prabe.sh.core.model.FileMetadata
import notebad.prabe.sh.ui.state.ViewMode

/**
 * Top app bar for the file viewer.
 *
 * @param metadata File metadata to display
 * @param viewMode Current view mode
 * @param isModified Whether the file has unsaved changes
 * @param onNavigateBack Callback for back navigation
 * @param onSave Callback to save the file
 * @param onReload Callback to reload the file
 * @param onViewModeChange Callback to change view mode
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerTopBar(
    metadata: FileMetadata,
    viewMode: ViewMode,
    isModified: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onReload: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showViewModeMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = metadata.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                if (isModified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = metadata.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.height(48.dp), // Compact height
        actions = {
            // View mode toggle
            IconButton(onClick = { showViewModeMenu = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = viewMode.icon,
                    contentDescription = "Change view mode",
                    modifier = Modifier.size(20.dp)
                )

                DropdownMenu(
                    expanded = showViewModeMenu,
                    onDismissRequest = { showViewModeMenu = false }
                ) {
                    ViewModeMenuItem(
                        mode = ViewMode.HEX,
                        isSelected = viewMode == ViewMode.HEX,
                        onClick = {
                            onViewModeChange(ViewMode.HEX)
                            showViewModeMenu = false
                        }
                    )
                    ViewModeMenuItem(
                        mode = ViewMode.TEXT,
                        isSelected = viewMode == ViewMode.TEXT,
                        onClick = {
                            onViewModeChange(ViewMode.TEXT)
                            showViewModeMenu = false
                        }
                    )
                    ViewModeMenuItem(
                        mode = ViewMode.CODE,
                        isSelected = viewMode == ViewMode.CODE,
                        onClick = {
                            onViewModeChange(ViewMode.CODE)
                            showViewModeMenu = false
                        }
                    )
                    if (metadata.detectedType == DetectedFileType.MARKDOWN) {
                        ViewModeMenuItem(
                            mode = ViewMode.MARKDOWN_PREVIEW,
                            isSelected = viewMode == ViewMode.MARKDOWN_PREVIEW,
                            onClick = {
                                onViewModeChange(ViewMode.MARKDOWN_PREVIEW)
                                showViewModeMenu = false
                            }
                        )
                        ViewModeMenuItem(
                            mode = ViewMode.MARKDOWN_SOURCE,
                            isSelected = viewMode == ViewMode.MARKDOWN_SOURCE,
                            onClick = {
                                onViewModeChange(ViewMode.MARKDOWN_SOURCE)
                                showViewModeMenu = false
                            }
                        )
                    }
                }
            }

            // Save button (only for editable modes)
            if (viewMode.isEditable && !metadata.isReadOnly) {
                IconButton(
                    onClick = onSave,
                    enabled = isModified,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(20.dp),
                        tint = if (isModified) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // More options menu
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier.size(20.dp)
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Reload") },
                        onClick = {
                            onReload()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ViewModeMenuItem(
    mode: ViewMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = mode.displayName,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    )
}

/**
 * Extension properties for ViewMode.
 */
val ViewMode.displayName: String
    get() = when (this) {
        ViewMode.HEX -> "Hex View"
        ViewMode.TEXT -> "Text"
        ViewMode.CODE -> "Code"
        ViewMode.MARKDOWN_PREVIEW -> "Preview"
        ViewMode.MARKDOWN_SOURCE -> "Source"
    }

val ViewMode.icon: ImageVector
    get() = when (this) {
        ViewMode.HEX -> Icons.Default.DataObject
        ViewMode.TEXT -> Icons.Default.Description
        ViewMode.CODE -> Icons.Default.Code
        ViewMode.MARKDOWN_PREVIEW -> Icons.Default.Preview
        ViewMode.MARKDOWN_SOURCE -> Icons.Default.Edit
    }

val ViewMode.isEditable: Boolean
    get() = this in listOf(ViewMode.TEXT, ViewMode.CODE, ViewMode.MARKDOWN_SOURCE)
