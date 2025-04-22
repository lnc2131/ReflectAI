package screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import navigation.Screen

class LoginViewModel {
    private val auth = FirebaseAuth.getInstance()
    
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onError(Exception("Email and password cannot be empty"))
            return
        }
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    
    fun createAccount(email: String, password: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onError(Exception("Email and password cannot be empty"))
            return
        }
        
        if (password.length < 6) {
            onError(Exception("Password should be at least 6 characters"))
            return
        }
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }
    
    fun getCurrentUserEmail(): String {
        return auth.currentUser?.email ?: ""
    }
    
    fun signOut() {
        auth.signOut()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    isSignUp: Boolean = false
) {
    val viewModel = remember { LoginViewModel() }
    val scope = rememberCoroutineScope()
    
    // If user already logged in, go to home screen
    LaunchedEffect(Unit) {
        if (viewModel.isUserLoggedIn()) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    
    // State for form fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isCreateAccount by remember { mutableStateOf(isSignUp) }
    
    val focusManager = LocalFocusManager.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Title/Logo
            Text(
                text = "ReflectAI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // Form title
            Text(
                text = if (isCreateAccount) "Create Account" else "Login",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                isError = errorMessage.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = errorMessage.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus()
                        if (!isLoading) {
                            isLoading = true
                            errorMessage = ""
                            
                            val action = if (isCreateAccount) {
                                viewModel.createAccount(email, password, 
                                    onSuccess = {
                                        isLoading = false
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    },
                                    onError = { e ->
                                        isLoading = false
                                        errorMessage = e.message ?: "Sign up failed"
                                    }
                                )
                            } else {
                                viewModel.login(email, password,
                                    onSuccess = {
                                        isLoading = false
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    },
                                    onError = { e ->
                                        isLoading = false
                                        errorMessage = e.message ?: "Login failed"
                                    }
                                )
                            }
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
            
            // Login/Create Account button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = ""
                    
                    if (isCreateAccount) {
                        viewModel.createAccount(email, password, 
                            onSuccess = {
                                isLoading = false
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onError = { e ->
                                isLoading = false
                                errorMessage = e.message ?: "Sign up failed"
                            }
                        )
                    } else {
                        viewModel.login(email, password,
                            onSuccess = {
                                isLoading = false
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onError = { e ->
                                isLoading = false
                                errorMessage = e.message ?: "Login failed"
                            }
                        )
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(if (isCreateAccount) "Create Account" else "Login")
                }
            }
            
            // Toggle between login and create account
            TextButton(
                onClick = { isCreateAccount = !isCreateAccount },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    if (isCreateAccount) "Already have an account? Login" else "Need an account? Sign up",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}