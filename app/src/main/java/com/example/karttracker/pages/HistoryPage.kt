package com.example.karttracker.pages

import androidx.compose.foundation.Image
import com.example.karttracker.R
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.DefaultLayout
import com.example.karttracker.components.HistoryViewModel
import com.example.karttracker.database.entity.RunSessionEntity
import com.example.karttracker.icons.Hourglass
import com.example.karttracker.utils.TimeUtils
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
                                },
                                onShare = {}
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
    onDeleteClick: (Long) -> Unit,
    onShare: (RunSessionEntity) -> Unit
) {

    val nameIsEmpty: Boolean = true;
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }.format(java.util.Date(session.startTimeMillis))

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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            //Main Info
            Column (modifier = Modifier.weight(1f)) {
                    Row {
                        Column (modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nameIsEmpty) "$dateFormat Session" else session.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(dateFormat,
                            style = MaterialTheme.typography.bodySmall)

                        }
                        // Overflow Menu
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Share") },
                                    onClick = {
                                        expanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        expanded = false
                                        onDeleteClick(session.id)
                                    }
                                )
                            }
                        }
                    }



                Spacer(modifier = Modifier.height(4.dp))

                // Thumbnail
                Row (
                    ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_runsession_placeholder), // fallback image
                        contentDescription = "Session photo placeholder",
                        modifier = Modifier
                            .size(156.dp, 56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⏱ ${TimeUtils.formatTime(session.totalDurationMillis)}",
                        style = MaterialTheme.typography.bodySmall)
                    Text("🏁 ${session.lapCount} laps",
                        style = MaterialTheme.typography.bodySmall)
                    session.avgLapTimeMillis.let {
                        Text("⚡ ${TimeUtils.formatTime(it)} avg",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun RunSessionCardPreview() {
    RunSessionCard(
        session = RunSessionEntity(
            id = 1,
            name = "Test Session",
            startTimeMillis =  0L,
            endTimeMillis = 600000L,
            totalDurationMillis = 600000L,
            avgLapTimeMillis = 90000L,
            maxSpeed = 80.0,
            lapCount = 5,
            fastestLap = 8
        ),
        onClick = {},
        onDeleteClick = {},
        onShare = {}
    )
}