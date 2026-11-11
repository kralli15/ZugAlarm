
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
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TransitPlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class TransitConnection(
    val label: String,
    val from: String,
    val to: String,
    val departure: String,
    val arrival: String,
    val duration: String,
    val detail: String
)

object TransitousClient {
    private const val USER_AGENT =
        "ZugAlarm/0.3.0 (https://github.com/kralli15/ZugAlarm)"

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
        arriveBy: Boolean
    ): List<TransitConnection> = withContext(Dispatchers.IO) {

        val builder = HttpUrl.Builder()
            .scheme("https")
            .host("api.transitous.org")
            .addPathSegments("api/v6/plan")
            .addQueryParameter("fromPlace", from.id)
            .addQueryParameter("toPlace", to.id)
            .addQueryParameter("numItineraries", "6")
            .addQueryParameter("language", "de")
            .addQueryParameter("arriveBy", arriveBy.toString())
            .addQueryParameter("joinInterlinedLegs", "false")

        if (!dateTime.isNullOrBlank()) {
            builder.addQueryParameter("time", dateTime)
        }

        val root = JSONObject(get(builder.build().toString()))
        val itineraries = root.optJSONArray("itineraries")
            ?: JSONArray()

        (0 until itineraries.length()).mapNotNull { index ->
            val journey = itineraries.optJSONObject(index)
                ?: return@mapNotNull null

            val legs = journey.optJSONArray("legs")
                ?: JSONArray()

            if (legs.length() == 0) {
                return@mapNotNull null
            }

            val first = legs.optJSONObject(0) ?: JSONObject()
            val last = legs.optJSONObject(legs.length() - 1)
                ?: JSONObject()

            val firstPlace = first.optJSONObject("from")
                ?: JSONObject()
            val lastPlace = last.optJSONObject("to")
                ?: JSONObject()

            val departure = formatTime(first.optString("departure"))
            val arrival = formatTime(last.optString("arrival"))

            val durationSeconds = journey.optLong("duration", 0L)
            val durationText = if (durationSeconds > 0L) {
                val minutes = durationSeconds / 60L
                "\${minutes / 60} Std. \${minutes % 60} Min."
            } else {
                "Dauer nicht angegeben"
            }

            val modes = (0 until legs.length()).mapNotNull { legIndex ->
                val leg = legs.optJSONObject(legIndex)
                    ?: return@mapNotNull null

                val mode = leg.optString("mode")
                if (mode == "WALK" || mode.isBlank()) {
                    null
                } else {
                    leg.optJSONObject("route")
                        ?.optString("displayName")
                        ?.takeIf { it.isNotBlank() }
                        ?: leg.optString("displayName")
                            .takeIf { it.isNotBlank() }
                        ?: mode
                }
            }.distinct()

            TransitConnection(
                label = modes.joinToString(" · ")
                    .ifBlank { "Öffentliche Verbindung" },
                from = firstPlace.optString("name")
                    .ifBlank { from.name },
                to = lastPlace.optString("name")
                    .ifBlank { to.name },
                departure = departure,
                arrival = arrival,
                duration = durationText,
                detail = modes.joinToString(" · ")
            )
        }
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
                    "Transitous meldet HTTP \${response.code}. Bitte später erneut versuchen."
                )
            }

            return response.body?.string()
                ?: throw IllegalStateException(
                    "Die Datenquelle hat keine Antwort geliefert."
                )
        }
    }

    private fun formatTime(value: String): String {
        if (value.isBlank()) return "?"

        return try {
            OffsetDateTime.parse(value).format(
                DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
            )
        } catch (_: Exception) {
            try {
                Instant.parse(value)
                    .atZone(java.time.ZoneId.systemDefault())
                    .format(
                        DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
                    )
            } catch (_: Exception) {
                value
            }
        }
    }
}
