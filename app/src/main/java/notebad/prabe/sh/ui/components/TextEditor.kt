package notebad.prabe.sh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import notebad.prabe.sh.ui.theme.CodeTextStyle

/**
 * Text editor component with optional syntax highlighting.
 *
 * @param text The current text content
 * @param onTextChange Callback when text is modified
 * @param isReadOnly Whether the editor is in read-only mode
 * @param language Optional language for syntax highlighting
 * @param showLineNumbers Whether to show line numbers
 * @param modifier Modifier for the component
 */
@Composable
fun TextEditor(
    text: String,
    onTextChange: (String) -> Unit,
    isReadOnly: Boolean = false,
    language: String? = null,
    showLineNumbers: Boolean = true,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text))
    }

    val lines = remember(text) { text.lines() }
    val lineCount = lines.size

    Row(modifier = modifier.fillMaxSize()) {
        // Line numbers column
        if (showLineNumbers) {
            LineNumberColumn(
                lineCount = lineCount,
                scrollState = verticalScrollState,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp)
            )
        }

        // Editor content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            if (isReadOnly) {
                // Read-only view with selection support and syntax highlighting
                SelectionContainer {
                    val highlightedText = if (language != null) {
                        highlightSyntax(text, language)
                    } else {
                        AnnotatedString(text)
                    }

                    Text(
                        text = highlightedText,
                        style = CodeTextStyle.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .padding(8.dp)
                    )
                }
            } else {
                // Editable text field
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        onTextChange(newValue.text)
                    },
                    textStyle = CodeTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * Line numbers column that syncs with editor scrolling.
 */
@Composable
private fun LineNumberColumn(
    lineCount: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val lineNumberWidth = remember(lineCount) {
        (lineCount.toString().length * 10 + 16).dp
    }

    Column(
        modifier = modifier
            .width(lineNumberWidth)
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.End
    ) {
        for (i in 1..lineCount) {
            Text(
                text = i.toString(),
                style = CodeTextStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
        }
    }
}

/**
 * Syntax highlighting using the Highlights library.
 * 
 * Supported languages: C, C++, C#, Dart, Java, Kotlin, Rust, CoffeeScript, JavaScript, 
 * Perl, Python, Ruby, Shell, Swift, TypeScript, Go, PHP.
 */
@Composable
private fun highlightSyntax(text: String, language: String): AnnotatedString {
    val syntaxLanguage = mapLanguageToSyntax(language)
    
    // If language not supported, return plain text
    if (syntaxLanguage == null) {
        return AnnotatedString(text)
    }
    
    val highlights = remember(text, language) {
        Highlights.Builder()
            .code(text)
            .theme(SyntaxThemes.darcula())
            .language(syntaxLanguage)
            .build()
    }
    
    return buildAnnotatedString {
        append(text)
        
        highlights.getHighlights().forEach { highlight ->
            when (highlight) {
                is ColorHighlight -> {
                    addStyle(
                        style = SpanStyle(color = Color(highlight.rgb).copy(alpha = 1f)),
                        start = highlight.location.start,
                        end = highlight.location.end
                    )
                }
                is BoldHighlight -> {
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

/**
 * Maps file extension/language name to SyntaxLanguage enum.
 * 
 * Supported languages by Highlights library:
 * C, C++, C#, Dart, Java, Kotlin, Rust, CoffeeScript, JavaScript, 
 * Perl, Python, Ruby, Shell, Swift, TypeScript, Go, PHP.
 */
private fun mapLanguageToSyntax(language: String): SyntaxLanguage? {
    return when (language.lowercase()) {
        // Direct matches
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
        
        // Use closest match for unsupported languages
        "scala", "clj", "clojure", "groovy" -> SyntaxLanguage.JAVA  // JVM-family
        "jsx", "tsx", "vue" -> SyntaxLanguage.JAVASCRIPT  // JS-like
        "objc", "m", "objective-c" -> SyntaxLanguage.C  // C-like
        
        // Use DEFAULT for generic code highlighting
        "json", "xml", "html", "xhtml", "svg", "css", "scss", "sass", "less" -> SyntaxLanguage.DEFAULT
        "yaml", "yml", "toml", "ini", "conf", "cfg" -> SyntaxLanguage.DEFAULT
        "sql", "mysql", "postgresql", "sqlite" -> SyntaxLanguage.DEFAULT
        "markdown", "md", "txt", "text" -> SyntaxLanguage.DEFAULT
        "lua", "r", "matlab", "fortran" -> SyntaxLanguage.DEFAULT
        "haskell", "hs", "erlang", "erl", "elixir", "ex" -> SyntaxLanguage.DEFAULT
        
        // Unknown language - no highlighting
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
