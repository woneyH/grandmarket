package com.pbl.grandmarket_android

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pbl.grandmarket_android.databinding.ItemSaleBinding

// ── 데이터 클래스 ──────────────────────────────────────

data class SaleItem(
    val id: String,
    val title: String,
    val price: Int,
    val category: ProductCategory,
    val imageUrl: String?,
    val stockCount: Int,
    val status: SaleStatus,
    val createdAt: Long
)

enum class ProductCategory {
    VEGETABLE,  // 채소
    MEAT        // 육류
}

enum class SaleStatus {
    ALL, ON_SALE, RESERVED, SOLD_OUT
}

// ── DiffUtil ───────────────────────────────────────────

class SaleItemDiffCallback : DiffUtil.ItemCallback<SaleItem>() {
    override fun areItemsTheSame(oldItem: SaleItem, newItem: SaleItem) =
        oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: SaleItem, newItem: SaleItem) =
        oldItem == newItem
}

// ── Adapter ────────────────────────────────────────────

class SalesAdapter(
    private val onItemClick: (SaleItem) -> Unit,
    private val onMoreClick: (SaleItem) -> Unit
) : ListAdapter<SaleItem, SalesAdapter.SaleViewHolder>(SaleItemDiffCallback()) {

    inner class SaleViewHolder(
        private val binding: ItemSaleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SaleItem) {

            // 상품명 / 가격 / 재고 / 날짜
            binding.tvProductName.text = item.title
            binding.tvPrice.text       = "%,d원".format(item.price)
            binding.tvStock.text       = "재고 ${item.stockCount}개"
            binding.tvDate.text        = formatRelativeTime(item.createdAt)

            // 카테고리 태그
            when (item.category) {
                ProductCategory.VEGETABLE -> {
                    binding.tvCategory.text = "채소"
                    binding.tvCategory.setTextColor(Color.parseColor("#C05E00"))
                    binding.tvCategory.setBackgroundResource(R.drawable.bg_item_list_veg)
                }
                ProductCategory.MEAT -> {
                    binding.tvCategory.text = "육류"
                    binding.tvCategory.setTextColor(Color.parseColor("#B71C1C"))
                    binding.tvCategory.setBackgroundResource(R.drawable.bg_item_list_meat)
                }
            }

            // 상태 dot 색상 + 텍스트
            val (dotColor, statusText, statusTextColor) = when (item.status) {
                SaleStatus.ON_SALE  -> Triple("#52B788", "판매중",  "#2D6A4F")
                SaleStatus.RESERVED -> Triple("#F4A261", "예약중",  "#C05E00")
                SaleStatus.SOLD_OUT -> Triple("#BDBDBD", "판매완료", "#9E9E9E")
                SaleStatus.ALL      -> Triple("#BDBDBD", "",        "#9E9E9E")
            }
            binding.viewStatusDot.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(dotColor))
            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(Color.parseColor(statusTextColor))

            // TODO: Glide / Coil 이미지 로드
            // Glide.with(binding.root)
            //     .load(item.imageUrl)
            //     .placeholder(R.drawable.ic_image_placeholder)
            //     .into(binding.ivProductImage)

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnMore.setOnClickListener { onMoreClick(item) }
        }

        private fun formatRelativeTime(timestamp: Long): String {
            val diff    = System.currentTimeMillis() - timestamp
            val minutes = diff / 60_000
            val hours   = minutes / 60
            val days    = hours / 24
            return when {
                minutes < 1  -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                hours   < 24 -> "${hours}시간 전"
                days    < 7  -> "${days}일 전"
                else         -> "${days / 7}주 전"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        SaleViewHolder(
            ItemSaleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) =
        holder.bind(getItem(position))
}
