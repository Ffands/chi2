package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.concurrent.CopyOnWriteArrayList
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect

class AutoClickService : AccessibilityService() {

    lateinit var uiManager: UIManager
    var showLines = true
    
    
    private fun parseNumericValue(text: String, suffixes: String): Double? {
        val noSpaces = text.replace(Regex("\\s+"), "").lowercase()
        val match = Regex("(-?\\d+[.,\\d]*)([a-zа-я]*)").find(noSpaces)
        if (match == null) return null
        
        var numPart = match.groupValues[1]
        val sufPart = match.groupValues[2]
        
        if (numPart.contains(",") && numPart.contains(".")) {
            numPart = numPart.replace(",", "")
        } else if (numPart.count { it == ',' } == 1 && !numPart.contains(".")) {
            numPart = numPart.replace(",", ".")
        } else {
            numPart = numPart.replace(",", "")
        }
        
        val dotCount = numPart.count { it == '.' }
        if (dotCount > 1) {
            numPart = numPart.replace(".", "")
        }
        
        val value = numPart.toDoubleOrNull() ?: return null
        
        var multiplier = 1.0
        if (sufPart.isNotEmpty() && suffixes.isNotEmpty()) {
            val pairs = suffixes.split(",")
            val suffixMap = mutableMapOf<String, Double>()
            for (p in pairs) {
                val kv = p.split(":")
                if (kv.size == 2) {
                    suffixMap[kv[0].trim().lowercase()] = kv[1].trim().toDoubleOrNull() ?: 1.0
                }
            }
            for ((suf, mult) in suffixMap) {
                if (sufPart.startsWith(suf)) {
                    multiplier = mult
                    break
                }
            }
        }
        
        return value * multiplier
    }

    fun normalizeCyrillic(str: String): String {
        var s = str.lowercase().replace(Regex("\\s+"), " ").trim()
        
        // Complex multi-character glyphs
        s = s.replace("llo", "лю")
             .replace("io", "ю")
             .replace("lo", "ю")
             .replace("10", "ю")
             .replace("wa", "ща")
             .replace("sh", "ш")
             .replace("ch", "ч")
             .replace("ya", "я")
             .replace("ji", "л")
             .replace("tl", "п")
             .replace("lļ,", "ц")
             .replace("lļ", "ц")
             .replace("ll,", "ц")
             .replace("li,", "ц")
             .replace("n,", "и,")
        
        // Single characters (Latin/Symbols to Cyrillic equivalent visually)
        s = s.replace("6", "б")
             .replace("0", "о")
             .replace("3", "з")
             .replace("4", "ч")
             .replace("9", " э") // 9 often parsed as " э" like in 069TOM -> об этом
             .replace("a", "а")
             .replace("b", "ь")
             .replace("c", "с")
             .replace("e", "е")
             .replace("k", "к")
             .replace("m", "м")
             .replace("h", "н")
             .replace("o", "о")
             .replace("p", "р")
             .replace("t", "т")
             .replace("x", "х")
             .replace("y", "у")
             .replace("n", "и") // n often parsed as и
             .replace("l", "л")
             .replace("u", "и")
             .replace("ñ", "й")
             .replace("r", "г")
             .replace("ó", "ф") // ó is often ф
             .replace("0", "о")
             .replace("o", "о")

        s = s.replace("i", "и")
             .replace("m", "и")

        // Final cleanup
        return s.replace(Regex("\\s+"), " ").trim()
    }
    
    val nodes = CopyOnWriteArrayList<TargetNode>()
    var isPlaying = false
        set(value) {
            field = value
            if (!value) {
                activeThreads.clear()
        if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()
                updateHighlight()
                gestureQueue.clear()
                try {
                    isTakingScreenshot = false
                    screenshotCallbacks.clear()
                    handler.removeCallbacksAndMessages(null)
                } catch(e: Exception) {}
            }
        }
    var maxCycles: Int? = null
    var currentCycle: Int = 0
    var playStartTimeMs: Long = 0L
    var maxPlayDurationMs: Long? = null
    var allowExtremeSpeed = false
    var enableMultitouch = false
    
    data class ExecutionFrame(
        val scriptNodes: List<TargetNode>?,
        val returnNodeId: Int?,
        val repetition: Int,
        val phantomId: Int?
    )

    data class ExecutionThread(
        val threadId: Int,
        var currentNodeId: Int?,
        var currentCheckCycle: Int = 0,
        var currentRepetition: Int = 0,
        var currentCycle: Int = 0,
        var isWaiting: Boolean = false,
        var isActive: Boolean = true,
        var currentScriptNodes: List<TargetNode>? = null,
        var phantomId: Int? = null
    ) {
        val callStack = java.util.Stack<ExecutionFrame>()
    }
    val activeThreads = java.util.concurrent.CopyOnWriteArrayList<ExecutionThread>()
    
    // Gesture Queue
    private val gestureQueue = java.util.concurrent.ConcurrentLinkedQueue<android.accessibilityservice.GestureDescription>()
    private var isDispatchingGesture = false
    
