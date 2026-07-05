package com.example.data.repository

import com.example.data.local.FolderDao
import com.example.data.local.NoteDao
import com.example.data.model.Folder
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao
) {
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val favoriteNotes: Flow<List<Note>> = noteDao.getFavoriteNotes()
    val uncategorizedNotes: Flow<List<Note>> = noteDao.getUncategorizedNotes()

    fun getNotesByFolder(folderId: Long): Flow<List<Note>> = noteDao.getNotesByFolder(folderId)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun insertFolder(folder: Folder): Long = folderDao.insertFolder(folder)

    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder)

    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    suspend fun getNotesCountInFolder(folderId: Long): Int = noteDao.getNotesCountInFolder(folderId)
}
