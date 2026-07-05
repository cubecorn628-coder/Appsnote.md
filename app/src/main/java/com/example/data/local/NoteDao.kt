package com.example.data.local

import androidx.room.*
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId IS NULL ORDER BY updatedAt DESC")
    fun getUncategorizedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId")
    suspend fun getNotesCountInFolder(folderId: Long): Int
}
