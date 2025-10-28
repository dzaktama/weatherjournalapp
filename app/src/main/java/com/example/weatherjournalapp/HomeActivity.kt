package com.example.weatherjournalapp

// ... (import biarin aja) ...
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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


// halaman utama setelah user login
class HomeActivity : AppCompatActivity() {

    // tag buat nge-cek di logcat
    private val TAG = "HomeActivity"

    // properti buat firebase auth
    private lateinit var auth: FirebaseAuth

    // properti buat database
    private lateinit var dbRef: DatabaseReference

    // properti buat ngambil lokasi gps
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // properti buat komponen ui di layout
    private lateinit var rvCityList: RecyclerView
    private lateinit var editAddCity: EditText // input nama tempat
    private lateinit var editAddNote: EditText
    private lateinit var btnAddCity: Button
    private lateinit var btnLogout: Button
    // tambahin variabel buat textview sapaan
    private lateinit var tvWelcomeUser: TextView

    // adapter buat ngatur recyclerview
    private lateinit var cityAdapter: CityAdapter

    companion object {
        // list data jurnal sementara di memori hp (static)
        val favoriteCities = ArrayList<FavoriteCity>()
    }

    // fungsi yg jalan pertama kali pas halaman dibuka
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home) // pake layout activity_home.xml
        Log.d(TAG, "onCreate: Activity dibuat")

        // siapin alat firebase auth & gps
        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // cek user udah login apa belum
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "User null, balik ke Login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // siapin alamat database buat user ini
        val dbPath = "journals/${currentUser.uid}"
        dbRef = Firebase.database.reference.child(dbPath)
        Log.d(TAG, "Database reference diatur ke: $dbPath")

        // sambungin variabel ke komponen di xml
        rvCityList = findViewById(R.id.rv_city_list)
        editAddCity = findViewById(R.id.edit_add_city)
        editAddNote = findViewById(R.id.edit_add_note)
        btnAddCity = findViewById(R.id.btn_add_city)
        btnLogout = findViewById(R.id.btn_logout)
        // sambungin textview sapaan
        tvWelcomeUser = findViewById(R.id.tv_welcome_user)

        // tampilin sapaan
        val userEmail = currentUser.email
        tvWelcomeUser.text = "Halo, ${userEmail ?: "Pengguna"}"

        // siapin recyclerview
        setupRecyclerView()

        // ambil data awal dari firebase
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
            // tambahin flags biar ga bisa back ke home setelah logout
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    } // akhir onCreate

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

    // fungsi buat ngambil data jurnal dari firebase sekali aja
    private fun loadJournalsFromFirebase() {
        Log.d(TAG, "loadJournalsFromFirebase: Mulai ambil data dari Firebase...")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            // kalo data berhasil diambil
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: Data diterima, jumlah: ${snapshot.childrenCount}")
                favoriteCities.clear() // kosongin list lama

                // loop tiap data jurnal yg diterima
                for (journalSnapshot in snapshot.children) {
                    val journal = journalSnapshot.getValue(FavoriteCity::class.java) // ubah jadi objek kotlin
                    if (journal != null) {
                        journal.firebaseId = journalSnapshot.key ?: "" // simpen id firebase
                        favoriteCities.add(journal) // tambahin ke list lokal
                    }
                }
                cityAdapter.notifyDataSetChanged() // refresh tampilan list
            }

            // kalo gagal ngambil data
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: Gagal muat data: ${error.message}")
                Toast.makeText(baseContext, "Gagal muat data", Toast.LENGTH_SHORT).show()
            }
        })
    } // akhir loadJournalsFromFirebase

    // fungsi buat nyiapin recyclerview
    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Adapter dan LayoutManager diatur")
        cityAdapter = CityAdapter(this) // bikin adapter
        rvCityList.layoutManager = LinearLayoutManager(this) // atur layout lurus ke bawah
        rvCityList.adapter = cityAdapter // pasang adapter ke recyclerview
    }

    // buat ngecek izin lokasi sebelum nyimpen
    private fun checkLocationPermission() {
        val placeName = editAddCity.text.toString() // ambil nama tempat
        if (placeName.isEmpty()) { // cek kalo kosong
            Log.w(TAG, "checkLocationPermission: Nama tempat kosong")
            Toast.makeText(this, "Nama tempat gaboleh kosong", Toast.LENGTH_SHORT).show()
            return // hentiin proses kalo kosong
        }

        // cek apa udah punya izin gps akurat
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkLocationPermission: Izin lokasi BELUM ada. Minta izin...")
            // kalo belum, minta izin ke user
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001) // kode permintaan izin
        } else {
            // kalo izin udah ada, langsung simpen jurnalnya
            Log.d(TAG, "checkLocationPermission: Izin lokasi SUDAH ada. Panggil saveJournalEntry().")
            saveJournalEntry() // panggil fungsi buat ambil gps & simpen
        }
    } // akhir checkLocationPermission

    // fungsi buat ambil lokasi gps dan nyimpen jurnal ke firebase
    private fun saveJournalEntry() {
        Log.d(TAG, "saveJournalEntry: Masuk fungsi, mulai ambil GPS...")
        // cek lagi izinnya buat jaga2
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "saveJournalEntry: Gagal, gak punya izin (ini harusnya gak kejadian)")
            return
        }

        // alat buat batalin request gps kalo kelamaan
        val cancellationTokenSource = CancellationTokenSource()

        // minta lokasi saat ini pake akurasi tinggi
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location -> // kalo berhasil dapet lokasi
                if (location != null) { // kalo lokasi gak null
                    Log.d(TAG, "onSuccess (GPS): Sukses dapet lokasi: ${location.latitude}")
                    // ambil nama tempat & catatan dari input
                    val name = editAddCity.text.toString()
                    val note = editAddNote.text.toString()
                    // ambil waktu sekarang dalam milidetik
                    val currentTimeMillis = System.currentTimeMillis()

                    // ## PASTIKAN KONSTRUKTOR SESUAI DEFINISI FavoriteCity ##
                    // pake named arguments biar aman dari salah urutan
                    val city = FavoriteCity(
                        name = name,
                        note = note,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = currentTimeMillis
                    )
                    // ## AKHIR PEMANGGILAN KONSTRUKTOR ##

                    // simpen data baru ke firebase
                    val newJournalRef = dbRef.push() // bikin id unik baru
                    newJournalRef.setValue(city) // simpen objeknya ke id itu
                        .addOnSuccessListener { // kalo sukses nyimpen ke firebase
                            Log.d(TAG, "onSuccess (Firebase): Sukses simpen ke database")
                            city.firebaseId = newJournalRef.key ?: "" // simpen id firebase ke objek lokal
                            favoriteCities.add(0, city) // tambahin ke list lokal di paling atas
                            cityAdapter.notifyItemInserted(0) // refresh lebih efisien
                            rvCityList.scrollToPosition(0) // scroll ke paling atas (opsional)

                            // kosongin input
                            editAddCity.text.clear()
                            editAddNote.text.clear()
                        }
                        .addOnFailureListener { e -> // kalo gagal nyimpen ke firebase
                            Log.e(TAG, "onFailure (Firebase Save): Gagal simpen: ${e.message}", e)
                            Toast.makeText(baseContext, "Gagal simpan ke DB: ${e.message}", Toast.LENGTH_LONG).show()
                        }

                } else { // kalo lokasi null (gps mati?)
                    Log.e(TAG, "onSuccess (GPS): Sukses, tapi lokasi NULL. Cek GPS/Layanan Lokasi di HP.")
                    Toast.makeText(this, "Lokasi (null), nyalain GPS", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception -> // kalo gagal dapet lokasi
                Log.e(TAG, "onFailure (GPS): Gagal dapet lokasi: ${exception.message}")
                Toast.makeText(this, "Gagal ambil lokasi: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    } // akhir saveJournalEntry

    // fungsi yg otomatis jalan setelah user jawab dialog izin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) { // cek kalo ini jawaban buat izin lokasi
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // cek kalo diizinin
                // kalo diizinin, lanjut simpen jurnalnya
                Log.d(TAG, "onRequestPermissionsResult: Izin DIBERIKAN. Panggil saveJournalEntry().")
                saveJournalEntry()
            } else { // kalo ditolak
                Log.w(TAG, "onRequestPermissionsResult: Izin DITOLAK.")
                Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    } // akhir onRequestPermissionsResult

    // fungsi buat nampilin pop-up konfirmasi hapus
    fun showDeleteDialog(position: Int) {
        // ambil data item yg mau dihapus dari list lokal
        // tambahin cek biar ga crash kalo posisi ga valid
        if (position < 0 || position >= favoriteCities.size) {
            Log.e(TAG, "showDeleteDialog dipanggil dengan posisi tidak valid: $position")
            return
        }
        val cityToDelete = favoriteCities[position]

        // bikin dialog konfirmasi
        AlertDialog.Builder(this)
            .setTitle("Hapus Jurnal")
            .setMessage("Yakin mau hapus jurnal untuk ${cityToDelete.name}?")
            .setPositiveButton("Ya") { _, _ -> // kalo user klik 'Ya'
                // panggil fungsi hapus
                deleteJournalEntry(position, cityToDelete.firebaseId)
            }
            .setNegativeButton("Batal", null) // kalo 'Batal', gak ngapa2in
            .show() // tampilin dialognya
    } // akhir showDeleteDialog

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
            .addOnSuccessListener { // kalo di firebase sukses
                Log.d(TAG, "onSuccess (Firebase Delete): Sukses hapus dari database")
                // cek lagi posisi sebelum hapus dari list lokal
                if (position >= 0 && position < favoriteCities.size) {
                    // Pastikan item yang akan dihapus dari list lokal memang benar
                    if (favoriteCities[position].firebaseId == firebaseId) {
                        favoriteCities.removeAt(position) // hapus juga dari list lokal pake posisi
                        // refresh adapter (bilangin item dihapus)
                        cityAdapter.notifyItemRemoved(position)
                        // refresh posisi item lain setelah dihapus (opsional)
                        cityAdapter.notifyItemRangeChanged(position, favoriteCities.size)
                        Toast.makeText(this, "Jurnal dihapus", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "onSuccess (Firebase Delete): Mismatch ID saat hapus list lokal! Posisi $position, ID $firebaseId")
                        loadJournalsFromFirebase() // Muat ulang jika ada ketidaksesuaian
                    }
                } else {
                    Log.e(TAG, "onSuccess (Firebase Delete): Posisi $position tidak valid untuk list lokal!")
                    loadJournalsFromFirebase() // Data di firebase sudah terhapus, muat ulang data dari awal
                }
            }
            .addOnFailureListener { e -> // kalo gagal hapus di firebase
                Log.e(TAG, "onFailure (Firebase Delete): Gagal hapus: ${e.message}")
                Toast.makeText(this, "Gagal hapus dari server", Toast.LENGTH_SHORT).show()
            }
    } // akhir deleteJournalEntry

} // akhir class HomeActivity