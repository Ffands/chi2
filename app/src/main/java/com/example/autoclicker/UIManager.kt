package com.example.autoclicker

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import java.util.concurrent.ConcurrentHashMap

class UIManager(
    private val service: AutoClickService,
    private val windowManager: WindowManager
) {
    private val handler = Handler(Looper.getMainLooper())
    private val nodeViews = ConcurrentHashMap<String, View>()
    private val phantomViews = ConcurrentHashMap<String, View>()
    private var controlBarView: View? = null
    private var settingsDialogView: View? = null
    private var isRecording = false

    private val overlayType: Int
        get() = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

    fun showControlBar() {
        if (controlBarView != null) return

        val context = service
        val barLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(0xEE18181B.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF3F3F46.toInt())
            }
            background = bg
            setPadding(12, 16, 12, 16)
            elevation = 16f
        }

        val btnPlay = createBarButton("▶", 0xFF00E5FF.toInt()) {
            service.toggleExecution()
        }
        val btnAddClick = createBarButton("+●", 0xFFFFFFFF.toInt()) {
            service.addClickNode()
        }
        val btnAddSwipe = createBarButton("⤹", 0xFFFFFFFF.toInt()) {
            service.addSwipeNode()
        }
        val btnRemove = createBarButton("—", 0xFFEF4444.toInt()) {
            service.removeLastNode()
        }
        val btnSettings = createBarButton("⚙", 0xFFE4E4E7.toInt()) {
            showSettingsDialog()
        }
        val btnScripts = createBarButton("📋", 0xFFA1A1AA.toInt()) {
            showProfilesDialog()
        }
        val btnRecord = createBarButton("⏺", 0xFFEF4444.toInt()) { btn ->
            isRecording = !isRecording
            service.toggleRecordMode(isRecording)
            if (isRecording) {
                (btn as? Button)?.text = "⏹"
                (btn as? Button)?.setTextColor(0xFFFF0055.toInt())
            } else {
                (btn as? Button)?.text = "⏺"
                (btn as? Button)?.setTextColor(0xFFEF4444.toInt())
            }
        }
        val btnClose = createBarButton("✕", 0xFF71717A.toInt()) {
            service.closeServiceUI()
        }

        barLayout.addView(btnPlay)
        barLayout.addView(createDivider())
        barLayout.addView(btnAddClick)
        barLayout.addView(btnAddSwipe)
        barLayout.addView(btnRemove)
        barLayout.addView(createDivider())
        barLayout.addView(btnSettings)
        barLayout.addView(btnScripts)
        barLayout.addView(btnRecord)
        barLayout.addView(createDivider())
        barLayout.addView(btnClose)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 250
        }

        makeDraggable(barLayout, params)
        windowManager.addView(barLayout, params)
        controlBarView = barLayout
    }

    fun updatePlayButtonState(state: ExecutionState) {
        handler.post {
            val bar = controlBarView as? LinearLayout ?: return@post
            val btnPlay = bar.getChildAt(0) as? Button ?: return@post
            when (state) {
                ExecutionState.RUNNING -> {
                    btnPlay.text = "❚❚"
                    btnPlay.setTextColor(0xFFFFCC00.toInt())
                }
                ExecutionState.PAUSED, ExecutionState.STOPPED -> {
                    btnPlay.text = "▶"
                    btnPlay.setTextColor(0xFF00E5FF.toInt())
                }
            }
        }
    }

    fun hideControlBar() {
        controlBarView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            controlBarView = null
        }
    }

    private fun createBarButton(label: String, color: Int, onClick: (View) -> Unit): Button {
        return Button(service).apply {
            text = label
            setTextColor(color)
            textSize = 15f
            val bg = GradientDrawable().apply {
                setColor(0xFF27272A.toInt())
                cornerRadius = 14f
            }
            background = bg
            val size = 96
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 6, 0, 6)
            }
            setPadding(0, 0, 0, 0)
            setOnClickListener { onClick(it) }
        }
    }

    private fun createDivider(): View {
        return View(service).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply {
                setMargins(4, 6, 4, 6)
            }
            setBackgroundColor(0xFF3F3F46.toInt())
        }
    }

    fun addNodeView(node: TargetNode, index: Int) {
        handler.post {
            if (nodeViews.containsKey(node.id)) {
                updateNodeView(node, index)
                return@post
            }

            val markerView = FrameLayout(service).apply {
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xCC09090B.toInt())
                    setStroke(4, if (node.isSwipe) 0xFFFF8800.toInt() else 0xFF00E5FF.toInt())
                }
                background = circle
            }

            val label = TextView(service).apply {
                text = "${index + 1}"
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            markerView.addView(
                label,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            val size = 110
            val params = WindowManager.LayoutParams(
                size,
                size,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = node.x - size / 2
                y = node.y - size / 2
            }

            makeNodeDraggable(markerView, params, node)
            markerView.setOnClickListener {
                showNodeConfigDialog(node, index)
            }

            windowManager.addView(markerView, params)
            nodeViews[node.id] = markerView
        }
    }

    fun updateNodeView(node: TargetNode, index: Int) {
        val view = nodeViews[node.id] as? FrameLayout ?: return
        val label = view.getChildAt(0) as? TextView
        label?.text = "${index + 1}"
    }

    fun updateNodeScreenPosition(node: TargetNode) {
        val view = nodeViews[node.id] ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        node.x = params.x + view.width / 2
        node.y = params.y + view.height / 2
    }

    fun removeNodeView(nodeId: String) {
        handler.post {
            nodeViews.remove(nodeId)?.let {
                try {
                    windowManager.removeView(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearAllNodes() {
        handler.post {
            for ((_, view) in nodeViews) {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            nodeViews.clear()
            clearPhantomNodes()
        }
    }

    fun showPhantomNodes(nodes: List<TargetNode>) {
        handler.post {
            clearPhantomNodes()
            for ((idx, node) in nodes.withIndex()) {
                val phantom = FrameLayout(service).apply {
                    val circle = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0x88002B36.toInt())
                        setStroke(4, 0xFF00E5FF.toInt(), 10f, 6f)
                    }
                    background = circle
                }

                val label = TextView(service).apply {
                    text = "Ф${idx + 1}"
                    textSize = 13f
                    setTextColor(0xFF00E5FF.toInt())
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                }
                phantom.addView(
                    label,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                val size = 100
                val params = WindowManager.LayoutParams(
                    size,
                    size,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = node.x - size / 2
                    y = node.y - size / 2
                }

                windowManager.addView(phantom, params)
                phantomViews["phantom_${node.id}"] = phantom
            }
        }
    }

    fun clearPhantomNodes() {
        handler.post {
            for ((_, view) in phantomViews) {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            phantomViews.clear()
        }
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var isMoved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isMoved = false
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isMoved = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (e: Exception) {}
                            return true
                        }
                    }
                }
                return false
            }
        })
    }

    private fun makeNodeDraggable(view: View, params: WindowManager.LayoutParams, node: TargetNode) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var isMoved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchX = event.rawX
                        touchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchX).toInt()
                        val dy = (event.rawY - touchY).toInt()
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            isMoved = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            node.x = params.x + view.width / 2
                            node.y = params.y + view.height / 2
                            try {
                                windowManager.updateViewLayout(view, params)
                            } catch (e: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoved) {
                            view.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun showNodeConfigDialog(node: TargetNode, index: Int) {
        val context = service
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF18181B.toInt())
            setPadding(32, 32, 32, 32)
            val bg = GradientDrawable().apply {
                setColor(0xFF18181B.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF3F3F46.toInt())
            }
            background = bg
        }

        val title = TextView(context).apply {
            text = "Настройка метки #${index + 1}"
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(title)

        val editDelay = createLabeledInput(context, "Задержка после клика (мс):", "${node.delayAfterMs}")
        dialogView.addView(editDelay.first)

        val editDuration = createLabeledInput(context, "Длительность нажатия (мс):", "${node.clickDurationMs}")
        dialogView.addView(editDuration.first)

        val editRepeat = createLabeledInput(context, "Повторений:", "${node.repeatCount}")
        dialogView.addView(editRepeat.first)

        val editRadius = createLabeledInput(context, "Антидетект радиус (px):", "${node.randomizeRadius}")
        dialogView.addView(editRadius.first)

        val editOcr = createLabeledInput(context, "OCR условие текста:", node.textCondition ?: "")
        dialogView.addView(editOcr.first)

        var dialogRef: View? = null

        val btnSave = Button(context).apply {
            text = "Сохранить"
            setBackgroundColor(0xFF00E5FF.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                node.delayAfterMs = editDelay.second.text.toString().toLongOrNull() ?: node.delayAfterMs
                node.clickDurationMs = editDuration.second.text.toString().toLongOrNull() ?: node.clickDurationMs
                node.repeatCount = editRepeat.second.text.toString().toIntOrNull() ?: node.repeatCount
                node.randomizeRadius = editRadius.second.text.toString().toIntOrNull() ?: node.randomizeRadius
                val txt = editOcr.second.text.toString().trim()
                node.textCondition = if (txt.isEmpty()) null else txt

                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnSave)

        val btnDelete = Button(context).apply {
            text = "Удалить эту метку"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFEF4444.toInt())
            setOnClickListener {
                service.removeNodeById(node.id)
                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnDelete)

        val params = WindowManager.LayoutParams(
            (service.resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        dialogRef = dialogView
        windowManager.addView(dialogView, params)
    }

    private fun createLabeledInput(context: Context, labelText: String, initialValue: String): Pair<LinearLayout, EditText> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 12)
        }
        val label = TextView(context).apply {
            text = labelText
            textSize = 13f
            setTextColor(0xFFA1A1AA.toInt())
        }
        val input = EditText(context).apply {
            setText(initialValue)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF27272A.toInt())
            setPadding(16, 12, 16, 12)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        row.addView(label)
        row.addView(input)
        return Pair(row, input)
    }

    private fun showSettingsDialog() {
        val context = service
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF18181B.toInt())
            setPadding(36, 36, 36, 36)
            val bg = GradientDrawable().apply {
                setColor(0xFF18181B.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF3F3F46.toInt())
            }
            background = bg
        }

        val title = TextView(context).apply {
            text = "⚙ Настройки UpwellClick"
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(title)

        val chkExtreme = CheckBox(context).apply {
            text = "⚡ Экстремальная скорость (Burst 30-60+ CPS)"
            isChecked = service.allowExtremeSpeed
            setTextColor(0xFFFFFFFF.toInt())
            setOnCheckedChangeListener { _, isChecked ->
                service.allowExtremeSpeed = isChecked
            }
        }
        dialogView.addView(chkExtreme)

        val chkMulti = CheckBox(context).apply {
            text = "🖐 Синхронный мультитач клик"
            isChecked = service.enableMultitouch
            setTextColor(0xFFFFFFFF.toInt())
            setOnCheckedChangeListener { _, isChecked ->
                service.enableMultitouch = isChecked
            }
        }
        dialogView.addView(chkMulti)

        var dialogRef: View? = null

        val btnClose = Button(context).apply {
            text = "Готово"
            setBackgroundColor(0xFF00E5FF.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnClose)

        val params = WindowManager.LayoutParams(
            (service.resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        dialogRef = dialogView
        windowManager.addView(dialogView, params)
    }

    private fun showProfilesDialog() {
        val context = service
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF18181B.toInt())
            setPadding(36, 36, 36, 36)
            val bg = GradientDrawable().apply {
                setColor(0xFF18181B.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF3F3F46.toInt())
            }
            background = bg
        }

        val title = TextView(context).apply {
            text = "📋 Сценарии и макросы"
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(title)

        val inputName = EditText(context).apply {
            hint = "Имя нового сценария"
            setHintTextColor(0xFF71717A.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF27272A.toInt())
            setPadding(16, 12, 16, 12)
        }
        dialogView.addView(inputName)

        var dialogRef: View? = null

        val btnSaveCurrent = Button(context).apply {
            text = "💾 Сохранить текущие метки"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val name = inputName.text.toString().trim()
                if (name.isNotEmpty()) {
                    service.saveCurrentAsProfile(name)
                    Toast.makeText(context, "Сценарий '$name' сохранен", Toast.LENGTH_SHORT).show()
                    dialogRef?.let { windowManager.removeView(it) }
                }
            }
        }
        dialogView.addView(btnSaveCurrent)

        val profilesList = service.getProfiles()
        if (profilesList.isNotEmpty()) {
            val listTitle = TextView(context).apply {
                text = "Сохраненные сценарии:"
                textSize = 14f
                setTextColor(0xFFA1A1AA.toInt())
                setPadding(0, 16, 0, 8)
            }
            dialogView.addView(listTitle)

            for (p in profilesList) {
                val btnLoad = Button(context).apply {
                    text = "▶ ${p.name} (${p.nodes.size} меток)"
                    setBackgroundColor(0xFF27272A.toInt())
                    setTextColor(0xFF00E5FF.toInt())
                    setOnClickListener {
                        service.loadProfile(p)
                        dialogRef?.let { windowManager.removeView(it) }
                    }
                }
                dialogView.addView(btnLoad)
            }
        }

        val btnClose = Button(context).apply {
            text = "Закрыть"
            setBackgroundColor(0xFF3F3F46.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnClose)

        val params = WindowManager.LayoutParams(
            (service.resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        dialogRef = dialogView
        windowManager.addView(dialogView, params)
    }
}
