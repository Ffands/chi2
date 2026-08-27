package com.example.autoclicker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast

class CrosshairView(context: Context, val node: TargetNode) : View(context) {
    private val density = context.resources.displayMetrics.density
    
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val highlightPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.CYAN
    }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        setShadowLayer(3f, 0f, 0f, Color.BLACK)
    }

    var isCurrentTarget = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        paint.color = node.crosshairColor
        paint.strokeWidth = 3f * density * node.sizeScale
        
        textPaint.color = node.numberColor
        textPaint.textSize = 14f * density * node.sizeScale
        
        val cx = width / 2f
        val cy = height / 2f
        val radius = 4f * density * node.sizeScale
        val armLength = 8f * density * node.sizeScale
        
        if (isCurrentTarget) {
            highlightPaint.strokeWidth = (3f * density * node.sizeScale) + 4f
            canvas.drawCircle(cx, cy, radius, highlightPaint)
            canvas.drawCircle(cx, cy, radius + armLength, highlightPaint)
        }

        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawLine(cx, cy - radius - armLength, cx, cy - radius, paint)
        canvas.drawLine(cx, cy + radius, cx, cy + radius + armLength, paint)
        canvas.drawLine(cx - radius - armLength, cy, cx - radius, cy, paint)
        canvas.drawLine(cx + radius, cy, cx + radius + armLength, cy, paint)
        
        canvas.drawText(node.id.toString(), cx + 5f * density * node.sizeScale, cy - 5f * density * node.sizeScale, textPaint)
    }
}

class SubMarkerView(context: Context, val node: TargetNode, val markerType: Int) : View(context) {
    // 1 = Swipe, 2 = Color Compare
    private val density = context.resources.displayMetrics.density
    
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = if (markerType == 1) Color.parseColor("#00BFFF") else Color.parseColor("#FF00FF") // Cyan for swipe, Magenta for color
        strokeWidth = 2f * density * node.sizeScale
    }
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = paint.color
        alpha = 80
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 10f * density * node.sizeScale
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        
        val radius = 8f * density * node.sizeScale
        val innerRadius = 3f * density * node.sizeScale
        val lineExt = 4f * density * node.sizeScale
        
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx, cy, innerRadius, fillPaint)
        
        canvas.drawLine(cx, cy - radius - lineExt, cx, cy + radius + lineExt, paint)
        canvas.drawLine(cx - radius - lineExt, cy, cx + radius + lineExt, cy, paint)
        
        val label = if (markerType == 1) "S${node.id}" else "C${node.id}"
        canvas.drawText(label, cx + radius + lineExt, cy - radius, textPaint)
    }
}

class UIManager(private val service: AutoClickService) {
    var screenOffsetX = 0
    var screenOffsetY = 0
    
    var isDebugWindowVisible = false
    private var debugWindow: View? = null
    private var debugTextView: TextView? = null
    private var debugScrollView: ScrollView? = null
    private val debugLogs = mutableListOf<String>()

    val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    var floatingControlBar: View? = null
    private var linesOverlayView: View? = null
    private var modMenu: View? = null
    private var modMenuParams: WindowManager.LayoutParams? = null
    var isMenuFullscreen = false
    var isCaffeineEnabled = false
    var showEyeBtn = true
    var showLinesBtn = true
    var showHotbarBtn = true
    var showSettingsBtn = true
    private lateinit var menuContentContainer: FrameLayout
    
