package com.glyphx.app.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glyphx.app.core.data.entity.NoteEntity
import com.glyphx.app.core.security.GlyphSecurityManager
import com.glyphx.app.core.ui.theme.NeonCyan
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.NeonPurpleBright
import com.glyphx.app.core.ui.theme.ObsidianBorder
import com.glyphx.app.core.ui.theme.ObsidianCard
import com.glyphx.app.core.ui.theme.TextMuted
import com.glyphx.app.core.ui.theme.TextPrimary
import com.glyphx.app.core.ui.theme.TextSecondary
import com.glyphx.app.core.ui.theme.VoidBlack
import com.glyphx.app.feature.notes.NoteEditorScreen
import com.glyphx.app.feature.notes.NotesScreen
import com.glyphx.app.feature.notes.NotesViewModel
import com.glyphx.app.feature.settings.SettingsScreen
import com.glyphx.app.feature.todo.TodoScreen
import com.glyphx.app.feature.todo.TodoViewModel

enum class MainTab(val label: String, val icon: ImageVector) {
    NOTES("Notes", Icons.Default.Description),
    TODO("To-Do", Icons.Default.Checklist),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun ResponsiveAppShell(
    notesViewModel: NotesViewModel,
    todoViewModel: TodoViewModel,
    securityManager: GlyphSecurityManager,
    onNavigateToSetupGlyph: () -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.NOTES) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        val isExpandedLayout = maxWidth >= 680.dp

        if (isExpandedLayout) {
            // Foldable / Tablet Split-Pane (mockup panel 9)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Navigation & List Pane
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder)
                ) {
                    // Left Header with App Emblem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(listOf(NeonPurple, NeonCyan)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "GlyphX",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }

                    // Navigation Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MainTab.values().forEach { tab ->
                            val isSelected = tab == currentTab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonPurple else VoidBlack)
                                    .clickable {
                                        currentTab = tab
                                        editingNote = null
                                        isCreatingNewNote = false
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Left Pane Content
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            MainTab.NOTES -> {
                                NotesScreen(
                                    viewModel = notesViewModel,
                                    onNoteClick = { note ->
                                        editingNote = note
                                        isCreatingNewNote = false
                                    },
                                    onCreateNoteClick = {
                                        editingNote = null
                                        isCreatingNewNote = true
                                    }
                                )
                            }
                            MainTab.TODO -> {
                                TodoScreen(viewModel = todoViewModel)
                            }
                            MainTab.SETTINGS -> {
                                SettingsScreen(
                                    securityManager = securityManager,
                                    onNavigateToSetupGlyph = onNavigateToSetupGlyph
                                )
                            }
                        }
                    }
                }

                // Right Detail Pane
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(VoidBlack)
                ) {
                    if (isCreatingNewNote || editingNote != null) {
                        NoteEditorScreen(
                            note = editingNote,
                            onSaveNote = { title, content, category, tags ->
                                notesViewModel.saveNote(
                                    id = editingNote?.id ?: 0L,
                                    title = title,
                                    content = content,
                                    category = category,
                                    tags = tags
                                )
                                editingNote = null
                                isCreatingNewNote = false
                            },
                            onNavigateBack = {
                                editingNote = null
                                isCreatingNewNote = false
                            }
                        )
                    } else {
                        // Empty state placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(Color(0xFF161824), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Select an item to view or edit",
                                    color = TextSecondary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Your private thoughts and tasks are securely encrypted.",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Phone / Compact Layout
            if (isCreatingNewNote || editingNote != null) {
                NoteEditorScreen(
                    note = editingNote,
                    onSaveNote = { title, content, category, tags ->
                        notesViewModel.saveNote(
                            id = editingNote?.id ?: 0L,
                            title = title,
                            content = content,
                            category = category,
                            tags = tags
                        )
                        editingNote = null
                        isCreatingNewNote = false
                    },
                    onNavigateBack = {
                        editingNote = null
                        isCreatingNewNote = false
                    }
                )
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = ObsidianCard,
                            tonalElevation = 0.dp
                        ) {
                            MainTab.values().forEach { tab ->
                                val isSelected = tab == currentTab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NeonPurpleBright,
                                        selectedTextColor = NeonPurpleBright,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color(0xFF231D38)
                                    )
                                )
                            }
                        }
                    },
                    containerColor = VoidBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            MainTab.NOTES -> {
                                NotesScreen(
                                    viewModel = notesViewModel,
                                    onNoteClick = { note -> editingNote = note },
                                    onCreateNoteClick = { isCreatingNewNote = true }
                                )
                            }
                            MainTab.TODO -> {
                                TodoScreen(viewModel = todoViewModel)
                            }
                            MainTab.SETTINGS -> {
                                SettingsScreen(
                                    securityManager = securityManager,
                                    onNavigateToSetupGlyph = onNavigateToSetupGlyph
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
