package screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import api.OpenAIClient
import api.OpenAIRequest
import com.example.reflectai.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestAPIScreen(onBackClick: () -> Unit) {
    var promptText by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "API Test", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // API key status with minimal styling
            val apiKey = BuildConfig.OPENAI_API_KEY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API Key Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (apiKey.isNotEmpty()) "✓ Set" else "✗ Missing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (apiKey.isNotEmpty()) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Length: ${apiKey.length}, Starts with: ${if (apiKey.length >= 5) apiKey.take(5) else apiKey}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Input field with minimal styling
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("Enter your prompt") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Light
                ),
                minLines = 3
            )

            // Send button with minimalist design
            Button(
                onClick = {
                    if (promptText.isNotEmpty()) {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = ""
                            try {
                                // Create simple request
                                val messages = listOf(
                                    OpenAIRequest.Message(
                                        role = "user",
                                        content = promptText
                                    )
                                )

                                val request = OpenAIRequest(messages = messages)

                                println("Test API: Making request with model=${request.model}")
                                val response = OpenAIClient.service.createChatCompletion(
                                    authorization = OpenAIClient.getAuthHeader(),
                                    request = request
                                )

                                if (response.isSuccessful) {
                                    val content = response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
                                    responseText = content
                                    println("Test API: Successful response")
                                } else {
                                    val error = response.errorBody()?.string() ?: "Unknown error"
                                    errorMessage = "API Error: $error"
                                    println("Test API: Error response: $error")
                                }
                            } catch (e: Exception) {
                                errorMessage = "Exception: ${e.message}"
                                e.printStackTrace()
                                println("Test API: Exception: ${e.javaClass.simpleName}: ${e.message}")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && promptText.isNotEmpty(),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            ) {
                Text(if (isLoading) "Processing..." else "Send to OpenAI")
            }

            // Loading indicator - thin line
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(1.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            // Error message with minimalist styling
            if (errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Response display with minimalist styling
            if (responseText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Response",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = responseText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Light
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}