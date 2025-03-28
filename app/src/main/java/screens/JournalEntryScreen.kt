package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import model.JournalEntry
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    navController: NavController,
    viewModel: JournalEntryViewModel
) {
    val scope = rememberCoroutineScope()
    var entryTitle by remember { mutableStateOf("") }
    var entryContent by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    val analysis by viewModel.analysis.collectAsState()

    // Add state for API test result
    var apiTestMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Journal Entry") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        // You can add an icon here if you want
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
            // Add the test button at the top
            Button(
                onClick = {
                    viewModel.testApiKey()
                    apiTestMessage = "Check Logcat for API key details"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test API Key")
            }

            // Show test message if available
            if (apiTestMessage.isNotEmpty()) {
                Text(
                    text = apiTestMessage,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = entryTitle,
                onValueChange = { entryTitle = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = entryContent,
                onValueChange = { entryContent = it },
                label = { Text("What's on your mind today?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 5
            )

            Button(
                onClick = {
                    if (entryContent.isNotBlank()) {
                        val entry = JournalEntry(
                            id = UUID.randomUUID().toString(),
                            userId = "testUser", // Replace with actual user ID in real app
                            title = entryTitle,
                            content = entryContent
                        )
                        isAnalyzing = true
                        scope.launch {
                            viewModel.saveAndAnalyzeEntry(entry)
                            isAnalyzing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Analyze")
            }

            // Show loading indicator while analyzing
            if (isAnalyzing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Display analysis results if available
            analysis?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "AI Analysis",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sentiment: ${result.sentiment}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Feedback: ${result.feedback}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}