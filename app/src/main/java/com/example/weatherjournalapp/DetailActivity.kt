package com.example.weatherjournalapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import java.util.Locale

// halaman buat nampilin detail jurnal
class DetailActivity : AppCompatActivity() {

    // tag buat nandain logcat biar gampang dicari
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
        val ivIcon: ImageView = findViewById(R.id.iv_detail_icon)
        val tvName: TextView = findViewById(R.id.tv_detail_name)
        val tvTemp: TextView = findViewById(R.id.tv_detail_temp)
        tvNote = findViewById(R.id.tv_detail_note)
        val tvCoordsValue: TextView = findViewById(R.id.tv_detail_coords_value)
        val btnEdit: Button = findViewById(R.id.btn_edit_note)
        val btnMap: Button = findViewById(R.id.btn_view_map)

        // tampilin data jurnal ke komponen layout
        ivIcon.setImageResource(cityData!!.getWeatherIcon()) // pake !! soalnya udah pasti gak null
        tvName.text = cityData!!.name
        tvTemp.text = cityData!!.temperature
        tvNote.text = cityData!!.note
        // format angka koordinat biar cuma 4 angka di belakang koma
        val coordsText = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", cityData!!.latitude, cityData!!.longitude)
        tvCoordsValue.text = coordsText
        Log.d(tag, "onCreate: Teks koordinat diatur: $coordsText")


        // ngasih aksi ke tombol lihat peta
        btnMap.setOnClickListener {
            // pake ?.let biar aman kalo cityData null (meski udah dicek)
            cityData?.let { data ->
                Log.d(tag, "Tombol 'Lihat Peta' diklik. Data: Lat=${data.latitude}, Lon=${data.longitude}")
                // cek kalo koordinatnya 0.0 (biasanya error pas nyimpen)
                if (data.latitude == 0.0 && data.longitude == 0.0) {
                    Log.w(tag, "Koordinat 0.0, 0.0. Mungkin GPS gagal saat menyimpan")
                    Toast.makeText(this, "Koordinat lokasi tidak valid (0,0)", Toast.LENGTH_LONG).show()
                    return@setOnClickListener // hentiin kalo 0.0
                }
                // bikin alamat geo: pake format q=latitude,longitude(Label)
                val geoUriString = String.format(Locale.US, "geo:0,0?q=%f,%f(Lokasi Kamu)", data.latitude, data.longitude)
                val gmmIntentUri = Uri.parse(geoUriString)

                Log.d(tag, "Membuat URI Peta (Format Baru): $gmmIntentUri")
                // bikin perintah buat buka alamat geo itu
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                // cek apa ada aplikasi peta di hp
                if (mapIntent.resolveActivity(packageManager) != null) {
                    Log.d(tag, "Aplikasi peta ditemukan, memulai Intent...")
                    startActivity(mapIntent) // buka aplikasi peta
                } else {
                    Log.w(tag, "Tidak ada aplikasi peta yang bisa menangani Intent")
                    Toast.makeText(this, "Gak ada aplikasi peta", Toast.LENGTH_SHORT).show()
                }
            } ?: Log.e(tag, "btnMap onClick: cityData is null!")
        }


