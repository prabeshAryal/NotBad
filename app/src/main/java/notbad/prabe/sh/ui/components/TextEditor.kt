package notbad.prabe.sh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
import notbad.prabe.sh.ui.theme.CodeTextStyle

/**
 * Memory-efficient text editor with:
 * - Word wrap toggle
 * - Syntax highlighting  
 * - Collapsible line numbers (synced with scroll)
 * - Find/Search functionality
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
    val shouldShowLineNumbers = showLineNumbers && language != null
    var lineNumbersExpanded by remember { mutableStateOf(true) }
    
    val textLength = text.length
    
    // Memory thresholds
    val useLazyLoading = textLength > 100_000
    val disableSyntaxHighlighting = textLength > 200_000
    val effectiveLanguage = if (disableSyntaxHighlighting) null else language
    
    // Only compute lines when needed
    val lines = remember(text, useLazyLoading) {
        if (useLazyLoading) text.lineSequence().toList() else emptyList()
    }
    val lineCount = if (useLazyLoading) lines.size else text.count { it == '\n' } + 1
    
    // Shared scroll state for syncing
    val lazyListState = rememberLazyListState()
    val contentScrollState = rememberScrollState()
    
    // Search state
    var currentSearchIndex by remember { mutableIntStateOf(0) }
    val searchResults = remember(text, searchQuery) {
        if (searchQuery.isNotEmpty()) {
            findAllOccurrences(text, searchQuery)
        } else emptyList()
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        AnimatedVisibility(visible = showSearch) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { 
                    onSearchQueryChange(it)
                    currentSearchIndex = 0
                },
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
                onClose = onToggleSearch
            )
        }
        
        Row(modifier = Modifier.weight(1f).fillMaxSize()) {
            // Synced line numbers
            if (shouldShowLineNumbers) {
                SyncedLineNumberColumn(
                    lineCount = lineCount,
                    expanded = lineNumbersExpanded,
                    onToggle = { lineNumbersExpanded = !lineNumbersExpanded },
                    lazyListState = if (useLazyLoading) lazyListState else null,
                    scrollState = if (!useLazyLoading) contentScrollState else null
                )
            }

            // Editor content
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                if (useLazyLoading && isReadOnly) {
                    LazyTextViewer(
                        lines = lines,
                        language = effectiveLanguage,
                        wordWrapEnabled = wordWrapEnabled,
                        listState = lazyListState,
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        currentSearchIndex = currentSearchIndex
                    )
                } else if (isReadOnly) {
                    SyncedReadOnlyTextViewer(
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
                        searchQuery = searchQuery
                    )
                }
            }
        }
    }
}

/**
 * Search bar component
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    currentIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find", style = MaterialTheme.typography.bodySmall) },
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
                    .height(40.dp)
                    .focusRequester(focusRequester)
            )
            
            // Result count
            if (query.isNotEmpty()) {
                Text(
                    text = if (resultCount > 0) "${currentIndex + 1}/$resultCount" else "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
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
                    modifier = Modifier.size(18.dp)
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
                    modifier = Modifier.size(18.dp)
                )
            }
            
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close search",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Line number column that syncs with content scroll
 */
