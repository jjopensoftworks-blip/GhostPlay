package com.example.ghostplay

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ghostplay.ui.navigation.AppNavigation
import com.example.ghostplay.ui.theme.CyberPulseTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("GhostPlay", "Firebase initialization failed", e)
        }
        enableEdgeToEdge()
        setContent {
            CyberPulseTheme {
                AppNavigation()
            }
        }
    }
}
