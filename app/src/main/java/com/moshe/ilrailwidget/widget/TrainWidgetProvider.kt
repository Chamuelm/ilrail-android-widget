package com.moshe.ilrailwidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.moshe.ilrailwidget.R
import com.moshe.ilrailwidget.data.TrainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TrainWidgetProvider : AppWidgetProvider() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val repository = TrainRepository.getInstance()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromStationId = prefs.getString(getFromStationKey(appWidgetId), "") ?: ""
        val toStationId = prefs.getString(getToStationKey(appWidgetId), "") ?: ""

        if (fromStationId.isEmpty() || toStationId.isEmpty()) {
            return
        }

        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        views.setTextViewText(R.id.time_text, context.getString(R.string.loading))

        // Update widget initially with loading state
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Fetch data
        scope.launch {
            try {
                // Get stations for names
                repository.loadStations()
                val stations = repository.stations.first()
                val fromStation = stations.find { it.Id == fromStationId }
                val toStation = stations.find { it.Id == toStationId }

                if (fromStation != null && toStation != null) {
                    views.setTextViewText(
                        R.id.route_text,
                        "${fromStation.English} → ${toStation.English}"
                    )

                    val train = repository.getNextTrain(fromStationId, toStationId)
                    val timeText = if (train != null) {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                        val departureTime = LocalDateTime.parse(train.DepartureTime, formatter)
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        
                        if (train.IsFullTrain) {
                            context.getString(
                                R.string.train_time_format_full,
                                departureTime.format(timeFormatter),
                                train.Platform
                            )
                        } else {
                            context.getString(
                                R.string.train_time_format,
                                departureTime.format(timeFormatter),
                                train.Platform
                            )
                        }
                    } else {
                        context.getString(R.string.no_trains)
                    }
                    
                    views.setTextViewText(R.id.time_text, timeText)
                } else {
                    views.setTextViewText(R.id.time_text, context.getString(R.string.no_trains))
                }
                
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
                views.setTextViewText(R.id.time_text, context.getString(R.string.no_trains))
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "com.moshe.ilrailwidget.widget"

        fun getFromStationKey(appWidgetId: Int) = "from_station_$appWidgetId"
        fun getToStationKey(appWidgetId: Int) = "to_station_$appWidgetId"

        fun saveWidgetSettings(
            context: Context,
            appWidgetId: Int,
            fromStation: String,
            toStation: String
        ) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(getFromStationKey(appWidgetId), fromStation)
                .putString(getToStationKey(appWidgetId), toStation)
                .apply()
        }
    }
}
