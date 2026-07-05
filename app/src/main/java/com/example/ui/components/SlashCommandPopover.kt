package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SlashCommand(
    val keyword: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val syntax: String,
    val cursorOffset: Int = 0,
    val isAction: Boolean = false,
    val actionType: String = "" // "image" or "video"
)

val SLASH_COMMANDS = listOf(
    SlashCommand("h1", "Heading 1", "Menambahkan judul besar", Icons.Default.Title, "# ", 0),
    SlashCommand("h2", "Heading 2", "Menambahkan judul sedang", Icons.Default.Title, "## ", 0),
    SlashCommand("todo", "Todo List", "Menambahkan daftar tugas", Icons.Default.CheckBox, "- [ ] ", 0),
    SlashCommand("list", "Bullet List", "Menambahkan daftar poin", Icons.AutoMirrored.Default.List, "- ", 0),
    SlashCommand("code", "Code Block", "Menambahkan blok kode", Icons.Default.Code, "```\n\n```", 4),
    SlashCommand("bold", "Teks Tebal", "Format teks menjadi tebal", Icons.Default.FormatBold, "****", 2),
    SlashCommand("italic", "Teks Miring", "Format teks menjadi miring", Icons.Default.FormatItalic, "**", 1),
    SlashCommand("image", "Upload Gambar", "Menyisipkan file gambar lokal", Icons.Default.Image, "", 0, isAction = true, actionType = "image"),
    SlashCommand("video", "Sematkan Video", "Menyematkan video eksternal", Icons.Default.Videocam, "", 0, isAction = true, actionType = "video")
)

@Composable
fun SlashCommandPopover(
    query: String,
    onSelectCommand: (syntax: String, cursorOffset: Int, isAction: Boolean, actionType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = SLASH_COMMANDS.filter {
        it.keyword.contains(query, ignoreCase = true) ||
        it.title.contains(query, ignoreCase = true)
    }

    if (filtered.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "PINTASAN FORMAT (/) ",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(filtered) { cmd ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCommand(cmd.syntax, cmd.cursorOffset, cmd.isAction, cmd.actionType)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cmd.icon,
                                contentDescription = cmd.title,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cmd.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = cmd.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "/${cmd.keyword}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
