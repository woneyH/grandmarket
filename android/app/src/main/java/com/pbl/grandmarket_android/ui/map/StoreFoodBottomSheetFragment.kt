package com.pbl.grandmarket_android.ui.map

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.data.model.FoodEntity
import com.pbl.grandmarket_android.data.model.StoreItem
import com.pbl.grandmarket_android.data.repository.FoodRepository
import com.pbl.grandmarket_android.ui.adapter.ProductCategory
import com.pbl.grandmarket_android.ui.adapter.SaleItem
import com.pbl.grandmarket_android.ui.adapter.SaleStatus
import com.pbl.grandmarket_android.ui.adapter.SalesAdapter

class StoreFoodBottomSheetFragment(
    private val store: StoreItem,
    private val onRouteClick: (StoreItem) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: SalesAdapter
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_bottom_sheet_store_food, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tv_store_food_title).text = "${store.storeName} 식자재"
        view.findViewById<TextView>(R.id.tv_store_food_distance).text = store.getFormattedDistance()
        emptyView = view.findViewById(R.id.tv_store_food_empty)

        adapter = SalesAdapter(
            onItemClick = {},
            onDeleteClick = {},
            onCompleteClick = {},
            isEditable = false
        )

        view.findViewById<RecyclerView>(R.id.recycler_view_store_foods).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StoreFoodBottomSheetFragment.adapter
        }

        view.findViewById<MaterialButton>(R.id.btn_route_store).setOnClickListener {
            onRouteClick(store)
        }

        loadStoreFoods()
        // 구매자가 점포 식자재 목록을 열면 해당 판매자의 storeLocation viewCount 1 증가
        incrementSellerViewCount()
    }

    // FoodRepository를 통해 판매자 점포의 viewCount를 Firebase에서 1 증가
    private fun incrementSellerViewCount() {
        FoodRepository().incrementStoreViewCount(store.storeId)
    }

    // storeLocation 문서 id와 닉네임을 foodList의 kakaoId/sellerName과 매칭해 해당 판매자 식자재만 조회
    private fun loadStoreFoods() {
        FirebaseFirestore.getInstance()
            .collection("foodList")
            .whereEqualTo("kakaoId", store.storeId)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { document ->
                    val food = document.toObject(FoodEntity::class.java) ?: return@mapNotNull null
                    if (food.sellerName != store.storeName) return@mapNotNull null
                    SaleItem(
                        id = document.id,
                        title = food.foodName,
                        price = food.price.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
                        category = food.category.toProductCategory(),
                        imageUrl = food.imageUrl,
                        stockCount = 0,
                        status = food.status.toSaleStatus(),
                        createdAt = food.timestamp
                    )
                }.sortedByDescending { it.createdAt }

                adapter.submitList(items)
                emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { error ->
                Log.e("StoreFoodBottomSheet", "판매자 식자재 조회 실패", error)
                Toast.makeText(requireContext(), "식자재 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                emptyView.visibility = View.VISIBLE
            }
    }

    // Firebase 문자열 카테고리를 기존 리스트 UI 카테고리 타입으로 변환
    private fun String.toProductCategory(): ProductCategory {
        return when (this) {
            "육류", "MEAT", "meat" -> ProductCategory.MEAT
            else -> ProductCategory.VEGETABLE
        }
    }

    // Firebase 판매 상태 문자열을 기존 리스트 UI 상태 타입으로 변환
    private fun String.toSaleStatus(): SaleStatus {
        return when (this) {
            "판매완료", "SOLD_OUT", "sold_out" -> SaleStatus.SOLD_OUT
            else -> SaleStatus.ON_SALE
        }
    }
}