    fun updateFloatingControlBarCaffeine() {
        val params = floatingControlBar?.layoutParams as? WindowManager.LayoutParams ?: return
        if (isCaffeineEnabled) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        }
        windowManager.updateViewLayout(floatingControlBar, params)
    }
    
    val nodeViews = mutableMapOf<Int, View>()
    val nodeParams = mutableMapOf<Int, WindowManager.LayoutParams>()
    
    val swipeEndViews = mutableMapOf<Int, View>()
    val swipeEndParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    val textZoneStartViews = mutableMapOf<Int, View>()
    val textZoneStartParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    val textZoneEndViews = mutableMapOf<Int, View>()
    val textZoneEndParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    val colorCompareViews = mutableMapOf<Int, View>()
    val colorCompareParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    var nodeCounter = 1
    var appMode: AppMode = AppMode.ADVANCED
    var uiScale: Float = 1.0f
    var uiAlpha: Float = 0.9f

    fun loadUISettings() {
        val prefs = service.getSharedPreferences("AutoClickerUISettings", android.content.Context.MODE_PRIVATE)
        uiScale = prefs.getFloat("uiScale", 1.0f)
        uiAlpha = prefs.getFloat("uiAlpha", 0.9f)
        showEyeBtn = prefs.getBoolean("showEyeBtn", true)
        showLinesBtn = prefs.getBoolean("showLinesBtn", true)
        showHotbarBtn = prefs.getBoolean("showHotbarBtn", true)
        showSettingsBtn = prefs.getBoolean("showSettingsBtn", true)
        isDebugWindowVisible = prefs.getBoolean("isDebugWindowVisible", false)
        isCaffeineEnabled = prefs.getBoolean("isCaffeineEnabled", false)
        isMenuFullscreen = prefs.getBoolean("isMenuFullscreen", false)
        service.enableMultitouch = prefs.getBoolean("enableMultitouch", false)
    }

    fun saveUISettings() {
        val prefs = service.getSharedPreferences("AutoClickerUISettings", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("uiScale", uiScale)
            putFloat("uiAlpha", uiAlpha)
            putBoolean("showEyeBtn", showEyeBtn)
            putBoolean("showLinesBtn", showLinesBtn)
            putBoolean("showHotbarBtn", showHotbarBtn)
            putBoolean("showSettingsBtn", showSettingsBtn)
            putBoolean("isDebugWindowVisible", isDebugWindowVisible)
            putBoolean("isCaffeineEnabled", isCaffeineEnabled)
            putBoolean("isMenuFullscreen", isMenuFullscreen)
            putBoolean("enableMultitouch", service.enableMultitouch)
            apply()
        }
    }

    init {
        loadUISettings()
    }

    fun getEffectiveUiScale(): Float {
        val isLandscape = service.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        return if (isLandscape) uiScale * 0.8f else uiScale
    }


    val phantomMap = mutableMapOf<Int, List<View>>()
    private var phantomIdCounter = 0

    fun showPhantomNodes(nodes: List<TargetNode>): Int {
        val id = ++phantomIdCounter
        val views = mutableListOf<View>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            for (node in nodes) {
                val crosshair = CrosshairView(service, node)
                crosshair.alpha = 0.5f // Phantom transparency
                
                val params = WindowManager.LayoutParams(
                    dpToPx(60), dpToPx(60),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.TOP or Gravity.START
                params.x = node.x - dpToPx(30)
                params.y = node.y - dpToPx(30)
                
                try {
                    windowManager.addView(crosshair, params)
                    views.add(crosshair)
                } catch(e: Exception){}
                
                if (node.isSwipe) {
                    val endCrosshair = CrosshairView(service, node)
                    endCrosshair.alpha = 0.5f
                    val endParams = WindowManager.LayoutParams(
                        dpToPx(60), dpToPx(60),
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    )
                    endParams.gravity = Gravity.TOP or Gravity.START
                    endParams.x = node.swipeEndX - dpToPx(30)
                    endParams.y = node.swipeEndY - dpToPx(30)
                    try {
                        windowManager.addView(endCrosshair, endParams)
                        views.add(endCrosshair)
                    } catch(e: Exception){}
                }
            }
            phantomMap[id] = views
        }
        return id
    }

    fun hidePhantomNodes(id: Int) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val views = phantomMap.remove(id)
            views?.forEach { 
                try { windowManager.removeView(it) } catch(e: Exception){} 
            }
        }
    }

    fun removeAllPhantomNodes() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            phantomMap.values.flatten().forEach { 
                try { windowManager.removeView(it) } catch(e: Exception){} 
            }
            phantomMap.clear()
        }
    }

    fun dpToPx(dp: Int): Int = (dp * service.resources.displayMetrics.density * getEffectiveUiScale()).toInt()

    fun TextView.setScaledTextSize(size: Float) {
        this.textSize = size * getEffectiveUiScale()
    }

    fun setNodesTouchable(touchable: Boolean) {
        val lists = listOf(
            nodeViews to nodeParams,
            swipeEndViews to swipeEndParams,
            colorCompareViews to colorCompareParams,
            textZoneStartViews to textZoneStartParams,
            textZoneEndViews to textZoneEndParams
        )
        for ((viewsMap, paramsMap) in lists) {
            for ((id, view) in viewsMap) {
                val params = paramsMap[id] ?: continue
                if (touchable) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                } else {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
                windowManager.updateViewLayout(view, params)
            }
        }
    }

    private fun setMenuFocusable(focusable: Boolean) {
        val params = modMenu?.layoutParams as? WindowManager.LayoutParams ?: return
        if (focusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowManager.updateViewLayout(modMenu, params)
    }

    fun toggleHudVisibility() {
        floatingControlBar?.let {
            val isVisible = it.visibility == View.VISIBLE
            it.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }


    private fun showHotbarConfigDialog() {
        val allProfiles = service.getSavedProfiles()
        val currentHotbar = service.getHotbarItems().toMutableList()
        
        val dialogView = ScrollView(service).apply {
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }
        val layout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        dialogView.addView(layout)
        
        val resultList = mutableListOf<Pair<String, android.widget.EditText>>()
        
        for (p in allProfiles) {
            val itemLayout = LinearLayout(service).apply { 
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dpToPx(10))
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            val cb = android.widget.CheckBox(service).apply {
                text = p
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val et = android.widget.EditText(service).apply {
                hint = "Смайл/Имя"
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dpToPx(100), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            
            val existing = currentHotbar.find { it.first == p }
            if (existing != null) {
                cb.isChecked = true
                et.setText(existing.second)
            } else {
                cb.isChecked = false
                et.setText(p)
            }
            
            itemLayout.addView(cb)
            itemLayout.addView(et)
            layout.addView(itemLayout)
            
            resultList.add(Pair(p, et))
            
            cb.setOnCheckedChangeListener { _, _ ->
                // Do nothing, just state
            }
        }
        
        val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Настройка Хотбара")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newItems = mutableListOf<Pair<String, String>>()
                for (i in 0 until layout.childCount) {
                    val row = layout.getChildAt(i) as LinearLayout
                    val cb = row.getChildAt(0) as android.widget.CheckBox
                    val et = row.getChildAt(1) as android.widget.EditText
                    if (cb.isChecked) {
                        newItems.add(Pair(cb.text.toString(), et.text.toString().takeIf { it.isNotBlank() } ?: cb.text.toString()))
                    }
                }
                service.saveHotbarItems(newItems)
                updateHotbar(hotbarContainer!!)
            }
            .setNegativeButton("Отмена", null)
            .create()
            
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }

    var hotbarContainer: LinearLayout? = null

    private fun updateHotbar(container: LinearLayout) {
        hotbarContainer = container
        container.removeAllViews()
        val profiles = service.getHotbarItems()
        
        val configBtn = Button(service).apply {
            text = "⚙️"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#444444"))
            setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, dpToPx(5), 0)
            layoutParams = params
            setOnClickListener { showHotbarConfigDialog() }
        }
        container.addView(configBtn)
        
        if (profiles.isEmpty()) {
            val tv = TextView(service).apply {
                text = "Пусто"
                setTextColor(Color.GRAY)
                setPadding(dpToPx(10), 0, dpToPx(10), 0)
            }
            container.addView(tv)
            return
        }
        for (p in profiles) {
            val btn = Button(service).apply {
                text = p.second
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#222222"))
                setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, dpToPx(5), 0)
                layoutParams = params
                
                setOnClickListener {
                    val df = java.text.SimpleDateFormat("HH-mm-ss", java.util.Locale.getDefault())
                    val autoSaveName = "AutoSave / " + df.format(java.util.Date())
                    service.saveProfile(autoSaveName)
                    
                    service.loadProfile(p.first)
                    service.uiManager.updateMenu()
                    android.widget.Toast.makeText(service, "Автосохранено '$autoSaveName'. Загружен: ${p.first}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            container.addView(btn)
        }
    }
    fun showFloatingControlBar() {
        if (floatingControlBar != null) {
            floatingControlBar?.visibility = View.VISIBLE
            return
        }
        
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5))
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(Color.parseColor("#E61E1E1E")) // 90% opacity dark grey
            drawable.setStroke(dpToPx(1), Color.parseColor("#333333"))
            drawable.cornerRadius = dpToPx(12).toFloat()
            background = drawable
            clipToOutline = true
        }
        
        val topRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        val hotbarRow = HorizontalScrollView(service).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(280), WindowManager.LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(5), 0, 0)
        }
        val hotbarContainer = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        hotbarRow.addView(hotbarContainer)


        val dragHandle = Button(service).apply {
            text = "‖"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(40))
            setPadding(0, 0, 0, 0)
        }
        
        val playBtn = Button(service).apply {
            text = if (service.isPlaying) "⏸" else "▶"
            setTextColor(if (service.isPlaying) Color.parseColor("#FFD50000") else Color.parseColor("#FF00C853"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                service.togglePlay()
                text = if (service.isPlaying) "⏸" else "▶"
                setTextColor(if (service.isPlaying) Color.parseColor("#FFD50000") else Color.parseColor("#FF00C853"))
            }

        }
        
        val recordBtn = Button(service).apply {
            text = if (service.isRecording) "■" else "🔴"
            setTextColor(if (service.isRecording) Color.parseColor("#FFD50000") else Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            visibility = if (appMode == AppMode.RECORD) View.VISIBLE else View.GONE
            setOnClickListener {
                service.toggleRecording()
                text = if (service.isRecording) "■" else "🔴"
                setTextColor(if (service.isRecording) Color.parseColor("#FFD50000") else Color.WHITE)
            }
        }
        
        val toggleVisBtn = Button(service).apply {
            text = "👁"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            visibility = if (showEyeBtn) View.VISIBLE else View.GONE
            setOnClickListener { 
                service.nodes.forEach {
                    it.isVisible = !it.isVisible
                    nodeViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    swipeEndViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    textZoneStartViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    textZoneEndViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    colorCompareViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                }
                invalidateLines()
            }
        }

        val gearBtn = Button(service).apply {
            text = "⚙"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            visibility = View.VISIBLE
            setOnClickListener { showModMenu() }
        }
        
        val exitBtn = Button(service).apply {
            text = "✖"
            setTextColor(Color.parseColor("#FFD50000"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            setOnClickListener {
                if (service.isPlaying) service.togglePlay()
                if (service.isRecording) service.toggleRecording()
                service.nodes.clear()
                removeAllViews()
                floatingControlBar = null
                modMenu = null
                nodeViews.clear()
                swipeEndViews.clear()
                textZoneStartViews.clear()
                textZoneEndViews.clear()
                colorCompareViews.clear()
                linesOverlayView?.let { try { windowManager.removeView(it) } catch(e:Exception){} }
                linesOverlayView = null
                // Also update main app status if needed, but the main app is likely in background.
            }
        }
        

        
        val hotbarToggleBtn = Button(service).apply {
            text = "⚡"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            visibility = if (showHotbarBtn) View.VISIBLE else View.GONE
            setOnClickListener {
                if (hotbarRow.visibility == View.VISIBLE) {
                    hotbarRow.visibility = View.GONE
                } else {
                    hotbarRow.visibility = View.VISIBLE
                    updateHotbar(hotbarContainer)
                }
                floatingControlBar?.let {
                    val p = it.layoutParams as? WindowManager.LayoutParams
                    if (p != null) windowManager.updateViewLayout(it, p)
                }
            }
        }
        
        val linesToggleBtn = Button(service).apply {
            text = "🕸"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            visibility = if (showLinesBtn) View.VISIBLE else View.GONE
            setOnClickListener {
                service.showLines = !service.showLines
                text = if (service.showLines) "🕸" else "🕸✖"
                linesOverlayView?.visibility = if (service.showLines) View.VISIBLE else View.INVISIBLE
                linesOverlayView?.visibility = if (service.showLines) View.VISIBLE else View.INVISIBLE; linesOverlayView?.invalidate()
            }
        }

        val scrollContent = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val topScroll = android.widget.HorizontalScrollView(service).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(scrollContent)
        }

        val minMaxBtn = Button(service).apply {
            text = "➖"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setPadding(0, 0, 0, 0)
            
            var isMinimized = false
            setOnClickListener {
                isMinimized = !isMinimized
                text = if (isMinimized) "➕" else "➖"
                val vis = if (isMinimized) View.GONE else View.VISIBLE
                playBtn.visibility = vis
                recordBtn.visibility = if (isMinimized || appMode != AppMode.RECORD) View.GONE else View.VISIBLE
                toggleVisBtn.visibility = if (isMinimized || !showEyeBtn) View.GONE else View.VISIBLE
                linesToggleBtn.visibility = if (isMinimized || !showLinesBtn) View.GONE else View.VISIBLE
                hotbarToggleBtn.visibility = if (isMinimized || !showHotbarBtn) View.GONE else View.VISIBLE
                gearBtn.visibility = if (isMinimized) View.GONE else View.VISIBLE
                exitBtn.visibility = vis
                topScroll.visibility = vis
                if (isMinimized) hotbarRow.visibility = View.GONE
                floatingControlBar?.let {
                    val p = it.layoutParams as? WindowManager.LayoutParams
                    if (p != null) windowManager.updateViewLayout(it, p)
                }
            }

        }


        topRow.addView(dragHandle)
        topRow.addView(minMaxBtn)
        scrollContent.addView(playBtn)
        scrollContent.addView(recordBtn)
        scrollContent.addView(toggleVisBtn)
        scrollContent.addView(linesToggleBtn)
        scrollContent.addView(hotbarToggleBtn)
        topRow.addView(topScroll)
        topRow.addView(gearBtn)
        topRow.addView(exitBtn)
        
        layout.addView(topRow)
        layout.addView(hotbarRow)

        var barFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (isCaffeineEnabled) barFlags = barFlags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            barFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        var isMoved = false
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isMoved = true
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + dx.toInt()
                    var newY = initialY + dy.toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(layout, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(layout, params)
        floatingControlBar = layout
    }

    fun setupDraggableWindow(dragHandle: View, windowView: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(windowView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun buildModMenuLayout(params: WindowManager.LayoutParams): View {
        val root = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FF111111"))
            setPadding(10, 10, 10, 10)
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(Color.parseColor("#FF111111"))
            drawable.setStroke(dpToPx(2), Color.parseColor("#FF4CAF50"))
            drawable.cornerRadius = dpToPx(8).toFloat()
            background = drawable
        }
        
        val topBar = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FF1A1A1A"))
            setPadding(10, 10, 10, 10)
        }
        val header = TextView(service).apply {
            text = "UpwellClick Меню"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(16f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        val fullscreenBtn = Button(service).apply {
            text = "⛶"
            setPadding(0,0,0,0)
            setTextColor(Color.parseColor("#FF2196F3"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { toggleMenuFullscreen() }
        }

        val closeBtn = Button(service).apply {
            text = "X"
            setPadding(0,0,0,0)
            setTextColor(Color.parseColor("#FFD50000"))
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { modMenu?.visibility = View.GONE }
        }
        topBar.addView(header)
        topBar.addView(fullscreenBtn)
        topBar.addView(closeBtn)
        root.addView(topBar)

        setupDraggableWindow(topBar, root, params)

        val tabsLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FF222222"))
        }
        val mainTab = Button(service).apply { text = "Метки"; setScaledTextSize(12f); setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { showMainMenu() } }
        val modeTab = Button(service).apply { text = "Режимы"; setScaledTextSize(12f); setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { showModeMenu() } }
        val settTab = Button(service).apply { text = "⚙"; setScaledTextSize(12f); setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); layoutParams = LinearLayout.LayoutParams(dpToPx(40), WindowManager.LayoutParams.WRAP_CONTENT); setOnClickListener { showSettingsMenu() } }
        
        tabsLayout.addView(mainTab)
        tabsLayout.addView(modeTab)
        tabsLayout.addView(settTab)
        root.addView(tabsLayout)

        menuContentContainer = FrameLayout(service).apply {
            layoutParams = LinearLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        root.addView(menuContentContainer)
        
        showMainMenu()
        
        return root
    }

    private fun getMenuDefaultWidth(): Int {
        val isLandscape = service.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        return if (isLandscape) dpToPx(450) else dpToPx(300)
    }

    fun toggleMenuFullscreen() {
        isMenuFullscreen = !isMenuFullscreen
        saveUISettings()
        modMenuParams?.let { params ->
            if (isMenuFullscreen) {
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.MATCH_PARENT
                params.x = 0
                params.y = 0
            } else {
                params.width = getMenuDefaultWidth()
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.x = dpToPx(60)
                params.y = dpToPx(60)
            }
            if (modMenu != null) {
                windowManager.updateViewLayout(modMenu, params)
                
                val currentTag = menuContentContainer.getChildAt(0)?.tag as? String
                when (currentTag) {
                    "main_menu" -> showMainMenu()
                    "help_menu" -> showHelpMenu()
                    "profile_menu_save" -> showProfileMenu(true)
                    "profile_menu_load" -> showProfileMenu(false)
                    else -> modMenu?.requestLayout()
                }
            }
        }
    }

    private fun showModMenu() {
        if (modMenu == null) {
            val params = WindowManager.LayoutParams(
                if (isMenuFullscreen) WindowManager.LayoutParams.MATCH_PARENT else getMenuDefaultWidth(),
                if (isMenuFullscreen) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = if (isMenuFullscreen) 0 else dpToPx(60)
                y = if (isMenuFullscreen) 0 else dpToPx(60)
            }
            modMenuParams = params
            modMenu = buildModMenuLayout(params)
            windowManager.addView(modMenu, params)
        } else {
            modMenu?.visibility = if (modMenu?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        updateMenu()
    }

    fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        modMenuParams?.let { params ->
            if (!isMenuFullscreen) {
                params.width = getMenuDefaultWidth()
                if (modMenu != null) {
                    windowManager.updateViewLayout(modMenu, params)
                }
            }
        }
    }

    fun updateMenu() {
        if (modMenu?.visibility == View.VISIBLE && menuContentContainer.getChildAt(0)?.tag == "main_menu") {
            showMainMenu()
        }
    }

    fun updateCurrentNodeHighlight(currentIds: List<Int>) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            for ((id, view) in nodeViews) {
                if (view is CrosshairView) {
                    val wasCurrent = view.isCurrentTarget
                    view.isCurrentTarget = currentIds.contains(id)
                    if (wasCurrent != view.isCurrentTarget) {
                        view.invalidate()
                    }
                }
            }
        }
    }

    fun recreateModMenu(tagToRestore: String?) {
        if (modMenu != null) {
            windowManager.removeView(modMenu)
            modMenu = null
            showModMenu()
            when (tagToRestore) {
                "main_menu" -> showMainMenu()
                "help_menu" -> showHelpMenu()
                "profile_menu_save" -> showProfileMenu(true)
                "profile_menu_load" -> showProfileMenu(false)
                "settings_menu" -> showSettingsMenu()
                "edit_menu" -> {
                    // we can't easily restore the exact node without keeping track of it,
                    // but for settings menu this is fine.
                }
            }
        }
    }

    fun applyUIScaleChange() {
        val currentTag = menuContentContainer.getChildAt(0)?.tag as? String
        recreateFloatingControlBar()
        
        // Remove all markers
        service.nodes.forEach { 
            nodeViews[it.id]?.let { view -> windowManager.removeView(view) }
            swipeEndViews[it.id]?.let { view -> windowManager.removeView(view) }
            colorCompareViews[it.id]?.let { view -> windowManager.removeView(view) }
            textZoneStartViews[it.id]?.let { view -> windowManager.removeView(view) }
            textZoneEndViews[it.id]?.let { view -> windowManager.removeView(view) }
        }
        nodeViews.clear()
        swipeEndViews.clear()
        colorCompareViews.clear()
        textZoneStartViews.clear()
        textZoneEndViews.clear()
        
        recreateAllNodeViews()
        recreateModMenu(currentTag)
    }

    fun recreateFloatingControlBar() {
        if (floatingControlBar != null) {
            windowManager.removeView(floatingControlBar)
            floatingControlBar = null
            showFloatingControlBar()
        }
    }

    fun recreateAllNodeViews() {
        for (node in service.nodes) {
            val crosshair = CrosshairView(service, node)
            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            if (service.isPlaying || service.isRecording) {
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            val params = WindowManager.LayoutParams(
                dpToPx(60), dpToPx(60),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = node.x - dpToPx(30)
                y = node.y - dpToPx(30)
            }
            crosshair.visibility = if (node.isVisible) View.VISIBLE else View.GONE
            crosshair.setOnClickListener { showEditNodeMenu(node) }
            setupNodeTouchListener(crosshair, params, node)
            windowManager.addView(crosshair, params)
            nodeViews[node.id] = crosshair
            nodeParams[node.id] = params
            
            if (node.isSwipe) {
                createSwipeEndMarker(node)
            }
            if (node.triggerMode == 0 && !node.dynamicColorUpdate && node.compareToNodeId == null && node.colorCompareX != null) {
                createColorCompareMarker(node)
            }
            if ((node.triggerMode == 1 || node.triggerMode == 2) && node.textZoneStartX != 0) {
                createTextZoneMarkers(node)
            }
        }
        updateMenu()
    }

    fun createViewsForNodes(nodes: List<TargetNode>) {
        for (node in nodes) {
            val crosshair = CrosshairView(service, node)
            var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            if (service.isPlaying || service.isRecording) {
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            val params = WindowManager.LayoutParams(
                dpToPx(60), dpToPx(60),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = node.x - dpToPx(30)
                y = node.y - dpToPx(30)
            }
            crosshair.visibility = if (node.isVisible) View.VISIBLE else View.GONE
            crosshair.setOnClickListener { showEditNodeMenu(node) }
            setupNodeTouchListener(crosshair, params, node)
            windowManager.addView(crosshair, params)
            nodeViews[node.id] = crosshair
            nodeParams[node.id] = params
            
            if (node.isSwipe) {
                createSwipeEndMarker(node)
            }
            if (node.triggerMode == 0 && !node.dynamicColorUpdate && node.compareToNodeId == null && node.colorCompareX != null) {
                createColorCompareMarker(node)
            }
            if ((node.triggerMode == 1 || node.triggerMode == 2) && node.textZoneStartX != 0) {
                createTextZoneMarkers(node)
            }
        }
        updateMenu()
    }

    private fun showMainMenu() {
        setMenuFocusable(false)
        menuContentContainer.removeAllViews()
        val layout = LinearLayout(service).apply { 
            orientation = LinearLayout.VERTICAL
            tag = "main_menu"
        }

        val headerRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 10)
        }
        val titleText = TextView(service).apply {
            text = "Меню Автокликера"
            setTextColor(Color.WHITE)
            setScaledTextSize(16f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val helpBtn = Button(service).apply {
            text = "?"
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40))
            setOnClickListener { showHelpMenu() }
        }
        headerRow.addView(titleText)
        headerRow.addView(helpBtn)

        val isRec = appMode == AppMode.RECORD
        val startBtn = Button(service).apply {
            text = if (isRec) {
                if (service.isRecording) "■ СТОП ЗАПИСЬ" else "🔴 СТАРТ ЗАПИСЬ"
            } else {
                if (service.isPlaying) "■ ОСТАНОВИТЬ" else "▶ СТАРТ"
            }
            setBackgroundColor(if (service.isPlaying || service.isRecording) Color.RED else Color.GREEN)
            setTextColor(Color.WHITE)
            setOnClickListener { 
                if (isRec) {
                    service.toggleRecording()
                } else {
                    service.togglePlay()
                }
                updateMenu()
            }
        }
        
        val addClickBtn = Button(service).apply {
            text = "+ ДОБАВИТЬ МЕТКУ (Клик/Свайп)"
            setOnClickListener { addNode(NodeType.CLICK) }
            visibility = View.VISIBLE
        }

        // color trigger removed

        val clearAllBtn = Button(service).apply {
            text = "ОЧИСТИТЬ ВСЕ МЕТКИ"
            setBackgroundColor(Color.parseColor("#FF5722"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                service.isPlaying = false
                service.nodes.clear()
                nodeViews.values.forEach { windowManager.removeView(it) }
                nodeViews.clear()
                nodeParams.clear()
                swipeEndViews.values.forEach { windowManager.removeView(it) }
                swipeEndViews.clear()
                swipeEndParams.clear()
                textZoneStartViews.values.forEach { windowManager.removeView(it) }
                textZoneStartViews.clear()
                textZoneStartParams.clear()
                textZoneEndViews.values.forEach { windowManager.removeView(it) }
                textZoneEndViews.clear()
                textZoneEndParams.clear()
                nodeCounter = 1
                updateMenu()
                invalidateLines()
            }
        }

        val profileLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        val saveBtn = Button(service).apply {
            text = "Сохр. План"
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showProfileMenu(true) }
        }
        val loadBtn = Button(service).apply {
            text = "Загр. План"
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showProfileMenu(false) }
        }
        profileLayout.addView(saveBtn)
        profileLayout.addView(loadBtn)

        layout.addView(headerRow)
        
        val recordPlayLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        if (isRec) {
            val rpStart = Button(service).apply {
                text = if (service.isRecording) "■ СТОП" else "🔴 ЗАПИСЬ"
                setBackgroundColor(if (service.isRecording) Color.RED else Color.GREEN)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { 
                    service.toggleRecording()
                    updateMenu()
                }
            }
            val rpPlay = Button(service).apply {
                text = if (service.isPlaying) "■ СТОП" else "▶ ПРОИГРАТЬ"
                setBackgroundColor(if (service.isPlaying) Color.RED else Color.parseColor("#FF00C853"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { 
                    service.togglePlay()
                    updateMenu()
                }
            }
            recordPlayLayout.addView(rpStart)
            recordPlayLayout.addView(rpPlay)
            layout.addView(recordPlayLayout)
        } else {
            layout.addView(startBtn)
        }
        
        layout.addView(addClickBtn)
        layout.addView(clearAllBtn)
        layout.addView(profileLayout)

        val scroll = ScrollView(service).apply {
            layoutParams = if (isMenuFullscreen) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(200))
            }
        }
        val list = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        
        service.nodes.forEach { node ->
            val row = LinearLayout(service).apply { 
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 10, 0, 10)
            }
            val title = TextView(service).apply {
                text = "[${node.id}] ${if(node.type==NodeType.CLICK) "Клик" else "Пров."}"
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            val editBtn = Button(service).apply {
                text = "Настр."
                setOnClickListener { showEditNodeMenu(node) }
            }
            val delBtn = Button(service).apply {
                text = "X"
                setBackgroundColor(Color.RED)
                setTextColor(Color.WHITE)
                setOnClickListener { removeNode(node.id) }
            }
            row.addView(title)
            row.addView(editBtn)
            row.addView(delBtn)
            list.addView(row)
        }
        scroll.addView(list)
        layout.addView(scroll)

        menuContentContainer.addView(layout)
    }

    private fun showHelpMenu() {
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val layout = LinearLayout(service).apply { 
            orientation = LinearLayout.VERTICAL
        }
        val title = TextView(service).apply {
            text = "Справка и Гайд"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(18f)
            setPadding(0, dpToPx(10), 0, dpToPx(15))
            gravity = Gravity.CENTER
        }
        layout.addView(title)
        
        val scroll = ScrollView(service).apply {
            layoutParams = if (isMenuFullscreen) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(350))
            }
            tag = "help_menu"
        }
        val textLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), 0, dpToPx(10), dpToPx(20))
        }

        fun createHelpCard(cardTitle: String, cardBody: String, colorHex: String) {
            val card = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor(colorHex))
                setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, dpToPx(15))
                layoutParams = params
            }
            val titleView = TextView(service).apply {
                text = cardTitle
                setTextColor(Color.parseColor("#4CAF50"))
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(15f)
                setPadding(0, 0, 0, dpToPx(8))
            }
            val bodyView = TextView(service).apply {
                text = cardBody
                setTextColor(Color.parseColor("#E0E0E0"))
                setScaledTextSize(13f)
                setLineSpacing(0f, 1.25f)
            }
            card.addView(titleView)
            card.addView(bodyView)
            textLayout.addView(card)
        }

        createHelpCard("👆 Одиночный и Последовательный режим",
            "В этих режимах всё просто:\n\n" +
            "• Одиночный: Вы нажимаете Play, и метка быстро кликает в одной точке, пока вы не нажмете Stop.\n" +
            "• Последовательный: Метки кликают строго по очереди: 1, затем 2, затем 3.\n\n" +
            "💡 Пример: Нужно собрать 20 наград из ящиков? Ставите метку на ящик, выставляете \"Повторений: 20\" и кликер сделает 20 кликов.", "#222222")

        createHelpCard("🧠 Продвинутый режим (Триггеры)",
            "Это мозг кликера! Здесь можно делать так, чтобы кликер принимал решения. Метку можно сделать \"ТРИГГЕРОМ\". Она будет смотреть на экран.\n\n" +
            "Триггер может:\n" +
            "• Ждать нужный цвет пикселя\n" +
            "• Искать фрагмент изображения\n" +
            "• Считывать текст (OCR) с экрана!\n\n" +
            "💡 Пример: Метка [1] (Триггер) смотрит в угол экрана. Вы задали условие ЖДАТЬ цвет. \"Идти если ДА\": к шагу 2. \"Идти если НЕТ\": к шагу 1. В итоге кликер будет сутками ждать кнопку и кликать (или свайпать), только когда она появится!", "#251A1A")

        createHelpCard("🔄 Свайпы (Перетаскивание)",
            "Свайпы настраиваются через настройки метки (шестеренка).\n" +
            "• Простой свайп: Впишите номер метки-цели в поле «Вести к метке №». Скорость свайпа настраивается в поле 'Задержка (мс)'.\n" +
            "• Записанный свайп: Если вы использовали Режим Записи, вы увидите свайп с точной кривой вашей траектории! Кликер в точности повторит изогнутую линию вашего пальца.\n" +
            "💡 Совет: Для TikTok / Reels ставьте метку [1] снизу, а метку [2] сверху. Укажите «Вести к 2», и кликер будет перелистывать видео!", "#1A251A")

        createHelpCard("🎥 Режим Записи жестов и Тайминги",
            "Не хотите настраивать метки? Запишите всё сами!\n\n" +
            "1. Включите режим «Запись» в меню (кнопка 'Режимы').\n" +
            "2. Нажмите красную кнопку [REC] на панели.\n" +
            "3. Пользуйтесь телефоном как обычно: делайте клики, листайте ленту (свайпы по любой траектории).\n" +
            "4. Нажмите кнопку [STOP] (квадрат).\n" +
            "5. Готово! Метки создадутся автоматически.\n\n" +
            "🕒 Тайминги теперь удобные: вы можете в любой настройке (Длительность, Пауза, Анти-Детект) одним нажатием переключать мс (миллисекунды) на сек (секунды)! А также задать настройки таймингов сразу всем меткам.", "#1A1A25")

        createHelpCard("⚙️ Сохранение и Импорт Профилей",
            "Сценариев может быть много. В меню нажмите \"СЦЕНАРИИ\".\n" +
            "Тут можно сохранять профили. А еще появились функции ЭКСПОРТ и ИМПОРТ в файл .json!\n" +
            "Вы можете настроить сложный бот, экспортировать файл и скинуть его другу — он просто загрузит его и запустит!", "#202020")

        createHelpCard("🕵️ Анти-Детект",
            "Позволяет сделать клики максимально похожими на человеческие.\n\n" +
            "• Рандомизация паузы: Добавляет случайное ожидание к стандартной паузе.\n" +
            "• Рандомизация координат (px): Клики/свайпы происходят со случайным отклонением в радиусе. Есть удобная кнопка 'Применить ко всем меткам'!", "#25201A")

        createHelpCard("🖼 Изображения и Поиск со смещением",
            "В настройках триггера по фрагменту можно задать % совпадения и «Допуск смещения поиска (px)». Даже если кнопка сдвинется, кликер её найдёт!", "#251A25")

        createHelpCard("☕ Кофеин (Не спать)",
            "В главном меню есть переключатель КОФЕИН. Включите его, и экран вашего устройства не погаснет. Идеально для долгого фарма!", "#1A2525")

        scroll.addView(textLayout)
        layout.addView(scroll)

        val backBtn = Button(service).apply {
            text = "НАЗАД В МЕНЮ"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(10), dpToPx(15), dpToPx(10), dpToPx(10))
            layoutParams = params
            setOnClickListener { showMainMenu() }
        }
        layout.addView(backBtn)

        menuContentContainer.addView(layout)
    }

    private fun showModeMenu() {
        setMenuFocusable(false)
        menuContentContainer.removeAllViews()
        val scroll = ScrollView(service).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val layout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(10, 10, 10, 10) }
        
        layout.addView(TextView(service).apply { text = "Режим работы кликера"; setTextColor(Color.WHITE); setScaledTextSize(16f); setPadding(0, 0, 0, 20) })

        val modes = listOf(
            AppMode.SEQUENTIAL to "Последовательный (Цепочка)",
            AppMode.ADVANCED to "Продвинутый (Триггеры, Логика)",
            AppMode.RECORD to "Запись (Клик по экрану ставит метку)"
        )

        modes.forEach { (mode, desc) ->
            val btn = Button(service).apply {
                text = desc
                setBackgroundColor(if (appMode == mode) Color.parseColor("#4CAF50") else Color.parseColor("#555555"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    if (mode == AppMode.SEQUENTIAL && service.nodes.size > 1) {
                        val first = service.nodes.firstOrNull()
                        service.nodes.clear()
                        nodeViews.values.forEach { windowManager.removeView(it) }
                        nodeViews.clear()
                        nodeParams.clear()
                        swipeEndViews.values.forEach { windowManager.removeView(it) }
                        swipeEndViews.clear()
                        swipeEndParams.clear()
                        textZoneStartViews.values.forEach { windowManager.removeView(it) }
                        textZoneStartViews.clear()
                        textZoneStartParams.clear()
                        textZoneEndViews.values.forEach { windowManager.removeView(it) }
                        textZoneEndViews.clear()
                        textZoneEndParams.clear()
                        colorCompareViews.values.forEach { windowManager.removeView(it) }
                        colorCompareViews.clear()
                        colorCompareParams.clear()
                        if (first != null) {
                            service.nodes.add(first)
                            recreateAllNodeViews()
                        }
                    } else if (mode == AppMode.SEQUENTIAL) {
                        // In sequential mode, triggers can stay.
                    }
                    appMode = mode
                    showModeMenu() // redraw
                    
                    if (floatingControlBar != null) {
                        windowManager.removeView(floatingControlBar)
                        floatingControlBar = null
                        showFloatingControlBar()
                    }
                    invalidateLines()
                }
            }
            layout.addView(btn)
        }
        scroll.tag = "mode_menu"
        scroll.addView(layout)
        menuContentContainer.addView(scroll)
    }

    private fun showSettingsMenu() {
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val scroll = ScrollView(service).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            tag = "settings_menu"
        }
        val layout = LinearLayout(service).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }
        
        layout.addView(TextView(service).apply { text = "Настройки интерфейса"; setTextColor(Color.WHITE); setScaledTextSize(16f); setPadding(0, 0, 0, 20) })

        val alphaLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        alphaLayout.addView(TextView(service).apply { text = "Прозрачность UI: "; setTextColor(Color.WHITE) })
        alphaLayout.addView(Button(service).apply { text = "-"; setOnClickListener { if(uiAlpha > 0.2f) { uiAlpha -= 0.1f; saveUISettings(); applyUISettings(); showSettingsMenu() } } })
        alphaLayout.addView(TextView(service).apply { text = String.format("%.1f", uiAlpha); setTextColor(Color.YELLOW); setPadding(10,0,10,0) })
        alphaLayout.addView(Button(service).apply { text = "+"; setOnClickListener { if(uiAlpha < 1.0f) { uiAlpha += 0.1f; saveUISettings(); applyUISettings(); showSettingsMenu() } } })
        layout.addView(alphaLayout)

        val scaleLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        scaleLayout.addView(TextView(service).apply { text = "Масштаб UI: "; setTextColor(Color.WHITE) })
        scaleLayout.addView(Button(service).apply { text = "-"; setOnClickListener { if(uiScale > 0.5f) { uiScale -= 0.1f; saveUISettings(); applyUIScaleChange() } } })
        scaleLayout.addView(TextView(service).apply { text = String.format("%.1f", uiScale); setTextColor(Color.YELLOW); setPadding(10,0,10,0) })
        scaleLayout.addView(Button(service).apply { text = "+"; setOnClickListener { if(uiScale < 2.0f) { uiScale += 0.1f; saveUISettings(); applyUIScaleChange() } } })
        layout.addView(scaleLayout)
        
        layout.addView(TextView(service).apply { text = "Панель инструментов"; setTextColor(Color.WHITE); setScaledTextSize(14f); setPadding(0, 15, 0, 10) })
        
        val pEye = android.widget.CheckBox(service).apply {
            text = "Кнопка 'Видимость меток' (👁)"
            setTextColor(Color.WHITE)
            isChecked = showEyeBtn
            setOnCheckedChangeListener { _, c -> showEyeBtn = c; saveUISettings(); recreateFloatingControlBar() }
        }
        val pLines = android.widget.CheckBox(service).apply {
            text = "Кнопка 'Линии связи' (🕸)"
            setTextColor(Color.WHITE)
            isChecked = showLinesBtn
            setOnCheckedChangeListener { _, c -> showLinesBtn = c; saveUISettings(); recreateFloatingControlBar() }
        }
        val pHotbar = android.widget.CheckBox(service).apply {
            text = "Кнопка 'Быстрая панель' (⚡)"
            setTextColor(Color.WHITE)
            isChecked = showHotbarBtn
            setOnCheckedChangeListener { _, c -> showHotbarBtn = c; saveUISettings(); recreateFloatingControlBar() }
        }
        layout.addView(pEye)
        layout.addView(pLines)
        layout.addView(pHotbar)

        val debugBtn = Button(service).apply {
            text = if (isDebugWindowVisible) "Дебаг Лог: ВКЛ" else "Дебаг Лог: ВЫКЛ"
            setOnClickListener {
                toggleDebugWindow()
                saveUISettings()
                showSettingsMenu()
            }
        }
        layout.addView(debugBtn)

        val caffeineBtn = Button(service).apply {
            text = if (isCaffeineEnabled) "КОФЕИН (Не спать): ВКЛ" else "КОФЕИН (Не спать): ВЫКЛ"
            setBackgroundColor(if (isCaffeineEnabled) Color.parseColor("#4CAF50") else Color.parseColor("#555555"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                isCaffeineEnabled = !isCaffeineEnabled
                saveUISettings()
                updateFloatingControlBarCaffeine()
                showSettingsMenu()
            }
        }
        layout.addView(caffeineBtn)

        layout.addView(TextView(service).apply { text = "Настройки выполнения (Глобальные)"; setTextColor(Color.WHITE); setScaledTextSize(16f); setPadding(0, 20, 0, 10) })

        val maxCyclesLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 10) }
        maxCyclesLayout.addView(TextView(service).apply { text = "Общее количество циклов (0 = ∞): "; setTextColor(Color.WHITE) })
        val maxCyclesEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (service.maxCycles != null && service.maxCycles!! > 0) service.maxCycles.toString() else "0")
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        maxCyclesLayout.addView(maxCyclesEdit)
        layout.addView(maxCyclesLayout)

        val maxTimeLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 10) }
        maxTimeLayout.addView(TextView(service).apply { text = "Время работы (минуты, 0 = ∞): "; setTextColor(Color.WHITE) })
        val maxTimeEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (service.maxPlayDurationMs != null && service.maxPlayDurationMs!! > 0) (service.maxPlayDurationMs!! / 60000L).toString() else "0")
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        maxTimeLayout.addView(maxTimeEdit)
        layout.addView(maxTimeLayout)

        val multitouchLayout = LinearLayout(service).apply { 
            orientation = LinearLayout.HORIZONTAL 
            setPadding(0, 10, 0, 10)
            gravity = Gravity.CENTER_VERTICAL
        }
        val multitouchCheck = android.widget.CheckBox(service).apply {
            text = "Синхронный Мультитач (может сбоить камеру)"
            setTextColor(Color.WHITE)
            isChecked = service.enableMultitouch
            setOnCheckedChangeListener { _, isChecked ->
                service.enableMultitouch = isChecked
                saveUISettings()
                if (isChecked) {
                    android.widget.Toast.makeText(service, "Внимание! Во многих играх мультитач вызывает сбой масштабирования камеры.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        multitouchLayout.addView(multitouchCheck)
        layout.addView(multitouchLayout)

        val extremeSpeedLayout = LinearLayout(service).apply { 
            orientation = LinearLayout.HORIZONTAL 
            setPadding(0, 10, 0, 10)
            gravity = Gravity.CENTER_VERTICAL
        }
        val extremeSpeedCheck = android.widget.CheckBox(service).apply {
            text = "Разрешить задержку 0 мс (ОПАСНО: может повесить устройство)"
            setTextColor(Color.RED)
            isChecked = service.allowExtremeSpeed
            setOnCheckedChangeListener { _, isChecked ->
                service.allowExtremeSpeed = isChecked
            }
        }
        extremeSpeedLayout.addView(extremeSpeedCheck)
        layout.addView(extremeSpeedLayout)

        
        val saveBtn = Button(service).apply {
            text = "Сохранить и Выйти"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val cyclesStr = maxCyclesEdit.text.toString().toIntOrNull() ?: 0
                service.maxCycles = if (cyclesStr > 0) cyclesStr else null
                val minsStr = maxTimeEdit.text.toString().toLongOrNull() ?: 0L
                service.maxPlayDurationMs = if (minsStr > 0) minsStr * 60000L else null
                showMainMenu()
            }
        }
        val margins = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        margins.setMargins(0, 20, 0, 0)
        saveBtn.layoutParams = margins
        layout.addView(saveBtn)

        scroll.addView(layout)
        menuContentContainer.addView(scroll)
    }

    fun applyUISettings() {
        floatingControlBar?.alpha = uiAlpha
        modMenu?.alpha = uiAlpha
        // We removed scaleX and scaleY because it breaks tap targets.
        // Instead, the user needs to restart the menu (we'll implement this).
    }

    private fun showProfileMenu(isSaving: Boolean) {
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val layout = LinearLayout(service).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
            tag = if (isSaving) "profile_menu_save" else "profile_menu_load"
        }
        
        layout.addView(TextView(service).apply {
            text = if (isSaving) "Сохранить профиль" else "Загрузить профиль"
            setScaledTextSize(18f)
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        })

        val nameEdit = EditText(service).apply {
            hint = "Имя профиля (Сценарий 1)"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
        }
        layout.addView(nameEdit)

        val actionBtn = Button(service).apply {
            text = if (isSaving) "СОХРАНИТЬ" else "ЗАГРУЗИТЬ"
            setBackgroundColor(if (isSaving) Color.parseColor("#4CAF50") else Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val name = nameEdit.text.toString().takeIf { it.isNotBlank() } ?: "Сценарий 1"
                if (isSaving) {
                    service.saveProfile(name)
                } else {
                    service.loadProfile(name)
                }
                showMainMenu()
            }
        }
        layout.addView(actionBtn)

        val profiles = service.getSavedProfiles()
        if (profiles.isNotEmpty()) {
            layout.addView(TextView(service).apply {
                text = "Сохраненные профили:"
                setTextColor(Color.parseColor("#E0E0E0"))
                setPadding(0, 20, 0, 10)
            })
            val scroll = ScrollView(service).apply {
                layoutParams = if (isMenuFullscreen) {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                } else {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(150))
                }
            }
            val profilesLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
            }
            profiles.forEach { profileName ->
                val row = LinearLayout(service).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }

                val profileBtn = Button(service).apply {
                    text = profileName
                    setBackgroundColor(Color.parseColor("#444444"))
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        nameEdit.setText(profileName)
                        if (!isSaving) {
                            service.loadProfile(profileName)
                            showMainMenu()
                        }
                    }
                }
                row.addView(profileBtn)

                if (!isSaving) {
                    val appendBtn = Button(service).apply {
                        text = "+ ВСТАВИТЬ"
                        setBackgroundColor(Color.parseColor("#2E7D32"))
                        setTextColor(Color.WHITE)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        setOnClickListener {
                            service.loadProfile(profileName, append = true)
                            showMainMenu()
                        }
                    }
                    row.addView(appendBtn)
                }

                val deleteBtn = Button(service).apply {
                    text = "X"
                    setBackgroundColor(Color.parseColor("#D32F2F"))
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        service.deleteProfile(profileName)
                        showProfileMenu(isSaving) // refresh the menu
                    }
                }
                row.addView(deleteBtn)

                profilesLayout.addView(row)
            }
            scroll.addView(profilesLayout)
            layout.addView(scroll)
        }

        val clipboardBtn = Button(service).apply {
            text = if (isSaving) "ЭКСПОРТ В БУФЕР ОБМЕНА" else "ИМПОРТ ИЗ БУФЕРА ОБМЕНА"
            setBackgroundColor(Color.parseColor("#555555"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (isSaving) {
                    service.exportProfileToClipboard()
                } else {
                    service.importProfileFromClipboard()
                }
                showMainMenu()
            }
        }
        layout.addView(clipboardBtn)

        val fileBtn = Button(service).apply {
            text = if (isSaving) "ЭКСПОРТ В ФАЙЛ" else "ИМПОРТ ИЗ ФАЙЛА"
            setBackgroundColor(Color.parseColor("#555555"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                if (isSaving) {
                    val name = nameEdit.text.toString().takeIf { it.isNotBlank() } ?: "Сценарий 1"
                    val prefs = service.getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
                    var jsonToExport = prefs.getString(name, null)
                    
                    if (jsonToExport == null) {
                        val obj = org.json.JSONObject()
                        val metrics = service.resources.displayMetrics
                        obj.put("screenWidth", metrics.widthPixels)
                        obj.put("screenHeight", metrics.heightPixels)
                        val arr = org.json.JSONArray()
                        for (node in service.nodes) arr.put(node.toJson())
                        obj.put("nodes", arr)
                        jsonToExport = obj.toString()
                    }
                    
                    MainActivity.pendingExportData = jsonToExport
                    
                    val intent = android.content.Intent(service, MainActivity::class.java).apply {
                        action = "ACTION_EXPORT_PROFILE"
                        putExtra("profile_name", name)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    service.startActivity(intent)
                } else {
                    val intent = android.content.Intent(service, MainActivity::class.java).apply {
                        action = "ACTION_IMPORT_PROFILE"
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    service.startActivity(intent)
                }
                showMainMenu()
                showModMenu()
            }
        }
        layout.addView(fileBtn)

        if (isSaving) {
            val shareBtn = Button(service).apply {
                text = "ПОДЕЛИТЬСЯ (TELEGRAM И ДР.)"
                setBackgroundColor(Color.parseColor("#446688"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    val name = nameEdit.text.toString().takeIf { it.isNotBlank() } ?: "Сценарий 1"
                    val prefs = service.getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
                    var jsonToExport = prefs.getString(name, null)
                    
                    if (jsonToExport == null) {
                        val obj = org.json.JSONObject()
                        val metrics = service.resources.displayMetrics
                        obj.put("screenWidth", metrics.widthPixels)
                        obj.put("screenHeight", metrics.heightPixels)
                        val arr = org.json.JSONArray()
                        for (node in service.nodes) arr.put(node.toJson())
                        obj.put("nodes", arr)
                        jsonToExport = obj.toString()
                    }
                    
                    MainActivity.pendingExportData = jsonToExport
                    
                    val intent = android.content.Intent(service, MainActivity::class.java).apply {
                        action = "ACTION_SHARE_PROFILE"
                        putExtra("profile_name", name)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    service.startActivity(intent)
                    
                    showMainMenu()
                    showModMenu()
                }
            }
            layout.addView(shareBtn)
        }

        layout.addView(Button(service).apply {
            text = "ОТМЕНА"
            setOnClickListener { showMainMenu() }
        })

        menuContentContainer.addView(layout)
    }

    private fun createColorRow(title: String, onColorSelected: (Int) -> Unit): View {
        val layout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 5) }
        layout.addView(TextView(service).apply { text = title; setTextColor(Color.parseColor("#EEEEEE")); setScaledTextSize(12f) })
        
        val scroll = android.widget.HorizontalScrollView(service)
        val colorsLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.BLACK)
        
        for (c in colors) {
            val btn = Button(service).apply {
                setBackgroundColor(c)
                layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30)).apply {
                    setMargins(5, 5, 5, 5)
                }
                setOnClickListener { onColorSelected(c) }
            }
            colorsLayout.addView(btn)
        }
        scroll.addView(colorsLayout)
        layout.addView(scroll)
        return layout
    }

    private fun createThemedSpinnerAdapter(items: Array<String>): android.widget.ArrayAdapter<String> {
        return object : android.widget.ArrayAdapter<String>(service, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.setBackgroundColor(Color.parseColor("#444444"))
                view.setPadding(20, 20, 20, 20)
                return view
            }
            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.WHITE)
                view.setBackgroundColor(Color.parseColor("#333333"))
                view.setPadding(30, 30, 30, 30)
                return view
            }
        }
    }

    private fun createTimeInputRow(label: String, initialMs: Long): Pair<LinearLayout, () -> Long> {
        val row = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 5, 0, 5)
        }
        
        val tv = TextView(service).apply {
            text = label
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val edit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(60), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        
        val toggleBtn = Button(service).apply {
            text = "мс"
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setPadding(10, 0, 10, 0)
        }
        
        var isSeconds = false
        edit.setText(initialMs.toString())
        
        toggleBtn.setOnClickListener {
            val currentVal = edit.text.toString().toFloatOrNull() ?: 0f
            isSeconds = !isSeconds
            toggleBtn.text = if (isSeconds) "сек" else "мс"
            
            if (isSeconds) {
                edit.setText((currentVal / 1000f).toString())
            } else {
                edit.setText((currentVal * 1000f).toLong().toString())
            }
        }
        
        row.addView(tv)
        row.addView(edit)
        row.addView(toggleBtn)
        
        val getValueMs = { 
            val v = edit.text.toString().toFloatOrNull() ?: 0f
            if (isSeconds) (v * 1000).toLong() else v.toLong()
        }
        
        return Pair(row, getValueMs)
    }

    private fun swapNodes(id1: Int, id2: Int) {
        val node1 = service.nodes.find { it.id == id1 }
        val node2 = service.nodes.find { it.id == id2 }
        if (node1 == null || node2 == null) return
        
        node1.id = id2
        node2.id = id1
        
        service.nodes.forEach { n ->
            if (n.nextNodeIdOnSuccess == id1) n.nextNodeIdOnSuccess = id2
            else if (n.nextNodeIdOnSuccess == id2) n.nextNodeIdOnSuccess = id1
            
            if (n.nextNodeIdOnFail == id1) n.nextNodeIdOnFail = id2
            else if (n.nextNodeIdOnFail == id2) n.nextNodeIdOnFail = id1
            
            if (n.compareToNodeId == id1) n.compareToNodeId = id2
            else if (n.compareToNodeId == id2) n.compareToNodeId = id1
            
            if (n.ocrCompareToNodeId == id1) n.ocrCompareToNodeId = id2
            else if (n.ocrCompareToNodeId == id2) n.ocrCompareToNodeId = id1
            
            if (n.swipeTargetNodeId == id1) n.swipeTargetNodeId = id2
            else if (n.swipeTargetNodeId == id2) n.swipeTargetNodeId = id1
            
            if (n.syncWithNodeIds.isNotEmpty()) {
                val syncIds = n.syncWithNodeIds.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
                for (i in 0 until syncIds.size) {
                    if (syncIds[i] == id1) syncIds[i] = id2
                    else if (syncIds[i] == id2) syncIds[i] = id1
                }
                n.syncWithNodeIds = syncIds.joinToString(",")
            }
        }
        
        service.nodes.sortBy { it.id }
        
        try { nodeViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        nodeViews.clear()
        nodeParams.clear()
        
        try { swipeEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        swipeEndViews.clear()
        swipeEndParams.clear()
        
        try { textZoneStartViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        textZoneStartViews.clear()
        textZoneStartParams.clear()
        
        try { textZoneEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        textZoneEndViews.clear()
        textZoneEndParams.clear()
        
        try { colorCompareViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        colorCompareViews.clear()
        colorCompareParams.clear()
        
        createViewsForNodes(service.nodes)
        updateMenu()
        invalidateLines()
        service.autoSave()
    }

    private fun showEditNodeMenu(node: TargetNode) {
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val layout = ScrollView(service).apply {
            tag = "edit_menu"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val content = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }

        val headerLayout = LinearLayout(service).apply { 
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            setPadding(0, 0, 0, 20)
        }
        val headerTitleLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        
        val btnLeft = Button(service).apply { 
            text = "⬅"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(10, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            isEnabled = node.id > 1
            setOnClickListener {
                swapNodes(node.id, node.id - 1)
            }
        }
        
        val tvNodeTitle = TextView(service).apply {
            text = " Метка [${node.id}] "
            setTextColor(Color.WHITE)
            setScaledTextSize(16f)
            gravity = Gravity.CENTER
        }
        
        val btnRight = Button(service).apply { 
            text = "➡"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(10, 0, 10, 0)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
            val maxId = service.nodes.maxOfOrNull { it.id } ?: 1
            isEnabled = node.id < maxId
            setOnClickListener {
                swapNodes(node.id, node.id + 1)
            }
        }
        
        headerTitleLayout.addView(btnLeft)
        headerTitleLayout.addView(tvNodeTitle)
        headerTitleLayout.addView(btnRight)
        
        headerLayout.addView(headerTitleLayout)
        val typeSpinner = android.widget.Spinner(service).apply {
            val items = arrayOf("🎯 Действие (Клик/Свайп)", "👁 Условие (Поиск)", "⚡ Вызов скрипта", "🔀 Менеджер (Логика)")
            val adapter = android.widget.ArrayAdapter(service, android.R.layout.simple_spinner_dropdown_item, items)
            this.adapter = adapter
            setSelection(when(node.type) {
                NodeType.CLICK -> 0
                NodeType.CHECK_COLOR -> 1
                NodeType.MACRO -> 2
                NodeType.MANAGER -> 3
            })
            visibility = if (appMode == AppMode.ADVANCED) View.VISIBLE else View.GONE
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                    val newType = when(pos) {
                        0 -> NodeType.CLICK
                        1 -> NodeType.CHECK_COLOR
                        2 -> NodeType.MACRO
                        3 -> NodeType.MANAGER
                        else -> NodeType.CLICK
                    }
                    if (node.type != newType) {
                        node.type = newType
                        node.crosshairColor = if (node.type == NodeType.CLICK) Color.RED else if (node.type == NodeType.MANAGER) Color.parseColor("#9C27B0") else Color.BLUE
                        nodeViews[node.id]?.invalidate()
                        showEditNodeMenu(node)
                        nodeViews[node.id]?.invalidate()
                    }
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
        }
        headerLayout.addView(typeSpinner)
        
        if (appMode == AppMode.ADVANCED) {
            if (node.type == NodeType.CHECK_COLOR || node.type == NodeType.MACRO || node.type == NodeType.MANAGER) {
                val skipSwitch = android.widget.Switch(service).apply {
                    text = "Функция (Пропуск в очереди)"
                    setTextColor(Color.CYAN)
                    isChecked = node.skipSequentialExecution
                    setOnCheckedChangeListener { _, isChecked ->
                        node.skipSequentialExecution = isChecked
                    }
                    setPadding(20, 0, 0, 0)
                }
                headerLayout.addView(skipSwitch)
            }
            
            val threadSwitch = android.widget.Switch(service).apply {
                text = "Отдельный поток (Параллельно)"
                setTextColor(Color.parseColor("#FFA500")) // Orange
                isChecked = node.isIndependentThread
                setOnCheckedChangeListener { _, isChecked ->
                    node.isIndependentThread = isChecked
                }
                setPadding(20, 0, 0, 0)
            }
            headerLayout.addView(threadSwitch)
        }
        content.addView(headerLayout)

        fun addSection(title: String, hasChanges: Boolean, buildContent: (LinearLayout) -> Unit): LinearLayout {
            val sectionLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                val marginParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                marginParams.setMargins(0, dpToPx(5), 0, dpToPx(5))
                layoutParams = marginParams
                
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#252525"))
                    cornerRadius = dpToPx(12).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#333333"))
                }
                clipToOutline = true
            }
            
            val headerLayout = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
            }
            
            val titleText = TextView(service).apply {
                text = title
                setTextColor(Color.parseColor("#EEEEEE"))
                setScaledTextSize(14f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val checkIcon = TextView(service).apply {
                text = " ● "
                setTextColor(Color.parseColor("#4CAF50"))
                setScaledTextSize(12f)
                visibility = if (hasChanges) View.VISIBLE else View.GONE
            }
            
            val icon = TextView(service).apply {
                text = "﹀"
                setTextColor(Color.parseColor("#888888"))
                setScaledTextSize(16f)
            }
            
            headerLayout.addView(titleText)
            headerLayout.addView(checkIcon)
            headerLayout.addView(icon)
            
            val bodyLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(15), 0, dpToPx(15), dpToPx(15))
                visibility = View.GONE
            }
            
            buildContent(bodyLayout)
            
            headerLayout.setOnClickListener {
                if (bodyLayout.visibility == View.VISIBLE) {
                    bodyLayout.visibility = View.GONE
                    icon.text = "﹀"
                } else {
                    bodyLayout.visibility = View.VISIBLE
                    icon.text = "︿"
                }
            }
            
            sectionLayout.addView(headerLayout)
            sectionLayout.addView(bodyLayout)
            content.addView(sectionLayout)
            return sectionLayout
        }

        // --- SECTION: Внешний вид ---
        val toggleVisBtn = Button(service).apply {
            text = if (node.isVisible) "Скрыть маркер" else "Показать маркер"
            setOnClickListener {
                node.isVisible = !node.isVisible
                text = if (node.isVisible) "Скрыть маркер" else "Показать маркер"
                nodeViews[node.id]?.visibility = if (node.isVisible) View.VISIBLE else View.GONE
                swipeEndViews[node.id]?.visibility = if (node.isVisible) View.VISIBLE else View.GONE
                invalidateLines()
            }
        }

        val sizeLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 10); gravity = Gravity.CENTER_VERTICAL }
        sizeLayout.addView(TextView(service).apply { text = "Размер: "; setTextColor(Color.WHITE) })
        sizeLayout.addView(Button(service).apply { 
            text = " - "
            setOnClickListener { 
                node.sizeScale = Math.max(0.5f, node.sizeScale - 0.2f)
                nodeViews[node.id]?.invalidate()
            } 
        })
        sizeLayout.addView(Button(service).apply { 
            text = " + "
            setOnClickListener { 
                node.sizeScale = Math.min(3.0f, node.sizeScale + 0.2f)
                nodeViews[node.id]?.invalidate()
            } 
        })

        val defCrosshair = if (node.type == NodeType.CLICK) Color.RED else Color.BLUE
        val defNumber = if (node.type == NodeType.CLICK) Color.WHITE else Color.YELLOW
        val hasViewChanges = !node.isVisible || node.sizeScale != 1.0f || node.crosshairColor != defCrosshair || node.numberColor != defNumber
        
        addSection("🎨 Внешний вид (Цвет / Размер)", hasViewChanges) { body ->
            body.addView(toggleVisBtn)
            body.addView(sizeLayout)
            body.addView(createColorRow("Цвет метки") { color ->
                node.crosshairColor = color
                nodeViews[node.id]?.invalidate()
            })
            body.addView(createColorRow("Цвет цифры") { color ->
                node.numberColor = color
                nodeViews[node.id]?.invalidate()
            })
        }

        // --- SECTION: Тайминги ---
        val (clickDurRow, getClickDurRowMs) = createTimeInputRow("Длительность нажатия:", node.clickDurationMs)
        val (delayRow, getDelayRowMs) = createTimeInputRow("Пауза перед след шагом:", node.delayAfterMs)
        
        val tvRepetitions = TextView(service).apply { text="Количество повторений:"; setTextColor(Color.WHITE) }
        val repetitionsEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.repetitions.toString())
            setTextColor(Color.WHITE)
        }

        val applyTimingsBtn = Button(service).apply {
            text = "ПРИМЕНИТЬ КО ВСЕМ МЕТКАМ"
            setBackgroundColor(Color.parseColor("#885555"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(10), 0, 0)
            }
            setOnClickListener {
                val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Подтверждение")
                    .setMessage("Применить текущие настройки таймингов ко всем меткам?")
                    .setPositiveButton("Да") { _, _ ->
                        val nDur = getClickDurRowMs()
                        val nDel = getDelayRowMs()
                        val nRep = repetitionsEdit.text.toString().toIntOrNull() ?: 1
                        for (nd in service.nodes) {
                            nd.clickDurationMs = nDur
                            nd.delayAfterMs = nDel
                            nd.repetitions = nRep
                        }
                        Toast.makeText(service, "Применено", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .create()
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                dialog.show()
            }
        }

        val hasTimingChanges = node.clickDurationMs != 30L || node.delayAfterMs != 300L || node.repetitions != 1
        val timingsSection = addSection("⏱ Тайминги и Задержки", hasTimingChanges) { body ->
            body.addView(clickDurRow)
            body.addView(delayRow)
            body.addView(tvRepetitions)
            body.addView(repetitionsEdit)
            body.addView(applyTimingsBtn)
        }

        // --- SECTION: Анти-Детект ---
        val (randomDelayRow, getRandomDelayRowMs) = createTimeInputRow("Рандомизация паузы до:", node.randomizeDelayMs)
        
        val tvRandomRadius = TextView(service).apply { text="Рандомизация координат (px):"; setTextColor(Color.WHITE) }
        val randomRadiusEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.randomizeRadius.toString())
            setTextColor(Color.WHITE)
        }
        
        val applyAntiDetectBtn = Button(service).apply {
            text = "ПРИМЕНИТЬ КО ВСЕМ МЕТКАМ"
            setBackgroundColor(Color.parseColor("#885555"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(10), 0, 0)
            }
            setOnClickListener {
                val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Подтверждение")
                    .setMessage("Применить текущие настройки анти-детект ко всем меткам?")
                    .setPositiveButton("Да") { _, _ ->
                        val nRD = getRandomDelayRowMs()
                        val nRR = randomRadiusEdit.text.toString().toIntOrNull() ?: 0
                        for (nd in service.nodes) {
                            nd.randomizeDelayMs = nRD
                            nd.randomizeRadius = nRR
                        }
                        Toast.makeText(service, "Применено", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .create()
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                dialog.show()
            }
        }

        // --- MANAGER SECTION ---
        var managerSection: View? = null
        if (node.type == NodeType.MANAGER) {
            managerSection = addSection("🔀 Настройки Менеджера", true) { body ->
                val desc = TextView(service).apply {
                    text = "Менеджер по очереди проверяет Триггеры. Если Триггер срабатывает, происходит переход к указанной Метке."
                    setTextColor(Color.LTGRAY)
                    setScaledTextSize(12f)
                    setPadding(0, 0, 0, 10)
                }
                body.addView(desc)
                
                val listContainer = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
                body.addView(listContainer)
                
                fun renderRoutes() {
                    listContainer.removeAllViews()
                    for ((index, route) in node.managerRoutes.withIndex()) {
                        val row = LinearLayout(service).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(0, 5, 0, 5)
                            background = android.graphics.drawable.GradientDrawable().apply {
                                setColor(Color.parseColor("#333333"))
                                setCornerRadius(8f)
                            }
                        }
                        
                        val txt1 = TextView(service).apply { text = "Триггер № "; setTextColor(Color.WHITE) }
                        val checkEdit = EditText(service).apply {
                            inputType = InputType.TYPE_CLASS_NUMBER
                            setText(if(route.checkNodeId != -1) route.checkNodeId.toString() else "")
                            setTextColor(Color.WHITE)
                            layoutParams = LinearLayout.LayoutParams(dpToPx(40), WindowManager.LayoutParams.WRAP_CONTENT)
                            addTextChangedListener(object: android.text.TextWatcher {
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    s?.toString()?.toIntOrNull()?.let { route.checkNodeId = it }
                                }
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            })
                        }
                        val txt2 = TextView(service).apply { text = " ➔ Метка № "; setTextColor(Color.WHITE) }
                        val goEdit = EditText(service).apply {
                            inputType = InputType.TYPE_CLASS_NUMBER
                            setText(if(route.onSuccessGoToId != -1) route.onSuccessGoToId.toString() else "")
                            setTextColor(Color.WHITE)
                            layoutParams = LinearLayout.LayoutParams(dpToPx(40), WindowManager.LayoutParams.WRAP_CONTENT)
                            addTextChangedListener(object: android.text.TextWatcher {
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    s?.toString()?.toIntOrNull()?.let { route.onSuccessGoToId = it }
                                }
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            })
                        }
                        val delBtn = Button(service).apply {
                            text = "X"
                            setTextColor(Color.RED)
                            setBackgroundColor(Color.TRANSPARENT)
                            setPadding(5, 5, 5, 5)
                            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                            setOnClickListener {
                                val mut = node.managerRoutes.toMutableList()
                                mut.removeAt(index)
                                node.managerRoutes = mut
                                renderRoutes()
                            }
                        }
                        
                        row.addView(txt1)
                        row.addView(checkEdit)
                        row.addView(txt2)
                        row.addView(goEdit)
                        row.addView(delBtn)
                        listContainer.addView(row)
                    }
                    
                    val addBtn = Button(service).apply {
                        text = "+ ДОБАВИТЬ УСЛОВИЕ"
                        setTextColor(Color.parseColor("#4CAF50"))
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener {
                            val mut = node.managerRoutes.toMutableList()
                            mut.add(ManagerRoute(-1, -1))
                            node.managerRoutes = mut
                            renderRoutes()
                        }
                    }
                    listContainer.addView(addBtn)
                }
                
                renderRoutes()
                
                val fallbackLayout = LinearLayout(service).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 20, 0, 0)
                    gravity = Gravity.CENTER_VERTICAL
                }
                fallbackLayout.addView(TextView(service).apply { text = "Если ничего не совпало, идти к №: "; setTextColor(Color.WHITE) })
                val fallbackEdit = EditText(service).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(node.nextNodeIdOnFail?.toString() ?: "")
                    hint = "(Стоп)"
                    setHintTextColor(Color.LTGRAY)
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(dpToPx(60), WindowManager.LayoutParams.WRAP_CONTENT)
                    addTextChangedListener(object: android.text.TextWatcher {
                        override fun afterTextChanged(s: android.text.Editable?) {
                            val v = s?.toString()?.toIntOrNull()
                            if (v != null) node.nextNodeIdOnFail = v else node.nextNodeIdOnFail = null
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
                fallbackLayout.addView(fallbackEdit)
                body.addView(fallbackLayout)
            }
        }
        
        val hasAntiDetect = node.randomizeDelayMs > 0 || node.randomizeRadius > 0
        val antiDetectSection = addSection("🛡 Анти-Детект (Случайности)", hasAntiDetect) { body ->
            body.addView(randomDelayRow)
            body.addView(tvRandomRadius)
            body.addView(randomRadiusEdit)
            body.addView(applyAntiDetectBtn)
        }
        if (node.type == NodeType.CHECK_COLOR) {
            antiDetectSection.visibility = View.GONE
        }
        
        val macroSection = addSection("⚡ Настройки вызова скрипта", node.macroProfileName != null) { body ->
            val tvMacroDesc = TextView(service).apply {
                text = "Если условие выполнится, будет загружен и запущен выбранный профиль."
                setTextColor(Color.LTGRAY)
                setScaledTextSize(12f)
                setPadding(0, 0, 0, 10)
            }
            body.addView(tvMacroDesc)
            
            val prefs = service.getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
            val allKeys = prefs.all.keys.toList()
            val spinnerItems = mutableListOf("— Не выбрано —")
            spinnerItems.addAll(allKeys)
            
            val macroSpinner = android.widget.Spinner(service).apply {
                val adapter = android.widget.ArrayAdapter(service, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
                this.adapter = adapter
                
                val currentIdx = spinnerItems.indexOf(node.macroProfileName ?: "")
                if (currentIdx != -1) {
                    setSelection(currentIdx)
                }
                onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                        if (pos == 0) {
                            node.macroProfileName = null
                        } else {
                            node.macroProfileName = spinnerItems[pos]
                        }
                    }
                    override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
                }
            }
            body.addView(macroSpinner)
            
            val parallelSwitch = android.widget.Switch(service).apply {
                text = "Исполнять фоном (Не прерывать текущий сценарий)"
                setTextColor(Color.parseColor("#FFA500"))
                isChecked = node.macroRunParallel
                setOnCheckedChangeListener { _, isChecked ->
                    node.macroRunParallel = isChecked
                }
                setPadding(20, 20, 0, 0)
            }
            body.addView(parallelSwitch)
        }
        if (node.type != NodeType.MACRO) {
            macroSection.visibility = View.GONE
            if (node.type == NodeType.CHECK_COLOR) {
                // If it's just check color (trigger), no antidetect
                antiDetectSection.visibility = View.GONE
            }
        } else {
            // It's a macro. Hide antidetect
            antiDetectSection.visibility = View.GONE
        }

        // --- SECTION: Логика и Ветвление ---
        val isCondition = node.triggerMode != -1

        val successLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 0) }
        successLayout.addView(TextView(service).apply { text = if (isCondition) "Успех (Идти к):" else "След. шаг:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val nextSuccessEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(node.nextNodeIdOnSuccess?.toString() ?: "")
            hint = if (isCondition) "(по порядку)" else "(-1 = стоп)"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        successLayout.addView(nextSuccessEdit)

        val failLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 0) }
        failLayout.addView(TextView(service).apply { text = "Ошибка (Идти к):"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val nextFailEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(node.nextNodeIdOnFail?.toString() ?: "")
            hint = "(ждать)"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        failLayout.addView(nextFailEdit)

        val cycleLimitLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 5) }
        cycleLimitLayout.addView(TextView(service).apply { text = "Лим. проверок (0=∞):"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val cycleLimitEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.maxCheckCycles?.toString() ?: "")
            hint = "∞"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        cycleLimitLayout.addView(cycleLimitEdit)
        
        val triggerModeLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 10) }
        triggerModeLayout.addView(TextView(service).apply { text = "ТИП ТРИГГЕРА:"; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD) })
        val triggerModeSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.parseColor("#444444"))
            val adapter = createThemedSpinnerAdapter(arrayOf("Отключен (Обычный шаг)", "Цвет пикселя", "Фрагмент экрана", "Текст (OCR)"))
            this.adapter = adapter
            setSelection(if (node.triggerMode > 2) 3 else node.triggerMode + 1)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                    val newMode = pos - 1
                    if (node.triggerMode != newMode) {
                        node.triggerMode = newMode
                        showEditNodeMenu(node)
                    }
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
        }
        triggerModeLayout.apply { addView(triggerModeSpinner) }

        val fragZoneBtn = Button(service).apply {
            text = if (textZoneStartViews.containsKey(node.id)) "СКРЫТЬ ОБЛАСТЬ" else "ЗАДАТЬ ОБЛАСТЬ (ЗОНА)"
            setOnClickListener {
                if (textZoneStartViews.containsKey(node.id)) {
                    removeTextZoneMarkers(node.id)
                    text = "ЗАДАТЬ ОБЛАСТЬ (ЗОНА)"
                } else {
                    createTextZoneMarkers(node)
                    text = "СКРЫТЬ ОБЛАСТЬ"
                }
            }
        }

        val textTargetLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 5) }
        textTargetLayout.addView(TextView(service).apply { text = "Искомый текст:"; setTextColor(Color.WHITE) })
        val textTargetEdit = EditText(service).apply {
            setText(node.targetText ?: "")
            hint = "Введите текст..."
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        textTargetLayout.addView(textTargetEdit)

        val textLangLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        textLangLayout.addView(TextView(service).apply { text = "Язык:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f) })
        val textLangSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.6f)
            setBackgroundColor(Color.parseColor("#444444"))
            val langs = arrayOf("Русский" to "rus", "Английский" to "eng", "Рус + Англ" to "rus+eng")
            this.adapter = createThemedSpinnerAdapter(langs.map { it.first }.toTypedArray())
            setSelection(if (node.targetLanguage == "eng") 1 else if (node.targetLanguage == "rus+eng") 2 else 0)
        }
        textLangLayout.addView(textLangSpinner)

        val ocrFullscreenLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(10), 0, dpToPx(5)) }
        val ocrFullscreenCheck = android.widget.CheckBox(service).apply {
            text = "Нажимать на найденный текст (центр метки сдвинется)"
            setTextColor(Color.parseColor("#FFCC00"))
            isChecked = node.ocrFullScreenClick
            setOnCheckedChangeListener { _, isChecked ->
                node.ocrFullScreenClick = isChecked
            }
        }
        ocrFullscreenLayout.addView(ocrFullscreenCheck)
        
        // Smart OCR Math Options
        val smartOcrLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dpToPx(10), 0, dpToPx(5)) }
        val smartOcrCheck = android.widget.CheckBox(service).apply {
            text = "Умный OCR: Математическое сравнение"
            setTextColor(Color.parseColor("#4CAF50"))
            isChecked = node.isSmartOcr
        }
        smartOcrLayout.addView(smartOcrCheck)
        
        val smartOcrSettings = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; visibility = if (node.isSmartOcr) View.VISIBLE else View.GONE }
        
        val smartOcrOpLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        smartOcrOpLayout.addView(TextView(service).apply { text = "Оператор:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f) })
        val smartOcrOpSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.6f)
            setBackgroundColor(Color.parseColor("#444444"))
            val ops = arrayOf("==", "!=", ">", "<", ">=", "<=")
            this.adapter = createThemedSpinnerAdapter(ops)
            setSelection(ops.indexOf(node.ocrOperator).coerceAtLeast(0))
        }
        smartOcrOpLayout.addView(smartOcrOpSpinner)
        smartOcrSettings.addView(smartOcrOpLayout)
        
        val ocrCompareTargetLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        ocrCompareTargetLayout.addView(TextView(service).apply { text = "Сравнить с:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f) })
        val availableOcrNodes = service.nodes.filter { it.id != node.id && it.triggerMode == 2 }
        val ocrCompareTargetSpinner = Spinner(service).apply {
            val list = mutableListOf("Заданным числом")
            availableOcrNodes.forEach { list.add("Метка ${it.id}") }
            
            val adp = ArrayAdapter(service, android.R.layout.simple_spinner_item, list)
            adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = adp
            
            var selIndex = 0
            if (node.ocrCompareToNodeId != null) {
                val idx = availableOcrNodes.indexOfFirst { it.id == node.ocrCompareToNodeId }
                if (idx != -1) selIndex = idx + 1
            }
            setSelection(selIndex)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.6f)
        }
        ocrCompareTargetLayout.addView(ocrCompareTargetSpinner)
        smartOcrSettings.addView(ocrCompareTargetLayout)

        val smartOcrValLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        smartOcrValLayout.addView(TextView(service).apply { text = "Значение:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f) })
        val smartOcrValEdit = EditText(service).apply {
            setText(node.ocrTargetValue.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.6f)
        }
        smartOcrValLayout.addView(smartOcrValEdit)
        smartOcrSettings.addView(smartOcrValLayout)
        
        ocrCompareTargetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                smartOcrValLayout.visibility = if (position == 0) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        val smartOcrSufLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        smartOcrSufLayout.addView(TextView(service).apply { text = "Суффиксы (k:1000, m:1000000):"; setTextColor(Color.GRAY) })
        val smartOcrSufEdit = EditText(service).apply {
            setText(node.ocrCustomSuffixes)
            setTextColor(Color.WHITE)
        }
        smartOcrSufLayout.addView(smartOcrSufEdit)
        smartOcrSettings.addView(smartOcrSufLayout)
        
        smartOcrCheck.setOnCheckedChangeListener { _, isChecked ->
            node.isSmartOcr = isChecked
            smartOcrSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        smartOcrLayout.addView(smartOcrSettings)

        val colorBehaviorLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 15) }
        colorBehaviorLayout.addView(TextView(service).apply { text = "ПОВЕДЕНИЕ (ДЛЯ ЦВЕТА):"; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD) })
        val colorBehaviorSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.parseColor("#444444"))
            val arr = arrayOf("Статичный (Заданный вручную)", "Динамичный (Обновить если изменился)", "Сравнить с другой меткой", "Сравнить с другой точкой")
            val adapter = createThemedSpinnerAdapter(arr)
            this.adapter = adapter
            val sel = if (node.dynamicColorUpdate) 1 else if (node.compareToNodeId != null) 2 else if (node.colorCompareX != null) 3 else 0
            setSelection(sel)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                    val wasDyn = node.dynamicColorUpdate
                    val wasComp = node.compareToNodeId != null
                    val wasPoint = node.colorCompareX != null
                    
                    if (pos == 0) { node.dynamicColorUpdate = false; node.compareToNodeId = null; node.colorCompareX = null; node.colorCompareY = null; removeColorCompareMarker(node.id) }
                    if (pos == 1) { node.dynamicColorUpdate = true; node.compareToNodeId = null; node.colorCompareX = null; node.colorCompareY = null; removeColorCompareMarker(node.id) }
                    if (pos == 2) { node.dynamicColorUpdate = false; node.colorCompareX = null; node.colorCompareY = null; removeColorCompareMarker(node.id); if (node.compareToNodeId == null) node.compareToNodeId = 0 }
                    if (pos == 3) { node.dynamicColorUpdate = false; node.compareToNodeId = null; createColorCompareMarker(node) }
                    
                    val isDyn = node.dynamicColorUpdate
                    val isComp = node.compareToNodeId != null
                    val isPoint = node.colorCompareX != null
                    
                    if (wasDyn != isDyn || wasComp != isComp || wasPoint != isPoint) {
                        showEditNodeMenu(node)
                    }
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
        }
        colorBehaviorLayout.apply { addView(colorBehaviorSpinner) }

        val colorPreview = TextView(service).apply {
            val hasColor = node.targetColor != null
            text = if (hasColor) String.format("#%06X", 0xFFFFFF and node.targetColor!!) + " (Сбросить - долгое наж.)" else "Условие цвета (Нет)"
            setBackgroundColor(node.targetColor ?: Color.TRANSPARENT)
            setTextColor(if (hasColor && Color.luminance(node.targetColor!!) > 0.5) Color.BLACK else Color.WHITE)
            setPadding(20,20,20,20)
            gravity = Gravity.CENTER
        }

        val capBtn = Button(service).apply {
            text = "ЗАХВАТ ЦВЕТА ПОД МЕТКОЙ"
            setOnClickListener {
                updateNodeScreenPosition(node)
                modMenu?.visibility = View.INVISIBLE
                linesOverlayView?.visibility = View.INVISIBLE
                textZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                textZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                colorCompareViews.values.forEach { it.visibility = View.INVISIBLE }
                nodeViews.values.forEach { it.visibility = View.INVISIBLE }

                service.handler.postDelayed({
                    service.captureColorAt(node.x, node.y) { color ->
                        modMenu?.visibility = View.VISIBLE
                        linesOverlayView?.visibility = View.VISIBLE
                        textZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        textZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        colorCompareViews.values.forEach { it.visibility = View.VISIBLE }
                        nodeViews.values.forEach { it.visibility = View.VISIBLE }

                        if (color != null) {
                            node.targetColor = color
                            colorPreview.setBackgroundColor(color)
                            colorPreview.text = String.format("#%06X", 0xFFFFFF and color)
                            colorPreview.setTextColor(if (Color.luminance(color) > 0.5) Color.BLACK else Color.WHITE)
                            Toast.makeText(service, "Цвет изменен!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, 300)
            }
            setOnLongClickListener {
                node.targetColor = null
                colorPreview.setBackgroundColor(Color.TRANSPARENT)
                colorPreview.text = "Условие цвета (Нет)"
                colorPreview.setTextColor(Color.WHITE)
                Toast.makeText(service, "Цвет сброшен!", Toast.LENGTH_SHORT).show()
                true
            }
        }

        val colorOpLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        colorOpLayout.addView(TextView(service).apply { text = "Условие:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f) })
        val colorOpSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.6f)
            setBackgroundColor(Color.parseColor("#444444"))
            val adapter = createThemedSpinnerAdapter(arrayOf("СОВПАДАЕТ (==)", "НЕ СОВПАДАЕТ (!=)"))
            this.adapter = adapter
            setSelection(if (node.colorOperator == "==") 0 else 1)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                    node.colorOperator = if (pos == 0) "==" else "!="
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
        }
        colorOpLayout.addView(colorOpSpinner)

        val colorToleranceLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        colorToleranceLayout.addView(TextView(service).apply { text = "Допуск (0-255):"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val colorTolEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.colorTolerance.toString())
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        colorToleranceLayout.addView(colorTolEdit)

        val linkedLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        linkedLayout.addView(TextView(service).apply { text = "Линковка к Метке №:"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val linkedEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (node.compareToNodeId != null) node.compareToNodeId.toString() else node.linkedConditionNodeId?.toString() ?: "")
            hint = "(Нет)"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        linkedLayout.addView(linkedEdit)

        val linkedOpBtn = Button(service).apply {
            text = if (node.linkedConditionOperator == "AND") "ОБЪЕДИНЕНИЕ: И (AND)" else "ОБЪЕДИНЕНИЕ: ИЛИ (OR)"
            setOnClickListener {
                node.linkedConditionOperator = if (node.linkedConditionOperator == "AND") "OR" else "AND"
                text = if (node.linkedConditionOperator == "AND") "ОБЪЕДИНЕНИЕ: И (AND)" else "ОБЪЕДИНЕНИЕ: ИЛИ (OR)"
            }
        }

        val imgThresholdLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        imgThresholdLayout.addView(TextView(service).apply { text = "Совпадение (%):"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val imgThresholdEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(node.imageThreshold.toString())
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        imgThresholdLayout.addView(imgThresholdEdit)

        val searchRadiusLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        searchRadiusLayout.addView(TextView(service).apply { text = "Зона поиска (px):"; setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val searchRadiusEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.searchRadius.toString())
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        searchRadiusLayout.addView(searchRadiusEdit)

        val resolutionScaleLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        val resolutionScaleText = TextView(service).apply { 
            text = "Качество (${(node.checkResolutionScale * 100).toInt()}%):"
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(130), WindowManager.LayoutParams.WRAP_CONTENT) 
        }
        val resolutionScaleBar = android.widget.SeekBar(service).apply {
            max = 90
            progress = ((node.checkResolutionScale - 0.1f) * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        resolutionScaleLayout.addView(resolutionScaleText)
        resolutionScaleLayout.addView(resolutionScaleBar)

        val imgPreview = android.widget.ImageView(service).apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(150)).apply { 
                gravity = Gravity.CENTER_HORIZONTAL 
                setMargins(0, dpToPx(10), 0, dpToPx(10))
            }
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }

        val updatePreviewImage = {
            if (node.targetImageBase64 != null) {
                try {
                    val bytes = android.util.Base64.decode(node.targetImageBase64, android.util.Base64.DEFAULT)
                    var bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    
                    val currentScale = (resolutionScaleBar.progress + 10) / 100f
                    if (node.triggerMode == 2) {
                        bitmap = service.enhanceBitmapForOcr(bitmap)
                    } else if (node.triggerMode == 1 && currentScale < 1.0f) {
                        val checkStep = (1f / currentScale).toInt().coerceAtLeast(1)
                        if (checkStep > 1) {
                            val sw = maxOf(1, bitmap.width / checkStep)
                            val sh = maxOf(1, bitmap.height / checkStep)
                            bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, sw, sh, false)
                        }
                    }
                    imgPreview.setImageBitmap(bitmap)
                    imgPreview.setBackgroundColor(Color.TRANSPARENT)
                } catch(e: Exception) {}
            } else {
                imgPreview.setBackgroundColor(Color.parseColor("#555555"))
            }
        }
        
        resolutionScaleBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = (progress + 10) / 100f
                resolutionScaleText.text = "Качество (${(scale * 100).toInt()}%):"
                updatePreviewImage()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        updatePreviewImage()

        
        val capImgBtn = Button(service).apply {
            text = if (node.triggerMode == 1) "ЗАХВАТИТЬ ФРАГМЕНТ ИЗОБРАЖЕНИЯ\n(Удерживать для сброса)" else "ТЕСТ РАСПОЗНАВАНИЯ ТЕКСТА"
            setOnClickListener {
                if (node.triggerMode == 2) {
                    node.targetText = textTargetEdit.text.toString().takeIf { it.isNotEmpty() }
                    node.targetLanguage = if (textLangSpinner.selectedItemPosition == 1) "eng" else if (textLangSpinner.selectedItemPosition == 2) "rus+eng" else "rus"
                }
                modMenu?.visibility = View.INVISIBLE
                linesOverlayView?.visibility = View.INVISIBLE
                textZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                textZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                colorCompareViews.values.forEach { it.visibility = View.INVISIBLE }
                nodeViews.values.forEach { it.visibility = View.INVISIBLE }
                
                service.handler.postDelayed({
                    service.captureImageFragment(node) { base64 ->
                        modMenu?.visibility = View.VISIBLE
                        linesOverlayView?.visibility = View.VISIBLE
                        textZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        textZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        colorCompareViews.values.forEach { it.visibility = View.VISIBLE }
                        nodeViews.values.forEach { it.visibility = View.VISIBLE }
                        
                        if (base64 != null) {
                            try {
                                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                var bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                updatePreviewImage()
                                node.targetImageBase64 = base64
                                node.cachedTargetBitmap = null
                                if (node.triggerMode == 1) {
                                    Toast.makeText(service, "Фрагмент захвачен!", Toast.LENGTH_SHORT).show()
                                } else {
                                    service.testTextRecognition(node, bitmap)
                                }
                            } catch(e: Exception) {}
                        }
                    }
                }, 300)
            }
            setOnLongClickListener {
                node.targetImageBase64 = null
                node.cachedTargetBitmap = null
                imgPreview.setImageBitmap(null)
                imgPreview.setBackgroundColor(Color.parseColor("#555555"))
                if (node.triggerMode == 1) {
                    Toast.makeText(service, "Фрагмент сброшен!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(service, "Превью сброшено!", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        val hasLogicChanges = node.targetColor != null || node.targetImageBase64 != null || node.targetText != null || node.colorOperator != "==" || node.colorTolerance != 15 || node.linkedConditionNodeId != null || node.compareToNodeId != null || node.dynamicColorUpdate || node.triggerMode >= 0
        val logicSection = addSection("👁 Настройки Поиска (Условие)", hasLogicChanges) { body ->
            body.addView(triggerModeLayout)
            
            if (node.triggerMode == 0) { // Color Pixel
                body.addView(colorBehaviorLayout)
                body.addView(colorOpLayout)
                body.addView(colorToleranceLayout)
                
                if (!node.dynamicColorUpdate && node.compareToNodeId == null && node.colorCompareX == null) {
                    body.addView(colorPreview)
                    body.addView(capBtn)
                }
                
                if (node.colorCompareX == null) {
                    body.addView(linkedLayout)
                }
                
                if (node.compareToNodeId == null && node.colorCompareX == null) {
                    body.addView(linkedOpBtn)
                }
            } else if (node.triggerMode == 1) { // Image Fragment
                body.addView(colorOpLayout)
                body.addView(fragZoneBtn)
                body.addView(imgThresholdLayout)
                body.addView(searchRadiusLayout)
                body.addView(resolutionScaleLayout)
                body.addView(imgPreview)
                body.addView(capImgBtn)
            } else if (node.triggerMode == 2) { // Text Recognition
                body.addView(colorOpLayout)
                body.addView(textLangLayout)
                body.addView(textTargetLayout)
                body.addView(ocrFullscreenLayout)
                body.addView(smartOcrLayout)
                body.addView(fragZoneBtn)
                body.addView(imgPreview)
                body.addView(capImgBtn)
            }
        }

        val hasRoutingChanges = node.nextNodeIdOnSuccess != null || node.nextNodeIdOnFail != null || node.maxCheckCycles != null
        val routingSection = addSection("🛣 Маршрутизация (Куда идти дальше)", hasRoutingChanges) { body ->
            body.addView(successLayout)
            if (node.triggerMode != -1) {
                body.addView(failLayout)
                body.addView(cycleLimitLayout)
            }
        }

        if (appMode != AppMode.ADVANCED) {
            successLayout.visibility = View.GONE
            failLayout.visibility = View.GONE
            cycleLimitLayout.visibility = View.GONE
        }
        
        if (false) {
            colorPreview.visibility = View.GONE
            capBtn.visibility = View.GONE
            capImgBtn.visibility = View.GONE
            imgPreview.visibility = View.GONE
            imgThresholdLayout.visibility = View.GONE
            searchRadiusLayout.visibility = View.GONE
        }
        if (appMode == AppMode.SEQUENTIAL && node.type == NodeType.CLICK) {
            colorPreview.visibility = View.GONE
            capBtn.visibility = View.GONE
            capImgBtn.visibility = View.GONE
            imgPreview.visibility = View.GONE
            imgThresholdLayout.visibility = View.GONE
            searchRadiusLayout.visibility = View.GONE
        }

        // Hide entire logic section if nothing is visible inside
        if (appMode == AppMode.SEQUENTIAL && node.type == NodeType.CLICK) {
            logicSection.visibility = View.GONE
            routingSection.visibility = View.GONE
        }

        // --- SECTION: Синхронизация и Свайпы ---
        val syncLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 5) }
        syncLayout.addView(TextView(service).apply { text = "Одноврем. с Метками (через запятую): "; setTextColor(Color.WHITE) })
        val syncEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(node.syncWithNodeIds)
            hint = "(Нет)"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        syncLayout.addView(syncEdit)

        val swipeLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 10); gravity = Gravity.CENTER_VERTICAL }
        val swipeBtn = Button(service).apply {
            text = if (node.isSwipe) "Свайп: ВКЛ" else "Свайп: ВЫКЛ"
            setOnClickListener {
                node.isSwipe = !node.isSwipe
                text = if (node.isSwipe) "Свайп: ВКЛ" else "Свайп: ВЫКЛ"
                if (node.isSwipe) {
                    createSwipeEndMarker(node)
                } else {
                    removeSwipeEndMarker(node.id)
                }
                invalidateLines()
            }
        }
        swipeLayout.addView(swipeBtn)
        
        val (swipeDurRow, getSwipeDurRowMs) = createTimeInputRow("Задержка:", node.swipeDurationMs)
        
        val swipeDeltaLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 10, 0, 0) }
        val modeSpinnerLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        modeSpinnerLayout.addView(TextView(service).apply { text = "Режим свайпа: "; setTextColor(Color.WHITE) })
        val modeSpinner = android.widget.Spinner(service).apply {
            adapter = android.widget.ArrayAdapter(service, android.R.layout.simple_spinner_item, arrayOf("Вектор (Суб-метка)", "К метке (ID)", "Ломаная линия (Жест)"))
            setSelection(if (node.swipePathPoints.isNotEmpty()) 2 else if (node.swipeTargetNodeId != null) 1 else 0)
        }
        modeSpinnerLayout.addView(modeSpinner)
        swipeDeltaLayout.addView(modeSpinnerLayout)

        val swipeTargetEditLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 5, 0, 0); gravity = Gravity.CENTER_VERTICAL }
        swipeTargetEditLayout.addView(TextView(service).apply { text = "ID цели: "; setTextColor(Color.WHITE) })
        val swipeTargetEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.swipeTargetNodeId?.toString() ?: "")
            hint = "(Введите ID)"
            setHintTextColor(Color.parseColor("#AAAAAA"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(dpToPx(100), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        swipeTargetEditLayout.addView(swipeTargetEdit)
        swipeDeltaLayout.addView(swipeTargetEditLayout)
        
        modeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                swipeTargetEditLayout.visibility = if (pos == 1) View.VISIBLE else View.GONE
                
                if (pos == 0) {
                    // Vector
                    node.swipeTargetNodeId = null
                    node.swipePathPoints = emptyList()
                    if (node.isSwipe) createSwipeEndMarker(node)
                } else if (pos == 1) {
                    // To ID
                    removeSwipeEndMarker(node.id)
                    node.swipePathPoints = emptyList()
                } else if (pos == 2) {
                    // Gesture (don't clear points if we just switched to view it)
                    removeSwipeEndMarker(node.id)
                    node.swipeTargetNodeId = null
                }
                invalidateLines()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        
        val hasSwipeChanges = node.syncWithNodeIds.isNotEmpty() || node.isSwipe || node.swipeTargetNodeId != null || node.swipeDurationMs != 500L
        val syncSwipeSection = addSection("⚙️ Доп. настройки (Свайп / Синхронизация)", hasSwipeChanges) { body ->
            body.addView(syncLayout)
            body.addView(swipeLayout)
            body.addView(swipeDurRow)
            body.addView(swipeDeltaLayout)
        }

        if (false) {
            syncSwipeSection.visibility = View.GONE
        }
            if (node.type == NodeType.CHECK_COLOR || node.type == NodeType.MACRO || node.type == NodeType.MANAGER) {
                swipeLayout.visibility = View.GONE
                swipeDurRow.visibility = View.GONE
                swipeDeltaLayout.visibility = View.GONE
                clickDurRow.visibility = View.GONE
            }
            if (node.type == NodeType.MANAGER) {
                timingsSection.visibility = View.GONE
                logicSection.visibility = View.GONE
                routingSection.visibility = View.GONE
                macroSection.visibility = View.GONE
                antiDetectSection.visibility = View.GONE
                syncSwipeSection.visibility = View.GONE
            }

        // --- SAVE BUTTON ---
        val saveBtn = Button(service).apply {
            text = " СОХРАНИТЬ И НАЗАД "
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                node.clickDurationMs = getClickDurRowMs()
                node.delayAfterMs = getDelayRowMs()
                node.randomizeDelayMs = getRandomDelayRowMs()
                node.randomizeRadius = randomRadiusEdit.text.toString().toIntOrNull() ?: 0
                node.repetitions = repetitionsEdit.text.toString().toIntOrNull() ?: 1
                node.nextNodeIdOnSuccess = nextSuccessEdit.text.toString().toIntOrNull()
                node.nextNodeIdOnFail = nextFailEdit.text.toString().toIntOrNull()
                val limit = cycleLimitEdit.text.toString().toIntOrNull()
                node.maxCheckCycles = if (limit != null && limit > 0) limit else null
                
                if (node.triggerMode == 0 && node.compareToNodeId != null) {
                    node.compareToNodeId = linkedEdit.text.toString().toIntOrNull()
                    node.linkedConditionNodeId = null
                } else {
                    node.linkedConditionNodeId = linkedEdit.text.toString().toIntOrNull()
                    node.compareToNodeId = null
                }
                
                node.colorTolerance = colorTolEdit.text.toString().toIntOrNull() ?: 15
                node.imageThreshold = imgThresholdEdit.text.toString().toFloatOrNull() ?: 80f
                node.searchRadius = searchRadiusEdit.text.toString().toIntOrNull() ?: 0
                node.checkResolutionScale = (resolutionScaleBar.progress + 10) / 100f
                node.targetText = textTargetEdit.text.toString().takeIf { it.isNotEmpty() }
                node.targetLanguage = if (textLangSpinner.selectedItemPosition == 1) "eng" else if (textLangSpinner.selectedItemPosition == 2) "rus+eng" else "rus"
                node.ocrFullScreenClick = ocrFullscreenCheck.isChecked
                node.ocrOperator = arrayOf("==", "!=", ">", "<", ">=", "<=")[smartOcrOpSpinner.selectedItemPosition]
                if (ocrCompareTargetSpinner.selectedItemPosition > 0) {
                    node.ocrCompareToNodeId = availableOcrNodes[ocrCompareTargetSpinner.selectedItemPosition - 1].id
                } else {
                    node.ocrCompareToNodeId = null
                }
                node.ocrTargetValue = smartOcrValEdit.text.toString().toDoubleOrNull() ?: 0.0
                node.ocrCustomSuffixes = smartOcrSufEdit.text.toString()
                node.syncWithNodeIds = syncEdit.text.toString().filter { it.isDigit() || it == ',' }
                node.swipeDurationMs = getSwipeDurRowMs()
                node.swipeTargetNodeId = swipeTargetEdit.text.toString().toIntOrNull()
                showMainMenu()
            }
        }
        
        val margins = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        margins.setMargins(0, 20, 0, 0)
        saveBtn.layoutParams = margins
        content.addView(saveBtn)

        layout.addView(content)
        menuContentContainer.addView(layout)
    }

    fun removeColorCompareMarker(nodeId: Int) {
        colorCompareViews[nodeId]?.let { windowManager.removeView(it) }
        colorCompareViews.remove(nodeId)
        colorCompareParams.remove(nodeId)
    }

    fun createColorCompareMarker(node: TargetNode) {
        if (colorCompareViews.containsKey(node.id)) return

        if (node.colorCompareX == null || node.colorCompareY == null) {
            node.colorCompareX = node.x + dpToPx(30)
            node.colorCompareY = node.y + dpToPx(30)
        }

        val container = SubMarkerView(service, node, 2)

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (service.isPlaying || service.isRecording) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            dpToPx(40), dpToPx(40),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.colorCompareX!! - dpToPx(20)
            y = node.colorCompareY!! - dpToPx(20)
        }

        container.visibility = if (node.isVisible) View.VISIBLE else View.GONE
        setupColorCompareTouchListener(container, params, node)

        windowManager.addView(container, params)
        colorCompareViews[node.id] = container
        colorCompareParams[node.id] = params
    }

    fun createTextZoneMarkers(node: TargetNode) {
        if (textZoneStartViews.containsKey(node.id)) return

        if (node.textZoneStartX == 0 && node.textZoneStartY == 0 && node.textZoneEndX == 0 && node.textZoneEndY == 0) {
            node.textZoneStartX = node.x - dpToPx(50)
            node.textZoneStartY = node.y - dpToPx(50)
            node.textZoneEndX = node.x + dpToPx(50)
            node.textZoneEndY = node.y + dpToPx(50)
        }

        // --- Start Marker ---
        val startView = FrameLayout(service)
        val startCircle = android.view.View(service).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.GREEN)
                setSize(dpToPx(30), dpToPx(30))
            }
            alpha = 0.8f
        }
        val startNumber = TextView(service).apply {
            text = "⌜"
            setTextColor(Color.WHITE)
            textSize = 24f * uiScale
            gravity = Gravity.CENTER
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        startView.addView(startCircle, FrameLayout.LayoutParams(dpToPx(30), dpToPx(30)))
        startView.addView(startNumber, FrameLayout.LayoutParams(dpToPx(30), dpToPx(30)))

        val startParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.textZoneStartX - dpToPx(15)
            y = node.textZoneStartY - dpToPx(15)
        }

        startView.visibility = if (node.isVisible) View.VISIBLE else View.GONE
        setupTextZoneTouchListener(startView, startParams, node, true)
        windowManager.addView(startView, startParams)
        textZoneStartViews[node.id] = startView
        textZoneStartParams[node.id] = startParams

        // --- End Marker ---
        val endView = FrameLayout(service)
        val endCircle = android.view.View(service).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.RED)
                setSize(dpToPx(30), dpToPx(30))
            }
            alpha = 0.8f
        }
        val endNumber = TextView(service).apply {
            text = "⌟"
            setTextColor(Color.WHITE)
            textSize = 24f * uiScale
            gravity = Gravity.CENTER
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        endView.addView(endCircle, FrameLayout.LayoutParams(dpToPx(30), dpToPx(30)))
        endView.addView(endNumber, FrameLayout.LayoutParams(dpToPx(30), dpToPx(30)))

        val endParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.textZoneEndX - dpToPx(15)
            y = node.textZoneEndY - dpToPx(15)
        }

        endView.visibility = if (node.isVisible) View.VISIBLE else View.GONE
        setupTextZoneTouchListener(endView, endParams, node, false)
        windowManager.addView(endView, endParams)
        textZoneEndViews[node.id] = endView
        textZoneEndParams[node.id] = endParams
    }

    fun removeTextZoneMarkers(id: Int) {
        val startV = textZoneStartViews.remove(id)
        if (startV != null) windowManager.removeView(startV)
        textZoneStartParams.remove(id)

        val endV = textZoneEndViews.remove(id)
        if (endV != null) windowManager.removeView(endV)
        textZoneEndParams.remove(id)
    }

    fun setupTextZoneTouchListener(view: View, params: WindowManager.LayoutParams, node: TargetNode, isStart: Boolean) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            if (service.isPlaying) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(view, params)
                    if (isStart) {
                        node.textZoneStartX = params.x + dpToPx(15)
                        node.textZoneStartY = params.y + dpToPx(15)
                    } else {
                        node.textZoneEndX = params.x + dpToPx(15)
                        node.textZoneEndY = params.y + dpToPx(15)
                    }
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isStart) {
                        node.textZoneStartX = params.x + dpToPx(15)
                        node.textZoneStartY = params.y + dpToPx(15)
                    } else {
                        node.textZoneEndX = params.x + dpToPx(15)
                        node.textZoneEndY = params.y + dpToPx(15)
                    }
                    invalidateLines()
                    service.autoSave()
                    true
                }
                else -> false
            }
        }
    }

    fun createSwipeEndMarker(node: TargetNode) {
        if (swipeEndViews.containsKey(node.id)) return
        
        if (node.swipeEndX == 0 && node.swipeEndY == 0) {
            node.swipeEndX = node.x + dpToPx(100)
            node.swipeEndY = node.y + dpToPx(100)
        }
        
        val swipeEndView = SubMarkerView(service, node, 1)
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (service.isPlaying || service.isRecording) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            dpToPx(60), dpToPx(60),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.swipeEndX - dpToPx(30)
            y = node.swipeEndY - dpToPx(30)
        }
        
        swipeEndView.visibility = if (node.isVisible) View.VISIBLE else View.GONE
        setupSwipeEndTouchListener(swipeEndView, params, node)
        
        windowManager.addView(swipeEndView, params)
        swipeEndViews[node.id] = swipeEndView
        swipeEndParams[node.id] = params
    }

    fun removeSwipeEndMarker(id: Int) {
        val view = swipeEndViews.remove(id)
        if (view != null) windowManager.removeView(view)
        swipeEndParams.remove(id)
    }

    fun setupColorCompareTouchListener(view: View, params: WindowManager.LayoutParams, node: TargetNode) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            if (service.isPlaying || service.isRecording) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(view, params)
                    val newEx = params.x + dpToPx(20)
                    val newEy = params.y + dpToPx(20)
                    node.colorCompareX = newEx
                    node.colorCompareY = newEy
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val newEx = params.x + dpToPx(20)
                    val newEy = params.y + dpToPx(20)
                    node.colorCompareX = newEx
                    node.colorCompareY = newEy
                    invalidateLines()
                    service.autoSave()
                    true
                }
                else -> false
            }
        }
    }

    fun setupSwipeEndTouchListener(view: View, params: WindowManager.LayoutParams, node: TargetNode) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            if (service.isPlaying || service.isRecording) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(view, params)
                    val newEx = params.x + dpToPx(30)
                    val newEy = params.y + dpToPx(30)
                    if (newEx != node.swipeEndX || newEy != node.swipeEndY) {
                        node.swipePathPoints = emptyList() // clear since user manually altered end position
                        node.swipeEndX = newEx
                        node.swipeEndY = newEy
                    }
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val newEx = params.x + dpToPx(30)
                    val newEy = params.y + dpToPx(30)
                    if (newEx != node.swipeEndX || newEy != node.swipeEndY) {
                        node.swipePathPoints = emptyList()
                        node.swipeEndX = newEx
                        node.swipeEndY = newEy
                    }
                    invalidateLines()
                    service.autoSave()
                    true
                }
                else -> false
            }
        }
    }

    fun setupNodeTouchListener(view: View, params: WindowManager.LayoutParams, node: TargetNode?) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            if (service.isPlaying || service.isRecording) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(view, params)
                    if (node != null) {
                        val nx = params.x + dpToPx(30)
                        val ny = params.y + dpToPx(30)
                        val dx = nx - node.x
                        val dy = ny - node.y
                        if (dx != 0 || dy != 0) {
                            node.x = nx
                            node.y = ny
                            if (node.swipePathPoints.isNotEmpty()) {
                                node.swipePathPoints = node.swipePathPoints.map { Pair(it.first + dx, it.second + dy) }
                            }
                        }
                    }
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (node != null) {
                        if (Math.abs(event.rawX - initialTouchX) >= 10 || Math.abs(event.rawY - initialTouchY) >= 10) {
                            service.autoSave()
                        }
                    }
                    if (Math.abs(event.rawX - initialTouchX) < 10 && Math.abs(event.rawY - initialTouchY) < 10) {
                        view.performClick()
                    }
                    invalidateLines()
                    true
                }
                else -> false
            }
        }
    }

    fun invalidateLines() {
        if (linesOverlayView == null) {
            linesOverlayView = object : View(service) {
                private val linePaint = Paint().apply {
                    color = Color.parseColor("#8800FFFF")
                    strokeWidth = dpToPx(4).toFloat()
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                private val arrowPaint = Paint().apply {
                    color = Color.parseColor("#8800FFFF")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                private val pathLocCache = IntArray(2)
                private val cachePath = android.graphics.Path()
                
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    
                    getLocationOnScreen(pathLocCache)
                    screenOffsetX = pathLocCache[0]
                    screenOffsetY = pathLocCache[1]
                    canvas.translate(-screenOffsetX.toFloat(), -screenOffsetY.toFloat())
                    
                    val density = resources.displayMetrics.density
                    
                    for (node in service.nodes) {
                        if (!node.isVisible) continue
                        
                        if (node.triggerMode == 0 && !node.dynamicColorUpdate && node.compareToNodeId == null && node.colorCompareX != null && node.colorCompareY != null) {
                            val params = nodeParams[node.id]
                            if (params != null) {
                                val startX = params.x + dpToPx(30).toFloat()
                                val startY = params.y + dpToPx(30).toFloat()
                                val endX = node.colorCompareX!!.toFloat()
                                val endY = node.colorCompareY!!.toFloat()
                                canvas.drawLine(startX, startY, endX, endY, Paint().apply {
                                    color = Color.parseColor("#88FF00FF") // Magenta with alpha
                                    strokeWidth = dpToPx(2).toFloat()
                                    style = Paint.Style.STROKE
                                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
                                })
                            }
                        }

                        // Swipe line
                        if (node.isSwipe) {
                            val params = nodeParams[node.id]
                            if (params != null) {
                                val startX = params.x + dpToPx(30).toFloat()
                                val startY = params.y + dpToPx(30).toFloat()
                                
                                var finalEndX = node.swipeEndX.toFloat()
                                var finalEndY = node.swipeEndY.toFloat()
                                
                                if (node.swipeTargetNodeId != null) {
                                    val tNode = service.nodes.find { it.id == node.swipeTargetNodeId }
                                    if (tNode != null) {
                                        val tParams = nodeParams[tNode.id]
                                        if (tParams != null) {
                                            finalEndX = tParams.x + dpToPx(30).toFloat()
                                            finalEndY = tParams.y + dpToPx(30).toFloat()
                                        }
                                    }
                                }
                                
                                val endX = finalEndX
                                val endY = finalEndY
                                
                                var pDX = endX.toFloat() - startX.toFloat()
                                var pDY = endY.toFloat() - startY.toFloat()
                                
                                if (node.swipePathPoints.isNotEmpty()) {
                                    cachePath.reset()
                                    cachePath.moveTo(node.swipePathPoints[0].first, node.swipePathPoints[0].second)
                                    for (i in 1 until node.swipePathPoints.size) {
                                        cachePath.lineTo(node.swipePathPoints[i].first, node.swipePathPoints[i].second)
                                    }
                                    canvas.drawPath(cachePath, linePaint)
                                    
                                    val n = node.swipePathPoints.size
                                    if (n >= 2) {
                                        pDX = node.swipePathPoints[n - 1].first - node.swipePathPoints[n - 2].first
                                        pDY = node.swipePathPoints[n - 1].second - node.swipePathPoints[n - 2].second
                                    }
                                } else {
                                    canvas.drawLine(startX, startY, endX.toFloat(), endY.toFloat(), linePaint)
                                }
                                
                                // Draw arrowhead
                                val angle = Math.atan2(pDY.toDouble(), pDX.toDouble())
                                val arrowLen = dpToPx(15).toFloat()
                                
                                val p1x = endX - arrowLen * Math.cos(angle - Math.PI / 6)
                                val p1y = endY - arrowLen * Math.sin(angle - Math.PI / 6)
                                val p2x = endX - arrowLen * Math.cos(angle + Math.PI / 6)
                                val p2y = endY - arrowLen * Math.sin(angle + Math.PI / 6)
                                
                                val path = android.graphics.Path().apply {
                                    moveTo(endX, endY)
                                    lineTo(p1x.toFloat(), p1y.toFloat())
                                    lineTo(p2x.toFloat(), p2y.toFloat())
                                    close()
                                }
                                canvas.drawPath(path, arrowPaint)
                            }
                        }
                        
                        // Text/Image fragment zone rectangle
                        if ((node.triggerMode == 1 || node.triggerMode == 2) && textZoneStartViews.containsKey(node.id)) {
                            val zonePaint = Paint().apply {
                                color = Color.parseColor("#8800FF00") // Green semitransparent
                                style = Paint.Style.STROKE
                                strokeWidth = dpToPx(3).toFloat()
                                isAntiAlias = true
                            }
                            val left = Math.min(node.textZoneStartX, node.textZoneEndX).toFloat()
                            val top = Math.min(node.textZoneStartY, node.textZoneEndY).toFloat()
                            val right = Math.max(node.textZoneStartX, node.textZoneEndX).toFloat()
                            val bottom = Math.max(node.textZoneStartY, node.textZoneEndY).toFloat()
                            canvas.drawRect(left, top, right, bottom, zonePaint)
                        }
                        
                        // "Target ID" (leads to) logic lines
                        val targetIds = listOfNotNull(node.nextNodeIdOnSuccess, node.nextNodeIdOnFail, node.linkedConditionNodeId)
                        for (targetId in targetIds.filter { it >= 0 }) {
                            val targetNode = service.nodes.find { it.id == targetId }
                            if (targetNode != null) {
                                val paramsStart = nodeParams[node.id]
                                val paramsEnd = nodeParams[targetNode.id]
                                if (paramsStart != null && paramsEnd != null) {
                                    val startX = paramsStart.x + dpToPx(30).toFloat()
                                    val startY = paramsStart.y + dpToPx(30).toFloat()
                                    val endX = paramsEnd.x + dpToPx(30).toFloat()
                                    val endY = paramsEnd.y + dpToPx(30).toFloat()
                                    
                                    val oldColor = linePaint.color
                                    linePaint.color = Color.parseColor("#88FF00FF") // Magenta for logic links
                                    val dashedPaint = Paint(linePaint).apply {
                                        pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 15f), 0f)
                                    }
                                    canvas.drawLine(startX, startY, endX, endY, dashedPaint)
                                    linePaint.color = oldColor
                                }
                            }
                        }
                    }
                }
            }
            
            val displayMetrics = android.util.DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            val params = WindowManager.LayoutParams(
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }
            windowManager.addView(linesOverlayView, params)
        }
        linesOverlayView?.visibility = if (service.showLines) View.VISIBLE else View.INVISIBLE; linesOverlayView?.invalidate()
        
        // Ensure floating bar stays on top of lines
        bringControlBarToFront()
    }

    fun addNodeAndReturn(type: NodeType, startX: Int? = null, startY: Int? = null): TargetNode {
        addNode(type, startX, startY)
        return service.nodes.last()
    }

    fun addNode(type: NodeType, startX: Int? = null, startY: Int? = null) {
        if (false) {
            android.widget.Toast.makeText(service, "В Одиночном режиме доступна только одна метка!", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val id = nodeCounter++
        val node = TargetNode(id = id, type = type).apply {
            crosshairColor = if (type == NodeType.CLICK) Color.RED else if (type == NodeType.MANAGER) Color.parseColor("#9C27B0") else Color.BLUE
            numberColor = if (type == NodeType.CLICK) Color.WHITE else Color.YELLOW
        }
        service.nodes.add(node)
        service.autoSave()
        
        val crosshair = CrosshairView(service, node)
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (service.isPlaying || service.isRecording) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        val params = WindowManager.LayoutParams(
            dpToPx(60), dpToPx(60),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (startX != null) startX - dpToPx(30) else (windowManager.defaultDisplay.width / 2 - dpToPx(30))
            y = if (startY != null) startY - dpToPx(30) else (windowManager.defaultDisplay.height / 2 - dpToPx(30))
        }
        node.x = params.x + dpToPx(30)
        node.y = params.y + dpToPx(30)

        crosshair.setOnClickListener { 
            if (!service.isPlaying && !service.isRecording) {
                showEditNodeMenu(node) 
            }
        }
        setupNodeTouchListener(crosshair, params, node)

        windowManager.addView(crosshair, params)
        nodeViews[id] = crosshair
        nodeParams[id] = params
        
        updateMenu()
    }

    private fun removeNode(id: Int) {
        val view = nodeViews.remove(id)
        if (view != null) windowManager.removeView(view)
        nodeParams.remove(id)
        removeSwipeEndMarker(id)
        removeTextZoneMarkers(id)
        removeColorCompareMarker(id)
        service.nodes.removeAll { it.id == id }
        updateMenu()
        invalidateLines()
    }

    fun updateNodeScreenPosition(node: TargetNode) {
        val view = nodeViews[node.id]
        val params = nodeParams[node.id]
        if (view != null && params != null) {
             node.x = params.x + dpToPx(30)
             node.y = params.y + dpToPx(30)
        }
    }

    fun removeAllViews() {
        try { floatingControlBar?.let { windowManager.removeView(it) } } catch(e: Exception){}
        try { modMenu?.let { windowManager.removeView(it) } } catch(e: Exception){}
        try { nodeViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { swipeEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { textZoneStartViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { textZoneEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { colorCompareViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { linesOverlayView?.let { windowManager.removeView(it) } } catch(e: Exception){}
        hideDebugWindow()
    }

    fun bringControlBarToFront() {
        floatingControlBar?.let { 
            val params = it.layoutParams as? WindowManager.LayoutParams
            if (params != null) {
                windowManager.removeView(it)
                windowManager.addView(it, params)
            }
        }
    }

    fun toggleDebugWindow() {
        isDebugWindowVisible = !isDebugWindowVisible
        if (isDebugWindowVisible) {
            showDebugWindow()
        } else {
            hideDebugWindow()
        }
    }

    private fun showDebugWindow() {
        if (debugWindow != null) return
        
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(10, 10, 10, 10)
        }

        val header = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(service).apply {
                text = "Дебаг Лог"
                setTextColor(Color.WHITE)
                setScaledTextSize(14f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(Button(service).apply {
                text = "X"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.RED)
                layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
                setOnClickListener { toggleDebugWindow() }
            })
        }
        layout.addView(header)

        debugScrollView = ScrollView(service).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(350), dpToPx(300))
        }

        debugTextView = TextView(service).apply {
            setTextColor(Color.GREEN)
            setScaledTextSize(12f)
            text = debugLogs.joinToString("\n")
            setPadding(0, 0, 0, dpToPx(10))
        }

        debugScrollView!!.addView(debugTextView)
        layout.addView(debugScrollView)

        // Clear log button
        val btnLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(service).apply {
                text = "Очистить"
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    debugLogs.clear()
                    debugTextView?.text = ""
                }
            })
        }
        layout.addView(btnLayout)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = dpToPx(10)
        params.y = dpToPx(100)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(layout, params)
                    true
                }
                else -> false
            }
        }

        debugWindow = layout
        windowManager.addView(layout, params)
    }

    private fun hideDebugWindow() {
        debugWindow?.let {
            windowManager.removeView(it)
            debugWindow = null
            debugScrollView = null
            debugTextView = null
        }
    }

    fun logDebug(msg: String) {
        val time = android.text.format.DateFormat.format("HH:mm:ss", java.util.Date())
        debugLogs.add("[$time] $msg")
        if (debugLogs.size > 100) debugLogs.removeAt(0)
        
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            debugTextView?.let { tv ->
                tv.text = debugLogs.joinToString("\n")
                debugScrollView?.post {
                    debugScrollView?.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    fun showOcrResultDialog(ocrText: String, searchedText: String, isMatch: Boolean, image: android.graphics.Bitmap) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val dialogView = android.widget.LinearLayout(service).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
                
                addView(android.widget.TextView(service).apply {
                    text = "Huawei OCR: '$ocrText'\n\nИскали: '$searchedText'\nИтог: $isMatch\n\nЧто видит OCR:"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 14f
                })
                
                val iv = android.widget.ImageView(service).apply {
                    setImageBitmap(image)
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        dpToPx(200)
                    ).apply { setMargins(0, dpToPx(10), 0, 0) }
                    scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                }
                addView(iv)
            }
            
            val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Результат OCR (Huawei)")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .create()
                
            dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            dialog.show()
        }
    }
}
