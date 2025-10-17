package com.example.karttracker.components.Session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SessionDataBlock(title: String, prefix: String, data: String){
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall)
        Row (verticalAlignment = Alignment.CenterVertically) {
            if(prefix.isNotBlank()){
                Text(prefix, modifier = Modifier.padding(end = 4.dp), style = MaterialTheme.typography.bodySmall)
            }
            Text(data, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
