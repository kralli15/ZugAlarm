
package de.zugalarm.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Background = Color(0xFF090D14)
private val Panel = Color(0xFF151D2A)
private val Accent = Color(0xFF36A9FF)
private val TextMain = Color(0xFFF3F6FA)
private val TextMuted = Color(0xFF9AA9BA)

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

private fun loadMonitored(context: Context): List<Connection> {
    val prefs = context.getSharedPreferences("ZugAlarm", Context.MODE_PRIVATE)
    val json = prefs.getString("monitored", "[]") ?: "[]"
    val array = JSONArray(json)

    return List(array.length()) { i ->
        val item = array.getJSONObject(i)
        Connection(
            train = item.getString("train"),
            from = item.getString("from"),
            to = item.getString("to"),
            departure = item.getString("departure"),
            arrival = item.getString("arrival"),
            duration = item.getString("duration")
        )
    }
}

private fun saveMonitored(context: Context, connections: List<Connection>) {
    val array = JSONArray()

    connections.forEach { connection ->
        val item = JSONObject()
        item.put("train", connection.train)
        item.put("from", connection.from)
        item.put("to", connection.to)
        item.put("departure", connection.departure)
        item.put("arrival", connection.arrival)
        item.put("duration", connection.duration)
        array.put(item)
    }

    context.getSharedPreferences("ZugAlarm", Context.MODE_PRIVATE)
        .edit()
        .putString("monitored", array.toString())
        .apply()
}

