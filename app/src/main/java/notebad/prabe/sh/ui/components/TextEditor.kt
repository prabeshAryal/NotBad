package notebad.prabe.sh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import notebad.prabe.sh.ui.theme.CodeTextStyle
import notebad.prabe.sh.ui.theme.MonospaceFamily
import notebad.prabe.sh.ui.theme.SyntaxComment
import notebad.prabe.sh.ui.theme.SyntaxFunction
import notebad.prabe.sh.ui.theme.SyntaxKeyword
import notebad.prabe.sh.ui.theme.SyntaxNumber
import notebad.prabe.sh.ui.theme.SyntaxString

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
                // Read-only view with selection support
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
 * Simple syntax highlighting for common patterns.
 * This is a basic implementation - for production, use a proper lexer.
 */
@Composable
private fun highlightSyntax(text: String, language: String): AnnotatedString {
    // Keywords for various languages
    val keywords = when (language.lowercase()) {
        "kotlin", "kt" -> setOf(
            "fun", "val", "var", "class", "object", "interface", "sealed",
            "data", "enum", "when", "if", "else", "for", "while", "do",
            "return", "break", "continue", "throw", "try", "catch", "finally",
            "import", "package", "private", "public", "protected", "internal",
            "open", "abstract", "override", "suspend", "inline", "infix",
            "operator", "companion", "init", "constructor", "this", "super",
            "null", "true", "false", "is", "as", "in", "out", "by", "where"
        )
        "java" -> setOf(
            "public", "private", "protected", "class", "interface", "enum",
            "extends", "implements", "static", "final", "abstract", "void",
            "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "return", "break", "continue", "throw", "try", "catch", "finally",
            "new", "this", "super", "null", "true", "false", "instanceof",
            "import", "package", "throws", "synchronized", "volatile", "transient"
        )
        "javascript", "js", "typescript", "ts" -> setOf(
            "function", "const", "let", "var", "class", "extends", "implements",
            "if", "else", "for", "while", "do", "switch", "case", "default",
            "return", "break", "continue", "throw", "try", "catch", "finally",
            "new", "this", "super", "null", "undefined", "true", "false",
            "import", "export", "from", "async", "await", "yield", "typeof",
            "instanceof", "delete", "void", "interface", "type", "enum"
        )
        "python", "py" -> setOf(
            "def", "class", "if", "elif", "else", "for", "while", "try",
            "except", "finally", "with", "as", "import", "from", "return",
            "yield", "break", "continue", "pass", "raise", "lambda", "and",
            "or", "not", "in", "is", "True", "False", "None", "global",
            "nonlocal", "assert", "del", "async", "await"
        )
        else -> emptySet()
    }

    return buildAnnotatedString {
        var i = 0
        val length = text.length

        while (i < length) {
            when {
                // String literals (double quotes)
                text[i] == '"' -> {
                    val start = i
                    i++
                    while (i < length && text[i] != '"') {
                        if (text[i] == '\\' && i + 1 < length) i++
                        i++
                    }
                    if (i < length) i++
                    withStyle(SpanStyle(color = SyntaxString)) {
                        append(text.substring(start, i))
                    }
                }

                // String literals (single quotes)
                text[i] == '\'' -> {
                    val start = i
                    i++
                    while (i < length && text[i] != '\'') {
                        if (text[i] == '\\' && i + 1 < length) i++
                        i++
                    }
                    if (i < length) i++
                    withStyle(SpanStyle(color = SyntaxString)) {
                        append(text.substring(start, i))
                    }
                }

                // Single-line comments
                text[i] == '/' && i + 1 < length && text[i + 1] == '/' -> {
                    val start = i
                    while (i < length && text[i] != '\n') i++
                    withStyle(SpanStyle(color = SyntaxComment)) {
                        append(text.substring(start, i))
                    }
                }

                // Python comments
                text[i] == '#' && language in listOf("python", "py") -> {
                    val start = i
                    while (i < length && text[i] != '\n') i++
                    withStyle(SpanStyle(color = SyntaxComment)) {
                        append(text.substring(start, i))
                    }
                }

                // Numbers
                text[i].isDigit() -> {
                    val start = i
                    while (i < length && (text[i].isDigit() || text[i] == '.' || text[i] == 'x' || text[i] in 'a'..'f' || text[i] in 'A'..'F')) {
                        i++
                    }
                    withStyle(SpanStyle(color = SyntaxNumber)) {
                        append(text.substring(start, i))
                    }
                }

                // Identifiers/Keywords
                text[i].isLetter() || text[i] == '_' -> {
                    val start = i
                    while (i < length && (text[i].isLetterOrDigit() || text[i] == '_')) {
                        i++
                    }
                    val word = text.substring(start, i)
                    val style = if (word in keywords) {
                        SpanStyle(color = SyntaxKeyword)
                    } else {
                        SpanStyle(color = MaterialTheme.colorScheme.onSurface)
                    }
                    withStyle(style) {
                        append(word)
                    }
                }

                // Default: append character as-is
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
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
