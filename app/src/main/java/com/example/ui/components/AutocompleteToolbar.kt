package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ToolbarAction(
    val icon: ImageVector,
    val label: String,
    val syntax: String,
    val cursorOffset: Int = 0, // position cursor back by this amount
    val isAction: Boolean = false,
    val onActionClick: (() -> Unit)? = null
)

@Composable
fun AutocompleteToolbar(
    onSyntaxInsert: (syntax: String, cursorOffset: Int) -> Unit,
    onUploadImage: () -> Unit,
    onEmbedVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        ToolbarAction(Icons.Default.Title, "H1", "# ", 0),
        ToolbarAction(Icons.Default.Title, "H2", "## ", 0),
        ToolbarAction(Icons.Default.FormatBold, "Tebal", "****", 2),
        ToolbarAction(Icons.Default.FormatItalic, "Miring", "**", 1),
        ToolbarAction(Icons.Default.CheckBox, "Tugas", "- [ ] ", 0),
        ToolbarAction(Icons.AutoMirrored.Default.List, "Daftar", "- ", 0),
        ToolbarAction(Icons.Default.Code, "Kode", "```\n\n```", 4),
        ToolbarAction(Icons.Default.Link, "Tautan", "[Judul](url)", 5),
        ToolbarAction(Icons.Default.Image, "Gambar", "", 0, isAction = true, onActionClick = onUploadImage),
        ToolbarAction(Icons.Default.Videocam, "Video", "", 0, isAction = true, onActionClick = onEmbedVideo)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actions.forEach { action ->
                InputChip(
                    selected = false,
                    onClick = {
                        if (action.isAction) {
                            action.onActionClick?.invoke()
                        } else {
                            onSyntaxInsert(action.syntax, action.cursorOffset)
                        }
                    },
                    label = { 
                        Text(
                            text = action.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
