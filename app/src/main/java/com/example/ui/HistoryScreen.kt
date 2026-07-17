package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    var selectedLangFilter by remember { mutableStateOf("All") } // All, EN, ZH, JA

    // Curated overseas shopping glossary dictionary
    val glossary = listOf(
        GlossaryItem("ZH", "包邮 (Bāo yóu)", "Bao bưu", "Miễn phí vận chuyển nội địa (Cực kỳ quan trọng trên Taobao/1688)."),
        GlossaryItem("ZH", "客服 (Kè fù)", "Khách phục", "Bộ phận chăm sóc khách hàng/Hỗ trợ chat trực tuyến."),
        GlossaryItem("ZH", "库存 (Kù cún)", "Khố tồn", "Hàng tồn kho hiện có của sản phẩm."),
        GlossaryItem("ZH", "秒杀 (Miǎo shā)", "Miểu sát", "Giờ vàng giá sốc (Flash sale giảm giá cực mạnh)."),
        GlossaryItem("ZH", "评价 (Píng jià)", "Bình giá", "Đánh giá, nhận xét thực tế kèm hình ảnh từ người mua."),
        GlossaryItem("ZH", "尺码表 (Chǐ mǎ biǎo)", "Xích mã biểu", "Bảng kích cỡ (Size chart) chi tiết của quần áo, giày dép."),
        
        GlossaryItem("EN", "Add to Cart", "Thêm vào giỏ", "Lưu sản phẩm vào giỏ hàng để gom đơn thanh toán sau."),
        GlossaryItem("EN", "Out of Stock", "Hết hàng", "Sản phẩm tạm thời không còn hàng trong kho."),
        GlossaryItem("EN", "Pre-order", "Đặt hàng trước", "Hàng chưa có sẵn, phải sản xuất hoặc đợi ngày mở bán."),
        GlossaryItem("EN", "Billing Address", "Địa chỉ hóa đơn", "Địa chỉ khai báo đăng ký thẻ thanh toán quốc tế."),
        GlossaryItem("EN", "Seller Rating", "Điểm uy tín", "Điểm đánh giá sao của người bán dựa trên lịch sử giao dịch."),
        
        GlossaryItem("JA", "送料無料 (Muryō sōryō)", "Vô liêu tống liêu", "Miễn phí vận chuyển nội địa Nhật Bản."),
        GlossaryItem("JA", "在庫切れ (Zaikogire)", "Tại khố thiết", "Sản phẩm đã hết hàng trong kho."),
        GlossaryItem("JA", "タイムセール (Taimu sēru)", "Time sale", "Khuyến mãi giới hạn thời gian (Săn Flash Sale)."),
        GlossaryItem("JA", "カートに入れる (Kāto ni ireru)", "Thêm giỏ hàng", "Bỏ sản phẩm vào giỏ hàng mua sắm."),
        GlossaryItem("JA", "お気に入り (Oki ni iri)", "Khả ái", "Thêm sản phẩm vào mục yêu thích để theo dõi giá.")
    )

    val filteredGlossary = if (selectedLangFilter == "All") {
        glossary
    } else {
        glossary.filter { it.lang == selectedLangFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Screen Header
        Text(
            text = "Cẩm Nang & Từ Điển AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Tra cứu thuật ngữ e-commerce nước ngoài và kinh nghiệm mua sắm quốc tế",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Guide Cards Title
        Text(
            text = "Kinh Nghiệm Mua Hàng Quốc Tế",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Tips List
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TipCard(
                icon = "👑",
                title = "Cách xem độ uy tín Shop Taobao",
                desc = "Độ uy tín xếp từ thấp đến cao: Trái tim -> Kim cương -> Vương miện xanh -> Vương miện vàng. Chỉ nên mua ở Shop có từ 3 Kim Cương trở lên để tránh hàng kém chất lượng.",
                color = Color(0xFFFF9800)
            )

            TipCard(
                icon = "📦",
                title = "Kinh nghiệm tránh hàng cấm biên",
                desc = "Khi ký gửi về Việt Nam, hãy tránh đặt các mặt hàng thuộc danh mục cấm bay: Pin lithium rời, bình xịt nén khí, chất lỏng dễ cháy, thuốc súng, dao kéo sắc nhọn.",
                color = Color(0xFFE91E63)
            )

            TipCard(
                icon = "⭐",
                title = "Đọc đánh giá thực tế (Review)",
                desc = "Luôn lọc xem các bình luận 1 sao và 2 sao trước. Trên Amazon hãy tìm từ khóa 'Verified Purchase' (Mua hàng đã xác thực) để đọc đúng nhận xét của người dùng thật.",
                color = Color(0xFF4CAF50)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Glossary Dictionary Section
        Text(
            text = "Sổ Tay Thuật Ngữ E-Commerce",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Lang filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All" to "Tất cả", "EN" to "🇺🇸 English", "ZH" to "🇨🇳 Trung Quốc", "JA" to "🇯🇵 Nhật Bản").forEach { (filter, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selectedLangFilter == filter) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { selectedLangFilter = filter }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (selectedLangFilter == filter) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Glossary Cards List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("glossary_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredGlossary.forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.term,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Badge of language
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (item.lang) {
                                            "EN" -> Color(0xFFE3F2FD)
                                            "ZH" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFF3E5F5)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (item.lang) {
                                        "EN" -> "English"
                                        "ZH" -> "Taobao"
                                        else -> "Rakuten"
                                    },
                                    fontSize = 9.sp,
                                    color = when (item.lang) {
                                        "EN" -> Color(0xFF1E88E5)
                                        "ZH" -> Color(0xFFFB8C00)
                                        else -> Color(0xFF8E24AA)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (item.sinoViet.isNotEmpty()) {
                            Text(
                                text = "Hán Việt: ${item.sinoViet}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 Nghĩa: ${item.meaning}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.desc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TipCard(
    icon: String,
    title: String,
    desc: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

data class GlossaryItem(
    val lang: String,
    val term: String,
    val sinoViet: String,
    val meaning: String,
    val desc: String = ""
)
