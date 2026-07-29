package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.RepeatMode
import com.example.data.Task
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(
    viewModel: TaskViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDarkTheme by remember { mutableStateOf(true) }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val tasks by viewModel.uiState.collectAsStateWithLifecycle()
            val filterDueDate by viewModel.filterDueDate.collectAsStateWithLifecycle()
            val filterCompletion by viewModel.filterCompletion.collectAsStateWithLifecycle()
            val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

            var isAddSheetOpen by remember { mutableStateOf(false) }
            var editingTask by remember { mutableStateOf<Task?>(null) }
            var pendingDeletionTaskId by remember { mutableStateOf<Int?>(null) }

            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                floatingActionButtonPosition = FabPosition.Start,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "لیست کارها",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { isDarkTheme = !isDarkTheme },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.NightlightRound else Icons.Default.NightsStay,
                                    contentDescription = if (isDarkTheme) "تغییر به تم سفید" else "تغییر به تم تاریک",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { isAddSheetOpen = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .testTag("add_task_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن کار جدید",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                // Horizontal filter and sort dashboard
                FilterAndSortHeader(
                    currentDueDateFilter = filterDueDate,
                    onDueDateFilterChange = { viewModel.setFilterDueDate(it) },
                    currentSortOption = sortBy,
                    onSortOptionChange = { viewModel.setSortBy(it) }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    if (tasks.isEmpty()) {
                        // Elegant Empty State based on active filters
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "هیچ کاری یافت نشد",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "کاری با فیلترها یا معیارهای انتخاب‌شده ثبت نشده است.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                TaskRow(
                                    task = task,
                                    isPendingDeletion = pendingDeletionTaskId == task.id,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) {
                                            pendingDeletionTaskId = task.id
                                        } else {
                                            if (pendingDeletionTaskId == task.id) {
                                                pendingDeletionTaskId = null
                                            }
                                        }
                                    },
                                    onClick = {
                                        editingTask = task
                                    }
                                )
                            }
                        }
                    }
                }

                // Modal Bottom Sheet for Adding Tasks
                if (isAddSheetOpen) {
                    TaskFormBottomSheet(
                        onDismiss = { isAddSheetOpen = false },
                        onTaskSaved = { description, targetDate, isCompleted, hasAlarm, repeatMode ->
                            viewModel.addTask(description, targetDate, isCompleted, hasAlarm, repeatMode)
                            isAddSheetOpen = false
                        }
                    )
                }

                // Modal Bottom Sheet for Editing Tasks
                if (editingTask != null) {
                    TaskFormBottomSheet(
                        task = editingTask,
                        onDismiss = { editingTask = null },
                        onTaskSaved = { description, targetDate, isCompleted, hasAlarm, repeatMode ->
                            viewModel.updateTask(editingTask!!.id, description, targetDate, isCompleted, hasAlarm, repeatMode)
                            editingTask = null
                        }
                    )
                }

                // Custom RTL Confirmation Dialog
                if (pendingDeletionTaskId != null) {
                    val taskId = pendingDeletionTaskId!!
                    AlertDialog(
                        onDismissRequest = { pendingDeletionTaskId = null },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = {
                            Text(
                                text = "تایید عملیات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        },
                        text = {
                            Text(
                                text = "مطمئن هستید کار مورد نظر انجام شده؟",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val taskToDelete = tasks.find { it.id == taskId }
                                    if (taskToDelete != null) {
                                        if (taskToDelete.hasAlarm) {
                                            dismissSystemAlarm(context, taskToDelete)
                                        }
                                        viewModel.deleteTask(taskId)
                                    }
                                    pendingDeletionTaskId = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("بله، انجام شده", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { pendingDeletionTaskId = null },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("انصراف")
                            }
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun FilterAndSortHeader(
    currentDueDateFilter: DueDateFilter,
    onDueDateFilterChange: (DueDateFilter) -> Unit,
    currentSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Filter Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(DueDateFilter.values()) { filter ->
                        val label = when (filter) {
                            DueDateFilter.ALL -> "همه زمان‌ها"
                            DueDateFilter.TODAY -> "امروز"
                            DueDateFilter.UPCOMING -> "آینده"
                            DueDateFilter.OVERDUE -> "گذشته"
                        }
                        val isSelected = filter == currentDueDateFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { onDueDateFilterChange(filter) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isExpanded) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "تنظیمات فیلتر",
                        tint = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Due Date Filter Row
                    Column {
                        Text(
                            text = "بازه زمانی:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DueDateFilter.values().forEach { filter ->
                                val label = when (filter) {
                                    DueDateFilter.ALL -> "همه"
                                    DueDateFilter.TODAY -> "امروز"
                                    DueDateFilter.UPCOMING -> "آینده"
                                    DueDateFilter.OVERDUE -> "عقب‌افتاده"
                                }
                                val isSelected = filter == currentDueDateFilter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onDueDateFilterChange(filter) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Sort Options Row
                    Column {
                        Text(
                            text = "مرتب‌سازی بر اساس:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SortOption.values().forEach { option ->
                                val label = when (option) {
                                    SortOption.DUE_DATE -> "تاریخ انجام"
                                    SortOption.DESCRIPTION -> "عنوان کار"
                                    SortOption.CREATION -> "زمان ساخت"
                                }
                                val isSelected = option == currentSortOption
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSortOptionChange(option) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    isPendingDeletion: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val formattedDate = remember(task.targetDate) {
        JalaliCalendarHelper.getJalaliDateTime(task.targetDate)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isPendingDeletion || task.isCompleted,
                onCheckedChange = { isChecked ->
                    onCheckedChange(isChecked)
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .testTag("task_checkbox_${task.id}")
                    .minimumInteractiveComponentSize()
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.description,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.hasAlarm) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "آلارم فعال",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (task.repeatMode != RepeatMode.NONE.name) {
                            val repeatText = when (task.repeatMode) {
                                RepeatMode.WEEKLY.name -> "هفتگی"
                                RepeatMode.MONTHLY.name -> "ماهانه"
                                RepeatMode.YEARLY.name -> "سالانه"
                                else -> ""
                            }
                            if (repeatText.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($repeatText)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "ویرایش",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormBottomSheet(
    task: Task? = null,
    onDismiss: () -> Unit,
    onTaskSaved: (description: String, targetDate: Long, isCompleted: Boolean, hasAlarm: Boolean, repeatMode: String) -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedDate by remember { mutableStateOf(task?.targetDate ?: System.currentTimeMillis()) }
    var isCompleted by remember { mutableStateOf(task?.isCompleted ?: false) }
    var setAlarm by remember { mutableStateOf(task?.hasAlarm ?: false) }
    var repeatMode by remember { mutableStateOf(task?.repeatMode ?: RepeatMode.NONE.name) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    val formattedDate = remember(selectedDate) {
        JalaliCalendarHelper.getJalaliDateTime(selectedDate)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = if (task == null) "افزودن کار جدید" else "ویرایش کار",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات کار") },
                    placeholder = { Text("مثال: خرید نان") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Jalali Date & Time Picker Trigger Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تاریخ و زمان انجام کار (شمسی)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { isDatePickerOpen = true }
                            .padding(16.dp)
                            .testTag("date_picker_trigger"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "انتخاب تاریخ و زمان",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formattedDate,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (repeatMode != RepeatMode.NONE.name) {
                                val repeatLabel = when (repeatMode) {
                                    RepeatMode.WEEKLY.name -> "تکرار هفتگی"
                                    RepeatMode.MONTHLY.name -> "تکرار ماهانه"
                                    RepeatMode.YEARLY.name -> "تکرار سالانه"
                                    else -> ""
                                }
                                if (repeatLabel.isNotEmpty()) {
                                    Text(
                                        text = repeatLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Alarm Switch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنظیم هشدار در برنامه ساعت گوشی",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "در زمان مشخص‌شده، آلارم گوشی فعال می‌شود",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = setAlarm,
                        onCheckedChange = { setAlarm = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            if (setAlarm) {
                                setSystemAlarm(context, description, selectedDate, repeatMode)
                            }
                            onTaskSaved(description, selectedDate, isCompleted, setAlarm, repeatMode)
                        }
                    },
                    enabled = description.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_task_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Text(
                        text = if (task == null) "افزودن به لیست" else "ذخیره تغییرات",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Custom Shamsi / Jalali Date & Time Picker Dialog
    if (isDatePickerOpen) {
        JalaliDatePickerDialog(
            initialMillis = selectedDate,
            initialRepeatMode = repeatMode,
            onDismiss = { isDatePickerOpen = false },
            onDateSelected = { newMillis, newRepeatMode ->
                selectedDate = newMillis
                repeatMode = newRepeatMode
            }
        )
    }
}

@Composable
fun JalaliDatePickerDialog(
    initialMillis: Long,
    initialRepeatMode: String = RepeatMode.NONE.name,
    onDismiss: () -> Unit,
    onDateSelected: (Long, String) -> Unit
) {
    val initialDateTime = remember(initialMillis) {
        JalaliCalendarHelper.millisToJalali(initialMillis)
    }

    var year by remember { mutableIntStateOf(initialDateTime.year) }
    var month by remember { mutableIntStateOf(initialDateTime.month) }
    var day by remember { mutableIntStateOf(initialDateTime.day) }
    var hour by remember { mutableIntStateOf(initialDateTime.hour) }
    var minute by remember { mutableIntStateOf(initialDateTime.minute) }
    var repeatMode by remember { mutableStateOf(initialRepeatMode) }

    val daysInMonth = remember(year, month) {
        JalaliCalendarHelper.getDaysInJalaliMonth(year, month)
    }

    val firstDayOffset = remember(year, month) {
        JalaliCalendarHelper.getFirstDayOfWeekForJalaliMonth(year, month)
    }

    LaunchedEffect(daysInMonth) {
        if (day > daysInMonth) {
            day = daysInMonth
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        val millis = JalaliCalendarHelper.jalaliToMillis(year, month, day, hour, minute)
                        onDateSelected(millis, repeatMode)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("date_picker_confirm")
                ) {
                    Text("تایید")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("date_picker_cancel")
                ) {
                    Text("لغو")
                }
            },
            title = {
                Text(
                    text = "انتخاب تاریخ و زمان (شمسی)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Month & Year Navigation Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (month == 1) {
                                    month = 12
                                    year -= 1
                                } else {
                                    month -= 1
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "ماه قبل"
                            )
                        }

                        Text(
                            text = "${JalaliCalendarHelper.JALALI_MONTH_NAMES[month - 1]} $year",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        IconButton(
                            onClick = {
                                if (month == 12) {
                                    month = 1
                                    year += 1
                                } else {
                                    month += 1
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "ماه بعد"
                            )
                        }
                    }

                    // Days of Week Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        JalaliCalendarHelper.JALALI_WEEK_DAYS.forEach { weekDay ->
                            Text(
                                text = weekDay,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                    }

                    // Days Grid
                    val totalGridCells = firstDayOffset + daysInMonth
                    val totalRows = (totalGridCells + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (rowIndex in 0 until totalRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (colIndex in 0..6) {
                                    val cellIndex = rowIndex * 7 + colIndex
                                    val dayNum = cellIndex - firstDayOffset + 1

                                    if (cellIndex in firstDayOffset until (firstDayOffset + daysInMonth)) {
                                        val isSelected = dayNum == day
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent
                                                )
                                                .clickable { day = dayNum },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Repeat Options Selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "تنظیم تکرار آلارم:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(RepeatMode.values()) { mode ->
                                val isSelected = mode.name == repeatMode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { repeatMode = mode.name },
                                    label = { Text(mode.title, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Time Selector (Hour & Minute)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ساعت و دقیقه یادآوری:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Hour Selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { hour = if (hour == 0) 23 else hour - 1 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "کاهش ساعت",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = String.format("%02d", hour),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { hour = (hour + 1) % 24 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "افزایش ساعت",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Text(":", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            // Minute Selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { minute = if (minute == 0) 59 else minute - 1 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "کاهش دقیقه",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = String.format("%02d", minute),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { minute = (minute + 1) % 60 },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "افزایش دقیقه",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

private fun setSystemAlarm(context: Context, title: String, timestamp: Long, repeatMode: String = RepeatMode.NONE.name) {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)

    val alarmMessage = when (repeatMode) {
        RepeatMode.WEEKLY.name -> "$title (تکرار هفتگی)"
        RepeatMode.MONTHLY.name -> "$title (تکرار ماهانه)"
        RepeatMode.YEARLY.name -> "$title (تکرار سالانه)"
        else -> title
    }

    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        putExtra(AlarmClock.EXTRA_MESSAGE, alarmMessage)
        putExtra(AlarmClock.EXTRA_HOUR, hour)
        putExtra(AlarmClock.EXTRA_MINUTES, minute)
        putExtra(AlarmClock.EXTRA_SKIP_UI, true)

        if (repeatMode == RepeatMode.WEEKLY.name) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val days = arrayListOf(dayOfWeek)
            putExtra(AlarmClock.EXTRA_DAYS, days)
        }

        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
        val toastText = when (repeatMode) {
            RepeatMode.WEEKLY.name -> "هشدار با تکرار هفتگی در گوشی تنظیم شد"
            RepeatMode.MONTHLY.name -> "هشدار با تکرار ماهانه در گوشی تنظیم شد"
            RepeatMode.YEARLY.name -> "هشدار با تکرار سالانه در گوشی تنظیم شد"
            else -> "هشدار در ساعت گوشی تنظیم شد"
        }
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "امکان تنظیم آلارم در این دستگاه وجود ندارد", Toast.LENGTH_SHORT).show()
    }
}

private fun dismissSystemAlarm(context: Context, task: Task) {
    if (!task.hasAlarm) return

    val cal = Calendar.getInstance().apply { timeInMillis = task.targetDate }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)

    val alarmMessage = when (task.repeatMode) {
        RepeatMode.WEEKLY.name -> "${task.description} (تکرار هفتگی)"
        RepeatMode.MONTHLY.name -> "${task.description} (تکرار ماهانه)"
        RepeatMode.YEARLY.name -> "${task.description} (تکرار سالانه)"
        else -> task.description
    }

    try {
        val intentByLabel = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
            putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL)
            putExtra(AlarmClock.EXTRA_MESSAGE, alarmMessage)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intentByLabel)
    } catch (e: Exception) {
        try {
            val intentByTime = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_TIME)
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intentByTime)
        } catch (e2: Exception) {
            // Dismiss alarm not supported on this device
        }
    }
}

