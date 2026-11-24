package com.chroma.studio.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.*
import java.text.SimpleDateFormat
import java.util.*

// ============================================================================
// STUDIO CUSTOM CALENDAR DATA & UTILITIES
// ============================================================================

enum class CalendarViewMode { DAYS, MONTHS, YEARS }

data class CalendarDayItem(
    val dayOfMonth: Int,
    val month: Int,
    val year: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isSelectable: Boolean,
    val epochMillis: Long
)

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

private val WEEKDAY_HEADERS = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")

/**
 * Calculates a 42-cell calendar grid for a given year and month.
 */
fun buildCalendarDays(
    year: Int,
    month: Int,
    selectedMillis: Long?,
    minDateMillis: Long?,
    maxDateMillis: Long?
): List<CalendarDayItem> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // Day of week for 1st day (1=Sunday, 2=Monday, ..., 7=Saturday)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Convert so Monday is 0, Sunday is 6
    val leadingDays = (firstDayOfWeek + 5) % 7

    val daysInCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Previous month info
    val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    val daysInPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val prevMonth = prevCal.get(Calendar.MONTH)
    val prevYear = prevCal.get(Calendar.YEAR)

    // Next month info
    val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    val nextMonth = nextCal.get(Calendar.MONTH)
    val nextYear = nextCal.get(Calendar.YEAR)

    val todayCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val selectedCal = selectedMillis?.let {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
    }

    val result = ArrayList<CalendarDayItem>(42)

    // Leading days from previous month
    for (i in (daysInPrevMonth - leadingDays + 1)..daysInPrevMonth) {
        val itemCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(prevYear, prevMonth, i, 12, 0, 0)
        }
        val millis = itemCal.timeInMillis
        val isSelectable = (minDateMillis == null || millis >= minDateMillis) &&
                (maxDateMillis == null || millis <= maxDateMillis)
        val isSelected = selectedCal?.let {
            it.get(Calendar.YEAR) == prevYear && it.get(Calendar.MONTH) == prevMonth && it.get(Calendar.DAY_OF_MONTH) == i
        } ?: false

        result.add(
            CalendarDayItem(
                dayOfMonth = i,
                month = prevMonth,
                year = prevYear,
                isCurrentMonth = false,
                isToday = (prevYear == todayYear && prevMonth == todayMonth && i == todayDay),
                isSelected = isSelected,
                isSelectable = isSelectable,
                epochMillis = millis
            )
        )
    }

    // Days in current month
    for (i in 1..daysInCurrentMonth) {
        val itemCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, i, 12, 0, 0)
        }
        val millis = itemCal.timeInMillis
        val isSelectable = (minDateMillis == null || millis >= minDateMillis) &&
                (maxDateMillis == null || millis <= maxDateMillis)
        val isSelected = selectedCal?.let {
            it.get(Calendar.YEAR) == year && it.get(Calendar.MONTH) == month && it.get(Calendar.DAY_OF_MONTH) == i
        } ?: false

        result.add(
            CalendarDayItem(
                dayOfMonth = i,
                month = month,
                year = year,
                isCurrentMonth = true,
                isToday = (year == todayYear && month == todayMonth && i == todayDay),
                isSelected = isSelected,
                isSelectable = isSelectable,
                epochMillis = millis
            )
        )
    }

    // Trailing days into next month to complete 42 cells (6 rows x 7 cols)
    val remaining = 42 - result.size
    for (i in 1..remaining) {
        val itemCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(nextYear, nextMonth, i, 12, 0, 0)
        }
        val millis = itemCal.timeInMillis
        val isSelectable = (minDateMillis == null || millis >= minDateMillis) &&
                (maxDateMillis == null || millis <= maxDateMillis)
        val isSelected = selectedCal?.let {
            it.get(Calendar.YEAR) == nextYear && it.get(Calendar.MONTH) == nextMonth && it.get(Calendar.DAY_OF_MONTH) == i
        } ?: false

        result.add(
            CalendarDayItem(
                dayOfMonth = i,
                month = nextMonth,
                year = nextYear,
                isCurrentMonth = false,
                isToday = (nextYear == todayYear && nextMonth == todayMonth && i == todayDay),
                isSelected = isSelected,
                isSelectable = isSelectable,
                epochMillis = millis
            )
        )
    }

    return result
}

