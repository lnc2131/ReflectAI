package com.example.reflectai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.reflectai.ui.theme.ReflectAITheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.FirebaseApp
import androidx.navigation.compose.rememberNavController
import navigation.AppNavigation

class MainActivity : ComponentActivity() {
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
}

@Composable
fun MainScreen() {

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ReflectAITheme {
        MainScreen()
    }
}