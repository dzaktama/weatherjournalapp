package com.example.weatherjournalapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

// adapter buat nampilin daftar jurnal di recyclerview
class CityAdapter(
    private val context: Context,
) :
    RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    // class buat megang komponen view tiap item di list
    class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // sambungin variabel ke komponen di item_city.xml
        val tvCityName: TextView = itemView.findViewById(R.id.tv_item_city_name)
        val tvCityNote: TextView = itemView.findViewById(R.id.tv_item_note)
        // sambungin textview timestamp
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_item_timestamp)
    }

    // fungsi yg jalan buat bikin tampilan tiap item (ViewHolder)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city, parent, false)
        return CityViewHolder(view)
    }

    // fungsi yg jalan buat nampilin data ke tiap item ViewHolder
    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        // ambil data jurnal dari list static di HomeActivity sesuai urutan (posisi)
        val city = HomeActivity.favoriteCities[position]

        // masukin data nama & note ke textview di item
        holder.tvCityName.text = city.name
        holder.tvCityNote.text = city.note
        // masukin data waktu yg udah diformat ke textview timestamp
        holder.tvTimestamp.text = city.getFormattedTimestamp()

        // ngasih aksi kalo itemnya diklik
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("EXTRA_CITY_DATA", city)
            intent.putExtra("EXTRA_POSITION", position)
            context.startActivity(intent)
        }

        // ngasih aksi kalo itemnya ditekan lama
        holder.itemView.setOnLongClickListener {
            (context as? HomeActivity)?.showDeleteDialog(position)
            true // balikin true biar klik biasa ga ikutan jalan
        }
    }

    // fungsi buat ngasih tau adapter ada berapa total item di list
    override fun getItemCount(): Int {
        return HomeActivity.favoriteCities.size
    }
}