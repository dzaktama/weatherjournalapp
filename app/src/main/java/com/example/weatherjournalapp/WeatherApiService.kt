package com.example.weatherjournalapp

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// interface buat ngomong sama api openweather
interface WeatherApiService {

    // fungsi buat dapetin cuaca berdasarkan nama kota
    @GET("weather")
    fun getWeatherByCity(
        @Query("q") cityName: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric" // biar dapet celcius
    ): Call<WeatherResponse>

    // fungsi buat dapetin cuaca berdasarkan gps (lat/lon)
    @GET("weather")
    fun getWeatherByCoordinates(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Call<WeatherResponse>
}

// objek biar gampang manggil retrofitnya
object ApiClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    val instance: WeatherApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(WeatherApiService::class.java)
    }
}