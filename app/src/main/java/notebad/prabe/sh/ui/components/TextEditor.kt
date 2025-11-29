package notebad.prabe.sh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import notebad.prabe.sh.ui.theme.CodeTextStyle

/**
 * Memory-efficient text editor with word wrap, syntax highlighting, and collapsible line numbers.
 * Uses LazyColumn for large files to prevent OOM crashes.
 *
 * @param text The current text content
 * @param onTextChange Callback when text is modified
 * @param isReadOnly Whether the editor is in read-only mode
 * @param language Optional language for syntax highlighting (null = plain text mode)
 * @param showLineNumbers Whether to show line numbers (only for code mode)
 * @param wordWrapEnabled Whether to enable word wrapping
 * @param modifier Modifier for the component
 */
@Composable
fun TextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    isReadOnly: Boolean = false,
    language: String? = null,
    showLineNumbers: Boolean = true,
    wordWrapEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Only show line numbers in code mode (when language is specified)
    val shouldShowLineNumbers = showLineNumbers && language != null
    var lineNumbersExpanded by remember { mutableStateOf(true) }
    
    // For memory efficiency, calculate line count without creating a full list for small files
    val textLength = text.length
    
    // Use LazyColumn for files > 100KB to prevent OOM
    // Also disable syntax highlighting for very large files (> 200KB)
    val useLazyLoading = textLength > 100_000
    val disableSyntaxHighlighting = textLength > 200_000
    val effectiveLanguage = if (disableSyntaxHighlighting) null else language
    
    // Only compute lines list when needed for lazy loading
    val lines = remember(text, useLazyLoading) {
        if (useLazyLoading) text.lineSequence().toList() else emptyList()
    }
    val lineCount = if (useLazyLoading) lines.size else text.count { it == '\n' } + 1
    
    Row(modifier = modifier.fillMaxSize()) {
        // Collapsible line numbers column (only for code mode)
        if (shouldShowLineNumbers) {
            CollapsibleLineNumberColumn(
                lineCount = lineCount,
                expanded = lineNumbersExpanded,
                onToggle = { lineNumbersExpanded = !lineNumbersExpanded }
            )
        }

        // Editor content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            if (useLazyLoading && isReadOnly) {
                // Large file: Use LazyColumn for memory efficiency
                LazyTextViewer(
                    lines = lines,
                    language = effectiveLanguage,
                    wordWrapEnabled = wordWrapEnabled
                )
            } else if (isReadOnly) {
                // Small file read-only: Regular scrollable text with word wrap
                ReadOnlyTextViewer(
                    text = text,
                    language = effectiveLanguage,
                    wordWrapEnabled = wordWrapEnabled
                )
            } else {
                // Editable mode with word wrap
                EditableTextField(
                    text = text,
                    onTextChange = onTextChange,
                    wordWrapEnabled = wordWrapEnabled
                )
            }
        }
    }
}

/**
 * Collapsible line number column - compact and expandable.
 */
