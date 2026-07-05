package com.example.ui.markdown

import java.util.regex.Pattern

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletItem(val depth: Int, val text: String) : MarkdownBlock()
    data class TodoItem(val checked: Boolean, val text: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class ImageBlock(val alt: String, val url: String) : MarkdownBlock()
    data class VideoBlock(val url: String) : MarkdownBlock()
}

object MarkdownParser {
    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.split("\n")
        var inCodeBlock = false
        var codeLanguage = ""
        val codeBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()

            // Code block handling
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeBuilder.toString().trimEnd()))
                    codeBuilder.setLength(0)
                    inCodeBlock = false
                } else {
                    codeLanguage = trimmed.substring(3).trim()
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBuilder.append(line).append("\n")
                continue
            }

            // Empty lines
            if (trimmed.isEmpty()) {
                continue
            }

            // Headers
            if (trimmed.startsWith("#")) {
                val headerMatch = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
                if (headerMatch != null) {
                    val level = headerMatch.groupValues[1].length
                    val text = headerMatch.groupValues[2]
                    blocks.add(MarkdownBlock.Header(level, text))
                    continue
                }
            }

            // BlockQuotes
            if (trimmed.startsWith(">")) {
                val quoteText = trimmed.substring(1).trim()
                blocks.add(MarkdownBlock.BlockQuote(quoteText))
                continue
            }

            // Todo list checkbox: "- [ ] " or "- [x] "
            if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                trimmed.startsWith("* [ ] ") || trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ")) {
                val checked = trimmed.contains("[x]", ignoreCase = true)
                val text = trimmed.substring(6).trim()
                blocks.add(MarkdownBlock.TodoItem(checked, text))
                continue
            }

            // Bullet items
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                val text = trimmed.substring(2).trim()
                val leadingSpaces = line.length - line.trimStart().length
                val depth = leadingSpaces / 2
                blocks.add(MarkdownBlock.BulletItem(depth, text))
                continue
            }

            // Images / Video embed syntax:
            val videoMatch = Regex("^(!|@)\\[video\\]\\((.*?)\\)$", RegexOption.IGNORE_CASE).find(trimmed)
            if (videoMatch != null) {
                val videoUrl = videoMatch.groupValues[2]
                blocks.add(MarkdownBlock.VideoBlock(videoUrl))
                continue
            }

            val imageMatch = Regex("^!\\[(.*?)\\]\\((.*?)\\)$").find(trimmed)
            if (imageMatch != null) {
                val alt = imageMatch.groupValues[1]
                val url = imageMatch.groupValues[2]
                if (url.contains("youtube.com") || url.contains("youtu.be") || url.endsWith(".mp4")) {
                    blocks.add(MarkdownBlock.VideoBlock(url))
                } else {
                    blocks.add(MarkdownBlock.ImageBlock(alt, url))
                }
                continue
            }

            // Direct YouTube / Vimeo / MP4 links on their own line
            if ((trimmed.startsWith("http://") || trimmed.startsWith("https://")) && 
                (trimmed.contains("youtube.com") || trimmed.contains("youtu.be") || trimmed.contains("vimeo.com") || trimmed.endsWith(".mp4"))) {
                blocks.add(MarkdownBlock.VideoBlock(trimmed))
                continue
            }

            // Default paragraph
            blocks.add(MarkdownBlock.Paragraph(line))
        }

        if (inCodeBlock) {
            blocks.add(MarkdownBlock.CodeBlock(codeLanguage, codeBuilder.toString().trimEnd()))
        }

        return blocks
    }

    fun extractYouTubeVideoId(url: String): String? {
        val pattern = "https?:\\/\\/(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})"
        val compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}
