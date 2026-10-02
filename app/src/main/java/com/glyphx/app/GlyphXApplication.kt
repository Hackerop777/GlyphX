package com.glyphx.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.glyphx.app.core.data.GlyphXDatabase
import com.glyphx.app.core.security.GlyphSecurityManager
import com.glyphx.app.feature.notes.NotesViewModel
import com.glyphx.app.feature.todo.TodoViewModel

class GlyphXApplication : Application(), DefaultLifecycleObserver {

    lateinit var database: GlyphXDatabase
        private set

    lateinit var securityManager: GlyphSecurityManager
        private set

    lateinit var notesViewModel: NotesViewModel
        private set

    lateinit var todoViewModel: TodoViewModel
        private set

    override fun onCreate() {
        super<Application>.onCreate()

        database = GlyphXDatabase.getDatabase(this)
        securityManager = GlyphSecurityManager(this)
        notesViewModel = NotesViewModel(database.noteDao())
        todoViewModel = TodoViewModel(database.taskDao())

        // Register app background/foreground lifecycle listener for auto-relocking
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        securityManager.recordAppBackgrounded()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        securityManager.evaluateForegroundSecurity()
    }
}
