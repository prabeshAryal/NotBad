package notbad.prabe.sh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FindReplace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import notbad.prabe.sh.ui.theme.CodeTextStyle

/**
 * Memory-efficient text editor with:
 * - Find & Replace functionality
 * - Synced line numbers
 * - Word wrap toggle
 * - Syntax highlighting (disabled for large files)
 * - Cursor position preservation
 */
@Composable
fun TextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    isReadOnly: Boolean = false,
    language: String? = null,
    showLineNumbers: Boolean = true,
    wordWrapEnabled: Boolean = true,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    showSearch: Boolean = false,
    onToggleSearch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textLength = text.length
    
    // Memory thresholds - skip syntax highlighting for large files
    val disableSyntaxHighlighting = textLength > 50_000 // Lowered threshold for syntax highlighting
    val useLargeFileViewer = textLength > 200_000 // Use LazyColumn for very large files
    
    val effectiveLanguage = if (disableSyntaxHighlighting) null else language
    
    // Calculate line count
    val lineCount = remember(text) { 
        if (useLargeFileViewer) 0 else text.count { it == '\n' } + 1 
    }
    
    // Shared scroll state for syncing line numbers with content
    val contentScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // Search state
    var currentSearchIndex by remember { mutableIntStateOf(0) }
    var showReplace by remember { mutableStateOf(false) }
    var replaceText by remember { mutableStateOf("") }
    
    val searchResults = remember(text, searchQuery) {
        if (searchQuery.isNotEmpty() && !useLargeFileViewer) findAllOccurrences(text, searchQuery) else emptyList()
    }
    
    // Navigate to search result when index changes
    LaunchedEffect(currentSearchIndex, searchResults) {
        if (searchResults.isNotEmpty() && currentSearchIndex in searchResults.indices) {
            val targetPosition = searchResults[currentSearchIndex].first
            // Calculate approximate scroll position based on character position
            val lineNumber = text.substring(0, targetPosition).count { it == '\n' }
            val lineHeight = 18 // approximate line height in pixels
            val scrollPosition = lineNumber * lineHeight
            scope.launch {
                contentScrollState.animateScrollTo(scrollPosition.coerceAtLeast(0))
            }
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Find & Replace bar
        AnimatedVisibility(
            visible = showSearch,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FindReplaceBar(
                query = searchQuery,
                onQueryChange = { 
                    onSearchQueryChange(it)
                    currentSearchIndex = 0
                },
                replaceText = replaceText,
                onReplaceTextChange = { replaceText = it },
                showReplace = showReplace,
                onToggleReplace = { showReplace = !showReplace },
                resultCount = searchResults.size,
                currentIndex = currentSearchIndex,
                onPrevious = {
                    if (searchResults.isNotEmpty()) {
                        currentSearchIndex = (currentSearchIndex - 1 + searchResults.size) % searchResults.size
                    }
                },
                onNext = {
                    if (searchResults.isNotEmpty()) {
                        currentSearchIndex = (currentSearchIndex + 1) % searchResults.size
                    }
                },
                onReplace = {
                    if (searchResults.isNotEmpty() && currentSearchIndex in searchResults.indices && !isReadOnly) {
                        val range = searchResults[currentSearchIndex]
                        val newText = text.substring(0, range.first) + replaceText + text.substring(range.last + 1)
                        onTextChange(newText)
                    }
                },
                onReplaceAll = {
                    if (searchResults.isNotEmpty() && searchQuery.isNotEmpty() && !isReadOnly) {
                        val newText = text.replace(searchQuery, replaceText, ignoreCase = true)
                        onTextChange(newText)
                        currentSearchIndex = 0
                    }
                },
                onClose = onToggleSearch,
                isReadOnly = isReadOnly
            )
        }
        
        if (useLargeFileViewer) {
            LargeFileViewer(
                text = text,
                showLineNumbers = showLineNumbers,
                modifier = Modifier.weight(1f)
            )
        } else {
            Row(modifier = Modifier.weight(1f).fillMaxSize()) {
                // Line numbers - synced with content scroll
                if (showLineNumbers && language != null) {
                    LineNumberColumn(
                        lineCount = lineCount,
                        scrollState = contentScrollState,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // Editor content
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (isReadOnly) {
                        ReadOnlyTextViewer(
                            text = text,
                            language = effectiveLanguage,
                            wordWrapEnabled = wordWrapEnabled,
                            scrollState = contentScrollState,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            currentSearchIndex = currentSearchIndex
                        )
                    } else {
                        EditableTextField(
                            text = text,
                            onTextChange = onTextChange,
                            wordWrapEnabled = wordWrapEnabled,
                            scrollState = contentScrollState,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            currentSearchIndex = currentSearchIndex
                        )
                    }
                }
            }
        }
    }
}

/**
 * Find & Replace bar with navigation
 */
@Composable
private fun FindReplaceBar(
    query: String,
    onQueryChange: (String) -> Unit,
    replaceText: String,
    onReplaceTextChange: (String) -> Unit,
    showReplace: Boolean,
    onToggleReplace: () -> Unit,
    resultCount: Int,
    currentIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    isReadOnly: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Find row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle replace button
                if (!isReadOnly) {
                    IconButton(
                        onClick = onToggleReplace,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FindReplace,
                            contentDescription = "Toggle Replace",
                            modifier = Modifier.size(18.dp),
                            tint = if (showReplace) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Find field
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { 
                        Text(
                            "Find",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        ) 
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .focusRequester(focusRequester)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Result count
                if (query.isNotEmpty()) {
                    Text(
                        text = if (resultCount > 0) "${currentIndex + 1}/$resultCount" else "0/0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(min = 40.dp),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Navigation buttons
                IconButton(
                    onClick = onPrevious,
                    enabled = resultCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onNext,
                    enabled = resultCount > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Replace row (if visible)
            AnimatedVisibility(visible = showReplace && !isReadOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(32.dp)) // Align with find field
                    
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = onReplaceTextChange,
                        placeholder = { 
                            Text(
                                "Replace",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            ) 
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    TextButton(
                        onClick = onReplace,
                        enabled = resultCount > 0
                    ) {
                        Text("Replace", style = MaterialTheme.typography.labelSmall)
                    }
                    
                    TextButton(
                        onClick = onReplaceAll,
                        enabled = resultCount > 0
                    ) {
                        Text("All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/**
 * Line number column synced with scroll state
 */
@Composable
private fun LineNumberColumn(
    lineCount: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val lineNumberWidth = remember(lineCount) {
        (lineCount.toString().length * 9 + 16).dp
    }
    
    val density = LocalDensity.current
    var lineHeight by remember { mutableStateOf(18.dp) }
    
    Box(
        modifier = modifier
            .width(lineNumberWidth)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (i in 1..lineCount) {
                Text(
                    text = i.toString(),
                    style = CodeTextStyle.copy(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        lineHeight = 18.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (i == 1) {
                                lineHeight = with(density) { coordinates.size.height.toDp() }
                            }
                        },
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Read-only text viewer with syntax highlighting and search highlighting
 */
@Composable
private fun ReadOnlyTextViewer(
    text: String,
    language: String?,
    wordWrapEnabled: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    searchQuery: String,
    searchResults: List<IntRange>,
    currentSearchIndex: Int,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    
    val activeColor = MaterialTheme.colorScheme.primaryContainer
    val inactiveColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)

    SelectionContainer {
        val displayText = remember(text, language, searchQuery, searchResults, currentSearchIndex, activeColor, inactiveColor) {
            buildHighlightedText(text, language, searchQuery, searchResults, currentSearchIndex, activeColor, inactiveColor)
        }
        
        Text(
            text = displayText,
            style = CodeTextStyle.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            softWrap = wordWrapEnabled,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .then(if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Editable text field with cursor position preservation and search highlighting
 */
@Composable
private fun EditableTextField(
    text: String,
    onTextChange: (String) -> Unit,
    wordWrapEnabled: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    searchQuery: String,
    searchResults: List<IntRange>,
    currentSearchIndex: Int,
    modifier: Modifier = Modifier
) {
    // Keep TextFieldValue separate to preserve cursor position
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }
    
    // Sync external text changes without resetting cursor position
    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            // Preserve cursor at same position if possible
            val cursorPos = textFieldValue.selection.start.coerceAtMost(text.length)
            textFieldValue = TextFieldValue(
                text = text,
                selection = TextRange(cursorPos)
            )
        }
    }
    
    val horizontalScrollState = rememberScrollState()
    
    val activeColor = MaterialTheme.colorScheme.primaryContainer
    val inactiveColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)

    // Build highlighted text for display
    val visualTransformation = remember(searchQuery, searchResults, currentSearchIndex, activeColor, inactiveColor) {
        if (searchQuery.isNotEmpty() && searchResults.isNotEmpty()) {
            SearchHighlightVisualTransformation(searchResults, currentSearchIndex, activeColor, inactiveColor)
        } else {
            null
        }
    }
    
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != text) {
                onTextChange(newValue.text)
            }
        },
        textStyle = CodeTextStyle.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .then(if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * Visual transformation for search highlighting in editable field
 */
private class SearchHighlightVisualTransformation(
    private val searchResults: List<IntRange>,
    private val currentIndex: Int,
    private val activeColor: Color,
    private val inactiveColor: Color
) {
    fun filter(text: AnnotatedString): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            searchResults.forEachIndexed { index, range ->
                if (range.last < text.length) {
                    val bgColor = if (index == currentIndex) {
                        activeColor
                    } else {
                        inactiveColor
                    }
                    addStyle(
                        SpanStyle(background = bgColor),
                        range.first,
                        range.last + 1
                    )
                }
            }
        }
    }
}

// ============== Helper functions ==============

private fun findAllOccurrences(text: String, query: String): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    val results = mutableListOf<IntRange>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index >= 0) {
        results.add(index until (index + query.length))
        index = text.indexOf(query, index + 1, ignoreCase = true)
    }
    return results
}

private fun buildHighlightedText(
    text: String,
    language: String?,
    searchQuery: String,
    searchResults: List<IntRange>,
    currentSearchIndex: Int,
    activeColor: Color,
    inactiveColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        
        // Apply syntax highlighting first (only for smaller files)
        if (language != null && text.length < 100_000) {
            try {
                val syntaxLanguage = mapLanguageToSyntax(language)
                if (syntaxLanguage != null) {
                    val highlights = Highlights.Builder()
                        .code(text.take(100_000))
                        .theme(SyntaxThemes.darcula())
                        .language(syntaxLanguage)
                        .build()
                    
                    highlights.getHighlights().forEach { highlight ->
                        when (highlight) {
                            is ColorHighlight -> {
                                if (highlight.location.end <= text.length) {
                                    addStyle(
                                        SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                                        highlight.location.start,
                                        highlight.location.end
                                    )
                                }
                            }
                            is BoldHighlight -> {
                                if (highlight.location.end <= text.length) {
                                    addStyle(
                                        SpanStyle(fontWeight = FontWeight.Bold),
                                        highlight.location.start,
                                        highlight.location.end
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Syntax highlighting failed, continue without it
            }
        }
        
        // Highlight search results (overlays on top of syntax highlighting)
        searchResults.forEachIndexed { index, range ->
            if (range.last < text.length) {
                val bgColor = if (index == currentSearchIndex) {
                    activeColor // Current match
                } else {
                    inactiveColor // Other matches
                }
                addStyle(
                    SpanStyle(background = bgColor),
                    range.first,
                    range.last + 1
                )
            }
        }
    }
}

private fun mapLanguageToSyntax(language: String): SyntaxLanguage? {
    return when (language.lowercase()) {
        "kotlin", "kt" -> SyntaxLanguage.KOTLIN
        "java" -> SyntaxLanguage.JAVA
        "javascript", "js" -> SyntaxLanguage.JAVASCRIPT
        "typescript", "ts" -> SyntaxLanguage.TYPESCRIPT
        "python", "py" -> SyntaxLanguage.PYTHON
        "c" -> SyntaxLanguage.C
        "cpp", "c++", "cc", "cxx" -> SyntaxLanguage.CPP
        "csharp", "cs", "c#" -> SyntaxLanguage.CSHARP
        "go" -> SyntaxLanguage.GO
        "rust", "rs" -> SyntaxLanguage.RUST
        "swift" -> SyntaxLanguage.SWIFT
        "php" -> SyntaxLanguage.PHP
        "ruby", "rb" -> SyntaxLanguage.RUBY
        "shell", "bash", "sh", "zsh" -> SyntaxLanguage.SHELL
        "dart" -> SyntaxLanguage.DART
        "perl" -> SyntaxLanguage.PERL
        "coffeescript", "coffee" -> SyntaxLanguage.COFFEESCRIPT
        "json" -> SyntaxLanguage.JAVASCRIPT
        else -> null
    }
}

/**
 * Optimized viewer for large files using LazyColumn
 */
@Composable
private fun LargeFileViewer(
    text: String,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    val lines = remember(text) { text.lines() }
    val listState = rememberLazyListState()
    
    Row(modifier = modifier.fillMaxSize()) {
        if (showLineNumbers) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(min = 40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .fillMaxHeight()
            ) {
                itemsIndexed(lines) { index, _ ->
                    Text(
                        text = (index + 1).toString(),
                        style = CodeTextStyle.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            lineHeight = 18.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 0.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(rememberScrollState())
        ) {
            itemsIndexed(lines) { _, line ->
                Text(
                    text = line,
                    style = CodeTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

