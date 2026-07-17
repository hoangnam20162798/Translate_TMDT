package com.example.ui

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.os.Build
import android.os.IBinder
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.data.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // State
    private var selectedTab = 0 // 0: Dịch chữ, 1: Quy đổi tiền tệ
    private var currentRate = 3500.0 // Default exchange rate
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle collapse to bubble or expand directly if needed
        return START_STICKY
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun setupFloatingBubble() {
        val size = dpToPx(56)
        
        // Custom draw bubble with green-blue gradient + "A/文" icon
        val bubbleFrame = FrameLayout(this)
        val backgroundDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#00E676"), Color.parseColor("#00B0FF"))
        ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dpToPx(2), Color.WHITE)
        }
        bubbleFrame.background = backgroundDrawable
        bubbleFrame.elevation = dpToPx(8).toFloat()

        // Inner Translate Symbol
        val symbolView = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = dpToPx(18).toFloat()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val x = width / 2f
                val y = height / 2f - ((paint.descent() + paint.ascent()) / 2f)
                canvas.drawText("文/A", x, y, paint)
            }
        }
        
        val symbolParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        bubbleFrame.addView(symbolView, symbolParams)

        // Set layout parameters
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            size,
            size,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = resources.displayMetrics.widthPixels - size - dpToPx(16)
            y = resources.displayMetrics.heightPixels / 3
        }

        bubbleView = bubbleFrame

        // Touch listener for drag & click
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        bubbleFrame.setOnTouchListener { _, event ->
            val params = bubbleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = (initialX + dx).toInt()
                        params.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Click detected - Expand overlay panel
                        showExpandedPanel()
                    } else {
                        // Snap to left or right edge of screen
                        val screenWidth = resources.displayMetrics.widthPixels
                        val midPoint = screenWidth / 2
                        val targetX = if (params.x + size / 2 < midPoint) {
                            dpToPx(8)
                        } else {
                            screenWidth - size - dpToPx(8)
                        }
                        params.x = targetX
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun showExpandedPanel() {
        // Temporarily hide the floating bubble
        bubbleView?.visibility = View.GONE

        if (panelView != null) {
            panelView?.visibility = View.VISIBLE
            windowManager.updateViewLayout(panelView, panelParams)
            return
        }

        // Create main container view
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#141218")) // Dark sleek cosmic theme background
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#381E72")) // M3 primary border color
            }
            background = bg
            elevation = dpToPx(16).toFloat()
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // Screen parameters for panel (Focusable to allow keyboard typing)
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        panelParams = WindowManager.LayoutParams(
            dpToPx(320),
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // Header view
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
        }

        val titleView = TextView(this).apply {
            text = "Dịch & Đổi Tệ AI ✨"
            textColor = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val collapseBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = createRippleDrawable(Color.TRANSPARENT, Color.parseColor("#33FFFFFF"), dpToPx(16))
            setColorFilter(Color.WHITE)
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setOnClickListener {
                collapsePanel()
            }
        }

        val closeBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            background = createRippleDrawable(Color.TRANSPARENT, Color.parseColor("#55FF5252"), dpToPx(16))
            setColorFilter(Color.parseColor("#FF5252"))
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setOnClickListener {
                stopSelf()
            }
        }

        headerLayout.addView(titleView)
        headerLayout.addView(collapseBtn)
        headerLayout.addView(closeBtn)
        cardLayout.addView(headerLayout)

        // Sub-tabs (Dịch Chữ vs Quy Đổi Tệ)
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#211F26"))
                cornerRadius = dpToPx(12).toFloat()
            }
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(16))
            }
        }

        val tabTranslate = TextView(this).apply {
            text = "Dịch Chữ"
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tabCurrency = TextView(this).apply {
            text = "Quy Đổi Tệ"
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dpToPx(8), 0, dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        tabLayout.addView(tabTranslate)
        tabLayout.addView(tabCurrency)
        cardLayout.addView(tabLayout)

        // Tab Content Views container
        val contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. Translation View
        val translateView = createTranslateView()
        // 2. Currency View
        val currencyView = createCurrencyView()

        contentContainer.addView(translateView)
        contentContainer.addView(currencyView)
        cardLayout.addView(contentContainer)

        // Tab switcher logic
        fun updateTabs() {
            if (selectedTab == 0) {
                tabTranslate.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#D0BCFF")) // Selected M3 primary dark color
                    cornerRadius = dpToPx(8).toFloat()
                }
                tabTranslate.setTextColor(Color.parseColor("#381E72"))
                
                tabCurrency.background = null
                tabCurrency.setTextColor(Color.parseColor("#CAC4D0"))

                translateView.visibility = View.VISIBLE
                currencyView.visibility = View.GONE
            } else {
                tabCurrency.background = GradientDrawable().apply {
                    setColor(Color.parseColor("#D0BCFF"))
                    cornerRadius = dpToPx(8).toFloat()
                }
                tabCurrency.setTextColor(Color.parseColor("#381E72"))

                tabTranslate.background = null
                tabTranslate.setTextColor(Color.parseColor("#CAC4D0"))

                translateView.visibility = View.GONE
                currencyView.visibility = View.VISIBLE
            }
        }

        tabTranslate.setOnClickListener {
            selectedTab = 0
            updateTabs()
        }

        tabCurrency.setOnClickListener {
            selectedTab = 1
            updateTabs()
        }

        selectedTab = 0
        updateTabs()

        panelView = cardLayout
        windowManager.addView(panelView, panelParams)
    }

    private fun createTranslateView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Input EditText
        val inputEditText = EditText(this).apply {
            hint = "Nhập hoặc dán chữ Trung Quốc..."
            setHintTextColor(Color.parseColor("#938F99"))
            textColor = Color.WHITE
            textSize = 14f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#211F26"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#49454F"))
            }
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            minLines = 3
            maxLines = 4
            gravity = Gravity.TOP or Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
        }
        root.addView(inputEditText)

        // Buttons row
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
        }

        val pasteBtn = Button(this).apply {
            text = "📋 Dán"
            textSize = 11f
            background = createRippleDrawable(Color.parseColor("#211F26"), Color.parseColor("#55D0BCFF"), dpToPx(8))
            textColor = Color.parseColor("#D0BCFF")
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
        }

        val clearBtn = Button(this).apply {
            text = "Xóa"
            textSize = 11f
            background = createRippleDrawable(Color.parseColor("#211F26"), Color.parseColor("#33FFFFFF"), dpToPx(8))
            textColor = Color.WHITE
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(dpToPx(4), 0, dpToPx(4), 0)
            }
        }

        val translateBtn = Button(this).apply {
            text = "Dịch AI"
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = createRippleDrawable(Color.parseColor("#D0BCFF"), Color.parseColor("#55381E72"), dpToPx(8))
            textColor = Color.parseColor("#381E72")
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1.2f).apply {
                setMargins(dpToPx(4), 0, 0, 0)
            }
        }

        btnRow.addView(pasteBtn)
        btnRow.addView(clearBtn)
        btnRow.addView(translateBtn)
        root.addView(btnRow)

        // Translation Result Display
        val resultScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(100)
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1D1B20"))
                cornerRadius = dpToPx(12).toFloat()
            }
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        val resultText = TextView(this).apply {
            text = "Kết quả dịch sẽ xuất hiện ở đây..."
            textColor = Color.parseColor("#CAC4D0")
            textSize = 13f
            setMovementMethod(ScrollingMovementMethod())
        }
        resultScroll.addView(resultText)
        root.addView(resultScroll)

        // Copy button
        val copyBtn = Button(this).apply {
            text = "Sao chép kết quả"
            textSize = 12f
            background = createRippleDrawable(Color.parseColor("#381E72"), Color.parseColor("#33FFFFFF"), dpToPx(12))
            textColor = Color.parseColor("#D0BCFF")
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            )
        }
        root.addView(copyBtn)

        // Action listeners
        pasteBtn.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val copiedText = clip.getItemAt(0).text?.toString() ?: ""
                inputEditText.setText(copiedText)
                Toast.makeText(this, "Đã dán văn bản", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không có nội dung trong khay nhớ tạm", Toast.LENGTH_SHORT).show()
            }
        }

        clearBtn.setOnClickListener {
            inputEditText.setText("")
            resultText.text = "Kết quả dịch sẽ xuất hiện ở đây..."
            resultText.setTextColor(Color.parseColor("#CAC4D0"))
        }

        translateBtn.setOnClickListener {
            val textToTranslate = inputEditText.text.toString().trim()
            if (textToTranslate.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập chữ cần dịch!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            resultText.text = "Đang dịch bằng Gemini AI..."
            resultText.setTextColor(Color.parseColor("#D0BCFF"))

            serviceScope.launch {
                try {
                    val translated = withContext(Dispatchers.IO) {
                        GeminiService.translateText(textToTranslate, "Chinese", "Vietnamese")
                    }
                    resultText.text = translated
                    resultText.setTextColor(Color.WHITE)
                } catch (e: Exception) {
                    resultText.text = "Có lỗi xảy ra: ${e.localizedMessage}"
                    resultText.setTextColor(Color.parseColor("#FF5252"))
                }
            }
        }

        copyBtn.setOnClickListener {
            val txt = resultText.text.toString()
            if (txt.isNotEmpty() && txt != "Kết quả dịch sẽ xuất hiện ở đây..." && txt != "Đang dịch bằng Gemini AI...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("Translated Text", txt)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Đã sao chép kết quả!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không có kết quả để sao chép", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    private fun createCurrencyView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Inputs Layout: CNY and Rate
        val inputsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
        }

        // CNY Input Block
        val cnyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply {
                setMargins(0, 0, dpToPx(8), 0)
            }
        }
        val cnyLabel = TextView(this).apply {
            text = "Nhập tệ (¥ RMB)"
            textColor = Color.parseColor("#CAC4D0")
            textSize = 11f
            setPadding(dpToPx(2), 0, 0, dpToPx(4))
        }
        val cnyEditText = EditText(this).apply {
            setText("100")
            textColor = Color.WHITE
            textSize = 14f
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#211F26"))
                cornerRadius = dpToPx(10).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#49454F"))
            }
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
        }
        cnyLayout.addView(cnyLabel)
        cnyLayout.addView(cnyEditText)

        // Exchange Rate Input Block
        val rateLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val rateLabel = TextView(this).apply {
            text = "Tỷ giá (VND/¥)"
            textColor = Color.parseColor("#CAC4D0")
            textSize = 11f
            setPadding(dpToPx(2), 0, 0, dpToPx(4))
        }
        val rateEditText = EditText(this).apply {
            setText("3500")
            textColor = Color.WHITE
            textSize = 14f
            inputType = InputType.TYPE_CLASS_NUMBER
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#211F26"))
                cornerRadius = dpToPx(10).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#49454F"))
            }
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
        }
        rateLayout.addView(rateLabel)
        rateLayout.addView(rateEditText)

        inputsRow.addView(cnyLayout)
        inputsRow.addView(rateLayout)
        root.addView(inputsRow)

        // Converted Value Highlight Card
        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#381E72").and(0x22FFFFFF)) // 15% opacity primary container
                cornerRadius = dpToPx(14).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#381E72"))
            }
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
        }

        val resultLabel = TextView(this).apply {
            text = "Giá trị ước tính bằng VND"
            textColor = Color.parseColor("#D0BCFF")
            textSize = 11f
            gravity = Gravity.CENTER
        }
        val convertedValueText = TextView(this).apply {
            text = "350.000 đ"
            textColor = Color.parseColor("#00E676") // Vibrant positive conversion green
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(4), 0, 0)
        }
        displayCard.addView(resultLabel)
        displayCard.addView(convertedValueText)
        root.addView(displayCard)

        // Quick lookup title
        val lookupTitle = TextView(this).apply {
            text = "BẢNG TRA CỨU NHANH (Tỷ giá: 3.500)"
            textColor = Color.parseColor("#938F99")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dpToPx(2), 0, 0, dpToPx(6))
        }
        root.addView(lookupTitle)

        // Quick conversion layout
        val quickLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1D1B20"))
                cornerRadius = dpToPx(12).toFloat()
            }
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
        }

        val lookupRows = mutableListOf<TextView>()
        val valuesToLookup = listOf(10, 50, 100, 500, 1000)

        // Helper function to format VND
        val vndFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

        fun updateCalculations() {
            val cnyAmount = cnyEditText.text.toString().toDoubleOrNull() ?: 0.0
            val rate = rateEditText.text.toString().toDoubleOrNull() ?: currentRate
            currentRate = rate

            // Update main converted field
            val totalVnd = cnyAmount * rate
            convertedValueText.text = vndFormatter.format(totalVnd)

            // Update quick conversion lookup text
            lookupTitle.text = "BẢNG TRA CỨU NHANH (Tỷ giá: ${String.format("%,.0f", rate)})"

            // Update lookup table rows
            for (i in valuesToLookup.indices) {
                val valCny = valuesToLookup[i]
                val valVnd = valCny * rate
                val formattedVnd = vndFormatter.format(valVnd)
                lookupRows[i].text = "¥${valCny}  ➔  ${formattedVnd}"
            }
        }

        // Generate rows
        for (value in valuesToLookup) {
            val rowText = TextView(this).apply {
                text = ""
                textColor = Color.WHITE
                textSize = 12f
                setPadding(0, dpToPx(4), 0, dpToPx(4))
            }
            quickLayout.addView(rowText)
            lookupRows.add(rowText)
        }
        root.addView(quickLayout)

        // Text watchers to update dynamically
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculations()
            }
        }
        cnyEditText.addTextChangedListener(watcher)
        rateEditText.addTextChangedListener(watcher)

        updateCalculations() // Initial compute

        return root
    }

    private fun collapsePanel() {
        panelView?.visibility = View.GONE
        bubbleView?.visibility = View.VISIBLE
        // Refresh layout position to match snapped edge
        val size = dpToPx(56)
        val params = bubbleParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val midPoint = screenWidth / 2
        params.x = if (params.x + size / 2 < midPoint) dpToPx(8) else screenWidth - size - dpToPx(8)
        windowManager.updateViewLayout(bubbleView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            try {
                if (bubbleView != null) {
                    windowManager.removeView(bubbleView)
                }
                if (panelView != null) {
                    windowManager.removeView(panelView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Toast.makeText(this, "Đã đóng bong bóng dịch", Toast.LENGTH_SHORT).show()
    }

    // Helper functions for backgrounds
    private fun createRippleDrawable(normalColor: Int, rippleColor: Int, radius: Int): RippleDrawable {
        val content = GradientDrawable().apply {
            setColor(normalColor)
            cornerRadius = radius.toFloat()
        }
        val mask = ShapeDrawable(OvalShape()).apply {
            paint.color = Color.WHITE
        }
        return RippleDrawable(
            ColorStateList.valueOf(rippleColor),
            content,
            mask
        )
    }

    // Helper extensions for TextView colors
    private var TextView.textColor: Int
        get() = currentTextColor
        set(value) {
            setTextColor(value)
        }
}
