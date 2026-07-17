package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GeminiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeScreen(
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("Shoes") } // Shoes, Tops, Bottoms

    // Input States for AI Advisor
    var heightInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") } // e.g. foot length or waist size
    var targetStandard by remember { mutableStateOf("CN") } // US, EU, CN, JP

    // Output States
    var aiLoading by remember { mutableStateOf(false) }
    var aiRecommendation by remember { mutableStateOf<String?>(null) }

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
            text = "Tra Cứu & Tư Vấn Size AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Chọn đúng kích thước US, EU, Trung Quốc mà không lo bị chật hay rộng",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Tab Selector for Categories
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Shoes" to "👟 Giày Dép", "Tops" to "👕 Áo Thun/Khoác", "Bottoms" to "👖 Quần/Váy nữ").forEach { (type, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedCategory == type) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { selectedCategory = type }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (selectedCategory == type) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Reference Charts (Offline Database)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("size_chart_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BẢNG QUY ĐỔI CHUẨN THAM KHẢO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                when (selectedCategory) {
                    "Shoes" -> {
                        SizeRow(cols = listOf("Foot (cm)", "VN / EU", "US Nam", "US Nữ"))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SizeRow(cols = listOf("24.0 cm", "38.5", "6.0", "7.5"))
                        SizeRow(cols = listOf("24.5 cm", "39", "6.5", "8.0"))
                        SizeRow(cols = listOf("25.0 cm", "40", "7.0", "8.5"))
                        SizeRow(cols = listOf("25.5 cm", "41", "8.0", "9.5"))
                        SizeRow(cols = listOf("26.0 cm", "42", "8.5", "10.0"))
                        SizeRow(cols = listOf("26.5 cm", "42.5", "9.0", "10.5"))
                        SizeRow(cols = listOf("27.0 cm", "43", "9.5", "11.0"))
                    }
                    "Tops" -> {
                        SizeRow(cols = listOf("Chiều cao", "Cân nặng", "Size VN", "Size US/EU"))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SizeRow(cols = listOf("1m50 - 1m60", "45 - 53kg", "S", "XS"))
                        SizeRow(cols = listOf("1m60 - 1m68", "53 - 60kg", "M", "S"))
                        SizeRow(cols = listOf("1m65 - 1m72", "60 - 68kg", "L", "M"))
                        SizeRow(cols = listOf("1m70 - 1m78", "68 - 76kg", "XL", "L"))
                        SizeRow(cols = listOf("1m75 - 1m83", "76 - 85kg", "XXL", "XL"))
                    }
                    "Bottoms" -> {
                        SizeRow(cols = listOf("Vòng bụng (cm)", "Cân nặng", "Size VN", "Size Quảng Châu"))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SizeRow(cols = listOf("60 - 64 cm", "40 - 45kg", "26 / S", "S (26)"))
                        SizeRow(cols = listOf("64 - 68 cm", "45 - 50kg", "27 / M", "M (27)"))
                        SizeRow(cols = listOf("68 - 72 cm", "50 - 55kg", "28 / L", "L (28)"))
                        SizeRow(cols = listOf("72 - 76 cm", "55 - 60kg", "29 / XL", "XL (29)"))
                        SizeRow(cols = listOf("76 - 80 cm", "60 - 65kg", "30 / XXL", "2XL (30)"))
                    }
                }
            }
        }

        // AI Size Advisor Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Trợ Lý Đo Size Bằng AI",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // AI Advisor Config Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Điền thông số cơ thể để AI đưa ra tư vấn size chuẩn xác nhất trên Amazon hoặc Taobao:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Height & Weight Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Chiều cao (cm)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_height_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Cân nặng (kg)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("size_weight_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detail and Target Standard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = detailInput,
                        onValueChange = { detailInput = it },
                        label = { Text("Số đo khác (Chiều dài chân, v.v.)") },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("size_detail_input"),
                        singleLine = true,
                        placeholder = { Text("Ví dụ: Chân dài 25.5cm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )

                    // Target Standard Selection
                    Column(
                        modifier = Modifier.weight(0.8f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Chuẩn Quốc gia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("US", "EU", "CN", "JP").forEach { std ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (targetStandard == std) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { targetStandard = std }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = std,
                                        fontSize = 11.sp,
                                        color = if (targetStandard == std) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button
                Button(
                    onClick = {
                        scope.launch {
                            aiLoading = true
                            aiRecommendation = null
                            
                            val prompt = """
                                Tôi muốn mua một món đồ thời trang quốc tế chuẩn size $targetStandard.
                                Thông số cơ thể của tôi là:
                                - Chiều cao: ${heightInput.ifEmpty { "Không cung cấp" }} cm
                                - Cân nặng: ${weightInput.ifEmpty { "Không cung cấp" }} kg
                                - Ghi chú số đo khác: ${detailInput.ifEmpty { "Không cung cấp" }}
                                
                                Hãy tư vấn chi tiết cho tôi:
                                1. Nên chọn size gì theo chuẩn size $targetStandard cho các loại đồ (Giày, Áo thun, Áo khoác, Quần/Váy)?
                                2. So sánh size này với size Việt Nam tiêu chuẩn để tôi dễ hình dung.
                                3. Đưa ra 2 lời khuyên đặc biệt quan trọng khi mua đồ của chuẩn $targetStandard (ví dụ: form đồ rộng hơn hay hẹp hơn form Châu Á).
                                
                                Hãy trả lời ngắn gọn, thân thiện, rõ ràng bằng Tiếng Việt. Chia các phần bằng bullet point đẹp mắt.
                            """.trimIndent()

                            val result = if (GeminiService.isApiKeyConfigured()) {
                                try {
                                    GeminiService.translateText(prompt, "Vietnamese", "Vietnamese")
                                } catch (e: Exception) {
                                    mockSizeRecommendation(targetStandard, heightInput, weightInput)
                                }
                            } else {
                                mockSizeRecommendation(targetStandard, heightInput, weightInput)
                            }
                            
                            aiRecommendation = result
                            aiLoading = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_size_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (aiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Phân Tích & Tư Vấn Size Bằng AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // AI Output Panel
                if (aiRecommendation != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_size_output_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Đề Xuất Từ Trợ Lý AI:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiRecommendation ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SizeRow(cols: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        cols.forEachIndexed { idx, text ->
            Text(
                text = text,
                fontSize = if (idx == 0) 13.sp else 12.sp,
                fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                color = if (idx == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = if (idx == 0) TextAlign.Start else TextAlign.Center
            )
        }
    }
}

/**
 * Highly customized local size recommendation fallback
 */
private fun mockSizeRecommendation(std: String, heightStr: String, weightStr: String): String {
    val h = heightStr.toIntOrNull() ?: 170
    val w = weightStr.toIntOrNull() ?: 62

    val sizeStr = when {
        w < 52 -> "Size S (Chuẩn US: XS, Chuẩn CN/JP: M)"
        w in 52..61 -> "Size M (Chuẩn US: S, Chuẩn CN/JP: L)"
        w in 62..70 -> "Size L (Chuẩn US: M, Chuẩn CN/JP: XL)"
        w in 71..80 -> "Size XL (Chuẩn US: L, Chuẩn CN/JP: 2XL)"
        else -> "Size XXL (Chuẩn US: XL, Chuẩn CN/JP: 3XL)"
    }

    val stdName = when (std) {
        "US" -> "Mỹ (US)"
        "EU" -> "Châu Âu (EU)"
        "CN" -> "Quảng Châu / Trung Quốc (CN)"
        "JP" -> "Nhật Bản (JP)"
        else -> std
    }

    return """
        💡 Dựa trên chiều cao ${h}cm và cân nặng ${w}kg của bạn, trợ lý gợi ý kích thước phù hợp theo chuẩn **$stdName**:
        
        *   👟 **Giày Dép**: Đề xuất size **${if (h > 165) "41 - 42" else "37 - 38"}** (Chuẩn US Nam tương đương: **${if (h > 165) "8.5" else "5.5"}**).
        *   👕 **Áo thun/Áo khoác**: Đề xuất size **$sizeStr**.
        *   👖 **Quần dài**: Bạn vừa vặn nhất với size **${if (w > 65) "31 - 32" else "28 - 29"}**.
        
        ⚠️ **Lời khuyên mua hàng quan trọng**:
        1.  ${if (std == "CN") "Quần áo Trung Quốc nội địa thường may theo phom Châu Á ôm sát (Slim fit). Do đó, bạn hãy đặt TĂNG lên +1 size nếu thích mặc thoải mái hoặc oversize dạo phố." else "Phom quần áo Âu Mỹ (US/EU) rộng hơn phom Châu Á rất nhiều. Bạn nên GIẢM đi -1 size so với size Việt Nam thường mặc để vừa khít cơ thể."}
        2.  Hãy kiểm tra kỹ phần phản hồi khách hàng (Reviews) trên trang web xem có nhiều người nhận xét sản phẩm bị co rút sau khi giặt hoặc bị lệch size thực tế hay không.
    """.trimIndent()
}
