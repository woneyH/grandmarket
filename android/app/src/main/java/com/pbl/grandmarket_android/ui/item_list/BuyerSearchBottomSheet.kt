package com.pbl.grandmarket_android.ui.item_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pbl.grandmarket_android.R

class BuyerSearchBottomSheet : BottomSheetDialogFragment() {

    private var onSearch: ((String) -> Unit)? = null
    private var onReset: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_buyer_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val keywordInput = view.findViewById<EditText>(R.id.etBuyerSearchKeyword)
        val searchButton = view.findViewById<AppCompatButton>(R.id.btnBuyerSearchSubmit)
        val resetButton = view.findViewById<AppCompatButton>(R.id.btnBuyerSearchReset)
        val initialKeyword = arguments?.getString(ARG_INITIAL_KEYWORD).orEmpty()
        val showResetButton = arguments?.getBoolean(ARG_SHOW_RESET, false) == true

        keywordInput.setText(initialKeyword)
        keywordInput.setSelection(keywordInput.text.length)
        resetButton.visibility = if (showResetButton) View.VISIBLE else View.GONE

        fun submitSearch() {
            onSearch?.invoke(keywordInput.text.toString().trim())
            dismiss()
        }

        searchButton.setOnClickListener { submitSearch() }
        resetButton.setOnClickListener {
            onReset?.invoke()
            dismiss()
        }
        keywordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else {
                false
            }
        }
    }

    fun setOnSearchListener(listener: (String) -> Unit): BuyerSearchBottomSheet {
        onSearch = listener
        return this
    }

    fun setOnResetListener(listener: () -> Unit): BuyerSearchBottomSheet {
        onReset = listener
        return this
    }

    companion object {
        private const val ARG_INITIAL_KEYWORD = "arg_initial_keyword"
        private const val ARG_SHOW_RESET = "arg_show_reset"

        fun newInstance(
            initialKeyword: String = "",
            showReset: Boolean = false
        ): BuyerSearchBottomSheet {
            return BuyerSearchBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_KEYWORD, initialKeyword)
                    putBoolean(ARG_SHOW_RESET, showReset)
                }
            }
        }
    }
}
