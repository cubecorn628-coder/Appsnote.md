package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.DeleteFolderDialog
import com.example.ui.components.PASTEL_COLORS
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: NoteViewModel,
    onNoteSelected: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val notes by viewModel.filteredNotes.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToEdit by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    // Launcher for importing .md files
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importMarkdownFile(context, uri)
                Toast.makeText(context, "Mulai mengimpor berkas Markdown...", Toast.LENGTH_SHORT).show()
            }
        }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folder Lokal",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showCreateFolderDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "Folder Baru",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Predefined Folders / Quick Filters
                NavigationDrawerItem(
                    label = { Text("Semua Catatan", fontWeight = FontWeight.SemiBold) },
                    selected = selectedFolderId == null,
                    onClick = {
                        viewModel.selectFolder(null)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Favorit", fontWeight = FontWeight.SemiBold) },
                    selected = selectedFolderId == -2L,
                    onClick = {
                        viewModel.selectFolder(-2L)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD185)) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Tanpa Folder", fontWeight = FontWeight.SemiBold) },
                    selected = selectedFolderId == -1L,
                    onClick = {
                        viewModel.selectFolder(-1L)
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.FolderOff, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Divider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                // Custom Folders List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (folders.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada folder kustom. Buat folder untuk mengelompokkan catatan Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(folders) { folder ->
                                val folderColor = Color(android.graphics.Color.parseColor(folder.colorHex))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (selectedFolderId == folder.id) MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.4f
                                            ) else Color.Transparent
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                viewModel.selectFolder(folder.id)
                                                scope.launch { drawerState.close() }
                                            },
                                            onLongClick = {
                                                folderToEdit = folder
                                            }
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(folderColor)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = folder.name,
                                        fontWeight = if (selectedFolderId == folder.id) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedFolderId == folder.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { folderToDelete = folder },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus Folder",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // App version / Info
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LokalNote v1.0 • 100% Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "LokalNote",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            val folderLabel = when (selectedFolderId) {
                                null -> "Semua Catatan"
                                -1L -> "Tanpa Folder"
                                -2L -> "Favorit"
                                else -> folders.find { it.id == selectedFolderId }?.name ?: "Folder"
                            }
                            Text(
                                text = folderLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Buka Laci")
                        }
                    },
                    actions = {
                        // Import Markdown
                        IconButton(
                            onClick = {
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Impor Berkas Markdown",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Toggle Dark theme
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Ganti Tema"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.createNewNote() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Catatan Baru",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Cari judul atau isi catatan...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes Display
                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Pencarian tidak ditemukan" else "Mulai Catatan Pertamamu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Coba kata kunci lain." else "Buat catatan Markdown yang terenkripsi lokal dan anti cloud.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(notes) { note ->
                            NoteCard(
                                note = note,
                                folder = folders.find { it.id == note.folderId },
                                onCardClick = { onNoteSelected(note) },
                                onFavoriteToggle = { viewModel.toggleFavorite(note) },
                                onDeleteClick = { viewModel.deleteNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name, hex ->
                viewModel.createFolder(name, hex)
                showCreateFolderDialog = false
            }
        )
    }

    if (folderToEdit != null) {
        CreateFolderDialog(
            onDismiss = { folderToEdit = null },
            onConfirm = { name, hex ->
                viewModel.updateFolder(folderToEdit!!.copy(name = name, colorHex = hex))
                folderToEdit = null
            },
            folderToEdit = folderToEdit
        )
    }

    if (folderToDelete != null) {
        DeleteFolderDialog(
            folder = folderToDelete!!,
            onDismiss = { folderToDelete = null },
            onConfirm = { keepNotes ->
                viewModel.deleteFolder(folderToDelete!!, keepNotes)
                folderToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    folder: Folder?,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val folderColor = remember(folder) {
        folder?.let { Color(android.graphics.Color.parseColor(it.colorHex)) } ?: Color.Transparent
    }

    val formattedDate = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = { showMenu = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder Tag Indicator
                        if (folder != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(folderColor.copy(alpha = 0.25f))
                                    .border(width = 1.dp, color = folderColor.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = folder.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Box(modifier = Modifier.size(1.dp))
                        }

                        // Star Favorite Button
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Tandai Favorit",
                                tint = if (note.isFavorite) Color(0xFFFFD185) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = note.title.ifEmpty { "Tanpa Judul" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = note.content,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }

            // Long Press Options Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Hapus Catatan") },
                    onClick = {
                        onDeleteClick()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}
