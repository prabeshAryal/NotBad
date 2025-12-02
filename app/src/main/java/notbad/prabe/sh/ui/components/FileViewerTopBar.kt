package notbad.prabe.sh.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import notbad.prabe.sh.core.model.DetectedFileType
import notbad.prabe.sh.core.model.FileMetadata
import notbad.prabe.sh.ui.state.ViewMode

/**
 * Compact top app bar for the file viewer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerTopBar(
    metadata: FileMetadata,
    viewMode: ViewMode,
    isModified: Boolean,
    wordWrapEnabled: Boolean = true,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onReload: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onToggleWordWrap: () -> Unit = {},
    onToggleSearch: () -> Unit = {},
    onShowFileInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showViewModeMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Truncated filename with ellipsis
                Text(
                    text = truncateFilename(metadata.displayName, 20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                if (isModified) {
                    Text(
                        text = " •",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
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
        modifier = modifier,
        actions = {
            // Search button
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // View mode toggle
            IconButton(
                onClick = { showViewModeMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = viewMode.icon,
                    contentDescription = "Change view mode",
                    modifier = Modifier.size(18.dp)
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
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp),
                        tint = if (isModified) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // More options menu
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    modifier = Modifier.size(18.dp)
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Word Wrap toggle
                    DropdownMenuItem(
                        text = { Text("Word Wrap") },
                        onClick = {
                            onToggleWordWrap()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = null)
                        },
                        trailingIcon = {
                            if (wordWrapEnabled) {
                                Icon(
                                    Icons.Default.Check, 
                                    contentDescription = "Enabled",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    
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
                    
                    HorizontalDivider()
                    
                    // File Info
                    DropdownMenuItem(
                        text = { Text("File Info") },
                        onClick = {
                            onShowFileInfo()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                }
            }
        }
    )
}

/**
 * Truncate filename with ellipsis in middle
 */
private fun truncateFilename(name: String, maxLength: Int): String {
    if (name.length <= maxLength) return name
    val extension = name.substringAfterLast('.', "")
    val baseName = if (extension.isNotEmpty()) name.substringBeforeLast('.') else name
    
    val availableLength = maxLength - extension.length - 4 // 4 for "..." and "."
    if (availableLength < 4) return name.take(maxLength - 3) + "..."
    
    val half = availableLength / 2
    return if (extension.isNotEmpty()) {
        "${baseName.take(half)}...${baseName.takeLast(half)}.$extension"
    } else {
        "${baseName.take(half)}...${baseName.takeLast(half)}"
    }
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
        ViewMode.HEX -> "Hex"
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
