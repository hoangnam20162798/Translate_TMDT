package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the Gemini API Key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Translates a simple text query or translates search terms for foreign e-commerce sites.
     */
    suspend fun translateText(text: String, sourceLang: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext mockTextTranslation(text, targetLang)
        }

        val prompt = "Translate the following e-commerce product title, term, or search query from $sourceLang to $targetLang. Keep it natural and relevant to online shopping. Only return the translated text directly, do not include any commentary, explanations, or quotes:\n\n$text"
        
        try {
            val jsonRequest = buildTextRequestBody(prompt)
            val request = Request.Builder()
                .url("$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}")
                .post(jsonRequest.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code: ${response.code}")
                    return@withContext "Lỗi: API trả về mã ${response.code}"
                }
                val bodyString = response.body?.string() ?: ""
                parseTextResponse(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Translation error", e)
            "Lỗi kết nối: ${e.localizedMessage}"
        }
    }

    /**
     * Takes an e-commerce webpage screenshot, runs Gemini multimodal vision model,
     * extracts details, translates options/specs, converts currency, and summarizes.
     */
    suspend fun translateShoppingScreenshot(bitmap: Bitmap, siteUrl: String): ShoppingTranslationResult = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            // Emulate based on site URL to make it highly immersive even without a key
            return@withContext generateSimulatedResult(siteUrl)
        }

        val base64Image = bitmapToBase64(bitmap)
        val prompt = """
            Bạn là một chuyên gia mua sắm quốc tế và trợ lý dịch thuật e-commerce thông minh. 
            Hãy phân tích hình ảnh chụp màn hình trang sản phẩm thương mại điện tử này (địa chỉ trang: $siteUrl) và thực hiện các yêu cầu sau:
            
            1. Tìm và dịch tên sản phẩm (Product Title) sang tiếng Việt một cách tự nhiên, hấp dẫn nhất.
            2. Trích xuất giá gốc của sản phẩm (bao gồm cả ký hiệu tiền tệ như $, ¥, €, £, v.v.).
            3. Quy đổi giá gốc sang tiền Việt Nam Đồng (VND) ước tính theo tỷ giá hiện thời (ví dụ: 1 USD ≈ 25,400 VND, 1 CNY ≈ 3,500 VND, 1 JPY ≈ 165 VND, v.v.).
            4. Dịch và liệt kê các phân loại sản phẩm quan trọng (Options/Variants như màu sắc, kích thước) hiển thị trên màn hình sang tiếng Việt.
            5. Trích xuất và dịch các thông số kỹ thuật (Specifications) hoặc chi tiết sản phẩm chính.
            6. Tóm tắt nhanh về sản phẩm và đưa ra lời khuyên mua sắm hữu ích cho người Việt (ví dụ: sản phẩm này có được đánh giá cao không, lưu ý gì khi chọn kích cỡ/màu sắc).
            
            Bạn PHẢI trả về kết quả định dạng JSON duy nhất, không kèm theo bất kỳ văn bản giải thích nào trước hoặc sau khối JSON đó. Khối JSON phải có cấu trúc chính xác như sau:
            {
              "productName": "Tên sản phẩm tiếng Việt",
              "originalPrice": "Giá gốc kèm đơn vị",
              "convertedPrice": "Giá VND (Ví dụ: ~1.250.000 VND)",
              "options": ["Màu đen - Size L", "Màu trắng - Size M"],
              "specs": {
                "Thương hiệu": "Ví dụ: Nike",
                "Chất liệu": "Ví dụ: 100% Polyester"
              },
              "summary": "Tóm tắt ngắn gọn đặc điểm nổi bật trong 1-2 câu",
              "advice": "Lời khuyên về kích cỡ, đánh giá người dùng hoặc lưu ý khi mua"
            }
        """.trimIndent()

        try {
            val jsonRequest = buildMultimodalRequestBody(prompt, base64Image)
            val request = Request.Builder()
                .url("$BASE_URL?key=${BuildConfig.GEMINI_API_KEY}")
                .post(jsonRequest.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ShoppingTranslationResult(
                        productName = "Lỗi dịch màn hình",
                        originalPrice = "Không rõ",
                        convertedPrice = "Không rõ",
                        options = listOf(),
                        specs = mapOf("Lỗi hệ thống" to "API trả về mã ${response.code}"),
                        summary = "Không thể kết nối với máy chủ AI. Vui lòng kiểm tra lại kết nối mạng.",
                        advice = "Hãy thử lại trong chốc lát."
                    )
                }
                val bodyString = response.body?.string() ?: ""
                parseShoppingResponse(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Screenshot translation error", e)
            ShoppingTranslationResult(
                productName = "Lỗi kết nối",
                originalPrice = "Không rõ",
                convertedPrice = "Không rõ",
                options = listOf(),
                specs = mapOf("Lỗi ngoại lệ" to (e.localizedMessage ?: "Unknown error")),
                summary = "Không thể liên lạc với Gemini API. Có lỗi xảy ra khi truyền tải hình ảnh.",
                advice = "Đảm bảo thiết bị của bạn có kết nối Internet hoạt động ổn định."
            )
        }
    }

    private fun buildTextRequestBody(prompt: String): String {
        val part = JSONObject().put("text", prompt)
        val content = JSONObject().put("parts", JSONArray().put(part))
        val contents = JSONArray().put(content)
        return JSONObject().put("contents", contents).toString()
    }

    private fun buildMultimodalRequestBody(prompt: String, base64Image: String): String {
        val promptPart = JSONObject().put("text", prompt)
        
        val inlineData = JSONObject()
            .put("mimeType", "image/jpeg")
            .put("data", base64Image)
        val imagePart = JSONObject().put("inlineData", inlineData)

        val parts = JSONArray().put(promptPart).put(imagePart)
        val content = JSONObject().put("parts", parts)
        val contents = JSONArray().put(content)
        
        // Instruct Gemini to return JSON
        val responseFormatText = JSONObject().put("mimeType", "application/json")
        val responseFormat = JSONObject().put("text", responseFormatText)
        val generationConfig = JSONObject().put("responseFormat", responseFormat)

        return JSONObject()
            .put("contents", contents)
            .put("generationConfig", generationConfig)
            .toString()
    }

    private fun parseTextResponse(responseString: String): String {
        return try {
            val root = JSONObject(responseString)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text").trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing text response", e)
            "Không thể phân tích phản hồi từ AI."
        }
    }

    private fun parseShoppingResponse(responseString: String): ShoppingTranslationResult {
        try {
            val root = JSONObject(responseString)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val rawText = parts.getJSONObject(0).getString("text").trim()
            
            // Extract JSON if it contains markdown wrappers like ```json
            val cleanJson = if (rawText.startsWith("```")) {
                val lines = rawText.split("\n")
                val start = if (lines.first().contains("json")) 1 else 0
                val end = if (lines.last().startsWith("```")) lines.size - 1 else lines.size
                lines.subList(start, end).joinToString("\n").trim()
            } else {
                rawText
            }

            val json = JSONObject(cleanJson)
            val productName = json.optString("productName", "Sản phẩm không rõ tên")
            val originalPrice = json.optString("originalPrice", "Không rõ")
            val convertedPrice = json.optString("convertedPrice", "Không rõ")
            
            val optionsJson = json.optJSONArray("options")
            val options = mutableListOf<String>()
            if (optionsJson != null) {
                for (i in 0 until optionsJson.length()) {
                    options.add(optionsJson.getString(i))
                }
            }

            val specsJson = json.optJSONObject("specs")
            val specs = mutableMapOf<String, String>()
            if (specsJson != null) {
                val keys = specsJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    specs[key] = specsJson.getString(key)
                }
            }

            val summary = json.optString("summary", "Không có tóm tắt.")
            val advice = json.optString("advice", "Không có lời khuyên mua hàng.")

            return ShoppingTranslationResult(
                productName = productName,
                originalPrice = originalPrice,
                convertedPrice = convertedPrice,
                options = options,
                specs = specs,
                summary = summary,
                advice = advice
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing shopping response JSON", e)
            return ShoppingTranslationResult(
                productName = "Lỗi phân tích cú pháp AI",
                originalPrice = "Không rõ",
                convertedPrice = "Không rõ",
                options = listOf(),
                specs = mapOf("Chi tiết lỗi" to (e.localizedMessage ?: "Mã phản hồi JSON không hợp lệ")),
                summary = "AI đã phản hồi nhưng định dạng không đúng chuẩn JSON yêu cầu.",
                advice = "Hãy thử dịch lại màn hình một lần nữa."
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap to max width 1024 to save bandwidth and stay well within context limit
        val scaledBitmap = if (bitmap.width > 1024) {
            val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
            val newHeight = (1024 * aspectRatio).toInt()
            Bitmap.createScaledBitmap(bitmap, 1024, newHeight, true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun mockTextTranslation(text: String, targetLang: String): String {
        return if (targetLang.lowercase() == "vi" || targetLang.lowercase().contains("viet")) {
            when {
                text.lowercase().contains("shoe") -> "Giày thể thao nam cổ thấp chạy bộ"
                text.lowercase().contains("dress") -> "Váy đầm nữ dáng dài phong cách Vintage"
                text.lowercase().contains("phone") -> "Điện thoại thông minh 5G dung lượng 256GB"
                text.lowercase().contains("jacket") -> "Áo khoác gió chống nước mùa đông"
                text.lowercase().contains("bag") -> "Túi xách tay da cao cấp sang trọng"
                else -> "Bản dịch thử nghiệm: $text (Hãy cài đặt GEMINI_API_KEY trong tab Secrets để dịch thật)"
            }
        } else {
            "Simulated translation of: $text to $targetLang"
        }
    }

    /**
     * Generates extremely detailed, simulated, high-fidelity translation outcomes based on e-commerce sites
     * so that the application is fully interactive and visually stunning even before configuring API keys.
     */
    fun generateSimulatedResult(url: String): ShoppingTranslationResult {
        val site = url.lowercase()
        return when {
            site.contains("amazon.com") || site.contains("amazon") -> {
                ShoppingTranslationResult(
                    productName = "Giày Thể Thao Nam Chạy Bộ Siêu Êm Ultra Cushion 2.0",
                    originalPrice = "${'$'}89.99",
                    convertedPrice = "~2.285.000 VND",
                    options = listOf(
                        "Đen Huyền Bí (Black Charcoal) - Size 9 US (42 VN)",
                        "Trắng Tuyết (Cloud White) - Size 9.5 US (43 VN)",
                        "Xanh Dương Thể Thao - Size 10 US (44 VN)"
                    ),
                    specs = mapOf(
                        "Thương hiệu" to "SportFit USA",
                        "Chất liệu đế" to "Cao su lưu hóa chống trượt",
                        "Chất liệu thân" to "Sợi dệt Mesh thoáng khí 90%",
                        "Trọng lượng" to "280g cực kỳ nhẹ",
                        "Công nghệ" to "Đệm lót Gel hấp thụ lực va chạm"
                    ),
                    summary = "Sản phẩm giày chạy chuyên nghiệp bán chạy số 1 trên Amazon Mỹ, đạt 4.8/5 sao từ hơn 12.000 người dùng.",
                    advice = "👉 LƯU Ý KÍCH CỠ: Form giày này hơi ôm chân (Narrow fit). Bạn nên tăng +0.5 size so với size giày thông thường của mình để thoải mái nhất."
                )
            }
            site.contains("taobao.com") || site.contains("taobao") || site.contains("1688") -> {
                ShoppingTranslationResult(
                    productName = "Áo Khoác Blazer Nữ Form Rộng Phong Cách Hàn Quốc Thu Đông",
                    originalPrice = "¥128.00 (Nhân dân tệ)",
                    convertedPrice = "~448.000 VND",
                    options = listOf(
                        "Màu Be Sữa (Khaki) - Size M (45 - 52kg)",
                        "Màu Đen Cổ Điển (Black) - Size L (52 - 60kg)",
                        "Màu Xám Tro - Size XL (60 - 68kg)"
                    ),
                    specs = mapOf(
                        "Chất liệu" to "Vải tuyết mưa Hàn Quốc cao cấp, đứng form",
                        "Số cúc" to "2 hàng cúc thanh lịch",
                        "Độ dày" to "Vừa phải, có lớp lót lụa mềm mịn bên trong",
                        "Phong cách" to "Oversize trẻ trung năng động"
                    ),
                    summary = "Mẫu áo khoác blazer quốc dân đang cực hot tại Taobao Trung Quốc với lượt mua khủng và phản hồi ảnh chụp thực tế rất đẹp.",
                    advice = "👉 LƯU Ý KÍCH CỠ: Hàng nội địa Trung Quốc có bảng size chuẩn Châu Á. Chiều dài áo dài phủ mông, bạn có thể chọn đúng size theo bảng cân nặng gợi ý ở trên."
                )
            }
            else -> {
                ShoppingTranslationResult(
                    productName = "Đồng Hồ Thông Minh Đa Năng AI Wear Pro S4",
                    originalPrice = "${'$'}149.00",
                    convertedPrice = "~3.785.000 VND",
                    options = listOf(
                        "Mặt đen dây Silicon - Bản Tiêu chuẩn",
                        "Mặt bạc dây Da bò - Bản Cao cấp (+${'$'}20)",
                        "Mặt vàng hồng dây Thép - Bản Gold Luxury"
                    ),
                    specs = mapOf(
                        "Màn hình" to "AMOLED 1.43 inch tràn viền siêu nét",
                        "Thời lượng pin" to "Lên tới 14 ngày sử dụng bình thường",
                        "Chống nước" to "Chuẩn 5ATM đi mưa bơi lội thoải mái",
                        "Tính năng chính" to "Đo nhịp tim, nồng độ oxy SpO2, 120 chế độ thể thao"
                    ),
                    summary = "Thiết bị đeo thông minh phân khúc tầm trung xuất sắc tích hợp trợ lý AI thông minh mới nhất.",
                    advice = "👉 KHUYÊN DÙNG: Đây là lựa chọn hoàn hảo thay thế cho các mẫu cao cấp đắt đỏ. Khả năng đồng bộ hóa tốt với cả iOS và Android."
                )
            }
        }
    }
}

data class ShoppingTranslationResult(
    val productName: String,
    val originalPrice: String,
    val convertedPrice: String,
    val options: List<String>,
    val specs: Map<String, String>,
    val summary: String,
    val advice: String
)
