package notbad.prabe.sh.ui.components

import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Markdown preview component using Markwon library.
 * Renders markdown content with support for:
 * - Tables
 * - Task lists
 * - Strikethrough
 * - HTML
 * - Auto-linkification
 *
 * @param markdown The markdown text to render
 * @param modifier Modifier for the component
 */
@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Colors from the theme
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    // Create Markwon instance with plugins
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                setBackgroundColor(backgroundColor)
                textSize = 16f
                setPadding(32, 32, 32, 32)

                // Enable link clicking
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            // Render markdown
            markwon.setMarkdown(textView, markdown)
        },
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    )
}

/**
 * Split view showing markdown source and preview side by side.
 * Useful for editing markdown with live preview.
 *
 * @param markdown The markdown text
 * @param onMarkdownChange Callback when markdown is modified
 * @param modifier Modifier for the component
 */
@Composable
fun MarkdownSplitView(
    markdown: String,
    onMarkdownChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxSize()
    ) {
        // Source editor
        TextEditor(
            text = markdown,
            onTextChange = onMarkdownChange,
            isReadOnly = false,
            language = "markdown",
            showLineNumbers = true,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        )

        // Divider
        androidx.compose.material3.VerticalDivider()

        // Preview
        MarkdownPreview(
            markdown = markdown,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
    }
}