// ============================================================================
// STANDALONE / EMBEDDABLE STUDIO CALENDAR VIEW
// ============================================================================

/**
 * Clean, Smooth, Professional Custom Calendar.
 * Features:
 * - Direct year/month fast switcher grid (1-tap access to any year/decade).
 * - Micro-beveled specular highlights on solid dark surfaces.
 * - Multi-stop gradient indicators on selected dates.
 * - Lightweight, high-performance Compose layout without heavy views.
 */
@Composable
fun StudioCustomCalendarView(
    selectedMillis: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    title: String = "Select Date",
    accentGradient: List<Color> = listOf(Color(0xFF0381FE), Color(0xFF0072DE))
) {
    val haptic = LocalHapticFeedback.current
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark

    // Initialize calendar to selected date or max allowed date or today
    val initialCal = remember(selectedMillis, maxDateMillis) {
        val targetMillis = selectedMillis ?: maxDateMillis ?: System.currentTimeMillis()
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = targetMillis }
    }

    var displayYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var displayMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var viewMode by remember { mutableStateOf(CalendarViewMode.DAYS) }

    // Computed days
    val daysList = remember(displayYear, displayMonth, selectedMillis, minDateMillis, maxDateMillis) {
        buildCalendarDays(
            year = displayYear,
            month = displayMonth,
            selectedMillis = selectedMillis,
            minDateMillis = minDateMillis,
            maxDateMillis = maxDateMillis
        )
    }

    val selectedDateText = remember(selectedMillis) {
        if (selectedMillis != null) {
            val fmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.format(Date(selectedMillis))
        } else {
            "No Date Selected"
        }
    }

    val containerBg = if (isDark) Color(0xFF161922) else Color(0xFFFFFFFF)
    val containerBorder = if (isDark) Color(0x1FFFFFFF) else Color(0x12000000)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(containerBg)
            .border(0.8.dp, containerBorder, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // ── TOP HEADER: TITLE & SELECTED PREVIEW ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    color = if (isDark) Color(0xFF7A869E) else Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = selectedDateText,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick Today/Jump button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0xFF1F2433) else Color(0xFFF1F5F9))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val target = maxDateMillis ?: System.currentTimeMillis()
                        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = target }
                        displayYear = c.get(Calendar.YEAR)
                        displayMonth = c.get(Calendar.MONTH)
                        viewMode = CalendarViewMode.DAYS
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Lucide.CalendarDays,
                        contentDescription = "Current",
                        tint = Color(0xFF0381FE),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Recent",
                        color = if (isDark) Color(0xFFBAC7E8) else Color(0xFF334155),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── MONTH & YEAR CONTROLLER WITH NAVIGATION ARROWS ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF1E2434) else Color(0xFFF1F5F9))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Month Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (displayMonth == 0) {
                        displayMonth = 11
                        displayYear--
                    } else {
                        displayMonth--
                    }
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = if (isDark) Color(0xFFB0BBD8) else Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Month & Year Selector Button (Toggle Mode)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (viewMode != CalendarViewMode.DAYS) (if (isDark) Color(0xFF2B334A) else Color(0xFFE2E8F0)) else Color.Transparent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewMode = if (viewMode == CalendarViewMode.DAYS) CalendarViewMode.YEARS else CalendarViewMode.DAYS
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${MONTH_NAMES[displayMonth]} $displayYear",
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (viewMode == CalendarViewMode.DAYS) Lucide.ChevronDown else Lucide.ChevronUp,
                    contentDescription = null,
                    tint = Color(0xFF0381FE),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Next Month Button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (displayMonth == 11) {
                        displayMonth = 0
                        displayYear++
                    } else {
                        displayMonth++
                    }
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = "Next Month",
                    tint = if (isDark) Color(0xFFB0BBD8) else Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── BODY: DAYS GRID OR YEARS/MONTHS QUICK SWITCHER ──
        AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "calendarViewModeTransition"
        ) { mode ->
            when (mode) {
                CalendarViewMode.DAYS -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Weekday Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            WEEKDAY_HEADERS.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 6 Weeks x 7 Days Grid
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (row in 0 until 6) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0 until 7) {
                                        val index = row * 7 + col
                                        if (index < daysList.size) {
                                            val dayItem = daysList[index]
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CalendarDayCell(
                                                    item = dayItem,
                                                    accentGradient = accentGradient,
                                                    onClick = {
                                                        if (dayItem.isSelectable) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            // If clicked outside current month, auto-navigate
                                                            if (!dayItem.isCurrentMonth) {
                                                                displayYear = dayItem.year
                                                                displayMonth = dayItem.month
                                                            }
                                                            onDateSelected(dayItem.epochMillis)
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CalendarViewMode.YEARS -> {
                    CalendarYearMonthPicker(
                        selectedYear = displayYear,
                        selectedMonth = displayMonth,
                        minDateMillis = minDateMillis,
                        maxDateMillis = maxDateMillis,
                        accentGradient = accentGradient,
                        onYearMonthPicked = { pickedYear, pickedMonth ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            displayYear = pickedYear
                            displayMonth = pickedMonth
                            viewMode = CalendarViewMode.DAYS
                        }
                    )
                }

                CalendarViewMode.MONTHS -> {
                    // Handled within CalendarYearMonthPicker
                }
            }
        }
    }
}

// ============================================================================
// CALENDAR DAY CELL (TACTILE, SMOOTH & THEMED)
// ============================================================================

@Composable
fun CalendarDayCell(
    item: CalendarDayItem,
    accentGradient: List<Color>,
    onClick: () -> Unit
) {
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    val isSelected = item.isSelected
    val isEnabled = item.isSelectable

    val cellBackground = when {
        isSelected -> Brush.linearGradient(accentGradient)
        item.isToday -> SolidColor(if (isDark) Color(0xFF1E2638) else Color(0xFFE0F2FE))
        else -> SolidColor(Color.Transparent)
    }

    val cellBorder = when {
        item.isToday && !isSelected -> SolidColor(Color(0xFF0381FE).copy(alpha = 0.5f))
        else -> null
    }

    val textColor = when {
        isSelected -> Color.White
        !isEnabled -> if (isDark) Color(0xFF333B50) else Color(0xFFCBD5E1)
        !item.isCurrentMonth -> if (isDark) Color(0xFF4B556D) else Color(0xFF94A3B8)
        item.isToday -> Color(0xFF0381FE)
        else -> if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(cellBackground)
            .then(
                if (cellBorder != null) {
                    Modifier.border(
                        width = 1.dp,
                        brush = cellBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.dayOfMonth.toString(),
                color = textColor,
                fontSize = 13.sp,
                fontWeight = if (isSelected || item.isToday) FontWeight.Bold else FontWeight.Medium
            )
            if (item.isToday && !isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0381FE))
                )
            }
        }
    }
}

