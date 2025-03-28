package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun LoginScreen(navController: NavController) {
    // State for email and password fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // We'll add the UI elements here
}