package com.example.weatherjournalapp

import android.os.Parcelable
import com.google.firebase.database.Exclude
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// cetakan data buat jurnal lokasi kita
@Parcelize // bikin class ini bisa dikirim lewat intent
data class FavoriteCity(
    @get:Exclude // bikin firebaseId ga ikut kesimpen di database
    var firebaseId: String = "", // buat nyimpen key unik dari firebase

    // data jurnalnya
    val name: String = "",
    val note: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L // buat nyimpen waktu pas jurnal dibuat
) : Parcelable { // nandain kalo class ini bisa dikirim
    // firebase butuh constructor kosong (otomatis dibuat kotlin kalo ada default value)

    // fungsi bantu buat ubah angka timestamp jadi teks tanggal yg gampang dibaca
    @Exclude // biar fungsi ini ga ikut kesimpen di firebase
    fun getFormattedTimestamp(): String {
        // formatnya: Senin, 28 Oktober 2025 10:30
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm", Locale("id", "ID")) // pake format indonesia
        val date = Date(timestamp) // ubah angka milidetik jadi objek tanggal
        return sdf.format(date) // ubah objek tanggal jadi teks sesuai format
    }
}