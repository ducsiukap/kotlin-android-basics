package com.vduczz.navigationcomponent.home.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vduczz.navigationcomponent.databinding.ItemItemBinding

class ItemAdapter(
    private val items: List<String>,
    private val onClickItem: (String) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    var isScrollingDown = true

    inner class ItemViewHolder(private val binding: ItemItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            val item = items[position]
            binding.tvItemName.text = item

            binding.root.setOnClickListener {
                onClickItem(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(position)

        // animation
        setAnimation(holder.itemView, position)
    }

    override fun getItemCount() = items.size


    private fun setAnimation(
        view: View,
        position: Int
    ) {
        if (isScrollingDown) {
            // Scroll xuống
            view.translationY = 100f
            view.alpha = 0f

            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()

        } else {
//            // Scroll lên
            view.translationY = -100f
            view.alpha = 0f

            view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
}

