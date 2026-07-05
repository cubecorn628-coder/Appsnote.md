package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Folder

val PASTEL_COLORS = listOf(
    "#FF8B8B" to "Coral",
    "#FFAE85" to "Peach",
    "#FFD185" to "Sand",
    "#A7F3D0" to "Sage",
    "#99F6E4" to "Mint",
    "#BAE6FD" to "Ice",
    "#C084FC" to "Lavender",
    "#F472B6" to "Rose"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit,
    folderToEdit: Folder? = null
) {
    var name by remember { mutableStateOf(folderToEdit?.name ?: "") }
    var selectedColorHex by remember { mutableStateOf(folderToEdit?.colorHex ?: PASTEL_COLORS.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (folderToEdit == null) "Folder Baru" else "Edit Folder",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Folder") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Warna Aksen",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PASTEL_COLORS.forEach { (hex, label) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                    color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onConfirm(name.trim(), selectedColorHex)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (folderToEdit == null) "Buat" else "Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun DeleteFolderDialog(
    folder: Folder,
    onDismiss: () -> Unit,
    onConfirm: (keepNotes: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hapus Folder?",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "Bagaimana Anda ingin menangani catatan di dalam folder \"${folder.name}\"?"
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { onConfirm(true) }
                ) {
                    Text("Simpan Catatan (Uncategorized)")
                }
                Button(
                    onClick = { onConfirm(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus Semua")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
