package com.example.weatherjournalapp

import android.os.Parcelable
import com.google.firebase.database.Exclude // IMPORT INI
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// data class buat nampung data cuaca dari api
data class WeatherResponse(
    @SerializedName("weather") val weatherInfo: List<WeatherInfo>,
    @SerializedName("main") val mainInfo: MainInfo,
    @SerializedName("name") val cityName: String
)

data class WeatherInfo(
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String
)

data class MainInfo(
    @SerializedName("temp") val temp: Double,
    @SerializedName("humidity") val humidity: Int
)


// cetakan data buat jurnal favorit kita
@Parcelize
data class FavoriteCity(
    @get:Exclude // bikin firebaseId gak ikut kesimpen di database
    var firebaseId: String = "", // buat nyimpen key unik dari firebase

    val name: String = "",
    val temperature: String = "",
    val note: String = "",
    val condition: String = "", // kita simpen 'Clouds' atau 'Rain', bukan id icon
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Parcelable {
    // firebase butuh constructor kosong
    constructor() : this("", "", "", "", "", 0.0, 0.0)

    // fungsi tambahan buat nentuin icon
    @Exclude
    fun getWeatherIcon(): Int {
        return when (condition.lowercase()) {
            "clouds" -> R.drawable.ic_cloud
            "rain", "drizzle", "thunderstorm" -> R.drawable.ic_rain
            "clear" -> R.drawable.ic_sun
            "snow" -> R.drawable.ic_snow
            else -> R.drawable.ic_default
        }
    }
}