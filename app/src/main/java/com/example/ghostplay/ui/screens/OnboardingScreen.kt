package com.example.ghostplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ghostplay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onNameSet: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isInitializing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "NEON_IDENTITY_SETUP",
                style = MaterialTheme.typography.displayMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp
            )
            
            Text(
                text = "ESTABLISH PLAYER_TAG FOR THE NETWORK",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (!isInitializing) name = it },
                label = { Text("PLAYER_TAG", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = Outline,
                    cursorColor = Secondary,
                    focusedLabelColor = Secondary,
                    unfocusedLabelColor = OnSurfaceVariant,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface
                ),
                singleLine = true,
                enabled = !isInitializing
            )

            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        isInitializing = true
                        onNameSet(name)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Secondary,
                    contentColor = OnSecondary,
                    disabledContainerColor = SurfaceContainerHighest
                ),
                enabled = name.isNotBlank() && !isInitializing
            ) {
                if (isInitializing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = OnSecondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "INITIATE_SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
