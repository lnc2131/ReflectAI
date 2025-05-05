package com.example.reflectai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.reflectai.ui.theme.ReflectAITheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp
import androidx.navigation.compose.rememberNavController
import navigation.AppNavigation
import screens.JournalEntryViewModel

class MainActivity : ComponentActivity() {
    // Shared ViewModel instance for speech recognition
    private var viewModel: JournalEntryViewModel? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        val database = FirebaseDatabase.getInstance()
        val auth = Firebase.auth
        
        setContent {
            ReflectAITheme {
                val navController = rememberNavController()
                
                Scaffold {innerPadding ->
                    AppNavigation(
                        navController = navController,
                        modifier = Modifier.fillMaxSize().padding(innerPadding)
                    )
                }
            }
        }
    }
    
    // Handle activity result for speech recognition
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel?.processSpeechResult(requestCode, resultCode, data)
    }
    
    // Method to set the current ViewModel for speech recognition
    fun setViewModel(viewModel: JournalEntryViewModel) {
        this.viewModel = viewModel
    }
}

// Unused MainScreen and Preview components removed