// ============================================================================
// ULTRA-FAST YEAR & MONTH PICKER FOR QUICK DECADE/YEAR JUMPING
// ============================================================================

@Composable
fun CalendarYearMonthPicker(
    selectedYear: Int,
    selectedMonth: Int,
    minDateMillis: Long?,
    maxDateMillis: Long?,
    accentGradient: List<Color>,
    onYearMonthPicked: (Int, Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark

    val minYear = remember(minDateMillis) {
        if (minDateMillis != null) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = minDateMillis }.get(Calendar.YEAR)
        } else 1920
    }

    val maxYear = remember(maxDateMillis) {
        if (maxDateMillis != null) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = maxDateMillis }.get(Calendar.YEAR)
        } else Calendar.getInstance().get(Calendar.YEAR) + 5
    }

    var tempYear by remember { mutableIntStateOf(selectedYear) }
    var tempMonth by remember { mutableIntStateOf(selectedMonth) }

    val yearsList = remember(minYear, maxYear) {
        (maxYear downTo minYear).toList()
    }

    val gridState = rememberLazyGridState()

    // Scroll automatically to selected year
    LaunchedEffect(Unit) {
        val index = yearsList.indexOf(selectedYear)
        if (index >= 0) {
            gridState.scrollToItem(maxOf(0, index - 2))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(290.dp)
    ) {
        // Month Selector Strip
        Text(
            text = "SELECT MONTH",
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(12) { monthIdx ->
                val isSelected = (monthIdx == tempMonth)
                val monthNameShort = MONTH_NAMES[monthIdx].take(3)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Brush.linearGradient(accentGradient)
                            else SolidColor(if (isDark) Color(0xFF1E2434) else Color(0xFFF1F5F9))
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tempMonth = monthIdx
                            onYearMonthPicked(tempYear, tempMonth)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = monthNameShort,
                        color = if (isSelected) Color.White else if (isDark) Color(0xFFB5C0DC) else Color(0xFF334155),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Year Selector Grid
        Text(
            text = "SELECT YEAR",
            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
        Spacer(Modifier.height(6.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(yearsList) { yr ->
                val isSelected = (yr == tempYear)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Brush.linearGradient(accentGradient)
                            else SolidColor(if (isDark) Color(0xFF1E2434) else Color(0xFFF1F5F9))
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tempYear = yr
                            onYearMonthPicked(tempYear, tempMonth)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = yr.toString(),
                        color = if (isSelected) Color.White else if (isDark) Color(0xFFB5C0DC) else Color(0xFF334155),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================================
// APP-WIDE STUDIO CUSTOM CALENDAR DIALOG MODAL
// ============================================================================

/**
 * Universal Dialog version of the Custom Studio Calendar.
 * Replaces cumbersome Material DatePickerDialogs across Onboarding, Auth,
 * Profile Settings, and Studio screens.
 */
@Composable
fun StudioCustomCalendarDialog(
    initialDateMillis: Long?,
    onDismissRequest: () -> Unit,
    onDateConfirmed: (Long) -> Unit,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    title: String = "Select Date"
) {
    val haptic = LocalHapticFeedback.current
    val isDark = com.chroma.studio.ui.theme.LocalOscellionColors.current.isDark
    var selectedEpoch by remember { mutableStateOf(initialDateMillis ?: maxDateMillis ?: System.currentTimeMillis()) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 390.dp)
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume inner click
                    )
            ) {
                StudioCustomCalendarView(
                    selectedMillis = selectedEpoch,
                    onDateSelected = { selectedEpoch = it },
                    minDateMillis = minDateMillis,
                    maxDateMillis = maxDateMillis,
                    title = title
                )

                Spacer(Modifier.height(10.dp))

                // Bottom Action Buttons with Theme-Aware Styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1F2433) else Color(0xFFF1F5F9))
                            .border(0.8.dp, if (isDark) Color(0x1FFFFFFF) else Color(0x0F000000), RoundedCornerShape(14.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismissRequest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = if (isDark) Color(0xFFBAC7E8) else Color(0xFF475569),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Confirm Date
                    val canConfirm = (minDateMillis == null || selectedEpoch >= minDateMillis) &&
                            (maxDateMillis == null || selectedEpoch <= maxDateMillis)

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (canConfirm) Brush.linearGradient(listOf(Color(0xFF0381FE), Color(0xFF0072DE)))
                                else SolidColor(if (isDark) Color(0xFF1E2232) else Color(0xFFE2E8F0))
                            )
                            .clickable(enabled = canConfirm) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDateConfirmed(selectedEpoch)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                tint = if (canConfirm) Color.White else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Confirm Date",
                                color = if (canConfirm) Color.White else if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
