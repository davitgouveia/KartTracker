package com.example.karttracker.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.DefaultLayout
import com.example.karttracker.components.HistoryViewModel
import com.example.karttracker.database.entity.RunSessionEntity
import com.example.karttracker.icons.Hourglass
import com.google.type.Date
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun HistoryPage(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val runSessions by historyViewModel.allRunSessions.collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }
    var sessionToDeleteId by remember { mutableStateOf<Long?>(null) }


    MaterialTheme {
        DefaultLayout(title = "Sessions History") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (runSessions.isEmpty()) {
                    Text("No sessions recorded yet.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(runSessions) { session ->
                            RunSessionCard(
                                session = session,
                                onClick = {
                                    navController.navigate("summary_route/${session.id}")
                                },
                                onDeleteClick = { id ->
                                    sessionToDeleteId = id
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }

                    // Delete Confirmation Dialog
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                // Dismiss the dialog if clicked outside or back button pressed
                                showDeleteDialog = false
                                sessionToDeleteId = null // Clear the ID
                            },
                            title = { Text("Confirm Deletion") },
                            text = { Text("Are you sure you want to delete this session? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    sessionToDeleteId?.let { id ->
                                        historyViewModel.deleteSession(id) // Perform deletion
                                    }
                                    showDeleteDialog = false
                                    sessionToDeleteId = null // Clear the ID
                                }) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                    sessionToDeleteId = null // Clear the ID
                                }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunSessionCard(
    session: RunSessionEntity,
    onClick: (RunSessionEntity) -> Unit,
    onDeleteClick: (Long) -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(session) }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray // Use a subtle background color from your theme
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Apply padding inside the card for content
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

                Text(
                    text = if (session.name.isEmpty()) "Session on ${dateFormat.format(
                        java.util.Date(
                            session.startTimeMillis
                        )
                    )}" else session.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))


                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Hourglass,
                        contentDescription = "Duration",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Duration: ${formatTime(session.totalDurationMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { onDeleteClick(session.id) },
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Session",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
