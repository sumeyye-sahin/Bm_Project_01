package com.sumeyyesahin.olumlamalar

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sumeyyesahin.olumlamalar.model.Olumlamalarlistmodel

class FavoriAdapter(private var favoriteAffirmations: List<Olumlamalarlistmodel>, private val userLanguage: String) :
    RecyclerView.Adapter<FavoriAdapter.FavoriteViewHolder>() {

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewAffirmation: TextView = itemView.findViewById(R.id.textViewAffirmation)
        val buttonDelete: Button = itemView.findViewById(R.id.buttonDelete)

        init {
            buttonDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val clickedAffirmation = favoriteAffirmations[position]
                    showDeleteConfirmationDialog(clickedAffirmation)
                }
            }
        }

        private fun showDeleteConfirmationDialog(clickedAffirmation: Olumlamalarlistmodel) {
            val builder = AlertDialog.Builder(itemView.context)
            builder.apply {
                setTitle("Olumlamayı Favorilerden Sil")
                setMessage("Bu olumlamayı favorilerden silmek istediğinizden emin misiniz?")
                setPositiveButton("Evet") { dialog, _ ->
                    deleteAffirmations(clickedAffirmation)
                    dialog.dismiss()
                }
                setNegativeButton("Hayır") { dialog, _ ->
                    dialog.dismiss()
                }
            }
            val dialog = builder.create()
            dialog.show()
        }

        private fun deleteAffirmations(clickedAffirmation: Olumlamalarlistmodel) {
            clickedAffirmation.favorite = false
            DBHelper(itemView.context).updateAffirmationFavStatus(clickedAffirmation)
            favoriteAffirmations = favoriteAffirmations.filterNot { it.affirmation == clickedAffirmation.affirmation && it.language == clickedAffirmation.language }
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.favori_item, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val currentItem = favoriteAffirmations[position]
        holder.textViewAffirmation.text = currentItem.affirmation
    }

    override fun getItemCount() = favoriteAffirmations.size
}
