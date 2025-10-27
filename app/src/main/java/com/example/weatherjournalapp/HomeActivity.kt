package com.example.weatherjournalapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log // IMPORT PENTING BUAT DEBUG
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Import ini
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// halaman utama setelah user login
class HomeActivity : AppCompatActivity() {


    private val API_KEY = "fc2df916b3b7b84d3bba2c097f507963"

    // ini tag buat nge-cek di logcat
    private val TAG = "HomeActivity"

    // properti buat firebase auth
    private lateinit var auth: FirebaseAuth

    // properti buat database
    private lateinit var dbRef: DatabaseReference

    // properti buat ngambil lokasi gps
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // properti buat ngurus daftar jurnal
    private lateinit var rvCityList: RecyclerView
    private lateinit var editAddCity: EditText
    private lateinit var editAddNote: EditText
    private lateinit var btnAddCity: Button
    private lateinit var btnLogout: Button

    // adapter-nya kita jadiin variabel global
    private lateinit var cityAdapter: CityAdapter

    companion object {
        // list data-nya kita bikin static biar bisa diakses dari DetailActivity
        val favoriteCities = ArrayList<FavoriteCity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        Log.d(TAG, "onCreate: Activity dibuat")

        // inisialisasi semua properti
        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // cek kalo user udah login
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User null, balik ke Login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // inisialisasi database ref, khusus buat user ini
        val dbPath = "journals/${currentUser.uid}"
        dbRef = Firebase.database.reference.child(dbPath)
        Log.d(TAG, "Database reference diatur ke: $dbPath")

        rvCityList = findViewById(R.id.rv_city_list)
        editAddCity = findViewById(R.id.edit_add_city)
        editAddNote = findViewById(R.id.edit_add_note)
        btnAddCity = findViewById(R.id.btn_add_city)
        btnLogout = findViewById(R.id.btn_logout)

        // manggil fungsi buat nyiapin recyclerview
        setupRecyclerView()

        // panggil fungsi buat ngambil data dari firebase
        loadJournalsFromFirebase()

        // ngasih aksi ke tombol tambah
        btnAddCity.setOnClickListener {
            Log.d(TAG, "Tombol 'Tambah' diklik")
            // pas diklik, kita cek izin gps dulu
            checkLocationPermission()
        }

        // ngasih aksi ke tombol logout
        btnLogout.setOnClickListener {
            Log.d(TAG, "Tombol 'Logout' diklik")
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // fungsi ini kepanggil tiap kita balik ke halaman ini
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Activity kembali aktif, refresh adapter")
        // kita refresh listnya, kali aja ada data yang di-edit
        if (::cityAdapter.isInitialized) {
            cityAdapter.notifyDataSetChanged()
        }
    }

    // nge-cek user udah login atau belum pas buka aplikasi
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "onStart: User null, balik ke Login")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // fungsi buat ngambildata dari firebase
    private fun loadJournalsFromFirebase() {
        Log.d(TAG, "loadJournalsFromFirebase: Mulai ambil data dari Firebase...")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: Data diterima, jumlah: ${snapshot.childrenCount}")
                favoriteCities.clear()

                for (journalSnapshot in snapshot.children) {
                    val journal = journalSnapshot.getValue(FavoriteCity::class.java)
                    if (journal != null) {
                        journal.firebaseId = journalSnapshot.key ?: ""
                        favoriteCities.add(journal)
                    }
                }
                cityAdapter.notifyDataSetChanged()
            }

            // kalo gagal dengerin
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: Gagal muat data: ${error.message}")
                Toast.makeText(baseContext, "Gagal muat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // fungsi buat nyiapin recyclerview
    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Adapter dan LayoutManager diatur")
        cityAdapter = CityAdapter(this)
        rvCityList.layoutManager = LinearLayoutManager(this)
        rvCityList.adapter = cityAdapter
    }

