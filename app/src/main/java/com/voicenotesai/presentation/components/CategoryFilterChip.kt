package com.voicenotesai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voicenotesai.domain.ai.ContentCategory

/**
 * A filter chip component for category selection with color-coded organization.
 */
@Composable
fun CategoryFilterChip(
    category: ContentCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    showCount: Int? = null
) {
    val backgroundColor = if (isSelected) {
        category.defaultColor.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isSelected) {
        category.defaultColor
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    
    val textColor = if (isSelected) {
        category.defaultColor
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor
        )
        
        showCount?.let { count ->
            if (count > 0) {
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * A horizontal scrollable row of category filter chips.
 */
@Composable
fun CategoryFilterRow(
    categories: List<ContentCategory>,
    selectedCategories: Set<ContentCategory>,
    onCategoryToggle: (ContentCategory) -> Unit,
    modifier: Modifier = Modifier,
    categoryCounts: Map<ContentCategory, Int> = emptyMap(),
    showIcons: Boolean = true,
    allowMultipleSelection: Boolean = false
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            CategoryFilterChip(
                category = category,
                isSelected = category in selectedCategories,
                onClick = {
                    if (allowMultipleSelection) {
                        onCategoryToggle(category)
                    } else {
                        // Single selection mode
                        if (category in selectedCategories) {
                            onCategoryToggle(category) // Deselect
                        } else {
                            onCategoryToggle(category) // Select (will replace current selection)
                        }
                    }
                },
                showIcon = showIcons,
                showCount = categoryCounts[category]
            )
        }
    }
}

/**
 * A category suggestion chip with confidence indicator.
 */
@Composable
fun CategorySuggestionChip(
    category: ContentCategory,
    confidence: Float,
    reason: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = category.defaultColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = category.defaultColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.defaultColor,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = category.defaultColor,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Confidence indicator
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                ) {
                    Text("Dismiss")
                }
                
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = category.defaultColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

/**
 * A compact category indicator for note cards.
 */
@Composable
fun CategoryIndicator(
    category: ContentCategory,
    modifier: Modifier = Modifier,
    size: CategoryIndicatorSize = CategoryIndicatorSize.Small
) {
    val iconSize = when (size) {
        CategoryIndicatorSize.Small -> 12.dp
        CategoryIndicatorSize.Medium -> 16.dp
        CategoryIndicatorSize.Large -> 20.dp
    }
    
    val padding = when (size) {
        CategoryIndicatorSize.Small -> 4.dp
        CategoryIndicatorSize.Medium -> 6.dp
        CategoryIndicatorSize.Large -> 8.dp
    }

    Box(
        modifier = modifier
            .background(
                color = category.defaultColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.displayName,
            tint = category.defaultColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

enum class CategoryIndicatorSize {
    Small, Medium, Large
}