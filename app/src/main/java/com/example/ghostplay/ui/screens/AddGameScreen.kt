package com.example.ghostplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ghostplay.data.model.Game
import com.example.ghostplay.data.repository.FirebaseGameRepository
import com.example.ghostplay.data.repository.GameRepository
import com.example.ghostplay.ui.theme.*
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
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = { Text("REGISTER_NEW_MODULE", style = MaterialTheme.typography.labelMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "DATA_ENTRY_PROTOCOL",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("MODULE_NAME", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = OutlineVariant,
                    cursorColor = Secondary
                )
            )

            OutlinedTextField(
                value = platform,
                onValueChange = { platform = it },
                label = { Text("HARDWARE_TARGET", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = OutlineVariant,
                    cursorColor = Secondary
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank() && platform.isNotBlank()) {
                        viewModel.addGame(name, platform, onGameAdded)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (name.isNotBlank() && platform.isNotBlank()) Brush.horizontalGradient(listOf(Primary, Secondary)) else Brush.linearGradient(listOf(SurfaceBright, SurfaceBright))),
                enabled = name.isNotBlank() && platform.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    "INITIALIZE_REGISTRATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (name.isNotBlank() && platform.isNotBlank()) OnPrimary else OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
