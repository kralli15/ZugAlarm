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
    val duration: String,
    val legs: List<TransitLeg> = emptyList()
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZugAlarmApp() }
    }
}

private fun loadMonitored(context: Context): List<Connection> {
    val prefs = context.getSharedPreferences("ZugAlarm", Context.MODE_PRIVATE)
    val array = JSONArray(prefs.getString("monitored", "[]") ?: "[]")

    return List(array.length()) { i ->
        val item = array.getJSONObject(i)
        val legArray = item.optJSONArray("legs") ?: JSONArray()

        val legs = (0 until legArray.length()).map { index ->
            val leg = legArray.getJSONObject(index)

            TransitLeg(
                line = leg.optString("line"),
                mode = leg.optString("mode"),
                from = leg.optString("from"),
                to = leg.optString("to"),
                departure = leg.optString("departure"),
                arrival = leg.optString("arrival"),
                scheduledDeparture = leg.optString("scheduledDeparture", "?"),
                scheduledArrival = leg.optString("scheduledArrival", "?"),
                departurePlatform = leg.optString("departurePlatform"),
                arrivalPlatform = leg.optString("arrivalPlatform"),
                realtimeAvailable = leg.optBoolean("realtimeAvailable"),
                cancelled = leg.optBoolean("cancelled"),
                tripId = leg.optString("tripId"),
                routeId = leg.optString("routeId"),
                headsign = leg.optString("headsign")
            )
        }

        Connection(
            train = item.getString("train"),
            from = item.getString("from"),
            to = item.getString("to"),
            departure = item.getString("departure"),
            arrival = item.getString("arrival"),
            duration = item.getString("duration"),
            legs = legs
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

        val legArray = JSONArray()

        connection.legs.forEach { leg ->
            val legJson = JSONObject()
            legJson.put("line", leg.line)
            legJson.put("mode", leg.mode)
            legJson.put("from", leg.from)
            legJson.put("to", leg.to)
            legJson.put("departure", leg.departure)
            legJson.put("arrival", leg.arrival)
            legJson.put("scheduledDeparture", leg.scheduledDeparture)
            legJson.put("scheduledArrival", leg.scheduledArrival)
            legJson.put("departurePlatform", leg.departurePlatform)
            legJson.put("arrivalPlatform", leg.arrivalPlatform)
            legJson.put("realtimeAvailable", leg.realtimeAvailable)
            legJson.put("cancelled", leg.cancelled)
            legJson.put("tripId", leg.tripId)
            legJson.put("routeId", leg.routeId)
            legJson.put("headsign", leg.headsign)
            legArray.put(legJson)
        }

        item.put("legs", legArray)
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

    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var time by remember {
        mutableStateOf(
            LocalTime.now().plusMinutes(5)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        )
    }

    var arriveBy by remember { mutableStateOf(false) }
    var connections by remember { mutableStateOf<List<TransitConnection>>(emptyList()) }
    var monitored by remember { mutableStateOf(loadMonitored(context)) }

    var searchLoading by remember { mutableStateOf(false) }
    var connectionLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchInfo by remember { mutableStateOf("") }

    var realtimeMessages by remember { mutableStateOf<List<RealtimeMessage>>(emptyList()) }
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
                        icon = { Icon(Icons.Default.NotificationsActive, null) },
                        label = { Text("Meldungen") }
                    )
                }
            }
        ) { padding ->
            when (tab) {
                0 -> SearchScreen(
                    padding = padding,
                    start = start,
                    destination = destination,
                    onStartChange = {
                        start = it
                        selectedFrom = null
                        fromPlaces = emptyList()
                        connections = emptyList()
                    },
                    onDestinationChange = {
                        destination = it
                        selectedTo = null
                        toPlaces = emptyList()
                        connections = emptyList()
                    },
                    fromPlaces = fromPlaces,
                    toPlaces = toPlaces,
                    selectedFrom = selectedFrom,
                    selectedTo = selectedTo,
                    onFromPlaces = { fromPlaces = it },
                    onToPlaces = { toPlaces = it },
                    onSelectFrom = { selectedFrom = it },
                    onSelectTo = { selectedTo = it },
                    date = date,
                    onDateChange = { date = it },
                    time = time,
                    onTimeChange = { time = it },
                    arriveBy = arriveBy,
                    onArriveByChange = { arriveBy = it },
                    connections = connections,
                    onConnections = { connections = it },
                    searchLoading = searchLoading,
                    onSearchLoading = { searchLoading = it },
                    connectionLoading = connectionLoading,
                    onConnectionLoading = { connectionLoading = it },
                    searchError = searchError,
                    onSearchError = { searchError = it },
                    searchInfo = searchInfo,
                    onSearchInfo = { searchInfo = it },
                    monitored = monitored,
                    onMonitor = { connection ->
                        if (!monitored.contains(connection)) {
                            monitored = monitored + connection
                        }
                    },
                    scope = scope,
                    uriHandler = uriHandler
                )

                1 -> MyTripsScreen(
                    padding = padding,
                    monitored = monitored,
                    onRemove = { monitored = monitored - it }
                )

                else -> MessagesScreen(
                    padding = padding,
                    messages = realtimeMessages,
                    feedTime = realtimeFeedTime,
                    error = realtimeError,
                    loading = realtimeLoading,
                    loaded = realtimeLoaded,
                    onLoad = {
                        scope.launch {
                            realtimeLoading = true
                            realtimeError = null
                            try {
                                val result = GtfsRealtimeClient.load()
                                realtimeMessages = result.messages
                                realtimeFeedTime = result.feedTime
                                realtimeLoaded = true
                            } catch (e: Exception) {
                                realtimeError = e.message ?: "Unbekannter Fehler"
                            } finally {
                                realtimeLoading = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchScreen(
    padding: PaddingValues,
    start: String,
    destination: String,
    onStartChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    fromPlaces: List<TransitPlace>,
    toPlaces: List<TransitPlace>,
    selectedFrom: TransitPlace?,
    selectedTo: TransitPlace?,
    onFromPlaces: (List<TransitPlace>) -> Unit,
    onToPlaces: (List<TransitPlace>) -> Unit,
    onSelectFrom: (TransitPlace) -> Unit,
    onSelectTo: (TransitPlace) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    time: String,
    onTimeChange: (String) -> Unit,
    arriveBy: Boolean,
    onArriveByChange: (Boolean) -> Unit,
    connections: List<TransitConnection>,
    onConnections: (List<TransitConnection>) -> Unit,
    searchLoading: Boolean,
    onSearchLoading: (Boolean) -> Unit,
    connectionLoading: Boolean,
    onConnectionLoading: (Boolean) -> Unit,
    searchError: String?,
    onSearchError: (String?) -> Unit,
    searchInfo: String,
    onSearchInfo: (String) -> Unit,
    monitored: List<Connection>,
    onMonitor: (Connection) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
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
                onValueChange = onStartChange,
                label = { Text("Startbahnhof oder Haltestelle") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = destination,
                onValueChange = onDestinationChange,
                label = { Text("Zielbahnhof oder Haltestelle") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    scope.launch {
                        onSearchLoading(true)
                        onSearchError(null)
                        onSearchInfo("")
                        onConnections(emptyList())

                        try {
                            onFromPlaces(TransitousClient.searchPlaces(start))
                            onToPlaces(TransitousClient.searchPlaces(destination))
                            onSearchInfo("Bitte wähle die passenden Haltestellen aus.")
                        } catch (e: Exception) {
                            onSearchError(
                                e.message ?: "Haltestellensuche fehlgeschlagen."
                            )
                        } finally {
                            onSearchLoading(false)
                        }
                    }
                },
                enabled = !searchLoading &&
                    start.isNotBlank() &&
                    destination.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (searchLoading) {
                        "Haltestellen werden gesucht ..."
                    } else {
                        "Haltestellen suchen"
                    }
                )
            }
        }

        if (fromPlaces.isNotEmpty()) {
            item {
                Text("Start auswählen", color = TextMain, fontWeight = FontWeight.Bold)
            }

            items(fromPlaces.take(8)) { place ->
                PlaceCard(place, selectedFrom?.id == place.id) {
                    onSelectFrom(place)
                }
            }
        }

        if (toPlaces.isNotEmpty()) {
            item {
                Text("Ziel auswählen", color = TextMain, fontWeight = FontWeight.Bold)
            }

            items(toPlaces.take(8)) { place ->
                PlaceCard(place, selectedTo?.id == place.id) {
                    onSelectTo(place)
                }
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
                    onValueChange = onDateChange,
                    label = { Text("Datum (JJJJ-MM-TT)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = onTimeChange,
                    label = { Text("Uhrzeit (HH:MM)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !arriveBy,
                        onClick = { onArriveByChange(false) },
                        label = { Text("Abfahrt") }
                    )

                    FilterChip(
                        selected = arriveBy,
                        onClick = { onArriveByChange(true) },
                        label = { Text("Ankunft") }
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            onConnectionLoading(true)
                            onSearchError(null)
                            onSearchInfo("")
                            onConnections(emptyList())

                            try {
                                val dateTime = LocalDateTime.of(
                                    LocalDate.parse(date),
                                    LocalTime.parse(time)
                                ).atZone(ZoneId.systemDefault())
                                    .toOffsetDateTime()
                                    .toString()

                                val found = TransitousClient.searchConnections(
                                    selectedFrom,
                                    selectedTo,
                                    dateTime,
                                    arriveBy
                                )

                                onConnections(found)

                                onSearchInfo(
                                    if (found.isEmpty()) {
                                        "Keine Verbindungen gefunden."
                                    } else {
                                        "${found.size} Verbindungen gefunden."
                                    }
                                )
                            } catch (e: Exception) {
                                onSearchError(
                                    e.message ?: "Verbindungssuche fehlgeschlagen."
                                )
                            } finally {
                                onConnectionLoading(false)
                            }
                        }
                    },
                    enabled = !connectionLoading,
                    modifier = Modifier
