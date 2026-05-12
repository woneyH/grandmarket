package com.pbl.grandmarket_android.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.data.model.StoreItem
import com.pbl.grandmarket_android.ui.adapter.StoreListAdapter

class StoreListBottomSheetFragment(
    private val sortedStores: List<StoreItem>
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottom_sheet_store_list, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_stores)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = StoreListAdapter(sortedStores) { clickedStore ->
            dismiss()
        }
        recyclerView.adapter = adapter

        return view
    }
}