@Composable
private fun SyncedLineNumberColumn(
    lineCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    lazyListState: LazyListState?,
    scrollState: androidx.compose.foundation.ScrollState?,
    modifier: Modifier = Modifier
) {
    val lineNumberWidth = remember(lineCount) {
        (lineCount.toString().length * 8 + 4).dp
    }
    
    Row(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        verticalAlignment = Alignment.Top
    ) {
        // Toggle button
        Box(
            modifier = Modifier
                .width(14.dp)
                .fillMaxHeight()
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp).size(14.dp)
            )
        }
        
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            if (lazyListState != null) {
                // For lazy loading - use separate LazyColumn synced by state
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .width(lineNumberWidth)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    items(lineCount) { index ->
                        Text(
                            text = (index + 1).toString(),
                            style = CodeTextStyle.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (scrollState != null) {
                // For regular scroll - sync with scroll state
                Column(
                    modifier = Modifier
                        .width(lineNumberWidth)
                        .verticalScroll(scrollState)
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            style = CodeTextStyle.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lazy text viewer for large files
 */
@Composable
private fun LazyTextViewer(
    lines: List<String>,
    language: String?,
    wordWrapEnabled: Boolean = true,
    listState: LazyListState,
    searchQuery: String = "",
    searchResults: List<IntRange> = emptyList(),
    currentSearchIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .then(if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            itemsIndexed(lines, key = { index, _ -> index }) { _, line ->
                val displayLine = if (line.isEmpty()) " " else line
                Text(
                    text = if (language != null) {
                        highlightLine(displayLine, language)
                    } else {
                        AnnotatedString(displayLine)
                    },
                    style = CodeTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    softWrap = wordWrapEnabled,
                    modifier = if (wordWrapEnabled) Modifier.fillParentMaxWidth() else Modifier
                )
            }
        }
    }
}

/**
 * Read-only text viewer synced with line numbers
 */
@Composable
private fun SyncedReadOnlyTextViewer(
    text: String,
    language: String?,
    wordWrapEnabled: Boolean = true,
    scrollState: androidx.compose.foundation.ScrollState,
    searchQuery: String = "",
    searchResults: List<IntRange> = emptyList(),
    currentSearchIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    
    SelectionContainer {
        val displayText = remember(text, language, searchQuery, searchResults, currentSearchIndex) {
            buildHighlightedText(text, language, searchQuery, searchResults, currentSearchIndex)
        }
        
        Text(
            text = displayText,
            style = CodeTextStyle.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                lineHeight = 18.sp
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
 * Editable text field with cursor position preservation
 */
@Composable
private fun EditableTextField(
    text: String,
    onTextChange: (String) -> Unit,
    wordWrapEnabled: Boolean = true,
    scrollState: androidx.compose.foundation.ScrollState,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    // FIXED: Don't reset TextFieldValue when text changes externally
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text)) }
    
    // Sync external text changes without resetting cursor
    LaunchedEffect(text) {
        if (textFieldValue.text != text) {
            textFieldValue = TextFieldValue(text)
        }
    }
    
    val horizontalScrollState = rememberScrollState()
    
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
            lineHeight = 18.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .then(if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
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
    currentSearchIndex: Int
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        
        // Apply syntax highlighting first
        if (language != null && text.length < 50_000) {
            try {
                val syntaxLanguage = mapLanguageToSyntax(language)
                if (syntaxLanguage != null) {
                    val highlights = Highlights.Builder()
                        .code(text.take(50_000))
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
            } catch (_: Exception) {}
        }
        
        // Highlight search results
        searchResults.forEachIndexed { index, range ->
            val bgColor = if (index == currentSearchIndex) {
                Color(0xFFFFE082) // Current match - yellow
            } else {
                Color(0xFFFFEB3B).copy(alpha = 0.3f) // Other matches
            }
            addStyle(
                SpanStyle(background = bgColor),
                range.first,
                range.last + 1
            )
        }
    }
}

@Composable
private fun highlightLine(line: String, language: String): AnnotatedString {
    val syntaxLanguage = mapLanguageToSyntax(language) ?: return AnnotatedString(line)
    
    return remember(line, language) {
        try {
            val highlights = Highlights.Builder()
                .code(line)
                .theme(SyntaxThemes.darcula())
                .language(syntaxLanguage)
                .build()
            
            buildAnnotatedString {
                append(line)
                highlights.getHighlights().forEach { highlight ->
                    when (highlight) {
                        is ColorHighlight -> {
                            if (highlight.location.start < line.length && highlight.location.end <= line.length) {
                                addStyle(
                                    SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                                    highlight.location.start,
                                    highlight.location.end
                                )
                            }
                        }
                        is BoldHighlight -> {
                            if (highlight.location.start < line.length && highlight.location.end <= line.length) {
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
        } catch (_: OutOfMemoryError) {
            AnnotatedString(line)
        } catch (_: Exception) {
            AnnotatedString(line)
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
        "sql" -> SyntaxLanguage.DEFAULT
        "json" -> SyntaxLanguage.DEFAULT
        "xml", "html", "htm" -> SyntaxLanguage.DEFAULT
        "yaml", "yml" -> SyntaxLanguage.DEFAULT
        "markdown", "md" -> SyntaxLanguage.DEFAULT
        "css" -> SyntaxLanguage.DEFAULT
        "gradle" -> SyntaxLanguage.KOTLIN
        else -> null
    }
}
