
package de.zugalarm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Background = Color(0xFF080D16)
private val Panel = Color(0xFF111B2A)
private val TextMain = Color(0xFFF2F5FA)
private val TextMuted = Color(0xFF9BA9BC)
private val Accent = Color(0xFF3988D8)
private val Good = Color(0xFF59C58A)
private val Bad = Color(0xFFFF7777)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZugAlarmApp() }
    }
}

@Composable
fun ZugAlarmApp() {
    var fromText by remember { mutableStateOf("") }
    var toText by remember { mutableStateOf("") }
    var fromPlace by remember { mutableStateOf<TransitPlace?>(null) }
    var toPlace by remember { mutableStateOf<TransitPlace?>(null) }
    var fromSuggestions by remember { mutableStateOf(emptyList<TransitPlace>()) }
    var toSuggestions by remember { mutableStateOf(emptyList<TransitPlace>()) }
    var connections by remember { mutableStateOf(emptyList<TransitConnection>()) }
    var selected by remember { mutableStateOf<TransitConnection?>(null) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fromText) {
        if (fromText.trim().length < 2) {
            fromSuggestions = emptyList()
        } else {
            delay(350)
            fromSuggestions = try {
                rankStations(
                    TransitousClient.searchPlaces(fromText.trim()),
                    fromText.trim()
                ).take(8)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    LaunchedEffect(toText) {
        if (toText.trim().length < 2) {
            toSuggestions = emptyList()
        } else {
            delay(350)
            toSuggestions = try {
                rankStations(
                    TransitousClient.searchPlaces(toText.trim()),
                    toText.trim()
                ).take(8)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    LaunchedEffect(searching) {
        if (searching) {
            try {
                val start = fromPlace
                val end = toPlace

                if (start == null || end == null) {
                    error = "Bitte Start und Ziel auswählen."
                } else {
                    connections = TransitousClient.searchConnections(
                        from = start,
                        to = end,
                        dateTime = null,
                        arriveBy = false
                    )

                    if (connections.isEmpty()) {
                        error = "Keine Verbindung gefunden."
                    }
                }
            } catch (e: Exception) {
                error = e.message ?: "Verbindungssuche fehlgeschlagen."
            } finally {
                searching = false
            }
        }
    }

    BackHandler(enabled = selected != null) {
        selected = null
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Background,
            surface = Panel,
            onSurface = TextMain
        )
    ) {
        Scaffold(
            containerColor = Background,
            topBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Text(
                        "ZugAlarm",
                        color = TextMain,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Dein persönlicher Streckenagent",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 8.dp,
                    bottom = 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Panel, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            "VERBINDUNG SUCHEN",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        StationInput(
                            label = "Von",
                            value = fromText,
                            suggestions = fromSuggestions,
                            onValueChange = {
                                fromText = it
                                fromPlace = null
                            },
                            onSelect = {
                                fromPlace = it
                                fromText = cleanStation(it.name)
                                fromSuggestions = emptyList()
                            }
                        )

                        StationInput(
                            label = "Nach",
                            value = toText,
                            suggestions = toSuggestions,
                            onValueChange = {
                                toText = it
                                toPlace = null
                            },
                            onSelect = {
                                toPlace = it
                                toText = cleanStation(it.name)
                                toSuggestions = emptyList()
                            }
                        )

                        Button(
                            onClick = {
                                connections = emptyList()
                                selected = null
                                error = null
                                searching = true
                            },
                            enabled = fromPlace != null &&
                                toPlace != null &&
                                !searching,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (searching) "Suche läuft …"
                                else "Verbindungen suchen"
                            )
                        }
                    }
                }

                if (searching) {
                    item {
                        Text(
                            "Verbindungen werden gesucht …",
                            color = TextMuted
                        )
                    }
                }

                if (error != null) {
                    item {
                        Text(
                            error!!,
                            color = Bad,
                            fontSize = 13.sp
                        )
                    }
                }

                if (connections.isNotEmpty() && selected == null) {
                    item {
                        Text(
                            "GEFUNDENE VERBINDUNGEN",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(connections) { connection ->
                        ConnectionCard(connection) {
                            selected = connection
                        }
                    }
                }

                selected?.let { connection ->
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DEINE REISE",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            TextButton(
                                onClick = { selected = null }
                            ) {
                                Text("Zurück")
                            }
                        }

                        Text(
                            "${cleanStation(connection.from)} nach ${cleanStation(connection.to)}",
                            color = TextMain,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "${connection.departure} bis ${connection.arrival} · ${connection.duration}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    items(connection.legs) { leg ->
                        TimelineLeg(leg)
                    }

                    item {
                        Text(
                            "Dies ist der Fahrtverlauf der Verbindung. Eine GPS-Position des Zuges wird von dieser Datenquelle nicht geliefert.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                if (
                    connections.isEmpty() &&
                    selected == null &&
                    error == null &&
                    !searching
                ) {
                    item {
                        Text(
                            "Start und Ziel auswählen, um Verbindungen abzurufen.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StationInput(
    label: String,
    value: String,
    suggestions: List<TransitPlace>,
    onValueChange: (String) -> Unit,
    onSelect: (TransitPlace) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    suggestions.firstOrNull()?.let(onSelect)
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Panel,
                focusedLabelColor = Accent,
                unfocusedLabelColor = TextMuted,
                cursorColor = Accent
            )
        )

        suggestions.forEach { place ->
            TextButton(
                onClick = { onSelect(place) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 10.dp,
                    vertical = 2.dp
                )
            ) {
                Text(
                    cleanStation(place.name),
                    color = TextMain,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: TransitConnection,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Panel,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                connection.label,
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "${connection.departure} → ${connection.arrival}   ${connection.duration}",
                color = Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                "${connection.legs.size} Teilstrecken",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TimelineLeg(leg: TransitLeg) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                leg.line,
                color = Accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (leg.cancelled) "FÄLLT AUS"
                else if (leg.realtimeAvailable) "ECHTZEIT"
                else "FAHRPLAN",
                color = if (leg.cancelled) Bad
                else if (leg.realtimeAvailable) Good
                else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        StopTimelineRow(
            station = leg.from,
            planned = leg.scheduledDeparture,
            actual = leg.departure,
            platform = leg.departurePlatform,
            label = "Abfahrt",
            cancelled = leg.cancelled
        )

        Box(
            Modifier
                .padding(start = 5.dp)
                .width(2.dp)
                .height(25.dp)
                .background(Accent)
        )

        StopTimelineRow(
            station = leg.to,
            planned = leg.scheduledArrival,
            actual = leg.arrival,
            platform = leg.arrivalPlatform,
            label = "Ankunft",
            cancelled = leg.cancelled
        )

        if (leg.headsign.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Zielanzeige: ${leg.headsign}",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StopTimelineRow(
    station: String,
    planned: String,
    actual: String,
    platform: String,
    label: String,
    cancelled: Boolean
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(12.dp)
                .background(
                    if (cancelled) Bad else Accent,
                    RoundedCornerShape(50)
                )
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                cleanStation(station),
                color = TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                label,
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    planned.ifBlank { "?" },
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    actual.ifBlank { "?" },
                    color = if (planned != actual) Bad else Good,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "Plan       Echtzeit",
                color = TextMuted,
                fontSize = 9.sp
            )
        }

        Spacer(Modifier.width(9.dp))

        if (platform.isNotBlank()) {
            Surface(
                color = Color(0xFF123E70),
                shape = RoundedCornerShape(3.dp)
            ) {
                Text(
                    platform,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    )
                )
            }
        } else {
            Spacer(Modifier.width(24.dp))
        }
    }
}

private fun cleanStation(name: String): String =
    name.replace("Hbf.", "Hbf").trim()

private fun rankStations(
    places: List<TransitPlace>,
    query: String
): List<TransitPlace> {
    val normalizedQuery = query
        .trim()
        .lowercase()
        .replace("hauptbahnhof", "hbf")

    return places
        .distinctBy { it.id }
        .filter { place ->
            val name = place.name
                .trim()
                .lowercase()

            val isOnlyCityName =
                name == normalizedQuery ||
                name == normalizedQuery.removeSuffix(" hbf") ||
                name == normalizedQuery.removeSuffix(" hauptbahnhof")

            !isOnlyCityName
        }
        .sortedWith(
            compareBy<TransitPlace> {
                stationPriority(it.name)
            }.thenBy {
                it.name.lowercase()
            }
        )
}

private fun stationPriority(name: String): Int {
    val value = name.lowercase()
    return when {
        value.contains("hauptbahnhof") ||
            value.contains("hbf") -> 0

        value.contains("bahnhof") -> 1

        value.contains("re") &&
            value.contains("station") -> 2

        value.contains("s-bahn") ||
            value.contains("s bahn") -> 3

        value.contains("u-bahn") ||
            value.contains("schwebebahn") -> 4

        value.contains("bus") ||
            value.contains("straße") ||
            value.contains("str.") ||
            value.contains("platz") -> 5

        else -> 2
    }
}
