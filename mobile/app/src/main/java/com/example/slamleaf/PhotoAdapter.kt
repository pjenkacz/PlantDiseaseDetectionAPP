package com.example.slamleaf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

class PhotoAdapter(
    private val onItemClick: (UiPhotoItem) -> Unit
) : ListAdapter<UiPhotoItem, PhotoAdapter.PhotoViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<UiPhotoItem>() {
        override fun areItemsTheSame(oldItem: UiPhotoItem, newItem: UiPhotoItem) =
            oldItem.localId == newItem.localId

        override fun areContentsTheSame(oldItem: UiPhotoItem, newItem: UiPhotoItem) =
            oldItem == newItem
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageThumb: ImageView = itemView.findViewById(R.id.imageThumb)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textSubtitle: TextView = itemView.findViewById(R.id.textSubtitle)
        private val progressConfidence: ProgressBar = itemView.findViewById(R.id.progressConfidence)

        fun bind(item: UiPhotoItem) {
            itemView.setOnClickListener {
                onItemClick(item)
            }
            when (item.status) {
                PredictionStatus.PENDING -> {
                    // obraz lokalny
                    Glide.with(itemView).load(File(item.localPath)).into(imageThumb)
                    textTitle.text = "Analizowanie..."
                    textSubtitle.text = "Czekaj na wynik modelu"
                    progressConfidence.visibility = View.GONE
                }
                PredictionStatus.SUCCESS -> {
                    val model = item.processedUrl ?: File(item.localPath)
                    Glide.with(itemView).load(model).into(imageThumb)
                    textTitle.text = item.mainDisease?.displayName ?: "Choroba nieznana"
                    val conf = item.confidence ?: 0f
                    textSubtitle.text = "Pewność: ${(conf * 100).toInt()} %"
                    progressConfidence.visibility = View.VISIBLE
                    progressConfidence.progress = (conf * 100).toInt()
                }
                PredictionStatus.ERROR -> {
                    Glide.with(itemView).load(File(item.localPath)).into(imageThumb)
                    textTitle.text = "Błąd analizy"
                    textSubtitle.text = "Spróbuj ponownie"
                    progressConfidence.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
