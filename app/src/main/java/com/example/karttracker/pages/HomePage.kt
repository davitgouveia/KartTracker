package com.example.karttracker.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.karttracker.components.DefaultLayout
import com.example.karttracker.components.HomePageViewModel
import com.example.karttracker.ui.theme.Typography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun HomePage(navController: NavController, viewModel: HomePageViewModel = hiltViewModel()){
    val currentCity by viewModel.currentCity.collectAsState()

    MaterialTheme {
        DefaultLayout(title = "Kart Tracker") {
            Column (modifier = Modifier.fillMaxSize()) {
                NearestTrackCard(currentCity)
                Box ( modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center  )  {
                    ElevatedButton(
                        onClick = {
                            navController.navigate("map")
                        },
                        modifier = Modifier.width(150.dp).height(150.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "RACE",
                            fontSize = 35.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

            }
        }
    }
}

@Composable
fun NearestTrackCard(currentCity: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackLocationMap(currentCity = currentCity)
                DateAndWeather()
            }
        }
    }
}

@Composable
fun TrackLocationMap(currentCity: String) {
    Box {
        Column {
            Text(text = currentCity, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun currentDateFormated(): String {
    val dataAtual = LocalDate.now()
    val formatador = DateTimeFormatter.ofPattern("EEE dd 'de' MMMM", Locale("pt", "BR"))
    return dataAtual.format(formatador).replaceFirstChar { it.uppercase() }
}

@Composable
fun DateAndWeather() {
    Box(

        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {

            Text(text = currentDateFormated(), style = Typography.bodyMedium, textAlign = TextAlign.End)
        }
    }
}