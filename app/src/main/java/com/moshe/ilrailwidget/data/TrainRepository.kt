package com.moshe.ilrailwidget.data

import com.moshe.ilrailwidget.api.ILRailService
import com.moshe.ilrailwidget.api.Station
import com.moshe.ilrailwidget.api.Travel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class TrainRepository {
    private val api: ILRailService = createApiService()
    private val _stations = MutableStateFlow<List<Station>>(emptyList())
    val stations: StateFlow<List<Station>> = _stations

    private fun createApiService(): ILRailService {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://mobile.rail.co.il/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ILRailService::class.java)
    }

    suspend fun loadStations() {
        try {
            val response = api.getStations()
            _stations.value = response.Data.Stations.sortedBy { it.English }
        } catch (e: Exception) {
            e.printStackTrace()
            _stations.value = emptyList()
        }
    }

    suspend fun getNextTrain(fromStation: String, toStation: String): Travel? {
        return try {
            val now = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            val hourFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            
            val response = api.getTrainSchedule(
                fromStation,
                toStation,
                now.format(dateFormatter),
                now.format(hourFormatter)
            )

            response.Data.Travels
                .filter { travel ->
                    !travel.IsCanceled && 
                    parseDateTimeFromApi(travel.DepartureTime).isAfter(now)
                }
                .minByOrNull { travel ->
                    parseDateTimeFromApi(travel.DepartureTime)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseDateTimeFromApi(dateTimeStr: String): LocalDateTime {
        // API returns date in format "202504201102" (YYYYMMDDHHmm)
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        return LocalDateTime.parse(dateTimeStr, formatter)
    }

    companion object {
        @Volatile
        private var instance: TrainRepository? = null

        fun getInstance(): TrainRepository {
            return instance ?: synchronized(this) {
                instance ?: TrainRepository().also { instance = it }
            }
        }
    }
}
