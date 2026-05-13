package com.pbl.grandmarket_android.ui.item_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.data.remote.ApiProductService
import com.pbl.grandmarket_android.data.repository.FoodRepository
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterItemBottomSheet : BottomSheetDialogFragment() {

    private lateinit var api: ApiProductService
    private var currentWholesalePrice = 0L // 불러온 도매가 저장용

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_item_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰
        val etSearchItem = view.findViewById<EditText>(R.id.etSearchItem)
        val btnSearch = view.findViewById<Button>(R.id.btnSearch)
        val tvWholesalePriceInfo = view.findViewById<TextView>(R.id.tvWholesalePriceInfo)
        val etSellPrice = view.findViewById<EditText>(R.id.etSellPrice)
        val btnMinusPrice = view.findViewById<Button>(R.id.btnMinusPrice)
        val btnPlusPrice = view.findViewById<Button>(R.id.btnPlusPrice)
        val btnRegisterComplete = view.findViewById<Button>(R.id.btnRegisterComplete)

        // API 세팅
        val retrofit = Retrofit.Builder()
            .baseUrl(com.pbl.grandmarket_android.BuildConfig.SERVER_IP)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(ApiProductService::class.java)

        // 도매가 검색 버튼 클릭
        btnSearch.setOnClickListener {
            val itemName = etSearchItem.text.toString()

            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etSearchItem.windowToken,0)

            if (itemName.isNotEmpty()) {
                tvWholesalePriceInfo.text = "조회 중..."
                lifecycleScope.launch {
                    try {
                        val response = api.getAveragePrice(itemName)
                        if (response.isSuccessful) {
                            currentWholesalePrice = response.body() ?: 0L
                            tvWholesalePriceInfo.text = "오늘의 $itemName 도매가: ${currentWholesalePrice}원"
                            etSellPrice.setText(currentWholesalePrice.toString()) // 초기 가격 세팅
                        } else {
                            tvWholesalePriceInfo.text = "도매가 정보를 찾을 수 없습니다."
                        }
                    } catch (e: Exception) {
                        tvWholesalePriceInfo.text = "네트워크 오류"
                    }
                }
            }
        }

        // 가격 조절 버튼 클릭
        btnMinusPrice.setOnClickListener {
            var price = etSellPrice.text.toString().toLongOrNull() ?: 0L
            if (price >= 100) price -= 100
            etSellPrice.setText(price.toString())
        }

        btnPlusPrice.setOnClickListener {
            var price = etSellPrice.text.toString().toLongOrNull() ?: 0L
            price += 100
            etSellPrice.setText(price.toString())
        }

        // 파이어베이스에 최종 등록 버튼 클릭
        btnRegisterComplete.setOnClickListener {
            val name = etSearchItem.text.toString()
            val sellPrice = etSellPrice.text.toString().toLongOrNull() ?: 0L

            if (name.isNotEmpty() && sellPrice > 0) {
                // 중복 클릭 방지
                btnRegisterComplete.isEnabled = false
                btnRegisterComplete.text = "등록 중..."

                // Repository 호출 (콜백 함수 전달)
                val repository = FoodRepository()
                repository.foodAddWithValidation(name, sellPrice, currentWholesalePrice) { isSuccess, message ->

                    // 결과가 돌아오면 UI 복구
                    btnRegisterComplete.isEnabled = true
                    btnRegisterComplete.text = "내 점포에 상품 등록하기"

                    // 콜백 결과(진동벨)에 따른 처리
                    if (isSuccess) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        dismiss() // 팝업창 닫기
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        // 실패 시에는 창을 닫지 않고 사용자가 다시 시도하거나 정보를 수정할 수 있게 둡니다.
                    }
                }
            } else {
                Toast.makeText(requireContext(), "상품명과 가격을 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}