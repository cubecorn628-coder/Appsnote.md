package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Folder
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    // Theme state (default to dark theme = true)
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // List of all folders
    val folders: StateFlow<List<Folder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected folder filter:
    // null -> All Notes
    // -1L -> Uncategorized Notes (folderId is null)
    // -2L -> Favorites Only
    // folderId -> Notes in specific folder
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Flow that emits notes based on selected folder and search query
    val filteredNotes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        _selectedFolderId,
        _searchQuery
    ) { notes, selectedFolderId, query ->
        var result = when (selectedFolderId) {
            null -> notes
            -1L -> notes.filter { it.folderId == null }
            -2L -> notes.filter { it.isFavorite }
            else -> notes.filter { it.folderId == selectedFolderId }
        }

        if (query.isNotEmpty()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active note in editor
    private val _activeNote = MutableStateFlow<Note?>(null)
    val activeNote: StateFlow<Note?> = _activeNote.asStateFlow()

    fun setActiveNote(note: Note?) {
        _activeNote.value = note
    }

    // Note operations
    fun createNewNote(title: String = "", content: String = "", folderId: Long? = null) {
        val newNote = Note(
            title = title.ifEmpty { "Catatan Baru" },
            content = content,
            folderId = folderId ?: _selectedFolderId.value?.let { if (it < 0L) null else it }
        )
        viewModelScope.launch {
            val id = repository.insertNote(newNote)
            val insertedNote = repository.getNoteById(id)
            _activeNote.value = insertedNote
        }
    }

    fun updateActiveNote(title: String, content: String, folderId: Long?) {
        val current = _activeNote.value ?: return
        val updated = current.copy(
            title = title,
            content = content,
            folderId = folderId,
            updatedAt = System.currentTimeMillis()
        )
        _activeNote.value = updated
        viewModelScope.launch {
            repository.updateNote(updated)
        }
    }

    fun deleteNote(note: Note) {
        if (_activeNote.value?.id == note.id) {
            _activeNote.value = null
        }
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleFavorite(note: Note) {
        val updated = note.copy(isFavorite = !note.isFavorite)
        if (_activeNote.value?.id == note.id) {
            _activeNote.value = updated
        }
        viewModelScope.launch {
            repository.updateNote(updated)
        }
    }

    // Folder operations
    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertFolder(Folder(name = name, colorHex = colorHex))
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    fun deleteFolder(folder: Folder, keepNotes: Boolean) {
        viewModelScope.launch {
            if (!keepNotes) {
                // Delete all notes inside this folder first
                repository.getNotesByFolder(folder.id).firstOrNull()?.forEach { note ->
                    repository.deleteNote(note)
                }
            }
            // Now delete folder itself
            repository.deleteFolder(folder)
            
            // Reset filter if deleted folder was active
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    // Copy uploaded file to internal files storage (fully offline/local)
    fun copyImageToLocal(context: Context, uri: Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri)
            val extension = when {
                mimeType == "image/png" -> "png"
                mimeType == "image/gif" -> "gif"
                mimeType == "image/webp" -> "webp"
                else -> "jpg"
            }
            val fileName = "img_${System.currentTimeMillis()}.$extension"
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            val destinationFile = File(imagesDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Import markdown note
    fun importMarkdownFile(context: Context, uri: Uri, targetFolderId: Long? = null) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                // Get display name (title of the note)
                var title = "Catatan Diimpor"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        val fileName = cursor.getString(nameIndex)
                        if (fileName.endsWith(".md", ignoreCase = true)) {
                            title = fileName.substring(0, fileName.length - 3)
                        } else {
                            title = fileName
                        }
                    }
                }

                // Read entire markdown file
                val contentBuilder = StringBuilder()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            contentBuilder.append(line).append("\n")
                            line = reader.readLine()
                        }
                    }
                }

                val noteContent = contentBuilder.toString().trimEnd()
                val folderToUse = targetFolderId ?: _selectedFolderId.value?.let { if (it < 0L) null else it }

                val importedNote = Note(
                    title = title,
                    content = noteContent,
                    folderId = folderToUse
                )
                repository.insertNote(importedNote)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

// ViewModel factory helper
class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
