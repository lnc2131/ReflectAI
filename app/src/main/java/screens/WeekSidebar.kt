package screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*
import model.JournalEntry

/**
 * A sidebar showing weeks for the current month and selected date
 */
@Composable
fun WeekSidebar(
    currentDate: LocalDate,
    currentMonth: YearMonth,
    datesWithEntries: Map<LocalDate, Boolean>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get all weeks for the current month
    val weeks = remember(currentMonth) {
        getWeeksForMonth(currentMonth)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(120.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(end = 12.dp)
    ) {
        // Month header
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )
        
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 0.5.dp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Weeks list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(weeks) { weekDates ->
                WeekRow(
                    dates = weekDates,
                    currentDate = currentDate,
                    datesWithEntries = datesWithEntries,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

/**
 * A row representing a week in the sidebar
 */
@Composable
fun WeekRow(
    dates: List<LocalDate?>,
    currentDate: LocalDate,
    datesWithEntries: Map<LocalDate, Boolean>,
    onDateSelected: (LocalDate) -> Unit
) {
    // Check if this week contains the current date
    val containsCurrentDate = dates.filterNotNull().any { it == currentDate }
    
    // Border if this is the current week
    val borderModifier = if (containsCurrentDate) {
        Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    } else {
        Modifier.padding(4.dp)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Week dates - showing only 3 days (first, middle, last) to save space
        dates.filterIndexed { index, _ -> index == 0 || index == 3 || index == 6 }
            .filterNotNull()
            .forEach { date ->
                val isCurrentDate = date == currentDate
                val hasEntry = datesWithEntries.containsKey(date)
                
                SidebarDay(
                    date = date,
                    isSelected = isCurrentDate,
                    hasEntry = hasEntry,
                    onClick = { onDateSelected(date) }
                )
            }
    }
}

/**
 * Individual day in the sidebar
 */
@Composable
fun SidebarDay(
    date: LocalDate,
    isSelected: Boolean,
    hasEntry: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light
                )
            )
            
            // Small dot indicator for entries
            if (hasEntry) {
                Spacer(modifier = Modifier.height(1.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Helper to generate all weeks for a month
 */
private fun getWeeksForMonth(month: YearMonth): List<List<LocalDate?>> {
    val firstDayOfMonth = month.atDay(1)
    val lastDayOfMonth = month.atEndOfMonth()
    
    val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
    var currentDay = firstDayOfMonth
    
    // Go back to the first day of the week
    while (currentDay.dayOfWeek != firstDayOfWeek) {
        currentDay = currentDay.minusDays(1)
    }
    
    // Generate weeks
    val weeks = mutableListOf<List<LocalDate?>>()
    var daysRemaining = true
    
    while (daysRemaining) {
        val week = mutableListOf<LocalDate?>()
        for (i in 0 until 7) {
            // Only include dates from the specified month
            if (currentDay.month == month.month) {
                week.add(currentDay)
            } else {
                week.add(null)
            }
            currentDay = currentDay.plusDays(1)
        }
        
        weeks.add(week)
        
        // Stop when we've gone past the last day of the month
        daysRemaining = currentDay.isBefore(lastDayOfMonth) || currentDay.isEqual(lastDayOfMonth)
    }
    
    return weeks
}