    private fun processGestureQueue() {
        if (gestureQueue.isEmpty() || !isPlaying) return
        if (isDispatchingGesture && !allowExtremeSpeed) return
        val gesture = gestureQueue.poll() ?: return
        
        isDispatchingGesture = true
        dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                if (!allowExtremeSpeed) {
                    isDispatchingGesture = false
                    processGestureQueue()
                }
            }
            override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                if (!allowExtremeSpeed) {
                    isDispatchingGesture = false
                    processGestureQueue()
                }
            }
        }, null)

        if (allowExtremeSpeed) {
            isDispatchingGesture = false
            handler.post { processGestureQueue() }
        }
    }
    
    fun updateHighlight() {
        if (::uiManager.isInitialized) {
            val activeIds = activeThreads.mapNotNull { it.currentNodeId }
            uiManager.updateCurrentNodeHighlight(activeIds)
        }
    }
    val handler = Handler(Looper.getMainLooper())

    companion object {
        var instance: AutoClickService? = null
    }

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_OFF) {
                if (isPlaying) {
                    togglePlay()
                    if (::uiManager.isInitialized) uiManager.logDebug("Авто-СТОП: Экран выключен (фона)")
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        try {
            uiManager = UIManager(this)
            
            val prefs = getSharedPreferences("AutoClickerSettings", android.content.Context.MODE_PRIVATE)
            val currentMode = prefs.getString("AppMode", "ADVANCED")
            updateAppMode(currentMode!!)

            
            MainActivity.pendingImportData?.let { data ->
                loadProfileFromJson(data)
                MainActivity.pendingImportData = null
                android.widget.Toast.makeText(this, "Профиль успешно загружен", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            uiManager.showFloatingControlBar()
            
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::uiManager.isInitialized) {
            uiManager.onConfigurationChanged(newConfig)
        }
    }

    private val ocrLock = Any()
    private var mlTextAnalyzer: com.huawei.hms.mlsdk.text.MLTextAnalyzer? = null

    fun enhanceBitmapForOcr(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val scale = if (w < 150 || h < 150) 2f else 1.5f
        
        val sw = (w * scale).toInt()
        val sh = (h * scale).toInt()
        return Bitmap.createScaledBitmap(src, sw, sh, true)
    }

    private var mlTextAnalyzerLang: String = "ru"
    
    private fun getHuaweiAnalyzer(lang: String): com.huawei.hms.mlsdk.text.MLTextAnalyzer {
        synchronized(ocrLock) {
            val hLang = if (lang == "eng") "en" else "ru"
            if (mlTextAnalyzer != null && mlTextAnalyzerLang == hLang) return mlTextAnalyzer!!
            if (mlTextAnalyzer != null) mlTextAnalyzer!!.stop()
            
            com.huawei.hms.mlsdk.common.MLApplication.getInstance().apiKey = "dummy_api_key_for_local_use_only"
            val setting = com.huawei.hms.mlsdk.text.MLLocalTextSetting.Factory()
                .setOCRMode(com.huawei.hms.mlsdk.text.MLLocalTextSetting.OCR_DETECT_MODE)
                .setLanguage(hLang)
                .create()
            mlTextAnalyzerLang = hLang
            mlTextAnalyzer = com.huawei.hms.mlsdk.MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting)
            return mlTextAnalyzer!!
        }
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SHOW_UI") {
            if (::uiManager.isInitialized) {
                uiManager.showFloatingControlBar()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun updateAppMode(modeStr: String) {
        if (::uiManager.isInitialized) {
            uiManager.appMode = try {
                AppMode.valueOf(modeStr)
            } catch (e: Exception) {
                AppMode.ADVANCED
            }
            uiManager.updateMenu()
        }
    }

    fun toggleRecording() {
        isRecording = !isRecording
        if (isRecording) {
            isPlaying = false
            lastRecordDownTime = 0L
            lastRecordedNodeId = null
            android.widget.Toast.makeText(this, "Запись! Жесты воспроизводятся после отпускания пальца (ограничение Android).", android.widget.Toast.LENGTH_LONG).show()
            uiManager.setNodesTouchable(false)
            showRecordOverlay()
        } else {
            android.widget.Toast.makeText(this, "Запись остановлена.", android.widget.Toast.LENGTH_SHORT).show()
            restoreTouchabilitySafe()
            hideRecordOverlay()
        }
        uiManager.updateMenu()
        uiManager.recreateFloatingControlBar()
    }

    fun togglePlay() {
        if (isRecording) {
            toggleRecording()
            return
        }

        if (isPlaying) {
            isPlaying = false
            gestureQueue.clear()
            if (::uiManager.isInitialized) uiManager.logDebug("--- СТОП ---")
            restoreTouchabilitySafe()
        } else {
            if (nodes.isEmpty()) return
            isPlaying = true
            currentCycle = 0
            playStartTimeMs = System.currentTimeMillis()
            activeThreads.clear()
        if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()
            
            // Check if user set multiple start points by looking at all unlinked nodes, or just the first one
            // We can spawn threads for all nodes that are NOT the target of any other node, unless there's a loop.
            // For now, spawn 1 thread at the first enabled node. Users can add more threads in the future.
            // But wait, if they have multiple separate "groups" of nodes, maybe spawn a thread for each?
            // Let's spawn 1 thread for now, and add a setting for multithread starting later.
            val startNodeId = nodes.firstOrNull { !it.skipSequentialExecution }?.id
            if (startNodeId == null) {
                isPlaying = false
                return
            }
            
            // MULTITHREADING: Automatically detect independent entry points (nodes with no incoming connections)
            val allTargets = mutableSetOf<Int>()
            for (n in nodes) {
                n.nextNodeIdOnSuccess?.let { allTargets.add(it) }
                n.nextNodeIdOnFail?.let { allTargets.add(it) }
                if (!n.skipSequentialExecution && n.nextNodeIdOnSuccess == null && n.nextNodeIdOnFail == null) {
                    // Linear sequence target
                    val idx = nodes.indexOf(n)
                    if (idx + 1 < nodes.size) {
                        allTargets.add(nodes[idx + 1].id)
                    }
                }
            }
            
            var threadIdCounter = 1
            for (n in nodes) {
                if (!n.skipSequentialExecution) {
                    if (n.isIndependentThread || !allTargets.contains(n.id)) {
                        activeThreads.add(ExecutionThread(threadIdCounter++, n.id))
                    }
                }
            }
            
            if (activeThreads.isEmpty()) {
                activeThreads.add(ExecutionThread(threadIdCounter, startNodeId))
            }
            
            updateHighlight()
            if (::uiManager.isInitialized) uiManager.logDebug("--- СТАРТ (${activeThreads.size} поток(ов)) ---")
            uiManager.setNodesTouchable(false)
            
            // Start all threads
            for (thread in activeThreads) {
                executeThread(thread)
            }
        }
        uiManager.updateMenu()
        uiManager.recreateFloatingControlBar()
    }

    private fun parseProfileNodes(profileName: String): List<TargetNode>? {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString(profileName, null) ?: return null
        try {
            val jsonObject = org.json.JSONObject(json)
            val nodesArray = jsonObject.getJSONArray("nodes")
            val parsedNodes = mutableListOf<TargetNode>()
            for (i in 0 until nodesArray.length()) {
                parsedNodes.add(TargetNode.fromJson(nodesArray.getJSONObject(i)))
            }
            return parsedNodes
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun evaluateManagerRoutes(routes: List<ManagerRoute>, index: Int, thread: ExecutionThread, managerNode: TargetNode) {
        if (!isPlaying || !thread.isActive) return
        if (index >= routes.size) {
            thread.currentRepetition = 0
            val currentNodesList = thread.currentScriptNodes ?: this.nodes
            thread.currentNodeId = managerNode.nextNodeIdOnFail ?: getNextNodeLinear(managerNode.id)
            if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${managerNode.id}: Менеджер -> ${thread.currentNodeId} (По умолчанию)")
            scheduleNextExecution(thread, managerNode.delayAfterMs)
            return
        }
        
        val route = routes[index]
        val currentNodesList = thread.currentScriptNodes ?: this.nodes
        val nodeToCheck = currentNodesList.find { it.id == route.checkNodeId }
        if (nodeToCheck == null || !nodeHasCondition(nodeToCheck)) {
            evaluateManagerRoutes(routes, index + 1, thread, managerNode)
            return
        }
        
        checkConditionForNode(nodeToCheck) { isMatch ->
            if (!isPlaying || !thread.isActive) return@checkConditionForNode
            if (isMatch) {
                thread.currentRepetition = 0
                thread.currentNodeId = route.onSuccessGoToId
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${managerNode.id}: Менеджер совпало ${route.checkNodeId} -> ${thread.currentNodeId}")
                scheduleNextExecution(thread, managerNode.delayAfterMs)
            } else {
                evaluateManagerRoutes(routes, index + 1, thread, managerNode)
            }
        }
    }

        private fun scheduleNextExecution(thread: ExecutionThread, delayMs: Long) {
        handler.postDelayed({ executeThread(thread) }, delayMs)
    }

    private fun executeThread(thread: ExecutionThread) {
        if (!isPlaying || !thread.isActive) return

        if (thread.currentNodeId == -1) {
            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            thread.isActive = false
            if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Достигнут шаг -1")
            checkAllThreadsStopped()
            return
        }

        if (maxPlayDurationMs != null && maxPlayDurationMs!! > 0L) {
            val elapsed = System.currentTimeMillis() - playStartTimeMs
            if (elapsed >= maxPlayDurationMs!!) {
                isPlaying = false
                gestureQueue.clear()
                if (::uiManager.isInitialized) uiManager.logDebug("СТОП: Лимит времени истек")
                restoreTouchabilitySafe()
                uiManager.updateMenu()
                return
            }
        }

        val currentNodes = thread.currentScriptNodes ?: this.nodes
        val node = currentNodes.find { it.id == thread.currentNodeId }
        
        if (node == null) {
            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            
            thread.currentNodeId = currentNodes.firstOrNull { !it.skipSequentialExecution }?.id
            
            if (thread.currentNodeId != null) {
                thread.currentCycle++
                if (maxCycles != null && maxCycles!! > 0 && thread.currentCycle >= maxCycles!!) {
                    thread.isActive = false
                    checkAllThreadsStopped()
                    return
                }
                thread.currentRepetition = 0
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Конец цикла. Запуск ${thread.currentCycle}...")
                handler.postDelayed({ executeThread(thread) }, 100L)
            } else {
                thread.isActive = false
                checkAllThreadsStopped()
            }
            return
        }

        if (activeThreads.firstOrNull()?.threadId == thread.threadId) {
            uiManager.updateNodeScreenPosition(node)
        }

        checkConditionForNode(node) { isMatch ->
            if (!isPlaying || !thread.isActive) return@checkConditionForNode
            
            val currentNodesList = thread.currentScriptNodes ?: this.nodes
            if (currentNodesList.find { it.id == node.id } == null) {
                thread.isActive = false
                checkAllThreadsStopped()
                return@checkConditionForNode
            }
            
            if (isMatch) {
                thread.currentCheckCycle = 0
                if (node.type == NodeType.CLICK) {
                    val activeNodes = mutableListOf(node)
                    if (node.syncWithNodeIds.isNotEmpty()) {
                        val idsStr = node.syncWithNodeIds.split(",")
                        for (idStr in idsStr) {
                            val id = idStr.trim().toIntOrNull()
                            if (id != null) {
                                val currentNodesList = thread.currentScriptNodes ?: this.nodes
                                val syncNode = currentNodesList.find { it.id == id }
                                if (syncNode != null && syncNode.type == NodeType.CLICK) {
                                    activeNodes.add(syncNode)
                                }
                            }
                        }
                    }
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: ${if (node.isSwipe) "Свайп" else "Клик"}")
                    performGestureForNodes(activeNodes)
                } else if (node.triggerMode == 2 && node.ocrFullScreenClick) {
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: OCR Клик выполнен")
                } else if (node.type == NodeType.MANAGER) {
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Менеджер...")
                    evaluateManagerRoutes(node.managerRoutes, 0, thread, node)
                    return@checkConditionForNode
                } else if (node.type == NodeType.MACRO && !node.macroProfileName.isNullOrEmpty()) {
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Запуск блока команд ${node.macroProfileName} (Параллельно: ${node.macroRunParallel})")
                    val currentNodesList = thread.currentScriptNodes ?: this.nodes
                    val macroNodes = parseProfileNodes(node.macroProfileName!!)
                    
                    if (macroNodes.isNullOrEmpty()) {
                        if (::uiManager.isInitialized) uiManager.logDebug("Ошибка: Не удалось загрузить блок команд ${node.macroProfileName}")
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)
                        scheduleNextExecution(thread, node.delayAfterMs)
                        return@checkConditionForNode
                    }
                    
                    if (node.macroRunParallel) {
                        val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                        val newThread = ExecutionThread(
                            threadId = activeThreads.size + 1,
                            currentNodeId = macroNodes.firstOrNull()?.id,
                            currentScriptNodes = macroNodes,
                            phantomId = pId
                        )
                        activeThreads.add(newThread)
                        executeThread(newThread)
                        
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)
                        scheduleNextExecution(thread, node.delayAfterMs)
                    } else {
                        val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)
                        val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                        thread.callStack.push(ExecutionFrame(thread.currentScriptNodes, nextId, thread.currentRepetition, thread.phantomId))
                        thread.phantomId = pId
                        
                        thread.currentScriptNodes = macroNodes
                        thread.currentRepetition = 0
                        thread.currentNodeId = macroNodes.firstOrNull()?.id
                        scheduleNextExecution(thread, node.delayAfterMs)
                    }
                    return@checkConditionForNode
                } else {
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Условие сработало")
                }
                
                thread.currentRepetition++
                if (thread.currentRepetition < node.repetitions) {
                    thread.currentNodeId = node.id
                } else {
                    thread.currentRepetition = 0
                    thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)
                }
            } else {
                if (node.maxCheckCycles != null && node.maxCheckCycles!! > 0) {
                    thread.currentCheckCycle++
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Ждем (${thread.currentCheckCycle}/${node.maxCheckCycles})")
                    if (thread.currentCheckCycle >= node.maxCheckCycles!!) {
                        thread.currentCheckCycle = 0
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnFail ?: getNextNodeLinear(node.id)
                    } else {
                        thread.currentNodeId = node.id
                    }
                } else {
                    thread.currentRepetition = 0
                    thread.currentNodeId = node.nextNodeIdOnFail ?: node.id
                }
            }

            updateHighlight()

            val randomDelay = if (node.randomizeDelayMs > 0) (0..node.randomizeDelayMs).random() else 0L
            val minDelay = if (allowExtremeSpeed) 0L else 30L
            
            val finalDelay = if (!isMatch && thread.currentNodeId == node.id) {
                val pollDelay = if (node.triggerMode == 2) 300L else 150L
                Math.max(pollDelay, minDelay)
            } else {
                Math.max(minDelay, node.delayAfterMs + randomDelay)
            }

            handler.postDelayed({ executeThread(thread) }, finalDelay)
        }
    }

    private fun restoreTouchabilitySafe() {
        if (isDispatchingGesture || isDispatchingRecordGesture) {
            handler.postDelayed({ restoreTouchabilitySafe() }, 100)
        } else {
            if (::uiManager.isInitialized) {
                uiManager.setNodesTouchable(true)
            }
        }
    }

    private fun checkAllThreadsStopped() {
        if (activeThreads.all { !it.isActive }) {
            isPlaying = false
            gestureQueue.clear()
            restoreTouchabilitySafe()
            uiManager.updateMenu()
        }
    }

    private fun getNextNodeLinear(currentId: Int): Int? {
        val index = nodes.indexOfFirst { it.id == currentId }
        var nextIndex = index + 1
        val nSize = nodes.size
        var looped = false
        
        while (nextIndex < nSize) {
            val n = nodes[nextIndex]
            if (!n.skipSequentialExecution) {
                return n.id
            }
            nextIndex++
        }
        
        return null // all nodes skipped
    }

    private fun performGestureForNodes(activeNodes: List<TargetNode>) {
        if (enableMultitouch) {
            val builder = GestureDescription.Builder()
            for (node in activeNodes) {
                val path = Path()
                var startX = node.x.toFloat()
                var startY = node.y.toFloat()
                if (node.randomizeRadius > 0) {
                    val angle = Math.random() * Math.PI * 2
                    val r = Math.random() * node.randomizeRadius
                    startX += (Math.cos(angle) * r).toFloat()
                    startY += (Math.sin(angle) * r).toFloat()
                }
                path.moveTo(startX, startY)
                if (node.isSwipe) {
                    var eX = node.swipeEndX.toFloat()
                    var eY = node.swipeEndY.toFloat()
                    if (node.swipeTargetNodeId != null) {
                        val tgtNode = (nodes).find { it.id == node.swipeTargetNodeId }
                        if (tgtNode != null) {
                            try {
                                uiManager.updateNodeScreenPosition(tgtNode)
                            } catch(e: Exception){}
                            eX = tgtNode.x.toFloat()
                            eY = tgtNode.y.toFloat()
                        }
                        if (node.randomizeRadius > 0) {
                            val angle = Math.random() * Math.PI * 2
                            val r = Math.random() * node.randomizeRadius
                            eX += (Math.cos(angle) * r).toFloat()
                            eY += (Math.sin(angle) * r).toFloat()
                        }
                        path.lineTo(eX, eY)
                    } else if (node.swipePathPoints.isNotEmpty()) {
                        path.moveTo(node.swipePathPoints[0].first, node.swipePathPoints[0].second)
                        for (i in 1 until node.swipePathPoints.size) {
                            path.lineTo(node.swipePathPoints[i].first, node.swipePathPoints[i].second)
                        }
                    } else {
                        if (node.randomizeRadius > 0) {
                            val angle = Math.random() * Math.PI * 2
                            val r = Math.random() * node.randomizeRadius
                            eX += (Math.cos(angle) * r).toFloat()
                            eY += (Math.sin(angle) * r).toFloat()
                        }
                        path.lineTo(eX, eY)
                    }
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)
                builder.addStroke(stroke)
            }
            gestureQueue.add(builder.build())
        } else {
            for (node in activeNodes) {
                val builder = GestureDescription.Builder()
                val path = Path()
                var startX = node.x.toFloat()
                var startY = node.y.toFloat()
                if (node.randomizeRadius > 0) {
                    val angle = Math.random() * Math.PI * 2
                    val r = Math.random() * node.randomizeRadius
                    startX += (Math.cos(angle) * r).toFloat()
                    startY += (Math.sin(angle) * r).toFloat()
                }
                path.moveTo(startX, startY)
                if (node.isSwipe) {
                    var eX = node.swipeEndX.toFloat()
                    var eY = node.swipeEndY.toFloat()
                    if (node.swipeTargetNodeId != null) {
                        val tgtNode = (nodes).find { it.id == node.swipeTargetNodeId }
                        if (tgtNode != null) {
                            try {
                                uiManager.updateNodeScreenPosition(tgtNode)
                            } catch(e: Exception){}
                            eX = tgtNode.x.toFloat()
                            eY = tgtNode.y.toFloat()
                        }
                        if (node.randomizeRadius > 0) {
                            val angle = Math.random() * Math.PI * 2
                            val r = Math.random() * node.randomizeRadius
                            eX += (Math.cos(angle) * r).toFloat()
                            eY += (Math.sin(angle) * r).toFloat()
                        }
                        path.lineTo(eX, eY)
                    } else if (node.swipePathPoints.isNotEmpty()) {
                        path.moveTo(node.swipePathPoints[0].first, node.swipePathPoints[0].second)
                        for (i in 1 until node.swipePathPoints.size) {
                            path.lineTo(node.swipePathPoints[i].first, node.swipePathPoints[i].second)
                        }
                    } else {
                        if (node.randomizeRadius > 0) {
                            val angle = Math.random() * Math.PI * 2
                            val r = Math.random() * node.randomizeRadius
                            eX += (Math.cos(angle) * r).toFloat()
                            eY += (Math.sin(angle) * r).toFloat()
                        }
                        path.lineTo(eX, eY)
                    }
                }
                val stroke = GestureDescription.StrokeDescription(path, 0, if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)
                builder.addStroke(stroke)
                gestureQueue.add(builder.build())
            }
        }
        
        processGestureQueue()
    }

    fun captureColorAt(x: Int, y: Int, callback: (Int?) -> Unit) {
        checkColorsAt(listOf(Pair(x, y))) { colors ->
            callback(colors.firstOrNull())
        }
    }

    fun captureImageFragment(node: TargetNode, callback: (String?) -> Unit) {
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    var base64Fragment: String? = null
                    try {
                        val buffer = screenshot.hardwareBuffer
                        val hwBitmap = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                        if (hwBitmap != null) {
                            val bitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            if (bitmap != null) {
                                val left = minOf(node.textZoneStartX, node.textZoneEndX)
                                val top = minOf(node.textZoneStartY, node.textZoneEndY)
                                val right = maxOf(node.textZoneStartX, node.textZoneEndX)
                                val bottom = maxOf(node.textZoneStartY, node.textZoneEndY)
                                
                                val cLeft = left.coerceIn(0, bitmap.width - 1)
                                val cTop = top.coerceIn(0, bitmap.height - 1)
                                val cRight = right.coerceIn(0, bitmap.width - 1)
                                val cBottom = bottom.coerceIn(0, bitmap.height - 1)

                                val w = maxOf(1, cRight - cLeft)
                                val h = maxOf(1, cBottom - cTop)
                                if (w > 0 && h > 0) {
                                    val cropped = Bitmap.createBitmap(bitmap, cLeft, cTop, w, h)
                                    val stream = java.io.ByteArrayOutputStream()
                                    cropped.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val bArray = stream.toByteArray()
                                    base64Fragment = android.util.Base64.encodeToString(bArray, android.util.Base64.DEFAULT)
                                    cropped.recycle()
                                }
                                bitmap.recycle()
                            }
                            hwBitmap.recycle()
                        }
                        buffer.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    callback(base64Fragment)
                }
                override fun onFailure(errorCode: Int) {
                    callback(null)
                }
            })
        } catch (e: Exception) {
            callback(null)
        }
    }

    fun nodeHasCondition(node: TargetNode): Boolean {
        if (node.triggerMode == -1) return false
        if (node.targetColor != null) return true
        if (node.targetImageBase64 != null) return true
        if (node.targetText != null) return true
        if (node.compareToNodeId != null) return true
        if (node.colorCompareX != null) return true
        if (node.dynamicColorUpdate) return true
        if (node.linkedConditionNodeId != null) return true
        return false
    }

    private var lastScreenshotTime = 0L
    private var cachedBitmap: Bitmap? = null
    private var isTakingScreenshot = false
    private val screenshotCallbacks = mutableListOf<(Bitmap?) -> Unit>()

    private fun requestScreenshot(callback: (Bitmap?) -> Unit) {
        val now = System.currentTimeMillis()
        if (cachedBitmap != null && now - lastScreenshotTime < 50) {
            callback(cachedBitmap)
            return
        }

        screenshotCallbacks.add(callback)

        if (isTakingScreenshot) return
        isTakingScreenshot = true

        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val buffer = screenshot.hardwareBuffer
                        val hwBitmap = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                        if (hwBitmap != null) {
                            val newBitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            hwBitmap.recycle()
                            buffer.close()

                            if (newBitmap != null) {
                                cachedBitmap?.recycle()
                                cachedBitmap = newBitmap
                                lastScreenshotTime = System.currentTimeMillis()

                                val callbacks = screenshotCallbacks.toList()
                                screenshotCallbacks.clear()
                                isTakingScreenshot = false

                                callbacks.forEach { it(cachedBitmap) }
                                return
                            }
                        }
                        buffer.close()
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                    failAll()
                }

                override fun onFailure(errorCode: Int) {
                    failAll()
                }

                private fun failAll() {
                    val callbacks = screenshotCallbacks.toList()
                    screenshotCallbacks.clear()
                    isTakingScreenshot = false
                    callbacks.forEach { it(null) }
                }
            })
        } catch(e: Exception) {
            val callbacks = screenshotCallbacks.toList()
            screenshotCallbacks.clear()
            isTakingScreenshot = false
            callbacks.forEach { it(null) }
        }
    }

    private fun checkConditionForNode(node: TargetNode, callback: (Boolean) -> Unit) {
        if (!nodeHasCondition(node)) {
            callback(true)
            return
        }

        requestScreenshot { bitmap ->
            if (bitmap == null) {
                callback(false)
                return@requestScreenshot
            }

            checkNodeConditionAsync(node, bitmap) { isMainMatch ->
                if (node.linkedConditionNodeId != null) {
                    val linkedNode = (nodes).find { it.id == node.linkedConditionNodeId }
                    if (linkedNode != null) {
                        checkNodeConditionAsync(linkedNode, bitmap) { isLinkedMatch ->
                            val isColorMatch = if (node.linkedConditionOperator == "OR") {
                                isMainMatch || isLinkedMatch
                            } else {
                                isMainMatch && isLinkedMatch
                            }
                            // Do not recycle bitmap here, it is cached globally
                            callback(isColorMatch)
                        }
                        return@checkNodeConditionAsync
                    }
                }
                // Do not recycle bitmap here, it is cached globally
                callback(isMainMatch)
            }
        }
    }

    private fun checkNodeConditionAsync(node: TargetNode, bitmap: Bitmap, callback: (Boolean) -> Unit) {
        if (node.triggerMode == 2 && node.targetText?.isNotEmpty() == true) {
            checkTextCondition(node, bitmap) { isMatch ->
                callback(isMatch)
            }
        } else {
            callback(checkNodeCondition(node, bitmap))
        }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = Math.min(Math.min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    private fun fuzzyContains(text: String, query: String, maxDist: Int): Boolean {
        if (query.isEmpty() || maxDist == 0) return text.contains(query)
        if (text.contains(query)) return true
        if (text.length < query.length) return levenshtein(text, query) <= maxDist

        for (i in 0..text.length - query.length) {
            val len1 = query.length
            val len2 = Math.min(query.length + 2, text.length - i)
            for (len in len1 - 1 .. len2) {
                if (len < 1) continue
                val window = text.substring(i, i + len)
                if (levenshtein(window, query) <= maxDist) return true
            }
        }
        return false
    }

    fun testTextRecognition(node: TargetNode, bitmap: Bitmap) {
        val searchStrOrig = node.targetText?.trim() ?: ""
        if (searchStrOrig.isEmpty()) {
            handler.post { Toast.makeText(this, "Пожалуйста, введите искомый текст", Toast.LENGTH_SHORT).show() }
            return
        }
        
        handler.post { Toast.makeText(this, "Тестирование распознавания...", Toast.LENGTH_SHORT).show() }
        
        try {
            val cropped = bitmap
            val w = cropped.width
            val h = cropped.height
            val isRegionSelected = true
            
            Thread {
                var debugBmp: Bitmap? = null
                try {
                    val recognizedText: String
                    if (w < 5 || h < 5) {
                        recognizedText = ""
                    } else {
                        val enhanced = enhanceBitmapForOcr(cropped)
                        debugBmp = enhanced
                        val analyzer = getHuaweiAnalyzer(node.targetLanguage)
                        val frame = com.huawei.hms.mlsdk.common.MLFrame.fromBitmap(enhanced)
                        val task = analyzer.asyncAnalyseFrame(frame)
                        val result = com.huawei.hmf.tasks.Tasks.await(task)
                        recognizedText = result?.stringValue ?: ""
                        if (enhanced != cropped && debugBmp == null) enhanced.recycle()
                    }
                    
                    val searchStr = normalizeCyrillic(searchStrOrig).replace(" ", "")
                    val recStrOcr = normalizeCyrillic(recognizedText).replace(" ", "")
                    val maxCost = if (searchStr.length <= 3) 0 else if (searchStr.length <= 6) 1 else searchStr.length / 4

                    var isMatch = false
                    var debugMsg = ""
                    
                    if (node.isSmartOcr) {
                        val parsedVal = parseNumericValue(recognizedText, node.ocrCustomSuffixes)
                        if (parsedVal != null) {
                            node.lastRecognizedValue = parsedVal
                            val compareValue = if (node.ocrCompareToNodeId != null) {
                                val otherNode = (nodes).find { it.id == node.ocrCompareToNodeId }
                                otherNode?.lastRecognizedValue ?: node.ocrTargetValue
                            } else {
                                node.ocrTargetValue
                            }
                            isMatch = when (node.ocrOperator) {
                                ">" -> parsedVal > compareValue
                                "<" -> parsedVal < compareValue
                                ">=" -> parsedVal >= compareValue
                                "<=" -> parsedVal <= compareValue
                                "==" -> parsedVal == compareValue
                                "!=" -> parsedVal != compareValue
                                else -> false
                            }
                            debugMsg = "Шаг ${node.id}: [Смарт OCR] Текст='$recognizedText', Число=$parsedVal. Условие: $parsedVal ${node.ocrOperator} $compareValue -> $isMatch"
                        } else {
                            debugMsg = "Шаг ${node.id}: [Смарт OCR] Текст='$recognizedText'. Не удалось распознать число."
                        }
                    } else {
                        val ocrMatch = fuzzyContains(recStrOcr, searchStr, maxCost) ||
                                      fuzzyContains(recognizedText.lowercase().replace(Regex("\\s+"), ""),
                                                   searchStrOrig.lowercase().replace(Regex("\\s+"), ""),
                                                   maxCost)
                        isMatch = ocrMatch
                        debugMsg = "Шаг ${node.id}: [OCR] '$recognizedText'. Ищем: '${node.targetText}'. Совпадение: $isMatch"
                    }
                    
                    if (::uiManager.isInitialized) {
                        handler.post {
                            uiManager.logDebug(debugMsg)
                            val finalBmp = debugBmp ?: cropped
                            uiManager.showOcrResultDialog(recognizedText, searchStrOrig, isMatch, finalBmp)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    handler.post { Toast.makeText(this@AutoClickService, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }.start()
            
        } catch (e: Exception) {
            e.printStackTrace()
            handler.post { Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun checkTextCondition(node: TargetNode, bitmap: Bitmap, callback: (Boolean) -> Unit) {
        try {
            val startX = node.textZoneStartX
            val startY = node.textZoneStartY
            val endX = node.textZoneEndX
            val endY = node.textZoneEndY
            val left = minOf(startX, endX).coerceIn(0, bitmap.width - 1)
            val top = minOf(startY, endY).coerceIn(0, bitmap.height - 1)
            val right = maxOf(startX, endX).coerceIn(0, bitmap.width - 1)
            val bottom = maxOf(startY, endY).coerceIn(0, bitmap.height - 1)
            val w = maxOf(1, right - left)
            val h = maxOf(1, bottom - top)
            
            var isRegionSelected = !(left == 0 && top == 0 && right == 0 && bottom == 0)
            var cropped = if (isRegionSelected) Bitmap.createBitmap(bitmap, left, top, w, h) else bitmap
            
            Thread {
                try {
                    val searchStrOrig = node.targetText!!
                    val recognizedText: String
                    var foundRect: android.graphics.Rect? = null
                    
                    if (w < 5 || h < 5) {
                        recognizedText = ""
                    } else {
                        val enhanced = enhanceBitmapForOcr(cropped)
                        val analyzer = getHuaweiAnalyzer(node.targetLanguage)
                        val frame = com.huawei.hms.mlsdk.common.MLFrame.fromBitmap(enhanced)
                        val task = analyzer.asyncAnalyseFrame(frame)
                        val result = com.huawei.hmf.tasks.Tasks.await(task)
                        recognizedText = result?.stringValue ?: ""
                        
                        if (node.ocrFullScreenClick && result != null) {
                            val searchStr = normalizeCyrillic(searchStrOrig).replace(" ", "").lowercase()
                            val maxCost = if (searchStr.length <= 3) 0 else if (searchStr.length <= 6) 1 else searchStr.length / 4

                            for (block in result.blocks) {
                                for (line in block.contents) {
                                    val blockText = normalizeCyrillic(line.stringValue).replace(" ", "").lowercase()
                                    if (fuzzyContains(blockText, searchStr, maxCost)) {
                                        foundRect = line.border
                                        break
                                    }
                                }
                                if (foundRect != null) break
                            }
                        }
                        if (enhanced != cropped) enhanced.recycle()
                    }
                    
                    val searchStr = normalizeCyrillic(searchStrOrig).replace(" ", "")
                    val recStrOcr = normalizeCyrillic(recognizedText).replace(" ", "")
                    
                    val maxCost = if (searchStr.length <= 3) 0 else if (searchStr.length <= 6) 1 else searchStr.length / 4
                    
                    var isMatch = false
                    var debugMsg = ""
                    
                    if (node.isSmartOcr) {
                        val parsedVal = parseNumericValue(recognizedText, node.ocrCustomSuffixes)
                        if (parsedVal != null) {
                            node.lastRecognizedValue = parsedVal
                            val compareValue = if (node.ocrCompareToNodeId != null) {
                                val otherNode = (nodes).find { it.id == node.ocrCompareToNodeId }
                                otherNode?.lastRecognizedValue ?: node.ocrTargetValue
                            } else {
                                node.ocrTargetValue
                            }
                            isMatch = when (node.ocrOperator) {
                                ">" -> parsedVal > compareValue
                                "<" -> parsedVal < compareValue
                                ">=" -> parsedVal >= compareValue
                                "<=" -> parsedVal <= compareValue
                                "==" -> parsedVal == compareValue
                                "!=" -> parsedVal != compareValue
                                else -> false
                            }
                            debugMsg = "Шаг ${node.id}: [Смарт OCR] Текст='$recognizedText', Число=$parsedVal. Условие: $parsedVal ${node.ocrOperator} $compareValue -> $isMatch"
                        } else {
                            debugMsg = "Шаг ${node.id}: [Смарт OCR] Текст='$recognizedText'. Не удалось распознать число."
                        }
                    } else {
                        val ocrMatch = fuzzyContains(recStrOcr, searchStr, maxCost) ||
                                      fuzzyContains(recognizedText.lowercase().replace(Regex("\\s+"), ""),
                                                   searchStrOrig.lowercase().replace(Regex("\\s+"), ""),
                                                   maxCost)
                        isMatch = ocrMatch
                        debugMsg = "Шаг ${node.id}: [OCR] '$recognizedText'. Ищем: '${node.targetText}'. Совпадение: $isMatch"
                    }
                    
                    if (::uiManager.isInitialized) {
                        handler.post {
                            uiManager.logDebug(debugMsg)
                        }
                    }
                    
                    if (isRegionSelected && !cropped.isRecycled) cropped.recycle()
                    
                    if (isMatch && node.ocrFullScreenClick && foundRect != null) {
                        val clickX = left + foundRect.centerX()
                        val clickY = top + foundRect.centerY()
                        performGlobalClick(clickX.toFloat(), clickY.toFloat(), node.clickDurationMs)
                    }
                    
                    val finalResult = if (node.colorOperator == "!=") !isMatch else isMatch
                    handler.post { callback(finalResult) }
                } catch (e: Exception) {
                    if (isRegionSelected && !cropped.isRecycled) cropped.recycle()
                    if (::uiManager.isInitialized) {
                        handler.post { uiManager.logDebug("OCR Ошибка: ${e.message}") }
                    }
                    handler.post { callback(false) }
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            handler.post { Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show() }
            callback(false)
        }
    }

    private fun checkNodeCondition(node: TargetNode, bitmap: Bitmap): Boolean {
        if (node.triggerMode == 1) {
            if (node.targetImageBase64 != null) {
                try {
                    var targetBmp = node.cachedTargetBitmap
                    if (targetBmp == null) {
                        val bytes = android.util.Base64.decode(node.targetImageBase64, android.util.Base64.DEFAULT)
                        targetBmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        node.cachedTargetBitmap = targetBmp
                    }
                    if (targetBmp != null) {
                        val startX = node.textZoneStartX
                        val startY = node.textZoneStartY
                        val endX = node.textZoneEndX
                        val endY = node.textZoneEndY
                        
                        val tgtLeft = minOf(startX, endX).coerceIn(0, bitmap.width - 1)
                        val tgtTop = minOf(startY, endY).coerceIn(0, bitmap.height - 1)
                        val tgtRight = maxOf(startX, endX).coerceIn(0, bitmap.width - 1)
                        val tgtBottom = maxOf(startY, endY).coerceIn(0, bitmap.height - 1)
                        val origW = tgtRight - tgtLeft
                        val origH = tgtBottom - tgtTop
                        
                        if (origW <= 0 || origH <= 0) {
                            return false
                        }
                        
                        val scaledTarget = if (targetBmp.width != origW || targetBmp.height != origH) {
                            Bitmap.createScaledBitmap(targetBmp, origW, origH, true)
                        } else targetBmp
                        
                        val total = origW * origH
                        val pTarget = IntArray(total)
                        scaledTarget.getPixels(pTarget, 0, origW, 0, 0, origW, origH)
                        
                        val r = node.searchRadius
                        val searchRectLeft = (tgtLeft - r).coerceIn(0, bitmap.width - 1)
                        val searchRectTop = (tgtTop - r).coerceIn(0, bitmap.height - 1)
                        val searchRectRight = (tgtRight + r).coerceIn(0, bitmap.width - 1)
                        val searchRectBottom = (tgtBottom + r).coerceIn(0, bitmap.height - 1)
                        val sW = searchRectRight - searchRectLeft
                        val sH = searchRectBottom - searchRectTop
                        
                        var found = false
                        
                        if (sW >= origW && sH >= origH) {
                            val pSearch = IntArray(sW * sH)
                            bitmap.getPixels(pSearch, 0, sW, searchRectLeft, searchRectTop, sW, sH)
                            
                            val checkStep = (1f / node.checkResolutionScale.coerceIn(0.1f, 1.0f)).toInt().coerceAtLeast(1)
                            val step = maxOf(if (r > 10) 2 else 1, checkStep / 2).coerceAtLeast(1)
                            val maxY = sH - origH
                            val maxX = sW - origW
                            
                            var totalPixelsChecked = 0
                            for (ty in 0 until origH step checkStep) {
                                for (tx in 0 until origW step checkStep) {
                                    totalPixelsChecked++
                                }
                            }
                            
                            for (dy in 0..maxY step step) {
                                for (dx in 0..maxX step step) {
                                    var matchCount = 0
                                    for (ty in 0 until origH step checkStep) {
                                        val tgtOffset = ty * origW
                                        val srcOffset = (dy + ty) * sW + dx
                                        for (tx in 0 until origW step checkStep) {
                                            val c1 = pSearch[srcOffset + tx]
                                            val c2 = pTarget[tgtOffset + tx]
                                            val rDiff = Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(c2))
                                            val gDiff = Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(c2))
                                            val bDiff = Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(c2))
                                            if (rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance) {
                                                matchCount++
                                            }
                                        }
                                    }
                                    val matchPercent = (matchCount.toFloat() / totalPixelsChecked) * 100f
                                    if (matchPercent >= node.imageThreshold) {
                                        found = true
                                        break
                                    }
                                }
                                if (found) break
                            }
                        }
                        
                        if (scaledTarget != targetBmp) scaledTarget.recycle()
                        // Do not recycle targetBmp since it is cached
                        
                        return if (node.colorOperator == "!=") !found else found
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }
        } else { // triggerMode == 0 (Color Pixel)
            val cx = node.x.coerceIn(0, bitmap.width - 1)
            val cy = node.y.coerceIn(0, bitmap.height - 1)
            
            if (node.compareToNodeId != null) {
                val otherNode = (nodes).find { it.id == node.compareToNodeId }
                if (otherNode != null) {
                    val color1 = bitmap.getPixel(cx, cy)
                    val cx2 = otherNode.x.coerceIn(0, bitmap.width - 1)
                    val cy2 = otherNode.y.coerceIn(0, bitmap.height - 1)
                    val color2 = bitmap.getPixel(cx2, cy2)
                    
                    val rDiff = Math.abs(android.graphics.Color.red(color1) - android.graphics.Color.red(color2))
                    val gDiff = Math.abs(android.graphics.Color.green(color1) - android.graphics.Color.green(color2))
                    val bDiff = Math.abs(android.graphics.Color.blue(color1) - android.graphics.Color.blue(color2))
                    val found = rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance
                    return if (node.colorOperator == "!=") !found else found
                }
                return false
            }

            if (node.colorCompareX != null && node.colorCompareY != null) {
                val color1 = bitmap.getPixel(cx, cy)
                val cx2 = node.colorCompareX!!.coerceIn(0, bitmap.width - 1)
                val cy2 = node.colorCompareY!!.coerceIn(0, bitmap.height - 1)
                val color2 = bitmap.getPixel(cx2, cy2)
                
                val rDiff = Math.abs(android.graphics.Color.red(color1) - android.graphics.Color.red(color2))
                val gDiff = Math.abs(android.graphics.Color.green(color1) - android.graphics.Color.green(color2))
                val bDiff = Math.abs(android.graphics.Color.blue(color1) - android.graphics.Color.blue(color2))
                val found = rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance
                return if (node.colorOperator == "!=") !found else found
            }
            
            if (node.dynamicColorUpdate) {
                val color = bitmap.getPixel(cx, cy)
                if (node.targetColor == null) {
                    node.targetColor = color // save initial state
                    return false // acts as setup delay, wait for next cycle to actually check change
                }
                val c1 = node.targetColor!!
                val rDiff = Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(color))
                val gDiff = Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(color))
                val bDiff = Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(color))
                val found = rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance
                
                val result = if (node.colorOperator == "!=") !found else found
                if (!found) {
                    node.targetColor = color // update tracked color
                }
                return result
            }
            
            if (node.targetColor != null) {
                val r = node.searchRadius
                if (r == 0) {
                    val color = bitmap.getPixel(cx, cy)
                    val c1 = node.targetColor!!
                    val rDiff = Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(color))
                    val gDiff = Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(color))
                    val bDiff = Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(color))
                    val found = rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance
                    return if (node.colorOperator == "!=") !found else found
                } else {
                    val searchRectLeft = (node.x - r).coerceIn(0, bitmap.width - 1)
                    val searchRectTop = (node.y - r).coerceIn(0, bitmap.height - 1)
                    val searchRectRight = (node.x + r).coerceIn(0, bitmap.width - 1)
                    val searchRectBottom = (node.y + r).coerceIn(0, bitmap.height - 1)
                    
                    val sW = Math.max(searchRectRight - searchRectLeft, 1)
                    val sH = Math.max(searchRectBottom - searchRectTop, 1)
                    
                    val pSearch = IntArray(sW * sH)
                    bitmap.getPixels(pSearch, 0, sW, searchRectLeft, searchRectTop, sW, sH)
                    
                    val c1 = node.targetColor!!
                    var found = false
                    val checkStep = (1f / node.checkResolutionScale.coerceIn(0.1f, 1.0f)).toInt().coerceAtLeast(1)
                    val step = maxOf(if (r > 10) 2 else 1, checkStep)
                    
                    for (y in 0 until sH step step) {
                        for (x in 0 until sW step step) {
                            val color = pSearch[y * sW + x]
                            val rDiff = Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(color))
                            val gDiff = Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(color))
                            val bDiff = Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(color))
                            if (rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance) {
                                found = true
                                break
                            }
                        }
                        if (found) break
                    }
                    return if (node.colorOperator == "!=") !found else found
                }
            }
        }
        
        return true
    }

    private fun checkColorsAt(points: List<Pair<Int, Int>>, callback: (List<Int?>) -> Unit) {
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val colors = mutableListOf<Int?>()
                    for (i in points.indices) colors.add(null)
                    try {
                        val buffer = screenshot.hardwareBuffer
                        val hwBitmap = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                        if (hwBitmap != null) {
                            val bitmap = hwBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            if (bitmap != null) {
                                for (i in points.indices) {
                                    val point = points[i]
                                    val cx = point.first.coerceIn(0, bitmap.width - 1)
                                    val cy = point.second.coerceIn(0, bitmap.height - 1)
                                    colors[i] = bitmap.getPixel(cx, cy)
                                }
                                bitmap.recycle()
                            }
                            hwBitmap.recycle()
                        }
                        buffer.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    callback(colors)
                }
                override fun onFailure(errorCode: Int) {
                    val fallback = points.map { null }
                    callback(fallback)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = points.map { null }
            callback(fallback)
        }
    }

    var isRecording = false
    private var recordOverlay: android.view.View? = null
    private var recordDownX: Float = 0f
    private var recordDownY: Float = 0f
    private var recordDownTime: Long = 0L
    private var lastRecordDownTime: Long = 0L
    private var lastRecordedNodeId: Int? = null
    private var isDispatchingRecordGesture = false

    private fun showRecordOverlay() {
        if (recordOverlay != null) return
        recordOverlay = android.widget.FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            val borderView = android.view.View(context).apply {
                val drawable = android.graphics.drawable.GradientDrawable()
                drawable.setColor(android.graphics.Color.TRANSPARENT)
                drawable.setStroke(uiManager.dpToPx(4), android.graphics.Color.parseColor("#88FF0000")) // Semi-transparent red border
                background = drawable
            }
            addView(borderView, android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                val margin = uiManager.dpToPx(2)
                setMargins(margin, margin, margin, margin)
            })

            var dragX = 0f
            var dragY = 0f
            var isDragging = false
            val swipePoints = mutableListOf<Pair<Float, Float>>()
            
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#8800FFFF")
                strokeWidth = uiManager.dpToPx(4).toFloat()
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
            }

            val drawingView = object : android.view.View(context) {
                private val overlayPathCache = android.graphics.Path()
                override fun onDraw(canvas: android.graphics.Canvas) {
                    super.onDraw(canvas)
                    if (isDragging && swipePoints.size >= 2) {
                        overlayPathCache.reset()
                        overlayPathCache.moveTo(swipePoints[0].first, swipePoints[0].second)
                        for (i in 1 until swipePoints.size) {
                            overlayPathCache.lineTo(swipePoints[i].first, swipePoints[i].second)
                        }
                        overlayPathCache.lineTo(dragX, dragY)
                        canvas.drawPath(overlayPathCache, paint)
                    } else if (isDragging) {
                        canvas.drawLine(recordDownX, recordDownY, dragX, dragY, paint)
                    }
                }
            }
            addView(drawingView, android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ))

            setOnTouchListener { _, event ->
                if (isDispatchingRecordGesture) return@setOnTouchListener true
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        if (::uiManager.isInitialized && uiManager.floatingControlBar != null) {
                            val hudRect = android.graphics.Rect()
                            uiManager.floatingControlBar?.getGlobalVisibleRect(hudRect)
                            val pad = 10
                            if (event.rawX >= hudRect.left - pad && event.rawX <= hudRect.right + pad &&
                                event.rawY >= hudRect.top - pad && event.rawY <= hudRect.bottom + pad) {
                                return@setOnTouchListener false
                            }
                        }
                        val currentTime = System.currentTimeMillis()
                        recordDownX = event.rawX
                        recordDownY = event.rawY
                        dragX = event.x
                        dragY = event.y
                        isDragging = true
                        swipePoints.clear()
                        swipePoints.add(Pair(event.x, event.y))
                        drawingView.invalidate()
                        recordDownTime = currentTime

                        if (lastRecordDownTime > 0L && lastRecordedNodeId != null) {
                            val delay = currentTime - lastRecordDownTime
                            nodes.find { it.id == lastRecordedNodeId }?.delayAfterMs = delay
                        }
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        dragX = event.x
                        dragY = event.y
                        val last = swipePoints.lastOrNull()
                        if (last == null || Math.hypot((event.x - last.first).toDouble(), (event.y - last.second).toDouble()) > 10) {
                            swipePoints.add(Pair(event.x, event.y))
                        }
                        drawingView.invalidate()
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        isDragging = false
                        swipePoints.add(Pair(event.x, event.y))
                        drawingView.invalidate()
                        val duration = System.currentTimeMillis() - recordDownTime
                        val startX = recordDownX
                        val startY = recordDownY
                        val upX = event.rawX
                        val upY = event.rawY
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val dx = upX - startX
                            val dy = upY - startY
                            val isSwipe = Math.hypot(dx.toDouble(), dy.toDouble()) > 10
                            
                            val path = android.graphics.Path().apply {
                                if (isSwipe && swipePoints.size >= 2) {
                                    moveTo(startX, startY)
                                    for (i in 1 until swipePoints.size) {
                                        // Mapping visual local coords back to global raw coords for correct gesture execution
                                        val pX = startX + (swipePoints[i].first - swipePoints[0].first)
                                        val pY = startY + (swipePoints[i].second - swipePoints[0].second)
                                        lineTo(pX, pY)
                                    }
                                } else {
                                    moveTo(startX, startY)
                                    if (isSwipe) lineTo(upX, upY)
                                }
                            }
                            val gestureDur = if (isSwipe) Math.max(duration, 30L) else Math.max(duration, 10L)
                            
                            val gesture = android.accessibilityservice.GestureDescription.Builder()
                                .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, gestureDur))
                                .build()
                                
                            val wm = getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
                            val rlParams = recordOverlay?.layoutParams as? android.view.WindowManager.LayoutParams
                            if (rlParams != null) {
                                rlParams.flags = rlParams.flags or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                wm.updateViewLayout(recordOverlay, rlParams)
                            }

                            val node = uiManager.addNodeAndReturn(NodeType.CLICK, startX.toInt(), startY.toInt())
                            if (isSwipe) {
                                node.isSwipe = true
                                node.swipeEndX = upX.toInt()
                                node.swipeEndY = upY.toInt()
                                node.swipeDurationMs = gestureDur
                                node.swipePathPoints = swipePoints.toList()
                                if (::uiManager.isInitialized) {
                                    uiManager.createSwipeEndMarker(node)
                                }
                            } else {
                                node.clickDurationMs = gestureDur
                            }
                            
                            lastRecordedNodeId = node.id
                            lastRecordDownTime = recordDownTime
                            
                            if (::uiManager.isInitialized) {
                                uiManager.logDebug("[Запись] Создана метка ${node.id} (${if (isSwipe) "Свайп" else "Клик"})")
                                uiManager.invalidateLines() // If we add lines drawing later
                            }

                            recordOverlay?.post({
                                isDispatchingRecordGesture = true
                                dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                                    override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                                        isDispatchingRecordGesture = false
                                        if (recordOverlay != null && rlParams != null) {
                                            rlParams.flags = rlParams.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                            wm.updateViewLayout(recordOverlay, rlParams)
                                        }
                                    }
                                    override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                                        isDispatchingRecordGesture = false
                                        if (recordOverlay != null && rlParams != null) {
                                            rlParams.flags = rlParams.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                                            wm.updateViewLayout(recordOverlay, rlParams)
                                        }
                                    }
                                }, null)
                            })
                        }
                    }
                }
                true
            }
        }
        val params = android.view.WindowManager.LayoutParams(
            android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        (getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).addView(recordOverlay, params)
        uiManager.bringControlBarToFront()
    }

    private fun hideRecordOverlay() {
        if (recordOverlay != null) {
            (getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager).removeView(recordOverlay)
            recordOverlay = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 5) {
                    if (::uiManager.isInitialized) {
                        uiManager.toggleHudVisibility()
                    }
                    return true
                }
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        isPlaying = false
        isRecording = false
        hideRecordOverlay()
        if (::uiManager.isInitialized) {
            restoreTouchabilitySafe()
            uiManager.updateMenu()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
        isRecording = false
        hideRecordOverlay()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {}
        if (::uiManager.isInitialized) {
            uiManager.removeAllViews()
        }
        cachedBitmap?.recycle()
        cachedBitmap = null
        if (mlTextAnalyzer != null) {
            try { mlTextAnalyzer!!.stop() } catch (e: Exception) {}
            mlTextAnalyzer = null
        }
        handler.removeCallbacksAndMessages(null)
        nodes.clear()
        activeThreads.clear()
        if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()
        gestureQueue.clear()
        screenshotCallbacks.clear()
        instance = null
    }

    fun getHotbarItems(): List<Pair<String, String>> {
        val prefs = getSharedPreferences("AutoClickerPrefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("HotbarItems", null)
        val list = mutableListOf<Pair<String, String>>()
        if (jsonStr != null) {
            try {
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(Pair(obj.getString("name"), obj.optString("label", obj.getString("name"))))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (list.isEmpty()) {
            getSavedProfiles().take(5).forEach { list.add(Pair(it, it)) }
        }
        return list
    }

    fun saveHotbarItems(items: List<Pair<String, String>>) {
        val prefs = getSharedPreferences("AutoClickerPrefs", android.content.Context.MODE_PRIVATE)
        val arr = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject()
            obj.put("name", item.first)
            obj.put("label", item.second)
            arr.put(obj)
        }
        prefs.edit().putString("HotbarItems", arr.toString()).apply()
    }

    fun getSavedProfiles(): List<String> {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        return prefs.all.keys.toList()
    }

    fun deleteProfile(name: String) {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(name).apply()
        android.widget.Toast.makeText(this, "Сценарий '$name' удален", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun autoSave() {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val obj = org.json.JSONObject()
        val metrics = resources.displayMetrics
        obj.put("screenWidth", metrics.widthPixels)
        obj.put("screenHeight", metrics.heightPixels)
        val arr = org.json.JSONArray()
        for (node in nodes) {
            arr.put(node.toJson())
        }
        obj.put("nodes", arr)
        prefs.edit().putString("Автосохранение", obj.toString()).apply()
    }

    fun saveProfile(name: String) {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val obj = org.json.JSONObject()
        val metrics = resources.displayMetrics
        obj.put("screenWidth", metrics.widthPixels)
        obj.put("screenHeight", metrics.heightPixels)
        val arr = org.json.JSONArray()
        for (node in nodes) {
            arr.put(node.toJson())
        }
        obj.put("nodes", arr)
        prefs.edit().putString(name, obj.toString()).apply()
        android.widget.Toast.makeText(this, "Сценарий '$name' сохранен", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun exportProfileToClipboard() {
        val obj = org.json.JSONObject()
        val metrics = resources.displayMetrics
        obj.put("screenWidth", metrics.widthPixels)
        obj.put("screenHeight", metrics.heightPixels)
        val arr = org.json.JSONArray()
        for (node in nodes) {
            arr.put(node.toJson())
        }
        obj.put("nodes", arr)
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("AutoClickerProfile", obj.toString())
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(this, "Сценарий скопирован в буфер обмена", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun importProfileFromClipboard() {
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrEmpty()) {
                try {
                    loadProfileFromJson(text)
                    android.widget.Toast.makeText(this, "Сценарий импортирован", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(this, "Ошибка импорта: неверный формат (не JSON)", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(this, "Буфер обмена пуст", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(this, "Буфер обмена пуст", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun loadProfileFromJson(jsonText: String, append: Boolean = false) {
        val tokener = org.json.JSONTokener(jsonText)
        val root = tokener.nextValue()
        
        var arr: org.json.JSONArray
        var scaleX = 1f
        var scaleY = 1f
        
        if (root is org.json.JSONObject) {
            arr = root.getJSONArray("nodes")
            if (root.has("screenWidth") && root.has("screenHeight")) {
                val origWidth = root.getInt("screenWidth")
                val origHeight = root.getInt("screenHeight")
                val metrics = resources.displayMetrics
                val curWidth = metrics.widthPixels
                val curHeight = metrics.heightPixels
                if (origWidth > 0 && curWidth > 0) scaleX = curWidth.toFloat() / origWidth
                if (origHeight > 0 && curHeight > 0) scaleY = curHeight.toFloat() / origHeight
            }
        } else if (root is org.json.JSONArray) {
            arr = root
        } else {
            throw Exception("Invalid JSON root type")
        }

        if (!append) {
            isPlaying = false
            nodes.clear()
            uiManager.nodeViews.values.forEach { uiManager.windowManager.removeView(it) }
            uiManager.nodeViews.clear()
            uiManager.nodeParams.clear()
            uiManager.swipeEndViews.values.forEach { uiManager.windowManager.removeView(it) }
            uiManager.swipeEndViews.clear()
            uiManager.swipeEndParams.clear()
            uiManager.textZoneStartViews.values.forEach { uiManager.windowManager.removeView(it) }
            uiManager.textZoneStartViews.clear()
            uiManager.textZoneStartParams.clear()
            uiManager.textZoneEndViews.values.forEach { uiManager.windowManager.removeView(it) }
            uiManager.textZoneEndViews.clear()
            uiManager.textZoneEndParams.clear()
        }
        
        var maxId = if (nodes.isNotEmpty()) nodes.maxOf { it.id } else 0
        val idOffset = if (append) maxId + 1 else 0
        
        val newNodes = mutableListOf<TargetNode>()
        for (i in 0 until arr.length()) {
            val node = TargetNode.fromJson(arr.getJSONObject(i))
            
            if (scaleX != 1f || scaleY != 1f) {
                node.x = (node.x * scaleX).toInt()
                node.y = (node.y * scaleY).toInt()
                if (node.swipeEndX != null) node.swipeEndX = (node.swipeEndX!! * scaleX).toInt()
                if (node.swipeEndY != null) node.swipeEndY = (node.swipeEndY!! * scaleY).toInt()
                node.textZoneStartX = (node.textZoneStartX * scaleX).toInt()
                node.textZoneStartY = (node.textZoneStartY * scaleY).toInt()
                node.textZoneEndX = (node.textZoneEndX * scaleX).toInt()
                node.textZoneEndY = (node.textZoneEndY * scaleY).toInt()
                
                if (node.swipePathPoints.isNotEmpty()) {
                    val scaledPoints = mutableListOf<Pair<Float, Float>>()
                    for (p in node.swipePathPoints) {
                        scaledPoints.add(Pair(p.first * scaleX, p.second * scaleY))
                    }
                    node.swipePathPoints = scaledPoints
                }
            }

            if (append) {
                node.id += idOffset
                if (node.swipeTargetNodeId != null && node.swipeTargetNodeId!! >= 0) node.swipeTargetNodeId = node.swipeTargetNodeId!! + idOffset
                if (node.nextNodeIdOnSuccess != null) node.nextNodeIdOnSuccess = node.nextNodeIdOnSuccess!! + idOffset
                if (node.nextNodeIdOnFail != null) node.nextNodeIdOnFail = node.nextNodeIdOnFail!! + idOffset
                if (node.linkedConditionNodeId != null) node.linkedConditionNodeId = node.linkedConditionNodeId!! + idOffset
            }
            newNodes.add(node)
            if (node.id > maxId) maxId = node.id
        }
        
        nodes.addAll(newNodes)
        uiManager.nodeCounter = maxId + 1
        
        if (append) {
            uiManager.createViewsForNodes(newNodes)
        } else {
            uiManager.recreateAllNodeViews()
        }
    }

    fun loadProfile(name: String, append: Boolean = false) {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val jsonString = prefs.getString(name, null) ?: return
        try {
            loadProfileFromJson(jsonString, append)
            android.widget.Toast.makeText(this, "Сценарий '$name' ${if(append) "добавлен" else "загружен"}", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Ошибка загрузки", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun performGlobalClick(x: Float, y: Float, duration: Long) {
        val path = android.graphics.Path().apply { moveTo(x, y) }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build()
        gestureQueue.offer(gesture)
        processGestureQueue()
    }
}
