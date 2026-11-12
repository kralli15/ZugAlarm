package de.zugalarm.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TransitPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class TransitLeg(
    val line: String,
    val mode: String,
    val from: String,
    val to: String,
    val departure: String,
    val arrival: String,
    val scheduledDeparture: String,
    val scheduledArrival: String,
    val departurePlatform: String,
    val arrivalPlatform: String,
    val realtimeAvailable: Boolean,
    val cancelled: Boolean,
    val tripId: String,
    val routeId: String,
    val headsign: String
)

data class TransitConnection(
    val label: String,
    val from: String,
    val to: String,
    val departure: String,
    val arrival: String,
    val duration: String,
    val detail: String,
    val legs: List<TransitLeg> = emptyList()
)

object TransitousClient {

    private const val USER_AGENT =
        "ZugAlarm/0.4.0 (https://github.com/kralli15/ZugAlarm)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun searchPlaces(text: String): List<TransitPlace> =
        withContext(Dispatchers.IO) {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("api.transitous.org")
                .addPathSegments("api/v1/geocode")
                .addQueryParameter("text", text)
                .addQueryParameter("language", "de")
                .addQueryParameter("type", "STOP")
                .build()

            val array = JSONArray(get(url.toString()))

            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index)
                    ?: return@mapNotNull null

                val id = item.optString("id")
                val name = item.optString("name")

                if (id.isBlank() || name.isBlank()) {
                    return@mapNotNull null
                }

