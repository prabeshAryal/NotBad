package notbad.prabe.sh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import notbad.prabe.sh.core.model.HexLine
import notbad.prabe.sh.ui.theme.HexAsciiColor
import notbad.prabe.sh.ui.theme.HexByteColor
import notbad.prabe.sh.ui.theme.HexNullByteColor
import notbad.prabe.sh.ui.theme.HexOffsetColor
import notbad.prabe.sh.ui.theme.HexTextStyle
import notbad.prabe.sh.ui.theme.MonospaceFamily

/**
 * Paginated hex viewer for binary files.
 * Uses LazyColumn for efficient rendering of large files.
 *
 * @param lines The currently loaded hex lines
 * @param totalLines Total number of lines in the file
 * @param isLoadingMore Whether more lines are currently being loaded
 * @param onLoadMore Callback to request more lines (startLine, endLine)
 * @param modifier Modifier for the component
 */
@Composable
fun HexViewer(
    lines: List<HexLine>,
    totalLines: Long,
    isLoadingMore: Boolean,
    onLoadMore: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    // Detect when user scrolls near the end and request more data
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) return@snapshotFlow null

            val lastVisibleIndex = visibleItems.last().index
            val totalItems = layoutInfo.totalItemsCount
            val buffer = 20 // Load more when 20 items from the end

            if (totalItems > 0 && lastVisibleIndex >= totalItems - buffer) {
                // Calculate the range to load
                val currentLastLine = lines.lastOrNull()?.lineIndex ?: 0
                val linesToLoad = 100L
                Pair(currentLastLine + 1, currentLastLine + 1 + linesToLoad)
            } else {
                null
            }
        }
            .distinctUntilChanged()
            .collect { range ->
                range?.let { (start, end) ->
                    if (start < totalLines && !isLoadingMore) {
                        onLoadMore(start, minOf(end, totalLines))
                    }
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header row
        HexHeader(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(horizontalScrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        HorizontalDivider()

        // Hex content
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(horizontalScrollState)
            ) {
                items(
                    count = lines.size,
                    key = { index -> lines[index].lineIndex }
                ) { index ->
                    HexLineRow(
                        hexLine = lines[index],
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // Loading indicator at the bottom
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // Status bar
        HexStatusBar(
            loadedLines = lines.size.toLong(),
            totalLines = totalLines,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/**
 * Header row showing column positions.
 */
@Composable
private fun HexHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        // Offset column header
        Text(
            text = "Offset",
            style = HexTextStyle,
            color = HexOffsetColor,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Hex column headers (00-0F)
        for (i in 0 until 16) {
            Text(
                text = "%02X".format(i),
                style = HexTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            if (i == 7) {
                Spacer(modifier = Modifier.width(8.dp)) // Gap between first and second 8 bytes
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ASCII header
        Text(
            text = "ASCII",
            style = HexTextStyle,
            color = HexAsciiColor
        )
    }
}

/**
 * A single row in the hex viewer.
 */
@Composable
private fun HexLineRow(
    hexLine: HexLine,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Offset
        Text(
            text = hexLine.formattedOffset,
            style = HexTextStyle,
            color = HexOffsetColor,
            modifier = Modifier.width(80.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Hex bytes
        val hexBytesAnnotated = buildHexBytesAnnotatedString(hexLine.hexBytes)
        Text(
            text = hexBytesAnnotated,
            style = HexTextStyle,
            modifier = Modifier.width(420.dp) // 16 * 24dp + 8dp gap + some padding
        )

        Spacer(modifier = Modifier.width(16.dp))

        // ASCII representation
        Text(
            text = hexLine.asciiRepresentation,
            style = HexTextStyle,
            color = HexAsciiColor
        )
    }
}

/**
 * Builds an annotated string for hex bytes with color coding.
 * Null bytes (00) are shown in a different color.
 */
@Composable
private fun buildHexBytesAnnotatedString(hexBytes: List<String>): AnnotatedString {
    return buildAnnotatedString {
        hexBytes.forEachIndexed { index, byte ->
            val color = if (byte == "00") HexNullByteColor else HexByteColor
            withStyle(SpanStyle(color = color)) {
                append(byte)
            }
            // Add space after each byte
            append(" ")
            // Add extra space after 8th byte
            if (index == 7) {
                append(" ")
            }
        }
        // Pad remaining spaces if line is incomplete
        val remaining = 16 - hexBytes.size
        if (remaining > 0) {
            val startPadIndex = hexBytes.size
            for (i in 0 until remaining) {
                append("   ") // "XX "
                if (startPadIndex + i == 7) {
                    append(" ")
                }
            }
        }
    }
}

/**
 * Status bar showing loaded/total lines.
 */
@Composable
private fun HexStatusBar(
    loadedLines: Long,
    totalLines: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hex View",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Lines: $loadedLines / $totalLines",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
