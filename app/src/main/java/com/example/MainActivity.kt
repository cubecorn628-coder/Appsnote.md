package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.NoteDatabase
import com.example.data.repository.NoteRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NoteViewModel
import com.example.ui.viewmodel.NoteViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room Database & Repository
        val database = NoteDatabase.getDatabase(this)
        val repository = NoteRepository(database.folderDao(), database.noteDao())

        setContent {
            // Get our unified NoteViewModel using the customized factory
            val viewModel: NoteViewModel = viewModel(
                factory = NoteViewModelFactory(repository)
            )

            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val activeNote by viewModel.activeNote.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (activeNote == null) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNoteSelected = { note ->
                                viewModel.setActiveNote(note)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        // Handle hardware back button inside Editor to return to dashboard
                        BackHandler {
                            viewModel.setActiveNote(null)
                        }

                        EditorScreen(
                            viewModel = viewModel,
                            note = activeNote!!,
                            onBack = {
                                viewModel.setActiveNote(null)
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

