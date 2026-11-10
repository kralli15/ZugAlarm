
package de.zugalarm.app

import com.google.transit.realtime.GtfsRealtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class RealtimeMessage(
    val title: String,
    val description: String,
    val affected: String
)

data class RealtimeResult(
    val messages: List<RealtimeMessage>,
    val feedTime: String,
    val entityCount: Int
)

object GtfsRealtimeClient {
    private const val FEED_URL = "https://realtime.gtfs.de/realtime-free.pb"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun load(): RealtimeResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(FEED_URL)
            .header("User-Agent", "ZugAlarm Android App")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Datenquelle antwortet mit HTTP ${response.code}"
                )
            }

            val body = response.body
                ?: throw IllegalStateException("Keine Daten erhalten.")

            val feed = GtfsRealtime.FeedMessage.parseFrom(body.byteStream())
            val formatter = SimpleDateFormat(
                "dd.MM.yyyy, HH:mm:ss",
                Locale.GERMANY
            )

            val feedTime = if (feed.header.hasTimestamp()) {
                formatter.format(Date(feed.header.timestamp * 1000L)) + " Uhr"
            } else {
                "Zeitstempel fehlt"
            }

            val messages = feed.entityList.mapNotNull { entity ->
                if (!entity.hasAlert()) return@mapNotNull null

                val alert = entity.alert

                val title = alert.headerText.translationList
                    .firstOrNull { it.language == "de" }?.text
                    ?: alert.headerText.translationList.firstOrNull()?.text
                    ?: "Verkehrsmeldung"

                val description = if (alert.hasDescriptionText()) {
                    alert.descriptionText.translationList
                        .firstOrNull { it.language == "de" }?.text
                        ?: alert.descriptionText.translationList.firstOrNull()?.text
                        ?: ""
                } else {
                    ""
                }

                val affected = alert.informedEntityList
                    .mapNotNull { informed ->
                        when {
                            informed.hasRouteId() ->
                                "Linie ${informed.routeId}"
                            informed.hasTrip() && informed.trip.hasTripId() ->
                                "Fahrt ${informed.trip.tripId}"
                            informed.hasStopId() ->
                                "Haltestelle ${informed.stopId}"
                            else -> null
                        }
                    }
                    .distinct()
                    .take(4)
                    .joinToString(", ")
                    .ifBlank { "Betroffener Bereich nicht angegeben" }

                RealtimeMessage(title, description, affected)
            }

            RealtimeResult(messages, feedTime, feed.entityCount)
        }
    }
}
