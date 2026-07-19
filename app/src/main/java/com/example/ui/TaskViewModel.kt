package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class DueDateFilter {
    ALL, TODAY, UPCOMING, OVERDUE
}

enum class CompletionFilter {
    ALL, INCOMPLETE, COMPLETED
}

enum class SortOption {
    DUE_DATE, DESCRIPTION, CREATION
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    
    private val _filterDueDate = MutableStateFlow(DueDateFilter.ALL)
    val filterDueDate: StateFlow<DueDateFilter> = _filterDueDate.asStateFlow()

    private val _filterCompletion = MutableStateFlow(CompletionFilter.ALL)
    val filterCompletion: StateFlow<CompletionFilter> = _filterCompletion.asStateFlow()

    private val _sortBy = MutableStateFlow(SortOption.DUE_DATE)
    val sortBy: StateFlow<SortOption> = _sortBy.asStateFlow()

    val uiState: StateFlow<List<Task>>

    init {
        val taskDao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(taskDao)

        uiState = combine(
            repository.allTasks,
            _filterDueDate,
            _filterCompletion,
            _sortBy
        ) { tasks, dueDateFilter, completionFilter, sortOption ->
            var filtered = tasks

            // 1. Filter by Completion Status
            filtered = when (completionFilter) {
                CompletionFilter.ALL -> filtered
                CompletionFilter.INCOMPLETE -> filtered.filter { !it.isCompleted }
                CompletionFilter.COMPLETED -> filtered.filter { it.isCompleted }
            }

            // 2. Filter by Due Date
            filtered = when (dueDateFilter) {
                DueDateFilter.ALL -> filtered
                DueDateFilter.TODAY -> filtered.filter { isToday(it.targetDate) }
                DueDateFilter.UPCOMING -> filtered.filter { isUpcoming(it.targetDate) }
                DueDateFilter.OVERDUE -> filtered.filter { isOverdue(it.targetDate) }
            }

            // 3. Sort tasks
            when (sortOption) {
                SortOption.DUE_DATE -> filtered.sortedBy { it.targetDate }
                SortOption.DESCRIPTION -> filtered.sortedBy { it.description.lowercase() }
                SortOption.CREATION -> filtered.sortedBy { it.id } // ID is auto-increment, reflects creation order
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun setFilterDueDate(filter: DueDateFilter) {
        _filterDueDate.value = filter
    }

    fun setFilterCompletion(filter: CompletionFilter) {
        _filterCompletion.value = filter
    }

    fun setSortBy(option: SortOption) {
        _sortBy.value = option
    }

    fun addTask(description: String, targetDate: Long, isCompleted: Boolean = false) {
        viewModelScope.launch {
            repository.insert(Task(description = description, targetDate = targetDate, isCompleted = isCompleted))
        }
    }

    fun updateTask(id: Int, description: String, targetDate: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.insert(Task(id = id, description = description, targetDate = targetDate, isCompleted = isCompleted))
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun isToday(timestamp: Long): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isUpcoming(timestamp: Long): Boolean {
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return timestamp > calToday.timeInMillis
    }

    private fun isOverdue(timestamp: Long): Boolean {
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return timestamp < calToday.timeInMillis && !isToday(timestamp)
    }
}
