package com.example.objectscanner.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.objectscanner.databinding.ItemResultBinding
import com.example.objectscanner.models.PhotoResult

class PhotoResultAdapter: ListAdapter<PhotoResult, PhotoResultAdapter.PhotoViewHolder>(
    PhotoDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PhotoViewHolder(private val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(photo: PhotoResult) {
            binding.ivResult.setImageURI(photo.imagePath.toUri())
            binding.tvTitle.text = photo.title
            binding.tvTime.text = photo.timestamp.toString()
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoResult>() {
        override fun areItemsTheSame(oldItem: PhotoResult, newItem: PhotoResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoResult, newItem: PhotoResult): Boolean {
            return oldItem == newItem
        }
    }
}