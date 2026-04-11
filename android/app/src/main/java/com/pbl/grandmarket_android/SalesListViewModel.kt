package com.pbl.grandmarket_android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SalesListViewModel : ViewModel() {

    private val _salesList = MutableLiveData<List<SaleItem>>()
    val salesList: LiveData<List<SaleItem>> = _salesList

    // 원본 전체 목록 (필터용)
    private val allItems = mutableListOf<SaleItem>()

    init {
        loadDummyData()
    }

    // 테스트용 더미 데이터 — 실제 구현 시 Firebase/Repository로 교체
    private fun loadDummyData() {
        allItems.addAll(
            listOf(
                SaleItem(
                    id         = "1",
                    title      = "국내산 배추",
                    price      = 3500,
                    category   = ProductCategory.VEGETABLE,
                    imageUrl   = null,
                    stockCount = 32,
                    status     = SaleStatus.ON_SALE,
                    createdAt  = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000L
                ),
                SaleItem(
                    id         = "2",
                    title      = "한우 등심",
                    price      = 42000,
                    category   = ProductCategory.MEAT,
                    imageUrl   = null,
                    stockCount = 5,
                    status     = SaleStatus.RESERVED,
                    createdAt  = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
                ),
                SaleItem(
                    id         = "3",
                    title      = "햇 양파",
                    price      = 5900,
                    category   = ProductCategory.VEGETABLE,
                    imageUrl   = null,
                    stockCount = 18,
                    status     = SaleStatus.ON_SALE,
                    createdAt  = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L
                ),
                SaleItem(
                    id         = "4",
                    title      = "국내산 삼겹살",
                    price      = 12800,
                    category   = ProductCategory.MEAT,
                    imageUrl   = null,
                    stockCount = 0,
                    status     = SaleStatus.SOLD_OUT,
                    createdAt  = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L
                )
            )
        )
        _salesList.value = allItems.toList()
    }

    // 필터 적용
    fun filterByStatus(status: SaleStatus) {
        _salesList.value = if (status == SaleStatus.ALL) {
            allItems.toList()
        } else {
            allItems.filter { it.status == status }
        }
    }

    // 상태 변경
    fun updateItemStatus(id: String, newStatus: SaleStatus) {
        val index = allItems.indexOfFirst { it.id == id }
        if (index != -1) {
            allItems[index] = allItems[index].copy(status = newStatus)
            _salesList.value = allItems.toList()
        }
    }

    // 삭제
    fun deleteItem(id: String) {
        allItems.removeAll { it.id == id }
        _salesList.value = allItems.toList()
    }
}
