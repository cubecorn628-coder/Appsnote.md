package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.components.AutocompleteToolbar
import com.example.ui.components.SlashCommandPopover
import com.example.ui.markdown.MarkdownRenderer
import com.example.ui.viewmodel.NoteViewModel

enum class EditorViewMode {
    EDIT, PREVIEW, SPLIT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NoteViewModel,
    note: Note,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()

    var titleState by remember { mutableStateOf(note.title) }
    var contentState by remember { mutableStateOf(TextFieldValue(note.content)) }
    var folderIdState by remember { mutableStateOf(note.folderId) }

    var viewMode by remember { mutableStateOf(EditorViewMode.EDIT) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var videoUrlInput by remember { mutableStateOf("") }

    // Auto-save changes locally to Room database
    LaunchedEffect(titleState, contentState.text, folderIdState) {
        viewModel.updateActiveNote(titleState, contentState.text, folderIdState)
    }

    // Modern SAF export launcher to export .md files securely offline
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(contentState.text.toByteArray())
                    }
                    Toast.makeText(context, "Catatan berhasil diekspor ke Markdown!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Gagal mengekspor catatan.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Image upload handler (copies gallery image to sandbox local directory)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                val localPath = viewModel.copyImageToLocal(context, uri)
                if (localPath != null) {
                    // Insert markdown image syntax at cursor position
                    val imageSyntax = "![Gambar Lokal]($localPath)\n"
                    val currentText = contentState.text
                    val start = contentState.selection.start
                    val end = contentState.selection.end
                    val before = currentText.substring(0, start)
                    val after = currentText.substring(end)
                    val newText = before + imageSyntax + after
                    val newCursorPos = start + imageSyntax.length
                    
                    contentState = TextFieldValue(
                        text = newText,
                        selection = androidx.compose.ui.text.TextRange(newCursorPos)
                    )
                    Toast.makeText(context, "Gambar berhasil disematkan secara luring!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal memproses gambar.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // Helper functions for inserting formatting
    fun insertSyntax(syntax: String, cursorOffset: Int) {
        val currentText = contentState.text
        val start = contentState.selection.start
        val end = contentState.selection.end
        val before = currentText.substring(0, start)
        val after = currentText.substring(end)
        val newText = before + syntax + after
        val newCursorPos = start + syntax.length - cursorOffset
        
        contentState = TextFieldValue(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newCursorPos)
        )
    }

    // Slash command query matching
    val currentText = contentState.text
    val selection = contentState.selection
    val cursorPosition = selection.start

    var slashQuery by remember { mutableStateOf<String?>(null) }
    var slashIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(currentText, cursorPosition) {
        if (cursorPosition > 0) {
            val lastSlash = currentText.substring(0, cursorPosition).lastIndexOf('/')
            if (lastSlash != -1) {
                val wordAfterSlash = currentText.substring(lastSlash + 1, cursorPosition)
                if (!wordAfterSlash.contains(" ") && !wordAfterSlash.contains("\n")) {
                    slashQuery = wordAfterSlash
                    slashIndex = lastSlash
                } else {
                    slashQuery = null
                }
            } else {
                slashQuery = null
            }
        } else {
            slashQuery = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Editor Catatan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Export Button
                    IconButton(
                        onClick = {
                            val fileName = titleState.ifEmpty { "catatan" }
                                .replace("[^a-zA-Z0-9]".toRegex(), "_")
                                .lowercase() + ".md"
                            exportLauncher.launch(fileName)
                        }
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Ekspor (.md)")
                    }

                    // Folder Assignment dropdown
                    Box {
                        IconButton(onClick = { showFolderMenu = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Pilih Folder")
                        }
                        DropdownMenu(
                            expanded = showFolderMenu,
                            onDismissRequest = { showFolderMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tanpa Folder") },
                                onClick = {
                                    folderIdState = null
                                    showFolderMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FolderOff, contentDescription = null) }
                            )
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                            folders.forEach { f ->
                                val color = Color(android.graphics.Color.parseColor(f.colorHex))
                                DropdownMenuItem(
                                    text = { Text(f.name) },
                                    onClick = {
                                        folderIdState = f.id
                                        showFolderMenu = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(color)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Favorite Toggle
                    IconButton(onClick = { viewModel.toggleFavorite(note) }) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Tandai Favorit",
                            tint = if (note.isFavorite) Color(0xFFFFD185) else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (viewMode == EditorViewMode.EDIT || viewMode == EditorViewMode.SPLIT) {
                Column {
                    // Slash Commands suggestions
                    slashQuery?.let { q ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            SlashCommandPopover(
                                query = q,
                                onSelectCommand = { syntax, offset, isAction, actionType ->
                                    // Remove slash and command word
                                    val beforeSlash = currentText.substring(0, slashIndex)
                                    val afterCursor = currentText.substring(cursorPosition)
                                    
                                    if (isAction) {
                                        // Execute action
                                        contentState = TextFieldValue(
                                            text = beforeSlash + afterCursor,
                                            selection = androidx.compose.ui.text.TextRange(slashIndex)
                                        )
                                        if (actionType == "image") {
                                            imagePickerLauncher.launch("image/*")
                                        } else if (actionType == "video") {
                                            showVideoDialog = true
                                        }
                                    } else {
                                        // Insert inline syntax
                                        val replacedText = beforeSlash + syntax + afterCursor
                                        val newCursor = slashIndex + syntax.length - offset
                                        contentState = TextFieldValue(
                                            text = replacedText,
                                            selection = androidx.compose.ui.text.TextRange(newCursor)
                                        )
                                    }
                                    slashQuery = null
                                }
                            )
                        }
                    }

                    AutocompleteToolbar(
                        onSyntaxInsert = { syntax, offset -> insertSyntax(syntax, offset) },
                        onUploadImage = { imagePickerLauncher.launch("image/*") },
                        onEmbedVideo = { showVideoDialog = true }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // View Mode Toggles (Edit, Preview, Split)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditorViewMode.values().forEach { mode ->
                    val selected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { viewMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                EditorViewMode.EDIT -> "Tulis"
                                EditorViewMode.PREVIEW -> "Preview"
                                EditorViewMode.SPLIT -> "Split"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // Body Area based on View Mode
            when (viewMode) {
                EditorViewMode.EDIT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Title Input
                        BasicTextField(
                            value = titleState,
                            onValueChange = { titleState = it },
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (titleState.isEmpty()) {
                                        Text(
                                            text = "Judul Catatan",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Markdown Editor text field
                        BasicTextField(
                            value = contentState,
                            onValueChange = { contentState = it },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 24.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    if (contentState.text.isEmpty()) {
                                        Text(
                                            text = "Mulai menulis Markdown... ketik '/' untuk pintasan format",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                EditorViewMode.PREVIEW -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        // Title
                        Text(
                            text = titleState.ifEmpty { "Tanpa Judul" },
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Render rich markdown
                        MarkdownRenderer(
                            markdown = contentState.text,
                            onTodoCheckedChange = { index, checked ->
                                // Optional interactive todo toggling
                                val lines = contentState.text.split("\n").toMutableList()
                                var todoIdx = 0
                                for (i in lines.indices) {
                                    val trimmed = lines[i].trim()
                                    if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                                        trimmed.startsWith("* [ ] ") || trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ")) {
                                        if (todoIdx == index) {
                                            val isX = trimmed.contains("[x]", ignoreCase = true)
                                            val sign = if (trimmed.startsWith("-")) "-" else "*"
                                            val newSymbol = if (checked) "$sign [x]" else "$sign [ ]"
                                            val rest = trimmed.substring(5)
                                            lines[i] = newSymbol + rest
                                            break
                                        }
                                        todoIdx++
                                    }
                                }
                                contentState = TextFieldValue(
                                    text = lines.joinToString("\n"),
                                    selection = contentState.selection
                                )
                            }
                        )
                    }
                }

                EditorViewMode.SPLIT -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Column (Editor)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = titleState,
                                onValueChange = { titleState = it },
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (titleState.isEmpty()) {
                                            Text(
                                                text = "Judul",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BasicTextField(
                                value = contentState,
                                onValueChange = { contentState = it },
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onBackground
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Right Column (Preview)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = titleState.ifEmpty { "Tanpa Judul" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MarkdownRenderer(markdown = contentState.text)
                        }
                    }
                }
            }
        }
    }

    // Video Embed Dialog
    if (showVideoDialog) {
        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            title = {
                Text(
                    text = "Sematkan Video",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Tempel tautan video eksternal (YouTube, Vimeo, atau direct berkas MP4).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = videoUrlInput,
                        onValueChange = { videoUrlInput = it },
                        label = { Text("URL Video") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (videoUrlInput.trim().isNotEmpty()) {
                            // Insert video embed syntax at cursor
                            val videoSyntax = "\n![video](${videoUrlInput.trim()})\n"
                            val currentText = contentState.text
                            val start = contentState.selection.start
                            val end = contentState.selection.end
                            val before = currentText.substring(0, start)
                            val after = currentText.substring(end)
                            val newText = before + videoSyntax + after
                            val newCursorPos = start + videoSyntax.length
                            
                            contentState = TextFieldValue(
                                text = newText,
                                selection = androidx.compose.ui.text.TextRange(newCursorPos)
                            )
                            videoUrlInput = ""
                            showVideoDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sematkan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        videoUrlInput = ""
                        showVideoDialog = false
                    }
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
