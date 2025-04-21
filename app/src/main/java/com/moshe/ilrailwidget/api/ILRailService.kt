package com.moshe.ilrailwidget.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ILRailService {
    @GET("api/v1/rail/timetable.json")
    suspend fun getTrainSchedule(
        @Query("OId") fromStation: String,
        @Query("TId") toStation: String,
        @Query("Date") date: String,
        @Query("Hour") hour: String
    ): ScheduleResponse

    @GET("api/v1/rail/stations.json")
    suspend fun getStations(): StationsResponse
}

data class StationsResponse(
    val Data: StationsData
)

data class StationsData(
    val Stations: List<Station>
)

data class Station(
    val Id: String,
    val Hebrew: String,
    val English: String
)

data class ScheduleResponse(
    val Data: TrainData
)

data class TrainData(
    val Travels: List<Travel>
)

data class Travel(
    val DepartureTime: String,
    val ArrivalTime: String,
    val Platform: String,
    val TrainNumber: String,
    val IsFullTrain: Boolean,
    val IsCanceled: Boolean
)
