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
import com.example.reflectai.ui.theme.Black
import com.example.reflectai.ui.theme.DarkGray
import com.example.reflectai.ui.theme.LightGray
import com.example.reflectai.ui.theme.MediumGray
import com.example.reflectai.ui.theme.White
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
                title = { 
                    Text(
                        "ReflectAI", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light
                        )
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewEntry.route) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Add TestAPI button with minimalist style
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
                onDateClick = { date ->
                    navController.navigate(Screen.NewEntry.route)
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
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
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
            Text(
                text = it.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday) FontWeight.Medium else FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}