package com.pbl.grandmarket_android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.pbl.grandmarket_android.databinding.FragmentSellListBinding
import androidx.fragment.app.viewModels

class SellListFragment: Fragment() {
    private var _binding: FragmentSellListBinding? = null

    private val binding get() = _binding!!
    private val viewModel: SalesListViewModel by viewModels()
    private lateinit var salesAdapter: SalesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilterTabs()
        setupFab()
        observeViewModel()
    }


    private fun setupRecyclerView() {
        salesAdapter = SalesAdapter(
            onItemClick = { item ->
                // TODO: 상품 상세 화면으로 이동
            },
            onMoreClick = { item ->
                showItemOptionsMenu(item)
            }
        )
        binding.rvSalesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter
        }

    }

    private fun setupFilterTabs() {
        binding.rgFilter.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            val filter = when (checkedId) {
                R.id.rbOnSale -> SaleStatus.ON_SALE
                R.id.rbReserved -> SaleStatus.RESERVED
                R.id.rbSoldOut -> SaleStatus.SOLD_OUT
                else -> SaleStatus.ALL  // rbAll
            }
            viewModel.filterByStatus(filter)
        }
    }


    private fun setupFab() {
        binding.fabAddProduct.setOnClickListener {
            // TODO: 상품 등록 화면으로 이동
        }
    }

    private fun observeViewModel() {
        viewModel.salesList.observe(viewLifecycleOwner) { items ->
            salesAdapter.submitList(items)
            val isEmpty = items.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvSalesList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.tvItemCount.text = "등록된 상품 ${items.size}개"
        }
    }

    private fun showItemOptionsMenu(item: SaleItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setItems(arrayOf("수정", "상태 변경", "삭제")) { _, which ->
                when (which) {
                    0 -> { /* TODO: 수정 */
                    }

                    1 -> showStatusChangeDialog(item)
                    2 -> viewModel.deleteItem(item.id)
                }
            }
            .show()
    }

    private fun showStatusChangeDialog(item: SaleItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("상태 변경")
            .setItems(arrayOf("판매중", "예약중", "판매완료")) { _, which ->
                val newStatus = when (which) {
                    0 -> SaleStatus.ON_SALE
                    1 -> SaleStatus.RESERVED
                    else -> SaleStatus.SOLD_OUT
                }
                viewModel.updateItemStatus(item.id, newStatus)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}