    // fungsi nge-fetch data cuaca buat ditambahin ke list
    private fun fetchWeatherForCity(cityName: String, note: String, lat: Double, lon: Double) {
        Log.d(TAG, "fetchWeatherForCity: Panggil API cuaca untuk $cityName")
        ApiClient.instance.getWeatherByCity(cityName, API_KEY)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            Log.d(TAG, "onResponse (Cuaca): Sukses dapet data cuaca")
                            val condition = it.weatherInfo.firstOrNull()?.main ?: "Clear"
                            val temp = "${it.mainInfo.temp}°C"

                            val city = FavoriteCity(
                                name = it.cityName,
                                temperature = temp,
                                note = note,
                                condition = condition,
                                latitude = lat,
                                longitude = lon
                            )

                            // simpen data baru ke firebase
                            val newJournalRef = dbRef.push()
                            newJournalRef.setValue(city)
                                .addOnSuccessListener {
                                    Log.d(TAG, "onSuccess (Firebase): Sukses simpen ke database")
                                    city.firebaseId = newJournalRef.key ?: ""
                                    favoriteCities.add(city)
                                    cityAdapter.notifyDataSetChanged()

                                    editAddCity.text.clear()
                                    editAddNote.text.clear()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "onFailure (Firebase): Gagal simpen: ${e.message}")
                                    Toast.makeText(baseContext, "Gagal nyimpen data", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Log.w(TAG, "onResponse (Cuaca): Gagal, kode: ${response.code()} (Kota gak ketemu?)")
                        Toast.makeText(baseContext, "Kota gak ketemu", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e(TAG, "onFailure (Cuaca): Gagal konek: ${t.message}")
                    Toast.makeText(baseContext, "Gagal konek: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // buat ngecek izin lokasi
    private fun checkLocationPermission() {
        val cityName = editAddCity.text.toString()
        if (cityName.isEmpty()) {
            Log.w(TAG, "checkLocationPermission: Nama kota kosong")
            Toast.makeText(this, "Nama kota gaboleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // kita cek izin FINE (akurasi tinggi) aja
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkLocationPermission: Izin lokasi BELUM ada. Minta izin...")
            // kalo belum ada izin, minta izin dulu
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001)
        } else {
            // kalo izin udah ada, langsung simpen jurnalnya
            Log.d(TAG, "checkLocationPermission: Izin lokasi SUDAH ada. Panggil saveJournalEntry().")
            saveJournalEntry()
        }
    }

    // fungsi buat ambil lokasi terakhir dan manggil api cuaca
    private fun saveJournalEntry() {
        Log.d(TAG, "saveJournalEntry: Masuk fungsi, mulai ambil GPS...")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "saveJournalEntry: Gagal, gak punya izin (ini harusnya gak kejadian)")
            return
        }

        val cancellationTokenSource = CancellationTokenSource()

        // minta lokasi saat ini, BUKAN lokasi terakhir
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "onSuccess (GPS): Sukses dapet lokasi: ${location.latitude}")
                    val name = editAddCity.text.toString()
                    val note = editAddNote.text.toString()

                    // baru kita panggil api cuaca sambil bawa data gps
                    fetchWeatherForCity(name, note, location.latitude, location.longitude)

                } else {
                    Log.e(TAG, "onSuccess (GPS): Sukses, tapi lokasi NULL. Cek GPS/Layanan Lokasi di HP.")
                    Toast.makeText(this, "Lokasi (null), nyalain GPS", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                // ini kalo user matiin gps atau gaada sinyal
                Log.e(TAG, "onFailure (GPS): Gagal dapet lokasi: ${exception.message}")
                Toast.makeText(this, "Gagal ambil lokasi: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    // fungsi yang jalan setelah user milih 'allow' atau 'deny' izin lokasi
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // kalo diizinin, simpen jurnalnya
                Log.d(TAG, "onRequestPermissionsResult: Izin DIBERIKAN. Panggil saveJournalEntry().")
                saveJournalEntry()
            } else {
                // kalo ditolak, kasih tau user
                Log.w(TAG, "onRequestPermissionsResult: Izin DITOLAK.")
                Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // fungsi buat nampilin pop-up konfirmasi hapus
    fun showDeleteDialog(position: Int) {
        // ambil data item yang mau dihapus
        val cityToDelete = favoriteCities[position]

        // bikin dialog konfirmasi
        AlertDialog.Builder(this)
            .setTitle("Hapus Jurnal")
            .setMessage("Yakin mau hapus jurnal untuk ${cityToDelete.name}?")
            .setPositiveButton("Ya") { _, _ ->
                // kalo user klik 'Ya', panggil fungsi hapus
                deleteJournalEntry(position, cityToDelete.firebaseId)
            }
            .setNegativeButton("Batal", null) // kalo batal, gak ngapa-ngapain
            .show()
    }

    // fungsi buat hapus data dari firebase dan list lokal
    private fun deleteJournalEntry(position: Int, firebaseId: String) {
        Log.d(TAG, "deleteJournalEntry: Mencoba hapus item di posisi $position dengan ID $firebaseId")

        // cek kalo ID-nya valid
        if (firebaseId.isBlank()) {
            Log.e(TAG, "deleteJournalEntry: Gagal, Firebase ID kosong!")
            Toast.makeText(this, "Gagal hapus, ID tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // hapus dari Firebase pake ID uniknya
        dbRef.child(firebaseId).removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "onSuccess (Firebase Delete): Sukses hapus dari database")
                // kalo di firebase sukses, hapus juga dari list lokal
                favoriteCities.removeAt(position)
                // refresh adapter-nya
                cityAdapter.notifyItemRemoved(position)
                // refresh posisi item lain setelah dihapus (opsional tapi bagus)
                cityAdapter.notifyItemRangeChanged(position, favoriteCities.size)

                Toast.makeText(this, "Jurnal dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "onFailure (Firebase Delete): Gagal hapus: ${e.message}")
                Toast.makeText(this, "Gagal hapus dari server", Toast.LENGTH_SHORT).show()
            }
    }
}