package com.example.karttracker.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.DefaultLayout
import com.example.karttracker.components.HistoryViewModel
import com.example.karttracker.database.entity.RunSessionEntity
import com.google.type.Date
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun HistoryPage(
    navController: NavController,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val runSessions by historyViewModel.allRunSessions.collectAsState(initial = emptyList())

    MaterialTheme {
        DefaultLayout(title = "Sessions History") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (runSessions.isEmpty()) {
                    Text("No sessions recorded yet.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(runSessions) { session ->
                            RunSessionCard(session = session) {
                                // Navigate to SessionSummaryScreen, passing the sessionId
                                navController.navigate("summary_route/${session.id}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RunSessionCard(session: RunSessionEntity, onClick: (RunSessionEntity) -> Unit) {

    Column(
        modifier = Modifier.padding(16.dp).fillMaxWidth().clickable { onClick(session) }
    ) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        Text(
            text = "Run on ${dateFormat.format(java.util.Date(session.startTimeMillis))}",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Duration: ${formatTime(session.totalDurationMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        // You can add more details here if needed, e.g., total distance, average speed
    }
}
