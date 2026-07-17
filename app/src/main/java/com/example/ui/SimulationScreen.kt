package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isOverlayServiceActive by remember { mutableStateOf(false) }
    var selectedMockScreen by remember { mutableStateOf("Taobao") } // Taobao, Amazon, JpStore
    
    // Check overlay status on compose
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isOverlayServiceActive = Settings.canDrawOverlays(context)
        } else {
            isOverlayServiceActive = false
        }
    }
    
    // Scan states
    var isScanning by remember { mutableStateOf(false) }
    var isTranslated by remember { mutableStateOf(false) }

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
            text = "Giả Lập Dịch Nổi Ngoài App",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Trải nghiệm tính năng chụp màn hình, quét OCR và dịch đè (Overlay) lên bất kỳ ứng dụng nào",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Overlay Service Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("overlay_service_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (isOverlayServiceActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isOverlayServiceActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(0.7f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOverlayServiceActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterFrames,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bong Bóng Dịch Nổi (Overlay)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isOverlayServiceActive) "Đang kích hoạt • Chạy ngầm" else "Chưa kích hoạt",
                            fontSize = 12.sp,
                            color = if (isOverlayServiceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Switch(
                    checked = isOverlayServiceActive,
                    onCheckedChange = { active ->
                        if (active) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                Toast.makeText(context, "Vui lòng cấp quyền Vẽ lên ứng dụng khác để bật bong bóng dịch nhé!", Toast.LENGTH_LONG).show()
                            } else {
                                isOverlayServiceActive = true
                                val serviceIntent = Intent(context, FloatingBubbleService::class.java)
                                context.startService(serviceIntent)
                                Toast.makeText(context, "Đã kích hoạt Bong Bóng Dịch Nổi!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            isOverlayServiceActive = false
                            val serviceIntent = Intent(context, FloatingBubbleService::class.java)
                            context.stopService(serviceIntent)
                            isTranslated = false
                        }
                    },
                    modifier = Modifier.testTag("overlay_switch")
                )
            }
        }

        Text(
            text = "LỰA CHỌN MÀN HÌNH GIẢ LẬP",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Screen selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Taobao" to "🇨🇳 Taobao App", "Amazon" to "🇺🇸 Amazon App", "JpStore" to "🇯🇵 Rakuten JP").forEach { (screen, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedMockScreen == screen) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selectedMockScreen == screen) MaterialTheme.colorScheme.secondary
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { 
                            selectedMockScreen = screen 
                            isTranslated = false
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Playground simulation Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(vertical = 16.dp)
                .testTag("simulation_canvas_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            border = BorderStroke(4.dp, Color(0xFF333333)) // Mobile phone frame border
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mock App Screen Content
                when (selectedMockScreen) {
                    "Taobao" -> MockTaobaoScreen(isTranslated = isTranslated)
                    "Amazon" -> MockAmazonScreen(isTranslated = isTranslated)
                    "JpStore" -> MockRakutenScreen(isTranslated = isTranslated)
                }

                // AI Sweep Scan Laser
                if (isScanning) {
                    LaserOverlayEffect()
                }

                // Float Balloon Overlay (Bóng dịch nổi)
                if (isOverlayServiceActive) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = EaseInOutQuad),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size((52 * pulseScale).dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00E676),
                                        Color(0xFF00B0FF)
                                    )
                                )
                            )
                            .clickable {
                                if (!isScanning) {
                                    scope.launch {
                                        isScanning = true
                                        isTranslated = false
                                        delay(1500)
                                        isScanning = false
                                        isTranslated = true
                                    }
                                }
                            }
                            .testTag("floating_balloon_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Translate Screen Overlay",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Non-active Prompt info
                if (!isOverlayServiceActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 8.dp)
                            )
                            Text(
                                text = "BẬT BONG BÓNG ĐỂ BẮT ĐẦU",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gạt nút công tắc phía trên để kích hoạt bong bóng dịch nổi. Sau đó bấm vào bong bóng để quét dịch màn hình điện thoại này nhé!",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else if (!isTranslated && !isScanning) {
                    // Tap indicator tip
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "👉 Bấm vào bong bóng dịch màu xanh bên phải!",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Educational details
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Cách thức hoạt động thực tế trên điện thoại:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                BulletText(text = "1. Sử dụng quyền MediaProjection để chụp ảnh màn hình hiện tại.")
                BulletText(text = "2. Sử dụng thư viện ML Kit OCR hoặc Gemini Vision để quét chữ quốc tế.")
                BulletText(text = "3. Sử dụng quyền SYSTEM_ALERT_WINDOW vẽ đè nhãn tiếng Việt trùng khớp tọa độ.")
            }
        }
    }
}

