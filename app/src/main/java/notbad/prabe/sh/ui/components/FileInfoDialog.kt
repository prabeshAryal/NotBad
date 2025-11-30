package notbad.prabe.sh.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import notbad.prabe.sh.core.model.DetectedFileType
import notbad.prabe.sh.core.model.FileMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File info dialog showing metadata about the file.
 */
@Composable
fun FileInfoDialog(
    metadata: FileMetadata,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "File Info",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // File name
                    InfoRow(label = "Name", value = metadata.displayName)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // File size
                    InfoRow(label = "Size", value = formatFileSize(metadata.size))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // File type
                    InfoRow(label = "Type", value = getFileTypeDescription(metadata.detectedType))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // MIME type
                    InfoRow(label = "MIME Type", value = metadata.mimeType ?: "Unknown")
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Extension
                    InfoRow(label = "Extension", value = metadata.extension.ifEmpty { "None" })
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Last modified
                    InfoRow(
                        label = "Modified", 
                        value = formatDateTime(metadata.lastModified)
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Read-only status
                    InfoRow(
                        label = "Access", 
                        value = if (metadata.isReadOnly) "Read Only" else "Read/Write"
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // URI (for debugging)
                    InfoRow(
                        label = "Location", 
                        value = metadata.uri.path ?: metadata.uri.toString(),
                        isMultiLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isMultiLine: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = if (isMultiLine) 3 else 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getFileTypeDescription(type: DetectedFileType): String {
    return when (type) {
        DetectedFileType.TEXT -> "Plain Text"
        DetectedFileType.SOURCE_CODE -> "Source Code"
        DetectedFileType.MARKDOWN -> "Markdown Document"
        DetectedFileType.BINARY -> "Binary File"
        DetectedFileType.UNKNOWN -> "Unknown"
    }
}

private fun formatDateTime(timestamp: Long): String {
    return if (timestamp > 0) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timestamp))
    } else {
        "Unknown"
    }
}
