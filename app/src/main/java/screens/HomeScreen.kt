package screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.example.reflectai.ui.theme.*
import model.JournalEntry
import model.MoodCounts
import model.User
import navigation.Screen
import repository.SimpleJournalRepository
import repository.UserRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.google.firebase.database.FirebaseDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Current date state
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Repository for checking date entries
    val repository = remember { SimpleJournalRepository() }
    
    // Firebase database for user mood counts
    val database = remember { FirebaseDatabase.getInstance() }
    val usersRef = remember { database.getReference("users") }
    
    // Map of dates with entries
    var datesWithEntries by remember { mutableStateOf(mapOf<LocalDate, Boolean>()) }
    
    // User mood counts
    var moodCounts by remember { mutableStateOf(MoodCounts()) }
    var isLoadingMoodCounts by remember { mutableStateOf(true) }
    
    // Coroutine scope for repository calls
    val scope = rememberCoroutineScope()
    
    // Get window size info for responsive layout
    val windowInfo = rememberWindowInfo()
    val showSidebar = windowInfo.shouldShowSidebar
    
    // Key to force reload when the screen is opened 
    var reloadKey by remember { mutableStateOf(0) }
    
    // Force refresh on first display
    LaunchedEffect(Unit) {
        reloadKey++
        println("Initial load of HomeScreen, forcing refresh")
    }
    
    // Load mood counts
    LaunchedEffect(reloadKey) {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            // If not logged in, navigate to login screen
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        
        usersRef.child(userId).child("moodCounts").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Manual mapping since Firebase might not deserialize properly
                    val happy = snapshot.child("happy").getValue(Int::class.java) ?: 0
                    val neutral = snapshot.child("neutral").getValue(Int::class.java) ?: 0
                    val sad = snapshot.child("sad").getValue(Int::class.java) ?: 0
                    
                    moodCounts = MoodCounts(happy = happy, neutral = neutral, sad = sad)
                    println("Loaded mood counts: happy=$happy, neutral=$neutral, sad=$sad")
                } else {
                    moodCounts = MoodCounts()
                    println("No mood counts found in Firebase")
                }
                isLoadingMoodCounts = false
            }
            .addOnFailureListener { error ->
                println("Error loading mood counts: ${error.message}")
                isLoadingMoodCounts = false
            }
    }
    
    // Refresh when screen gains focus
    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == Screen.Home.route) {
                // Increment reload key to force LaunchedEffect to run again
                reloadKey++
                println("Returned to HomeScreen, forcing refresh with key=$reloadKey")
                
                // Clear and rebuild the dates map
                datesWithEntries = emptyMap()
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    
    // Load entries for the current month
    LaunchedEffect(currentMonth, reloadKey) {
        val firstDay = currentMonth.atDay(1)
        val lastDay = currentMonth.atEndOfMonth()
        
        // Build a map of dates with entries
        val dateMap = mutableMapOf<LocalDate, Boolean>()
        
        // Get all entries for this user
        repository.getEntries(
            onSuccess = { entries ->
                println("Found ${entries.size} total entries")
                
                // Check each entry's date
                entries.forEach { entry ->
                    try {
                        println("Processing entry: ID=${entry.id}, date=${entry.date}")
                        val entryDate = JournalEntry.toDate(entry.date)
                        // If this entry is in the current month
                        if (entryDate.month == currentMonth.month && 
                            entryDate.year == currentMonth.year) {
                            dateMap[entryDate] = true
                            println("Adding date ${entryDate} to highlighted dates")
                        } else {
                            println("Entry date ${entryDate} is not in current month ${currentMonth}")
                        }
                    } catch (e: Exception) {
                        println("Error parsing date ${entry.date}: ${e.message}")
                    }
                }
                
                // Update the state
                datesWithEntries = dateMap
                println("Updated dates with entries: ${dateMap.keys}")
            },
            onError = { error ->
                println("Error getting entries: ${error.message}")
            }
        )
    }
    
    // Function to handle date selection
    val onDateSelected = { date: LocalDate ->
        selectedDate = date
        val formattedDate = JournalEntry.fromDate(date)
        if (datesWithEntries.containsKey(date)) {
            // Navigate to existing entry for this date
            navController.navigate(Screen.EntryForDate.createRoute(formattedDate))
        } else {
            // Create new entry for this date
            navController.navigate(Screen.NewEntryWithDate.createRoute(formattedDate))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "ReflectAI", 
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    val formattedDate = JournalEntry.fromDate(selectedDate)
                    navController.navigate(Screen.NewEntryWithDate.createRoute(formattedDate))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Entry"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        // When on a tablet in landscape mode, show the sidebar
        if (showSidebar) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Left sidebar with weeks
                WeekSidebar(
                    currentDate = selectedDate,
                    currentMonth = currentMonth,
                    datesWithEntries = datesWithEntries,
                    onDateSelected = onDateSelected,
                    modifier = Modifier.fillMaxHeight()
                )
                
                // Divider between sidebar and main content
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                
                // Main content area with calendar and mood stats at the bottom
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Row {
                            // Logout button
                            val userRepo = remember { UserRepository() }
                            OutlinedButton(
                                onClick = {
                                    userRepo.signOut()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Logout",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Month navigation
                    MonthNavigator(
                        currentMonth = currentMonth,
                        onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                    )
    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Calendar grid - without mood stats
                    Column(modifier = Modifier.weight(1f)) {
                        // Day of week headers
                        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Light
                                    )
                                )
                            }
                        }

                        // Calendar days grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Get all the days in the month, plus padding days for the grid
                            val firstDayOfMonth = currentMonth.atDay(1)
                            val lastDayOfMonth = currentMonth.atEndOfMonth()

                            // Padding for days before the 1st of the month
                            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
                            val paddingDaysStart = (0 until firstDayOfWeek).map { null }

                            // Actual days of the month
                            val daysInMonth = (1..lastDayOfMonth.dayOfMonth).map { day ->
                                currentMonth.atDay(day)
                            }

                            // Combine padding and actual days
                            val allDays = paddingDaysStart + daysInMonth

                            items(allDays) { date ->
                                CalendarDay(
                                    date = date,
                                    hasEntry = date != null && datesWithEntries.containsKey(date),
                                    onDateClick = onDateSelected
                                )
                            }
                        }
                    }
                    
                    // Mood stats at the bottom
                    MoodStats(moodCounts, isLoadingMoodCounts)
                }
            }
        } else {
            // Phone layout - just the calendar with mood stats at the bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Row {
                        // Logout button
                        val userRepo = remember { UserRepository() }
                        OutlinedButton(
                            onClick = {
                                userRepo.signOut()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Logout",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Month navigation
                MonthNavigator(
                    currentMonth = currentMonth,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Calendar grid (without mood stats)
                Column(modifier = Modifier.weight(1f)) {
                    // Day of week headers
                    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Light
                                )
                            )
                        }
                    }

                    // Calendar days grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Get all the days in the month, plus padding days for the grid
                        val firstDayOfMonth = currentMonth.atDay(1)
                        val lastDayOfMonth = currentMonth.atEndOfMonth()

                        // Padding for days before the 1st of the month
                        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
                        val paddingDaysStart = (0 until firstDayOfWeek).map { null }

                        // Actual days of the month
                        val daysInMonth = (1..lastDayOfMonth.dayOfMonth).map { day ->
                            currentMonth.atDay(day)
                        }

                        // Combine padding and actual days
                        val allDays = paddingDaysStart + daysInMonth

                        items(allDays) { date ->
                            CalendarDay(
                                date = date,
                                hasEntry = date != null && datesWithEntries.containsKey(date),
                                onDateClick = onDateSelected
                            )
                        }
                    }
                }
                
                // Mood stats at the bottom
                MoodStats(moodCounts, isLoadingMoodCounts)
            }
        }
    }
}

