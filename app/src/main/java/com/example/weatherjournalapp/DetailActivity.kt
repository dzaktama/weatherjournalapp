package com.example.weatherjournalapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import java.util.Locale

// halaman buat nampilin detail jurnal
class DetailActivity : AppCompatActivity() {

    // tag buat logcat
    private val tag = "DetailActivity"

    // variabel buat nampung data jurnal yang diterima
    private var cityData: FavoriteCity? = null
    // variabel buat nyimpen urutan item ini di list
    private var cityPosition: Int = -1

    // variabel buat nampung komponen textview catatan
    private lateinit var tvNote: TextView

    // fungsi yang jalan pertama kali pas halaman detail dibuka
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail) // nyambungin ke layout activity_detail.xml
        Log.d(tag, "onCreate: Activity Detail Dimulai")

        // ambil data jurnal yang dikirim dari HomeActivity
        cityData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_CITY_DATA", FavoriteCity::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<FavoriteCity?>("EXTRA_CITY_DATA")
        }

        // kalo data jurnalnya gak ada, tutup halaman ini
        if (cityData == null) {
            Log.e(tag, "onCreate: Gagal mendapatkan cityData dari Intent!")
            Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            // kalo data ada, ambil juga posisi itemnya
            cityPosition = intent.getIntExtra("EXTRA_POSITION", -1)
            Log.d(tag, "onCreate: Data diterima: Lat=${cityData?.latitude}, Lon=${cityData?.longitude}, Posisi=$cityPosition")
        }


        // sambungin variabel ke komponen di layout xml
        val tvName: TextView = findViewById(R.id.tv_detail_name)
        tvNote = findViewById(R.id.tv_detail_note)
        val tvCoordsValue: TextView = findViewById(R.id.tv_detail_coords_value)
        // sambungin textview waktu
        val tvTimestamp: TextView = findViewById(R.id.tv_detail_timestamp)

        val btnEdit: Button = findViewById(R.id.btn_edit_note)
        val btnMap: Button = findViewById(R.id.btn_view_map)

        // tampilin data jurnal ke komponen layout
        tvName.text = cityData!!.name // pake !! soalnya udah pasti gak null
        tvNote.text = cityData!!.note
        // format angka koordinat
        val coordsText = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", cityData!!.latitude, cityData!!.longitude)
        tvCoordsValue.text = coordsText
        // tampilin waktu pake fungsi dari model
        tvTimestamp.text = "Dicatat pada: ${cityData!!.getFormattedTimestamp()}"
        Log.d(tag, "onCreate: Teks koordinat & timestamp diatur")


        // ngasih aksi ke tombol lihat peta
        btnMap.setOnClickListener {
            // pake ?.let biar aman kalo cityData null (meski udah dicek)
            cityData?.let { data ->
                Log.d(tag, "Tombol 'Lihat Peta' diklik. Data: Lat=${data.latitude}, Lon=${data.longitude}")
                // cek kalo koordinatnya 0.0
                if (data.latitude == 0.0 && data.longitude == 0.0) { /* ... handle koordinat 0 ... */ return@setOnClickListener }
                // bikin alamat geo: pake format q=latitude,longitude(Label)
                val geoUriString = String.format(Locale.US, "geo:0,0?q=%f,%f(Lokasi Jurnal)", data.latitude, data.longitude)
                val gmmIntentUri = Uri.parse(geoUriString)

                Log.d(tag, "Membuat URI Peta (Format Baru): $gmmIntentUri")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                // cek apa ada aplikasi peta di hp
                if (mapIntent.resolveActivity(packageManager) != null) { /* ... buka peta ... */ startActivity(mapIntent) }
                else { /* ... handle ga ada peta ... */ Toast.makeText(this, "Gak ada aplikasi peta", Toast.LENGTH_SHORT).show() }
            } ?: Log.e(tag, "btnMap onClick: cityData is null!")
        }


        // ngasih aksi ke tombol edit
        btnEdit.setOnClickListener {
            // cek dulu data sama posisinya valid gak
            if (cityData != null && cityPosition != -1) { /* ... panggil showEditNoteDialog ... */ showEditNoteDialog() }
            else { /* ... handle data ga valid ... */ Toast.makeText(this, "Data tidak valid untuk diedit", Toast.LENGTH_SHORT).show() }
        }
    } // akhir onCreate

    // fungsi buat nampilin pop-up edit catatan
    private fun showEditNoteDialog() {
        Log.d(tag, "showEditNoteDialog: Menampilkan dialog edit")
        val builder = AlertDialog.Builder(this) // siapin dialog
        builder.setTitle("Edit Catatan")

        val input = EditText(this) // bikin input teks
        input.setText(tvNote.text.toString()) // isi pake catatan lama
        builder.setView(input) // masukin input ke dialog

        // nambahin tombol 'Simpan'
        builder.setPositiveButton("Simpan") { dialog, _ ->
            val newNote = input.text.toString() // ambil teks baru
            Log.d(tag, "Dialog Edit: Tombol 'Simpan' diklik. Catatan baru: $newNote")

            cityData?.let { currentData -> // pastiin data jurnal gak null
                // bikin salinan data jurnal tapi catatannya diganti
                val updatedCity = currentData.copy(note = newNote)
                // cek posisi valid
                if (cityPosition != -1 && cityPosition < HomeActivity.favoriteCities.size) {
                    // update data di list static HomeActivity
                    HomeActivity.favoriteCities[cityPosition] = updatedCity
                    Log.d(tag, "Dialog Edit: List lokal di posisi $cityPosition diupdate")
                } else { /* ... handle posisi ga valid ... */ Log.w(tag, "Posisi $cityPosition tidak valid") }

                // update teks catatan di halaman ini
                tvNote.text = newNote
                // update data jurnal di variabel activity ini
                cityData = updatedCity
                Log.d(tag, "Dialog Edit: TextView catatan di halaman ini diupdate")

                // panggil fungsi buat nyimpen catatan baru ke firebase
                updateNoteInFirebase(newNote)

                Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
            } ?: Log.e(tag, "Dialog Edit Simpan: cityData is null!")

            dialog.dismiss() // tutup dialog
        }

        // nambahin tombol 'Batal'
        builder.setNegativeButton("Batal") { dialog, _ ->
            Log.d(tag, "Dialog Edit: Tombol 'Batal' diklik")
            dialog.cancel() // tutup dialog
        }

        builder.show() // tampilin dialog
    } // akhir showEditNoteDialog

    // fungsi buat ngirim data note baru ke firebase
    private fun updateNoteInFirebase(newNote: String) {
        val currentUser = Firebase.auth.currentUser // ambil info user
        // cek semua data valid
        if (currentUser != null && cityData != null && cityPosition >= 0 && cityPosition < HomeActivity.favoriteCities.size) {
            val firebaseId = cityData!!.firebaseId // ambil id unik firebase

            // cek id nya gak kosong
            if(firebaseId.isBlank()){ /* ... handle id kosong ... */ return }
            Log.d(tag, "updateNoteInFirebase: Mengupdate catatan di Firebase untuk ID: $firebaseId")

            // bikin alamat spesifik ke field 'note' di item itu
            val dbRef = Firebase.database.reference
                .child("journals")
                .child(currentUser.uid)
                .child(firebaseId)
                .child("note")

            // kirim nilai catatan baru ke alamat itu
            dbRef.setValue(newNote)
                .addOnSuccessListener { /* ... log sukses ... */ Log.d(tag, "Sukses update Firebase") }
                .addOnFailureListener { e -> /* ... handle gagal ... */ Toast.makeText(this, "Gagal update ke server", Toast.LENGTH_SHORT).show() }
        } else { /* ... handle data ga valid ... */ Toast.makeText(this, "Gagal update, data tidak valid", Toast.LENGTH_SHORT).show() }
    } // akhir updateNoteInFirebase

} // akhir class DetailActivity