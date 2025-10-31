package com.voicenotesai.presentation.sharing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.voicenotesai.R
import com.voicenotesai.domain.sharing.CalendarEventDetails
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for creating calendar events from meeting notes
 */
@Composable
fun CalendarEventDialog(
    isVisible: Boolean,
    noteTitle: String,
    noteContent: String,
    suggestedEvent: CalendarEventDetails?,
    onDismiss: () -> Unit,
    onCreateEvent: (CalendarEventDetails) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(suggestedEvent?.title ?: noteTitle) }
    var description by remember { mutableStateOf(suggestedEvent?.description ?: noteContent) }
    var location by remember { mutableStateOf(suggestedEvent?.location ?: "") }
    var startDate by remember { mutableStateOf(Date(suggestedEvent?.startTime ?: System.currentTimeMillis())) }
    var startTime by remember { mutableStateOf(Date(suggestedEvent?.startTime ?: System.currentTimeMillis())) }
    var endDate by remember { mutableStateOf(Date(suggestedEvent?.endTime ?: (System.currentTimeMillis() + 60 * 60 * 1000))) }
    var endTime by remember { mutableStateOf(Date(suggestedEvent?.endTime ?: (System.currentTimeMillis() + 60 * 60 * 1000))) }
    var attendees by remember { mutableStateOf(suggestedEvent?.attendees?.joinToString(", ") ?: "") }
    
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    if (isVisible) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.create_calendar_event),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    }
                    
                    if (suggestedEvent != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.ai_suggested_event_details),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    
                    // Event title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.event_title)) },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    // Event description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.event_description)) },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    // Location
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(stringResource(R.string.event_location)) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    
                    // Start date and time
                    Text(
                        text = stringResource(R.string.start_time),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dateFormat.format(startDate),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.date)) },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = timeFormat.format(startTime),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.time)) },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // End date and time
                    Text(
                        text = stringResource(R.string.end_time),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dateFormat.format(endDate),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.date)) },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OutlinedTextField(
                            value = timeFormat.format(endTime),
                            onValueChange = { },
                            label = { Text(stringResource(R.string.time)) },
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Attendees
                    OutlinedTextField(
                        value = attendees,
                        onValueChange = { attendees = it },
                        label = { Text(stringResource(R.string.attendees)) },
                        leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.attendees_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        
                        Button(
                            onClick = {
                                val startCalendar = Calendar.getInstance().apply {
                                    time = startDate
                                    val timeCalendar = Calendar.getInstance().apply { time = startTime }
                                    set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                }
                                
                                val endCalendar = Calendar.getInstance().apply {
                                    time = endDate
                                    val timeCalendar = Calendar.getInstance().apply { time = endTime }
                                    set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                                }
                                
                                val eventDetails = CalendarEventDetails(
                                    title = title,
                                    description = description,
                                    startTime = startCalendar.timeInMillis,
                                    endTime = endCalendar.timeInMillis,
                                    location = location.takeIf { it.isNotBlank() },
                                    attendees = attendees.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                )
                                
                                onCreateEvent(eventDetails)
                            },
                            enabled = !isLoading && title.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.create_event))
                            }
                        }
                    }
                }
            }
        }
    }
}