package com.glyphx.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.glyphx.app.core.data.dao.NoteDao
import com.glyphx.app.core.data.dao.TaskDao
import com.glyphx.app.core.data.entity.NoteEntity
import com.glyphx.app.core.data.entity.TaskEntity
import com.glyphx.app.core.data.entity.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [NoteEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GlyphXDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: GlyphXDatabase? = null

        fun getDatabase(context: Context): GlyphXDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GlyphXDatabase::class.java,
                    "glyphx_database.db"
                )
                    .addCallback(DatabasePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabasePrepopulateCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        populateInitialData(database.noteDao(), database.taskDao())
                    }
                }
            }

            private suspend fun populateInitialData(noteDao: NoteDao, taskDao: TaskDao) {
                val now = System.currentTimeMillis()

                // Starter Notes matching mockup panel 5
                noteDao.insertNote(
                    NoteEntity(
                        title = "Trip Plan - Bali",
                        content = "Places to visit:\n• Uluwatu Temple (Sunset kecak dance)\n• Nusa Penida (Kelingking Beach hike)\n• Seminyak Beach (Dinner at sunset)\n\nPacking Checklist:\n☑ Passport & Visa\n☑ Universal Adapter\n☑ Sunscreen & Shades",
                        category = "Travel",
                        tags = "Travel,Personal",
                        isPinned = true,
                        colorHex = "#1E293B",
                        createdAt = now - 3600_000 * 2,
                        updatedAt = now - 3600_000 * 2
                    )
                )

                noteDao.insertNote(
                    NoteEntity(
                        title = "Project Ideas",
                        content = "AI App Concepts & Features:\n1. GlyphX in-app gesture canvas lock\n2. Real-time resampled gesture normalization\n3. Zero-telemetry on-device encrypted storage\n4. Dual-pane foldable experience for tablets & foldables",
                        category = "Work",
                        tags = "Ideas,Work",
                        isPinned = true,
                        colorHex = "#2E1065",
                        createdAt = now - 3600_000 * 8,
                        updatedAt = now - 3600_000 * 8
                    )
                )

                noteDao.insertNote(
                    NoteEntity(
                        title = "Meeting Notes",
                        content = "Key Discussion Points:\n- UI review: high-contrast dark futuristic aesthetic.\n- Security validation: AES-256-GCM Keystore keys.\n- Next release timeline: Sprint demo tomorrow.",
                        category = "Work",
                        tags = "Work,Team",
                        isPinned = false,
                        colorHex = "#1E2235",
                        createdAt = now - 86400_000L,
                        updatedAt = now - 86400_000L
                    )
                )

                noteDao.insertNote(
                    NoteEntity(
                        title = "Daily Journal",
                        content = "Grateful for:\n1. A calm morning coffee and clear skies.\n2. Deep focus on high-fidelity gesture recognition.\n3. Building something delightful and secure.",
                        category = "Personal",
                        tags = "Personal,Journal",
                        isPinned = false,
                        colorHex = "#064E3B",
                        createdAt = now - 86400_000L * 2,
                        updatedAt = now - 86400_000L * 2
                    )
                )

                // Starter Tasks matching mockup panel 7
                taskDao.insertTask(
                    TaskEntity(
                        title = "Finish project proposal",
                        description = "Review budget estimates and final slide deck",
                        isCompleted = true,
                        priority = TaskPriority.HIGH,
                        category = "Work",
                        createdAt = now - 3600_000 * 5
                    )
                )

                taskDao.insertTask(
                    TaskEntity(
                        title = "Gym workout",
                        description = "Upper body strength training & 20 min cardio",
                        isCompleted = false,
                        priority = TaskPriority.HIGH,
                        category = "Personal",
                        createdAt = now - 3600_000 * 3
                    )
                )

                taskDao.insertTask(
                    TaskEntity(
                        title = "Plan weekend trip",
                        description = "Book eco-resort cabins & check route",
                        isCompleted = false,
                        priority = TaskPriority.HIGH,
                        category = "Personal",
                        createdAt = now - 3600_000 * 2
                    )
                )

                taskDao.insertTask(
                    TaskEntity(
                        title = "Buy gift for mom",
                        description = "Order custom hand-carved necklace",
                        isCompleted = false,
                        priority = TaskPriority.HIGH,
                        category = "Personal",
                        createdAt = now - 3600_000
                    )
                )

                taskDao.insertTask(
                    TaskEntity(
                        title = "Read a book",
                        description = "Atomic Habits chapter 4",
                        isCompleted = false,
                        priority = TaskPriority.MEDIUM,
                        category = "Personal",
                        createdAt = now
                    )
                )

                taskDao.insertTask(
                    TaskEntity(
                        title = "Clean workspace",
                        description = "Cable management and wipe down desk",
                        isCompleted = false,
                        priority = TaskPriority.LOW,
                        category = "Home",
                        createdAt = now
                    )
                )

                taskDao.insertTask(
                    TaskEntity(
                        title = "Review app design",
                        description = "Audit responsive tablet layout and haptic curves",
                        isCompleted = false,
                        priority = TaskPriority.HIGH,
                        category = "Work",
                        createdAt = now
                    )
                )
            }
        }
    }
}
