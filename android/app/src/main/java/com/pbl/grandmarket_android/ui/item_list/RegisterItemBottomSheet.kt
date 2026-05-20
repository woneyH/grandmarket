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
    private var initialItemName: String? = null // AI 인식 결과로 전달된 기본 식자재명

    companion object {
        private const val ARG_ITEM_NAME = "arg_item_name"

        // AI 인식 결과를 기본 검색어로 전달하기 위한 팩토리 메서드
        fun newInstance(itemName: String?): RegisterItemBottomSheet {
            val fragment = RegisterItemBottomSheet()
            fragment.arguments = Bundle().apply {
                putString(ARG_ITEM_NAME, itemName)
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialItemName = arguments?.getString(ARG_ITEM_NAME)
    }

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

        // AI 인식 결과가 있으면 자동으로 검색어를 채워줌
        initialItemName?.takeIf { it.isNotBlank() }?.let { itemName ->
            etSearchItem.setText(itemName)
            btnSearch.performClick()
        }

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

                // Repository 호출
                val repository = FoodRepository()
                repository.foodAddWithValidation(name, sellPrice, currentWholesalePrice) { isSuccess, message ->

                    // 결과가 돌아오면 UI 복구
                    btnRegisterComplete.isEnabled = true
                    btnRegisterComplete.text = "내 점포에 상품 등록하기"

                    if (isSuccess) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "상품명과 가격을 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
