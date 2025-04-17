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
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.example.reflectai.ui.theme.*
import model.JournalEntry
import navigation.Screen
import repository.FirebaseJournalRepository
import repository.SimpleJournalRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Current date state
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Repository for checking date entries
    val repository = remember { SimpleJournalRepository() }
    
    // Map of dates with entries
    var datesWithEntries by remember { mutableStateOf(mapOf<LocalDate, Boolean>()) }
    
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
            userId = "testUser",
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
                
                // Main content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Test API button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.TestAPI.route) },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Test API",
                                style = MaterialTheme.typography.labelMedium
                            )
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
                    
                    // Calendar grid - now larger
                    CalendarGrid(
                        currentMonth = currentMonth,
                        datesWithEntries = datesWithEntries,
                        onDateClick = onDateSelected
                    )
                }
            }
        } else {
            // Phone layout - just the calendar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Test API button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.TestAPI.route) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Test API",
                            style = MaterialTheme.typography.labelMedium
                        )
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
                
                // Calendar grid - now larger
                CalendarGrid(
                    currentMonth = currentMonth,
                    datesWithEntries = datesWithEntries,
                    onDateClick = onDateSelected
                )
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
    onDateClick: (LocalDate) -> Unit
) {
    // These will be the day headers (S, M, T, W, T, F, S)
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Day of week headers
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

        // Calendar days - larger grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp), // Make calendar bigger
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
                    onDateClick = onDateClick
                )
            }
        }
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
                                color = if (hasEntry) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
                
                // Show indicator dot for days with entries
                if (hasEntry) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}