        // ngasih aksi ke tombol edit
        btnEdit.setOnClickListener {
            // cek dulu data sama posisinya valid gak
            if (cityData != null && cityPosition != -1) {
                Log.d(tag, "Tombol 'Edit Catatan' diklik")
                showEditNoteDialog() // panggil fungsi buat nampilin pop-up edit
            } else {
                Log.e(tag, "btnEdit onClick: cityData atau cityPosition tidak valid!")
                Toast.makeText(this, "Data tidak valid untuk diedit", Toast.LENGTH_SHORT).show()
            }
        }
    } // akhir onCreate

    // fungsi buat nampilin pop-up edit catatan
    private fun showEditNoteDialog() {
        Log.d(tag, "showEditNoteDialog: Menampilkan dialog edit")
        // siapin kerangka dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Catatan")

        // bikin kotak input teks buat di dalem dialog
        val input = EditText(this)
        input.setText(tvNote.text.toString()) // isi pake teks catatan yang sekarang
        builder.setView(input) // masukin kotak input ke dialog

        // nambahin tombol 'Simpan' ke dialog
        builder.setPositiveButton("Simpan") { dialog, _ ->
            val newNote = input.text.toString() // ambil teks baru dari input
            Log.d(tag, "Dialog Edit: Tombol 'Simpan' diklik. Catatan baru: $newNote")

            // pastiin data jurnal gak null
            cityData?.let { currentData ->
                // bikin salinan data jurnal tapi catatannya diganti baru
                val updatedCity = currentData.copy(note = newNote)
                // cek posisi valid gak
                if (cityPosition != -1 && cityPosition < HomeActivity.favoriteCities.size) {
                    // update data di list static HomeActivity
                    HomeActivity.favoriteCities[cityPosition] = updatedCity
                    Log.d(tag, "Dialog Edit: List lokal di posisi $cityPosition diupdate")
                } else {
                    Log.w(tag, "Dialog Edit: Posisi $cityPosition tidak valid untuk update list lokal")
                }

                // update teks catatan di halaman detail ini
                tvNote.text = newNote
                // update juga data jurnal di variabel activity ini
                cityData = updatedCity
                Log.d(tag, "Dialog Edit: TextView catatan di halaman ini diupdate")

                // panggil fungsi buat nyimpen catatan baru ke firebase
                updateNoteInFirebase(newNote)

                Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show()
            } ?: Log.e(tag, "Dialog Edit Simpan: cityData is null!")

            dialog.dismiss() // tutup dialognya
        }

        // nambahin tombol 'Batal' ke dialog
        builder.setNegativeButton("Batal") { dialog, _ ->
            Log.d(tag, "Dialog Edit: Tombol 'Batal' diklik")
            dialog.cancel() // tutup dialognya tanpa ngapa-ngapain
        }

        builder.show() // tampilin dialognya ke layar
    } // akhir showEditNoteDialog

    // fungsi buat ngirim data note baru ke firebase
    private fun updateNoteInFirebase(newNote: String) {
        val currentUser = Firebase.auth.currentUser // ambil info user
        // cek semua data valid gak
        if (currentUser != null && cityData != null && cityPosition >= 0 && cityPosition < HomeActivity.favoriteCities.size) {
            val firebaseId = cityData!!.firebaseId // ambil id unik firebase dari data jurnal

            // cek id nya gak kosong
            if(firebaseId.isBlank()){
                Log.e(tag, "updateNoteInFirebase: Gagal update, ID Firebase kosong!")
                Toast.makeText(this, "Gagal update, ID Firebase tidak valid", Toast.LENGTH_SHORT).show()
                return
            }
            Log.d(tag, "updateNoteInFirebase: Mengupdate catatan di Firebase untuk ID: $firebaseId")

            // bikin alamat spesifik ke field 'note' di item jurnal itu
            val dbRef = Firebase.database.reference
                .child("journals")
                .child(currentUser.uid)
                .child(firebaseId)
                .child("note")

            // kirim nilai catatan baru ke alamat itu
            dbRef.setValue(newNote)
                .addOnSuccessListener {
                    Log.d(tag, "updateNoteInFirebase: Sukses update catatan di Firebase")
                }
                .addOnFailureListener { e -> // kalo gagal
                    Log.e(tag, "updateNoteInFirebase: Gagal update ke server: ${e.message}")
                    Toast.makeText(this, "Gagal update ke server", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e(tag, "updateNoteInFirebase: Gagal update, data user/cityData/posisi tidak valid")
            Toast.makeText(this, "Gagal update, data tidak valid", Toast.LENGTH_SHORT).show()
        }
    } // akhir updateNoteInFirebase

} // akhir class DetailActivity