@Composable
fun ZugAlarmApp() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var tab by remember { mutableIntStateOf(0) }

    var start by remember { mutableStateOf("Wuppertal") }
    var destination by remember { mutableStateOf("Köln") }

    var fromPlaces by remember { mutableStateOf<List<TransitPlace>>(emptyList()) }
    var toPlaces by remember { mutableStateOf<List<TransitPlace>>(emptyList()) }

    var selectedFrom by remember { mutableStateOf<TransitPlace?>(null) }
    var selectedTo by remember { mutableStateOf<TransitPlace?>(null) }

    var date by remember {
        mutableStateOf(LocalDate.now().toString())
    }
    var time by remember {
        mutableStateOf(
            LocalTime.now().plusMinutes(5)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }

    var arriveBy by remember { mutableStateOf(false) }

    var connections by remember {
        mutableStateOf<List<TransitConnection>>(emptyList())
    }

    var monitored by remember {
        mutableStateOf(loadMonitored(context))
    }

    var searchLoading by remember { mutableStateOf(false) }
    var connectionLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchInfo by remember { mutableStateOf("") }

    var realtimeMessages by remember {
        mutableStateOf<List<RealtimeMessage>>(emptyList())
    }
    var realtimeFeedTime by remember { mutableStateOf("") }
    var realtimeError by remember { mutableStateOf<String?>(null) }
    var realtimeLoading by remember { mutableStateOf(false) }
    var realtimeLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(monitored) {
        saveMonitored(context, monitored)
    }

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
                    Modifier
                        .fillMaxWidth()
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
                        icon = {
                            Icon(Icons.Default.NotificationsActive, null)
                        },
                        label = { Text("Meldungen") }
                    )
                }
            }
        ) { padding ->

            when (tab) {
                0 -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Verbindung suchen",
                            color = TextMain,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = start,
                            onValueChange = {
                                start = it
                                selectedFrom = null
                                fromPlaces = emptyList()
                                connections = emptyList()
                            },
                            label = { Text("Startbahnhof oder Haltestelle") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = destination,
                            onValueChange = {
                                destination = it
                                selectedTo = null
                                toPlaces = emptyList()
                                connections = emptyList()
                            },
                            label = { Text("Zielbahnhof oder Haltestelle") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    searchLoading = true
                                    searchError = null
                                    searchInfo = ""
                                    connections = emptyList()
                                    selectedFrom = null
                                    selectedTo = null

                                    try {
                                        fromPlaces =
                                            TransitousClient.searchPlaces(start)
                                        toPlaces =
                                            TransitousClient.searchPlaces(destination)

                                        if (fromPlaces.isEmpty()) {
                                            searchInfo =
                                                "Für den Start wurde keine Haltestelle gefunden."
                                        } else if (toPlaces.isEmpty()) {
                                            searchInfo =
                                                "Für das Ziel wurde keine Haltestelle gefunden."
                                        } else {
                                            searchInfo =
                                                "Bitte wähle die passende Haltestelle für Start und Ziel."
                                        }
                                    } catch (e: Exception) {
                                        searchError =
                                            e.message ?: "Fehler bei der Haltestellensuche."
                                    } finally {
                                        searchLoading = false
                                    }
                                }
                            },
                            enabled = !searchLoading &&
                                start.isNotBlank() &&
                                destination.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (searchLoading)
                                    "Haltestellen werden gesucht ..."
                                else
                                    "Haltestellen suchen"
                            )
                        }
                    }

                    if (fromPlaces.isNotEmpty()) {
                        item {
                            Text(
                                "Start auswählen",
                                color = TextMain,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(fromPlaces.take(8)) { place ->
                            PlaceCard(
                                place = place,
                                selected = selectedFrom?.id == place.id,
                                onClick = {
                                    selectedFrom = place
                                }
                            )
                        }
                    }

                    if (toPlaces.isNotEmpty()) {
                        item {
                            Text(
                                "Ziel auswählen",
                                color = TextMain,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(toPlaces.take(8)) { place ->
                            PlaceCard(
                                place = place,
                                selected = selectedTo?.id == place.id,
                                onClick = {
                                    selectedTo = place
                                }
                            )
                        }
                    }

                    if (selectedFrom != null && selectedTo != null) {
                        item {
                            Text(
                                "Reisedatum und Uhrzeit",
                                color = TextMain,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Datum (JJJJ-MM-TT)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = time,
                                onValueChange = { time = it },
                                label = { Text("Uhrzeit (HH:MM)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !arriveBy,
                                    onClick = { arriveBy = false },
                                    label = { Text("Abfahrt") }
                                )
                                FilterChip(
                                    selected = arriveBy,
                                    onClick = { arriveBy = true },
                                    label = { Text("Ankunft") }
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        connectionLoading = true
                                        searchError = null
                                        searchInfo = ""
                                        connections = emptyList()

                                        try {
                                            val dateTime =
                                                LocalDateTime.of(
                                                    LocalDate.parse(date),
                                                    LocalTime.parse(time)
                                                )
                                                    .atZone(ZoneId.systemDefault())
                                                    .toOffsetDateTime()
                                                    .toString()

                                            connections =
                                                TransitousClient.searchConnections(
                                                    selectedFrom!!,
                                                    selectedTo!!,
                                                    dateTime,
                                                    arriveBy
                                                )

                                            searchInfo =
                                                if (connections.isEmpty()) {
                                                    "Keine Verbindungen gefunden. Das bedeutet nicht automatisch, dass die Strecke gesperrt ist."
                                                } else {
                                                    "${connections.size} Verbindungen gefunden."
                                                }
                                        } catch (e: Exception) {
                                            searchError =
                                                e.message ?: "Fehler bei der Verbindungssuche. Bitte Datum und Uhrzeit prüfen."
                                        } finally {
                                            connectionLoading = false
                                        }
                                    }
                                },
                                enabled = !connectionLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (connectionLoading)
                                        "Verbindungen werden gesucht ..."
                                    else
                                        "Echte Verbindungen suchen"
                                )
                            }
                        }
                    }

                    if (searchError != null) {
                        item {
                            Text(
                                searchError!!,
                                color = Color(0xFFFF7777)
                            )
                        }
                    }

                    if (searchInfo.isNotBlank()) {
                        item {
                            Text(
                                searchInfo,
                                color = TextMuted
                            )
                        }
                    }

                    items(connections) { result ->
                        val connection = Connection(
                            train = result.label,
                            from = result.from,
                            to = result.to,
                            departure = result.departure,
                            arrival = result.arrival,
                            duration = result.duration
                        )

                        ConnectionCard(
                            connection = connection,
                            buttonText = if (monitored.contains(connection))
                                "Wird überwacht"
                            else
                                "Überwachen",
                            onClick = {
                                if (!monitored.contains(connection)) {
                                    monitored = monitored + connection
                                }
                            }
                        )
                    }

                    item {
                        Text(
                            "Fahrplandaten: Transitous. Die Verfügbarkeit von Echtzeitdaten hängt vom jeweiligen Verkehrsunternehmen ab.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Text(
                            "Datenquellen und Hinweise ansehen",
                            color = Accent,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://transitous.org/sources/")
                            }
                        )

                        Text(
                            "Kartendaten © OpenStreetMap contributors",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                1 -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
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
                            onClick = {
                                monitored = monitored - connection
                            }
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Meldungen",
                            color = TextMain,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Öffentliche Echtzeitdaten aus Deutschland",
                            color = TextMuted,
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    realtimeLoading = true
                                    realtimeError = null

                                    try {
                                        val result = GtfsRealtimeClient.load()
                                        realtimeMessages = result.messages
                                        realtimeFeedTime = result.feedTime
                                        realtimeLoaded = true
                                    } catch (e: Exception) {
                                        realtimeError =
                                            e.message ?: "Unbekannter Fehler"
                                    } finally {
                                        realtimeLoading = false
                                    }
                                }
                            },
                            enabled = !realtimeLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (realtimeLoading)
                                    "Meldungen werden geladen ..."
                                else
                                    "Meldungen aktualisieren"
                            )
                        }
                    }

                    if (realtimeError != null) {
                        item {
                            Text(
                                "Fehler beim Laden:\n$realtimeError",
                                color = Color(0xFFFF7777)
                            )
                        }
                    }

                    if (realtimeLoaded && realtimeError == null) {
                        item {
                            Text(
                                "Datenstand: $realtimeFeedTime",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }

                        if (realtimeMessages.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Panel
                                    )
                                ) {
                                    Text(
                                        "Aktuell wurden keine Verkehrsmeldungen in diesem Datenstrom gefunden. Das bedeutet nicht automatisch, dass alle Züge pünktlich fahren.",
                                        color = TextMuted,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        items(realtimeMessages.take(30)) { message ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Panel
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        message.title,
                                        color = TextMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )

                                    if (message.description.isNotBlank()) {
                                        Text(
                                            message.description,
                                            color = TextMuted
                                        )
                                    }

                                    Text(
                                        "Betroffen: ${message.affected}",
                                        color = Accent,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!realtimeLoaded && realtimeError == null) {
                        item {
                            Text(
                                "Tippe auf „Meldungen aktualisieren“, um aktuelle Verkehrsmeldungen abzurufen.",
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceCard(
    place: TransitPlace,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF203D59) else Panel
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                place.name,
                color = TextMain,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (selected) "Ausgewählt" else "Zum Auswählen antippen",
                color = if (selected) Accent else TextMuted,
                fontSize = 12.sp
            )
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
                    Text(
                        connection.departure,
                        color = TextMain,
                        fontSize = 22.sp
                    )
                    Text(
                        connection.from,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        connection.duration,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        "→",
                        color = Accent,
                        fontSize = 22.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        connection.arrival,
                        color = TextMain,
                        fontSize = 22.sp
                    )
                    Text(
                        connection.to,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
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
