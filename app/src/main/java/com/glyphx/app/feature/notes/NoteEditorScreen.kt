package com.glyphx.app.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glyphx.app.core.data.entity.NoteEntity
import com.glyphx.app.core.ui.theme.NeonCyan
import com.glyphx.app.core.ui.theme.NeonPurple
import com.glyphx.app.core.ui.theme.ObsidianBorder
import com.glyphx.app.core.ui.theme.ObsidianCard
import com.glyphx.app.core.ui.theme.TextMuted
import com.glyphx.app.core.ui.theme.TextPrimary
import com.glyphx.app.core.ui.theme.TextSecondary
import com.glyphx.app.core.ui.theme.VoidBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: NoteEntity?,
    onSaveNote: (title: String, content: String, category: String, tags: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var category by remember { mutableStateOf(note?.category ?: "Personal") }
    var tags by remember { mutableStateOf(note?.tags ?: "") }

    val categories = listOf("Personal", "Travel", "Work", "Ideas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (note == null) "New Note" else "Edit Note",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            onSaveNote(title, content, category, tags)
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonPurple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VoidBlack
                )
            )
        },
        containerColor = VoidBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Note Title
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Note Title",
                        color = TextMuted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonPurple,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Formatting bar (panel 6 in mockup)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianCard)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { content += "**bold**" }) {
                    Icon(Icons.Default.FormatBold, "Bold", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { content += "*italic*" }) {
                    Icon(Icons.Default.FormatItalic, "Italic", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { content += "<u>underline</u>" }) {
                    Icon(Icons.Default.FormatUnderlined, "Underline", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { content += "\n• " }) {
                    Icon(Icons.Default.FormatListBulleted, "Bullet list", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { content += "\n☑ " }) {
                    Icon(Icons.Default.CheckBox, "Checkbox list", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { content += "\n```\ncode\n```" }) {
                    Icon(Icons.Default.Code, "Code block", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { /* image preview attachment */ }) {
                    Icon(Icons.Default.Image, "Image", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content area
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "Start typing your thoughts, tasks, or code...",
                        color = TextMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = NeonPurple,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Category & Tag Selector Bar at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Category",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = cat == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) NeonPurple else ObsidianCard)
                                .border(1.dp, if (isSelected) NeonCyan else ObsidianBorder, RoundedCornerShape(16.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
