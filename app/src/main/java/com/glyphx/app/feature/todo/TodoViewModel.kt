package com.glyphx.app.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyphx.app.core.data.dao.TaskDao
import com.glyphx.app.core.data.entity.TaskEntity
import com.glyphx.app.core.data.entity.TaskPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TodoFilter {
    ALL,
    TODAY,
    UPCOMING,
    COMPLETED
}

class TodoViewModel(
    private val taskDao: TaskDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TodoFilter.ALL)
    val selectedFilter: StateFlow<TodoFilter> = _selectedFilter.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = combine(
        taskDao.getAllTasks(),
        _selectedFilter
    ) { allTasks, filter ->
        when (filter) {
            TodoFilter.ALL -> allTasks
            TodoFilter.TODAY -> allTasks.filter { !it.isCompleted }
            TodoFilter.UPCOMING -> allTasks.filter { !it.isCompleted && it.dueDate != null }
            TodoFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setFilter(filter: TodoFilter) {
        _selectedFilter.value = filter
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.updateTaskStatus(task.id, !task.isCompleted)
        }
    }

    fun addTask(
        title: String,
        description: String = "",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: Long? = null,
        category: String = "General"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                isCompleted = false,
                priority = priority,
                dueDate = dueDate,
                category = category
            )
            taskDao.insertTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }
}
