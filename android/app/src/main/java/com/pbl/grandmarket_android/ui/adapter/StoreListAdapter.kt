package com.pbl.grandmarket_android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.data.model.StoreItem

class StoreListAdapter(
    private val storeList: List<StoreItem>,
    private val onItemClick: (StoreItem) -> Unit
) : RecyclerView.Adapter<StoreListAdapter.StoreViewHolder>() {

    inner class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStoreName: TextView = itemView.findViewById(R.id.tv_store_name)
        val tvStoreAddress: TextView = itemView.findViewById(R.id.tv_store_address)
        val tvStoreDistance: TextView = itemView.findViewById(R.id.tv_store_distance)

        fun bind(store: StoreItem) {
            tvStoreName.text = store.storeName
            tvStoreAddress.text = store.address

            tvStoreDistance.text = store.getFormattedDistance()

            itemView.setOnClickListener {
                onItemClick(store)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store_list, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(storeList[position])
    }

    override fun getItemCount(): Int {
        return storeList.size
    }
}