package com.example.karttracker.pages

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karttracker.ui.theme.Typography
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Preview
@Composable
fun HomePage(){
    val context = LocalContext.current
    Column {
        MainTitle("Kart Tracker")
        NearestTrackCard()
        Box ( contentAlignment = Alignment.BottomEnd  )  {
            ElevatedButton(
                onClick = {
                    Toast.makeText(context, "Click", Toast.LENGTH_SHORT).show()
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