package com.example.ghostplay.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.data.model.Game
import com.example.ghostplay.data.repository.FirebaseGameRepository
import com.example.ghostplay.data.repository.GameRepository
import kotlinx.coroutines.launch

class AddGameViewModel(
    private val repository: GameRepository = FirebaseGameRepository()
) : ViewModel() {
    fun addGame(name: String, platform: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.addGame(Game(name = name, platform = platform))
            onComplete()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGameScreen(
    onBack: () -> Unit,
    onGameAdded: () -> Unit,
    viewModel: AddGameViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Game Name") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("Platform (e.g. PC, PS5)") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (name.isNotBlank() && platform.isNotBlank()) {
                        viewModel.addGame(name, platform, onGameAdded)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && platform.isNotBlank()
            ) {
                Text("Add Game")
            }
        }
    }
}
