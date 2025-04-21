package com.moshe.ilrailwidget.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.moshe.ilrailwidget.R
import com.moshe.ilrailwidget.api.Station
import com.moshe.ilrailwidget.data.TrainRepository
import com.moshe.ilrailwidget.widget.TrainWidgetProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WidgetConfigActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val repository = TrainRepository.getInstance()
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var saveButton: Button
    private var stations: List<Station> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Initialize views
        fromSpinner = findViewById(R.id.spinner_from_station)
        toSpinner = findViewById(R.id.spinner_to_station)
        progressBar = findViewById(R.id.progress_bar)
        saveButton = findViewById(R.id.button_save)

        // Load stations
        lifecycleScope.launch {
            repository.loadStations()
            repository.stations.collectLatest { stationList ->
                stations = stationList
                if (stations.isNotEmpty()) {
                    setupSpinners()
                    progressBar.visibility = View.GONE
                    saveButton.isEnabled = true
                }
            }
        }

        // Set up save button
        saveButton.isEnabled = false
        saveButton.setOnClickListener {
            val fromStation = stations[fromSpinner.selectedItemPosition]
            val toStation = stations[toSpinner.selectedItemPosition]

            // Save settings
            TrainWidgetProvider.saveWidgetSettings(
                this,
                appWidgetId,
                fromStation.Id,
                toStation.Id
            )

            // Update widget
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val provider = TrainWidgetProvider()
            provider.onUpdate(this, appWidgetManager, intArrayOf(appWidgetId))

            // Set result and finish
            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    private fun setupSpinners() {
        val stationNames = stations.map { it.English }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stationNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        fromSpinner.adapter = adapter
        toSpinner.adapter = adapter.clone() as ArrayAdapter<String>
    }
}
