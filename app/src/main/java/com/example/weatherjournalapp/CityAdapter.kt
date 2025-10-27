package com.example.weatherjournalapp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog // Import ini
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// adapter buat nampilin daftar kota favorit di recyclerview
class CityAdapter(
    private val context: Context,
) :
    RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    // class buat megang komponen view tiap item di list
    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCityName: TextView = itemView.findViewById(R.id.tv_item_city_name)
        val tvCityTemp: TextView = itemView.findViewById(R.id.tv_item_city_temp)
        val tvCityNote: TextView = itemView.findViewById(R.id.tv_item_note)
        val ivWeatherIcon: ImageView = itemView.findViewById(R.id.iv_weather_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        // nyambungin layout item_city.xml ke adapternya
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        // ambil data dari list static
        val city = HomeActivity.favoriteCities[position]

        holder.tvCityName.text = city.name
        holder.tvCityTemp.text = city.temperature
        holder.tvCityNote.text = city.note

        // nentuin iconnya pas mau ditampilin
        holder.ivWeatherIcon.setImageResource(city.getWeatherIcon())

        // nambahin aksi klik buat buka halaman detail
        holder.itemView.setOnClickListener {
            // siapin intent buat pindah halaman
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("EXTRA_CITY_DATA", city)
            intent.putExtra("EXTRA_POSITION", position)
            context.startActivity(intent)
        }

        // nambahin aksi TEKAN LAMA buat hapus
        holder.itemView.setOnLongClickListener {
            // panggil fungsi dialog hapus di HomeActivity
            // kita kirim posisi item yang ditekan
            (context as? HomeActivity)?.showDeleteDialog(position)
            true // balikin true biar klik biasa gak jalan barengan
        }
    }

    override fun getItemCount(): Int {
        // ambil ukuran dari list static
        return HomeActivity.favoriteCities.size
    }
}