@Composable
fun BulletText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
fun LaserOverlayEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.03f)
                .align(Alignment.TopCenter)
                .fillMaxHeight(laserYOffset)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x3300E676),
                            Color(0xFF00E676),
                            Color(0x3300E676),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// --- MOCK APP SCREENS ---

@Composable
fun MockTaobaoScreen(isTranslated: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F4F4))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                // Taobao branding logo badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF5000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("淘宝 Taobao", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Price Line
                Text(
                    text = if (isTranslated) "~ 448.000 VND" else "¥ 128.00",
                    color = if (isTranslated) Color(0xFF4CAF50) else Color(0xFFFF5000),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = if (isTranslated) "Áo Khoác Blazer Nữ Form Rộng Phong Cách Hàn Quốc Thu Đông Hàng Cao Cấp" 
                           else "韩版宽松显瘦百搭双排扣西装外套女网红春秋季高级感上衣潮",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Size Options section
                Text(
                    text = if (isTranslated) "PHÂN LOẠI SIZE (GỢI Ý KÈM CÂN NẶNG)" else "尺码规格",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        if (isTranslated) "M (45-52kg)" else "M码 (90-105斤)",
                        if (isTranslated) "L (52-60kg)" else "L码 (105-120斤)"
                    ).forEach { size ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(size, fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFF9000))
                        .clickable {}
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTranslated) "THÊM GIỎ HÀNG" else "加入购物车",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFF5000))
                        .clickable {}
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTranslated) "MUA NGAY" else "立即购买",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MockAmazonScreen(isTranslated: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Amazon Nav bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF232F3E))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "amazon",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Image Product
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFF9F9F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Laptop,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(72.dp)
                )
            }

            // Info details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isTranslated) "Máy Tính Xách Tay Ultrabook 14 Inch Mỏng Nhẹ, Pin Khủng 12 Giờ"
                           else "Premium Thin & Light 14\" Ultrabook Laptop - 12 Hour Battery Life",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stars
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1,248 ratings", fontSize = 11.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (isTranslated) "~ 12.670.000 VND" else "${'$'}499.00",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFB12704)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTranslated) "Đã tính thuế nhập khẩu Mỹ" else "List Price: ${'$'}599.00",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shipping info
                Text(
                    text = if (isTranslated) "✈️ Giao hàng miễn phí tới Việt Nam nếu đơn hàng trên $49"
                           else "✈️ FREE Shipping to Vietnam on orders over ${'$'}49",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF007600)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFFD814))
                    .clickable {},
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTranslated) "MUA NGAY (GIAO VỀ VIỆT NAM)" else "Buy Now",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun MockRakutenScreen(isTranslated: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFBF0000))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rakuten 楽天市場", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
            }

            // Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Title
                Text(
                    text = if (isTranslated) "Đồng Hồ Cơ Automatic Sang Trọng Chống Nước Made in Japan"
                           else "日本製 自動巻き 機械式腕時計 メンズ サファイaガラス 5気圧防水",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price
                Text(
                    text = if (isTranslated) "~ 4.125.000 VND" else "25,000 円",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFBF0000)
                )
                
                Spacer(modifier = Modifier.height(10.dp))

                // Specs table
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isTranslated) "Xuất xứ" else "原産国", fontSize = 11.sp, color = Color.Gray)
                            Text(if (isTranslated) "Nhật Bản (Made in Japan)" else "日本", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isTranslated) "Chống nước" else "防水性", fontSize = 11.sp, color = Color.Gray)
                            Text(if (isTranslated) "Chống nước 50m (5 Bar)" else "5気圧防水", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFBF0000))
                    .clickable {},
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTranslated) "THÊM VÀO GIỎ HÀNG" else "買い物かごに入れる",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
