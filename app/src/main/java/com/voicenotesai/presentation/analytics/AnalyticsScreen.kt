package com.voicenotesai.presentation.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voicenotesai.presentation.components.GradientHeader
import com.voicenotesai.presentation.components.StatsCard

/**
 * Analytics Screen matching the mockup design
 * Displays note statistics, charts, and insights
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val analyticsData by viewModel.analyticsData.collectAsState()

    Scaffold(
        topBar = {
            GradientHeader(
                title = "Analytics",
                showSearch = false,
                onSearchQueryChange = {},
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Time Period Selector
            item {
                TimePeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { viewModel.selectPeriod(it) }
                )
            }

            // Key Metrics Cards
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCard(
                            label = "Total Notes",
                            value = "${analyticsData.totalNotes}",
                            modifier = Modifier.weight(1f),
                            valueColor = MaterialTheme.colorScheme.primary,
                            trend = analyticsData.notesTrend,
                            showTrend = true
                        )
                        
                        StatsCard(
                            label = "Completed Tasks",
                            value = "${analyticsData.completedTasks}",
                            modifier = Modifier.weight(1f),
                            valueColor = MaterialTheme.colorScheme.tertiary,
                            trend = analyticsData.tasksTrend,
                            showTrend = true
                        )
                    }
                    
                    StatsCard(
                        label = "Average Note Length",
                        value = "${analyticsData.averageNoteLength} words",
                        modifier = Modifier.fillMaxWidth(),
                        valueColor = MaterialTheme.colorScheme.secondary,
                        trend = analyticsData.lengthTrend,
                        showTrend = true
                    )
                }
            }



            // Bar Chart
            item {
                Text(
                    text = "Notes Created",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                BarChart(
                    data = analyticsData.dailyCounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // Top Tags
            item {
                Text(
                    text = "Top Tags",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(analyticsData.topTags) { tagCount ->
                TagCountItem(
                    tag = tagCount.tag,
                    count = tagCount.count,
                    maxCount = analyticsData.topTags.firstOrNull()?.count ?: 1
                )
            }

            // Empty state
            if (analyticsData.topTags.isEmpty()) {
                item {
                    EmptyTagsState()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimePeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        Color.Transparent,
                    label = "backgroundColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "textColor"
                )

                Button(
                    onClick = { onPeriodSelected(period) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = backgroundColor,
                        contentColor = textColor
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}



@Composable
private fun BarChart(
    data: List<DailyCount>,
    modifier: Modifier = Modifier
) {
    val maxCount = data.maxOfOrNull { it.count } ?: 1

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { dailyCount ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Bar
                    val barHeightFraction = if (maxCount > 0) 
                        (dailyCount.count.toFloat() / maxCount) 
                    else 
                        0f
                    val barHeight = 120.dp * barHeightFraction

                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(barHeight.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Label
                    Text(
                        text = dailyCount.label.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TagCountItem(
    tag: String,
    count: Int,
    maxCount: Int
) {
    val percentage = if (maxCount > 0) (count.toFloat() / maxCount) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tag name with indicator bar
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tag.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Count
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyTagsState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No tags yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start adding tags to your notes to see insights here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m" else "${seconds}s"
}
