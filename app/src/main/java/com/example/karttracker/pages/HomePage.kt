package com.example.karttracker.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.karttracker.ui.theme.Typography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun HomePage(){
    Column {
        MainTitle("Kart Tracker")
        NearestTrackCard()
    }
}


@Composable
fun MainTitle(text: String){
    Text(text = text, style = Typography.headlineMedium)
}



@Composable
fun NearestTrackCard() {
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
                TrackLocationMap()
                DateAndWeather()
            }
        }
    }
}

@Composable
fun TrackLocationMap() {
    Box {
        Column {
            Text(text = "ECPA")
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
        contentAlignment = Alignment.CenterEnd // Aligns content to the end
    ) {
        Column(
            horizontalAlignment = Alignment.End // Aligns text to the right within Column
        ) {
            Text(text = "36°C", style = Typography.titleLarge, textAlign = TextAlign.End)
            Text(text = currentDateFormated(), style = Typography.bodyMedium, textAlign = TextAlign.End)
        }
    }
}