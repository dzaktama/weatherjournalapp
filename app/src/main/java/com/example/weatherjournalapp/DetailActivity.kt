package com.example.weatherjournalapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import java.util.Locale // Import ini untuk format angka

// halaman buat nampilin detail jurnal
class DetailActivity : AppCompatActivity() {

    // kita siapin variabel buat nampung data
    private lateinit var cityData: FavoriteCity
    private var cityPosition: Int = -1

    // variabel buat nampung view
    private lateinit var tvNote: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // ambil data yang dikirim dari HomeActivity
        cityData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("EXTRA_CITY_DATA", FavoriteCity::class.java)
        } else {
            intent.getParcelableExtra("EXTRA_CITY_DATA")
        } ?: return // kalo datanya null, mending tutup aja halamannya

        // ambil posisi itemnya
        cityPosition = intent.getIntExtra("EXTRA_POSITION", -1)

        // sambungin view dari xml
        val ivIcon: ImageView = findViewById(R.id.iv_detail_icon)
        val tvName: TextView = findViewById(R.id.tv_detail_name)
        val tvTemp: TextView = findViewById(R.id.tv_detail_temp)
        tvNote = findViewById(R.id.tv_detail_note)

        // ## Sambungkan TextView Koordinat BARU ##
        val tvCoordsValue: TextView = findViewById(R.id.tv_detail_coords_value)

        val btnEdit: Button = findViewById(R.id.btn_edit_note)
        val btnMap: Button = findViewById(R.id.btn_view_map)

        // isi view-nya pake data
        ivIcon.setImageResource(cityData.getWeatherIcon())
        tvName.text = cityData.name
        tvTemp.text = cityData.temperature
        tvNote.text = cityData.note

        // ## Isi TextView Koordinat BARU ##
        // Format angka latitude dan longitude jadi teks (ambil 4 angka di belakang koma)
        val coordsText = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", cityData.latitude, cityData.longitude)
        tvCoordsValue.text = coordsText
        // ## Akhir Pengisian Koordinat ##

        // ngasih aksi ke tombol lihat peta
        btnMap.setOnClickListener {
            val gmmIntentUri = Uri.parse("geo:${cityData.latitude},${cityData.longitude}?q=${Uri.encode(cityData.name)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(this, "Gak ada aplikasi peta", Toast.LENGTH_SHORT).show()
            }
        }

        // ngasih aksi ke tombol edit
        btnEdit.setOnClickListener {
            showEditNoteDialog()
        }
    }

    // fungsi buat nampilin pop-up edit catatan
    private fun showEditNoteDialog() {
        // siapin pop-up
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Catatan")

        // bikin tempat ngetik di dalem pop-up
        val input = EditText(this)
        input.setText(tvNote.text.toString()) // isi pake catatan lama
        builder.setView(input)

        // tombol simpan
        builder.setPositiveButton("Simpan") { dialog, _ ->
            val newNote = input.text.toString()

            // 1. update data di list static lokal
            val updatedCity = cityData.copy(note = newNote)
            HomeActivity.favoriteCities[cityPosition] = updatedCity

            // 2. update data di halaman ini
            tvNote.text = newNote
            cityData = updatedCity // Update data lokal di activity ini juga

            // 3. update data di firebase
            updateNoteInFirebase(newNote)

            Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // tombol batal
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    // fungsi buat ngirim data note baru ke firebase
    private fun updateNoteInFirebase(newNote: String) {
        val currentUser = Firebase.auth.currentUser
        // Pastikan cityPosition valid sebelum mengakses list
        if (currentUser != null && cityPosition >= 0 && cityPosition < HomeActivity.favoriteCities.size) {
            // ambil id unik dari itemnya
            val firebaseId = cityData.firebaseId

            // Pastikan firebaseId tidak kosong
            if(firebaseId.isBlank()){
                Toast.makeText(this, "Gagal update, ID Firebase tidak valid", Toast.LENGTH_SHORT).show()
                return
            }

            // bikin referensi langsung ke 'note' di item itu
            val dbRef = Firebase.database.reference
                .child("journals")
                .child(currentUser.uid)
                .child(firebaseId)
                .child("note")

            dbRef.setValue(newNote)
                .addOnFailureListener {
                    // kalo gagal, kasih tau
                    Toast.makeText(this, "Gagal update ke server", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Gagal update, data tidak valid", Toast.LENGTH_SHORT).show()
        }
    }
}