@Composable
fun MonthNavigator(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    datesWithEntries: Map<LocalDate, Boolean>,
    onDateClick: (LocalDate) -> Unit,
    moodCounts: MoodCounts,
    isLoadingMoodCounts: Boolean
) {
    // Note: This function is kept for backward compatibility but the implementation 
    // has been moved directly into the HomeScreen layouts
    // This implementation is no longer used - see the inline calendar implementation in HomeScreen
}

@Composable
fun MoodStats(moodCounts: MoodCounts, isLoading: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Mood Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Happy count
                    MoodCounter(
                        emoji = "😊",
                        count = moodCounts.happy,
                        color = Color(0xFF81C995)
                    )
                    
                    // Neutral count
                    MoodCounter(
                        emoji = "😐",
                        count = moodCounts.neutral,
                        color = Color(0xFFFDD663)
                    )
                    
                    // Sad count
                    MoodCounter(
                        emoji = "😔",
                        count = moodCounts.sad,
                        color = Color(0xFFF28B82)
                    )
                }
                
                // Add total entries count
                val totalEntries = moodCounts.total()
                if (totalEntries > 0) {
                    Text(
                        text = "Total journal entries: $totalEntries",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "No journal entries yet. Start by adding one!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun MoodCounter(emoji: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}

@Composable
fun CalendarDay(
    date: LocalDate?,
    hasEntry: Boolean,
    onDateClick: (LocalDate) -> Unit
) {
    // Identify today's date
    val today = LocalDate.now()
    val isToday = date == today
    
    // Repository to check entry details
    val repository = remember { SimpleJournalRepository() }
    var entryMood by remember { mutableStateOf<String?>(null) }
    
    // Check for entry mood if this date has an entry
    LaunchedEffect(date, hasEntry) {
        if (date != null && hasEntry) {
            val dateStr = JournalEntry.fromDate(date)
            repository.getEntriesByDate(
                dateString = dateStr,
                onSuccess = { entries ->
                    if (entries.isNotEmpty()) {
                        entryMood = entries.first().mood
                    }
                },
                onError = { /* ignore errors */ }
            )
        }
    }
    
    // Determine the mood color for entry
    val moodColor = when (entryMood) {
        "sad" -> Color(0xFFF28B82)  // Red
        "happy" -> Color(0xFF81C995)  // Green
        "neutral" -> Color(0xFFFDD663)  // Yellow
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Make each day cell a square
            .then(
                if (date != null) {
                    if (isToday) {
                        Modifier
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .background(MaterialTheme.colorScheme.background)
                            .clickable { onDateClick(date) }
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .border(
                                width = 0.5.dp,
                                color = if (hasEntry) moodColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { onDateClick(date) }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        date?.let {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = it.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isToday || hasEntry) FontWeight.Medium else FontWeight.Light
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Show indicator dot for days with entries, colored by mood
                if (hasEntry) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = moodColor,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}