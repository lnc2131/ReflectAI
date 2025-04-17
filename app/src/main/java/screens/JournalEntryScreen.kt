package screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.reflectai.ui.theme.WindowSize
import com.example.reflectai.ui.theme.rememberWindowSize
import kotlinx.coroutines.launch
import model.JournalEntry
import navigation.Screen
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    viewModel: JournalEntryViewModel,
    onBackClick: () -> Unit = {},
    initialDate: String = "",
    entryId: String = "",
    isExistingEntry: Boolean = false,
    navController: NavController? = null
) {
    var content by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("neutral") }

    val isLoading by viewModel.isLoading.collectAsState()
    val responseText by viewModel.responseText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    
    // Get the entry if we're loading an existing one
    val entry by viewModel.entry.collectAsState()
    
    // Parse the current date for the week sidebar
    val currentDate = if (initialDate.isNotEmpty()) {
        try {
            LocalDate.parse(initialDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }
    } else {
        LocalDate.now()
    }
    
    val currentMonth = YearMonth.from(currentDate)
    
    // Map to track dates with entries
    val datesWithEntries = remember { mutableStateMapOf<LocalDate, Boolean>() }
    
    // Load entries for the current week for the sidebar
    LaunchedEffect(currentDate) {
        // Get Monday of the current week
        val startOfWeek = currentDate.with(DayOfWeek.MONDAY)
        // Get Friday of the current week
        val endOfWeek = startOfWeek.plusDays(4) // Friday is 4 days after Monday
        
        // Format dates for repository
        val startDateStr = startOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endDateStr = endOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        // Get entries for the date range
        viewModel.getEntryDatesForWeek(startDateStr, endDateStr) { dateMap ->
            // Clear previous entries
            datesWithEntries.clear()
            
            // Convert string dates to LocalDate and add to the map
            dateMap.forEach { (dateStr, hasEntry) ->
                try {
                    val date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                    datesWithEntries[date] = hasEntry
                } catch (e: Exception) {
                    println("Error parsing date: $dateStr - ${e.message}")
                }
            }
        }
    }
    
    // Window size detection for responsive layout
    val windowSize = rememberWindowSize()
    val isTablet = windowSize != WindowSize.Compact
    
    // Format date for display
    val displayDate = if (initialDate.isNotEmpty()) {
        try {
            println("Formatting initialDate for display: $initialDate")
            val date = LocalDate.parse(initialDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val formatted = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
            println("Formatted date for display: $formatted")
            formatted
        } catch (e: Exception) {
            println("Error formatting date for display: ${e.message}")
            initialDate // Fallback to showing the raw date string
        }
    } else {
        "New Entry"
    }
    
    // Effect to load entry when screen appears
    LaunchedEffect(initialDate, entryId) {
        if (isExistingEntry) {
            if (initialDate.isNotEmpty()) {
                viewModel.loadEntryByDate(initialDate)
            } else if (entryId.isNotEmpty()) {
                viewModel.loadEntryById(entryId)
            }
        }
    }
    
    // Update UI when entry changes
    LaunchedEffect(entry) {
        entry?.let {
            content = it.content
            selectedMood = it.mood
        }
    }

    val scope = rememberCoroutineScope()
    
    // Create a weekly sidebar for tablets
    val onDateSelected: (LocalDate) -> Unit = { date ->
        // Get formatted date string for navigation
        val formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        navController?.navigate(Screen.EntryForDate.createRoute(formattedDate))
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                // Date info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                    
                    // Date display
                    if (displayDate.isNotEmpty() && displayDate != "New Entry") {
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    } else {
                        Text(
                            text = if (isExistingEntry) "Journal Entry" else "New Entry", 
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    
                    // Empty space to balance layout
                    Box(modifier = Modifier.width(48.dp))
                }
                
                // Mood selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Sad mood
                    MoodButton(
                        mood = "sad",
                        emoji = "😔",
                        color = Color(0xFFF28B82),  // Red
                        isSelected = selectedMood == "sad",
                        onClick = { selectedMood = "sad" }
                    )
                    
                    // Neutral mood
                    MoodButton(
                        mood = "neutral",
                        emoji = "😐",
                        color = Color(0xFFFDD663),  // Yellow
                        isSelected = selectedMood == "neutral",
                        onClick = { selectedMood = "neutral" }
                    )
                    
                    // Happy mood
                    MoodButton(
                        mood = "happy",
                        emoji = "😊",
                        color = Color(0xFF81C995),  // Green
                        isSelected = selectedMood == "happy", 
                        onClick = { selectedMood = "happy" }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize()) {
            // Weekly sidebar for tablets - only show on tablet sizes
            if (isTablet && navController != null) {
                // Convert current date to beginning of the week (Monday)
                val startOfWeek = currentDate.with(DayOfWeek.MONDAY)
                
                // Create a row with weekday names and navigation
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .fillMaxHeight()
                        .padding(top = padding.calculateTopPadding())
                ) {
                    Text(
                        text = "This Week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                    
                    // Monday through Friday buttons
                    for (i in 0..4) {
                        val date = startOfWeek.plusDays(i.toLong())
                        val isSelected = date.isEqual(currentDate)
                        val hasEntry = datesWithEntries[date] == true
                        
                        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        val dateText = date.format(DateTimeFormatter.ofPattern("MMM d"))
                        
                        WeekdayButton(
                            dayName = dayName,
                            dateText = dateText,
                            isSelected = isSelected,
                            hasEntry = hasEntry,
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
                
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
            
            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // Content input with minimal styling
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind today?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                    minLines = 5
                )

                // Button row with minimalist design
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Save without analysis - outlined button
                    OutlinedButton(
                        onClick = {
                            if (content.isNotBlank()) {
                                scope.launch {
                                    viewModel.saveWithoutAnalysis(content, selectedMood)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && content.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text("Save Only")
                    }

                    // Analyze & Save - filled button 
                    Button(
                        onClick = {
                            if (content.isNotBlank()) {
                                scope.launch {
                                    viewModel.processJournalEntry(content, selectedMood)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && content.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(if (isLoading) "Processing..." else "Get Support")
                    }
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

                // Success message with minimalist styling
                if (saveSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text(
                            text = "Entry saved successfully!",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // AI Response display with minimalist styling
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
                                text = "Therapist Response",
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
}

@Composable
fun MoodButton(
    mood: String,
    emoji: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) color else color.copy(alpha = 0.5f),
                shape = androidx.compose.foundation.shape.CircleShape
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 32.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WeekdayButton(
    dayName: String,
    dateText: String,
    isSelected: Boolean,
    hasEntry: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) 
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else 
        MaterialTheme.colorScheme.background
    
    val textColor = if (isSelected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onBackground
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary) 
        else 
            null,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
            
            // Indicator for entries
            if (hasEntry) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}