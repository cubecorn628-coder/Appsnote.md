package com.example.ui.markdown

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.extendedspans.SquigglyUnderlineSpanPainter
import com.mikepenz.markdown.model.markdownPadding

/**
 * Drop-in replacement for the old custom MarkdownRenderer.
 *
 * Uses mikepenz/multiplatform-markdown-renderer which is CommonMark-compliant
 * and fully native to Jetpack Compose (no AndroidView / TextView wrapper).
 *
 * [onTodoCheckedChange] is kept for API compatibility with EditorScreen but
 * mikepenz handles checkbox rendering natively — interactive toggling is done
 * via the raw text manipulation already present in EditorScreen.
 */
@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    onTodoCheckedChange: ((todoIndex: Int, checked: Boolean) -> Unit)? = null
) {
    val colors = markdownColor(
        text = MaterialTheme.colorScheme.onSurface,
        codeBackground = MaterialTheme.colorScheme.surfaceVariant,
        inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant,
        dividerColor = MaterialTheme.colorScheme.outlineVariant,
    )

    val typography = markdownTypography(
        h1 = MaterialTheme.typography.headlineLarge,
        h2 = MaterialTheme.typography.headlineMedium,
        h3 = MaterialTheme.typography.headlineSmall,
        h4 = MaterialTheme.typography.titleLarge,
        h5 = MaterialTheme.typography.titleMedium,
        h6 = MaterialTheme.typography.titleSmall,
        text = MaterialTheme.typography.bodyLarge,
        code = MaterialTheme.typography.bodyMedium,
        quote = MaterialTheme.typography.bodyLarge,
        paragraph = MaterialTheme.typography.bodyLarge,
        ordered = MaterialTheme.typography.bodyLarge,
        bullet = MaterialTheme.typography.bodyLarge,
        list = MaterialTheme.typography.bodyLarge,
    )

    Markdown(
        content = markdown,
        colors = colors,
        typography = typography,
        padding = markdownPadding(
            block = 4.dp,
            list = 0.dp,
            indentList = 8.dp,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// YouTubePlayer — kept intact, still used by EditorScreen for video embeds
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun YouTubePlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val videoId = remember(videoUrl) { MarkdownParser.extractYouTubeVideoId(videoUrl) }

    if (videoId != null) {
        val embedHtml = remember(videoId) {
            """
            <html>
            <body style="margin:0;padding:0;background:black;">
            <iframe width="100%" height="100%" src="https://www.youtube.com/embed/$videoId?autoplay=0&rel=0" frameborder="0" allowfullscreen style="height:100vh;"></iframe>
            </body>
            </html>
            """.trimIndent()
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = modifier
        )
    } else {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    val html = """
                        <html>
                        <body style="margin:0;padding:0;background:black;display:flex;align-items:center;justify-content:center;height:100%;">
                        <video width="100%" height="100%" controls style="max-height:100%;" autoplay>
                          <source src="$videoUrl" type="video/mp4">
                          Your browser does not support the video tag.
                        </video>
                        </body>
                        </html>
                    """.trimIndent()
                    loadData(html, "text/html", "UTF-8")
                }
            },
            modifier = modifier
        )
    }
}
