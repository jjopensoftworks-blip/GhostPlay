package com.example.ghostplay.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
                text = "NEON_INITIALIZATION",
                style = MaterialTheme.typography.displayMedium,
                color = Primary,
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                text = "IDENTIFY YOURSELF FOR THE CYBER-PULSE NETWORK",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("PLAYER_TAG", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Secondary,
                    unfocusedBorderColor = Outline,
                    cursorColor = Secondary,
                    focusedLabelColor = Secondary,
                    unfocusedLabelColor = OnSurfaceVariant
                ),
                singleLine = true
            )

            Button(
                onClick = { if (name.isNotBlank()) onNameSet(name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Primary, Secondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INITIATE_SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        color = OnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
