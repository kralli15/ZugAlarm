package de.zugalarm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF17365D)
private val Blue = Color(0xFF2673B8)
private val Pale = Color(0xFFF2F5F8)
private val Green = Color(0xFF287A4B)
private val Red = Color(0xFFB3261E)
private val Amber = Color(0xFF9A6200)

data class TrainLeg(
    val train: String,
    val from: String,
    val fromTrack: String,
    val plannedDeparture: String,
    val liveDeparture: String,
    val to: String,
    val toTrack: String,
    val plannedArrival: String,
    val liveArrival: String,
    val note: String = "Beispieldaten"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZugAlarmApp() }
    }
}

@Composable
fun ZugAlarmApp() {
    var selectedTab by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var legs by remember {
        mutableStateOf(
            listOf(
                TrainLeg("RE 7", "Wuppertal Hbf", "1", "07:04", "07:04", "Köln Hbf", "5", "08:12", "08:12"),
                TrainLeg("RB 48", "Köln Hbf", "4", "08:21", "08:21", "Bonn Hbf", "2", "08:49", "08:49")
            )
        )
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = Blue,
            background = Pale,
            surface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Pale,
            topBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Navy)
                        .padding(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    Text(
                        "ZugAlarm",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Dein persönlicher Streckenagent",
                        color = Color.White.copy(alpha = .86f),
                        fontSize = 12.sp
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Train, contentDescription = null) },
                        label = { Text("Meine Reise") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null) },
                        label = { Text("Meldungen") }
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { showAdd = true },
                        containerColor = Blue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zug hinzufügen")
                    }
                }
            }
        ) { padding ->
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Meine Reisekette",
                                    color = Navy,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Wuppertal Hbf  →  Bonn Hbf",
                                    color = Color.DarkGray,
                                    fontSize = 12.sp
                                )
                            }
                            Surface(
                                color = Color(0xFFE5F2E9),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "Überwachung aktiv",
                                    color = Green,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    items(legs.size) { index ->
                        CompactLegCard(index + 1, legs[index])
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Anschlusskontrolle", color = Navy, fontWeight = FontWeight.Bold)
                                Text(
                                    "ZugAlarm vergleicht die prognostizierte Ankunft mit der nächsten Abfahrt. Bei gefährdeten Anschlüssen soll hier eine Alternative erscheinen.",
                                    color = Color.DarkGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            "Demoansicht: Zeiten und Gleise sind Beispieldaten. Live-Daten und automatische Push-Alarme sind noch nicht verbunden.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Meldungen & Alternativen",
                        color = Navy,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Noch keine Live-Meldungen", color = Navy, fontWeight = FontWeight.Bold)
                            Text(
                                "Nach Anbindung einer Echtzeit-Datenquelle erscheinen hier Verspätungen, Gleisänderungen, Zugausfälle und alternative Reiseketten.",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        if (showAdd) {
            AddLegDialog(
                onDismiss = { showAdd = false },
                onAdd = { train, from, dep, fromTrack, to, arr, toTrack ->
                    legs = legs + TrainLeg(
                        train,
                        from,
                        fromTrack,
                        dep,
                        dep,
                        to,
                        toTrack,
                        arr,
                        arr,
                        "Manuell hinzugefügt"
                    )
                    showAdd = false
                }
            )
        }
    }
}

@Composable
fun CompactLegCard(number: Int, leg: TrainLeg) {
    val delayed = leg.plannedDeparture != leg.liveDeparture || leg.plannedArrival != leg.liveArrival

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$number.  ${leg.train}  ·  ${leg.from} → ${leg.to}",
                    color = Navy,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    if (delayed) "Abweichung" else "Planmäßig",
                    color = if (delayed) Red else Green,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeBlock(
                    modifier = Modifier.weight(1f),
                    title = "ABFAHRT · GL. ${leg.fromTrack}",
                    station = leg.from,
                    planned = leg.plannedDeparture,
                    live = leg.liveDeparture,
                    delayed = leg.plannedDeparture != leg.liveDeparture
                )
                TimeBlock(
                    modifier = Modifier.weight(1f),
                    title = "ANKUNFT · GL. ${leg.toTrack}",
                    station = leg.to,
                    planned = leg.plannedArrival,
                    live = leg.liveArrival,
                    delayed = leg.plannedArrival != leg.liveArrival
                )
            }
        }
    }
}

@Composable
fun TimeBlock(
    modifier: Modifier,
    title: String,
    station: String,
    planned: String,
    live: String,
    delayed: Boolean
) {
    Column(
        modifier.background(Pale, RoundedCornerShape(8.dp)).padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(station, color = Navy, fontSize = 10.sp, maxLines = 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(planned, color = Color.Gray, fontSize = 12.sp)
            Text("→", color = Color.Gray, fontSize = 10.sp)
            Text(live, color = if (delayed) Red else Green, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text("Plan       Echtzeit", color = Color.Gray, fontSize = 9.sp)
    }
}

@Composable
fun AddLegDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, String, String) -> Unit
) {
    var train by remember { mutableStateOf("RE 7") }
    var from by remember { mutableStateOf("") }
    var fromTrack by remember { mutableStateOf("") }
    var departure by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var toTrack by remember { mutableStateOf("") }
    var arrival by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zugverbindung hinzufügen", color = Navy, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(
                    value = train,
                    onValueChange = { train = it },
                    label = { Text("Zugnummer, z. B. RE 7") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = from,
                    onValueChange = { from = it },
                    label = { Text("Startbahnhof") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = fromTrack,
                        onValueChange = { fromTrack = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Startgleis") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = departure,
                        onValueChange = { departure = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Planabfahrt") },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = to,
                    onValueChange = { to = it },
                    label = { Text("Zielbahnhof") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = toTrack,
                        onValueChange = { toTrack = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Zielgleis") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = arrival,
                        onValueChange = { arrival = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Planankunft") },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = train.isNotBlank() && from.isNotBlank() && to.isNotBlank(),
                onClick = {
                    onAdd(
                        train.trim(),
                        from.trim(),
                        departure.ifBlank { "—" },
                        fromTrack.ifBlank { "—" },
                        to.trim(),
                        arrival.ifBlank { "—" },
                        toTrack.ifBlank { "—" }
                    )
                }
            ) {
                Text("Hinzufügen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}