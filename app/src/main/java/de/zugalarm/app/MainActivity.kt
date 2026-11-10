package de.zugalarm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF090D14)
private val Panel = Color(0xFF151D2A)
private val Accent = Color(0xFF36A9FF)
private val TextMain = Color(0xFFF3F6FA)
private val TextMuted = Color(0xFF9AA9BA)
private val Green = Color(0xFF45D483)

data class Connection(
    val train: String,
    val from: String,
    val to: String,
    val departure: String,
    val arrival: String,
    val duration: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZugAlarmApp() }
    }
}

@Composable
fun ZugAlarmApp() {
    var tab by remember { mutableIntStateOf(0) }
    var start by remember { mutableStateOf("Wuppertal Hbf") }
    var destination by remember { mutableStateOf("Köln Hbf") }
    var searched by remember { mutableStateOf(false) }
    var monitored by remember { mutableStateOf(listOf<Connection>()) }

    val demoConnections = listOf(
        Connection("RE 7", start, destination, "07:04", "08:12", "1 Std. 8 Min."),
        Connection("S 8", start, destination, "07:18", "08:35", "1 Std. 17 Min."),
        Connection("RE 13", start, destination, "07:32", "08:41", "1 Std. 9 Min.")
    )

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = Panel
        )
    ) {
        Scaffold(
            containerColor = Background,
            topBar = {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Background)
                        .padding(20.dp)
                ) {
                    Text(
                        "ZugAlarm",
                        color = TextMain,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Dein persönlicher Streckenagent",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Suche") }
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.Train, null) },
                        label = { Text("Meine Reisen") }
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.NotificationsActive, null) },
                        label = { Text("Meldungen") }
                    )
                }
            }
        ) { padding ->
            when (tab) {
                0 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            "Verbindung suchen",
                            color = TextMain,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = start,
                            onValueChange = { start = it },
                            label = { Text("Startbahnhof") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(9.dp))
                        OutlinedTextField(
                            value = destination,
                            onValueChange = { destination = it },
                            label = { Text("Zielbahnhof") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { searched = true },
                            enabled = start.isNotBlank() && destination.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Verbindungen suchen")
                        }
                    }

                    if (searched) {
                        item {
                            Text(
                                "Suchergebnisse",
                                color = TextMain,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Beispielverbindungen zur Ansicht",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                        items(demoConnections) { connection ->
                            ConnectionCard(
                                connection = connection,
                                buttonText = if (monitored.contains(connection))
                                    "Wird überwacht" else "Überwachen",
                                onClick = {
                                    if (!monitored.contains(connection)) {
                                        monitored = monitored + connection
                                    }
                                }
                            )
                        }
                        item {
                            Text(
                                "Hinweis: Diese Verbindungen sind Beispieldaten. Eine echte Fahrplansuche ist noch nicht angeschlossen.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Meine Reisen",
                            color = TextMain,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Deine ausgewählten Verbindungen",
                            color = TextMuted
                        )
                    }
                    if (monitored.isEmpty()) {
                        item {
                            Text(
                                "Noch keine Verbindung ausgewählt. Suche zuerst eine Verbindung und tippe auf Überwachen.",
                                color = TextMuted
                            )
                        }
                    }
                    items(monitored) { connection ->
                        ConnectionCard(
                            connection = connection,
                            buttonText = "Überwachung beenden",
                            onClick = { monitored = monitored - connection }
                        )
                    }
                }

                else -> Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Meldungen",
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Text(
                            "Noch keine Live-Meldungen. Sobald eine Echtzeit-Datenquelle angeschlossen ist, können hier Verspätungen, Gleisänderungen und Ausfälle erscheinen.",
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    connection: Connection,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                connection.train,
                color = Accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(connection.departure, color = TextMain, fontSize = 22.sp)
                    Text(connection.from, color = TextMuted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(connection.duration, color = TextMuted, fontSize = 11.sp)
                    Text("→", color = Accent, fontSize = 22.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(connection.arrival, color = TextMain, fontSize = 22.sp)
                    Text(connection.to, color = TextMuted, fontSize = 12.sp)
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}
