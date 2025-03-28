package screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import navigation.Screen
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    // Current date state
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ReflectAI Journal") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewEntry.route) }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Entry"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month navigation
            MonthNavigator(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )

            // Calendar grid
            CalendarGrid(
                currentMonth = currentMonth,
                onDateClick = { date ->
                    // This will handle navigation to entry view or creation
                    // For new entries on empty dates
                    navController.navigate(Screen.NewEntry.route)
                    // For existing entries (we'll update this logic later)
                    // navController.navigate(Screen.EntryDetail.createRoute("entry-id"))
                }
            )
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
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                contentDescription = "Previous Month"
            )
        }

        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Next Month"
            )
        }
    }
}


@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    onDateClick: (LocalDate) -> Unit
) {
    // These will be the day headers (Sun, Mon, Tue, etc.)
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Calendar days
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
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
                    onDateClick = onDateClick
                )
            }
        }
    }
}

@Composable
fun CalendarDay(
    date: LocalDate?,
    onDateClick: (LocalDate) -> Unit
) {
    // Identify today's date
    val today = LocalDate.now()
    val isToday = date == today

    Box(
        modifier = Modifier
            .aspectRatio(1f) // Make each day cell a square
            .padding(2.dp)
            .background(
                when {
                    date == null -> Color.Transparent
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> Color.LightGray.copy(alpha = 0.3f)
                }
            )
            // Only make actual dates clickable
            .then(
                if (date != null) {
                    Modifier.clickable { onDateClick(date) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        date?.let {
            Text(
                text = it.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Unspecified
            )
        }
    }
}