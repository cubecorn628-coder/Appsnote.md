package com.example.ui.markdown

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import java.io.File

@Composable
fun MarkdownRenderer(
    markdown: String,
    modifier: Modifier = Modifier,
    onTodoCheckedChange: ((todoIndex: Int, checked: Boolean) -> Unit)? = null
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var todoCounter = 0
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = colorScheme.primary)
                        2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = colorScheme.secondary)
                        3 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = colorScheme.tertiary)
                        else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = block.text,
                        style = style,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    val annotatedText = parseInlineMarkdown(block.text, colorScheme)
                    Text(
                        text = annotatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (block.depth * 16 + 8).dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colorScheme.primary),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, colorScheme),
                            style = MaterialTheme.typography.bodyLarge,
                            color = colorScheme.onSurface,
                            lineHeight = 24.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.TodoItem -> {
                    val currentIndex = todoCounter++
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = block.checked,
                            onCheckedChange = { checked ->
                                onTodoCheckedChange?.invoke(currentIndex, checked)
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(block.text, colorScheme),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = if (block.checked) TextDecoration.LineThrough else null,
                                color = if (block.checked) colorScheme.onSurfaceVariant else colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onTodoCheckedChange?.invoke(currentIndex, !block.checked)
                                }
                        )
                    }
                }

                is MarkdownBlock.BlockQuote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .border(width = 3.dp, color = colorScheme.primary, shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(block.text, colorScheme),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic,
                                color = colorScheme.onSurfaceVariant
                            ),
                            lineHeight = 22.sp
                        )
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    val clipboardManager = LocalClipboardManager.current
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = block.language.uppercase().ifEmpty { "CODE" },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(block.code))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Salin Kode",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = block.code,
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = colorScheme.onSurfaceVariant
                                    ),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.ImageBlock -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Render image from web or local file path
                            val imageSource = if (block.url.startsWith("/")) {
                                File(block.url)
                            } else {
                                block.url
                            }

                            AsyncImage(
                                model = imageSource,
                                contentDescription = block.alt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colorScheme.surfaceVariant)
                            )
                            if (block.alt.isNotEmpty()) {
                                Text(
                                    text = block.alt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.VideoBlock -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            YouTubePlayer(
                                videoUrl = block.url,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

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
        // Direct media embed/mp4 player fallback inside WebView
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

fun parseInlineMarkdown(text: String, colorScheme: ColorScheme): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val length = text.length

        while (i < length) {
            // Bold-Italic combination ***text***
            if (i + 2 < length && text[i] == '*' && text[i + 1] == '*' && text[i + 2] == '*') {
                val end = text.indexOf("***", i + 3)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(text.substring(i + 3, end))
                    pop()
                    i = end + 3
                    continue
                }
            }

            // Bold **text**
            if (i + 1 < length && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Bold __text__
            if (i + 1 < length && text[i] == '_' && text[i + 1] == '_') {
                val end = text.indexOf("__", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                    continue
                }
            }

            // Italic *text*
            if (text[i] == '*') {
                val end = text.indexOf('*', i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Italic _text_
            if (text[i] == '_') {
                val end = text.indexOf('_', i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Inline code `code`
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = colorScheme.surfaceVariant,
                            color = colorScheme.onSurfaceVariant
                        )
                    )
                    append(" ${text.substring(i + 1, end)} ")
                    pop()
                    i = end + 1
                    continue
                }
            }

            // Link [text](url)
            if (text[i] == '[') {
                val closeText = text.indexOf(']', i + 1)
                if (closeText != -1 && closeText + 1 < length && text[closeText + 1] == '(') {
                    val closeUrl = text.indexOf(')', closeText + 2)
                    if (closeUrl != -1) {
                        val linkText = text.substring(i + 1, closeText)
                        val linkUrl = text.substring(closeText + 2, closeUrl)
                        pushStyle(
                            SpanStyle(
                                color = colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        append(linkText)
                        pop()
                        i = closeUrl + 1
                        continue
                    }
                }
            }

            append(text[i])
            i++
        }
    }
}
