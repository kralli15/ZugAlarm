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
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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
private val SevMagenta = Color(0xFF8A1B59)
private val BusOrange = Color(0xFFF5A623)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZugAlarmApp()
        }
    }
}

@Composable
fun ZugAlarmApp() {
    val keyboardController = LocalSoftwareKeyboardController.current

    var fromText by remember {
        mutableStateOf(TextFieldValue(""))
    }
    var toText by remember {
        mutableStateOf(TextFieldValue(""))
    }

    var fromPlace by remember {
        mutableStateOf<TransitPlace?>(null)
    }
    var toPlace by remember {
        mutableStateOf<TransitPlace?>(null)
    }

    var fromSuggestions by remember {
        mutableStateOf(emptyList<TransitPlace>())
    }
    var toSuggestions by remember {
        mutableStateOf(emptyList<TransitPlace>())
    }

    var connections by remember {
        mutableStateOf(emptyList<TransitConnection>())
    }
    var selected by remember {
        mutableStateOf<TransitConnection?>(null)
    }

    var searching by remember {
        mutableStateOf(false)
    }
    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(fromText.text, fromPlace) {
        if (fromPlace != null) {
            fromSuggestions = emptyList()
        } else {
            val query = fromText.text.trim()
            if (query.length < 2) {
                fromSuggestions = emptyList()
            } else {
                delay(400)
                fromSuggestions = try {
                    rankStations(
                        TransitousClient.searchPlaces(query),
                        query
                    ).take(8)
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    LaunchedEffect(toText.text, toPlace) {
        if (toPlace != null) {
            toSuggestions = emptyList()
        } else {
            val query = toText.text.trim()
            if (query.length < 2) {
                toSuggestions = emptyList()
            } else {
                delay(400)
                toSuggestions = try {
                    rankStations(
                        TransitousClient.searchPlaces(query),
                        query
                    ).take(8)
                } catch (_: Exception) {
                    emptyList()
                }
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
                    connections =
                        TransitousClient.searchConnections(
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
                error = e.message
                    ?: "Verbindungssuche fehlgeschlagen."
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
                                val name = cleanStation(it.name)
                                fromPlace = it
                                fromText = TextFieldValue(
                                    text = name,
                                    selection = TextRange(name.length)
                                )
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
                                val name = cleanStation(it.name)
                                toPlace = it
                                toText = TextFieldValue(
                                    text = name,
                                    selection = TextRange(name.length)
                                )
                                toSuggestions = emptyList()
                            }
                        )

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                fromSuggestions = emptyList()
                                toSuggestions = emptyList()
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
                            "Der angezeigte Abfahrtsort entspricht den Angaben der Fahrplandaten. Eine GPS-Position des Zuges wird nicht geliefert.",
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
    @Composable
private fun StationInput(
    label: String,
    value: TextFieldValue,
    suggestions: List<TransitPlace>,
    onValueChange: (TextFieldValue) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            connection.legs.forEachIndexed { index, leg ->
                if (isReplacementBus(leg)) {
                    SevBadge(leg.line)
                } else {
                    Text(
                        leg.line,
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                PreviewTimeRow(
                    label = "Ab",
                    station = leg.from,
                    planned = leg.scheduledDeparture,
                    actual = leg.departure,
                    platform = leg.departurePlatform
                )

                PreviewTimeRow(
                    label = "An",
                    station = leg.to,
                    planned = leg.scheduledArrival,
                    actual = leg.arrival,
                    platform = leg.arrivalPlatform
                )

                if (index < connection.legs.lastIndex) {
                    Divider(
                        color = TextMuted.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SevBadge(line: String) {
    val serviceName = line
        .replace("Schienenersatzverkehr", "", ignoreCase = true)
        .replace("SEV", "", ignoreCase = true)
        .trim()
        .ifBlank { "Bus" }
        .let {
            if (it.startsWith("Bus", true)) it else "Bus $it"
        }

    Row(
        modifier = Modifier
            .background(
                SevMagenta,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            Icons.Default.Train,
            contentDescription = "Zug",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )

        Text(
            "→",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .size(23.dp)
                .background(BusOrange, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DirectionsBus,
                contentDescription = "Bus",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(
            Modifier
                .width(1.dp)
                .height(19.dp)
                .background(Color.White.copy(alpha = 0.8f))
        )

        Text(
            serviceName,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PreviewTimeRow(
    label: String,
    station: String,
    planned: String,
    actual: String,
    platform: String
) {
    val delay = delayMinutes(planned, actual)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.width(22.dp)
        )

        Column(Modifier.weight(1f)) {
            Text(
                cleanStation(station),
                color = TextMain,
                fontSize = 12.sp,
                maxLines = 2
            )

            if (platform.isNotBlank()) {
                Text(
                    if (isBusPlatform(platform)) {
                        "Busstg. ${platform.removePrefix("Busstg.").trim()}"
                    } else {
                        "Gleis $platform"
                    },
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }

        Text(
            planned.ifBlank { "?" },
            color = TextMuted,
            fontSize = 11.sp
        )

        Spacer(Modifier.width(7.dp))

        Text(
            actual.ifBlank { "?" },
            color = when {
                delay == null -> TextMain
                delay > 0 -> Bad
                delay < 0 -> Good
                else -> Good
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        if (delay != null && delay != 0) {
            Spacer(Modifier.width(5.dp))
            Text(
                if (delay > 0) "+${delay}′" else "${delay}′",
                color = if (delay > 0) Bad else Good,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (delay == 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                "✓",
                color = Good,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun isBusPlatform(value: String): Boolean {
    return value.contains("bus", ignoreCase = true) ||
        value.contains("busstg", ignoreCase = true) ||
        value.contains("bussteig", ignoreCase = true)
}

private fun isReplacementBus(leg: TransitLeg): Boolean {
    if (!leg.mode.equals("BUS", ignoreCase = true)) {
        return false
    }

    val details = listOf(
        leg.line,
        leg.headsign,
        leg.routeId
    ).joinToString(" ").lowercase()

    return details.contains("sev") ||
        details.contains("schienenersatz") ||
        details.contains("replacement bus") ||
        details.contains("ersatzverkehr")
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
            if (isReplacementBus(leg)) {
                SevBadge(leg.line)
            } else {
                Text(
                    leg.line,
                    color = Accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                when {
                    leg.cancelled -> "FÄLLT AUS"
                    leg.realtimeAvailable -> "ECHTZEIT"
                    else -> "FAHRPLAN"
                },
                color = when {
                    leg.cancelled -> Bad
                    leg.realtimeAvailable -> Good
                    else -> TextMuted
                },
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
    val delay = delayMinutes(planned, actual)

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
                    color = when {
                        delay == null -> TextMain
                        delay > 0 -> Bad
                        else -> Good
                    },
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
                    if (isBusPlatform(platform)) {
                        "Busstg. ${platform.removePrefix("Busstg.").trim()}"
                    } else {
                        platform
                    },
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

private fun delayMinutes(
    planned: String,
    actual: String
): Int? {
    if (
        planned.isBlank() ||
        actual.isBlank() ||
        planned == "?" ||
        actual == "?"
    ) return null

    return try {
        val p = planned.split(":")
        val a = actual.split(":")
        if (p.size != 2 || a.size != 2) return null

        val plannedMinutes = p[0].toInt() * 60 + p[1].toInt()
        val actualMinutes = a[0].toInt() * 60 + a[1].toInt()

        var difference = actualMinutes - plannedMinutes
        if (difference < -720) difference += 1440
        if (difference > 720) difference -= 1440
        difference
    } catch (_: Exception) {
        null
    }
}

private fun cleanStation(name: String): String {
    return name.replace("Hbf.", "Hbf").trim()
}

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
            val name = place.name.trim().lowercase()
            name != normalizedQuery &&
                name != normalizedQuery.removeSuffix(" hbf")
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
}

