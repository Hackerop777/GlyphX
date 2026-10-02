package com.glyphx.app.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyphx.app.core.data.dao.NoteDao
import com.glyphx.app.core.data.entity.NoteEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteDao: NoteDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedNote = MutableStateFlow<NoteEntity?>(null)
    val selectedNote: StateFlow<NoteEntity?> = _selectedNote.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        if (query.isNotBlank()) {
            noteDao.searchNotes(query)
        } else if (category != "All") {
            noteDao.getNotesByCategory(category)
        } else {
            noteDao.getAllNotes()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String) {
        _selectedCategory.value = category
    }

    fun selectNote(note: NoteEntity?) {
        _selectedNote.value = note
    }

    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        category: String,
        tags: String,
        isPinned: Boolean = false,
        colorHex: String = "#191C2B"
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = NoteEntity(
                id = id,
                title = title.ifBlank { "Untitled Note" },
                content = content,
                category = category,
                tags = tags,
                isPinned = isPinned,
                colorHex = colorHex,
                createdAt = if (id == 0L) now else _selectedNote.value?.createdAt ?: now,
                updatedAt = now
            )
            val newId = noteDao.insertNote(entity)
            _selectedNote.value = entity.copy(id = if (id == 0L) newId else id)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
            }
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }
}
