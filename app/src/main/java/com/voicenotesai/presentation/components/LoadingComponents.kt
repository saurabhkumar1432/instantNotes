package com.voicenotesai.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.animations.LoadingSkeleton
import com.voicenotesai.presentation.theme.Spacing

/**
 * Loading skeleton for notes list
 */
@Composable
fun NotesListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 3
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.medium)
    ) {
        items(itemCount) {
            NoteItemSkeleton()
        }
    }
}

/**
 * Loading skeleton for individual note item
 */
@Composable
fun NoteItemSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            // Header with timestamp and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LoadingSkeleton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                
                LoadingSkeleton(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape
                )
            }
            
            // Content preview
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                LoadingSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                LoadingSkeleton(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                LoadingSkeleton(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

/**
 * Loading skeleton for processing state
 */
@Composable
fun ProcessingSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        // Processing indicator
        LoadingSkeleton(
            modifier = Modifier.size(72.dp),
            shape = CircleShape
        )
        
        // Status text
        LoadingSkeleton(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp),
            shape = RoundedCornerShape(12.dp)
        )
        
        // Description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            LoadingSkeleton(
                modifier = Modifier
                    .width(250.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(8.dp)
            )
            LoadingSkeleton(
                modifier = Modifier
                    .width(180.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

/**
 * Loading skeleton for settings screen
 */
@Composable
fun SettingsFormSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.large)
    ) {
        // Section header
        LoadingSkeleton(
            modifier = Modifier
                .width(150.dp)
                .height(24.dp),
            shape = RoundedCornerShape(12.dp)
        )
        
        // Form fields
        repeat(3) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small)
            ) {
                LoadingSkeleton(
                    modifier = Modifier
                        .width(100.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                LoadingSkeleton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.medium))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            LoadingSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            )
            LoadingSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}