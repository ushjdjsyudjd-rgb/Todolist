package com.example.ui

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Task
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskApp(
    viewModel: TaskViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
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
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Horizontal filter and sort dashboard
                FilterAndSortHeader(
                    currentDueDateFilter = filterDueDate,
                    onDueDateFilterChange = { viewModel.setFilterDueDate(it) },
                    currentCompletionFilter = filterCompletion,
                    onCompletionFilterChange = { viewModel.setFilterCompletion(it) },
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
            }
        }
    }

    // Modal Bottom Sheet for Adding Tasks
    if (isAddSheetOpen) {
        TaskFormBottomSheet(
            onDismiss = { isAddSheetOpen = false },
            onTaskSaved = { description, targetDate, isCompleted ->
                viewModel.addTask(description, targetDate, isCompleted)
                isAddSheetOpen = false
            }
        )
    }

    // Modal Bottom Sheet for Editing Tasks
    if (editingTask != null) {
        TaskFormBottomSheet(
            task = editingTask,
            onDismiss = { editingTask = null },
            onTaskSaved = { description, targetDate, isCompleted ->
                viewModel.updateTask(editingTask!!.id, description, targetDate, isCompleted)
                editingTask = null
            }
        )
    }

    // Custom RTL Confirmation Dialog
    if (pendingDeletionTaskId != null) {
        val taskId = pendingDeletionTaskId!!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                            viewModel.deleteTask(taskId)
                            pendingDeletionTaskId = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("dialog_confirm_button")
                    ) {
                        Text(text = "بله", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingDeletionTaskId = null },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("dialog_dismiss_button")
                    ) {
                        Text(text = "خیر", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun FilterAndSortHeader(
    currentDueDateFilter: DueDateFilter,
    onDueDateFilterChange: (DueDateFilter) -> Unit,
    currentCompletionFilter: CompletionFilter,
    onCompletionFilterChange: (CompletionFilter) -> Unit,
    currentSortOption: SortOption,
    onSortOptionChange: (SortOption) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "فیلترها",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فیلترها و مرتب‌سازی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Short badge summarizing current state
                Text(
                    text = when (currentDueDateFilter) {
                        DueDateFilter.ALL -> "همه"
                        DueDateFilter.TODAY -> "امروز"
                        DueDateFilter.UPCOMING -> "آینده"
                        DueDateFilter.OVERDUE -> "معوقه"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "بستن" else "باز کردن",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                // 1. Completion Status Filter Row
                Column {
                    Text(
                        text = "وضعیت انجام:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompletionFilter.values().forEach { filter ->
                            val label = when (filter) {
                                CompletionFilter.ALL -> "همه"
                                CompletionFilter.INCOMPLETE -> "انجام نشده"
                                CompletionFilter.COMPLETED -> "انجام شده"
                            }
                            val isSelected = filter == currentCompletionFilter
                            FilterChip(
                                selected = isSelected,
                                onClick = { onCompletionFilterChange(filter) },
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

                // 2. Due Date Filter Row
                Column {
                    Text(
                        text = "بازه زمانی:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DueDateFilter.values()) { filter ->
                            val label = when (filter) {
                                DueDateFilter.ALL -> "همه تاریخ‌ها"
                                DueDateFilter.TODAY -> "امروز"
                                DueDateFilter.UPCOMING -> "آینده"
                                DueDateFilter.OVERDUE -> "معوقه"
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

                // 3. Sort Options Row
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
                                SortOption.DESCRIPTION -> "توضیحات"
                                SortOption.CREATION -> "ترتیب ثبت"
                            }
                            val isSelected = option == currentSortOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSortOptionChange(option) },
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
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        sdf.format(Date(task.targetDate))
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
    onTaskSaved: (description: String, targetDate: Long, isCompleted: Boolean) -> Unit
) {
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedDate by remember { mutableStateOf(task?.targetDate ?: System.currentTimeMillis()) }
    var isCompleted by remember { mutableStateOf(task?.isCompleted ?: false) }
    var isDatePickerOpen by remember { mutableStateOf(false) }

    val formattedDate = remember(selectedDate) {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        sdf.format(Date(selectedDate))
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

                // Date Picker Trigger Row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تاریخ انجام کار",
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
                            contentDescription = "انتخاب تاریخ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = formattedDate,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (task != null) {
                    // Task Completion Switch for Editing Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "وضعیت کار (انجام شده)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isCompleted,
                            onCheckedChange = { isCompleted = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (description.isNotBlank()) {
                            onTaskSaved(description, selectedDate, isCompleted)
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

    // Material 3 System Date Picker Dialog
    if (isDatePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        isDatePickerOpen = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm")
                ) {
                    Text("تایید")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isDatePickerOpen = false },
                    modifier = Modifier.testTag("date_picker_cancel")
                ) {
                    Text("لغو")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

