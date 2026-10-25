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

class ZoneCornerView(
    context: Context,
    val node: TargetNode,
    val isZone2: Boolean,
    val isStart: Boolean,
    var isSelectedCorner: Boolean = false
) : View(context) {
    private val density = context.resources.displayMetrics.density

    private val mainColor: Int
        get() = when {
            isZone2 && isStart -> Color.parseColor("#00E5FF")
            isZone2 && !isStart -> Color.parseColor("#448AFF")
            !isZone2 && isStart -> Color.parseColor("#00E676")
            else -> Color.parseColor("#FF5252")
        }

    private val shadowPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.parseColor("#CC000000")
        strokeWidth = 3.2f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val crossPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 1.6f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val bracketPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 2.4f * density
        strokeCap = Paint.Cap.SQUARE
    }

    private val selectRingPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.parseColor("#FFD600")
        strokeWidth = 1.6f * density
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f * density, 4f * density), 0f)
    }

    private val badgeBgPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.parseColor("#D9101824")
    }

    private val badgeTextPaint = Paint().apply {
        isAntiAlias = true
        textSize = 9.5f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val c = mainColor
        crossPaint.color = c
        bracketPaint.color = c
        badgeTextPaint.color = c

        val gap = 2.5f * density
        val arm = 11f * density

        // Тёмная подложка под тонкий крестик для идеальной видимости на любом фоне
        canvas.drawLine(cx - arm, cy, cx - gap, cy, shadowPaint)
        canvas.drawLine(cx + gap, cy, cx + arm, cy, shadowPaint)
        canvas.drawLine(cx, cy - arm, cx, cy - gap, shadowPaint)
        canvas.drawLine(cx, cy + gap, cx, cy + arm, shadowPaint)

        // Тонкий снайперский крестик с прозрачным центром ровно в точке (cx, cy)
        canvas.drawLine(cx - arm, cy, cx - gap, cy, crossPaint)
        canvas.drawLine(cx + gap, cy, cx + arm, cy, crossPaint)
        canvas.drawLine(cx, cy - arm, cx, cy - gap, crossPaint)
        canvas.drawLine(cx, cy + gap, cx, cy + arm, crossPaint)

        // Г-образный уголок в сторону внутренней части зоны
        val dirX = if (isStart) 1f else -1f
        val dirY = if (isStart) 1f else -1f
        val bLen = 13f * density
        canvas.drawLine(cx, cy, cx + dirX * bLen, cy, bracketPaint)
        canvas.drawLine(cx, cy, cx, cy + dirY * bLen, bracketPaint)

        // Подсветка активного угла при работе со стрелочками сдвига
        if (isSelectedCorner) {
            canvas.drawCircle(cx, cy, 13.5f * density, selectRingPaint)
        }

        // Компактный бейдж снаружи угла, чтобы не перекрывать проверяемую область внутри рамки
        val label = if (isZone2) {
            if (isStart) "2↖" else "↘2"
        } else {
            if (isStart) "1↖" else "↘1"
        }
        val badgeOffsetX = -dirX * 9.5f * density
        val badgeOffsetY = -dirY * 9.5f * density
        val bx = (cx + badgeOffsetX).coerceIn(8f * density, width - 8f * density)
        val by = (cy + badgeOffsetY).coerceIn(7f * density, height - 7f * density)
        val rect = android.graphics.RectF(bx - 7.5f * density, by - 5.5f * density, bx + 7.5f * density, by + 5.5f * density)
        canvas.drawRoundRect(rect, 3.5f * density, 3.5f * density, badgeBgPaint)
        val textY = by - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2f
        canvas.drawText(label, bx, textY, badgeTextPaint)
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
    private var zoneNudgePopupView: View? = null
    private var zoneNudgeParams: WindowManager.LayoutParams? = null
    private var zoneNudgeNodeId: Int? = null
    private var zoneNudgeIsZone2: Boolean = false
    // 0 = Угол 1 (Start), 1 = Вся рамка, 2 = Угол 2 (End)
    private var zoneNudgeCornerMode: Int = 1
    private var zoneNudgeStepPx: Int = 1
    private var zoneNudgeSizeTextView: TextView? = null
    private var modMenu: View? = null
    private var modMenuParams: WindowManager.LayoutParams? = null
    var isMenuFullscreen = false
    var isCaffeineEnabled = false
    var showEyeBtn = true
    var showLinesBtn = true
    var showHotbarBtn = true
    var showTreeBtn = true
    var uiAccentColor: String = "#00C853"
    private lateinit var menuContentContainer: FrameLayout
    private var fullscreenBtnRef: Button? = null
    
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

    val compareZoneStartViews = mutableMapOf<Int, View>()
    val compareZoneStartParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    val compareZoneEndViews = mutableMapOf<Int, View>()
    val compareZoneEndParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    val colorCompareViews = mutableMapOf<Int, View>()
    val colorCompareParams = mutableMapOf<Int, WindowManager.LayoutParams>()

    var nodeCounter = 1
    var appMode: AppMode = AppMode.ADVANCED
    var uiScale: Float = 1.0f
    var uiAlpha: Float = 0.9f
    var hudX: Int = 0
    var hudY: Int = 100
    var isHudVisible: Boolean = true

    fun loadUISettings() {
        val prefs = service.getSharedPreferences("AutoClickerUISettings", android.content.Context.MODE_PRIVATE)
        val mainPrefs = service.getSharedPreferences("AutoClickerSettings", android.content.Context.MODE_PRIVATE)
        uiScale = prefs.getFloat("uiScale", 1.0f)
        uiAlpha = prefs.getFloat("uiAlpha", 0.9f)
        showEyeBtn = prefs.getBoolean("showEyeBtn", true)
        showLinesBtn = prefs.getBoolean("showLinesBtn", true)
        showHotbarBtn = prefs.getBoolean("showHotbarBtn", true)
        showTreeBtn = prefs.getBoolean("showTreeBtn", true)
        isDebugWindowVisible = prefs.getBoolean("isDebugWindowVisible", false)
        isCaffeineEnabled = prefs.getBoolean("isCaffeineEnabled", false)
        isMenuFullscreen = prefs.getBoolean("isMenuFullscreen", false)
        uiAccentColor = mainPrefs.getString("uiAccentColor", prefs.getString("uiAccentColor", "#00C853")) ?: "#00C853"
        service.enableMultitouch = prefs.getBoolean("enableMultitouch", false)
        service.allowExtremeSpeed = prefs.getBoolean("allowExtremeSpeed", false)
        service.cycleEndDelayMs = prefs.getLong("cycleEndDelayMs", 0L)
        hudX = prefs.getInt("hudX", 0)
        hudY = prefs.getInt("hudY", 100)
        isHudVisible = prefs.getBoolean("isHudVisible", true)
    }

    fun saveUISettings() {
        val prefs = service.getSharedPreferences("AutoClickerUISettings", android.content.Context.MODE_PRIVATE)
        val mainPrefs = service.getSharedPreferences("AutoClickerSettings", android.content.Context.MODE_PRIVATE)
        mainPrefs.edit().putString("uiAccentColor", uiAccentColor).apply()
        prefs.edit().apply {
            putFloat("uiScale", uiScale)
            putFloat("uiAlpha", uiAlpha)
            putBoolean("showEyeBtn", showEyeBtn)
            putBoolean("showLinesBtn", showLinesBtn)
            putBoolean("showHotbarBtn", showHotbarBtn)
            putBoolean("showTreeBtn", showTreeBtn)
            putBoolean("isDebugWindowVisible", isDebugWindowVisible)
            putBoolean("isCaffeineEnabled", isCaffeineEnabled)
            putBoolean("isMenuFullscreen", isMenuFullscreen)
            putString("uiAccentColor", uiAccentColor)
            putBoolean("enableMultitouch", service.enableMultitouch)
            putBoolean("allowExtremeSpeed", service.allowExtremeSpeed)
            putLong("cycleEndDelayMs", service.cycleEndDelayMs)
            putInt("hudX", hudX)
            putInt("hudY", hudY)
            putBoolean("isHudVisible", isHudVisible)
            apply()
        }
    }

    fun applyThemeColor(colorHex: String) {
        try {
            Color.parseColor(colorHex)
            uiAccentColor = colorHex
            saveUISettings()
            val currentTag = if (::menuContentContainer.isInitialized) menuContentContainer.getChildAt(0)?.tag as? String else null
            recreateFloatingControlBar()
            recreateModMenu(currentTag)
        } catch (_: Exception) {}
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
        if (!touchable) {
            hideZoneNudgePopup()
        }
        val lists = listOf(
            nodeViews to nodeParams,
            swipeEndViews to swipeEndParams,
            colorCompareViews to colorCompareParams,
            textZoneStartViews to textZoneStartParams,
            textZoneEndViews to textZoneEndParams,
            compareZoneStartViews to compareZoneStartParams,
            compareZoneEndViews to compareZoneEndParams
        )
        val referencedColorIds = service.nodes.mapNotNull { it.compareToNodeId }.toSet()
        for ((viewsMap, paramsMap) in lists) {
            for ((id, view) in viewsMap) {
                val params = paramsMap[id] ?: continue
                if (touchable) {
                    params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                } else {
                    params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }
                val node = service.nodes.find { it.id == id }
                if (!touchable) {
                    // Во время работы макроса скрываем маркеры зон и прицелы проверяемых меток,
                    // чтобы они не перекрывали маленькие картинки и цвета на скриншотах takeScreenshot
                    val isZoneOrColorMarker = (viewsMap === textZoneStartViews || viewsMap === textZoneEndViews ||
                        viewsMap === compareZoneStartViews || viewsMap === compareZoneEndViews || viewsMap === colorCompareViews)
                    val isVisualCheckNode = (viewsMap === nodeViews && node != null && (node.triggerMode >= 0 || referencedColorIds.contains(node.id)))
                    if (isZoneOrColorMarker || isVisualCheckNode) {
                        view.visibility = View.INVISIBLE
                    }
                } else if (node != null) {
                    view.visibility = if (node.isVisible) View.VISIBLE else View.GONE
                }
                try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
            }
        }
        invalidateLines()
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
        if (floatingControlBar == null) {
            isHudVisible = true
            saveUISettings()
            showFloatingControlBar()
            return
        }
        floatingControlBar?.let {
            val isVisible = it.visibility == View.VISIBLE
            it.visibility = if (isVisible) View.GONE else View.VISIBLE
            isHudVisible = (it.visibility == View.VISIBLE)
            saveUISettings()
        }
    }


    private fun disableOverlaySelectionToolbar(editText: EditText) {
        val noPopupCallback = object : android.view.ActionMode.Callback {
            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: android.view.Menu?): Boolean = false
            override fun onActionItemClicked(mode: android.view.ActionMode?, item: android.view.MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }
        editText.customSelectionActionModeCallback = noPopupCallback
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            editText.customInsertionActionModeCallback = noPopupCallback
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            editText.setTextClassifier(android.view.textclassifier.TextClassifier.NO_OP)
        }
        editText.isLongClickable = false
    }

    private fun showHotbarConfigDialog() {
        val allProfiles = service.getSavedProfiles()
        val currentHotbar = service.getHotbarItems().toMutableList()
        val themedCtx = android.view.ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        
        val dialogView = ScrollView(themedCtx).apply {
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }
        val layout = LinearLayout(themedCtx).apply { orientation = LinearLayout.VERTICAL }
        dialogView.addView(layout)
        
        val resultList = mutableListOf<Triple<String, android.widget.CheckBox, android.widget.EditText>>()
        
        for (p in allProfiles) {
            val itemLayout = LinearLayout(themedCtx).apply { 
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dpToPx(10))
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            val cb = android.widget.CheckBox(themedCtx).apply {
                text = p
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val et = android.widget.EditText(themedCtx).apply {
                hint = "Смайл/Имя"
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setSingleLine(true)
                disableOverlaySelectionToolbar(this)
                layoutParams = LinearLayout.LayoutParams(dpToPx(110), LinearLayout.LayoutParams.WRAP_CONTENT)
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
            
            resultList.add(Triple(p, cb, et))
        }
        
        val dialog = android.app.AlertDialog.Builder(themedCtx)
            .setTitle("Настройка Хотбара")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newItems = mutableListOf<Pair<String, String>>()
                for ((profileName, cb, et) in resultList) {
                    if (cb.isChecked) {
                        val customLabel = et.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: profileName
                        newItems.add(Pair(profileName, customLabel))
                    }
                }
                service.saveHotbarItems(newItems)
                hotbarContainer?.let {
                    try {
                        updateHotbar(it)
                    } catch (_: Exception) {}
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
            
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)
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
                    service.loadProfile(p.first)
                    service.uiManager.updateMenu()
                }
            }
            container.addView(btn)
        }
    }
    private var isHudMinimized = false

    private fun showQuickAddNodeDialog() {
        val items = arrayOf(
            "🎯 Метка (Клик / Свайп / Удержание)",
            "👁 Триггер (Проверка условия)",
            "🔀 Менеджер (Ветвление условий)",
            "⚡ Вызов скрипта (Подмакрос)",
            "🌳 Открыть Древо сценария"
        )
        val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Быстрое действие")
            .setItems(items) { _, which ->
                val (screenW, screenH) = getScreenDimensions()
                val offset = (service.nodes.size % 6) * dpToPx(18)
                val cx = (screenW / 2 + offset).coerceIn(dpToPx(40), (screenW - dpToPx(40)).coerceAtLeast(dpToPx(40)))
                val cy = (screenH / 2 + offset).coerceIn(dpToPx(40), (screenH - dpToPx(40)).coerceAtLeast(dpToPx(40)))
                when (which) {
                    0 -> addNode(NodeType.CLICK, cx, cy)
                    1 -> addNode(NodeType.CHECK_COLOR, cx, cy)
                    2 -> addNode(NodeType.MANAGER, cx, cy)
                    3 -> addNode(NodeType.MACRO, cx, cy)
                    4 -> {
                        if (modMenu == null || modMenu?.visibility != View.VISIBLE) {
                            showModMenu()
                        }
                        showScenarioTreeMenu()
                    }
                }
                recreateFloatingControlBar()
            }
            .setNegativeButton("Отмена", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }

    private fun showExitConfirmationDialog() {
        val options = arrayOf(
            "🙈 Просто скрыть панель HUD",
            "🗑 Очистить все метки (${service.nodes.size} шт.)",
            "❌ Закрыть HUD и очистить метки"
        )
        val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Управление панелью HUD")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        toggleHudVisibility()
                        android.widget.Toast.makeText(service, "HUD скрыт (можно вернуть из приложения)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (service.isPlaying) service.togglePlay()
                        if (service.isRecording) service.toggleRecording()
                        val ids = service.nodes.map { it.id }
                        ids.forEach { removeNode(it) }
                        recreateFloatingControlBar()
                        android.widget.Toast.makeText(service, "Все метки очищены", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        if (service.isPlaying) service.togglePlay()
                        if (service.isRecording) service.toggleRecording()
                        service.nodes.clear()
                        service.autoSave()
                        isHudVisible = false
                        saveUISettings()
                        removeAllViews()
                        floatingControlBar = null
                        modMenu = null
                        nodeViews.clear()
                        swipeEndViews.clear()
                        textZoneStartViews.clear()
                        textZoneEndViews.clear()
                        compareZoneStartViews.clear()
                        compareZoneEndViews.clear()
                        colorCompareViews.clear()
                        linesOverlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        linesOverlayView = null
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }

    fun showFloatingControlBar() {
        if (floatingControlBar != null) {
            floatingControlBar?.visibility = if (isHudVisible) View.VISIBLE else View.GONE
            return
        }

        val borderColor = when {
            service.isPlaying -> Color.parseColor("#00E676")
            service.isRecording -> Color.parseColor("#FF5252")
            else -> Color.parseColor("#3D5AFE")
        }

        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(6), dpToPx(5), dpToPx(6), dpToPx(5))
            alpha = uiAlpha

            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(Color.parseColor("#EE141824"))
            drawable.setStroke(dpToPx(2), borderColor)
            drawable.cornerRadius = dpToPx(18).toFloat()
            background = drawable
            clipToOutline = true
        }

        val topRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val hotbarRow = HorizontalScrollView(service).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(dpToPx(280), WindowManager.LayoutParams.WRAP_CONTENT)
            setPadding(0, dpToPx(6), 0, dpToPx(2))
        }
        val hotbarContainer = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        hotbarRow.addView(hotbarContainer)

        fun styleHudButton(btn: Button, bgColor: String = "#23293A", wDp: Int = 38, hDp: Int = 38) {
            btn.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = dpToPx(11).toFloat()
            }
            btn.layoutParams = LinearLayout.LayoutParams(dpToPx(wDp), dpToPx(hDp)).apply {
                setMargins(dpToPx(2), 0, dpToPx(2), 0)
            }
            btn.setPadding(0, 0, 0, 0)
            btn.setScaledTextSize(14f)
        }

        val dragHandle = Button(service).apply {
            text = "⠿"
            setTextColor(Color.parseColor("#90A4AE"))
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dpToPx(26), dpToPx(38))
            setPadding(0, 0, 0, 0)
            setScaledTextSize(16f)
        }

        val statusBadge = TextView(service).apply {
            text = when {
                service.isPlaying -> "● РАБОТА"
                service.isRecording -> "● ЗАПИСЬ"
                else -> "${service.nodes.size}🎯"
            }
            setTextColor(
                when {
                    service.isPlaying -> Color.parseColor("#00E676")
                    service.isRecording -> Color.parseColor("#FF5252")
                    else -> Color.parseColor("#B0BEC5")
                }
            )
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(11f)
            setPadding(dpToPx(7), dpToPx(4), dpToPx(7), dpToPx(4))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1D2230"))
                cornerRadius = dpToPx(10).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(2), 0, dpToPx(3), 0)
            }
            visibility = if (showTreeBtn) View.VISIBLE else View.GONE
            setOnClickListener {
                if (modMenu == null || modMenu?.visibility != View.VISIBLE) {
                    showModMenu()
                }
                showScenarioTreeMenu()
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Нажмите для открытия Древа сценария (меток: ${service.nodes.size})", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val playBtn = Button(service).apply {
            text = if (service.isPlaying) "⏸" else "▶"
            setTextColor(Color.WHITE)
            styleHudButton(this, if (service.isPlaying) "#D32F2F" else "#00C853", 40, 38)
            setOnClickListener {
                service.togglePlay()
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Старт / Пауза макроса", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val quickAddBtn = Button(service).apply {
            text = "➕"
            setTextColor(Color.WHITE)
            styleHudButton(this, uiAccentColor, 38, 38)
            visibility = if (service.isPlaying || service.isRecording) View.GONE else View.VISIBLE
            setOnClickListener {
                val metrics = service.resources.displayMetrics
                val offset = (service.nodes.size % 6) * dpToPx(18)
                val cx = (metrics.widthPixels / 2 + offset).coerceIn(dpToPx(40), metrics.widthPixels - dpToPx(40))
                val cy = (metrics.heightPixels / 2 + offset).coerceIn(dpToPx(40), metrics.heightPixels - dpToPx(40))
                addNode(NodeType.CLICK, cx, cy)
                recreateFloatingControlBar()
            }
            setOnLongClickListener {
                showQuickAddNodeDialog()
                true
            }
        }

        val recordBtn = Button(service).apply {
            text = if (service.isRecording) "■" else "🔴"
            setTextColor(if (service.isRecording) Color.parseColor("#FFD50000") else Color.WHITE)
            styleHudButton(this, if (service.isRecording) "#3E1A1A" else "#23293A")
            visibility = if (appMode == AppMode.RECORD) View.VISIBLE else View.GONE
            setOnClickListener {
                service.toggleRecording()
                recreateFloatingControlBar()
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Запись действий в реальном времени", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val allVisibleNow = service.nodes.isEmpty() || service.nodes.any { it.isVisible }
        val toggleVisBtn = Button(service).apply {
            text = if (allVisibleNow) "👁" else "🙈"
            setTextColor(Color.WHITE)
            styleHudButton(this, if (allVisibleNow) "#23293A" else "#37474F")
            visibility = if (showEyeBtn) View.VISIBLE else View.GONE
            setOnClickListener {
                val targetVis = service.nodes.any { !it.isVisible }
                service.nodes.forEach {
                    it.isVisible = targetVis
                    nodeViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    swipeEndViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    textZoneStartViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    textZoneEndViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    compareZoneStartViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    compareZoneEndViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                    colorCompareViews[it.id]?.visibility = if (it.isVisible) View.VISIBLE else View.GONE
                }
                text = if (targetVis) "👁" else "🙈"
                invalidateLines()
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Показать / скрыть метки на экране", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val linesToggleBtn = Button(service).apply {
            text = if (service.showLines) "🕸" else "🕸✖"
            setTextColor(Color.WHITE)
            styleHudButton(this, if (service.showLines) uiAccentColor else "#23293A")
            visibility = if (showLinesBtn) View.VISIBLE else View.GONE
            setOnClickListener {
                service.showLines = !service.showLines
                text = if (service.showLines) "🕸" else "🕸✖"
                styleHudButton(this, if (service.showLines) uiAccentColor else "#23293A")
                linesOverlayView?.visibility = if (service.showLines) View.VISIBLE else View.INVISIBLE
                linesOverlayView?.invalidate()
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Линии маршрутов, зон и траекторий свайпов", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val hotbarToggleBtn = Button(service).apply {
            text = "⚡"
            setTextColor(Color.parseColor("#FFD54F"))
            styleHudButton(this)
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
            setOnLongClickListener {
                showHotbarConfigDialog()
                true
            }
        }

        val gearBtn = Button(service).apply {
            text = "☰"
            setTextColor(Color.WHITE)
            styleHudButton(this, "#2E3B4E")
            visibility = View.VISIBLE
            setOnClickListener { showModMenu() }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Главное меню и список меток", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val exitBtn = Button(service).apply {
            text = "✖"
            setTextColor(Color.parseColor("#FF8A80"))
            styleHudButton(this, "#3B1E22", 34, 38)
            setOnClickListener {
                showExitConfirmationDialog()
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Скрыть HUD или очистить метки", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        val scrollContent = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val topScroll = android.widget.HorizontalScrollView(service).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(scrollContent)
        }

        val minMaxBtn = Button(service).apply {
            text = if (isHudMinimized) "▸" else "◂"
            setTextColor(Color.parseColor("#90A4AE"))
            styleHudButton(this, "#1D2230", 28, 38)
            setOnClickListener {
                isHudMinimized = !isHudMinimized
                text = if (isHudMinimized) "▸" else "◂"
                val vis = if (isHudMinimized) View.GONE else View.VISIBLE
                statusBadge.visibility = if (isHudMinimized || !showTreeBtn) View.GONE else View.VISIBLE
                quickAddBtn.visibility = if (isHudMinimized || service.isPlaying || service.isRecording) View.GONE else View.VISIBLE
                recordBtn.visibility = if (isHudMinimized || appMode != AppMode.RECORD) View.GONE else View.VISIBLE
                toggleVisBtn.visibility = if (isHudMinimized || !showEyeBtn) View.GONE else View.VISIBLE
                linesToggleBtn.visibility = if (isHudMinimized || !showLinesBtn) View.GONE else View.VISIBLE
                hotbarToggleBtn.visibility = if (isHudMinimized || !showHotbarBtn) View.GONE else View.VISIBLE
                gearBtn.visibility = vis
                exitBtn.visibility = vis
                topScroll.visibility = vis
                if (isHudMinimized) hotbarRow.visibility = View.GONE
                floatingControlBar?.let {
                    val p = it.layoutParams as? WindowManager.LayoutParams
                    if (p != null) windowManager.updateViewLayout(it, p)
                }
            }
            setOnLongClickListener {
                android.widget.Toast.makeText(service, "Свернуть / развернуть HUD (кнопка Старт остаётся доступной)", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
        }

        // Apply initial minimized state
        if (isHudMinimized) {
            statusBadge.visibility = View.GONE
            quickAddBtn.visibility = View.GONE
            topScroll.visibility = View.GONE
            gearBtn.visibility = View.GONE
            exitBtn.visibility = View.GONE
        }

        topRow.addView(dragHandle)
        topRow.addView(playBtn)
        topRow.addView(quickAddBtn)
        topRow.addView(statusBadge)
        scrollContent.addView(recordBtn)
        scrollContent.addView(toggleVisBtn)
        scrollContent.addView(linesToggleBtn)
        scrollContent.addView(hotbarToggleBtn)
        topRow.addView(topScroll)
        topRow.addView(gearBtn)
        topRow.addView(exitBtn)
        topRow.addView(minMaxBtn)

        layout.addView(topRow)
        layout.addView(hotbarRow)

        var barFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (isCaffeineEnabled) barFlags = barFlags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val (screenWidth, screenHeight) = getScreenDimensions()
        val safeX = hudX.coerceIn(0, (screenWidth - dpToPx(60)).coerceAtLeast(0))
        val safeY = hudY.coerceIn(0, (screenHeight - dpToPx(60)).coerceAtLeast(0))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            barFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = safeX
            y = safeY
        }

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
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val (realW, realH) = getScreenDimensions()
                    var newX = initialX + dx.toInt()
                    var newY = initialY + dy.toInt()
                    newX = newX.coerceIn(0, (realW - dpToPx(40)).coerceAtLeast(0))
                    newY = newY.coerceIn(0, (realH - dpToPx(40)).coerceAtLeast(0))
                    params.x = newX
                    params.y = newY
                    hudX = newX
                    hudY = newY
                    windowManager.updateViewLayout(layout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    hudX = params.x
                    hudY = params.y
                    saveUISettings()
                    true
                }
                else -> false
            }
        }

        layout.visibility = if (isHudVisible) View.VISIBLE else View.GONE
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    newX = newX.coerceIn(0, (screenWidth - dpToPx(50)).coerceAtLeast(0))
                    newY = newY.coerceIn(0, (screenHeight - dpToPx(50)).coerceAtLeast(0))
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(windowView, params)
                    true
                }
                else -> false
            }
        }
    }

    private var currentActiveTabIndex = 0
    private var tabButtonsRef = mutableListOf<Button>()

    private fun stylePillButton(btn: Button, bgColorHex: String, strokeHex: String? = null, radiusDp: Int = 10, textSizeSp: Float = 13f) {
        btn.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(bgColorHex))
            cornerRadius = dpToPx(radiusDp).toFloat()
            if (strokeHex != null) {
                setStroke(dpToPx(1), Color.parseColor(strokeHex))
            }
        }
        btn.setTextColor(Color.WHITE)
        btn.setScaledTextSize(textSizeSp)
        btn.setTypeface(null, Typeface.BOLD)
        btn.isAllCaps = false
    }

    private fun updateTabSelection(selectedIndex: Int) {
        currentActiveTabIndex = selectedIndex
        for ((idx, btn) in tabButtonsRef.withIndex()) {
            val isSelected = idx == selectedIndex
            btn.background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor(if (isSelected) uiAccentColor else "#1D2230"))
                cornerRadius = dpToPx(8).toFloat()
            }
            btn.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#90A4AE"))
            btn.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun getScreenDimensions(): Pair<Int, Int> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    private fun getMenuContentLayoutParams(windowedHeightDp: Int): FrameLayout.LayoutParams {
        return if (isMenuFullscreen) {
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        } else {
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(windowedHeightDp)
            )
        }
    }

    private fun buildModMenuLayout(params: WindowManager.LayoutParams): View {
        val (screenW, screenH) = getScreenDimensions()
        val root = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))
            alpha = uiAlpha
            if (isMenuFullscreen) {
                minimumWidth = screenW
                minimumHeight = screenH
            }

            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(Color.parseColor("#F2121620"))
            drawable.setStroke(dpToPx(2), Color.parseColor(uiAccentColor))
            drawable.cornerRadius = if (isMenuFullscreen) 0f else dpToPx(16).toFloat()
            background = drawable
            clipToOutline = true
        }

        val topBar = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(4), dpToPx(8))
        }
        val header = TextView(service).apply {
            text = "⠿ UpwellClick"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(16f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        val helpTopBtn = Button(service).apply {
            text = "❓"
            stylePillButton(this, "#23293A", null, 8, 12f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setOnClickListener { showHelpMenu() }
        }
        val fullscreenBtn = Button(service).apply {
            text = if (isMenuFullscreen) "🗗" else "⛶"
            stylePillButton(this, "#23293A", null, 8, 13f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(32)).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setOnClickListener { toggleMenuFullscreen() }
        }
        fullscreenBtnRef = fullscreenBtn
        val closeBtn = Button(service).apply {
            text = "✕"
            stylePillButton(this, "#3B1E22", null, 8, 13f)
            setTextColor(Color.parseColor("#FF8A80"))
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(32))
            setOnClickListener { modMenu?.visibility = View.GONE }
        }
        topBar.addView(header)
        topBar.addView(helpTopBtn)
        topBar.addView(fullscreenBtn)
        topBar.addView(closeBtn)
        root.addView(topBar)

        setupDraggableWindow(topBar, root, params)

        val tabsLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#181C26"))
                cornerRadius = dpToPx(10).toFloat()
            }
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dpToPx(8))
            layoutParams = lp
        }

        tabButtonsRef.clear()
        val tabDefs = listOf(
            "🎯 Метки" to { updateTabSelection(0); showMainMenu() },
            "📁 Профили" to { updateTabSelection(1); showProfileMenu(false) },
            "🎛 Режим" to { updateTabSelection(2); showModeMenu() },
            "⚙ Опции" to { updateTabSelection(3); showSettingsMenu() }
        )
        for ((idx, pair) in tabDefs.withIndex()) {
            val tBtn = Button(service).apply {
                text = pair.first
                setScaledTextSize(11.5f)
                isAllCaps = false
                setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6))
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply {
                    if (idx < tabDefs.size - 1) setMargins(0, 0, dpToPx(3), 0)
                }
                setOnClickListener { pair.second() }
            }
            tabButtonsRef.add(tBtn)
            tabsLayout.addView(tBtn)
        }
        root.addView(tabsLayout)

        menuContentContainer = FrameLayout(service).apply {
            layoutParams = if (isMenuFullscreen) {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
        }
        root.addView(menuContentContainer)

        updateTabSelection(0)
        showMainMenu()

        return root
    }

    private fun getMenuDefaultWidth(): Int {
        val isLandscape = service.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        return if (isLandscape) dpToPx(460) else dpToPx(330)
    }

    private var currentEditingNode: TargetNode? = null

    fun toggleMenuFullscreen() {
        isMenuFullscreen = !isMenuFullscreen
        saveUISettings()
        val (screenW, screenH) = getScreenDimensions()
        modMenuParams?.let { params ->
            if (isMenuFullscreen) {
                params.width = screenW
                params.height = screenH
                params.x = 0
                params.y = 0
            } else {
                params.width = getMenuDefaultWidth()
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.x = dpToPx(24)
                params.y = dpToPx(60)
            }
            fullscreenBtnRef?.text = if (isMenuFullscreen) "🗗" else "⛶"
            modMenu?.let { root ->
                root.minimumWidth = if (isMenuFullscreen) screenW else 0
                root.minimumHeight = if (isMenuFullscreen) screenH else 0
                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.setColor(Color.parseColor("#F2121620"))
                drawable.setStroke(dpToPx(2), Color.parseColor(uiAccentColor))
                drawable.cornerRadius = if (isMenuFullscreen) 0f else dpToPx(16).toFloat()
                root.background = drawable
            }
            if (::menuContentContainer.isInitialized) {
                menuContentContainer.layoutParams = if (isMenuFullscreen) {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                } else {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
            }
            if (modMenu != null) {
                windowManager.updateViewLayout(modMenu, params)
                val currentTag = menuContentContainer.getChildAt(0)?.tag as? String
                when (currentTag) {
                    "main_menu" -> showMainMenu()
                    "help_menu" -> showHelpMenu()
                    "tree_menu" -> showScenarioTreeMenu()
                    "profile_menu_save" -> showProfileMenu(true)
                    "profile_menu_load" -> showProfileMenu(false)
                    "mode_menu" -> showModeMenu()
                    "settings_menu" -> showSettingsMenu()
                    "edit_menu" -> currentEditingNode?.let { showEditNodeMenu(it) } ?: modMenu?.requestLayout()
                    else -> modMenu?.requestLayout()
                }
            }
        }
    }

    private fun showModMenu() {
        if (modMenu == null) {
            val (screenW, screenH) = getScreenDimensions()
            val params = WindowManager.LayoutParams(
                if (isMenuFullscreen) screenW else getMenuDefaultWidth(),
                if (isMenuFullscreen) screenH else WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = if (isMenuFullscreen) 0 else dpToPx(24)
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
            if (isMenuFullscreen) {
                val (screenW, screenH) = getScreenDimensions()
                params.width = screenW
                params.height = screenH
                params.x = 0
                params.y = 0
            } else {
                params.width = getMenuDefaultWidth()
            }
            if (modMenu != null) {
                windowManager.updateViewLayout(modMenu, params)
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
                "main_menu" -> { updateTabSelection(0); showMainMenu() }
                "help_menu" -> showHelpMenu()
                "tree_menu" -> showScenarioTreeMenu()
                "profile_menu_save", "profile_menu_load" -> { updateTabSelection(1); showProfileMenu(false) }
                "mode_menu" -> { updateTabSelection(2); showModeMenu() }
                "settings_menu" -> { updateTabSelection(3); showSettingsMenu() }
            }
        }
    }

    fun applyUIScaleChange() {
        val currentTag = menuContentContainer.getChildAt(0)?.tag as? String
        recreateFloatingControlBar()

        service.nodes.forEach {
            nodeViews[it.id]?.let { view -> windowManager.removeView(view) }
            swipeEndViews[it.id]?.let { view -> windowManager.removeView(view) }
            colorCompareViews[it.id]?.let { view -> windowManager.removeView(view) }
            textZoneStartViews[it.id]?.let { view -> windowManager.removeView(view) }
            textZoneEndViews[it.id]?.let { view -> windowManager.removeView(view) }
            compareZoneStartViews[it.id]?.let { view -> windowManager.removeView(view) }
            compareZoneEndViews[it.id]?.let { view -> windowManager.removeView(view) }
        }
        nodeViews.clear()
        swipeEndViews.clear()
        colorCompareViews.clear()
        textZoneStartViews.clear()
        textZoneEndViews.clear()
        compareZoneStartViews.clear()
        compareZoneEndViews.clear()

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
            if ((node.triggerMode == 1 || node.triggerMode == 2) && node.compareZoneEnabled && node.compareZoneStartX != 0) {
                createCompareZoneMarkers(node)
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
            if ((node.triggerMode == 1 || node.triggerMode == 2) && node.compareZoneEnabled && node.compareZoneStartX != 0) {
                createCompareZoneMarkers(node)
            }
        }
        updateMenu()
    }

    private fun getNodeSummarySubtitle(node: TargetNode): String {
        val parts = mutableListOf<String>()
        when (node.type) {
            NodeType.CLICK -> {
                when {
                    node.isSwipe && node.swipePathPoints.size >= 2 -> parts.add("Кривая (${node.swipeDurationMs} мс)")
                    node.isSwipe -> parts.add("Свайп ${node.swipeDurationMs} мс")
                    node.isHold -> parts.add("Зажатие в фоне ${node.holdDurationMs} мс")
                    else -> parts.add("Клик ${node.clickDurationMs} мс" + if (node.repetitions > 1) " ×${node.repetitions}" else "")
                }
                parts.add("пауза ${node.delayAfterMs} мс")
            }
            NodeType.CHECK_COLOR -> {
                parts.add("Только проверка")
            }
            NodeType.MANAGER -> {
                parts.add("Условий: ${node.managerRoutes.size}")
            }
            NodeType.MACRO -> {
                parts.add("Скрипт: ${node.macroProfileName ?: "не выбран"}")
            }
        }
        when (node.triggerMode) {
            0 -> parts.add("🎨 Цвет")
            1 -> parts.add("🖼 Картинка")
            2 -> parts.add(if (node.isSmartOcr) "🔢 Число OCR" else "🔤 Текст OCR")
        }
        if (node.isIndependentThread) parts.add("⚡ Поток")
        if (node.skipSequentialExecution) parts.add("Функция")
        return parts.joinToString(" • ")
    }

    private fun showMainMenu() {
        updateTabSelection(0)
        setMenuFocusable(false)
        menuContentContainer.removeAllViews()
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            tag = "main_menu"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                if (isMenuFullscreen) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Play / Record primary bar
        val isRec = appMode == AppMode.RECORD
        val controlRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }
        if (isRec) {
            val rpStart = Button(service).apply {
                text = if (service.isRecording) "■ Стоп запись" else "🔴 Записать жесты"
                stylePillButton(this, if (service.isRecording) "#D32F2F" else "#C62828", null, 10, 13f)
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
                setOnClickListener {
                    service.toggleRecording()
                    updateMenu()
                }
            }
            val rpPlay = Button(service).apply {
                text = if (service.isPlaying) "⏸ Остановить" else "▶ Запустить"
                stylePillButton(this, if (service.isPlaying) "#D32F2F" else "#00C853", null, 10, 13f)
                layoutParams = LinearLayout.LayoutParams(0, dpToPx(40), 1f).apply { setMargins(dpToPx(4), 0, 0, 0) }
                setOnClickListener {
                    service.togglePlay()
                    updateMenu()
                }
            }
            controlRow.addView(rpStart)
            controlRow.addView(rpPlay)
        } else {
            val startBtn = Button(service).apply {
                text = if (service.isPlaying) "⏸ ОСТАНОВИТЬ СЦЕНАРИЙ" else "▶ ЗАПУСТИТЬ СЦЕНАРИЙ"
                stylePillButton(this, if (service.isPlaying) "#D32F2F" else "#00C853", null, 10, 13.5f)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40))
                setOnClickListener {
                    service.togglePlay()
                    updateMenu()
                }
            }
            controlRow.addView(startBtn)
        }
        layout.addView(controlRow)

        // Add Node & Tools Row
        val actionsRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(8))
            }
        }
        val addClickBtn = Button(service).apply {
            text = "➕ Добавить шаг"
            stylePillButton(this, uiAccentColor, null, 10, 12.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1.2f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener {
                if (appMode == AppMode.ADVANCED) {
                    showQuickAddNodeDialog()
                } else {
                    addNode(NodeType.CLICK)
                    recreateFloatingControlBar()
                }
            }
        }
        val treeBtn = Button(service).apply {
            text = "🌳 Схема"
            stylePillButton(this, "#263238", uiAccentColor, 10, 12f)
            visibility = if (showTreeBtn) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 0.75f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener { showScenarioTreeMenu() }
        }
        val clearAllBtn = Button(service).apply {
            text = "🗑"
            stylePillButton(this, "#3B1E22", null, 10, 13f)
            setTextColor(Color.parseColor("#FF8A80"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(38))
            setOnClickListener {
                if (service.nodes.isEmpty()) return@setOnClickListener
                val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Очистить все метки?")
                    .setMessage("Все метки на экране (${service.nodes.size} шт.) будут удалены.")
                    .setPositiveButton("Удалить") { _, _ ->
                        if (service.isPlaying) service.togglePlay()
                        service.nodes.clear()
                        nodeViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        nodeViews.clear()
                        nodeParams.clear()
                        swipeEndViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        swipeEndViews.clear()
                        swipeEndParams.clear()
                        textZoneStartViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        textZoneStartViews.clear()
                        textZoneStartParams.clear()
                        textZoneEndViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        textZoneEndViews.clear()
                        textZoneEndParams.clear()
                        compareZoneStartViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        compareZoneStartViews.clear()
                        compareZoneStartParams.clear()
                        compareZoneEndViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        compareZoneEndViews.clear()
                        compareZoneEndParams.clear()
                        colorCompareViews.values.forEach { try { windowManager.removeView(it) } catch (_: Exception) {} }
                        colorCompareViews.clear()
                        colorCompareParams.clear()
                        nodeCounter = 1
                        service.autoSave()
                        recreateFloatingControlBar()
                        updateMenu()
                        invalidateLines()
                    }
                    .setNegativeButton("Отмена", null)
                    .create()
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                dialog.show()
            }
        }
        actionsRow.addView(addClickBtn)
        actionsRow.addView(treeBtn)
        actionsRow.addView(clearAllBtn)
        layout.addView(actionsRow)

        // List of nodes or friendly empty state
        val scroll = ScrollView(service).apply {
            layoutParams = if (isMenuFullscreen) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(230))
            }
        }
        val list = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }

        if (service.nodes.isEmpty()) {
            val emptyBox = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(24))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#181C26"))
                    cornerRadius = dpToPx(12).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#263238"))
                }
            }
            emptyBox.addView(TextView(service).apply {
                text = "На экране пока нет меток"
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(14f)
                gravity = Gravity.CENTER
            })
            emptyBox.addView(TextView(service).apply {
                text = "Нажмите «➕ Добавить шаг» выше или кнопку [➕] на панели HUD.\n\nЧтобы настроить свайп, тайминги или поиск цвета — просто нажмите на появившийся прицел!"
                setTextColor(Color.parseColor("#90A4AE"))
                setScaledTextSize(12f)
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(8), 0, 0)
            })
            list.addView(emptyBox)
        } else {
            service.nodes.forEach { node ->
                val cardRow = LinearLayout(service).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dpToPx(10), dpToPx(8), dpToPx(8), dpToPx(8))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#1B212E"))
                        cornerRadius = dpToPx(10).toFloat()
                        val strokeCol = when (node.type) {
                            NodeType.CLICK -> "#37474F"
                            NodeType.CHECK_COLOR -> "#1E88E5"
                            NodeType.MANAGER -> "#8E24AA"
                            NodeType.MACRO -> "#FB8C00"
                        }
                        setStroke(dpToPx(1), Color.parseColor(strokeCol))
                    }
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0, 0, 0, dpToPx(6))
                    layoutParams = lp
                    setOnClickListener { showEditNodeMenu(node) }
                }

                val badgeTv = TextView(service).apply {
                    text = "#${node.id}"
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    setScaledTextSize(12.5f)
                    gravity = Gravity.CENTER
                    background = android.graphics.drawable.GradientDrawable().apply {
                        val badgeCol = when (node.type) {
                            NodeType.CLICK -> if (node.isSwipe) "#00838F" else "#C62828"
                            NodeType.CHECK_COLOR -> "#1565C0"
                            NodeType.MANAGER -> "#6A1B9A"
                            NodeType.MACRO -> "#EF6C00"
                        }
                        setColor(Color.parseColor(badgeCol))
                        cornerRadius = dpToPx(8).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34)).apply {
                        setMargins(0, 0, dpToPx(8), 0)
                    }
                }

                val textCol = LinearLayout(service).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val typeTitle = when (node.type) {
                    NodeType.CLICK -> when {
                        node.isSwipe -> "👆 Свайп"
                        node.isHold -> "✋ Удержание"
                        else -> "🎯 Клик"
                    }
                    NodeType.CHECK_COLOR -> "👁 Проверка (Триггер)"
                    NodeType.MACRO -> "⚡ Вызов скрипта"
                    NodeType.MANAGER -> "🔀 Логика (Менеджер)"
                }
                textCol.addView(TextView(service).apply {
                    text = typeTitle
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    setScaledTextSize(13f)
                })
                textCol.addView(TextView(service).apply {
                    text = getNodeSummarySubtitle(node)
                    setTextColor(Color.parseColor("#90A4AE"))
                    setScaledTextSize(11f)
                })

                val editBtn = Button(service).apply {
                    text = "⚙"
                    stylePillButton(this, "#283246", null, 8, 13f)
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34)).apply {
                        setMargins(dpToPx(4), 0, dpToPx(4), 0)
                    }
                    setOnClickListener { showEditNodeMenu(node) }
                }
                val delBtn = Button(service).apply {
                    text = "✕"
                    stylePillButton(this, "#3B1E22", null, 8, 12f)
                    setTextColor(Color.parseColor("#FF8A80"))
                    setPadding(0, 0, 0, 0)
                    layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34))
                    setOnClickListener {
                        removeNode(node.id)
                        recreateFloatingControlBar()
                    }
                }

                cardRow.addView(badgeTv)
                cardRow.addView(textCol)
                cardRow.addView(editBtn)
                cardRow.addView(delBtn)
                list.addView(cardRow)
            }
        }
        scroll.addView(list)
        layout.addView(scroll)

        menuContentContainer.addView(layout)
    }

    private fun showScenarioTreeMenu() {
        setMenuFocusable(false)
        menuContentContainer.removeAllViews()
        val rootLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            tag = "tree_menu"
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                if (isMenuFullscreen) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val headerRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(8))
        }
        headerRow.addView(TextView(service).apply {
            text = "🌳 Схема сценария и переходов"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(14.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val backTopBtn = Button(service).apply {
            text = "⬅ Назад"
            stylePillButton(this, "#283246", null, 8, 11.5f)
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            setOnClickListener { showMainMenu() }
        }
        headerRow.addView(backTopBtn)
        rootLayout.addView(headerRow)

        val scroll = ScrollView(service).apply {
            layoutParams = if (isMenuFullscreen) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(270))
            }
        }
        val listLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
        }

        val allIds = service.nodes.map { it.id }.toSet()
        if (service.nodes.isEmpty()) {
            listLayout.addView(TextView(service).apply {
                text = "Сценарий пуст. Добавьте хотя бы одну метку."
                setTextColor(Color.LTGRAY)
                setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(12))
            })
        } else {
            for ((idx, node) in service.nodes.withIndex()) {
                val card = LinearLayout(service).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    lp.setMargins(0, 0, 0, dpToPx(8))
                    layoutParams = lp
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#1B212E"))
                        cornerRadius = dpToPx(10).toFloat()
                        val borderColor = when (node.type) {
                            NodeType.CLICK -> Color.parseColor("#EF5350")
                            NodeType.CHECK_COLOR -> Color.parseColor("#42A5F5")
                            NodeType.MANAGER -> Color.parseColor("#AB47BC")
                            NodeType.MACRO -> Color.parseColor("#FFA726")
                        }
                        setStroke(dpToPx(1), borderColor)
                    }
                }

                val typeTitle = when (node.type) {
                    NodeType.CLICK -> when {
                        node.isSwipe && node.swipePathPoints.size >= 2 -> "🎯 Шаг #${node.id} — Кривой свайп (${node.swipePathPoints.size} тчк, ${node.swipeDurationMs} мс)"
                        node.isSwipe -> "🎯 Шаг #${node.id} — Свайп (${node.swipeDurationMs} мс)"
                        node.isHold -> "🎯 Шаг #${node.id} — ✋ Зажатие в фоне (${node.holdDurationMs} мс)"
                        else -> "🎯 Шаг #${node.id} — Клик (${node.clickDurationMs} мс, ×${node.repetitions})"
                    }
                    NodeType.CHECK_COLOR -> "👁 Шаг #${node.id} — Проверка условия"
                    NodeType.MANAGER -> "🔀 Шаг #${node.id} — Менеджер условий (${node.managerRoutes.size} шт.)"
                    NodeType.MACRO -> "⚡ Шаг #${node.id} — Вызов скрипта '${node.macroProfileName ?: "Не выбран"}'"
                }
                val badges = mutableListOf<String>()
                if (node.isIndependentThread) badges.add("⚡ Отдельный поток")
                if (node.skipSequentialExecution) badges.add("Функция")
                val headerStr = if (badges.isNotEmpty()) "$typeTitle  [${badges.joinToString(", ")}]" else typeTitle

                card.addView(TextView(service).apply {
                    text = headerStr
                    setTextColor(Color.WHITE)
                    setTypeface(null, Typeface.BOLD)
                    setScaledTextSize(12.5f)
                })

                val details = StringBuilder()
                if (node.type == NodeType.MANAGER) {
                    if (node.managerRoutes.isEmpty()) {
                        details.append("  • ⚠️ Условия не добавлены\n")
                    } else {
                        for ((rIdx, route) in node.managerRoutes.withIndex()) {
                            val checkWarn = if (!allIds.contains(route.checkNodeId)) " ⚠️(нет метки)" else ""
                            val goWarn = if (route.onSuccessGoToId != -1 && !allIds.contains(route.onSuccessGoToId)) " ⚠️(нет метки)" else ""
                            details.append("  • #${rIdx + 1}: Если сработала [#${route.checkNodeId}]$checkWarn ➔ Идти к [#${route.onSuccessGoToId}]$goWarn\n")
                        }
                    }
                    val fb = node.nextNodeIdOnFail?.toString() ?: "Следующий шаг"
                    details.append("  • Иначе ➔ [$fb]\n")
                } else {
                    when (node.triggerMode) {
                        -1 -> details.append("  • Проверка экрана: Нет (выполняется сразу)\n")
                        0 -> {
                            val modeDesc = when {
                                node.dynamicColorUpdate -> "Изменение цвета (${node.colorOperator}${if (node.dynamicColorLookbackMs > 0) ", память ${node.dynamicColorLookbackMs}мс" else ""})"
                                node.compareToNodeId != null -> "Сравнение цвета с меткой [#${node.compareToNodeId}]"
                                node.colorCompareX != null -> "Сравнение с точкой (${node.colorCompareX}, ${node.colorCompareY})"
                                else -> "Цвет пикселя ${node.colorOperator} (допуск ${node.colorTolerance})"
                            }
                            details.append("  • Проверка: $modeDesc\n")
                        }
                        1 -> {
                            if (node.compareZoneEnabled) {
                                details.append("  • Проверка: Сравнение 2 областей экрана (${node.colorOperator}, мин. ${node.imageThreshold.toInt()}%)\n")
                            } else {
                                val areaInfo = if (node.imageFullScreenSearch) "Весь экран" else "В радиусе ${node.searchRadius}px"
                                val clickInfo = if (node.imageClickFoundCenter) ", Клик по найденному" else ""
                                details.append("  • Проверка: Картинка (${node.colorOperator}, точность ${node.imageThreshold.toInt()}%, $areaInfo$clickInfo)\n")
                            }
                        }
                        2 -> {
                            if (node.isSmartOcr) {
                                val cmpTarget = when {
                                    node.compareZoneEnabled -> "Область 2 (Зона 2)"
                                    node.ocrCompareToNodeId != null -> "Метка [#${node.ocrCompareToNodeId}]"
                                    else -> "${node.ocrTargetValue}"
                                }
                                details.append("  • Проверка: Число OCR (${node.ocrOperator} $cmpTarget)\n")
                            } else if (node.compareZoneEnabled) {
                                details.append("  • Проверка: Текст OCR (Зона 1 ${node.colorOperator} Зона 2)\n")
                            } else {
                                details.append("  • Проверка: Текст OCR '${node.targetText ?: ""}' (${node.colorOperator})\n")
                            }
                        }
                    }

                    if (node.linkedConditionNodeId != null) {
                        val warn = if (!allIds.contains(node.linkedConditionNodeId)) " ⚠️(не найдена)" else ""
                        details.append("  • Связка: ${node.linkedConditionOperator} с шагом [#${node.linkedConditionNodeId}]$warn\n")
                    }

                    val nextSeq = service.nodes.drop(idx + 1).firstOrNull { !it.skipSequentialExecution }?.id
                    val succStr = when {
                        node.nextNodeIdOnSuccess == -1 -> "Остановить макрос (-1)"
                        node.nextNodeIdOnSuccess != null -> {
                            val warn = if (!allIds.contains(node.nextNodeIdOnSuccess)) " ⚠️(не найдена)" else ""
                            "Шаг #${node.nextNodeIdOnSuccess}$warn"
                        }
                        nextSeq != null -> "Шаг #$nextSeq (по очереди)"
                        else -> "В начало цикла ↻"
                    }
                    details.append("  • Дальше ➔ $succStr (пауза ${node.delayAfterMs} мс)\n")

                    if (node.triggerMode != -1) {
                        val failStr = when {
                            node.nextNodeIdOnFail == -1 -> "Остановить макрос (-1)"
                            node.nextNodeIdOnFail != null -> {
                                val warn = if (!allIds.contains(node.nextNodeIdOnFail)) " ⚠️(не найдена)" else ""
                                "Шаг #${node.nextNodeIdOnFail}$warn"
                            }
                            node.maxCheckCycles != null && node.maxCheckCycles!! > 0 -> {
                                if (nextSeq != null) "Ждать ${node.maxCheckCycles} раз, затем #$nextSeq" else "Ждать ${node.maxCheckCycles} раз, затем ↻"
                            }
                            else -> "Ждать на #${node.id} пока не появится ∞"
                        }
                        details.append("  • Если не найдено ➔ $failStr")
                    }
                }

                card.addView(TextView(service).apply {
                    text = details.toString().trimEnd()
                    setTextColor(Color.parseColor("#CFD8DC"))
                    setScaledTextSize(11.5f)
                    setPadding(0, dpToPx(4), 0, 0)
                })

                card.setOnClickListener { showEditNodeMenu(node) }
                listLayout.addView(card)
            }
        }

        scroll.addView(listLayout)
        rootLayout.addView(scroll)
        menuContentContainer.addView(rootLayout)
    }

    private fun showHelpMenu() {
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            tag = "help_menu"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                if (isMenuFullscreen) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val headerRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(8))
        }
        headerRow.addView(TextView(service).apply {
            text = "📖 Краткая шпаргалка"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(15f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val backBtnTop = Button(service).apply {
            text = "⬅ Назад"
            stylePillButton(this, "#283246", null, 8, 12f)
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            setOnClickListener { showMainMenu() }
        }
        headerRow.addView(backBtnTop)
        layout.addView(headerRow)

        val scroll = ScrollView(service).apply {
            layoutParams = if (isMenuFullscreen) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(280))
            }
        }
        val textLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(2), 0, dpToPx(2), dpToPx(12))
        }

        fun createHelpCard(cardTitle: String, cardBody: String, accentHex: String) {
            val card = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#181C26"))
                    cornerRadius = dpToPx(10).toFloat()
                    setStroke(dpToPx(1), Color.parseColor(accentHex))
                }
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, dpToPx(8))
                layoutParams = params
            }
            val titleView = TextView(service).apply {
                text = cardTitle
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(13.5f)
                setPadding(0, 0, 0, dpToPx(4))
            }
            val bodyView = TextView(service).apply {
                text = cardBody
                setTextColor(Color.parseColor("#CFD8DC"))
                setScaledTextSize(12f)
                setLineSpacing(0f, 1.2f)
            }
            card.addView(titleView)
            card.addView(bodyView)
            textLayout.addView(card)
        }

        createHelpCard(
            "🎯 1. Клики, Свайпы и Удержание",
            "• Нажмите [➕] для добавления метки. Нажмите на сам прицел, чтобы открыть его настройки.\n" +
            "• Внутри настроек метки выберите действие: Клик, Свайп (прямой или нарисованный от руки) либо Удержание в фоне (полезно для джойстика ходьбы в играх).",
            "#2962FF"
        )
        createHelpCard(
            "👁 2. Проверка цвета, картинки или текста (OCR)",
            "• В режиме «Умный» любая метка может проверять экран перед действием.\n" +
            "• Цвет пикселя: прицел оснащён лупой в углу, чтобы палец не загораживал точку.\n" +
            "• Картинка (Фрагмент): выделите рамкой иконку и нажмите «Захватить». Кликер сам найдёт её на экране!\n" +
            "• Текст (OCR): считывает слова или сравнивает числа (например, цена < 5000).",
            "#00C853"
        )
        createHelpCard(
            "🎥 3. Запись жестов вживую",
            "• Во вкладке «Режим» выберите «Запись жестов».\n" +
            "• Нажмите [🔴] на панели и просто покажите свои нажатия и свайпы пальцем — кликер сам расставит метки и запомнит паузы!",
            "#FF5252"
        )
        createHelpCard(
            "📁 4. Профили и Быстрая панель (⚡)",
            "• Во вкладке «Профили» сохраняйте свои сценарии, делитесь ими в файл .json или делайте полный бэкап всех макросов.\n" +
            "• Нажмите [⚡] на плавающей панели HUD для мгновенного переключения между любимыми профилями.",
            "#FFD54F"
        )

        scroll.addView(textLayout)
        layout.addView(scroll)
        menuContentContainer.addView(layout)
    }

    private fun showModeMenu() {
        updateTabSelection(2)
        setMenuFocusable(false)
        menuContentContainer.removeAllViews()
        val scroll = ScrollView(service).apply {
            layoutParams = getMenuContentLayoutParams(280)
            tag = "mode_menu"
        }
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(8))
        }

        layout.addView(TextView(service).apply {
            text = "Выберите подходящий режим:"
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(12.5f)
            setPadding(0, 0, 0, dpToPx(8))
        })

        data class ModeCardInfo(val mode: AppMode, val title: String, val subtitle: String, val accent: String)
        val modes = listOf(
            ModeCardInfo(
                AppMode.SEQUENTIAL,
                "🎯 Простой режим (Цепочка)",
                "Идеально для новичков. Метки просто нажимают или свайпают по очереди (1 ➔ 2 ➔ 3). Никаких сложных настроек!",
                "#00C853"
            ),
            ModeCardInfo(
                AppMode.ADVANCED,
                "🧠 Умный режим (Триггеры и Логика)",
                "Полный доступ: проверка цвета с лупой, поиск фрагментов картинок, чтение текста/чисел (OCR), сравнение областей, ветвление и подпрограммы.",
                uiAccentColor
            ),
            ModeCardInfo(
                AppMode.RECORD,
                "🎥 Режим записи жестов",
                "На панели появляется кнопка [🔴]. Нажмите её и выполните действия на экране вручную — кликер повторит всё за вами.",
                "#FF5252"
            )
        )

        modes.forEach { info ->
            val isSelected = appMode == info.mode
            val card = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor(if (isSelected) "#1E293B" else "#161B24"))
                    cornerRadius = dpToPx(12).toFloat()
                    setStroke(dpToPx(if (isSelected) 2 else 1), Color.parseColor(if (isSelected) info.accent else "#263238"))
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 0, dpToPx(8))
                layoutParams = lp
                setOnClickListener {
                    appMode = info.mode
                    service.getSharedPreferences("AutoClickerSettings", Context.MODE_PRIVATE)
                        .edit().putString("AppMode", info.mode.name).apply()
                    showModeMenu()
                    recreateFloatingControlBar()
                    invalidateLines()
                }
            }
            val topRow = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            topRow.addView(TextView(service).apply {
                text = info.title
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(13.5f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (isSelected) {
                topRow.addView(TextView(service).apply {
                    text = "✔ Активен"
                    setTextColor(Color.parseColor(info.accent))
                    setTypeface(null, Typeface.BOLD)
                    setScaledTextSize(11.5f)
                })
            }
            card.addView(topRow)
            card.addView(TextView(service).apply {
                text = info.subtitle
                setTextColor(Color.parseColor("#90A4AE"))
                setScaledTextSize(11.5f)
                setPadding(0, dpToPx(4), 0, 0)
            })
            layout.addView(card)
        }

        scroll.addView(layout)
        menuContentContainer.addView(scroll)
    }

    private fun showSettingsMenu() {
        updateTabSelection(3)
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val scroll = ScrollView(service).apply {
            layoutParams = getMenuContentLayoutParams(290)
            tag = "settings_menu"
        }
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(8))
        }

        fun createGroupCard(titleStr: String): LinearLayout {
            val box = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#181C26"))
                    cornerRadius = dpToPx(12).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#263238"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(10))
                }
            }
            box.addView(TextView(service).apply {
                text = titleStr
                setTextColor(try { Color.parseColor(uiAccentColor) } catch (_: Exception) { Color.parseColor("#00C853") })
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(13f)
                setPadding(0, 0, 0, dpToPx(6))
            })
            layout.addView(box)
            return box
        }

        // Card 1: Внешний вид и кнопки HUD
        val uiCard = createGroupCard("🎨 Внешний вид и тема интерфейса")

        // Color Theme Presets
        uiCard.addView(TextView(service).apply {
            text = "Акцентный цвет (HUD и меню):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            setPadding(0, 0, 0, dpToPx(4))
        })
        val themePresets = listOf(
            "🟢 Зелёный" to "#00C853",
            "🔵 Синий" to "#2962FF",
            "💠 Бирюза" to "#00B8D4",
            "🟣 Фиолет." to "#AA00FF",
            "🟠 Оранж." to "#FF6D00",
            "🔴 Красный" to "#FF1744"
        )
        val themeScroll = HorizontalScrollView(service).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(6))
        }
        val themeRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        for ((label, hex) in themePresets) {
            val isCurrent = uiAccentColor.equals(hex, ignoreCase = true)
            themeRow.addView(Button(service).apply {
                text = if (isCurrent) "✓ $label" else label
                stylePillButton(this, if (isCurrent) hex else "#23293A", hex, 8, 11f)
                setPadding(dpToPx(10), 0, dpToPx(10), 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(32)).apply {
                    setMargins(0, 0, dpToPx(5), 0)
                }
                setOnClickListener {
                    uiAccentColor = hex
                    saveUISettings()
                    recreateFloatingControlBar()
                    recreateModMenu("settings_menu")
                }
            })
        }
        themeScroll.addView(themeRow)
        uiCard.addView(themeScroll)

        val alphaLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(4))
        }
        alphaLayout.addView(TextView(service).apply {
            text = "Прозрачность окон:"
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        alphaLayout.addView(Button(service).apply {
            text = "−"
            stylePillButton(this, "#283246", null, 8, 14f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
            setOnClickListener { if (uiAlpha > 0.3f) { uiAlpha -= 0.1f; saveUISettings(); applyUISettings(); showSettingsMenu() } }
        })
        alphaLayout.addView(TextView(service).apply {
            text = "${(uiAlpha * 100).toInt()}%"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), WindowManager.LayoutParams.WRAP_CONTENT)
        })
        alphaLayout.addView(Button(service).apply {
            text = "+"
            stylePillButton(this, "#283246", null, 8, 14f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
            setOnClickListener { if (uiAlpha < 1.0f) { uiAlpha = (uiAlpha + 0.1f).coerceAtMost(1.0f); saveUISettings(); applyUISettings(); showSettingsMenu() } }
        })
        uiCard.addView(alphaLayout)

        val scaleLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(6))
        }
        scaleLayout.addView(TextView(service).apply {
            text = "Размер элементов:"
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        scaleLayout.addView(Button(service).apply {
            text = "−"
            stylePillButton(this, "#283246", null, 8, 14f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
            setOnClickListener { if (uiScale > 0.6f) { uiScale -= 0.1f; saveUISettings(); applyUIScaleChange() } }
        })
        scaleLayout.addView(TextView(service).apply {
            text = "${(uiScale * 100).toInt()}%"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), WindowManager.LayoutParams.WRAP_CONTENT)
        })
        scaleLayout.addView(Button(service).apply {
            text = "+"
            stylePillButton(this, "#283246", null, 8, 14f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
            setOnClickListener { if (uiScale < 1.8f) { uiScale += 0.1f; saveUISettings(); applyUIScaleChange() } }
        })
        uiCard.addView(scaleLayout)

        val currentAvgNodeScale = service.nodes.firstOrNull()?.sizeScale ?: 1.0f
        val nodeScaleLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(6))
        }
        nodeScaleLayout.addView(TextView(service).apply {
            text = "Размер всех меток:"
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val nodeScaleValTv = TextView(service).apply {
            text = "${(currentAvgNodeScale * 100).toInt()}%"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        nodeScaleLayout.addView(Button(service).apply {
            text = "−"
            stylePillButton(this, "#283246", null, 8, 14f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
            setOnClickListener {
                val base = service.nodes.firstOrNull()?.sizeScale ?: 1.0f
                val next = (Math.round((base - 0.1f) * 10f) / 10f).coerceIn(0.5f, 2.5f)
                service.nodes.forEach { n ->
                    n.sizeScale = next
                    nodeViews[n.id]?.invalidate()
                    swipeEndViews[n.id]?.invalidate()
                    colorCompareViews[n.id]?.invalidate()
                }
                nodeScaleValTv.text = "${(next * 100).toInt()}%"
                invalidateLines()
                service.autoSave()
            }
        })
        nodeScaleLayout.addView(nodeScaleValTv)
        nodeScaleLayout.addView(Button(service).apply {
            text = "+"
            stylePillButton(this, "#283246", null, 8, 14f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
            setOnClickListener {
                val base = service.nodes.firstOrNull()?.sizeScale ?: 1.0f
                val next = (Math.round((base + 0.1f) * 10f) / 10f).coerceIn(0.5f, 2.5f)
                service.nodes.forEach { n ->
                    n.sizeScale = next
                    nodeViews[n.id]?.invalidate()
                    swipeEndViews[n.id]?.invalidate()
                    colorCompareViews[n.id]?.invalidate()
                }
                nodeScaleValTv.text = "${(next * 100).toInt()}%"
                invalidateLines()
                service.autoSave()
            }
        })
        uiCard.addView(nodeScaleLayout)

        uiCard.addView(android.widget.CheckBox(service).apply {
            text = "Кнопка «Скрыть метки» (👁) на панели"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = showEyeBtn
            setOnCheckedChangeListener { _, c -> showEyeBtn = c; saveUISettings(); recreateFloatingControlBar() }
        })
        uiCard.addView(android.widget.CheckBox(service).apply {
            text = "Кнопка «Линии маршрутов» (🕸) на панели"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = showLinesBtn
            setOnCheckedChangeListener { _, c -> showLinesBtn = c; saveUISettings(); recreateFloatingControlBar() }
        })
        uiCard.addView(android.widget.CheckBox(service).apply {
            text = "Кнопка «Быстрые профили» (⚡) на панели"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = showHotbarBtn
            setOnCheckedChangeListener { _, c -> showHotbarBtn = c; saveUISettings(); recreateFloatingControlBar() }
        })
        uiCard.addView(android.widget.CheckBox(service).apply {
            text = "Кнопка счётчика меток / Древа сценария (1🎯) на панели и в меню"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = showTreeBtn
            setOnCheckedChangeListener { _, c ->
                showTreeBtn = c
                saveUISettings()
                recreateFloatingControlBar()
            }
        })

        // Card 2: Ограничения работы и циклы
        val execCard = createGroupCard("⏱ Таймер работы и циклы")

        val maxCyclesRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        maxCyclesRow.addView(TextView(service).apply {
            text = "Остановить после циклов (0 = ∞):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val maxCyclesEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (service.maxCycles != null && service.maxCycles!! > 0) service.maxCycles.toString() else "0")
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(70), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        maxCyclesRow.addView(maxCyclesEdit)
        execCard.addView(maxCyclesRow)

        val cycleDelayRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        cycleDelayRow.addView(TextView(service).apply {
            text = "Пауза между кругами (мс):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val cycleDelayEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(service.cycleEndDelayMs.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(70), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        cycleDelayRow.addView(cycleDelayEdit)
        execCard.addView(cycleDelayRow)

        val maxTimeRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        maxTimeRow.addView(TextView(service).apply {
            text = "Таймер авто-стопа (мин, 0 = ∞):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val maxTimeEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (service.maxPlayDurationMs != null && service.maxPlayDurationMs!! > 0) (service.maxPlayDurationMs!! / 60000L).toString() else "0")
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(70), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        maxTimeRow.addView(maxTimeEdit)
        execCard.addView(maxTimeRow)

        // Card 3: Системные функции
        val sysCard = createGroupCard("⚡ Система и отладка")

        sysCard.addView(android.widget.CheckBox(service).apply {
            text = "☕ Кофеин (Не гасить экран во время работы)"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = isCaffeineEnabled
            setOnCheckedChangeListener { _, c ->
                isCaffeineEnabled = c
                saveUISettings()
                updateFloatingControlBarCaffeine()
            }
        })
        sysCard.addView(android.widget.CheckBox(service).apply {
            text = "🐞 Показать окно Дебаг-лога на экране"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = isDebugWindowVisible
            setOnCheckedChangeListener { _, _ ->
                toggleDebugWindow()
                saveUISettings()
            }
        })
        sysCard.addView(android.widget.CheckBox(service).apply {
            text = "🖐 Синхронный мультитач (несколько пальцев)"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            isChecked = service.enableMultitouch
            setOnCheckedChangeListener { _, isChecked ->
                service.enableMultitouch = isChecked
                saveUISettings()
            }
        })
        sysCard.addView(android.widget.CheckBox(service).apply {
            text = "🚀 Экстремальная скорость (снятие лимитов задержки)"
            setTextColor(Color.parseColor("#FF8A80"))
            setScaledTextSize(12f)
            isChecked = service.allowExtremeSpeed
            setOnCheckedChangeListener { _, isChecked ->
                service.allowExtremeSpeed = isChecked
                saveUISettings()
            }
        })

        val saveBtn = Button(service).apply {
            text = "✔ Применить настройки"
            stylePillButton(this, uiAccentColor, null, 10, 13.5f)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40)).apply {
                setMargins(0, dpToPx(4), 0, 0)
            }
            setOnClickListener {
                val cyclesStr = maxCyclesEdit.text.toString().toIntOrNull() ?: 0
                service.maxCycles = if (cyclesStr > 0) cyclesStr else null
                service.cycleEndDelayMs = cycleDelayEdit.text.toString().toLongOrNull() ?: 0L
                val minsStr = maxTimeEdit.text.toString().toLongOrNull() ?: 0L
                service.maxPlayDurationMs = if (minsStr > 0) minsStr * 60000L else null
                saveUISettings()
                Toast.makeText(service, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                showMainMenu()
            }
        }
        layout.addView(saveBtn)

        scroll.addView(layout)
        menuContentContainer.addView(scroll)
    }

    fun applyUISettings() {
        floatingControlBar?.alpha = uiAlpha
        modMenu?.alpha = uiAlpha
    }

    private fun showProfileMenu(isSaving: Boolean) {
        updateTabSelection(1)
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val scrollRoot = ScrollView(service).apply {
            layoutParams = getMenuContentLayoutParams(290)
            tag = if (isSaving) "profile_menu_save" else "profile_menu_load"
        }
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(8))
        }

        // Save current scenario card
        val saveCard = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#181C26"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#263238"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(10))
            }
        }
        saveCard.addView(TextView(service).apply {
            text = "💾 Сохранить текущий сценарий (${service.nodes.size} шаг.)"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(13f)
        })
        val saveInputRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(4), 0, 0)
        }
        val nameEdit = EditText(service).apply {
            hint = "Название (напр. Фарм босса)"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(6), 0)
            }
        }
        val saveNowBtn = Button(service).apply {
            text = "Сохранить"
            stylePillButton(this, "#00C853", null, 8, 12f)
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, dpToPx(36))
            setOnClickListener {
                val name = nameEdit.text.toString().trim().takeIf { it.isNotEmpty() } ?: "Сценарий ${service.getSavedProfiles().size + 1}"
                service.saveProfile(name)
                showProfileMenu(false)
            }
        }
        saveInputRow.addView(nameEdit)
        saveInputRow.addView(saveNowBtn)
        saveCard.addView(saveInputRow)
        layout.addView(saveCard)

        // Saved profiles list
        val profiles = service.getSavedProfiles()
        layout.addView(TextView(service).apply {
            text = if (profiles.isEmpty()) "Сохранённых профилей пока нет" else "📂 Ваши сохранённые профили (${profiles.size}):"
            setTextColor(Color.parseColor("#90A4AE"))
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12f)
            setPadding(dpToPx(2), 0, 0, dpToPx(6))
        })

        profiles.forEach { profileName ->
            val pCard = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(10), dpToPx(6), dpToPx(6), dpToPx(6))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#1B212E"))
                    cornerRadius = dpToPx(10).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#2E3B4E"))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dpToPx(6))
                }
            }
            val titleTv = TextView(service).apply {
                text = profileName
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(12.5f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    nameEdit.setText(profileName)
                }
            }
            val loadBtn = Button(service).apply {
                text = "Загр."
                stylePillButton(this, "#1E88E5", null, 8, 11.5f)
                setPadding(dpToPx(8), 0, dpToPx(8), 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(32)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                }
                setOnClickListener {
                    service.loadProfile(profileName, append = false)
                    recreateFloatingControlBar()
                    showMainMenu()
                }
            }
            val appendBtn = Button(service).apply {
                text = "+К текущ."
                stylePillButton(this, "#283246", null, 8, 11f)
                setPadding(dpToPx(6), 0, dpToPx(6), 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(32)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                }
                setOnClickListener {
                    service.loadProfile(profileName, append = true)
                    recreateFloatingControlBar()
                    showMainMenu()
                }
            }
            val deleteBtn = Button(service).apply {
                text = "✕"
                stylePillButton(this, "#3B1E22", null, 8, 11.5f)
                setTextColor(Color.parseColor("#FF8A80"))
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
                setOnClickListener {
                    service.deleteProfile(profileName)
                    showProfileMenu(false)
                }
            }
            pCard.addView(titleTv)
            pCard.addView(loadBtn)
            pCard.addView(appendBtn)
            pCard.addView(deleteBtn)
            layout.addView(pCard)
        }

        // Export / Import & Backup section
        val ioCard = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(10))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#181C26"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#263238"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(6), 0, 0)
            }
        }
        ioCard.addView(TextView(service).apply {
            text = "🔄 Обмен файлами и резервная копия"
            setTextColor(Color.parseColor("#64B5F6"))
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12.5f)
            setPadding(0, 0, 0, dpToPx(6))
        })

        val clipRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }
        clipRow.addView(Button(service).apply {
            text = "📋 Копировать"
            stylePillButton(this, "#263238", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener { service.exportProfileToClipboard() }
        })
        clipRow.addView(Button(service).apply {
            text = "📥 Вставить"
            stylePillButton(this, "#263238", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(dpToPx(4), 0, 0, 0) }
            setOnClickListener {
                service.importProfileFromClipboard()
                recreateFloatingControlBar()
                showMainMenu()
            }
        })
        ioCard.addView(clipRow)

        val fileRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }
        fileRow.addView(Button(service).apply {
            text = "📤 В файл .json"
            stylePillButton(this, "#263238", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener {
                val name = nameEdit.text.toString().takeIf { it.isNotBlank() } ?: "AutoClickerProfile"
                val obj = org.json.JSONObject()
                val (screenWidth, screenHeight) = getScreenDimensions()
                obj.put("screenWidth", screenWidth)
                obj.put("screenHeight", screenHeight)
                val arr = org.json.JSONArray()
                for (node in service.nodes) arr.put(node.toJson())
                obj.put("nodes", arr)
                MainActivity.pendingExportData = obj.toString()
                val intent = android.content.Intent(service, MainActivity::class.java).apply {
                    action = "ACTION_EXPORT_PROFILE"
                    putExtra("profile_name", name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                modMenu?.visibility = View.GONE
            }
        })
        fileRow.addView(Button(service).apply {
            text = "📂 Загрузить файл"
            stylePillButton(this, "#263238", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(dpToPx(4), 0, 0, 0) }
            setOnClickListener {
                val intent = android.content.Intent(service, MainActivity::class.java).apply {
                    action = "ACTION_IMPORT_PROFILE"
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                modMenu?.visibility = View.GONE
            }
        })
        ioCard.addView(fileRow)

        val shareAndBackupRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        shareAndBackupRow.addView(Button(service).apply {
            text = "✈ Поделиться"
            stylePillButton(this, "#1E88E5", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener {
                val name = nameEdit.text.toString().takeIf { it.isNotBlank() } ?: "AutoClickerProfile"
                val obj = org.json.JSONObject()
                val (screenWidth, screenHeight) = getScreenDimensions()
                obj.put("screenWidth", screenWidth)
                obj.put("screenHeight", screenHeight)
                val arr = org.json.JSONArray()
                for (node in service.nodes) arr.put(node.toJson())
                obj.put("nodes", arr)
                MainActivity.pendingExportData = obj.toString()
                val intent = android.content.Intent(service, MainActivity::class.java).apply {
                    action = "ACTION_SHARE_PROFILE"
                    putExtra("profile_name", name)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                modMenu?.visibility = View.GONE
            }
        })
        shareAndBackupRow.addView(Button(service).apply {
            text = "📦 Бэкап всех"
            stylePillButton(this, "#6A1B9A", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(dpToPx(4), 0, 0, 0) }
            setOnClickListener {
                MainActivity.pendingExportData = service.exportAllProfilesJson()
                val intent = android.content.Intent(service, MainActivity::class.java).apply {
                    action = "ACTION_EXPORT_PROFILE"
                    putExtra("profile_name", "UpwellClick_AllProfiles_Backup")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                modMenu?.visibility = View.GONE
            }
        })
        ioCard.addView(shareAndBackupRow)
        layout.addView(ioCard)

        scrollRoot.addView(layout)
        menuContentContainer.addView(scrollRoot)
    }

    private fun createColorRow(title: String, initialColor: Int, onColorSelected: (Int) -> Unit): View {
        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(4), 0, dpToPx(6))
        }
        layout.addView(TextView(service).apply {
            text = title
            setTextColor(Color.parseColor("#ECEFF1"))
            setScaledTextSize(12f)
            setPadding(0, 0, 0, dpToPx(4))
        })

        val scroll = android.widget.HorizontalScrollView(service).apply {
            isHorizontalScrollBarEnabled = false
        }
        val colorsLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val colors = intArrayOf(
            Color.RED,
            Color.parseColor("#FF9800"),
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN,
            Color.BLUE,
            Color.parseColor("#9C27B0"),
            Color.MAGENTA,
            Color.WHITE,
            Color.BLACK
        )

        var selectedColor = initialColor
        val swatchButtons = mutableListOf<Pair<Int, View>>()

        fun updateSwatchBorders() {
            for ((c, v) in swatchButtons) {
                val isSel = (c == selectedColor)
                v.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(c)
                    setStroke(
                        dpToPx(if (isSel) 3 else 1),
                        if (isSel) (if (c == Color.WHITE || c == Color.YELLOW) Color.parseColor("#00E676") else Color.WHITE) else Color.parseColor("#455A64")
                    )
                }
            }
        }

        for (c in colors) {
            val btn = View(service).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30)).apply {
                    setMargins(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
                }
                setOnClickListener {
                    selectedColor = c
                    updateSwatchBorders()
                    onColorSelected(c)
                }
            }
            swatchButtons.add(c to btn)
            colorsLayout.addView(btn)
        }
        updateSwatchBorders()
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
            disableOverlaySelectionToolbar(this)
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

            if (n.linkedConditionNodeId == id1) n.linkedConditionNodeId = id2
            else if (n.linkedConditionNodeId == id2) n.linkedConditionNodeId = id1

            if (n.managerRoutes.isNotEmpty()) {
                n.managerRoutes.forEach { r ->
                    if (r.checkNodeId == id1) r.checkNodeId = id2
                    else if (r.checkNodeId == id2) r.checkNodeId = id1
                    if (r.onSuccessGoToId == id1) r.onSuccessGoToId = id2
                    else if (r.onSuccessGoToId == id2) r.onSuccessGoToId = id1
                }
            }
            
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
        
        try { compareZoneStartViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        compareZoneStartViews.clear()
        compareZoneStartParams.clear()
        
        try { compareZoneEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        compareZoneEndViews.clear()
        compareZoneEndParams.clear()
        
        try { colorCompareViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        colorCompareViews.clear()
        colorCompareParams.clear()
        
        createViewsForNodes(service.nodes)
        updateMenu()
        invalidateLines()
        service.autoSave()
    }

    fun reopenNodeEditMenuFromExternal(node: TargetNode) {
        if (modMenu == null) {
            showModMenu()
        } else {
            modMenu?.visibility = View.VISIBLE
        }
        showEditNodeMenu(node)
    }

    private fun showEditNodeMenu(node: TargetNode) {
        currentEditingNode = node
        setMenuFocusable(true)
        menuContentContainer.removeAllViews()
        val layout = ScrollView(service).apply {
            tag = "edit_menu"
            layoutParams = getMenuContentLayoutParams(310)
        }
        val content = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(8))
        }

        val headerCard = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#181C26"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#2E3B4E"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }

        val headerTitleLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val backQuickBtn = Button(service).apply {
            text = "⬅ Список"
            stylePillButton(this, "#283246", null, 8, 11.5f)
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, dpToPx(30))
            setOnClickListener {
                service.autoSave()
                showMainMenu()
            }
        }

        val tvNodeTitle = TextView(service).apply {
            text = "Шаг #${node.id}"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnLeft = Button(service).apply {
            text = "▲"
            stylePillButton(this, "#23293A", null, 8, 11f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30)).apply { setMargins(0, 0, dpToPx(3), 0) }
            isEnabled = node.id > 1
            alpha = if (isEnabled) 1f else 0.35f
            setOnClickListener {
                swapNodes(node.id, node.id - 1)
                showEditNodeMenu(node)
            }
        }

        val btnRight = Button(service).apply {
            text = "▼"
            stylePillButton(this, "#23293A", null, 8, 11f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(30), dpToPx(30))
            val maxId = service.nodes.maxOfOrNull { it.id } ?: 1
            isEnabled = node.id < maxId
            alpha = if (isEnabled) 1f else 0.35f
            setOnClickListener {
                swapNodes(node.id, node.id + 1)
                showEditNodeMenu(node)
            }
        }

        headerTitleLayout.addView(backQuickBtn)
        headerTitleLayout.addView(tvNodeTitle)
        headerTitleLayout.addView(btnLeft)
        headerTitleLayout.addView(btnRight)
        headerCard.addView(headerTitleLayout)

        val typeSpinner = android.widget.Spinner(service).apply {
            val items = arrayOf(
                "🎯 Действие (Клик / Свайп / Удержание)",
                "👁 Только условие (Ждать цвет / картинку / текст)",
                "⚡ Вызов подпрограммы (Другой профиль)",
                "🔀 Менеджер условий (Развилка)"
            )
            this.adapter = createThemedSpinnerAdapter(items)
            setSelection(when (node.type) {
                NodeType.CLICK -> 0
                NodeType.CHECK_COLOR -> 1
                NodeType.MACRO -> 2
                NodeType.MANAGER -> 3
            })
            visibility = if (appMode == AppMode.ADVANCED) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(6), 0, 0)
            }
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                    val newType = when (pos) {
                        0 -> NodeType.CLICK
                        1 -> NodeType.CHECK_COLOR
                        2 -> NodeType.MACRO
                        3 -> NodeType.MANAGER
                        else -> NodeType.CLICK
                    }
                    if (node.type != newType) {
                        node.type = newType
                        if (newType == NodeType.CHECK_COLOR && node.triggerMode == -1) {
                            node.triggerMode = 0
                        }
                        node.crosshairColor = if (node.type == NodeType.CLICK) Color.RED else if (node.type == NodeType.MANAGER) Color.parseColor("#9C27B0") else Color.BLUE
                        nodeViews[node.id]?.invalidate()
                        showEditNodeMenu(node)
                    }
                }
                override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
            }
        }
        headerCard.addView(typeSpinner)
        content.addView(headerCard)

        fun addSection(title: String, hasChanges: Boolean, startExpanded: Boolean = false, buildContent: (LinearLayout) -> Unit): LinearLayout {
            val sectionLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                val marginParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                marginParams.setMargins(0, dpToPx(4), 0, dpToPx(4))
                layoutParams = marginParams

                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#181C26"))
                    cornerRadius = dpToPx(12).toFloat()
                    setStroke(dpToPx(1), Color.parseColor(if (hasChanges) "#2E4C6D" else "#263238"))
                }
                clipToOutline = true
            }

            val secHeader = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(12), dpToPx(11), dpToPx(12), dpToPx(11))
            }

            val titleText = TextView(service).apply {
                text = title
                setTextColor(Color.WHITE)
                setScaledTextSize(13f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            }

            val checkIcon = TextView(service).apply {
                text = " ● "
                setTextColor(Color.parseColor("#00E676"))
                setScaledTextSize(11f)
                visibility = if (hasChanges) View.VISIBLE else View.GONE
            }

            val icon = TextView(service).apply {
                text = if (startExpanded) "︿" else "﹀"
                setTextColor(Color.parseColor("#90A4AE"))
                setScaledTextSize(14f)
            }

            secHeader.addView(titleText)
            secHeader.addView(checkIcon)
            secHeader.addView(icon)

            val bodyLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(12), 0, dpToPx(12), dpToPx(12))
                visibility = if (startExpanded) View.VISIBLE else View.GONE
            }

            buildContent(bodyLayout)

            secHeader.setOnClickListener {
                if (bodyLayout.visibility == View.VISIBLE) {
                    bodyLayout.visibility = View.GONE
                    icon.text = "﹀"
                } else {
                    bodyLayout.visibility = View.VISIBLE
                    icon.text = "︿"
                }
            }

            sectionLayout.addView(secHeader)
            sectionLayout.addView(bodyLayout)
            content.addView(sectionLayout)
            return sectionLayout
        }

        // --- SECTION: Тайминги (Открыта сразу для обычных кликов) ---
        val (clickDurRow, getClickDurRowMs) = createTimeInputRow("Длительность нажатия:", node.clickDurationMs)
        val (delayRow, getDelayRowMs) = createTimeInputRow("Пауза после шага:", node.delayAfterMs)

        val repRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        repRow.addView(TextView(service).apply {
            text = "Повторить подряд (раз):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val repetitionsEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.repetitions.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        repRow.addView(repetitionsEdit)

        val applyTimingsBtn = Button(service).apply {
            text = "Применить эти тайминги ко всем меткам"
            stylePillButton(this, "#283246", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(34)).apply {
                setMargins(0, dpToPx(6), 0, 0)
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
                        Toast.makeText(service, "Применено ко всем меткам", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Отмена", null)
                    .create()
                dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
                dialog.show()
            }
        }

        val hasTimingChanges = node.clickDurationMs != 30L || node.delayAfterMs != 300L || node.repetitions != 1
        val timingsSection = addSection(
            "⏱ 1. Тайминги и повторы",
            hasTimingChanges,
            startExpanded = (node.type == NodeType.CLICK && !node.isSwipe && !node.isHold)
        ) { body ->
            body.addView(clickDurRow)
            body.addView(delayRow)
            body.addView(repRow)
            body.addView(applyTimingsBtn)
        }

        // --- SECTION: Анти-Детект ---
        val (randomDelayRow, getRandomDelayRowMs) = createTimeInputRow("Случайный разброс паузы (+до):", node.randomizeDelayMs)
        
        val randRadiusRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(4), 0, dpToPx(4))
        }
        val tvRandomRadius = TextView(service).apply {
            text = "Разброс точки клика (px):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        val randomRadiusEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.randomizeRadius.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(64), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        randRadiusRow.addView(tvRandomRadius)
        randRadiusRow.addView(randomRadiusEdit)
        
        val applyAntiDetectBtn = Button(service).apply {
            text = "Применить анти-детект ко всем меткам"
            stylePillButton(this, "#283246", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(34)).apply {
                setMargins(0, dpToPx(6), 0, 0)
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
                        Toast.makeText(service, "Применено ко всем меткам", Toast.LENGTH_SHORT).show()
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
            managerSection = addSection("🔀 Настройки Менеджера (Развилка)", true, startExpanded = true) { body ->
                val desc = TextView(service).apply {
                    text = "Менеджер по очереди проверяет указанные шаги-условия. Как только условие совпало — переходит к целевому шагу."
                    setTextColor(Color.parseColor("#B0BEC5"))
                    setScaledTextSize(11.5f)
                    setPadding(0, 0, 0, dpToPx(8))
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
                            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                            background = android.graphics.drawable.GradientDrawable().apply {
                                setColor(Color.parseColor("#222836"))
                                cornerRadius = dpToPx(8).toFloat()
                            }
                            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 0, 0, dpToPx(4))
                            }
                        }
                        
                        val txt1 = TextView(service).apply { text = "Если шаг #"; setTextColor(Color.WHITE); setScaledTextSize(12f) }
                        val checkEdit = EditText(service).apply {
                            inputType = InputType.TYPE_CLASS_NUMBER
                            setText(if(route.checkNodeId != -1) route.checkNodeId.toString() else "")
                            setTextColor(Color.WHITE)
                            setScaledTextSize(12f)
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(dpToPx(44), WindowManager.LayoutParams.WRAP_CONTENT)
                            addTextChangedListener(object: android.text.TextWatcher {
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    s?.toString()?.toIntOrNull()?.let { route.checkNodeId = it }
                                }
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            })
                        }
                        val txt2 = TextView(service).apply { text = " ➔ идти к #"; setTextColor(Color.parseColor("#69F0AE")); setScaledTextSize(12f) }
                        val goEdit = EditText(service).apply {
                            inputType = InputType.TYPE_CLASS_NUMBER
                            setText(if(route.onSuccessGoToId != -1) route.onSuccessGoToId.toString() else "")
                            setTextColor(Color.WHITE)
                            setScaledTextSize(12f)
                            gravity = Gravity.CENTER
                            layoutParams = LinearLayout.LayoutParams(dpToPx(44), WindowManager.LayoutParams.WRAP_CONTENT)
                            addTextChangedListener(object: android.text.TextWatcher {
                                override fun afterTextChanged(s: android.text.Editable?) {
                                    s?.toString()?.toIntOrNull()?.let { route.onSuccessGoToId = it }
                                }
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            })
                        }
                        val spacer = View(service).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }
                        val delBtn = Button(service).apply {
                            text = "✕"
                            stylePillButton(this, "#3A1D24", null, 6, 11f)
                            setTextColor(Color.parseColor("#FF8A80"))
                            setPadding(0, 0, 0, 0)
                            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
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
                        row.addView(spacer)
                        row.addView(delBtn)
                        listContainer.addView(row)
                    }
                    
                    val addBtn = Button(service).apply {
                        text = "＋ Добавить правило проверки"
                        stylePillButton(this, "#1E3A2F", "#2E7D32", 8, 12f)
                        setTextColor(Color.parseColor("#69F0AE"))
                        layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(34)).apply {
                            setMargins(0, dpToPx(4), 0, 0)
                        }
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
                    setPadding(0, dpToPx(8), 0, 0)
                    gravity = Gravity.CENTER_VERTICAL
                }
                fallbackLayout.addView(TextView(service).apply {
                    text = "Если ничего не совпало, идти к #:"
                    setTextColor(Color.WHITE)
                    setScaledTextSize(12f)
                    layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                })
                val fallbackEdit = EditText(service).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText(node.nextNodeIdOnFail?.toString() ?: "")
                    hint = "Стоп"
                    setHintTextColor(Color.parseColor("#78909C"))
                    setTextColor(Color.WHITE)
                    setScaledTextSize(12f)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dpToPx(64), WindowManager.LayoutParams.WRAP_CONTENT)
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
        
        val macroSection = addSection("⚡ Вызов подпрограммы (профиля)", node.macroProfileName != null, startExpanded = (node.type == NodeType.MACRO)) { body ->
            val tvMacroDesc = TextView(service).apply {
                text = "При выполнении этого шага запустится выбранный сохранённый профиль."
                setTextColor(Color.parseColor("#B0BEC5"))
                setScaledTextSize(11.5f)
                setPadding(0, 0, 0, dpToPx(8))
            }
            body.addView(tvMacroDesc)
            
            val prefs = service.getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
            val allKeys = prefs.all.keys.toList()
            val spinnerItems = mutableListOf("— Не выбрано —")
            spinnerItems.addAll(allKeys)
            
            val macroSpinner = android.widget.Spinner(service).apply {
                val adapter = createThemedSpinnerAdapter(spinnerItems.toTypedArray())
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
                text = "Запускать параллельно (не прерывая текущий сценарий)"
                setTextColor(Color.parseColor("#FFD54F"))
                setScaledTextSize(12f)
                isChecked = node.macroRunParallel
                setOnCheckedChangeListener { _, isChecked ->
                    node.macroRunParallel = isChecked
                }
                setPadding(0, dpToPx(8), 0, 0)
            }
            body.addView(parallelSwitch)
        }
        if (node.type != NodeType.MACRO) {
            macroSection.visibility = View.GONE
        }

        // --- SECTION: Логика и Ветвление ---
        val isCondition = node.triggerMode != -1

        val successLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(2), 0, dpToPx(2)) }
        successLayout.addView(TextView(service).apply { text = if (isCondition) "При успехе ➔ шаг #:" else "Следующий шаг #:"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val nextSuccessEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(node.nextNodeIdOnSuccess?.toString() ?: "")
            hint = if (isCondition) "По порядку" else "-1 = Стоп"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(110), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        successLayout.addView(nextSuccessEdit)

        val failLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(2), 0, dpToPx(2)) }
        failLayout.addView(TextView(service).apply { text = "При неудаче ➔ шаг #:"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val nextFailEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(node.nextNodeIdOnFail?.toString() ?: "")
            hint = "Ждать"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(110), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        failLayout.addView(nextFailEdit)

        val cycleLimitLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(2), 0, dpToPx(4)) }
        cycleLimitLayout.addView(TextView(service).apply { text = "Макс. попыток проверки (0=∞):"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val cycleLimitEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.maxCheckCycles?.toString() ?: "")
            hint = "∞"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(110), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        cycleLimitLayout.addView(cycleLimitEdit)
        
        val triggerModeLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dpToPx(4), 0, dpToPx(8)) }
        triggerModeLayout.addView(TextView(service).apply { text = "Проверять условие перед шагом:"; setTextColor(Color.parseColor("#B0BEC5")); setScaledTextSize(12f); setPadding(0, 0, 0, dpToPx(4)) })
        val triggerModeSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            val adapter = createThemedSpinnerAdapter(arrayOf("Без условия (Обычный шаг)", "🎨 Цвет пикселя", "🖼 Картинка (Фрагмент экрана)", "🔤 Текст или числа (OCR)"))
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
            text = if (textZoneStartViews.containsKey(node.id)) "Скрыть рамку Зоны 1 (✚)" else "🔲 Показать рамку Зоны 1 + Стрелки (✚)"
            stylePillButton(this, "#283246", "#3E506B", 8, 12f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(36)).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
            setOnClickListener {
                if (textZoneStartViews.containsKey(node.id)) {
                    removeTextZoneMarkers(node.id)
                    text = "🔲 Показать рамку Зоны 1 + Стрелки (✚)"
                } else {
                    createTextZoneMarkers(node)
                    showZoneNudgePopup(node, isZone2 = false, initialCornerMode = 1)
                    text = "Скрыть рамку Зоны 1 (✚)"
                }
            }
        }

        val compareZoneBtn = Button(service).apply {
            text = if (compareZoneStartViews.containsKey(node.id)) "Скрыть рамку Зоны 2 (✚)" else "🟦 Показать рамку Зоны 2 + Стрелки (✚)"
            stylePillButton(this, "#006064", "#00ACC1", 8, 12f)
            visibility = if (node.compareZoneEnabled) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(36)).apply {
                setMargins(0, dpToPx(2), 0, dpToPx(6))
            }
            setOnClickListener {
                if (compareZoneStartViews.containsKey(node.id)) {
                    removeCompareZoneMarkers(node.id)
                    text = "🟦 Показать рамку Зоны 2 + Стрелки (✚)"
                } else {
                    createCompareZoneMarkers(node)
                    showZoneNudgePopup(node, isZone2 = true, initialCornerMode = 1)
                    text = "Скрыть рамку Зоны 2 (✚)"
                }
            }
        }

        val compareTwoRegionsCheck = android.widget.CheckBox(service).apply {
            text = "🔄 Сравнить две области экрана (Зона 1 и Зона 2)"
            setTextColor(Color.parseColor("#4DD0E1"))
            setScaledTextSize(12f)
            isChecked = node.compareZoneEnabled
            setOnCheckedChangeListener { _, isChecked ->
                node.compareZoneEnabled = isChecked
                compareZoneBtn.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked && !compareZoneStartViews.containsKey(node.id)) {
                    createCompareZoneMarkers(node)
                    showZoneNudgePopup(node, isZone2 = true, initialCornerMode = 1)
                    compareZoneBtn.text = "Скрыть рамку Зоны 2 (✚)"
                } else if (!isChecked) {
                    removeCompareZoneMarkers(node.id)
                }
            }
        }

        val textTargetLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 5) }
        textTargetLayout.addView(TextView(service).apply { text = "Искомый текст (если не сравниваем 2 зоны):"; setTextColor(Color.WHITE); setScaledTextSize(12f) })
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
            val langs = arrayOf("Русский" to "rus", "Английский" to "eng", "Рус + Англ" to "rus+eng")
            this.adapter = createThemedSpinnerAdapter(langs.map { it.first }.toTypedArray())
            setSelection(if (node.targetLanguage == "eng") 1 else if (node.targetLanguage == "rus+eng") 2 else 0)
        }
        textLangLayout.addView(textLangSpinner)

        val ocrFullscreenLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(6), 0, dpToPx(4)) }
        val ocrFullscreenCheck = android.widget.CheckBox(service).apply {
            text = "Нажимать на найденный текст (центр метки сдвинется)"
            setTextColor(Color.parseColor("#FFCC00"))
            setScaledTextSize(12f)
            isChecked = node.ocrFullScreenClick
            setOnCheckedChangeListener { _, isChecked ->
                node.ocrFullScreenClick = isChecked
            }
        }
        ocrFullscreenLayout.addView(ocrFullscreenCheck)
        
        // Smart OCR Math Options
        val smartOcrLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dpToPx(6), 0, dpToPx(5)) }
        val smartOcrCheck = android.widget.CheckBox(service).apply {
            text = "🔢 Умный OCR: Математическое сравнение чисел"
            setTextColor(Color.parseColor("#69F0AE"))
            setScaledTextSize(12.5f)
            isChecked = node.isSmartOcr
        }
        smartOcrLayout.addView(smartOcrCheck)
        
        val smartOcrSettings = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; visibility = if (node.isSmartOcr) View.VISIBLE else View.GONE }
        
        val smartOcrOpLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(4), 0, dpToPx(4)) }
        smartOcrOpLayout.addView(TextView(service).apply { text = "Оператор:"; setTextColor(Color.WHITE); setScaledTextSize(12f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.38f) })
        val smartOcrOpSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.62f)
            val ops = arrayOf("==", "!=", ">", "<", ">=", "<=")
            this.adapter = createThemedSpinnerAdapter(ops)
            setSelection(ops.indexOf(node.ocrOperator).coerceAtLeast(0))
        }
        smartOcrOpLayout.addView(smartOcrOpSpinner)
        smartOcrSettings.addView(smartOcrOpLayout)
        
        val ocrCompareTargetLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dpToPx(4), 0, dpToPx(4)) }
        ocrCompareTargetLayout.addView(TextView(service).apply { text = "Сравнить с:"; setTextColor(Color.WHITE); setScaledTextSize(12f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.38f) })
        val availableOcrNodes = service.nodes.filter { it.id != node.id && it.triggerMode == 2 }
        val ocrCompareTargetSpinner = Spinner(service).apply {
            val list = mutableListOf("Заданным числом", "Второй областью (Зона 2)")
            availableOcrNodes.forEach { list.add("Метка #${it.id}") }
            
            this.adapter = createThemedSpinnerAdapter(list.toTypedArray())
            
            var selIndex = 0
            if (node.compareZoneEnabled) {
                selIndex = 1
            } else if (node.ocrCompareToNodeId != null) {
                val idx = availableOcrNodes.indexOfFirst { it.id == node.ocrCompareToNodeId }
                if (idx != -1) selIndex = idx + 2
            }
            setSelection(selIndex)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.62f)
        }
        ocrCompareTargetLayout.addView(ocrCompareTargetSpinner)
        smartOcrSettings.addView(ocrCompareTargetLayout)

        val smartOcrValLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        smartOcrValLayout.addView(TextView(service).apply { text = "Значение:"; setTextColor(Color.WHITE); setScaledTextSize(12f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f) })
        val smartOcrValEdit = EditText(service).apply {
            setText(node.ocrTargetValue.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.6f)
        }
        smartOcrValLayout.addView(smartOcrValEdit)
        smartOcrSettings.addView(smartOcrValLayout)
        
        ocrCompareTargetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                smartOcrValLayout.visibility = if (position == 0) View.VISIBLE else View.GONE
                val isRegion2 = (position == 1)
                if (node.isSmartOcr) {
                    node.compareZoneEnabled = isRegion2
                    compareTwoRegionsCheck.isChecked = isRegion2
                    compareZoneBtn.visibility = if (isRegion2) View.VISIBLE else View.GONE
                    if (isRegion2 && !compareZoneStartViews.containsKey(node.id)) {
                        createCompareZoneMarkers(node)
                        compareZoneBtn.text = "Скрыть рамку Зоны 2 (✚)"
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Suffix Sets UI ("Суффиксы сохраняются наборами и используются тоже наборами")
        val smartOcrSufLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1D2432"))
                cornerRadius = dpToPx(10).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#2E3B4E"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(6), 0, dpToPx(4))
            }
        }
        smartOcrSufLayout.addView(TextView(service).apply {
            text = "📦 Набор суффиксов чисел (K, M, B, тыс...):"
            setTextColor(Color.parseColor("#80CBC4"))
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12f)
            setPadding(0, 0, 0, dpToPx(4))
        })

        val smartOcrSufEdit = EditText(service).apply {
            setText(service.getEffectiveOcrSuffixes(node))
            hint = "k:1000, m:1000000, b:1000000000"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(11.5f)
        }

        val suffixSetContainer = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        fun rebuildSuffixSetSpinner() {
            suffixSetContainer.removeAllViews()
            val setsMap = service.getSuffixSetsMap()
            val setNames = mutableListOf("— Свой список (вручную) —")
            setNames.addAll(setsMap.keys)

            val spinnerRow = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val setSpinner = Spinner(service).apply {
                adapter = createThemedSpinnerAdapter(setNames.toTypedArray())
                val curIdx = if (node.ocrSuffixSetName.isNotEmpty()) setNames.indexOf(node.ocrSuffixSetName).coerceAtLeast(0) else 0
                setSelection(curIdx)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                        if (pos == 0) {
                            node.ocrSuffixSetName = ""
                        } else {
                            val chosenName = setNames[pos]
                            node.ocrSuffixSetName = chosenName
                            setsMap[chosenName]?.let {
                                smartOcrSufEdit.setText(it)
                                node.ocrCustomSuffixes = it
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            val delSetBtn = Button(service).apply {
                text = "🗑"
                stylePillButton(this, "#3B1E22", null, 8, 11.5f)
                setTextColor(Color.parseColor("#FF8A80"))
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(32)).apply { setMargins(dpToPx(4), 0, 0, 0) }
                setOnClickListener {
                    val selName = node.ocrSuffixSetName
                    if (selName.isNotEmpty() && setsMap.containsKey(selName)) {
                        service.deleteSuffixSet(selName)
                        node.ocrSuffixSetName = ""
                        Toast.makeText(service, "Набор «$selName» удалён", Toast.LENGTH_SHORT).show()
                        rebuildSuffixSetSpinner()
                    } else {
                        Toast.makeText(service, "Сначала выберите сохранённый набор", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            spinnerRow.addView(setSpinner)
            spinnerRow.addView(delSetBtn)
            suffixSetContainer.addView(spinnerRow)
        }
        rebuildSuffixSetSpinner()
        smartOcrSufLayout.addView(suffixSetContainer)
        smartOcrSufLayout.addView(smartOcrSufEdit)

        // Quick Add single suffix pair row (e.g. "aa" -> "1e15")
        val quickAddPairRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(4))
        }
        val sufKeyEdit = EditText(service).apply {
            hint = "Суффикс (aa)"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(11.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.4f).apply { setMargins(0, 0, dpToPx(4), 0) }
        }
        val sufMultEdit = EditText(service).apply {
            hint = "Множитель (1e15)"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(11.5f)
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.45f).apply { setMargins(0, 0, dpToPx(4), 0) }
        }
        val addPairBtn = Button(service).apply {
            text = "＋"
            stylePillButton(this, "#283246", null, 8, 13f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(36), dpToPx(32))
            setOnClickListener {
                val k = sufKeyEdit.text.toString().trim()
                val m = sufMultEdit.text.toString().trim()
                if (k.isNotEmpty() && m.isNotEmpty()) {
                    val cur = smartOcrSufEdit.text.toString().trim()
                    smartOcrSufEdit.setText(if (cur.isEmpty()) "$k:$m" else "$cur,$k:$m")
                    sufKeyEdit.setText("")
                    sufMultEdit.setText("")
                }
            }
        }
        quickAddPairRow.addView(sufKeyEdit)
        quickAddPairRow.addView(sufMultEdit)
        quickAddPairRow.addView(addPairBtn)
        smartOcrSufLayout.addView(quickAddPairRow)

        // Save as named Suffix Set row
        val saveSetRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val setNameInput = EditText(service).apply {
            hint = "Имя набора (напр. Моя Игра)"
            setText(node.ocrSuffixSetName)
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(11.5f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
        }
        val saveSetBtn = Button(service).apply {
            text = "💾 В набор"
            stylePillButton(this, uiAccentColor, null, 8, 11.5f)
            setPadding(dpToPx(10), 0, dpToPx(10), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(32))
            setOnClickListener {
                val setName = setNameInput.text.toString().trim().ifEmpty { node.ocrSuffixSetName.ifEmpty { "Набор ${service.getSuffixSetsMap().size + 1}" } }
                val sufVal = smartOcrSufEdit.text.toString().trim()
                service.saveSuffixSet(setName, sufVal)
                node.ocrSuffixSetName = setName
                node.ocrCustomSuffixes = sufVal
                setNameInput.setText(setName)
                rebuildSuffixSetSpinner()
                Toast.makeText(service, "Набор суффиксов «$setName» сохранён!", Toast.LENGTH_SHORT).show()
            }
        }
        saveSetRow.addView(setNameInput)
        saveSetRow.addView(saveSetBtn)
        smartOcrSufLayout.addView(saveSetRow)

        smartOcrSettings.addView(smartOcrSufLayout)
        
        smartOcrCheck.setOnCheckedChangeListener { _, isChecked ->
            node.isSmartOcr = isChecked
            smartOcrSettings.visibility = if (isChecked) View.VISIBLE else View.GONE
            textTargetLayout.visibility = if (isChecked || node.compareZoneEnabled) View.GONE else View.VISIBLE
        }
        smartOcrLayout.addView(smartOcrSettings)

        val colorBehaviorLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 5, 0, 15) }
        colorBehaviorLayout.addView(TextView(service).apply { text = "Режим проверки цвета:"; setTextColor(Color.parseColor("#B0BEC5")); setScaledTextSize(12f); setPadding(0, 0, 0, dpToPx(4)) })
        val colorBehaviorSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            val arr = arrayOf("Статичный (Заданный цвет)", "Динамический (Изменение со временем)", "Сравнить с другой меткой", "Сравнить с другой точкой")
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
                    if (pos == 1) {
                        if (!wasDyn) node.colorOperator = "!="
                        node.dynamicColorUpdate = true; node.compareToNodeId = null; node.colorCompareX = null; node.colorCompareY = null; removeColorCompareMarker(node.id)
                    }
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

        val (dynamicLookbackRow, getDynamicLookbackMs) = createTimeInputRow("Память цвета (0 = пред. кадр):", node.dynamicColorLookbackMs)

        val colorPreview = TextView(service).apply {
            val hasColor = node.targetColor != null
            text = if (hasColor) String.format("Выбран цвет: #%06X  (удерж. кнопку для сброса)", 0xFFFFFF and node.targetColor!!) else "Цвет ещё не захвачен"
            setBackgroundColor(node.targetColor ?: Color.parseColor("#263238"))
            setTextColor(if (hasColor && Color.luminance(node.targetColor!!) > 0.5) Color.BLACK else Color.WHITE)
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            setScaledTextSize(12f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(6), 0, dpToPx(6))
            }
        }

        val capBtn = Button(service).apply {
            text = "🎯 Захватить цвет под меткой"
            stylePillButton(this, "#1976D2", null, 8, 12.5f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(38))
            setOnClickListener {
                updateNodeScreenPosition(node)
                modMenu?.visibility = View.INVISIBLE
                linesOverlayView?.visibility = View.INVISIBLE
                zoneNudgePopupView?.visibility = View.INVISIBLE
                textZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                textZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                compareZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                compareZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                colorCompareViews.values.forEach { it.visibility = View.INVISIBLE }
                nodeViews.values.forEach { it.visibility = View.INVISIBLE }

                service.handler.postDelayed({
                    service.captureColorAt(node.x, node.y) { color ->
                        modMenu?.visibility = View.VISIBLE
                        zoneNudgePopupView?.visibility = View.VISIBLE
                        textZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        textZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        compareZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        compareZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        colorCompareViews.values.forEach { it.visibility = View.VISIBLE }
                        nodeViews.values.forEach { it.visibility = View.VISIBLE }
                        invalidateLines()

                        if (color != null) {
                            node.targetColor = color
                            colorPreview.setBackgroundColor(color)
                            colorPreview.text = String.format("Выбран цвет: #%06X", 0xFFFFFF and color)
                            colorPreview.setTextColor(if (Color.luminance(color) > 0.5) Color.BLACK else Color.WHITE)
                            Toast.makeText(service, "Цвет захвачен!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, 300)
            }
            setOnLongClickListener {
                node.targetColor = null
                colorPreview.setBackgroundColor(Color.parseColor("#263238"))
                colorPreview.text = "Цвет ещё не захвачен"
                colorPreview.setTextColor(Color.WHITE)
                Toast.makeText(service, "Цвет сброшен!", Toast.LENGTH_SHORT).show()
                true
            }
        }

        val colorOpLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        colorOpLayout.addView(TextView(service).apply { text = "Когда срабатывать:"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.45f) })
        val colorOpSpinner = android.widget.Spinner(service).apply {
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 0.55f)
            val adapter = createThemedSpinnerAdapter(arrayOf("Совпадает (==)", "Не совпадает (!=)"))
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
        colorToleranceLayout.addView(TextView(service).apply { text = "Погрешность оттенка (0-255):"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val colorTolEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.colorTolerance.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(72), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        colorToleranceLayout.addView(colorTolEdit)

        val linkedLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        linkedLayout.addView(TextView(service).apply { text = "Доп. условие с меткой #:"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val linkedEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(if (node.compareToNodeId != null) node.compareToNodeId.toString() else node.linkedConditionNodeId?.toString() ?: "")
            hint = "Нет"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(72), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        linkedLayout.addView(linkedEdit)

        val linkedOpBtn = Button(service).apply {
            text = if (node.linkedConditionOperator == "AND") "Связка условий: И (AND)" else "Связка условий: ИЛИ (OR)"
            stylePillButton(this, "#283246", null, 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(34)).apply {
                setMargins(0, dpToPx(4), 0, 0)
            }
            setOnClickListener {
                node.linkedConditionOperator = if (node.linkedConditionOperator == "AND") "OR" else "AND"
                text = if (node.linkedConditionOperator == "AND") "Связка условий: И (AND)" else "Связка условий: ИЛИ (OR)"
            }
        }

        val imgThresholdLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        imgThresholdLayout.addView(TextView(service).apply { text = "Мин. совпадение (%):"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val imgThresholdEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(node.imageThreshold.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(72), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        imgThresholdLayout.addView(imgThresholdEdit)

        val searchRadiusLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        searchRadiusLayout.addView(TextView(service).apply { text = "Радиус поиска вокруг зоны (px):"; setTextColor(Color.WHITE); setScaledTextSize(12.5f); layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f) })
        val searchRadiusEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.searchRadius.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(72), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        searchRadiusLayout.addView(searchRadiusEdit)

        val resolutionScaleLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5, 0, 5) }
        val resolutionScaleText = TextView(service).apply { 
            text = "Точность (${(node.checkResolutionScale * 100).toInt()}%):"
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(dpToPx(120), WindowManager.LayoutParams.WRAP_CONTENT) 
        }
        val resolutionScaleBar = android.widget.SeekBar(service).apply {
            max = 90
            progress = ((node.checkResolutionScale - 0.1f) * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        resolutionScaleLayout.addView(resolutionScaleText)
        resolutionScaleLayout.addView(resolutionScaleBar)

        fun createCheckerboardDrawable(cellPx: Int = dpToPx(8)): android.graphics.drawable.Drawable {
            return object : android.graphics.drawable.Drawable() {
                private val lightPaint = Paint().apply { color = Color.parseColor("#2E3A48"); style = Paint.Style.FILL }
                private val darkPaint = Paint().apply { color = Color.parseColor("#1B222C"); style = Paint.Style.FILL }
                override fun draw(canvas: Canvas) {
                    val b = bounds
                    val step = cellPx.coerceAtLeast(4)
                    var y = b.top
                    var row = 0
                    while (y < b.bottom) {
                        var x = b.left
                        var col = 0
                        val nextY = minOf(y + step, b.bottom)
                        while (x < b.right) {
                            val nextX = minOf(x + step, b.right)
                            val p = if ((row + col) % 2 == 0) lightPaint else darkPaint
                            canvas.drawRect(x.toFloat(), y.toFloat(), nextX.toFloat(), nextY.toFloat(), p)
                            x += step
                            col++
                        }
                        y += step
                        row++
                    }
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
                @Deprecated("Deprecated in Java")
                override fun getOpacity(): Int = PixelFormat.OPAQUE
            }
        }

        val imgPreview = android.widget.ImageView(service).apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(130)).apply { 
                gravity = Gravity.CENTER_HORIZONTAL 
                setMargins(0, dpToPx(8), 0, dpToPx(8))
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
                        if (checkStep > 1 && bitmap.width * bitmap.height > 3600 && minOf(bitmap.width, bitmap.height) > 48) {
                            val sw = maxOf(1, bitmap.width / checkStep)
                            val sh = maxOf(1, bitmap.height / checkStep)
                            bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, sw, sh, false)
                        }
                    }
                    imgPreview.setImageBitmap(bitmap)
                    imgPreview.background = createCheckerboardDrawable(dpToPx(7))
                } catch(e: Exception) {}
            } else {
                imgPreview.setBackgroundColor(Color.parseColor("#263238"))
            }
        }
        
        resolutionScaleBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = (progress + 10) / 100f
                resolutionScaleText.text = "Точность (${(scale * 100).toInt()}%):"
                updatePreviewImage()
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
        
        updatePreviewImage()

        val imageFullScreenSearchCheck = android.widget.CheckBox(service).apply {
            text = "Искать картинку по всему экрану"
            setTextColor(Color.parseColor("#80CBC4"))
            isChecked = node.imageFullScreenSearch
            setOnCheckedChangeListener { _, isChecked ->
                node.imageFullScreenSearch = isChecked
            }
        }

        val imageClickFoundCenterCheck = android.widget.CheckBox(service).apply {
            text = "Нажимать в центр найденной картинки"
            setTextColor(Color.parseColor("#FFCC00"))
            isChecked = node.imageClickFoundCenter
            setOnCheckedChangeListener { _, isChecked ->
                node.imageClickFoundCenter = isChecked
            }
        }

        val adaptiveScaleSettingsLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12), dpToPx(2), dpToPx(4), dpToPx(6))
            visibility = if (node.imageAdaptiveScale) View.VISIBLE else View.GONE
        }
        adaptiveScaleSettingsLayout.addView(TextView(service).apply {
            text = "Отклонение размера (±%, 5–50):"
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val imageScaleRangeEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.imageScaleRangePct.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(75), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        adaptiveScaleSettingsLayout.addView(imageScaleRangeEdit)

        val imageAdaptiveScaleCheck = android.widget.CheckBox(service).apply {
            text = "🔍 Адаптивный масштаб (если спрайт больше/меньше)"
            setTextColor(Color.parseColor("#81D4FA"))
            isChecked = node.imageAdaptiveScale
            setOnCheckedChangeListener { _, isChecked ->
                node.imageAdaptiveScale = isChecked
                adaptiveScaleSettingsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }

        val multiTargetSettingsLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1D2432"))
                cornerRadius = dpToPx(8).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#2E3B4E"))
            }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dpToPx(2), 0, dpToPx(6))
            }
            visibility = if (node.imageMultiTarget) View.VISIBLE else View.GONE
        }

        val imageMultiRandomOrderCheck = android.widget.CheckBox(service).apply {
            text = "🎲 Нажимать в случайном порядке (анти-детект)"
            setTextColor(Color.parseColor("#CE93D8"))
            setScaledTextSize(12f)
            isChecked = node.imageMultiRandomOrder
            setOnCheckedChangeListener { _, isChecked ->
                node.imageMultiRandomOrder = isChecked
            }
        }
        multiTargetSettingsLayout.addView(imageMultiRandomOrderCheck)

        val multiMaxRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(2))
        }
        multiMaxRow.addView(TextView(service).apply {
            text = "Макс. целей за раз (0 = все):"
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val imageMultiMaxTargetsEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.imageMultiMaxTargets.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(75), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        multiMaxRow.addView(imageMultiMaxTargetsEdit)
        multiTargetSettingsLayout.addView(multiMaxRow)

        val multiDelayRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(2))
        }
        multiDelayRow.addView(TextView(service).apply {
            text = "Пауза между целями (мс):"
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val imageMultiClickDelayEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.imageMultiClickDelayMs.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(75), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        multiDelayRow.addView(imageMultiClickDelayEdit)
        multiTargetSettingsLayout.addView(multiDelayRow)

        val multiRandDelayRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(2))
        }
        multiRandDelayRow.addView(TextView(service).apply {
            text = "Разброс паузы (+мс, анти-детект):"
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        })
        val imageMultiRandomDelayEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.imageMultiRandomDelayMs.toString())
            setTextColor(Color.WHITE)
            setScaledTextSize(12f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(75), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        multiRandDelayRow.addView(imageMultiRandomDelayEdit)
        multiTargetSettingsLayout.addView(multiRandDelayRow)

        val imageMultiTargetCheck = android.widget.CheckBox(service).apply {
            text = "🎯 Несколько одинаковых целей (нажать все)"
            setTextColor(Color.parseColor("#69F0AE"))
            isChecked = node.imageMultiTarget
            setOnCheckedChangeListener { _, isChecked ->
                node.imageMultiTarget = isChecked
                multiTargetSettingsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (isChecked && !imageClickFoundCenterCheck.isChecked) {
                    imageClickFoundCenterCheck.isChecked = true
                    node.imageClickFoundCenter = true
                }
            }
        }

        val capImgBtn = Button(service).apply {
            text = if (node.triggerMode == 1) "📸 Захватить образец с экрана (удерж. = сброс)" else "🔍 Проверить распознавание текста"
            stylePillButton(this, "#1976D2", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(38)).apply {
                setMargins(0, dpToPx(4), 0, dpToPx(4))
            }
            setOnClickListener {
                updateNodeScreenPosition(node)
                if (node.triggerMode == 2) {
                    node.targetText = textTargetEdit.text.toString().takeIf { it.isNotEmpty() }
                    node.targetLanguage = if (textLangSpinner.selectedItemPosition == 1) "eng" else if (textLangSpinner.selectedItemPosition == 2) "rus+eng" else "rus"
                    node.isSmartOcr = smartOcrCheck.isChecked
                    node.ocrOperator = arrayOf("==", "!=", ">", "<", ">=", "<=")[smartOcrOpSpinner.selectedItemPosition]
                    node.ocrTargetValue = smartOcrValEdit.text.toString().toDoubleOrNull() ?: 0.0
                    node.ocrCustomSuffixes = smartOcrSufEdit.text.toString()
                    if (node.ocrSuffixSetName.isNotEmpty()) {
                        service.saveSuffixSet(node.ocrSuffixSetName, node.ocrCustomSuffixes)
                    }
                    val selPos = ocrCompareTargetSpinner.selectedItemPosition
                    if (node.isSmartOcr) {
                        node.compareZoneEnabled = (selPos == 1)
                        node.ocrCompareToNodeId = if (selPos >= 2) availableOcrNodes[selPos - 2].id else null
                    } else {
                        node.compareZoneEnabled = compareTwoRegionsCheck.isChecked
                        node.ocrCompareToNodeId = null
                    }
                }
                if (node.textZoneStartX == 0 && node.textZoneStartY == 0 && node.textZoneEndX == 0 && node.textZoneEndY == 0) {
                    createTextZoneMarkers(node)
                    fragZoneBtn.text = "Скрыть рамку Зоны 1 (✚)"
                }
                modMenu?.visibility = View.INVISIBLE
                linesOverlayView?.visibility = View.INVISIBLE
                zoneNudgePopupView?.visibility = View.INVISIBLE
                textZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                textZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                compareZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                compareZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                colorCompareViews.values.forEach { it.visibility = View.INVISIBLE }
                nodeViews.values.forEach { it.visibility = View.INVISIBLE }
                
                service.handler.postDelayed({
                    service.captureImageFragment(node) { base64 ->
                        modMenu?.visibility = View.VISIBLE
                        zoneNudgePopupView?.visibility = View.VISIBLE
                        textZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        textZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        compareZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        compareZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        colorCompareViews.values.forEach { it.visibility = View.VISIBLE }
                        nodeViews.values.forEach { it.visibility = View.VISIBLE }
                        invalidateLines()
                        
                        if (base64 != null) {
                            try {
                                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                node.invalidateImageCache()
                                node.targetImageBase64 = base64
                                updatePreviewImage()
                                if (node.triggerMode == 1) {
                                    Toast.makeText(service, "Фрагмент захвачен (${bitmap.width}x${bitmap.height})!", Toast.LENGTH_SHORT).show()
                                } else {
                                    service.testTextRecognition(node, bitmap)
                                }
                            } catch(e: Exception) {}
                        }
                    }
                }, 300)
            }
            setOnLongClickListener {
                node.invalidateImageCache()
                node.targetImageBase64 = null
                imgPreview.setImageBitmap(null)
                imgPreview.setBackgroundColor(Color.parseColor("#263238"))
                if (node.triggerMode == 1) {
                    Toast.makeText(service, "Фрагмент сброшен!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(service, "Превью сброшено!", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        val importImgBtn = Button(service).apply {
            text = "📂 Загрузить картинку из галереи / файла"
            stylePillButton(this, "#283246", "#3E506B", 8, 12f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(36)).apply {
                setMargins(0, 0, 0, dpToPx(4))
            }
            setOnClickListener {
                val intent = android.content.Intent(service, MainActivity::class.java).apply {
                    action = "ACTION_IMPORT_IMAGE"
                    putExtra("NODE_ID", node.id)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
            }
        }

        val spriteLibraryBtnRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(4))
            }
        }

        val saveSpritePanel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1D2432"))
                cornerRadius = dpToPx(8).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#FFB300"))
            }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
            visibility = View.GONE
        }

        val spriteLibraryListPanel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#18202C"))
                cornerRadius = dpToPx(8).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#00ACC1"))
            }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
            visibility = View.GONE
        }

        lateinit var insertSavedSpriteBtnRef: Button
        lateinit var refreshSpriteLibraryList: () -> Unit

        val saveSpriteNameEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Название спрайта (например: Кнопка Бой)"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        saveSpritePanel.addView(TextView(service).apply {
            text = "💾 Сохранение текущего образца в кэш спрайтов:"
            setTextColor(Color.parseColor("#FFD54F"))
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(11.5f)
        })
        saveSpritePanel.addView(saveSpriteNameEdit)

        val saveSpriteActionRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(4), 0, 0)
        }
        saveSpriteActionRow.addView(Button(service).apply {
            text = "✓ Сохранить в кэш"
            stylePillButton(this, "#2E7D32", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setOnClickListener {
                val b64 = node.targetImageBase64
                if (b64.isNullOrEmpty()) {
                    Toast.makeText(service, "Сначала захватите или загрузите картинку!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                var w = 0
                var h = 0
                try {
                    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        w = bmp.width
                        h = bmp.height
                        bmp.recycle()
                    }
                } catch (_: Exception) {}
                val defaultName = "Спрайт #${service.getSavedSprites().size + 1} (${w}x${h})"
                val chosenName = saveSpriteNameEdit.text.toString().trim().ifEmpty { defaultName }
                service.saveSpriteToLibrary(chosenName, b64, w, h)
                saveSpritePanel.visibility = View.GONE
                val count = service.getSavedSprites().size
                insertSavedSpriteBtnRef.text = "📦 Вставить спрайт ($count)"
                if (spriteLibraryListPanel.visibility == View.VISIBLE) {
                    refreshSpriteLibraryList()
                }
                Toast.makeText(service, "Спрайт '$chosenName' сохранён в кэш!", Toast.LENGTH_SHORT).show()
            }
        })
        saveSpriteActionRow.addView(Button(service).apply {
            text = "Отмена"
            stylePillButton(this, "#37474F", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(dpToPx(85), dpToPx(34))
            setOnClickListener {
                saveSpritePanel.visibility = View.GONE
            }
        })
        saveSpritePanel.addView(saveSpriteActionRow)

        refreshSpriteLibraryList = {
            spriteLibraryListPanel.removeAllViews()
            val sprites = service.getSavedSprites()
            insertSavedSpriteBtnRef.text = "📦 Вставить спрайт (${sprites.size})"

            val headerRow = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dpToPx(6))
            }
            headerRow.addView(TextView(service).apply {
                text = "📦 Сохранённые спрайты (${sprites.size}):"
                setTextColor(Color.parseColor("#80DEEA"))
                setTypeface(null, Typeface.BOLD)
                setScaledTextSize(12f)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            })
            headerRow.addView(Button(service).apply {
                text = "✕"
                stylePillButton(this, "#37474F", null, 6, 11f)
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(26))
                setOnClickListener {
                    spriteLibraryListPanel.visibility = View.GONE
                }
            })
            spriteLibraryListPanel.addView(headerRow)

            if (sprites.isEmpty()) {
                spriteLibraryListPanel.addView(TextView(service).apply {
                    text = "Кэш спрайтов пуст.\nСначала захватите образец на экран или загрузите из галереи, затем нажмите «💾 Сохранить как спрайт»."
                    setTextColor(Color.parseColor("#90A4AE"))
                    setScaledTextSize(11.5f)
                    setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                })
            } else {
                sprites.forEach { sp ->
                    val itemRow = LinearLayout(service).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dpToPx(6), dpToPx(5), dpToPx(6), dpToPx(5))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(Color.parseColor("#243040"))
                            cornerRadius = dpToPx(6).toFloat()
                        }
                        layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                            setMargins(0, 0, 0, dpToPx(4))
                        }
                    }

                    val thumbView = android.widget.ImageView(service).apply {
                        layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(42)).apply {
                            setMargins(0, 0, dpToPx(8), 0)
                        }
                        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        setBackgroundColor(Color.parseColor("#121820"))
                        setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                        try {
                            val bytes = android.util.Base64.decode(sp.base64, android.util.Base64.DEFAULT)
                            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            setImageBitmap(bmp)
                        } catch (_: Exception) {}
                    }
                    itemRow.addView(thumbView)

                    val infoCol = LinearLayout(service).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    infoCol.addView(TextView(service).apply {
                        text = sp.name
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        setScaledTextSize(12f)
                    })
                    infoCol.addView(TextView(service).apply {
                        text = if (sp.width > 0 && sp.height > 0) "${sp.width}×${sp.height} px" else "Спрайт"
                        setTextColor(Color.parseColor("#90A4AE"))
                        setScaledTextSize(10.5f)
                    })
                    itemRow.addView(infoCol)

                    itemRow.addView(Button(service).apply {
                        text = "Вставить"
                        stylePillButton(this, "#00838F", null, 6, 11.5f)
                        setPadding(dpToPx(8), 0, dpToPx(8), 0)
                        layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, dpToPx(32)).apply {
                            setMargins(dpToPx(4), 0, dpToPx(4), 0)
                        }
                        setOnClickListener {
                            node.invalidateImageCache()
                            node.targetImageBase64 = sp.base64
                            updatePreviewImage()
                            service.autoSave()
                            spriteLibraryListPanel.visibility = View.GONE
                            Toast.makeText(service, "Вставлен спрайт: ${sp.name}", Toast.LENGTH_SHORT).show()
                        }
                    })

                    itemRow.addView(Button(service).apply {
                        text = "🗑"
                        stylePillButton(this, "#4E2A2A", null, 6, 11.5f)
                        setPadding(0, 0, 0, 0)
                        layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
                        setOnClickListener {
                            service.deleteSpriteFromLibrary(sp.name)
                            refreshSpriteLibraryList()
                            Toast.makeText(service, "Спрайт '${sp.name}' удалён из кэша", Toast.LENGTH_SHORT).show()
                        }
                    })

                    spriteLibraryListPanel.addView(itemRow)
                }
            }
        }

        val saveAsSpriteBtn = Button(service).apply {
            text = "💾 Сохранить как спрайт"
            stylePillButton(this, "#5D4037", "#FFB300", 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setOnClickListener {
                val b64 = node.targetImageBase64
                if (b64.isNullOrEmpty()) {
                    Toast.makeText(service, "Сначала захватите образец с экрана или загрузите картинку!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (saveSpritePanel.visibility == View.VISIBLE) {
                    saveSpritePanel.visibility = View.GONE
                } else {
                    var w = 0
                    var h = 0
                    try {
                        val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                        val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            w = bmp.width
                            h = bmp.height
                            bmp.recycle()
                        }
                    } catch (_: Exception) {}
                    val nextNum = service.getSavedSprites().size + 1
                    saveSpriteNameEdit.setText("Спрайт #$nextNum (${w}x${h})")
                    saveSpriteNameEdit.setSelection(saveSpriteNameEdit.text.length)
                    spriteLibraryListPanel.visibility = View.GONE
                    saveSpritePanel.visibility = View.VISIBLE
                }
            }
        }

        val insertSavedSpriteBtn = Button(service).apply {
            val initialCount = service.getSavedSprites().size
            text = "📦 Вставить спрайт ($initialCount)"
            stylePillButton(this, "#006064", "#00ACC1", 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(dpToPx(4), 0, 0, 0)
            }
            setOnClickListener {
                if (spriteLibraryListPanel.visibility == View.VISIBLE) {
                    spriteLibraryListPanel.visibility = View.GONE
                } else {
                    saveSpritePanel.visibility = View.GONE
                    refreshSpriteLibraryList()
                    spriteLibraryListPanel.visibility = View.VISIBLE
                }
            }
        }
        insertSavedSpriteBtnRef = insertSavedSpriteBtn

        spriteLibraryBtnRow.addView(saveAsSpriteBtn)
        spriteLibraryBtnRow.addView(insertSavedSpriteBtn)

        val spriteEditorPanel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#161F2C"))
                cornerRadius = dpToPx(8).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#AB47BC"))
            }
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
            visibility = View.GONE
        }

        var editW = 0
        var editH = 0
        var editPixels = IntArray(0)
        var origW = 0
        var origH = 0
        var origPixels = IntArray(0)
        data class EditorSnapshot(val w: Int, val h: Int, val pixels: IntArray, val oW: Int, val oH: Int, val oPixels: IntArray)
        val undoStack = ArrayDeque<EditorSnapshot>()
        var editorToolMode = 0 // 0 = Magic Wand (Фон по касанию), 1 = Eraser (Ластик), 2 = Restore (Восстановить)
        var bgTolerance = 28
        var wandConnectedOnly = true
        var brushRadius = 1 // 0 -> 1px, 1 -> 2px, 2 -> 4px, 3 -> 6px

        fun pushUndoSnapshot() {
            if (editW <= 0 || editH <= 0 || editPixels.isEmpty()) return
            if (undoStack.size >= 15) undoStack.removeFirst()
            undoStack.addLast(
                EditorSnapshot(
                    editW,
                    editH,
                    editPixels.clone(),
                    origW,
                    origH,
                    origPixels.clone()
                )
            )
        }

        val editorHeaderRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(4))
        }
        val editorTitleTv = TextView(service).apply {
            text = "✂️ Редактор спрайта (удаление фона)"
            setTextColor(Color.parseColor("#E1BEE7"))
            setTypeface(null, Typeface.BOLD)
            setScaledTextSize(12f)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        val editorCloseBtn = Button(service).apply {
            text = "✕"
            stylePillButton(this, "#37474F", null, 6, 11f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(28), dpToPx(26))
            setOnClickListener {
                spriteEditorPanel.visibility = View.GONE
            }
        }
        editorHeaderRow.addView(editorTitleTv)
        editorHeaderRow.addView(editorCloseBtn)
        spriteEditorPanel.addView(editorHeaderRow)

        val editorStatsTv = TextView(service).apply {
            text = "Коснитесь фона на картинке, чтобы сделать его прозрачным (игнорируется при поиске)"
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(11f)
            setPadding(0, 0, 0, dpToPx(6))
        }
        spriteEditorPanel.addView(editorStatsTv)

        lateinit var spriteCanvasView: View
        lateinit var undoEditorBtn: Button

        fun updateEditorStats() {
            if (editW <= 0 || editH <= 0 || editPixels.isEmpty()) return
            var opaqueCount = 0
            for (px in editPixels) {
                if ((px ushr 24) >= 128) opaqueCount++
            }
            val total = editW * editH
            val activePct = ((opaqueCount * 100f) / total.coerceAtLeast(1)).toInt()
            val maskedPct = 100 - activePct
            editorStatsTv.text = "Размер: ${editW}×${editH} px | Спрайт: $activePct% ($opaqueCount пикс.) | Удалено фона: $maskedPct%"
            if (::undoEditorBtn.isInitialized) {
                undoEditorBtn.text = "↩️ Назад (${undoStack.size})"
            }
        }

        fun colorMatchesWithinTol(c1: Int, c2: Int, tol: Int): Boolean {
            val r1 = (c1 shr 16) and 0xFF
            val g1 = (c1 shr 8) and 0xFF
            val b1 = c1 and 0xFF
            val r2 = (c2 shr 16) and 0xFF
            val g2 = (c2 shr 8) and 0xFF
            val b2 = c2 and 0xFF
            return Math.abs(r1 - r2) <= tol && Math.abs(g1 - g2) <= tol && Math.abs(b1 - b2) <= tol
        }

        fun eraseBackgroundAt(startX: Int, startY: Int) {
            if (startX !in 0 until editW || startY !in 0 until editH) return
            val seedColor = editPixels[startY * editW + startX]
            if ((seedColor ushr 24) < 128) return
            pushUndoSnapshot()

            if (!wandConnectedOnly) {
                for (i in editPixels.indices) {
                    val c = editPixels[i]
                    if ((c ushr 24) >= 128 && colorMatchesWithinTol(c, seedColor, bgTolerance)) {
                        editPixels[i] = 0x00000000
                    }
                }
            } else {
                val visited = BooleanArray(editW * editH)
                val queue = IntArray(editW * editH)
                var head = 0
                var tail = 0
                val startIdx = startY * editW + startX
                visited[startIdx] = true
                queue[tail++] = startIdx

                while (head < tail) {
                    val idx = queue[head++]
                    editPixels[idx] = 0x00000000
                    val cx = idx % editW
                    val cy = idx / editW
                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny !in 0 until editH) continue
                        val rowOff = ny * editW
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            if (nx !in 0 until editW) continue
                            val nIdx = rowOff + nx
                            if (!visited[nIdx]) {
                                val nc = editPixels[nIdx]
                                if ((nc ushr 24) >= 128 && colorMatchesWithinTol(nc, seedColor, bgTolerance)) {
                                    visited[nIdx] = true
                                    queue[tail++] = nIdx
                                }
                            }
                        }
                    }
                }
            }
            updateEditorStats()
            spriteCanvasView.invalidate()
        }

        fun applyBrushAt(centerX: Int, centerY: Int, restore: Boolean) {
            if (editW <= 0 || editH <= 0) return
            val r = brushRadius
            val minY = (centerY - r).coerceAtLeast(0)
            val maxY = (centerY + r).coerceAtMost(editH - 1)
            val minX = (centerX - r).coerceAtLeast(0)
            val maxX = (centerX + r).coerceAtMost(editW - 1)
            for (y in minY..maxY) {
                val rowOff = y * editW
                for (x in minX..maxX) {
                    if ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY) <= r * r + 1) {
                        val idx = rowOff + x
                        if (restore) {
                            if (origW == editW && origH == editH && idx in origPixels.indices) {
                                editPixels[idx] = origPixels[idx]
                            } else {
                                editPixels[idx] = editPixels[idx] or (0xFF shl 24)
                            }
                        } else {
                            editPixels[idx] = 0x00000000
                        }
                    }
                }
            }
            updateEditorStats()
            spriteCanvasView.invalidate()
        }

        spriteCanvasView = object : View(service) {
            private val lightCellPaint = Paint().apply { color = Color.parseColor("#323F4F"); style = Paint.Style.FILL }
            private val darkCellPaint = Paint().apply { color = Color.parseColor("#1D2530"); style = Paint.Style.FILL }
            private val borderPaint = Paint().apply {
                color = Color.parseColor("#CE93D8")
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(1).toFloat()
            }
            private val bmpPaint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }
            private val dstRect = android.graphics.RectF()
            private var strokeStarted = false

            private fun computeDestRect(): android.graphics.RectF {
                val pad = dpToPx(8).toFloat()
                val availW = (width - pad * 2f).coerceAtLeast(1f)
                val availH = (height - pad * 2f).coerceAtLeast(1f)
                if (editW <= 0 || editH <= 0) {
                    dstRect.set(pad, pad, pad + availW, pad + availH)
                    return dstRect
                }
                val scale = minOf(availW / editW.toFloat(), availH / editH.toFloat())
                val drawW = editW * scale
                val drawH = editH * scale
                val left = (width - drawW) / 2f
                val top = (height - drawH) / 2f
                dstRect.set(left, top, left + drawW, top + drawH)
                return dstRect
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val rect = computeDestRect()
                val cell = dpToPx(7).coerceAtLeast(4)
                var y = rect.top.toInt()
                var row = 0
                val rightInt = rect.right.toInt()
                val bottomInt = rect.bottom.toInt()
                while (y < bottomInt) {
                    var x = rect.left.toInt()
                    var col = 0
                    val nextY = minOf(y + cell, bottomInt)
                    while (x < rightInt) {
                        val nextX = minOf(x + cell, rightInt)
                        val p = if ((row + col) % 2 == 0) lightCellPaint else darkCellPaint
                        canvas.drawRect(x.toFloat(), y.toFloat(), nextX.toFloat(), nextY.toFloat(), p)
                        x += cell
                        col++
                    }
                    y += cell
                    row++
                }

                if (editW > 0 && editH > 0 && editPixels.size == editW * editH) {
                    val tempBmp = android.graphics.Bitmap.createBitmap(editPixels, editW, editH, android.graphics.Bitmap.Config.ARGB_8888)
                    canvas.drawBitmap(tempBmp, null, rect, bmpPaint)
                    tempBmp.recycle()
                }
                canvas.drawRect(rect, borderPaint)
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (editW <= 0 || editH <= 0 || editPixels.isEmpty()) return false
                val rect = computeDestRect()
                val tx = ((event.x - rect.left) / rect.width() * editW).toInt()
                val ty = ((event.y - rect.top) / rect.height() * editH).toInt()

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        if (tx in 0 until editW && ty in 0 until editH) {
                            if (editorToolMode == 0) {
                                eraseBackgroundAt(tx, ty)
                            } else {
                                pushUndoSnapshot()
                                strokeStarted = true
                                applyBrushAt(tx, ty, restore = (editorToolMode == 2))
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        if (editorToolMode != 0) {
                            val clampedX = tx.coerceIn(0, editW - 1)
                            val clampedY = ty.coerceIn(0, editH - 1)
                            if (!strokeStarted) {
                                pushUndoSnapshot()
                                strokeStarted = true
                            }
                            applyBrushAt(clampedX, clampedY, restore = (editorToolMode == 2))
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        strokeStarted = false
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return true
                    }
                }
                return super.onTouchEvent(event)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(200)).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }
        spriteEditorPanel.addView(spriteCanvasView)

        // Tool mode buttons row: Палочка (Фон) | Ластик | Восстановить
        val toolModeRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }
        lateinit var wandToolBtn: Button
        lateinit var eraserToolBtn: Button
        lateinit var restoreToolBtn: Button
        lateinit var wandSettingsContainer: LinearLayout
        lateinit var brushSettingsContainer: LinearLayout

        fun updateToolButtonsUI() {
            stylePillButton(wandToolBtn, if (editorToolMode == 0) "#7B1FA2" else "#283246", if (editorToolMode == 0) "#E1BEE7" else null, 8, 11f)
            stylePillButton(eraserToolBtn, if (editorToolMode == 1) "#C62828" else "#283246", if (editorToolMode == 1) "#FFCDD2" else null, 8, 11f)
            stylePillButton(restoreToolBtn, if (editorToolMode == 2) "#2E7D32" else "#283246", if (editorToolMode == 2) "#A5D6A7" else null, 8, 11f)
            wandSettingsContainer.visibility = if (editorToolMode == 0) View.VISIBLE else View.GONE
            brushSettingsContainer.visibility = if (editorToolMode != 0) View.VISIBLE else View.GONE
        }

        wandToolBtn = Button(service).apply {
            text = "🪄 Палочка (Фон)"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1.15f).apply { setMargins(0, 0, dpToPx(3), 0) }
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            setOnClickListener {
                editorToolMode = 0
                updateToolButtonsUI()
            }
        }
        eraserToolBtn = Button(service).apply {
            text = "🧽 Ластик"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 0.9f).apply { setMargins(dpToPx(3), 0, dpToPx(3), 0) }
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            setOnClickListener {
                editorToolMode = 1
                updateToolButtonsUI()
            }
        }
        restoreToolBtn = Button(service).apply {
            text = "🖌 Вернуть"
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 0.95f).apply { setMargins(dpToPx(3), 0, 0, 0) }
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            setOnClickListener {
                editorToolMode = 2
                updateToolButtonsUI()
            }
        }
        toolModeRow.addView(wandToolBtn)
        toolModeRow.addView(eraserToolBtn)
        toolModeRow.addView(restoreToolBtn)
        spriteEditorPanel.addView(toolModeRow)

        wandSettingsContainer = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(4), 0, dpToPx(4), dpToPx(4))
        }
        val wandConnectedCheck = android.widget.CheckBox(service).apply {
            text = "🔗 Только смежные пиксели (не трогать цвет внутри иконки)"
            setTextColor(Color.parseColor("#CE93D8"))
            setScaledTextSize(11.5f)
            isChecked = wandConnectedOnly
            setOnCheckedChangeListener { _, isChecked ->
                wandConnectedOnly = isChecked
            }
        }
        wandSettingsContainer.addView(wandConnectedCheck)

        val tolRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(2), 0, dpToPx(2))
        }
        val tolLabelTv = TextView(service).apply {
            text = "Допуск цвета фона ($bgTolerance):"
            setTextColor(Color.WHITE)
            setScaledTextSize(11.5f)
        }
        val tolBar = android.widget.SeekBar(service).apply {
            max = 100
            progress = bgTolerance
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    bgTolerance = progress.coerceAtLeast(1)
                    tolLabelTv.text = "Допуск цвета фона ($bgTolerance):"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        tolRow.addView(tolLabelTv)
        tolRow.addView(tolBar)
        wandSettingsContainer.addView(tolRow)
        spriteEditorPanel.addView(wandSettingsContainer)

        brushSettingsContainer = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(6))
            visibility = View.GONE
        }
        val brushLabelTv = TextView(service).apply {
            text = "Размер кисти (${brushRadius * 2 + 1} px):"
            setTextColor(Color.WHITE)
            setScaledTextSize(11.5f)
        }
        val brushSeekBar = android.widget.SeekBar(service).apply {
            max = 6
            progress = brushRadius
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    brushRadius = progress.coerceIn(0, 6)
                    brushLabelTv.text = "Размер кисти (${brushRadius * 2 + 1} px):"
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        brushSettingsContainer.addView(brushLabelTv)
        brushSettingsContainer.addView(brushSeekBar)
        spriteEditorPanel.addView(brushSettingsContainer)

        updateToolButtonsUI()

        // Quick actions row: ⚡ Авто-фон с краёв | ✂️ Обрезать пустые края
        val quickAutoRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dpToPx(6))
            }
        }
        val autoBorderBgBtn = Button(service).apply {
            text = "⚡ Авто-фон с краёв"
            stylePillButton(this, "#4A148C", "#BA68C8", 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener {
                if (editW <= 0 || editH <= 0 || editPixels.isEmpty()) return@setOnClickListener
                pushUndoSnapshot()
                val visited = BooleanArray(editW * editH)
                val queue = IntArray(editW * editH)
                val seedColors = IntArray(editW * editH)
                var head = 0
                var tail = 0

                fun enqueueBorderPixel(x: Int, y: Int) {
                    val idx = y * editW + x
                    if (!visited[idx]) {
                        val c = editPixels[idx]
                        visited[idx] = true
                        if ((c ushr 24) >= 128) {
                            seedColors[tail] = c
                            queue[tail] = idx
                            tail++
                        }
                    }
                }

                for (x in 0 until editW) {
                    enqueueBorderPixel(x, 0)
                    enqueueBorderPixel(x, editH - 1)
                }
                for (y in 0 until editH) {
                    enqueueBorderPixel(0, y)
                    enqueueBorderPixel(editW - 1, y)
                }

                var removed = 0
                while (head < tail) {
                    val idx = queue[head]
                    val refColor = seedColors[head]
                    head++
                    if ((editPixels[idx] ushr 24) >= 128) {
                        editPixels[idx] = 0x00000000
                        removed++
                    }
                    val cx = idx % editW
                    val cy = idx / editW
                    for (dy in -1..1) {
                        val ny = cy + dy
                        if (ny !in 0 until editH) continue
                        val rowOff = ny * editW
                        for (dx in -1..1) {
                            if (dx == 0 && dy == 0) continue
                            val nx = cx + dx
                            if (nx !in 0 until editW) continue
                            val nIdx = rowOff + nx
                            if (!visited[nIdx]) {
                                val nc = editPixels[nIdx]
                                if ((nc ushr 24) >= 128 && colorMatchesWithinTol(nc, refColor, bgTolerance)) {
                                    visited[nIdx] = true
                                    seedColors[tail] = refColor
                                    queue[tail] = nIdx
                                    tail++
                                }
                            }
                        }
                    }
                }
                updateEditorStats()
                spriteCanvasView.invalidate()
                Toast.makeText(service, "Авто-очистка краёв: удалено $removed пикс. фона", Toast.LENGTH_SHORT).show()
            }
        }
        val autoCropTransparentBtn = Button(service).apply {
            text = "✂️ Обрезать края"
            stylePillButton(this, "#00695C", "#4DB6AC", 8, 11.5f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply { setMargins(dpToPx(4), 0, 0, 0) }
            setOnClickListener {
                if (editW <= 0 || editH <= 0 || editPixels.isEmpty()) return@setOnClickListener
                var minX = editW
                var minY = editH
                var maxX = -1
                var maxY = -1
                for (y in 0 until editH) {
                    val rowOff = y * editW
                    for (x in 0 until editW) {
                        if ((editPixels[rowOff + x] ushr 24) >= 128) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                    }
                }
                if (maxX < minX || maxY < minY) {
                    Toast.makeText(service, "Все пиксели прозрачны — нечего обрезать!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val newW = maxX - minX + 1
                val newH = maxY - minY + 1
                if (newW == editW && newH == editH) {
                    Toast.makeText(service, "Пустых прозрачных краёв нет", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                pushUndoSnapshot()
                val cropped = IntArray(newW * newH)
                val croppedOrig = IntArray(newW * newH)
                for (y in 0 until newH) {
                    val srcRow = (minY + y) * editW + minX
                    val dstRow = y * newW
                    System.arraycopy(editPixels, srcRow, cropped, dstRow, newW)
                    if (origW == editW && origH == editH) {
                        System.arraycopy(origPixels, srcRow, croppedOrig, dstRow, newW)
                    } else {
                        System.arraycopy(editPixels, srcRow, croppedOrig, dstRow, newW)
                    }
                }
                editW = newW
                editH = newH
                editPixels = cropped
                origW = newW
                origH = newH
                origPixels = croppedOrig
                updateEditorStats()
                spriteCanvasView.invalidate()
                Toast.makeText(service, "Края обрезаны: новый размер ${editW}×${editH} px", Toast.LENGTH_SHORT).show()
            }
        }
        quickAutoRow.addView(autoBorderBgBtn)
        quickAutoRow.addView(autoCropTransparentBtn)
        spriteEditorPanel.addView(quickAutoRow)

        // History & Apply row: ↩️ Назад | 🔄 Сброс | ✓ Применить к метке
        val editorBottomRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        undoEditorBtn = Button(service).apply {
            text = "↩️ Назад (0)"
            stylePillButton(this, "#37474F", null, 8, 11f)
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 0.85f).apply { setMargins(0, 0, dpToPx(3), 0) }
            setOnClickListener {
                if (undoStack.isEmpty()) {
                    Toast.makeText(service, "Нет действий для отмены", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val snap = undoStack.removeLast()
                editW = snap.w
                editH = snap.h
                editPixels = snap.pixels
                origW = snap.oW
                origH = snap.oH
                origPixels = snap.oPixels
                updateEditorStats()
                spriteCanvasView.invalidate()
            }
        }
        val resetEditorBtn = Button(service).apply {
            text = "🔄 Сброс"
            stylePillButton(this, "#455A64", null, 8, 11f)
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 0.75f).apply { setMargins(dpToPx(3), 0, dpToPx(3), 0) }
            setOnClickListener {
                if (origPixels.isEmpty()) return@setOnClickListener
                pushUndoSnapshot()
                editW = origW
                editH = origH
                editPixels = origPixels.clone()
                updateEditorStats()
                spriteCanvasView.invalidate()
                Toast.makeText(service, "Восстановлен исходный образец", Toast.LENGTH_SHORT).show()
            }
        }
        val applyEditorBtn = Button(service).apply {
            text = "✓ Применить к метке"
            stylePillButton(this, "#2E7D32", "#81C784", 8, 11.5f)
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1.4f).apply { setMargins(dpToPx(3), 0, 0, 0) }
            setOnClickListener {
                if (editW <= 0 || editH <= 0 || editPixels.isEmpty()) return@setOnClickListener
                var opaqueCount = 0
                for (px in editPixels) {
                    if ((px ushr 24) >= 128) opaqueCount++
                }
                if (opaqueCount == 0) {
                    Toast.makeText(service, "Нельзя сохранить полностью пустой спрайт! Оставьте хотя бы часть иконки.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                try {
                    val resultBmp = android.graphics.Bitmap.createBitmap(editPixels, editW, editH, android.graphics.Bitmap.Config.ARGB_8888)
                    val out = java.io.ByteArrayOutputStream()
                    resultBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    resultBmp.recycle()
                    val newB64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.DEFAULT)
                    node.invalidateImageCache()
                    node.targetImageBase64 = newB64
                    updatePreviewImage()
                    service.autoSave()
                    spriteEditorPanel.visibility = View.GONE
                    val activePct = ((opaqueCount * 100f) / (editW * editH).coerceAtLeast(1)).toInt()
                    Toast.makeText(service, "Спрайт обновлён (${editW}×${editH}, активных пикселей: $activePct%)! Фон будет игнорироваться.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(service, "Ошибка сохранения спрайта", Toast.LENGTH_SHORT).show()
                }
            }
        }
        editorBottomRow.addView(undoEditorBtn)
        editorBottomRow.addView(resetEditorBtn)
        editorBottomRow.addView(applyEditorBtn)
        spriteEditorPanel.addView(editorBottomRow)

        val openSpriteEditorBtn = Button(service).apply {
            text = "✂️ Редактор спрайта (убрать фон / ластик)"
            stylePillButton(this, "#4A148C", "#AB47BC", 8, 12f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(36)).apply {
                setMargins(0, 0, 0, dpToPx(4))
            }
            setOnClickListener {
                val b64 = node.targetImageBase64
                if (b64.isNullOrEmpty()) {
                    Toast.makeText(service, "Сначала захватите образец с экрана или загрузите картинку!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (spriteEditorPanel.visibility == View.VISIBLE) {
                    spriteEditorPanel.visibility = View.GONE
                } else {
                    try {
                        val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                        val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (decoded == null || decoded.width <= 0 || decoded.height <= 0) {
                            Toast.makeText(service, "Не удалось загрузить картинку для редактора", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        val argbBmp = if (decoded.config == android.graphics.Bitmap.Config.ARGB_8888) {
                            decoded
                        } else {
                            val copy = decoded.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                            decoded.recycle()
                            copy
                        }
                        editW = argbBmp.width
                        editH = argbBmp.height
                        editPixels = IntArray(editW * editH)
                        argbBmp.getPixels(editPixels, 0, editW, 0, 0, editW, editH)
                        argbBmp.recycle()

                        origW = editW
                        origH = editH
                        origPixels = editPixels.clone()
                        undoStack.clear()
                        updateEditorStats()

                        saveSpritePanel.visibility = View.GONE
                        spriteLibraryListPanel.visibility = View.GONE
                        spriteEditorPanel.visibility = View.VISIBLE
                        spriteCanvasView.invalidate()
                    } catch (e: Exception) {
                        Toast.makeText(service, "Ошибка открытия редактора спрайта", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val testImgSearchBtn = Button(service).apply {
            text = "🎯 Тест поиска картинки / сравнения зон"
            stylePillButton(this, "#00695C", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(36))
            setOnClickListener {
                updateNodeScreenPosition(node)
                node.colorTolerance = colorTolEdit.text.toString().toIntOrNull() ?: 15
                node.imageThreshold = imgThresholdEdit.text.toString().toFloatOrNull() ?: 80f
                node.searchRadius = searchRadiusEdit.text.toString().toIntOrNull() ?: 0
                node.checkResolutionScale = (resolutionScaleBar.progress + 10) / 100f
                node.imageFullScreenSearch = imageFullScreenSearchCheck.isChecked
                node.imageClickFoundCenter = imageClickFoundCenterCheck.isChecked
                node.imageAdaptiveScale = imageAdaptiveScaleCheck.isChecked
                node.imageScaleRangePct = (imageScaleRangeEdit.text.toString().toIntOrNull() ?: 20).coerceIn(5, 50)
                node.imageMultiTarget = imageMultiTargetCheck.isChecked
                node.imageMultiRandomOrder = imageMultiRandomOrderCheck.isChecked
                node.imageMultiMaxTargets = (imageMultiMaxTargetsEdit.text.toString().toIntOrNull() ?: 0).coerceAtLeast(0)
                node.imageMultiClickDelayMs = (imageMultiClickDelayEdit.text.toString().toLongOrNull() ?: 120L).coerceAtLeast(0L)
                node.imageMultiRandomDelayMs = (imageMultiRandomDelayEdit.text.toString().toLongOrNull() ?: 40L).coerceAtLeast(0L)
                node.compareZoneEnabled = compareTwoRegionsCheck.isChecked

                modMenu?.visibility = View.INVISIBLE
                linesOverlayView?.visibility = View.INVISIBLE
                textZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                textZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                compareZoneStartViews.values.forEach { it.visibility = View.INVISIBLE }
                compareZoneEndViews.values.forEach { it.visibility = View.INVISIBLE }
                colorCompareViews.values.forEach { it.visibility = View.INVISIBLE }
                nodeViews.values.forEach { it.visibility = View.INVISIBLE }

                service.handler.postDelayed({
                    service.testImageFragmentSearch(node)
                    service.handler.postDelayed({
                        modMenu?.visibility = View.VISIBLE
                        linesOverlayView?.visibility = View.VISIBLE
                        textZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        textZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        compareZoneStartViews.values.forEach { it.visibility = View.VISIBLE }
                        compareZoneEndViews.values.forEach { it.visibility = View.VISIBLE }
                        colorCompareViews.values.forEach { it.visibility = View.VISIBLE }
                        nodeViews.values.forEach { it.visibility = View.VISIBLE }
                    }, 150)
                }, 250)
            }
        }

        // --- SECTION: Жест (Свайп / Удержание / Синхронизация) ---
        val syncLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dpToPx(4), 0, dpToPx(4)) }
        syncLayout.addView(TextView(service).apply { text = "Нажать одновременно с шагами (через запятую):"; setTextColor(Color.parseColor("#B0BEC5")); setScaledTextSize(12f) })
        val syncEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(node.syncWithNodeIds)
            hint = "Например: 2, 3"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            setScaledTextSize(12.5f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        syncLayout.addView(syncEdit)

        val swipeLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dpToPx(6), 0, dpToPx(6)); gravity = Gravity.CENTER_VERTICAL }
        val (holdDurRow, getHoldDurRowMs) = createTimeInputRow("Длит. зажатия (в фоне):", node.holdDurationMs)
        val holdHintTv = TextView(service).apply {
            text = "💡 Метка зажимает экран на указанное время, а макрос сразу продолжает выполнять следующие шаги"
            setTextColor(Color.parseColor("#80CBC4"))
            setScaledTextSize(11f)
            setPadding(0, 0, 0, dpToPx(6))
            visibility = if (node.isHold && !node.isSwipe) View.VISIBLE else View.GONE
        }
        holdDurRow.visibility = if (node.isHold && !node.isSwipe) View.VISIBLE else View.GONE

        lateinit var holdBtnRef: Button
        val swipeBtn = Button(service).apply {
            text = if (node.isSwipe) "↗ Свайп: ВКЛ" else "↗ Свайп: ВЫКЛ"
            stylePillButton(this, if (node.isSwipe) "#1976D2" else "#283246", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
            setOnClickListener {
                node.isSwipe = !node.isSwipe
                if (node.isSwipe) {
                    node.isHold = false
                    holdBtnRef.text = "✋ Удержание: ВЫКЛ"
                    stylePillButton(holdBtnRef, "#283246", null, 8, 12f)
                    holdDurRow.visibility = View.GONE
                    holdHintTv.visibility = View.GONE
                    if (node.swipeTargetNodeId == null && node.swipePathPoints.isEmpty()) {
                        createSwipeEndMarker(node)
                    }
                } else {
                    removeSwipeEndMarker(node.id)
                }
                text = if (node.isSwipe) "↗ Свайп: ВКЛ" else "↗ Свайп: ВЫКЛ"
                stylePillButton(this, if (node.isSwipe) "#1976D2" else "#283246", null, 8, 12f)
                invalidateLines()
            }
        }
        val holdBtn = Button(service).apply {
            text = if (node.isHold && !node.isSwipe) "✋ Удержание: ВКЛ" else "✋ Удержание: ВЫКЛ"
            stylePillButton(this, if (node.isHold && !node.isSwipe) "#00796B" else "#283246", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(dpToPx(4), 0, 0, 0)
            }
            setOnClickListener {
                node.isHold = !node.isHold
                if (node.isHold) {
                    node.isSwipe = false
                    swipeBtn.text = "↗ Свайп: ВЫКЛ"
                    stylePillButton(swipeBtn, "#283246", null, 8, 12f)
                    removeSwipeEndMarker(node.id)
                }
                text = if (node.isHold) "✋ Удержание: ВКЛ" else "✋ Удержание: ВЫКЛ"
                stylePillButton(this, if (node.isHold) "#00796B" else "#283246", null, 8, 12f)
                holdDurRow.visibility = if (node.isHold) View.VISIBLE else View.GONE
                holdHintTv.visibility = if (node.isHold) View.VISIBLE else View.GONE
                invalidateLines()
            }
        }
        holdBtnRef = holdBtn
        swipeLayout.addView(swipeBtn)
        swipeLayout.addView(holdBtn)
        
        val (swipeDurRow, getSwipeDurRowMs) = createTimeInputRow("Длительность свайпа:", node.swipeDurationMs)
        
        val swipeDeltaLayout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dpToPx(6), 0, 0) }
        val modeSpinnerLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        modeSpinnerLayout.addView(TextView(service).apply { text = "Тип траектории: "; setTextColor(Color.WHITE); setScaledTextSize(12.5f) })
        val modeSpinner = android.widget.Spinner(service).apply {
            adapter = createThemedSpinnerAdapter(arrayOf("Прямая к суб-метке", "Прямая к шагу #ID", "Кривая (Нарисовать пальцем)"))
            setSelection(if (node.swipePathPoints.isNotEmpty()) 2 else if (node.swipeTargetNodeId != null) 1 else 0)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        modeSpinnerLayout.addView(modeSpinner)
        swipeDeltaLayout.addView(modeSpinnerLayout)

        val swipeTargetEditLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 5, 0, 0); gravity = Gravity.CENTER_VERTICAL }
        swipeTargetEditLayout.addView(TextView(service).apply { text = "Свайп к шагу #: "; setTextColor(Color.WHITE); setScaledTextSize(12.5f) })
        val swipeTargetEdit = EditText(service).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(node.swipeTargetNodeId?.toString() ?: "")
            hint = "ID"
            setHintTextColor(Color.parseColor("#78909C"))
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dpToPx(80), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        swipeTargetEditLayout.addView(swipeTargetEdit)
        swipeDeltaLayout.addView(swipeTargetEditLayout)

        val customPathLayout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(6), 0, dpToPx(4))
        }
        val customPathInfoTv = TextView(service).apply {
            text = if (node.swipePathPoints.size >= 2) {
                "✅ Записана траектория: ${node.swipePathPoints.size} точек (${node.swipeDurationMs} мс)"
            } else {
                "Траектория не записана (нажмите кнопку ниже и проведите пальцем по экрану)"
            }
            setTextColor(if (node.swipePathPoints.size >= 2) Color.parseColor("#69F0AE") else Color.parseColor("#B0BEC5"))
            setScaledTextSize(11f)
            setPadding(0, 0, 0, dpToPx(4))
        }
        val recordSwipePathBtn = Button(service).apply {
            text = "✍️ Нарисовать траекторию пальцем на экране"
            stylePillButton(this, "#00838F", null, 8, 12f)
            layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(36))
            setOnClickListener {
                modMenu?.visibility = View.GONE
                service.startSingleSwipeRecording(node) {
                    modMenu?.visibility = View.VISIBLE
                    showEditNodeMenu(node)
                }
            }
        }
        customPathLayout.addView(customPathInfoTv)
        customPathLayout.addView(recordSwipePathBtn)
        swipeDeltaLayout.addView(customPathLayout)
        
        modeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: android.widget.AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                swipeTargetEditLayout.visibility = if (pos == 1) View.VISIBLE else View.GONE
                customPathLayout.visibility = if (pos == 2) View.VISIBLE else View.GONE
                
                if (pos == 0) {
                    node.swipeTargetNodeId = null
                    node.swipePathPoints = emptyList()
                    if (node.isSwipe) createSwipeEndMarker(node)
                } else if (pos == 1) {
                    removeSwipeEndMarker(node.id)
                    node.swipePathPoints = emptyList()
                } else if (pos == 2) {
                    node.swipeTargetNodeId = null
                    if (node.isSwipe && node.swipePathPoints.isNotEmpty()) {
                        createSwipeEndMarker(node)
                    }
                }
                invalidateLines()
            }
            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }
        
        val hasSwipeChanges = node.syncWithNodeIds.isNotEmpty() || node.isSwipe || node.isHold || node.swipeTargetNodeId != null || node.swipeDurationMs != 500L
        val syncSwipeSection = addSection(
            "👆 2. Жест (Свайп / Удержание / Мультитач)",
            hasSwipeChanges,
            startExpanded = (node.isSwipe || node.isHold)
        ) { body ->
            body.addView(swipeLayout)
            body.addView(holdHintTv)
            body.addView(holdDurRow)
            body.addView(swipeDurRow)
            body.addView(swipeDeltaLayout)
            body.addView(syncLayout)
        }

        val hasLogicChanges = node.targetColor != null || node.targetImageBase64 != null || node.targetText != null || node.colorOperator != "==" || node.colorTolerance != 15 || node.linkedConditionNodeId != null || node.compareToNodeId != null || node.dynamicColorUpdate || node.triggerMode >= 0
        val logicSection = addSection(
            "👁 3. Условие срабатывания (Цвет / Картинка / Текст)",
            hasLogicChanges,
            startExpanded = (node.type == NodeType.CHECK_COLOR || node.triggerMode != -1)
        ) { body ->
            body.addView(triggerModeLayout)
            
            if (node.triggerMode == 0) { // Color Pixel
                body.addView(colorBehaviorLayout)
                body.addView(colorOpLayout)
                body.addView(colorToleranceLayout)
                if (node.dynamicColorUpdate) {
                    body.addView(dynamicLookbackRow)
                }
                
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
                body.addView(colorToleranceLayout)
                body.addView(fragZoneBtn)
                body.addView(compareTwoRegionsCheck)
                body.addView(compareZoneBtn)
                body.addView(imageFullScreenSearchCheck)
                body.addView(imageClickFoundCenterCheck)
                body.addView(imageAdaptiveScaleCheck)
                body.addView(adaptiveScaleSettingsLayout)
                body.addView(imageMultiTargetCheck)
                body.addView(multiTargetSettingsLayout)
                body.addView(imgThresholdLayout)
                body.addView(searchRadiusLayout)
                body.addView(resolutionScaleLayout)
                body.addView(imgPreview)
                body.addView(capImgBtn)
                body.addView(importImgBtn)
                body.addView(openSpriteEditorBtn)
                body.addView(spriteEditorPanel)
                body.addView(spriteLibraryBtnRow)
                body.addView(saveSpritePanel)
                body.addView(spriteLibraryListPanel)
                body.addView(testImgSearchBtn)
            } else if (node.triggerMode == 2) { // Text Recognition
                body.addView(colorOpLayout)
                body.addView(textLangLayout)
                body.addView(compareTwoRegionsCheck)
                body.addView(textTargetLayout)
                body.addView(ocrFullscreenLayout)
                body.addView(smartOcrLayout)
                body.addView(fragZoneBtn)
                body.addView(compareZoneBtn)
                body.addView(imgPreview)
                body.addView(capImgBtn)
            }
        }

        val hasRoutingChanges = node.nextNodeIdOnSuccess != null || node.nextNodeIdOnFail != null || node.maxCheckCycles != null
        val routingSection = addSection(
            "🛣 4. Переходы (Куда идти после шага)",
            hasRoutingChanges,
            startExpanded = hasRoutingChanges
        ) { body ->
            body.addView(successLayout)
            if (node.triggerMode != -1) {
                body.addView(failLayout)
                body.addView(cycleLimitLayout)
            }
        }

        val hasAntiDetect = node.randomizeDelayMs > 0 || node.randomizeRadius > 0
        val antiDetectSection = addSection(
            "🛡 5. Анти-Детект (Случайный разброс)",
            hasAntiDetect,
            startExpanded = false
        ) { body ->
            body.addView(randomDelayRow)
            body.addView(randRadiusRow)
            body.addView(applyAntiDetectBtn)
        }

        val defaultCrosshairColor = if (node.type == NodeType.CLICK) Color.RED else if (node.type == NodeType.MANAGER) Color.parseColor("#9C27B0") else Color.BLUE
        val defaultNumberColor = if (node.type == NodeType.CLICK) Color.WHITE else Color.YELLOW
        val hasAppearanceChanges = node.sizeScale != 1.0f || node.crosshairColor != defaultCrosshairColor || node.numberColor != defaultNumberColor
        addSection(
            "🎨 6. Внешний вид метки (Цвет и размер)",
            hasAppearanceChanges,
            startExpanded = true
        ) { body ->
            val sizeHeaderRow = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(2), 0, dpToPx(4))
            }
            val sizeLabelTv = TextView(service).apply {
                text = "Размер метки: ${(node.sizeScale * 100).toInt()}%"
                setTextColor(Color.WHITE)
                setScaledTextSize(12.5f)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            }
            lateinit var sizeSeekBar: android.widget.SeekBar
            fun applyNodeScale(newScale: Float) {
                val clamped = (Math.round(newScale * 10f) / 10f).coerceIn(0.5f, 2.5f)
                node.sizeScale = clamped
                sizeLabelTv.text = "Размер метки: ${(clamped * 100).toInt()}%"
                sizeSeekBar.progress = ((clamped - 0.5f) * 10f).toInt().coerceIn(0, 20)
                nodeViews[node.id]?.invalidate()
                swipeEndViews[node.id]?.invalidate()
                colorCompareViews[node.id]?.invalidate()
                invalidateLines()
            }
            sizeHeaderRow.addView(sizeLabelTv)
            sizeHeaderRow.addView(Button(service).apply {
                text = "−"
                stylePillButton(this, "#283246", null, 8, 14f)
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30)).apply { setMargins(0, 0, dpToPx(4), 0) }
                setOnClickListener { applyNodeScale(node.sizeScale - 0.1f) }
            })
            sizeHeaderRow.addView(Button(service).apply {
                text = "+"
                stylePillButton(this, "#283246", null, 8, 14f)
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(30))
                setOnClickListener { applyNodeScale(node.sizeScale + 0.1f) }
            })
            body.addView(sizeHeaderRow)

            sizeSeekBar = android.widget.SeekBar(service).apply {
                max = 20 // 0.5x .. 2.5x (step 0.1)
                progress = ((node.sizeScale - 0.5f) * 10f).toInt().coerceIn(0, 20)
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(8))
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) {
                            val sc = 0.5f + p * 0.1f
                            node.sizeScale = sc
                            sizeLabelTv.text = "Размер метки: ${(sc * 100).toInt()}%"
                            nodeViews[node.id]?.invalidate()
                            swipeEndViews[node.id]?.invalidate()
                            colorCompareViews[node.id]?.invalidate()
                            invalidateLines()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
                })
            }
            body.addView(sizeSeekBar)

            body.addView(createColorRow("Цвет прицела:", node.crosshairColor) { c ->
                node.crosshairColor = c
                nodeViews[node.id]?.invalidate()
                invalidateLines()
            })

            body.addView(createColorRow("Цвет номера метки:", node.numberColor) { c ->
                node.numberColor = c
                nodeViews[node.id]?.invalidate()
            })

            body.addView(Button(service).apply {
                text = "🎨 Применить этот размер и цвета ко всем меткам"
                stylePillButton(this, "#263238", "#3E506B", 8, 11.5f)
                layoutParams = LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dpToPx(34)).apply {
                    setMargins(0, dpToPx(6), 0, 0)
                }
                setOnClickListener {
                    service.nodes.forEach { other ->
                        other.sizeScale = node.sizeScale
                        other.crosshairColor = node.crosshairColor
                        other.numberColor = node.numberColor
                        nodeViews[other.id]?.invalidate()
                        swipeEndViews[other.id]?.invalidate()
                        colorCompareViews[other.id]?.invalidate()
                    }
                    invalidateLines()
                    service.autoSave()
                    Toast.makeText(service, "Внешний вид применён ко всем меткам (${service.nodes.size})", Toast.LENGTH_SHORT).show()
                }
            })
        }

        if (appMode != AppMode.ADVANCED) {
            successLayout.visibility = View.GONE
            failLayout.visibility = View.GONE
            cycleLimitLayout.visibility = View.GONE
        }

        if (appMode == AppMode.SEQUENTIAL && node.type == NodeType.CLICK) {
            logicSection.visibility = View.GONE
            routingSection.visibility = View.GONE
        }

        if (node.type == NodeType.CHECK_COLOR || node.type == NodeType.MACRO || node.type == NodeType.MANAGER) {
            swipeLayout.visibility = View.GONE
            holdHintTv.visibility = View.GONE
            holdDurRow.visibility = View.GONE
            swipeDurRow.visibility = View.GONE
            swipeDeltaLayout.visibility = View.GONE
            clickDurRow.visibility = View.GONE
            antiDetectSection.visibility = View.GONE
            syncSwipeSection.visibility = View.GONE
        }
        if (node.type == NodeType.MANAGER) {
            timingsSection.visibility = View.GONE
            logicSection.visibility = View.GONE
            routingSection.visibility = View.GONE
            macroSection.visibility = View.GONE
            antiDetectSection.visibility = View.GONE
            syncSwipeSection.visibility = View.GONE
        }

        val saveAndExit = {
            node.clickDurationMs = getClickDurRowMs()
            node.holdDurationMs = getHoldDurRowMs()
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
            node.dynamicColorLookbackMs = getDynamicLookbackMs()
            node.imageThreshold = imgThresholdEdit.text.toString().toFloatOrNull() ?: 80f
            node.searchRadius = searchRadiusEdit.text.toString().toIntOrNull() ?: 0
            node.checkResolutionScale = (resolutionScaleBar.progress + 10) / 100f
            node.imageFullScreenSearch = imageFullScreenSearchCheck.isChecked
            node.imageClickFoundCenter = imageClickFoundCenterCheck.isChecked
            node.imageAdaptiveScale = imageAdaptiveScaleCheck.isChecked
            node.imageScaleRangePct = (imageScaleRangeEdit.text.toString().toIntOrNull() ?: 20).coerceIn(5, 50)
            node.imageMultiTarget = imageMultiTargetCheck.isChecked
            node.imageMultiRandomOrder = imageMultiRandomOrderCheck.isChecked
            node.imageMultiMaxTargets = (imageMultiMaxTargetsEdit.text.toString().toIntOrNull() ?: 0).coerceAtLeast(0)
            node.imageMultiClickDelayMs = (imageMultiClickDelayEdit.text.toString().toLongOrNull() ?: 120L).coerceAtLeast(0L)
            node.imageMultiRandomDelayMs = (imageMultiRandomDelayEdit.text.toString().toLongOrNull() ?: 40L).coerceAtLeast(0L)
            node.targetText = textTargetEdit.text.toString().takeIf { it.isNotEmpty() }
            node.targetLanguage = if (textLangSpinner.selectedItemPosition == 1) "eng" else if (textLangSpinner.selectedItemPosition == 2) "rus+eng" else "rus"
            node.ocrFullScreenClick = ocrFullscreenCheck.isChecked
            node.ocrOperator = arrayOf("==", "!=", ">", "<", ">=", "<=")[smartOcrOpSpinner.selectedItemPosition]
            val selPos = ocrCompareTargetSpinner.selectedItemPosition
            if (node.triggerMode == 2 && node.isSmartOcr) {
                node.compareZoneEnabled = (selPos == 1)
                node.ocrCompareToNodeId = if (selPos >= 2) availableOcrNodes[selPos - 2].id else null
            } else {
                node.compareZoneEnabled = compareTwoRegionsCheck.isChecked
                node.ocrCompareToNodeId = null
            }
            node.ocrTargetValue = smartOcrValEdit.text.toString().toDoubleOrNull() ?: 0.0
            node.ocrCustomSuffixes = smartOcrSufEdit.text.toString()
            if (node.ocrSuffixSetName.isNotEmpty()) {
                service.saveSuffixSet(node.ocrSuffixSetName, node.ocrCustomSuffixes)
            }
            node.syncWithNodeIds = syncEdit.text.toString().filter { it.isDigit() || it == ',' }
            node.swipeDurationMs = getSwipeDurRowMs()
            node.swipeTargetNodeId = swipeTargetEdit.text.toString().toIntOrNull()
            invalidateLines()
            service.autoSave()
            showMainMenu()
        }

        backQuickBtn.setOnClickListener { saveAndExit() }

        // --- SAVE BUTTON ---
        val saveBtn = Button(service).apply {
            text = "✓ Сохранить и вернуться к списку"
            stylePillButton(this, uiAccentColor, null, 10, 13.5f)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(42)).apply {
                setMargins(0, dpToPx(10), 0, dpToPx(4))
            }
            setOnClickListener { saveAndExit() }
        }
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

    fun createCompareZoneMarkers(node: TargetNode) {
        if (compareZoneStartViews.containsKey(node.id)) return

        if (node.compareZoneStartX == 0 && node.compareZoneStartY == 0 && node.compareZoneEndX == 0 && node.compareZoneEndY == 0) {
            node.compareZoneStartX = (node.x - dpToPx(50)).coerceAtLeast(dpToPx(20))
            node.compareZoneStartY = (node.y + dpToPx(65)).coerceAtLeast(dpToPx(20))
            node.compareZoneEndX = node.x + dpToPx(50)
            node.compareZoneEndY = node.y + dpToPx(135)
        }

        val markerSize = dpToPx(36)
        val half = dpToPx(18)

        // --- Start Marker (Zone 2) ---
        val startView = ZoneCornerView(service, node, isZone2 = true, isStart = true)
        val startParams = WindowManager.LayoutParams(
            markerSize,
            markerSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.compareZoneStartX - half
            y = node.compareZoneStartY - half
        }

        startView.visibility = if (node.isVisible && !service.isPlaying) View.VISIBLE else View.GONE
        setupCompareZoneTouchListener(startView, startParams, node, true)
        windowManager.addView(startView, startParams)
        compareZoneStartViews[node.id] = startView
        compareZoneStartParams[node.id] = startParams

        // --- End Marker (Zone 2) ---
        val endView = ZoneCornerView(service, node, isZone2 = true, isStart = false)
        val endParams = WindowManager.LayoutParams(
            markerSize,
            markerSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.compareZoneEndX - half
            y = node.compareZoneEndY - half
        }

        endView.visibility = if (node.isVisible && !service.isPlaying) View.VISIBLE else View.GONE
        setupCompareZoneTouchListener(endView, endParams, node, false)
        windowManager.addView(endView, endParams)
        compareZoneEndViews[node.id] = endView
        compareZoneEndParams[node.id] = endParams
        invalidateLines()
    }

    fun removeCompareZoneMarkers(id: Int) {
        val startV = compareZoneStartViews.remove(id)
        if (startV != null) try { windowManager.removeView(startV) } catch (_: Exception) {}
        compareZoneStartParams.remove(id)

        val endV = compareZoneEndViews.remove(id)
        if (endV != null) try { windowManager.removeView(endV) } catch (_: Exception) {}
        compareZoneEndParams.remove(id)
        if (zoneNudgeNodeId == id && zoneNudgeIsZone2) {
            hideZoneNudgePopup()
        }
        invalidateLines()
    }

    fun setupCompareZoneTouchListener(view: View, params: WindowManager.LayoutParams, node: TargetNode, isStart: Boolean) {
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    val half = dpToPx(18)
                    var targetX = initialX + half + (event.rawX - initialTouchX).toInt()
                    var targetY = initialY + half + (event.rawY - initialTouchY).toInt()
                    targetX = targetX.coerceIn(0, screenWidth)
                    targetY = targetY.coerceIn(0, screenHeight)
                    params.x = targetX - half
                    params.y = targetY - half
                    windowManager.updateViewLayout(view, params)
                    if (isStart) {
                        node.compareZoneStartX = targetX
                        node.compareZoneStartY = targetY
                    } else {
                        node.compareZoneEndX = targetX
                        node.compareZoneEndY = targetY
                    }
                    updateZoneNudgePopupPosition(node, isZone2 = true)
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val half = dpToPx(18)
                    val targetX = (params.x + half).coerceIn(0, getScreenDimensions().first)
                    val targetY = (params.y + half).coerceIn(0, getScreenDimensions().second)
                    if (isStart) {
                        node.compareZoneStartX = targetX
                        node.compareZoneStartY = targetY
                    } else {
                        node.compareZoneEndX = targetX
                        node.compareZoneEndY = targetY
                    }
                    val movedDist = Math.abs(event.rawX - initialTouchX) + Math.abs(event.rawY - initialTouchY)
                    if (movedDist < dpToPx(10)) {
                        // Тап по маркеру открывает стрелочки точечного сдвига прямо над рамкой
                        showZoneNudgePopup(node, isZone2 = true, initialCornerMode = if (isStart) 0 else 2)
                    } else {
                        updateZoneNudgePopupPosition(node, isZone2 = true)
                    }
                    invalidateLines()
                    service.autoSave()
                    true
                }
                else -> false
            }
        }
    }

    fun createTextZoneMarkers(node: TargetNode) {
        if (textZoneStartViews.containsKey(node.id)) return

        if (node.textZoneStartX == 0 && node.textZoneStartY == 0 && node.textZoneEndX == 0 && node.textZoneEndY == 0) {
            node.textZoneStartX = node.x - dpToPx(50)
            node.textZoneStartY = node.y - dpToPx(50)
            node.textZoneEndX = node.x + dpToPx(50)
            node.textZoneEndY = node.y + dpToPx(50)
        }

        val markerSize = dpToPx(36)
        val half = dpToPx(18)

        // --- Start Marker (Zone 1) ---
        val startView = ZoneCornerView(service, node, isZone2 = false, isStart = true)
        val startParams = WindowManager.LayoutParams(
            markerSize,
            markerSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.textZoneStartX - half
            y = node.textZoneStartY - half
        }

        startView.visibility = if (node.isVisible && !service.isPlaying) View.VISIBLE else View.GONE
        setupTextZoneTouchListener(startView, startParams, node, true)
        windowManager.addView(startView, startParams)
        textZoneStartViews[node.id] = startView
        textZoneStartParams[node.id] = startParams

        // --- End Marker (Zone 1) ---
        val endView = ZoneCornerView(service, node, isZone2 = false, isStart = false)
        val endParams = WindowManager.LayoutParams(
            markerSize,
            markerSize,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = node.textZoneEndX - half
            y = node.textZoneEndY - half
        }

        endView.visibility = if (node.isVisible && !service.isPlaying) View.VISIBLE else View.GONE
        setupTextZoneTouchListener(endView, endParams, node, false)
        windowManager.addView(endView, endParams)
        textZoneEndViews[node.id] = endView
        textZoneEndParams[node.id] = endParams
        invalidateLines()
    }

    fun removeTextZoneMarkers(id: Int) {
        val startV = textZoneStartViews.remove(id)
        if (startV != null) try { windowManager.removeView(startV) } catch (_: Exception) {}
        textZoneStartParams.remove(id)

        val endV = textZoneEndViews.remove(id)
        if (endV != null) try { windowManager.removeView(endV) } catch (_: Exception) {}
        textZoneEndParams.remove(id)
        if (zoneNudgeNodeId == id && !zoneNudgeIsZone2) {
            hideZoneNudgePopup()
        }
        invalidateLines()
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    val half = dpToPx(18)
                    var targetX = initialX + half + (event.rawX - initialTouchX).toInt()
                    var targetY = initialY + half + (event.rawY - initialTouchY).toInt()
                    targetX = targetX.coerceIn(0, screenWidth)
                    targetY = targetY.coerceIn(0, screenHeight)
                    params.x = targetX - half
                    params.y = targetY - half
                    windowManager.updateViewLayout(view, params)
                    if (isStart) {
                        node.textZoneStartX = targetX
                        node.textZoneStartY = targetY
                    } else {
                        node.textZoneEndX = targetX
                        node.textZoneEndY = targetY
                    }
                    updateZoneNudgePopupPosition(node, isZone2 = false)
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val half = dpToPx(18)
                    val targetX = (params.x + half).coerceIn(0, getScreenDimensions().first)
                    val targetY = (params.y + half).coerceIn(0, getScreenDimensions().second)
                    if (isStart) {
                        node.textZoneStartX = targetX
                        node.textZoneStartY = targetY
                    } else {
                        node.textZoneEndX = targetX
                        node.textZoneEndY = targetY
                    }
                    val movedDist = Math.abs(event.rawX - initialTouchX) + Math.abs(event.rawY - initialTouchY)
                    if (movedDist < dpToPx(10)) {
                        // Тап по маркеру открывает стрелочки точечного сдвига прямо над рамкой
                        showZoneNudgePopup(node, isZone2 = false, initialCornerMode = if (isStart) 0 else 2)
                    } else {
                        updateZoneNudgePopupPosition(node, isZone2 = false)
                    }
                    invalidateLines()
                    service.autoSave()
                    true
                }
                else -> false
            }
        }
    }

    private fun refreshZoneCornerSelectionHighlights() {
        textZoneStartViews.forEach { (id, v) ->
            (v as? ZoneCornerView)?.apply {
                isSelectedCorner = (zoneNudgePopupView != null && zoneNudgeNodeId == id && !zoneNudgeIsZone2 && (zoneNudgeCornerMode == 0 || zoneNudgeCornerMode == 1))
                invalidate()
            }
        }
        textZoneEndViews.forEach { (id, v) ->
            (v as? ZoneCornerView)?.apply {
                isSelectedCorner = (zoneNudgePopupView != null && zoneNudgeNodeId == id && !zoneNudgeIsZone2 && (zoneNudgeCornerMode == 2 || zoneNudgeCornerMode == 1))
                invalidate()
            }
        }
        compareZoneStartViews.forEach { (id, v) ->
            (v as? ZoneCornerView)?.apply {
                isSelectedCorner = (zoneNudgePopupView != null && zoneNudgeNodeId == id && zoneNudgeIsZone2 && (zoneNudgeCornerMode == 0 || zoneNudgeCornerMode == 1))
                invalidate()
            }
        }
        compareZoneEndViews.forEach { (id, v) ->
            (v as? ZoneCornerView)?.apply {
                isSelectedCorner = (zoneNudgePopupView != null && zoneNudgeNodeId == id && zoneNudgeIsZone2 && (zoneNudgeCornerMode == 2 || zoneNudgeCornerMode == 1))
                invalidate()
            }
        }
    }

    fun hideZoneNudgePopup() {
        zoneNudgePopupView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        zoneNudgePopupView = null
        zoneNudgeParams = null
        zoneNudgeNodeId = null
        zoneNudgeSizeTextView = null
        refreshZoneCornerSelectionHighlights()
    }

    private fun syncZoneMarkerPositions(node: TargetNode, isZone2: Boolean) {
        val half = dpToPx(18)
        if (isZone2) {
            val sv = compareZoneStartViews[node.id]
            val sp = compareZoneStartParams[node.id]
            if (sv != null && sp != null) {
                sp.x = node.compareZoneStartX - half
                sp.y = node.compareZoneStartY - half
                try { windowManager.updateViewLayout(sv, sp) } catch (_: Exception) {}
            }
            val ev = compareZoneEndViews[node.id]
            val ep = compareZoneEndParams[node.id]
            if (ev != null && ep != null) {
                ep.x = node.compareZoneEndX - half
                ep.y = node.compareZoneEndY - half
                try { windowManager.updateViewLayout(ev, ep) } catch (_: Exception) {}
            }
        } else {
            val sv = textZoneStartViews[node.id]
            val sp = textZoneStartParams[node.id]
            if (sv != null && sp != null) {
                sp.x = node.textZoneStartX - half
                sp.y = node.textZoneStartY - half
                try { windowManager.updateViewLayout(sv, sp) } catch (_: Exception) {}
            }
            val ev = textZoneEndViews[node.id]
            val ep = textZoneEndParams[node.id]
            if (ev != null && ep != null) {
                ep.x = node.textZoneEndX - half
                ep.y = node.textZoneEndY - half
                try { windowManager.updateViewLayout(ev, ep) } catch (_: Exception) {}
            }
        }
    }

    private fun updateZoneNudgePopupPosition(node: TargetNode, isZone2: Boolean) {
        if (zoneNudgePopupView == null || zoneNudgeNodeId != node.id || zoneNudgeIsZone2 != isZone2) return
        val params = zoneNudgeParams ?: return
        val (screenW, screenH) = getScreenDimensions()
        val sx = if (isZone2) node.compareZoneStartX else node.textZoneStartX
        val sy = if (isZone2) node.compareZoneStartY else node.textZoneStartY
        val ex = if (isZone2) node.compareZoneEndX else node.textZoneEndX
        val ey = if (isZone2) node.compareZoneEndY else node.textZoneEndY

        val left = minOf(sx, ex)
        val top = minOf(sy, ey)
        val right = maxOf(sx, ex)
        val bottom = maxOf(sy, ey)
        val w = (right - left).coerceAtLeast(0)
        val h = (bottom - top).coerceAtLeast(0)

        zoneNudgeSizeTextView?.text = "Зона ${if (isZone2) 2 else 1}: ${w}×${h} px"

        val popupW = dpToPx(246)
        val popupH = dpToPx(74)
        val centerX = (left + right) / 2
        params.x = (centerX - popupW / 2).coerceIn(dpToPx(6), (screenW - popupW - dpToPx(6)).coerceAtLeast(dpToPx(6)))
        val aboveY = top - popupH - dpToPx(20)
        params.y = if (aboveY >= dpToPx(24)) {
            aboveY
        } else {
            (bottom + dpToPx(22)).coerceAtMost((screenH - popupH - dpToPx(8)).coerceAtLeast(dpToPx(24)))
        }
        try { windowManager.updateViewLayout(zoneNudgePopupView, params) } catch (_: Exception) {}
    }

    fun showZoneNudgePopup(node: TargetNode, isZone2: Boolean, initialCornerMode: Int = 1) {
        hideZoneNudgePopup()
        zoneNudgeNodeId = node.id
        zoneNudgeIsZone2 = isZone2
        zoneNudgeCornerMode = initialCornerMode

        val container = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#EC101722"))
                cornerRadius = dpToPx(12).toFloat()
                setStroke(dpToPx(1), if (isZone2) Color.parseColor("#00E5FF") else Color.parseColor("#00E676"))
            }
        }

        // Верхняя строка: выбор угла / всей рамки + размер + закрыть
        val topRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(4))
        }

        lateinit var modeBtn0: Button
        lateinit var modeBtn1: Button
        lateinit var modeBtn2: Button

        fun updateModeButtonsStyle() {
            val activeBg = if (isZone2) "#00838F" else "#1B5E20"
            val activeStroke = if (isZone2) "#00E5FF" else "#00E676"
            stylePillButton(modeBtn0, if (zoneNudgeCornerMode == 0) activeBg else "#1E293B", if (zoneNudgeCornerMode == 0) activeStroke else null, 6, 10f)
            stylePillButton(modeBtn1, if (zoneNudgeCornerMode == 1) activeBg else "#1E293B", if (zoneNudgeCornerMode == 1) activeStroke else null, 6, 10f)
            stylePillButton(modeBtn2, if (zoneNudgeCornerMode == 2) activeBg else "#1E293B", if (zoneNudgeCornerMode == 2) activeStroke else null, 6, 10f)
            refreshZoneCornerSelectionHighlights()
        }

        modeBtn0 = Button(service).apply {
            text = "↖ Угол 1"
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(24), 1f).apply { setMargins(0, 0, dpToPx(3), 0) }
            setOnClickListener {
                zoneNudgeCornerMode = 0
                updateModeButtonsStyle()
            }
        }
        modeBtn1 = Button(service).apply {
            text = "✥ Вся"
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(24), 0.9f).apply { setMargins(0, 0, dpToPx(3), 0) }
            setOnClickListener {
                zoneNudgeCornerMode = 1
                updateModeButtonsStyle()
            }
        }
        modeBtn2 = Button(service).apply {
            text = "↘ Угол 2"
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(24), 1f).apply { setMargins(0, 0, dpToPx(4), 0) }
            setOnClickListener {
                zoneNudgeCornerMode = 2
                updateModeButtonsStyle()
            }
        }

        val closeBtn = Button(service).apply {
            text = "✕"
            stylePillButton(this, "#3B1E22", null, 6, 10f)
            setTextColor(Color.parseColor("#FF8A80"))
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
            setOnClickListener { hideZoneNudgePopup() }
        }

        topRow.addView(modeBtn0)
        topRow.addView(modeBtn1)
        topRow.addView(modeBtn2)
        topRow.addView(closeBtn)
        container.addView(topRow)

        // Нижняя строка: стрелочки ◀ ▲ ▼ ▶ + шаг (1px / 5px / 15px) + размер
        val arrowsRow = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        fun nudgeZone(dx: Int, dy: Int) {
            val (screenW, screenH) = getScreenDimensions()
            val stepDx = dx * zoneNudgeStepPx
            val stepDy = dy * zoneNudgeStepPx
            if (isZone2) {
                when (zoneNudgeCornerMode) {
                    0 -> {
                        node.compareZoneStartX = (node.compareZoneStartX + stepDx).coerceIn(0, screenW)
                        node.compareZoneStartY = (node.compareZoneStartY + stepDy).coerceIn(0, screenH)
                    }
                    2 -> {
                        node.compareZoneEndX = (node.compareZoneEndX + stepDx).coerceIn(0, screenW)
                        node.compareZoneEndY = (node.compareZoneEndY + stepDy).coerceIn(0, screenH)
                    }
                    else -> {
                        node.compareZoneStartX = (node.compareZoneStartX + stepDx).coerceIn(0, screenW)
                        node.compareZoneStartY = (node.compareZoneStartY + stepDy).coerceIn(0, screenH)
                        node.compareZoneEndX = (node.compareZoneEndX + stepDx).coerceIn(0, screenW)
                        node.compareZoneEndY = (node.compareZoneEndY + stepDy).coerceIn(0, screenH)
                    }
                }
            } else {
                when (zoneNudgeCornerMode) {
                    0 -> {
                        node.textZoneStartX = (node.textZoneStartX + stepDx).coerceIn(0, screenW)
                        node.textZoneStartY = (node.textZoneStartY + stepDy).coerceIn(0, screenH)
                    }
                    2 -> {
                        node.textZoneEndX = (node.textZoneEndX + stepDx).coerceIn(0, screenW)
                        node.textZoneEndY = (node.textZoneEndY + stepDy).coerceIn(0, screenH)
                    }
                    else -> {
                        node.textZoneStartX = (node.textZoneStartX + stepDx).coerceIn(0, screenW)
                        node.textZoneStartY = (node.textZoneStartY + stepDy).coerceIn(0, screenH)
                        node.textZoneEndX = (node.textZoneEndX + stepDx).coerceIn(0, screenW)
                        node.textZoneEndY = (node.textZoneEndY + stepDy).coerceIn(0, screenH)
                    }
                }
            }
            syncZoneMarkerPositions(node, isZone2)
            updateZoneNudgePopupPosition(node, isZone2)
            invalidateLines()
            service.autoSave()
        }

        fun makeArrowBtn(symbol: String, dx: Int, dy: Int): Button {
            return Button(service).apply {
                text = symbol
                stylePillButton(this, "#263238", "#455A64", 7, 12f)
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(30)).apply {
                    setMargins(0, 0, dpToPx(3), 0)
                }
                setOnClickListener { nudgeZone(dx, dy) }
            }
        }

        arrowsRow.addView(makeArrowBtn("◀", -1, 0))
        arrowsRow.addView(makeArrowBtn("▲", 0, -1))
        arrowsRow.addView(makeArrowBtn("▼", 0, 1))
        arrowsRow.addView(makeArrowBtn("▶", 1, 0))

        val stepBtn = Button(service).apply {
            text = "${zoneNudgeStepPx}px"
            stylePillButton(this, "#1E293B", "#3B82F6", 7, 10.5f)
            setTextColor(Color.parseColor("#93C5FD"))
            setPadding(dpToPx(4), 0, dpToPx(4), 0)
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(30)).apply {
                setMargins(dpToPx(2), 0, dpToPx(4), 0)
            }
            setOnClickListener {
                zoneNudgeStepPx = when (zoneNudgeStepPx) {
                    1 -> 5
                    5 -> 15
                    else -> 1
                }
                text = "${zoneNudgeStepPx}px"
            }
        }
        arrowsRow.addView(stepBtn)

        val sizeTv = TextView(service).apply {
            setTextColor(Color.parseColor("#B0BEC5"))
            setScaledTextSize(9.5f)
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }
        zoneNudgeSizeTextView = sizeTv
        arrowsRow.addView(sizeTv)

        container.addView(arrowsRow)
        updateModeButtonsStyle()

        val params = WindowManager.LayoutParams(
            dpToPx(246),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        zoneNudgePopupView = container
        zoneNudgeParams = params
        try {
            windowManager.addView(container, params)
            updateZoneNudgePopupPosition(node, isZone2)
        } catch (_: Exception) {}
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    val half = dpToPx(20)
                    var targetX = initialX + half + (event.rawX - initialTouchX).toInt()
                    var targetY = initialY + half + (event.rawY - initialTouchY).toInt()
                    targetX = targetX.coerceIn(0, screenWidth)
                    targetY = targetY.coerceIn(0, screenHeight)
                    params.x = targetX - half
                    params.y = targetY - half
                    windowManager.updateViewLayout(view, params)
                    node.colorCompareX = targetX
                    node.colorCompareY = targetY
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val half = dpToPx(20)
                    val targetX = (params.x + half).coerceIn(0, getScreenDimensions().first)
                    val targetY = (params.y + half).coerceIn(0, getScreenDimensions().second)
                    node.colorCompareX = targetX
                    node.colorCompareY = targetY
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    val half = dpToPx(30)
                    var targetX = initialX + half + (event.rawX - initialTouchX).toInt()
                    var targetY = initialY + half + (event.rawY - initialTouchY).toInt()
                    targetX = targetX.coerceIn(0, screenWidth)
                    targetY = targetY.coerceIn(0, screenHeight)
                    params.x = targetX - half
                    params.y = targetY - half
                    windowManager.updateViewLayout(view, params)
                    if (targetX != node.swipeEndX || targetY != node.swipeEndY) {
                        node.swipePathPoints = emptyList() // clear since user manually altered end position
                        node.swipeEndX = targetX
                        node.swipeEndY = targetY
                    }
                    invalidateLines()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val half = dpToPx(30)
                    val targetX = (params.x + half).coerceIn(0, getScreenDimensions().first)
                    val targetY = (params.y + half).coerceIn(0, getScreenDimensions().second)
                    if (targetX != node.swipeEndX || targetY != node.swipeEndY) {
                        node.swipePathPoints = emptyList()
                        node.swipeEndX = targetX
                        node.swipeEndY = targetY
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    val half = dpToPx(30)
                    var targetX = initialX + half + (event.rawX - initialTouchX).toInt()
                    var targetY = initialY + half + (event.rawY - initialTouchY).toInt()
                    targetX = targetX.coerceIn(0, screenWidth)
                    targetY = targetY.coerceIn(0, screenHeight)
                    params.x = targetX - half
                    params.y = targetY - half
                    windowManager.updateViewLayout(view, params)
                    if (node != null) {
                        val dx = targetX - node.x
                        val dy = targetY - node.y
                        if (dx != 0 || dy != 0) {
                            node.x = targetX
                            node.y = targetY
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
                        val half = dpToPx(30)
                        val targetX = (params.x + half).coerceIn(0, getScreenDimensions().first)
                        val targetY = (params.y + half).coerceIn(0, getScreenDimensions().second)
                        node.x = targetX
                        node.y = targetY
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
                private val zoneFillPaint = Paint().apply {
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                private val zoneShadowStrokePaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = Color.parseColor("#AA000000")
                    strokeWidth = dpToPx(3).toFloat()
                    isAntiAlias = true
                }
                private val zoneStrokePaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (1.5f * resources.displayMetrics.density)
                    isAntiAlias = true
                }
                private val zoneLabelBgPaint = Paint().apply {
                    style = Paint.Style.FILL
                    color = Color.parseColor("#D9101824")
                    isAntiAlias = true
                }
                private val zoneLabelTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f * resources.displayMetrics.density
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                }
                private val pathLocCache = IntArray(2)
                private val cachePath = android.graphics.Path()
                
                override fun onDraw(canvas: Canvas) {
                    super.onDraw(canvas)
                    // Во время работы макроса не рисуем линии и рамки зон поверх экрана,
                    // чтобы они никогда не попадали на скриншоты takeScreenshot!
                    if (service.isPlaying) return
                    
                    getLocationOnScreen(pathLocCache)
                    screenOffsetX = pathLocCache[0]
                    screenOffsetY = pathLocCache[1]
                    canvas.translate(-screenOffsetX.toFloat(), -screenOffsetY.toFloat())
                    
                    val density = resources.displayMetrics.density
                    
                    for (node in service.nodes) {
                        if (!node.isVisible) continue
                        
                        if (service.showLines) {
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
                        }
                        
                        // Рамка Зоны 1 (отображается всегда, пока открыты маркеры Зоны 1)
                        if ((node.triggerMode == 1 || node.triggerMode == 2) && textZoneStartViews.containsKey(node.id)) {
                            val left = Math.min(node.textZoneStartX, node.textZoneEndX).toFloat()
                            val top = Math.min(node.textZoneStartY, node.textZoneEndY).toFloat()
                            val right = Math.max(node.textZoneStartX, node.textZoneEndX).toFloat()
                            val bottom = Math.max(node.textZoneStartY, node.textZoneEndY).toFloat()
                            val w = (right - left).toInt()
                            val h = (bottom - top).toInt()

                            zoneFillPaint.color = Color.parseColor("#1A00E676")
                            canvas.drawRect(left, top, right, bottom, zoneFillPaint)

                            zoneStrokePaint.color = Color.parseColor("#EE00E676")
                            zoneStrokePaint.pathEffect = null
                            canvas.drawRect(left, top, right, bottom, zoneShadowStrokePaint)
                            canvas.drawRect(left, top, right, bottom, zoneStrokePaint)

                            val label = "#${node.id} Зона 1: ${w}×${h}px (нажми ✚ для стрелок)"
                            val tw = zoneLabelTextPaint.measureText(label)
                            val lx = left.coerceAtLeast(4f * density)
                            val ly = if (top > 22f * density) top - 5f * density else bottom + 14f * density
                            val bgRect = android.graphics.RectF(lx - 4f * density, ly - 11f * density, lx + tw + 4f * density, ly + 3f * density)
                            canvas.drawRoundRect(bgRect, 4f * density, 4f * density, zoneLabelBgPaint)
                            zoneLabelTextPaint.color = Color.parseColor("#69F0AE")
                            canvas.drawText(label, lx, ly, zoneLabelTextPaint)
                        }

                        // Рамка Зоны 2 (отображается всегда, пока открыты маркеры Зоны 2)
                        if ((node.triggerMode == 1 || node.triggerMode == 2) && node.compareZoneEnabled && compareZoneStartViews.containsKey(node.id)) {
                            val left = Math.min(node.compareZoneStartX, node.compareZoneEndX).toFloat()
                            val top = Math.min(node.compareZoneStartY, node.compareZoneEndY).toFloat()
                            val right = Math.max(node.compareZoneStartX, node.compareZoneEndX).toFloat()
                            val bottom = Math.max(node.compareZoneStartY, node.compareZoneEndY).toFloat()
                            val w = (right - left).toInt()
                            val h = (bottom - top).toInt()

                            zoneFillPaint.color = Color.parseColor("#1A00E5FF")
                            canvas.drawRect(left, top, right, bottom, zoneFillPaint)

                            zoneStrokePaint.color = Color.parseColor("#EE00E5FF")
                            zoneStrokePaint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f * density, 6f * density), 0f)
                            canvas.drawRect(left, top, right, bottom, zoneShadowStrokePaint)
                            canvas.drawRect(left, top, right, bottom, zoneStrokePaint)

                            val label = "#${node.id} Зона 2: ${w}×${h}px (нажми ✚ для стрелок)"
                            val tw = zoneLabelTextPaint.measureText(label)
                            val lx = left.coerceAtLeast(4f * density)
                            val ly = if (top > 22f * density) top - 5f * density else bottom + 14f * density
                            val bgRect = android.graphics.RectF(lx - 4f * density, ly - 11f * density, lx + tw + 4f * density, ly + 3f * density)
                            canvas.drawRoundRect(bgRect, 4f * density, 4f * density, zoneLabelBgPaint)
                            zoneLabelTextPaint.color = Color.parseColor("#80D8FF")
                            canvas.drawText(label, lx, ly, zoneLabelTextPaint)
                        }
                        
                        // "Target ID" (leads to) logic lines
                        if (service.showLines) {
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
        val hasActiveZones = textZoneStartViews.isNotEmpty() || compareZoneStartViews.isNotEmpty()
        linesOverlayView?.visibility = if (!service.isPlaying && (service.showLines || hasActiveZones)) View.VISIBLE else View.INVISIBLE
        linesOverlayView?.invalidate()
        
        // Ensure floating bar stays on top of lines
        bringControlBarToFront()
    }

    fun addNodeAndReturn(type: NodeType, startX: Int? = null, startY: Int? = null): TargetNode {
        addNode(type, startX, startY)
        return service.nodes.last()
    }

    fun addNode(type: NodeType, startX: Int? = null, startY: Int? = null) {
        val id = nodeCounter++
        val existingScale = service.nodes.firstOrNull()?.sizeScale ?: 1.0f
        val node = TargetNode(id = id, type = type).apply {
            sizeScale = existingScale
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

        val (screenWidth, screenHeight) = getScreenDimensions()
        val params = WindowManager.LayoutParams(
            dpToPx(60), dpToPx(60),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (startX != null) startX - dpToPx(30) else (screenWidth / 2 - dpToPx(30))
            y = if (startY != null) startY - dpToPx(30) else (screenHeight / 2 - dpToPx(30))
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
        removeCompareZoneMarkers(id)
        removeColorCompareMarker(id)
        if (zoneNudgeNodeId == id) hideZoneNudgePopup()
        service.nodes.removeAll { it.id == id }
        service.autoSave()
        updateMenu()
        invalidateLines()
    }

    fun updateNodeViewPosition(node: TargetNode) {
        val view = nodeViews[node.id]
        val params = nodeParams[node.id]
        if (view != null && params != null) {
            params.x = node.x - dpToPx(30)
            params.y = node.y - dpToPx(30)
            try { windowManager.updateViewLayout(view, params) } catch (_: Exception) {}
        }
        val endView = swipeEndViews[node.id]
        val endParams = swipeEndParams[node.id]
        if (endView != null && endParams != null) {
            endParams.x = node.swipeEndX - dpToPx(30)
            endParams.y = node.swipeEndY - dpToPx(30)
            try { windowManager.updateViewLayout(endView, endParams) } catch (_: Exception) {}
        }
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
        hideZoneNudgePopup()
        try { floatingControlBar?.let { windowManager.removeView(it) } } catch(e: Exception){}
        try { modMenu?.let { windowManager.removeView(it) } } catch(e: Exception){}
        try { nodeViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { swipeEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { textZoneStartViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { textZoneEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { compareZoneStartViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
        try { compareZoneEndViews.values.forEach { windowManager.removeView(it) } } catch(e: Exception){}
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
                    val (screenWidth, screenHeight) = getScreenDimensions()
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    newX = newX.coerceIn(0, (screenWidth - dpToPx(30)).coerceAtLeast(0))
                    newY = newY.coerceIn(0, (screenHeight - dpToPx(30)).coerceAtLeast(0))
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

    private var lastLogUpdateTime = 0L

    fun logDebug(msg: String) {
        if (!isDebugWindowVisible && debugTextView == null) return
        val time = android.text.format.DateFormat.format("HH:mm:ss", java.util.Date())
        debugLogs.add("[$time] $msg")
        if (debugLogs.size > 100) debugLogs.removeAt(0)
        
        val now = System.currentTimeMillis()
        if (now - lastLogUpdateTime < 150L) return
        lastLogUpdateTime = now

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