@Composable
private fun CollapsibleLineNumberColumn(
    lineCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineNumberWidth = remember(lineCount) {
        (lineCount.toString().length * 8 + 8).dp
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
                .width(16.dp)
                .fillMaxHeight()
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.TopCenter
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Line numbers (animated visibility)
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Column(
                modifier = Modifier
                    .width(lineNumberWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    Text(
                        text = i.toString(),
                        style = CodeTextStyle.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Memory-efficient lazy text viewer for large files.
 * Renders only visible lines using LazyColumn.
 */
@Composable
private fun LazyTextViewer(
    lines: List<String>,
    language: String?,
    wordWrapEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    
    SelectionContainer {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState)
                    else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            itemsIndexed(
                items = lines,
                key = { index, _ -> index }
            ) { index, line ->
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
 * Read-only text viewer with optional word wrap for smaller files.
 */
@Composable
private fun ReadOnlyTextViewer(
    text: String,
    language: String?,
    wordWrapEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    SelectionContainer {
        val displayText = if (language != null) {
            highlightSyntax(text, language)
        } else {
            AnnotatedString(text)
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
                .then(
                    if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState)
                    else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Editable text field with optional word wrap.
 */
@Composable
private fun EditableTextField(
    text: String,
    onTextChange: (String) -> Unit,
    wordWrapEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text))
    }
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            onTextChange(newValue.text)
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
            .then(
                if (!wordWrapEnabled) Modifier.horizontalScroll(horizontalScrollState)
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * Highlights a single line for use in LazyColumn.
 */
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
                                    style = SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                                    start = highlight.location.start,
                                    end = highlight.location.end
                                )
                            }
                        }
                        is BoldHighlight -> {
                            if (highlight.location.start < line.length && highlight.location.end <= line.length) {
                                addStyle(
                                    style = SpanStyle(fontWeight = FontWeight.Bold),
                                    start = highlight.location.start,
                                    end = highlight.location.end
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            // Fallback to plain text on OOM
            AnnotatedString(line)
        } catch (e: Exception) {
            AnnotatedString(line)
        }
    }
}

/**
 * Syntax highlighting for entire text (used for smaller files).
 */
@Composable
private fun highlightSyntax(text: String, language: String): AnnotatedString {
    val syntaxLanguage = mapLanguageToSyntax(language) ?: return AnnotatedString(text)
    
    return remember(text, language) {
        try {
            // Limit highlighting to first 50KB for performance and memory safety
            val textToHighlight = if (text.length > 50_000) text.take(50_000) else text
            
            val highlights = Highlights.Builder()
                .code(textToHighlight)
                .theme(SyntaxThemes.darcula())
                .language(syntaxLanguage)
                .build()
            
            buildAnnotatedString {
                append(text)
                highlights.getHighlights().forEach { highlight ->
                    when (highlight) {
                        is ColorHighlight -> {
                            if (highlight.location.end <= text.length) {
                                addStyle(
                                    style = SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                                    start = highlight.location.start,
                                    end = highlight.location.end
                                )
                            }
                        }
                        is BoldHighlight -> {
                            if (highlight.location.end <= text.length) {
                                addStyle(
                                    style = SpanStyle(fontWeight = FontWeight.Bold),
                                    start = highlight.location.start,
                                    end = highlight.location.end
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: OutOfMemoryError) {
            // Fallback to plain text on OOM
            AnnotatedString(text)
        } catch (e: Exception) {
            AnnotatedString(text)
        }
    }
}

/**
 * Maps file extension/language name to SyntaxLanguage enum.
 */
private fun mapLanguageToSyntax(language: String): SyntaxLanguage? {
    return when (language.lowercase()) {
        "kotlin", "kt", "kts" -> SyntaxLanguage.KOTLIN
        "java" -> SyntaxLanguage.JAVA
        "javascript", "js" -> SyntaxLanguage.JAVASCRIPT
        "typescript", "ts" -> SyntaxLanguage.TYPESCRIPT
        "python", "py" -> SyntaxLanguage.PYTHON
        "c", "h" -> SyntaxLanguage.C
        "cpp", "cc", "cxx", "hpp", "c++" -> SyntaxLanguage.CPP
        "rust", "rs" -> SyntaxLanguage.RUST
        "go" -> SyntaxLanguage.GO
        "swift" -> SyntaxLanguage.SWIFT
        "php" -> SyntaxLanguage.PHP
        "ruby", "rb" -> SyntaxLanguage.RUBY
        "shell", "sh", "bash", "zsh", "bsh", "csh" -> SyntaxLanguage.SHELL
        "dart" -> SyntaxLanguage.DART
        "cs", "csharp" -> SyntaxLanguage.CSHARP
        "coffee", "coffeescript" -> SyntaxLanguage.COFFEESCRIPT
        "perl", "pl", "pm" -> SyntaxLanguage.PERL
        "scala", "clj", "clojure", "groovy" -> SyntaxLanguage.JAVA
        "jsx", "tsx", "vue" -> SyntaxLanguage.JAVASCRIPT
        "objc", "m", "objective-c" -> SyntaxLanguage.C
        "json", "xml", "html", "xhtml", "svg", "css", "scss", "sass", "less" -> SyntaxLanguage.DEFAULT
        "yaml", "yml", "toml", "ini", "conf", "cfg" -> SyntaxLanguage.DEFAULT
        "sql", "mysql", "postgresql", "sqlite" -> SyntaxLanguage.DEFAULT
        "markdown", "md", "txt", "text" -> SyntaxLanguage.DEFAULT
        else -> null
    }
}

/**
 * Read-only code viewer with syntax highlighting.
 */
@Composable
fun CodeViewer(
    text: String,
    language: String?,
    showLineNumbers: Boolean = true,
    modifier: Modifier = Modifier
) {
    TextEditor(
        text = text,
        onTextChange = {},
        isReadOnly = true,
        language = language,
        showLineNumbers = showLineNumbers,
        modifier = modifier
    )
}
