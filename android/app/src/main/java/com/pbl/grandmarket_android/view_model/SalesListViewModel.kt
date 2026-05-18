package com.pbl.grandmarket_android.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pbl.grandmarket_android.data.repository.FoodRepository
import com.pbl.grandmarket_android.data.repository.SellerFoodItem
import com.pbl.grandmarket_android.ui.adapter.ProductCategory
import com.pbl.grandmarket_android.ui.adapter.SaleItem
import com.pbl.grandmarket_android.ui.adapter.SaleStatus

class SalesListViewModel : ViewModel() {
    private val repository = FoodRepository()

    private val _salesList = MutableLiveData<List<SaleItem>>()
    val salesList: LiveData<List<SaleItem>> = _salesList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val allItems = mutableListOf<SaleItem>()
    private var currentFilter: SaleStatus = SaleStatus.ALL

    fun loadSalesList() {
        repository.getSellerFoodList { isSuccess, items, message ->
            if (!isSuccess) {
                _errorMessage.value = message ?: "판매 목록 조회에 실패했습니다"
                return@getSellerFoodList
            }

            allItems.clear()
            allItems.addAll(items.map { mapToSaleItem(it) })
            applyFilter()
        }
    }

    fun loadBuyerNearbySalesList(userLat: Double, userLng: Double, searchRadiusMeter: Float) {
        repository.getNearbyOnSaleFoodList(userLat, userLng, searchRadiusMeter) { isSuccess, items, message ->
            if (!isSuccess) {
                _errorMessage.value = message ?: "주변 판매 상품 조회에 실패했습니다"
                return@getNearbyOnSaleFoodList
            }

            allItems.clear()
            allItems.addAll(items.map { mapToSaleItem(it) })
            currentFilter = SaleStatus.ON_SALE
            applyFilter()
        }
    }

    fun clearSalesList() {
        allItems.clear()
        applyFilter()
    }

    fun filterByStatus(status: SaleStatus) {
        currentFilter = status
        applyFilter()
    }

    fun updateItemStatus(id: String, newStatus: SaleStatus) {
        repository.updateFoodStatus(id, newStatus.toFirestoreStatus()) { isSuccess, message ->
            if (!isSuccess) {
                _errorMessage.value = message ?: "상태 변경에 실패했습니다"
                return@updateFoodStatus
            }

            val index = allItems.indexOfFirst { it.id == id }
            if (index != -1) {
                allItems[index] = allItems[index].copy(status = newStatus)
                applyFilter()
            }
        }
    }

    fun deleteItem(id: String) {
        repository.deleteFoodItem(id) { isSuccess, message ->
            if (!isSuccess) {
                _errorMessage.value = message ?: "삭제에 실패했습니다"
                return@deleteFoodItem
            }

            allItems.removeAll { it.id == id }
            applyFilter()
        }
    }

    private fun applyFilter() {
        _salesList.value = if (currentFilter == SaleStatus.ALL) {
            allItems.toList()
        } else {
            allItems.filter { it.status == currentFilter }
        }
    }

    private fun mapToSaleItem(item: SellerFoodItem): SaleItem {
        return SaleItem(
            id = item.id,
            title = item.foodName,
            price = item.price.toInt(),
            category = item.category.toProductCategory(),
            imageUrl = null,
            stockCount = 0,
            status = item.status.toSaleStatus(),
            createdAt = item.timestamp
        )
    }

    private fun String.toProductCategory(): ProductCategory {
        return when {
            contains("육") -> ProductCategory.MEAT
            else -> ProductCategory.VEGETABLE
        }
    }

    private fun String.toSaleStatus(): SaleStatus {
        return when (trim()) {
            "판매중" -> SaleStatus.ON_SALE
            "판매완료", "완료", "품절" -> SaleStatus.SOLD_OUT
            else -> SaleStatus.ON_SALE
        }
    }

    private fun SaleStatus.toFirestoreStatus(): String {
        return when (this) {
            SaleStatus.ON_SALE -> "판매중"
            SaleStatus.SOLD_OUT -> "판매완료"
            SaleStatus.ALL -> "판매중"
        }
    }
}
