package com.example.autoclicker

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
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
    private var isRecording = false

    private val overlayType: Int
        get() = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

    fun showControlBar() {
        if (controlBarView != null) return

        val context = service
        val barLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(0xF018181B.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF3F3F46.toInt())
            }
            background = bg
            setPadding(10, 14, 10, 14)
            elevation = 20f
        }

        // Control buttons
        val btnPlay = createBarButton("▶", 0xFF00E5FF.toInt(), "Старт / Пауза") {
            service.toggleExecution()
        }
        val btnAddClick = createBarButton("+●", 0xFFFFFFFF.toInt(), "Добавить метку клика") {
            service.addClickNode()
        }
        val btnAddSwipe = createBarButton("⤹", 0xFFFF9900.toInt(), "Добавить свайп") {
            service.addSwipeNode()
        }
        val btnLinkSwipe = createBarButton("A→B", 0xFF38BDF8.toInt(), "Связать две метки в свайп") {
            showLinkSwipeDialog()
        }
        val btnRemoveLast = createBarButton("—", 0xFFEF4444.toInt(), "Удалить последнюю метку") {
            service.removeLastNode()
        }
        val btnClearAll = createBarButton("🗑", 0xFFA1A1AA.toInt(), "Очистить все метки") {
            service.clearAllNodes()
        }
        val btnSettings = createBarButton("⚙", 0xFFE4E4E7.toInt(), "Настройки скрипта и скорости") {
            showSettingsDialog()
        }
        val btnScripts = createBarButton("📋", 0xFFA78BFA.toInt(), "Профили и сценарии") {
            showProfilesDialog()
        }
        val btnRecord = createBarButton("⏺", 0xFFEF4444.toInt(), "Запись жестов (Macro Record)") { btn ->
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
        val btnHideNodes = createBarButton("👁", 0xFF94A3B8.toInt(), "Показать/Скрыть метки") {
            service.toggleNodesVisibility()
        }
        val btnClose = createBarButton("✕", 0xFF71717A.toInt(), "Закрыть автокликер") {
            service.closeServiceUI()
        }

        barLayout.addView(btnPlay)
        barLayout.addView(createDivider())
        barLayout.addView(btnAddClick)
        barLayout.addView(btnAddSwipe)
        barLayout.addView(btnLinkSwipe)
        barLayout.addView(btnRemoveLast)
        barLayout.addView(btnClearAll)
        barLayout.addView(createDivider())
        barLayout.addView(btnSettings)
        barLayout.addView(btnScripts)
        barLayout.addView(btnRecord)
        barLayout.addView(btnHideNodes)
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
            x = 24
            y = 220
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

    private fun createBarButton(label: String, color: Int, tooltip: String, onClick: (View) -> Unit): Button {
        return Button(service).apply {
            text = label
            setTextColor(color)
            textSize = 14f
            val bg = GradientDrawable().apply {
                setColor(0xFF27272A.toInt())
                cornerRadius = 14f
            }
            background = bg
            val size = 96
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(0, 4, 0, 4)
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
                setMargins(4, 4, 4, 4)
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

            val strokeColor = node.customColor ?: if (node.isSwipe) 0xFFFF8800.toInt() else 0xFF00E5FF.toInt()

            val markerView = FrameLayout(service).apply {
                val circle = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xD909090B.toInt())
                    setStroke(4, strokeColor)
                }
                background = circle
            }

            val labelText = node.label ?: "${index + 1}"
            val label = TextView(service).apply {
                text = labelText
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

            val size = 112
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
        label?.text = node.label ?: "${index + 1}"
        val strokeColor = node.customColor ?: if (node.isSwipe) 0xFFFF8800.toInt() else 0xFF00E5FF.toInt()
        (view.background as? GradientDrawable)?.setStroke(4, strokeColor)
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

    fun setNodesVisible(visible: Boolean) {
        handler.post {
            for ((_, view) in nodeViews) {
                view.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }
    }

    fun showPhantomNodes(nodes: List<TargetNode>) {
        handler.post {
            clearPhantomNodes()
            for ((idx, node) in nodes.withIndex()) {
                val phantom = FrameLayout(service).apply {
                    val circle = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(0x80083344.toInt())
                        setStroke(4, 0xFF06B6D4.toInt(), 10f, 6f)
                    }
                    background = circle
                }

                val label = TextView(service).apply {
                    text = "Ф${idx + 1}"
                    textSize = 13f
                    setTextColor(0xFF22D3EE.toInt())
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

                val size = 104
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
        val scroll = ScrollView(context)
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
        scroll.addView(dialogView)

        val title = TextView(context).apply {
            text = "Параметры метки #${index + 1}"
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(title)

        // Delay after
        val editDelay = createLabeledInput(context, "Задержка ПОСЛЕ клика (мс):", "${node.delayAfterMs}")
        dialogView.addView(editDelay.first)

        // Delay before
        val editDelayBefore = createLabeledInput(context, "Задержка ДО клика (мс):", "${node.delayBeforeMs}")
        dialogView.addView(editDelayBefore.first)

        // Click duration
        val editDuration = createLabeledInput(context, "Длительность нажатия (мс):", "${node.clickDurationMs}")
        dialogView.addView(editDuration.first)

        // Repeat count
        val editRepeat = createLabeledInput(context, "Количество повторений:", "${node.repeatCount}")
        dialogView.addView(editRepeat.first)

        // Anti-detect Jitter
        val editRadius = createLabeledInput(context, "Антидетект радиус разброса (px):", "${node.randomizeRadius}")
        dialogView.addView(editRadius.first)

        val editTimeJitter = createLabeledInput(context, "Рандомизация задержки (± мс):", "${node.randomizeTimeMs}")
        dialogView.addView(editTimeJitter.first)

        // Exact Coordinates
        val editCoordX = createLabeledInput(context, "Координата X (пиксели):", "${node.x}")
        dialogView.addView(editCoordX.first)

        val editCoordY = createLabeledInput(context, "Координата Y (пиксели):", "${node.y}")
        dialogView.addView(editCoordY.first)

        // OCR text condition
        val editOcr = createLabeledInput(context, "OCR условие текста (слово/шаблон):", node.textCondition ?: "")
        dialogView.addView(editOcr.first)

        val chkExactMatch = CheckBox(context).apply {
            text = "Точное совпадение текста OCR"
            isChecked = node.textConditionExact
            setTextColor(0xFFE4E4E7.toInt())
        }
        dialogView.addView(chkExactMatch)

        val chkStopOnSuccess = CheckBox(context).apply {
            text = "Остановить сценарий при обнаружении"
            isChecked = node.stopOnSuccess
            setTextColor(0xFFE4E4E7.toInt())
        }
        dialogView.addView(chkStopOnSuccess)

        // Macro Profile Link
        val profiles = service.getProfiles()
        var selectedMacroId = node.macroScriptId
        if (profiles.isNotEmpty()) {
            val macroTitle = TextView(context).apply {
                text = "Запустить сценарий-макрос вместо клика:"
                textSize = 13f
                setTextColor(0xFFA1A1AA.toInt())
                setPadding(0, 12, 0, 4)
            }
            dialogView.addView(macroTitle)

            val spinner = Spinner(context)
            val profileNames = mutableListOf("— Нет (обычный клик) —")
            profileNames.addAll(profiles.map { it.name })
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, profileNames)
            spinner.adapter = adapter

            val currentIdx = if (node.macroScriptId == null) 0 else profiles.indexOfFirst { it.id == node.macroScriptId } + 1
            if (currentIdx > 0) spinner.setSelection(currentIdx)

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedMacroId = if (position == 0) null else profiles[position - 1].id
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            dialogView.addView(spinner)
        }

        var dialogRef: View? = null

        val btnSave = Button(context).apply {
            text = "✓ Сохранить настройки"
            setBackgroundColor(0xFF00E5FF.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                node.delayAfterMs = editDelay.second.text.toString().toLongOrNull() ?: node.delayAfterMs
                node.delayBeforeMs = editDelayBefore.second.text.toString().toLongOrNull() ?: node.delayBeforeMs
                node.clickDurationMs = editDuration.second.text.toString().toLongOrNull() ?: node.clickDurationMs
                node.repeatCount = editRepeat.second.text.toString().toIntOrNull() ?: node.repeatCount
                node.randomizeRadius = editRadius.second.text.toString().toIntOrNull() ?: node.randomizeRadius
                node.randomizeTimeMs = editTimeJitter.second.text.toString().toLongOrNull() ?: node.randomizeTimeMs
                node.x = editCoordX.second.text.toString().toIntOrNull() ?: node.x
                node.y = editCoordY.second.text.toString().toIntOrNull() ?: node.y
                node.textConditionExact = chkExactMatch.isChecked
                node.stopOnSuccess = chkStopOnSuccess.isChecked
                node.macroScriptId = selectedMacroId

                val txt = editOcr.second.text.toString().trim()
                node.textCondition = if (txt.isEmpty()) null else txt

                updateNodeView(node, index)
                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnSave)

        val btnDelete = Button(context).apply {
            text = "🗑 Удалить эту метку"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFEF4444.toInt())
            setOnClickListener {
                service.removeNodeById(node.id)
                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnDelete)

        val params = WindowManager.LayoutParams(
            (service.resources.displayMetrics.widthPixels * 0.90).toInt(),
            (service.resources.displayMetrics.heightPixels * 0.80).toInt(),
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        dialogRef = scroll
        windowManager.addView(scroll, params)
    }

    private fun showLinkSwipeDialog() {
        val nodes = service.nodes
        if (nodes.size < 2) {
            Toast.makeText(service, "Нужно минимум 2 метки для связки свайпа!", Toast.LENGTH_SHORT).show()
            return
        }

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
            text = "Связать свайп между метками"
            textSize = 18f
            setTextColor(0xFF38BDF8.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(title)

        val items = nodes.mapIndexed { idx, _ -> "Метка #${idx + 1}" }

        val labelFrom = TextView(context).apply { text = "Начальная метка (A):"; setTextColor(0xFFA1A1AA.toInt()) }
        val spinnerFrom = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
        }

        val labelTo = TextView(context).apply { text = "Конечная метка (B):"; setTextColor(0xFFA1A1AA.toInt()) }
        val spinnerTo = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, items)
            if (items.size > 1) setSelection(1)
        }

        dialogView.addView(labelFrom)
        dialogView.addView(spinnerFrom)
        dialogView.addView(labelTo)
        dialogView.addView(spinnerTo)

        val editDuration = createLabeledInput(context, "Длительность свайпа (мс):", "300")
        dialogView.addView(editDuration.first)

        var dialogRef: View? = null

        val btnApply = Button(context).apply {
            text = "✓ Создать связь свайпа"
            setBackgroundColor(0xFF38BDF8.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                val fromIdx = spinnerFrom.selectedItemPosition
                val toIdx = spinnerTo.selectedItemPosition
                if (fromIdx != toIdx && fromIdx in nodes.indices && toIdx in nodes.indices) {
                    val nodeA = nodes[fromIdx]
                    val nodeB = nodes[toIdx]
                    nodeA.isSwipe = true
                    nodeA.swipeEndX = nodeB.x
                    nodeA.swipeEndY = nodeB.y
                    nodeA.swipeDurationMs = editDuration.second.text.toString().toLongOrNull() ?: 300L
                    nodeA.swipeTargetNodeId = nodeB.id
                    updateNodeView(nodeA, fromIdx)
                    Toast.makeText(context, "Свайп настроен: #${fromIdx + 1} ➔ #${toIdx + 1}", Toast.LENGTH_SHORT).show()
                }
                dialogRef?.let { windowManager.removeView(it) }
            }
        }
        dialogView.addView(btnApply)

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
            setPadding(0, 0, 0, 10)
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
            text = "⚙ Настройки скорости и сценария"
            textSize = 18f
            setTextColor(0xFF00E5FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        dialogView.addView(title)

        val chkExtreme = CheckBox(context).apply {
            text = "⚡ Extreme Burst Mode (30-60+ CPS)"
            isChecked = service.allowExtremeSpeed
            setTextColor(0xFFFFFFFF.toInt())
            setOnCheckedChangeListener { _, isChecked ->
                service.allowExtremeSpeed = isChecked
            }
        }
        dialogView.addView(chkExtreme)

        val chkMulti = CheckBox(context).apply {
            text = "🖐 Мультитач (синхронный клик всеми метками)"
            isChecked = service.enableMultitouch
            setTextColor(0xFFFFFFFF.toInt())
            setOnCheckedChangeListener { _, isChecked ->
                service.enableMultitouch = isChecked
            }
        }
        dialogView.addView(chkMulti)

        val editLoops = createLabeledInput(context, "Количество циклов (0 = бесконечно):", "${service.targetLoopCount}")
        dialogView.addView(editLoops.first)

        val editTimeLimit = createLabeledInput(context, "Ограничение по времени (сек, 0 = без лимита):", "${service.targetTimeLimitSec}")
        dialogView.addView(editTimeLimit.first)

        var dialogRef: View? = null

        val btnClose = Button(context).apply {
            text = "✓ Применить"
            setBackgroundColor(0xFF00E5FF.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                service.targetLoopCount = editLoops.second.text.toString().toIntOrNull() ?: 0
                service.targetTimeLimitSec = editTimeLimit.second.text.toString().toLongOrNull() ?: 0L
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
        val scroll = ScrollView(context)
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
        scroll.addView(dialogView)

        val title = TextView(context).apply {
            text = "📋 Сценарии и макросы"
            textSize = 18f
            setTextColor(0xFFA78BFA.toInt())
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
            setBackgroundColor(0xFFA78BFA.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
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
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 4, 0, 4)
                }

                val btnLoad = Button(context).apply {
                    text = "▶ ${p.name} (${p.nodes.size} меток)"
                    setBackgroundColor(0xFF27272A.toInt())
                    setTextColor(0xFF00E5FF.toInt())
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
                    setOnClickListener {
                        service.loadProfile(p)
                        dialogRef?.let { windowManager.removeView(it) }
                    }
                }

                val btnDeleteProfile = Button(context).apply {
                    text = "✕"
                    setBackgroundColor(0xFF27272A.toInt())
                    setTextColor(0xFFEF4444.toInt())
                    setOnClickListener {
                        service.deleteProfile(p.id)
                        Toast.makeText(context, "Удален: ${p.name}", Toast.LENGTH_SHORT).show()
                        dialogRef?.let { windowManager.removeView(it) }
                    }
                }

                row.addView(btnLoad)
                row.addView(btnDeleteProfile)
                dialogView.addView(row)
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
            (service.resources.displayMetrics.widthPixels * 0.90).toInt(),
            (service.resources.displayMetrics.heightPixels * 0.70).toInt(),
            overlayType,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            dimAmount = 0.5f
            gravity = Gravity.CENTER
        }

        dialogRef = scroll
        windowManager.addView(scroll, params)
    }
}

