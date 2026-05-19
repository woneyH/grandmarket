package com.pbl.grandmarket_android.ui.item_list

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pbl.grandmarket_android.R
import com.pbl.grandmarket_android.data.local.UserSession
import com.pbl.grandmarket_android.data.model.UserRole
import com.pbl.grandmarket_android.ui.adapter.SaleStatus
import com.pbl.grandmarket_android.ui.adapter.SalesAdapter
import com.pbl.grandmarket_android.view_model.SalesListViewModel
import com.pbl.grandmarket_android.databinding.FragmentSellListBinding

class SellListFragment: Fragment() {
    private var _binding: FragmentSellListBinding? = null

    private val binding get() = _binding!!
    private val viewModel: SalesListViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var salesAdapter: SalesAdapter
    private var isSellerUser = true

    private val buyerSearchRadiusMeter = 10_000f

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocationGranted || coarseLocationGranted) {
                requestCurrentLocationAndLoadNearbyItems()
            } else {
                viewModel.clearSalesList()
                Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        isSellerUser = UserSession.getRole(requireContext()) == UserRole.SELLER

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
            onDeleteClick = { item -> viewModel.deleteItem(item.id) },
            onCompleteClick = { item -> viewModel.updateItemStatus(item.id, SaleStatus.SOLD_OUT) },
            isEditable = isSellerUser
        )
        binding.rvSalesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = salesAdapter
        }

    }

    private fun setupFilterTabs() {
        if (!isSellerUser) {
            binding.rgFilter.visibility = View.GONE
            return
        }
        binding.rgFilter.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            val filter = when (checkedId) {
                R.id.rbOnSale -> SaleStatus.ON_SALE
                R.id.rbSoldOut -> SaleStatus.SOLD_OUT
                else -> SaleStatus.ALL  // rbAll
            }
            viewModel.filterByStatus(filter)
        }
    }


    private fun setupFab() {
        if (!isSellerUser) {
            binding.fabAddProduct.visibility = View.GONE
            binding.fabAddProduct.isEnabled = false
            return
        }

        binding.fabAddProduct.setOnClickListener {
            RegisterItemBottomSheet().show(childFragmentManager, "RegisterItemBottomSheet")
        }
    }

    private fun observeViewModel() {
        viewModel.salesList.observe(viewLifecycleOwner) { items ->
            salesAdapter.submitList(items)
            val isEmpty = items.isEmpty()
            binding.layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvSalesList.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.tvItemCount.text = if (isSellerUser) {
                "등록된 상품 ${items.size}개"
            } else {
                binding.tvEmptyItemMessage.text = "주변에 살 수 있는 식자재가 없어요!"
                binding.tvEmptyItemMessage2.text = ""
                "주변 10km 판매 상품 ${items.size}개"
            }
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if (isSellerUser) {
            viewModel.loadSalesList()
        } else {
            checkLocationPermissionAndLoadNearbyItems()
        }
    }

    private fun checkLocationPermissionAndLoadNearbyItems() {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            requestCurrentLocationAndLoadNearbyItems()
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocationAndLoadNearbyItems() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location == null) {
                    viewModel.clearSalesList()
                    Toast.makeText(requireContext(), "현재 위치를 찾지 못했습니다.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                viewModel.loadBuyerNearbySalesList(
                    userLat = location.latitude,
                    userLng = location.longitude,
                    searchRadiusMeter = buyerSearchRadiusMeter
                )
            }
            .addOnFailureListener {
                viewModel.clearSalesList()
                Toast.makeText(requireContext(), "현재 위치를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