                TransitPlace(
                    id = id,
                    name = name,
                    latitude = item.optDouble("lat", 0.0),
                    longitude = item.optDouble("lon", 0.0)
                )
            }
        }

    suspend fun searchConnections(
        from: TransitPlace,
        to: TransitPlace,
        dateTime: String?,
        arriveBy: Boolean,
        onlyLocalTransport: Boolean = false
    ): List<TransitConnection> = withContext(Dispatchers.IO) {

        val builder = HttpUrl.Builder()
            .scheme("https")
            .host("api.transitous.org")
            .addPathSegments("api/v6/plan")
            .addQueryParameter("fromPlace", from.id)
            .addQueryParameter("toPlace", to.id)
            .addQueryParameter("numItineraries", "8")
            .addQueryParameter("language", "de")
            .addQueryParameter("arriveBy", arriveBy.toString())
            .addQueryParameter("joinInterlinedLegs", "false")
            .addQueryParameter("realtimeMode", "REALTIME")

        if (!dateTime.isNullOrBlank()) {
            builder.addQueryParameter("time", dateTime)
        }

        val root = JSONObject(get(builder.build().toString()))
        val itineraries = root.optJSONArray("itineraries") ?: JSONArray()

        (0 until itineraries.length()).mapNotNull { index ->
            val journey = itineraries.optJSONObject(index)
                ?: return@mapNotNull null

            val rawLegs = journey.optJSONArray("legs") ?: JSONArray()

            if (rawLegs.length() == 0) {
                return@mapNotNull null
            }

            val first = rawLegs.optJSONObject(0) ?: JSONObject()
            val last = rawLegs.optJSONObject(rawLegs.length() - 1)
                ?: return@mapNotNull null

            val firstPlace = first.optJSONObject("from") ?: JSONObject()
            val lastPlace = last.optJSONObject("to") ?: JSONObject()

            val legs = (0 until rawLegs.length()).mapNotNull { legIndex ->
                val leg = rawLegs.optJSONObject(legIndex)
                    ?: return@mapNotNull null

                val mode = leg.optString("mode")

                if (
                    mode.equals("WALK", true) ||
                    mode.equals("FOOT", true) ||
                    mode.isBlank()
                ) {
                    return@mapNotNull null
                }

                val route = leg.optJSONObject("route") ?: JSONObject()
                val fromStop = leg.optJSONObject("from") ?: JSONObject()
                val toStop = leg.optJSONObject("to") ?: JSONObject()

                val line = leg.optString("displayName")
                    .takeIf { it.isNotBlank() }
                    ?: route.optString("displayName")
                        .takeIf { it.isNotBlank() }
                    ?: leg.optString("routeShortName")
                        .takeIf { it.isNotBlank() }
                    ?: mode

                TransitLeg(
                    line = line,
                    mode = mode,
                    from = fromStop.optString("name"),
                    to = toStop.optString("name"),

                    departure = formatTime(
                        leg.optString("startTime")
                            .ifBlank { fromStop.optString("departure") }
                    ),

                    arrival = formatTime(
                        leg.optString("endTime")
                            .ifBlank { toStop.optString("arrival") }
                    ),

                    scheduledDeparture = formatTime(
                        leg.optString("scheduledStartTime")
                            .ifBlank {
                                fromStop.optString("scheduledDeparture")
                            }
                    ),

                    scheduledArrival = formatTime(
                        leg.optString("scheduledEndTime")
                            .ifBlank {
                                toStop.optString("scheduledArrival")
                            }
                    ),

                    departurePlatform = platformName(fromStop),
                    arrivalPlatform = platformName(toStop),

                    realtimeAvailable = leg.optBoolean("realTime", false),

                    cancelled = leg.optBoolean("cancelled", false) ||
                        leg.optBoolean("tripCancelled", false),

                    tripId = leg.optString("tripId"),
                    routeId = leg.optString("routeId"),
                    headsign = leg.optString("headsign")
                )
            }

            if (legs.isEmpty()) {
                return@mapNotNull null
            }

            if (
                onlyLocalTransport &&
                legs.any { isKnownLongDistance(it.mode) }
            ) {
                return@mapNotNull null
            }

            val durationSeconds = journey.optLong("duration", 0L)

            val durationText = if (durationSeconds > 0L) {
                val minutes = durationSeconds / 60L
                "${minutes / 60} Std. ${minutes % 60} Min."
            } else {
                "Dauer nicht angegeben"
            }

            val label = legs.joinToString(" → ") { it.line }
                .ifBlank { "Öffentliche Verbindung" }

            TransitConnection(
                label = label,
                from = firstPlace.optString("name").ifBlank { from.name },
                to = lastPlace.optString("name").ifBlank { to.name },
                departure = formatTime(
                    journey.optString("startTime")
                        .ifBlank { first.optString("startTime") }
                ),
                arrival = formatTime(
                    journey.optString("endTime")
                        .ifBlank { last.optString("endTime") }
                ),
                duration = durationText,
                detail = label,
                legs = legs
            )
        }
    }

    private fun platformName(stop: JSONObject): String {
        val currentTrack = stop.optString("track")
            .takeIf { it.isNotBlank() && it != "null" }

        val scheduledTrack = stop.optString("scheduledTrack")
            .takeIf { it.isNotBlank() && it != "null" }

        return currentTrack ?: scheduledTrack ?: ""
    }

    private fun isKnownLongDistance(mode: String): Boolean {
        val normalized = mode.uppercase(Locale.ROOT)

        return normalized in setOf(
            "LONG_DISTANCE",
            "HIGH_SPEED_RAIL",
            "INTERCITY",
            "INTERCITY_EXPRESS",
            "NIGHT_RAIL"
        )
    }

    private fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Transitous meldet HTTP ${response.code}. Bitte später erneut versuchen."
                )
            }

            return response.body?.string()
                ?: throw IllegalStateException(
                    "Die Datenquelle hat keine Antwort geliefert."
                )
        }
    }

    private fun formatTime(value: String): String {
        if (value.isBlank() || value == "null") {
            return "?"
        }

        return try {
            OffsetDateTime.parse(value)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(
                    DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
                )
        } catch (_: Exception) {
            try {
                Instant.parse(value)
                    .atZone(ZoneId.systemDefault())
                    .format(
                        DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
                    )
            } catch (_: Exception) {
                "?"
            }
        }
    }
}
