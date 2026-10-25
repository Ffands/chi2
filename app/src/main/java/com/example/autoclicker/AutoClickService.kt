package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.MotionEvent
import android.view.View
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
    
    
    private fun normalizeSuffixToken(s: String): String {
        return s.lowercase()
            .replace("тыс", "k")
            .replace("млн", "m")
            .replace("млрд", "b")
            .replace("трлн", "t")
            .replace("mln", "m")
            .replace("bln", "b")
            .replace("trn", "t")
            .replace('к', 'k')
            .replace('м', 'm')
            .replace('б', 'b')
            .replace('в', 'b')
            .replace('т', 't')
    }

    private fun buildSuffixMap(suffixes: String): List<Pair<String, Double>> {
        val suffixEntries = mutableListOf<Pair<String, Double>>(
            "тыс" to 1_000.0,
            "млн" to 1_000_000.0,
            "млрд" to 1_000_000_000.0,
            "трлн" to 1_000_000_000_000.0,
            "mln" to 1_000_000.0,
            "bln" to 1_000_000_000.0,
            "kk" to 1_000_000.0,
            "кк" to 1_000_000.0,
            "t" to 1_000_000_000_000.0,
            "т" to 1_000_000_000_000.0
        )
        if (suffixes.isNotEmpty()) {
            for (p in suffixes.split(Regex("[,;\\n]+"))) {
                val kv = p.split(Regex("[:=]"))
                if (kv.size == 2) {
                    val key = kv[0].trim().lowercase()
                    val mult = kv[1].trim().toDoubleOrNull()
                    if (key.isNotEmpty() && mult != null) {
                        suffixEntries.removeAll { it.first == key }
                        suffixEntries.add(Pair(key, mult))
                    }
                }
            }
        }
        suffixEntries.sortByDescending { it.first.length }
        return suffixEntries
    }

    fun getSuffixSetsMap(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        map["Стандарт (K, M, B, T)"] = "k:1000,m:1000000,b:1000000000,t:1000000000000,к:1000,м:1000000,б:1000000000,т:1000000000000"
        map["Idle-игры (K..Qi, Sx, Sp)"] = "k:1e3,m:1e6,b:1e9,t:1e12,qa:1e15,qi:1e18,sx:1e21,sp:1e24,oc:1e27,no:1e30,dc:1e33"
        map["Русские (тыс, млн, млрд)"] = "тыс:1e3,млн:1e6,млрд:1e9,трлн:1e12,к:1e3,м:1e6,б:1e9"
        map["Буквенные (AA, AB, AC..)"] = "k:1e3,m:1e6,b:1e9,t:1e12,aa:1e15,ab:1e18,ac:1e21,ad:1e24,ae:1e27,af:1e30"
        try {
            val prefs = getSharedPreferences("AutoClickerSettings", android.content.Context.MODE_PRIVATE)
            val jsonStr = prefs.getString("SavedSuffixSetsJson", null)
            if (!jsonStr.isNullOrEmpty()) {
                val obj = org.json.JSONObject(jsonStr)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = obj.optString(k, "")
                    if (k.isNotEmpty() && v.isNotEmpty()) {
                        map[k] = v
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun saveSuffixSet(setName: String, suffixesStr: String) {
        val cleanName = setName.trim()
        val cleanSuf = suffixesStr.trim()
        if (cleanName.isEmpty() || cleanSuf.isEmpty()) return
        val map = getSuffixSetsMap()
        map[cleanName] = cleanSuf
        val obj = org.json.JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        getSharedPreferences("AutoClickerSettings", android.content.Context.MODE_PRIVATE)
            .edit().putString("SavedSuffixSetsJson", obj.toString()).apply()
    }

    fun deleteSuffixSet(setName: String) {
        val map = getSuffixSetsMap()
        map.remove(setName)
        val obj = org.json.JSONObject()
        for ((k, v) in map) {
            obj.put(k, v)
        }
        getSharedPreferences("AutoClickerSettings", android.content.Context.MODE_PRIVATE)
            .edit().putString("SavedSuffixSetsJson", obj.toString()).apply()
    }

    fun getEffectiveOcrSuffixes(node: TargetNode): String {
        if (node.ocrSuffixSetName.isNotBlank()) {
            val map = getSuffixSetsMap()
            map[node.ocrSuffixSetName]?.let { return it }
        }
        if (node.ocrCustomSuffixes.isNotBlank()) return node.ocrCustomSuffixes
        return "k:1000,m:1000000,b:1000000000,t:1000000000000,к:1000,м:1000000,б:1000000000,т:1000000000000"
    }

    private fun parseAllNumericValues(text: String, suffixes: String): List<Double> {
        if (text.isBlank()) return emptyList()
        // Fix common OCR misreads inside and at the end of numbers: O/o/О/о after digits -> 0
        var cleaned = text.trim()
            .replace('—', '-')
            .replace('–', '-')

        // Repeatedly convert O/o/О/о adjacent to digits into '0' (handles "1OO", "10O", "O.5")
        var prev: String
        do {
            prev = cleaned
            cleaned = cleaned
                .replace(Regex("(?<=\\d)[oOоО]"), "0")
                .replace(Regex("[oOоО](?=[.,]?\\d)"), "0")
        } while (cleaned != prev)

        // Collapse spaces only between digit groups (e.g. "1 250 000" -> "1250000"), preserving word boundaries for suffixes
        cleaned = cleaned.replace(Regex("(?<=\\d)[ \\t]+(?=\\d{3}\\b)"), "")

        val suffixEntries = buildSuffixMap(suffixes)
        // Match number optionally followed by a standalone letter token (suffix candidate)
        val regex = Regex("(-?\\d+(?:[.,]\\d+)*)(?:[ \\t]*([a-zA-Zа-яА-ЯёЁ]+))?")
        val results = mutableListOf<Double>()

        for (match in regex.findAll(cleaned)) {
            var numPart = match.groupValues[1]
            val sufPart = match.groupValues[2].lowercase()

            if (numPart.contains(",") && numPart.contains(".")) {
                val lastComma = numPart.lastIndexOf(',')
                val lastDot = numPart.lastIndexOf('.')
                numPart = if (lastComma > lastDot) {
                    numPart.replace(".", "").replace(",", ".")
                } else {
                    numPart.replace(",", "")
                }
            } else if (numPart.contains(",")) {
                val commaCount = numPart.count { it == ',' }
                if (commaCount > 1) {
                    numPart = numPart.replace(",", "")
                } else {
                    val afterComma = numPart.substringAfter(',')
                    numPart = if (sufPart.isEmpty() && afterComma.length == 3 && !numPart.startsWith("0,") && !numPart.startsWith("-0,")) {
                        numPart.replace(",", "")
                    } else {
                        numPart.replace(",", ".")
                    }
                }
            } else if (numPart.count { it == '.' } > 1) {
                numPart = numPart.replace(".", "")
            }

            val value = numPart.toDoubleOrNull() ?: continue

            var multiplier = 1.0
            if (sufPart.isNotEmpty()) {
                val normSufPart = normalizeSuffixToken(sufPart)
                // Require exact token match (or normalized exact match) so words like "монет" or "кристаллов" do not falsely match "м" or "к"
                for ((suf, mult) in suffixEntries) {
                    if (sufPart == suf || normSufPart == normalizeSuffixToken(suf)) {
                        multiplier = mult
                        break
                    }
                }
            }

            results.add(value * multiplier)
        }

        return results
    }

    private fun evaluateSmartOcrCondition(value: Double, operator: String, compareValue: Double): Boolean {
        return when (operator) {
            ">" -> value > compareValue
            "<" -> value < compareValue
            ">=" -> value >= compareValue
            "<=" -> value <= compareValue
            "==" -> Math.abs(value - compareValue) < 1e-6
            "!=" -> Math.abs(value - compareValue) >= 1e-6
            else -> false
        }
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
    var cycleEndDelayMs = 0L
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
    
    // Gesture Queue & Background Holds
    data class QueuedGesture(
        val gesture: android.accessibilityservice.GestureDescription,
        val durationMs: Long,
        val isSwipe: Boolean,
        val swipeNodeIds: List<Int> = emptyList()
    )
    private val gestureQueue = java.util.concurrent.ConcurrentLinkedDeque<QueuedGesture>()
    private val activeSwipeNodeEndMs = java.util.concurrent.ConcurrentHashMap<Int, Long>()
    private var isDispatchingGesture = false
    private var currentGestureEndTimeMs = 0L
    private var currentGestureIsLong = false
    private var lastGestureDispatchTimeNs = 0L
    private val processGestureRunnable = Runnable { processGestureQueue() }

    data class ActiveHold(
        val nodeId: Int,
        val x: Float,
        val y: Float,
        val endTimeMs: Long
    )
    private val activeHolds = java.util.concurrent.CopyOnWriteArrayList<ActiveHold>()
    private var isHoldSustaining = false

    private fun sustainActiveHolds() {
        if (!isPlaying) {
            activeHolds.clear()
            isHoldSustaining = false
            return
        }
        val nowMs = System.currentTimeMillis()
        activeHolds.removeAll { it.endTimeMs <= nowMs + 15L }
        if (activeHolds.isEmpty() || !gestureQueue.isEmpty()) {
            isHoldSustaining = false
            return
        }

        val maxStrokes = GestureDescription.getMaxStrokeCount()
        val builder = GestureDescription.Builder()
        var added = 0
        for (hold in activeHolds.take(maxStrokes)) {
            val rem = (hold.endTimeMs - nowMs).coerceIn(10L, 60000L)
            val path = Path().apply {
                moveTo(hold.x, hold.y)
                lineTo(hold.x + 1f, hold.y + 1f)
            }
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, rem))
            added++
        }
        if (added == 0) return

        try {
            val gesture = builder.build()
            isDispatchingGesture = true
            isHoldSustaining = true
            val dispatched = dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    isDispatchingGesture = false
                    isHoldSustaining = false
                    if (isPlaying) {
                        if (!gestureQueue.isEmpty()) {
                            processGestureQueue()
                        } else {
                            val checkNow = System.currentTimeMillis()
                            activeHolds.removeAll { it.endTimeMs <= checkNow + 15L }
                            if (activeHolds.isNotEmpty()) {
                                sustainActiveHolds()
                            }
                        }
                    }
                }
                override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                    isDispatchingGesture = false
                    isHoldSustaining = false
                    if (!gestureQueue.isEmpty() && isPlaying) {
                        processGestureQueue()
                    }
                }
            }, null)
            if (!dispatched) {
                isDispatchingGesture = false
                isHoldSustaining = false
            }
        } catch (e: Exception) {
            isDispatchingGesture = false
            isHoldSustaining = false
        }
    }

    private fun processGestureQueue() {
        if (!isPlaying) {
            gestureQueue.clear()
            activeSwipeNodeEndMs.clear()
            activeHolds.clear()
            isHoldSustaining = false
            currentGestureIsLong = false
            return
        }
        if (gestureQueue.isEmpty()) {
            val nowMs = System.currentTimeMillis()
            activeHolds.removeAll { it.endTimeMs <= nowMs + 15L }
            if (activeHolds.isNotEmpty() && !isDispatchingGesture) {
                sustainActiveHolds()
            }
            return
        }

        // Do not interrupt an in-progress swipe or long gesture; wait for onCompleted
        val nowMs = System.currentTimeMillis()
        if (isDispatchingGesture && !isHoldSustaining && currentGestureIsLong && nowMs < currentGestureEndTimeMs + 40L) {
            val waitMs = (currentGestureEndTimeMs - nowMs + 10L).coerceAtLeast(5L)
            handler.removeCallbacks(processGestureRunnable)
            handler.postDelayed(processGestureRunnable, waitMs)
            return
        }
        
        // Pacing: 3.1ms without flag (~300-320 CPS), 1.6ms with extreme flag (~600-625 CPS)
        val minIntervalNs = if (allowExtremeSpeed) 1_600_000L else 3_100_000L
        val now = System.nanoTime()
        val elapsed = now - lastGestureDispatchTimeNs
        
        if (elapsed < minIntervalNs) {
            val remainingNs = minIntervalNs - elapsed
            handler.removeCallbacks(processGestureRunnable)
            if (remainingNs < 1_500_000L) {
                handler.post(processGestureRunnable)
            } else {
                handler.postDelayed(processGestureRunnable, remainingNs / 1_000_000L)
            }
            return
        }
        
        val queued = gestureQueue.pollFirst() ?: return
        val gesture = queued.gesture
        lastGestureDispatchTimeNs = System.nanoTime()
        val dispatchStartMs = System.currentTimeMillis()
        
        isDispatchingGesture = true
        isHoldSustaining = false
        currentGestureIsLong = queued.isSwipe || queued.durationMs > 15L
        currentGestureEndTimeMs = dispatchStartMs + queued.durationMs
        for (id in queued.swipeNodeIds) {
            activeSwipeNodeEndMs[id] = currentGestureEndTimeMs
        }
        val success = dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                currentGestureIsLong = false
                for (id in queued.swipeNodeIds) {
                    activeSwipeNodeEndMs.remove(id)
                }
                if (isPlaying) {
                    if (!gestureQueue.isEmpty()) {
                        processGestureQueue()
                    } else {
                        val checkNow = System.currentTimeMillis()
                        activeHolds.removeAll { it.endTimeMs <= checkNow + 15L }
                        if (activeHolds.isNotEmpty()) {
                            sustainActiveHolds()
                        }
                    }
                }
            }
            override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                currentGestureIsLong = false
                for (id in queued.swipeNodeIds) {
                    activeSwipeNodeEndMs.remove(id)
                }
                if (isPlaying) {
                    if (!gestureQueue.isEmpty()) {
                        processGestureQueue()
                    } else {
                        val checkNow = System.currentTimeMillis()
                        activeHolds.removeAll { it.endTimeMs <= checkNow + 15L }
                        if (activeHolds.isNotEmpty()) {
                            sustainActiveHolds()
                        }
                    }
                }
            }
        }, null)
        
        if (!success) {
            isDispatchingGesture = false
            currentGestureIsLong = false
            gestureQueue.addFirst(queued)
            handler.removeCallbacks(processGestureRunnable)
            handler.postDelayed(processGestureRunnable, 5L)
        } else {
            if (!gestureQueue.isEmpty() && isPlaying) {
                handler.removeCallbacks(processGestureRunnable)
                if (currentGestureIsLong) {
                    handler.postDelayed(processGestureRunnable, queued.durationMs + 30L)
                } else {
                    val remainingNs = minIntervalNs - (System.nanoTime() - lastGestureDispatchTimeNs)
                    if (remainingNs <= 0L || remainingNs < 1_500_000L) {
                        handler.post(processGestureRunnable)
                    } else {
                        handler.postDelayed(processGestureRunnable, remainingNs / 1_000_000L)
                    }
                }
            }
        }
    }
    
    private var lastHighlightedIds: List<Int>? = null
    fun updateHighlight() {
        if (::uiManager.isInitialized) {
            val activeIds = activeThreads.mapNotNull { it.currentNodeId }
            if (activeIds != lastHighlightedIds) {
                lastHighlightedIds = activeIds
                uiManager.updateCurrentNodeHighlight(activeIds)
            }
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
            } ?: run {
                if (nodes.isEmpty()) {
                    val profilePrefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
                    val savedAuto = profilePrefs.getString("Автосохранение", null)
                    if (!savedAuto.isNullOrEmpty()) {
                        try {
                            loadProfileFromJson(savedAuto, append = false)
                        } catch (_: Exception) {}
                    }
                }
            }
            
            if (uiManager.isHudVisible) {
                uiManager.showFloatingControlBar()
            }
            
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
                uiManager.isHudVisible = true
                uiManager.saveUISettings()
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
            autoSave()
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
            activeSwipeNodeEndMs.clear()
            activeHolds.clear()
            isHoldSustaining = false
            currentGestureIsLong = false
            if (::uiManager.isInitialized) uiManager.logDebug("--- СТОП ---")
            restoreTouchabilitySafe()
        } else {
            if (nodes.isEmpty()) return
            isPlaying = true
            currentCycle = 0
            playStartTimeMs = System.currentTimeMillis()
            activeThreads.clear()
            isDispatchingGesture = false
            currentGestureIsLong = false
            gestureQueue.clear()
            activeSwipeNodeEndMs.clear()
            activeHolds.clear()
            isHoldSustaining = false
            for (n in nodes) {
                n.colorHistory.clear()
                n.lastFoundCenterX = null
                n.lastFoundCenterY = null
                if (n.dynamicColorUpdate) {
                    n.targetColor = null
                }
            }
            if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()
            
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
            thread.currentNodeId = managerNode.nextNodeIdOnFail ?: getNextNodeLinear(managerNode.id, currentNodesList)
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
        val effectiveDelay = Limits.getStepDelay(allowExtremeSpeed, delayMs)
        if (effectiveDelay <= 0L) {
            handler.post { executeThread(thread) }
        } else {
            handler.postDelayed({ executeThread(thread) }, effectiveDelay)
        }
    }

    private fun executeThread(thread: ExecutionThread) {
        if (!isPlaying || !thread.isActive) return

        if (thread.currentNodeId == -1) {
            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
                thread.phantomId = frame.phantomId
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            thread.isActive = false
            if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
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
                if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
                thread.phantomId = frame.phantomId
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
                val cycleDelay = Limits.getCycleEndDelay(allowExtremeSpeed, cycleEndDelayMs)
                if (cycleDelay <= 0L) {
                    if (gestureQueue.size >= 2) {
                        handler.postDelayed({ executeThread(thread) }, 1L)
                    } else {
                        handler.post { executeThread(thread) }
                    }
                } else {
                    handler.postDelayed({ executeThread(thread) }, cycleDelay)
                }
            } else {
                thread.isActive = false
                checkAllThreadsStopped()
            }
            return
        }

        if (node.type == NodeType.CLICK && node.isSwipe) {
            val ongoingRem = (activeSwipeNodeEndMs[node.id] ?: 0L) - System.currentTimeMillis()
            if (ongoingRem > 5L || gestureQueue.any { node.id in it.swipeNodeIds }) {
                handler.postDelayed({ executeThread(thread) }, ongoingRem.coerceAtLeast(10L))
                return
            }
        }

        if (activeThreads.firstOrNull()?.threadId == thread.threadId && (!allowExtremeSpeed || thread.currentRepetition == 0)) {
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
            
            var actionDurationMs = 0L
            if (isMatch) {
                thread.currentCheckCycle = 0
                val isMultiTargetClick = node.triggerMode == 1 &&
                    node.imageMultiTarget &&
                    node.lastFoundCenters.size > 1 &&
                    (node.type == NodeType.CLICK || node.imageClickFoundCenter)

                if (isMultiTargetClick) {
                    val centersToClick = node.lastFoundCenters.toList()
                    if (::uiManager.isInitialized) {
                        val orderLabel = if (node.imageMultiRandomOrder) "случайный порядок" else "по порядку"
                        uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Мульти-цель — найдено ${centersToClick.size} шт. ($orderLabel)")
                    }
                    performMultiTargetClicks(thread, node, centersToClick, 0) {
                        if (!isPlaying || !thread.isActive) return@performMultiTargetClicks
                        thread.currentRepetition++
                        if (thread.currentRepetition < node.repetitions) {
                            thread.currentNodeId = node.id
                        } else {
                            thread.currentRepetition = 0
                            thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        }
                        updateHighlight()
                        val randomDelay = if (node.randomizeDelayMs > 0) (0..node.randomizeDelayMs).random() else 0L
                        val requestedDelay = Math.max(0L, node.delayAfterMs + randomDelay)
                        val finalDelay = Limits.getStepDelay(allowExtremeSpeed, requestedDelay)
                        if (finalDelay <= 0L) {
                            handler.post { executeThread(thread) }
                        } else {
                            handler.postDelayed({ executeThread(thread) }, finalDelay)
                        }
                    }
                    return@checkConditionForNode
                }

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
                    val actionLabel = when {
                        node.isSwipe -> if (node.swipePathPoints.size > 2) "Свайп по траектории (${node.swipePathPoints.size} тчк)" else "Свайп"
                        node.isHold -> "Удержание (${node.holdDurationMs}мс в фоне)"
                        else -> "Клик"
                    }
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: $actionLabel${if (node.triggerMode == 1 && node.imageClickFoundCenter && node.lastFoundCenterX != null) " (в центр фрагмента ${node.lastFoundCenterX},${node.lastFoundCenterY})" else ""}")
                    actionDurationMs = performGestureForNodes(activeNodes)
                } else if (node.triggerMode == 1 && node.imageClickFoundCenter && node.lastFoundCenterX != null && node.lastFoundCenterY != null) {
                    val cx = node.lastFoundCenterX!!.toFloat()
                    val cy = node.lastFoundCenterY!!.toFloat()
                    val dur = Limits.getClickDuration(allowExtremeSpeed, node.clickDurationMs)
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Клик в центр найденного фрагмента (${cx.toInt()}, ${cy.toInt()})")
                    performGlobalClick(cx, cy, dur)
                    actionDurationMs = dur
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
                        thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        scheduleNextExecution(thread, node.delayAfterMs)
                        return@checkConditionForNode
                    }
                    
                    if (node.macroRunParallel) {
                        val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                        val newThread = ExecutionThread(
                            threadId = activeThreads.size + 1,
                            currentNodeId = macroNodes.firstOrNull { !it.skipSequentialExecution }?.id,
                            currentScriptNodes = macroNodes,
                            phantomId = pId
                        )
                        activeThreads.add(newThread)
                        executeThread(newThread)
                        
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        scheduleNextExecution(thread, node.delayAfterMs)
                    } else {
                        val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                        thread.callStack.push(ExecutionFrame(thread.currentScriptNodes, nextId, thread.currentRepetition, thread.phantomId))
                        thread.phantomId = pId
                        
                        thread.currentScriptNodes = macroNodes
                        thread.currentRepetition = 0
                        thread.currentNodeId = macroNodes.firstOrNull { !it.skipSequentialExecution }?.id
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
                    thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                }
            } else {
                if (node.maxCheckCycles != null && node.maxCheckCycles!! > 0) {
                    thread.currentCheckCycle++
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Ждем (${thread.currentCheckCycle}/${node.maxCheckCycles})")
                    if (thread.currentCheckCycle >= node.maxCheckCycles!!) {
                        thread.currentCheckCycle = 0
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnFail ?: getNextNodeLinear(node.id, currentNodesList)
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
            val requestedDelay = Math.max(0L, node.delayAfterMs + randomDelay)
            val baseDelay = if (!isMatch && thread.currentNodeId == node.id) {
                Limits.getCheckDelay(allowExtremeSpeed, node.triggerMode == 1 || node.triggerMode == 2, requestedDelay)
            } else {
                Limits.getStepDelay(allowExtremeSpeed, requestedDelay)
            }
            // Wait for swipe / long gesture completion before starting the post-step pause
            val finalDelay = if (isMatch && (node.isSwipe || actionDurationMs > 15L)) {
                actionDurationMs + baseDelay
            } else {
                baseDelay
            }

            if (node.type == NodeType.CLICK && finalDelay <= Limits.MIN_STEP_DELAY) {
                if (gestureQueue.size >= 2) {
                    handler.postDelayed({ executeThread(thread) }, 1L)
                } else {
                    handler.post { executeThread(thread) }
                }
            } else if (finalDelay <= 0L) {
                handler.post { executeThread(thread) }
            } else {
                handler.postDelayed({ executeThread(thread) }, finalDelay)
            }
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
            activeSwipeNodeEndMs.clear()
            activeHolds.clear()
            isHoldSustaining = false
            currentGestureIsLong = false
            restoreTouchabilitySafe()
            uiManager.updateMenu()
        }
    }

    private fun getNextNodeLinear(currentId: Int, nodesList: List<TargetNode> = this.nodes): Int? {
        val index = nodesList.indexOfFirst { it.id == currentId }
        if (index == -1) return null
        var nextIndex = index + 1
        val nSize = nodesList.size
        
        while (nextIndex < nSize) {
            val n = nodesList[nextIndex]
            if (!n.skipSequentialExecution) {
                return n.id
            }
            nextIndex++
        }
        
        return null // all nodes skipped
    }

    fun buildSmoothSwipePath(startX: Float, startY: Float, points: List<Pair<Float, Float>>): Path {
        val path = Path()
        path.moveTo(startX, startY)
        if (points.isEmpty()) {
            path.lineTo(startX + 1f, startY + 1f)
            return path
        }
        val baseX = points.first().first
        val baseY = points.first().second
        val offsetX = startX - baseX
        val offsetY = startY - baseY
        if (points.size <= 2) {
            val last = points.last()
            path.lineTo((last.first + offsetX).coerceAtLeast(0f), (last.second + offsetY).coerceAtLeast(0f))
            return path
        }
        for (i in 1 until points.size - 1) {
            val p1x = (points[i].first + offsetX).coerceAtLeast(0f)
            val p1y = (points[i].second + offsetY).coerceAtLeast(0f)
            val p2x = (points[i + 1].first + offsetX).coerceAtLeast(0f)
            val p2y = (points[i + 1].second + offsetY).coerceAtLeast(0f)
            val midX = (p1x + p2x) / 2f
            val midY = (p1y + p2y) / 2f
            path.quadTo(p1x, p1y, midX, midY)
        }
        val last = points.last()
        path.lineTo((last.first + offsetX).coerceAtLeast(0f), (last.second + offsetY).coerceAtLeast(0f))
        return path
    }

    private fun buildNodeStroke(node: TargetNode): Pair<GestureDescription.StrokeDescription?, Long> {
        val useFoundCenter = !node.isSwipe && node.triggerMode == 1 && node.imageClickFoundCenter && node.lastFoundCenterX != null && node.lastFoundCenterY != null
        var startX = if (useFoundCenter) node.lastFoundCenterX!!.toFloat() else node.x.toFloat()
        var startY = if (useFoundCenter) node.lastFoundCenterY!!.toFloat() else node.y.toFloat()
        if (node.randomizeRadius > 0) {
            val angle = Math.random() * Math.PI * 2
            val r = Math.random() * node.randomizeRadius
            startX = (startX + (Math.cos(angle) * r).toFloat()).coerceAtLeast(0f)
            startY = (startY + (Math.sin(angle) * r).toFloat()).coerceAtLeast(0f)
        }

        if (node.isHold && !node.isSwipe) {
            val nowMs = System.currentTimeMillis()
            val holdDur = Limits.getSwipeDuration(allowExtremeSpeed, node.holdDurationMs)
            activeHolds.removeAll { it.nodeId == node.id }
            activeHolds.add(ActiveHold(node.id, startX, startY, nowMs + holdDur))
            return Pair(null, holdDur)
        }

        val path: Path
        if (node.isSwipe) {
            if (node.swipeTargetNodeId != null) {
                var eX = node.swipeEndX.toFloat()
                var eY = node.swipeEndY.toFloat()
                val tgtNode = nodes.find { it.id == node.swipeTargetNodeId }
                if (tgtNode != null) {
                    try {
                        uiManager.updateNodeScreenPosition(tgtNode)
                    } catch (_: Exception) {}
                    eX = tgtNode.x.toFloat()
                    eY = tgtNode.y.toFloat()
                }
                if (node.randomizeRadius > 0) {
                    val angle = Math.random() * Math.PI * 2
                    val r = Math.random() * node.randomizeRadius
                    eX = (eX + (Math.cos(angle) * r).toFloat()).coerceAtLeast(0f)
                    eY = (eY + (Math.sin(angle) * r).toFloat()).coerceAtLeast(0f)
                }
                path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(eX, eY)
                }
            } else if (node.swipePathPoints.size >= 2) {
                path = buildSmoothSwipePath(startX, startY, node.swipePathPoints)
            } else {
                var eX = node.swipeEndX.toFloat()
                var eY = node.swipeEndY.toFloat()
                if (node.randomizeRadius > 0) {
                    val angle = Math.random() * Math.PI * 2
                    val r = Math.random() * node.randomizeRadius
                    eX = (eX + (Math.cos(angle) * r).toFloat()).coerceAtLeast(0f)
                    eY = (eY + (Math.sin(angle) * r).toFloat()).coerceAtLeast(0f)
                }
                path = Path().apply {
                    moveTo(startX, startY)
                    lineTo(eX, eY)
                }
            }
        } else {
            path = Path().apply {
                moveTo(startX, startY)
                lineTo(startX + 1f, startY + 1f)
            }
        }

        val duration = if (node.isSwipe) Limits.getSwipeDuration(allowExtremeSpeed, node.swipeDurationMs) else Limits.getClickDuration(allowExtremeSpeed, node.clickDurationMs)
        return Pair(GestureDescription.StrokeDescription(path, 0, duration), duration)
    }

    private fun performGestureForNodes(activeNodes: List<TargetNode>): Long {
        val nowMs = System.currentTimeMillis()
        activeHolds.removeAll { it.endTimeMs <= nowMs + 15L }
        val maxStrokes = GestureDescription.getMaxStrokeCount()
        var maxGestureDur = 0L

        if (enableMultitouch) {
            val builder = GestureDescription.Builder()
            var strokeCount = 0
            var maxNormalDur = 1L
            var hasSwipe = false
            val swipeIds = mutableListOf<Int>()
            for (node in activeNodes) {
                if (strokeCount >= maxStrokes) break
                if (node.isSwipe) {
                    val ongoingRem = (activeSwipeNodeEndMs[node.id] ?: 0L) - nowMs
                    if (ongoingRem > 5L || gestureQueue.any { node.id in it.swipeNodeIds }) {
                        if (ongoingRem > maxGestureDur) maxGestureDur = ongoingRem
                        continue
                    }
                }
                val (stroke, dur) = buildNodeStroke(node)
                if (stroke != null) {
                    builder.addStroke(stroke)
                    if (dur > maxNormalDur) maxNormalDur = dur
                    if (node.isSwipe) {
                        hasSwipe = true
                        swipeIds.add(node.id)
                    }
                    strokeCount++
                }
            }
            if (strokeCount > 0) {
                if (activeHolds.isNotEmpty() && strokeCount < maxStrokes) {
                    for (hold in activeHolds) {
                        if (strokeCount >= maxStrokes) break
                        val rem = (hold.endTimeMs - nowMs).coerceAtLeast(1L)
                        val holdPath = Path().apply {
                            moveTo(hold.x, hold.y)
                            lineTo(hold.x + 1f, hold.y + 1f)
                        }
                        builder.addStroke(GestureDescription.StrokeDescription(holdPath, 0, Math.min(rem, maxNormalDur).coerceAtLeast(1L)))
                        strokeCount++
                    }
                }
                for (id in swipeIds) {
                    activeSwipeNodeEndMs[id] = nowMs + maxNormalDur
                }
                gestureQueue.add(QueuedGesture(builder.build(), maxNormalDur, hasSwipe, swipeIds))
                maxGestureDur = maxNormalDur
            }
        } else {
            for (node in activeNodes) {
                if (node.isSwipe) {
                    val ongoingRem = (activeSwipeNodeEndMs[node.id] ?: 0L) - nowMs
                    if (ongoingRem > 5L || gestureQueue.any { node.id in it.swipeNodeIds }) {
                        if (ongoingRem > maxGestureDur) maxGestureDur = ongoingRem
                        continue
                    }
                }
                val (stroke, dur) = buildNodeStroke(node)
                if (stroke != null) {
                    val builder = GestureDescription.Builder()
                    builder.addStroke(stroke)
                    if (activeHolds.isNotEmpty() && maxStrokes >= 2) {
                        var strokeCount = 1
                        for (hold in activeHolds) {
                            if (strokeCount >= maxStrokes) break
                            val rem = (hold.endTimeMs - nowMs).coerceAtLeast(1L)
                            val holdPath = Path().apply {
                                moveTo(hold.x, hold.y)
                                lineTo(hold.x + 1f, hold.y + 1f)
                            }
                            builder.addStroke(GestureDescription.StrokeDescription(holdPath, 0, Math.min(rem, dur).coerceAtLeast(1L)))
                            strokeCount++
                        }
                    }
                    val swipeIds = if (node.isSwipe) listOf(node.id) else emptyList()
                    if (node.isSwipe) {
                        activeSwipeNodeEndMs[node.id] = nowMs + dur
                    }
                    gestureQueue.add(QueuedGesture(builder.build(), dur, node.isSwipe, swipeIds))
                    if (dur > maxGestureDur) maxGestureDur = dur
                }
            }
        }

        if (gestureQueue.isEmpty()) {
            if (activeHolds.isNotEmpty() && (!isDispatchingGesture || isHoldSustaining)) {
                sustainActiveHolds()
            }
        } else {
            processGestureQueue()
        }
        return maxGestureDur
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
                                if (node.textZoneStartX == 0 && node.textZoneStartY == 0 && node.textZoneEndX == 0 && node.textZoneEndY == 0) {
                                    val padX = if (::uiManager.isInitialized) uiManager.dpToPx(60) else 120
                                    val padY = if (::uiManager.isInitialized) uiManager.dpToPx(40) else 80
                                    node.textZoneStartX = (node.x - padX).coerceIn(0, bitmap.width - 1)
                                    node.textZoneStartY = (node.y - padY).coerceIn(0, bitmap.height - 1)
                                    node.textZoneEndX = (node.x + padX).coerceIn(0, bitmap.width - 1)
                                    node.textZoneEndY = (node.y + padY).coerceIn(0, bitmap.height - 1)
                                }
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
        if (!node.targetText.isNullOrEmpty()) return true
        if (node.isSmartOcr) return true
        if (node.compareZoneEnabled) return true
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
        if (node.triggerMode == 2 && (!node.targetText.isNullOrEmpty() || node.isSmartOcr || node.compareZoneEnabled)) {
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

    private fun recognizeZoneSync(fullBitmap: Bitmap, sx: Int, sy: Int, ex: Int, ey: Int, lang: String): String {
        val left = minOf(sx, ex).coerceIn(0, fullBitmap.width - 1)
        val top = minOf(sy, ey).coerceIn(0, fullBitmap.height - 1)
        val right = maxOf(sx, ex).coerceIn(0, fullBitmap.width - 1)
        val bottom = maxOf(sy, ey).coerceIn(0, fullBitmap.height - 1)
        val w = maxOf(1, right - left)
        val h = maxOf(1, bottom - top)
        if (w < 5 || h < 5) return ""
        val cropped = Bitmap.createBitmap(fullBitmap, left, top, w, h)
        val enhanced = enhanceBitmapForOcr(cropped)
        return try {
            val analyzer = getHuaweiAnalyzer(lang)
            val frame = com.huawei.hms.mlsdk.common.MLFrame.fromBitmap(enhanced)
            val task = analyzer.asyncAnalyseFrame(frame)
            val result = com.huawei.hmf.tasks.Tasks.await(task)
            result?.stringValue ?: ""
        } finally {
            if (enhanced != cropped && !enhanced.isRecycled) enhanced.recycle()
            if (!cropped.isRecycled) cropped.recycle()
        }
    }

    fun testTextRecognition(node: TargetNode, bitmap: Bitmap) {
        val searchStrOrig = node.targetText?.trim() ?: ""
        if (searchStrOrig.isEmpty() && !node.isSmartOcr && !node.compareZoneEnabled) {
            handler.post { Toast.makeText(this, "Пожалуйста, введите искомый текст или включите Умный OCR", Toast.LENGTH_SHORT).show() }
            return
        }
        
        handler.post { Toast.makeText(this, "Тестирование распознавания...", Toast.LENGTH_SHORT).show() }

        val runOcrTestWithOptionalFullBitmap: (Bitmap?) -> Unit = { fullScreenBmp ->
            try {
                val cropped = bitmap
                val w = cropped.width
                val h = cropped.height
                
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

                        var zone2Text = ""
                        if (node.compareZoneEnabled && fullScreenBmp != null) {
                            zone2Text = recognizeZoneSync(
                                fullScreenBmp,
                                node.compareZoneStartX,
                                node.compareZoneStartY,
                                node.compareZoneEndX,
                                node.compareZoneEndY,
                                node.targetLanguage
                            )
                        }
                        
                        val searchStr = normalizeCyrillic(searchStrOrig).replace(" ", "")
                        val recStrOcr = normalizeCyrillic(recognizedText).replace(" ", "")
                        val maxCost = if (searchStr.length <= 3) 0 else if (searchStr.length <= 6) 1 else searchStr.length / 4

                        var isMatch = false
                        var debugMsg = ""
                        var targetSummary = searchStrOrig
                        val effectiveSuffixes = getEffectiveOcrSuffixes(node)
                        
                        if (node.isSmartOcr) {
                            val allValues = parseAllNumericValues(recognizedText, effectiveSuffixes)
                            val compareValue: Double? = when {
                                node.compareZoneEnabled -> {
                                    parseAllNumericValues(zone2Text, effectiveSuffixes).firstOrNull()
                                }
                                node.ocrCompareToNodeId != null -> {
                                    val otherNode = (nodes).find { it.id == node.ocrCompareToNodeId }
                                    otherNode?.lastRecognizedValue ?: node.ocrTargetValue
                                }
                                else -> node.ocrTargetValue
                            }
                            if (compareValue == null) {
                                targetSummary = "[Умный OCR: Сравнение областей]\nОбласть 1: ${allValues.joinToString(", ").ifEmpty { "НЕТ" }}\nОбласть 2 ('$zone2Text'): число не найдено"
                                debugMsg = "Шаг ${node.id}: В Области 2 ('$zone2Text') не удалось распознать число."
                            } else {
                                val matchedVal = allValues.firstOrNull { evaluateSmartOcrCondition(it, node.ocrOperator, compareValue) } ?: allValues.firstOrNull()
                                val cmpSourceDesc = if (node.compareZoneEnabled) "Область 2 ($compareValue, текст '$zone2Text')" else "$compareValue"
                                targetSummary = "[Умный OCR (${node.ocrSuffixSetName})]\nУсловие: Число ${node.ocrOperator} $cmpSourceDesc\nОбласть 1 числа: ${if (allValues.isEmpty()) "НЕТ" else allValues.joinToString(", ")}"
                                if (matchedVal != null) {
                                    node.lastRecognizedValue = matchedVal
                                    isMatch = evaluateSmartOcrCondition(matchedVal, node.ocrOperator, compareValue)
                                    debugMsg = "Шаг ${node.id}: [Смарт OCR] Зона1='$recognizedText' ($matchedVal) ${node.ocrOperator} $cmpSourceDesc -> $isMatch"
                                } else {
                                    debugMsg = "Шаг ${node.id}: [Смарт OCR] Зона1='$recognizedText'. Не удалось распознать число."
                                }
                            }
                        } else if (node.compareZoneEnabled) {
                            val norm1 = normalizeCyrillic(recognizedText).replace(Regex("\\s+"), "").lowercase()
                            val norm2 = normalizeCyrillic(zone2Text).replace(Regex("\\s+"), "").lowercase()
                            val rawEq = norm1.isNotEmpty() && norm1 == norm2
                            isMatch = if (node.colorOperator == "!=") !rawEq else rawEq
                            targetSummary = "Сравнение текста с Областью 2: '$zone2Text' (${node.colorOperator})"
                            debugMsg = "Шаг ${node.id}: [OCR Области] Зона1='$recognizedText' vs Зона2='$zone2Text' -> $isMatch"
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
                                uiManager.showOcrResultDialog(recognizedText, targetSummary, isMatch, finalBmp)
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

        if (node.compareZoneEnabled) {
            requestScreenshot { fullBmp ->
                runOcrTestWithOptionalFullBitmap(fullBmp)
            }
        } else {
            runOcrTestWithOptionalFullBitmap(null)
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
                    val searchStrOrig = node.targetText ?: ""
                    val recognizedText: String
                    var foundRect: android.graphics.Rect? = null
                    val effectiveSuffixes = getEffectiveOcrSuffixes(node)

                    var zone2Text = ""
                    if (node.compareZoneEnabled) {
                        zone2Text = recognizeZoneSync(
                            bitmap,
                            node.compareZoneStartX,
                            node.compareZoneStartY,
                            node.compareZoneEndX,
                            node.compareZoneEndY,
                            node.targetLanguage
                        )
                    }
                    
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
                            val compareValue: Double? = when {
                                node.compareZoneEnabled -> parseAllNumericValues(zone2Text, effectiveSuffixes).firstOrNull()
                                node.ocrCompareToNodeId != null -> {
                                    val otherNode = (nodes).find { it.id == node.ocrCompareToNodeId }
                                    otherNode?.lastRecognizedValue ?: node.ocrTargetValue
                                }
                                else -> node.ocrTargetValue
                            }

                            for (block in result.blocks) {
                                for (line in block.contents) {
                                    if (node.isSmartOcr && compareValue != null) {
                                        val lineVals = parseAllNumericValues(line.stringValue, effectiveSuffixes)
                                        if (lineVals.any { evaluateSmartOcrCondition(it, node.ocrOperator, compareValue) }) {
                                            foundRect = line.border
                                            break
                                        }
                                    } else if (searchStr.isNotEmpty()) {
                                        val blockText = normalizeCyrillic(line.stringValue).replace(" ", "").lowercase()
                                        if (fuzzyContains(blockText, searchStr, maxCost)) {
                                            foundRect = line.border
                                            break
                                        }
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
                        val allValues = parseAllNumericValues(recognizedText, effectiveSuffixes)
                        val compareValue: Double? = when {
                            node.compareZoneEnabled -> parseAllNumericValues(zone2Text, effectiveSuffixes).firstOrNull()
                            node.ocrCompareToNodeId != null -> {
                                val otherNode = (nodes).find { it.id == node.ocrCompareToNodeId }
                                otherNode?.lastRecognizedValue ?: node.ocrTargetValue
                            }
                            else -> node.ocrTargetValue
                        }
                        if (compareValue != null) {
                            val matchedVal = allValues.firstOrNull { evaluateSmartOcrCondition(it, node.ocrOperator, compareValue) } ?: allValues.firstOrNull()
                            if (matchedVal != null) {
                                node.lastRecognizedValue = matchedVal
                                isMatch = evaluateSmartOcrCondition(matchedVal, node.ocrOperator, compareValue)
                                debugMsg = "Шаг ${node.id}: [Смарт OCR] Зона1='$recognizedText' ($matchedVal) ${node.ocrOperator} $compareValue -> $isMatch"
                            } else {
                                debugMsg = "Шаг ${node.id}: [Смарт OCR] Зона1='$recognizedText'. Не удалось распознать число."
                            }
                        } else {
                            debugMsg = "Шаг ${node.id}: [Смарт OCR] В Области 2 ('$zone2Text') не удалось распознать число."
                        }
                    } else if (node.compareZoneEnabled) {
                        val norm1 = normalizeCyrillic(recognizedText).replace(Regex("\\s+"), "").lowercase()
                        val norm2 = normalizeCyrillic(zone2Text).replace(Regex("\\s+"), "").lowercase()
                        isMatch = norm1.isNotEmpty() && norm1 == norm2
                        debugMsg = "Шаг ${node.id}: [OCR Сравнение областей] '$recognizedText' vs '$zone2Text' -> $isMatch"
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
                    
                    val finalResult = if (node.isSmartOcr) isMatch else if (node.colorOperator == "!=") !isMatch else isMatch
                    if (finalResult && node.ocrFullScreenClick && foundRect != null) {
                        val scaleFactor = if (w < 150 || h < 150) 2f else 1.5f
                        val clickX = left + (foundRect.centerX() / scaleFactor)
                        val clickY = top + (foundRect.centerY() / scaleFactor)
                        performGlobalClick(clickX, clickY, node.clickDurationMs)
                    }
                    
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

    private fun performMultiTargetClicks(
        thread: ExecutionThread,
        node: TargetNode,
        centers: List<Pair<Int, Int>>,
        index: Int,
        onAllDone: () -> Unit
    ) {
        if (!isPlaying || !thread.isActive || index >= centers.size) {
            onAllDone()
            return
        }
        val (cx, cy) = centers[index]
        node.lastFoundCenterX = cx
        node.lastFoundCenterY = cy

        var actionDur = 0L
        if (node.type == NodeType.CLICK) {
            val activeNodes = mutableListOf(node)
            if (node.syncWithNodeIds.isNotEmpty()) {
                val idsStr = node.syncWithNodeIds.split(",")
                val currentNodesList = thread.currentScriptNodes ?: this.nodes
                for (idStr in idsStr) {
                    val id = idStr.trim().toIntOrNull()
                    if (id != null) {
                        val syncNode = currentNodesList.find { it.id == id }
                        if (syncNode != null && syncNode.type == NodeType.CLICK) {
                            activeNodes.add(syncNode)
                        }
                    }
                }
            }
            if (::uiManager.isInitialized) {
                uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Цель [${index + 1}/${centers.size}] -> (${cx}, ${cy})")
            }
            actionDur = performGestureForNodes(activeNodes)
        } else {
            val dur = Limits.getClickDuration(allowExtremeSpeed, node.clickDurationMs)
            if (::uiManager.isInitialized) {
                uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Клик по цели [${index + 1}/${centers.size}] -> (${cx}, ${cy})")
            }
            performGlobalClick(cx.toFloat(), cy.toFloat(), dur)
            actionDur = dur
        }

        if (index + 1 < centers.size) {
            val randPause = if (node.imageMultiRandomDelayMs > 0) (0..node.imageMultiRandomDelayMs).random() else 0L
            val interDelay = maxOf(Limits.MIN_STEP_DELAY, node.imageMultiClickDelayMs + randPause) + maxOf(15L, actionDur)
            handler.postDelayed({
                performMultiTargetClicks(thread, node, centers, index + 1, onAllDone)
            }, interDelay)
        } else {
            val finishWait = if (actionDur > 15L) actionDur else 15L
            handler.postDelayed({ onAllDone() }, finishWait)
        }
    }

    private fun ensureTemplateCached(node: TargetNode, screenW: Int, screenH: Int): Boolean {
        val b64 = node.targetImageBase64 ?: return false
        try {
            if (node.cachedTargetBitmap == null || node.cachedTargetPixels == null || node.cachedTargetW <= 0 || node.cachedTargetH <= 0) {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                var bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
                if (bmp.width > screenW || bmp.height > screenH) {
                    val scale = minOf(screenW.toFloat() / bmp.width, screenH.toFloat() / bmp.height)
                    val sw = maxOf(1, (bmp.width * scale).toInt())
                    val sh = maxOf(1, (bmp.height * scale).toInt())
                    val resized = Bitmap.createScaledBitmap(bmp, sw, sh, true)
                    if (resized != bmp) bmp.recycle()
                    bmp = resized
                }
                val w = bmp.width
                val h = bmp.height
                val px = IntArray(w * h)
                bmp.getPixels(px, 0, w, 0, 0, w, h)
                node.cachedTargetBitmap = bmp
                node.cachedTargetPixels = px
                node.cachedTargetW = w
                node.cachedTargetH = h
                node.cachedScaledTemplates = null
            }

            val baseBmp = node.cachedTargetBitmap ?: return false
            val basePx = node.cachedTargetPixels ?: return false
            val origW = node.cachedTargetW
            val origH = node.cachedTargetH
            val rangePct = node.imageScaleRangePct.coerceIn(5, 50)

            if (node.cachedScaledTemplates == null ||
                node.cachedAdaptiveScaleEnabled != node.imageAdaptiveScale ||
                node.cachedAdaptiveScaleRangePct != rangePct
            ) {
                val templates = mutableListOf<ScaledTemplate>()
                templates.add(ScaledTemplate(1.0f, origW, origH, basePx))

                if (node.imageAdaptiveScale) {
                    val seenSizes = HashSet<Long>()
                    seenSizes.add((origW.toLong() shl 32) or (origH.toLong() and 0xFFFFFFFFL))

                    val stepPct = 5
                    var deltaPct = stepPct
                    while (deltaPct <= rangePct) {
                        for (sign in intArrayOf(-1, 1)) {
                            val factor = 1.0f + (sign * deltaPct) / 100f
                            if (factor < 0.4f || factor > 2.0f) continue
                            val sw = Math.round(origW * factor)
                            val sh = Math.round(origH * factor)
                            if (sw < 4 || sh < 4 || sw > screenW || sh > screenH) continue
                            val key = (sw.toLong() shl 32) or (sh.toLong() and 0xFFFFFFFFL)
                            if (seenSizes.add(key)) {
                                val scaledBmp = Bitmap.createScaledBitmap(baseBmp, sw, sh, true)
                                val scaledPx = IntArray(sw * sh)
                                scaledBmp.getPixels(scaledPx, 0, sw, 0, 0, sw, sh)
                                if (scaledBmp != baseBmp && !scaledBmp.isRecycled) {
                                    scaledBmp.recycle()
                                }
                                templates.add(ScaledTemplate(factor, sw, sh, scaledPx))
                            }
                        }
                        deltaPct += stepPct
                    }
                }
                node.cachedScaledTemplates = templates
                node.cachedAdaptiveScaleEnabled = node.imageAdaptiveScale
                node.cachedAdaptiveScaleRangePct = rangePct
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private data class CandidateMatch(
        val centerX: Int,
        val centerY: Int,
        val pct: Float,
        val scaleFactor: Float,
        val w: Int,
        val h: Int
    )

    private fun searchTemplateInBitmap(node: TargetNode, bitmap: Bitmap): Pair<Boolean, Float> {
        if (!ensureTemplateCached(node, bitmap.width, bitmap.height)) return Pair(false, 0f)
        val origW = node.cachedTargetW
        val origH = node.cachedTargetH
        val templates = node.cachedScaledTemplates ?: return Pair(false, 0f)
        if (origW <= 0 || origH <= 0 || origW > bitmap.width || origH > bitmap.height) return Pair(false, 0f)

        val maxTemplateW = templates.maxOfOrNull { it.w } ?: origW
        val maxTemplateH = templates.maxOfOrNull { it.h } ?: origH

        val r = node.searchRadius.coerceAtLeast(0)
        val searchRectLeft: Int
        val searchRectTop: Int
        val searchRectRight: Int
        val searchRectBottom: Int

        if (node.imageFullScreenSearch) {
            searchRectLeft = 0
            searchRectTop = 0
            searchRectRight = bitmap.width
            searchRectBottom = bitmap.height
        } else {
            val startX = node.textZoneStartX
            val startY = node.textZoneStartY
            val endX = node.textZoneEndX
            val endY = node.textZoneEndY
            val hasZone = !(startX == 0 && startY == 0 && endX == 0 && endY == 0)

            var baseLeft: Int
            var baseTop: Int
            var baseRight: Int
            var baseBottom: Int

            if (hasZone) {
                baseLeft = minOf(startX, endX)
                baseTop = minOf(startY, endY)
                baseRight = maxOf(startX, endX)
                baseBottom = maxOf(startY, endY)
                if (baseRight - baseLeft < maxTemplateW) {
                    val cx = (baseLeft + baseRight) / 2
                    baseLeft = cx - maxTemplateW / 2
                    baseRight = baseLeft + maxTemplateW
                }
                if (baseBottom - baseTop < maxTemplateH) {
                    val cy = (baseTop + baseBottom) / 2
                    baseTop = cy - maxTemplateH / 2
                    baseBottom = baseTop + maxTemplateH
                }
            } else {
                baseLeft = node.x - maxTemplateW / 2
                baseTop = node.y - maxTemplateH / 2
                baseRight = baseLeft + maxTemplateW
                baseBottom = baseTop + maxTemplateH
            }

            // Provide a generous default local search window (at least 18px or half template size)
            // so small UI shifts or slightly off-center markers/frames never miss the target.
            val minMargin = maxOf(18, minOf(origW, origH) / 2)
            val effR = maxOf(r, minMargin)
            searchRectLeft = (baseLeft - effR).coerceIn(0, maxOf(0, bitmap.width - maxTemplateW))
            searchRectTop = (baseTop - effR).coerceIn(0, maxOf(0, bitmap.height - maxTemplateH))
            searchRectRight = (baseRight + effR).coerceIn(minOf(bitmap.width, searchRectLeft + maxTemplateW), bitmap.width)
            searchRectBottom = (baseBottom + effR).coerceIn(minOf(bitmap.height, searchRectTop + maxTemplateH), bitmap.height)
        }

        val sW = searchRectRight - searchRectLeft
        val sH = searchRectBottom - searchRectTop
        if (sW <= 0 || sH <= 0) return Pair(false, 0f)

        val pSearch = IntArray(sW * sH)
        bitmap.getPixels(pSearch, 0, sW, searchRectLeft, searchRectTop, sW, sH)

        val tol = node.colorTolerance
        val softTol = tol + 14
        val anchorTol = tol + 18
        val targetThreshold = node.imageThreshold.coerceIn(1f, 100f)
        val wantMulti = node.imageMultiTarget

        val allCandidates = mutableListOf<CandidateMatch>()
        var globalBestPct = 0f
        var bestFallbackCenter: Pair<Int, Int>? = null

        for (tpl in templates) {
            val tW = tpl.w
            val tH = tpl.h
            val pTarget = tpl.pixels
            if (tW > sW || tH > sH) continue

            val isSmallTemplate = (tW * tH <= 4096) || (minOf(tW, tH) <= 52)
            val baseStep = if (isSmallTemplate) 1 else (1f / node.checkResolutionScale.coerceIn(0.1f, 1.0f)).toInt().coerceAtLeast(1)
            val densityStep = if (isSmallTemplate) 1 else Math.sqrt((tW * tH) / 650.0).toInt().coerceAtLeast(1)
            val checkStep = maxOf(baseStep, densityStep)

            // Trim outer border (1px for >=6px, 2px for >=28px) so background bleed at template edges doesn't penalize matches
            val marginX = when {
                tW >= 28 -> 2
                tW >= 6 -> 1
                else -> 0
            }
            val marginY = when {
                tH >= 28 -> 2
                tH >= 6 -> 1
                else -> 0
            }
            val startTx = marginX
            val endTx = tW - marginX
            val startTy = marginY
            val endTy = tH - marginY

            var totalPixelsChecked = 0
            var tyCount = startTy
            while (tyCount < endTy) {
                var txCount = startTx
                while (txCount < endTx) {
                    totalPixelsChecked++
                    txCount += checkStep
                }
                tyCount += checkStep
            }
            if (totalPixelsChecked == 0) continue

            // 5 interior anchor points for fast pre-filtering on large search areas
            val ax0 = tW / 2
            val ay0 = tH / 2
            val ax1 = maxOf(startTx, tW / 3)
            val ay1 = maxOf(startTy, tH / 3)
            val ax2 = minOf(endTx - 1, (tW * 2) / 3)
            val ay2 = ay1
            val ax3 = ax1
            val ay3 = minOf(endTy - 1, (tH * 2) / 3)
            val ax4 = ax2
            val ay4 = ay3

            val anchorTx = intArrayOf(ax0, ax1, ax2, ax3, ax4)
            val anchorTy = intArrayOf(ay0, ay1, ay2, ay3, ay4)
            val anchorColors = IntArray(5) { idx -> pTarget[anchorTy[idx] * tW + anchorTx[idx]] }

            // Checks if an anchor point matches screen at (sx, sy) or its 1px neighbors (sub-pixel/1px shift resilience)
            fun anchorMatchesAt(sx: Int, sy: Int, targetColor: Int): Boolean {
                val tr = (targetColor shr 16) and 0xFF
                val tg = (targetColor shr 8) and 0xFF
                val tb = targetColor and 0xFF
                val baseIdx = sy * sW + sx
                val c0 = pSearch[baseIdx]
                if (Math.abs(((c0 shr 16) and 0xFF) - tr) <= anchorTol &&
                    Math.abs(((c0 shr 8) and 0xFF) - tg) <= anchorTol &&
                    Math.abs((c0 and 0xFF) - tb) <= anchorTol
                ) return true

                // Check 4-connected 1px neighbors so coarse step or sub-pixel anti-aliasing never skips a valid small icon
                if (sx > 0) {
                    val cL = pSearch[baseIdx - 1]
                    if (Math.abs(((cL shr 16) and 0xFF) - tr) <= anchorTol &&
                        Math.abs(((cL shr 8) and 0xFF) - tg) <= anchorTol &&
                        Math.abs((cL and 0xFF) - tb) <= anchorTol
                    ) return true
                }
                if (sx + 1 < sW) {
                    val cR = pSearch[baseIdx + 1]
                    if (Math.abs(((cR shr 16) and 0xFF) - tr) <= anchorTol &&
                        Math.abs(((cR shr 8) and 0xFF) - tg) <= anchorTol &&
                        Math.abs((cR and 0xFF) - tb) <= anchorTol
                    ) return true
                }
                if (sy > 0) {
                    val cU = pSearch[baseIdx - sW]
                    if (Math.abs(((cU shr 16) and 0xFF) - tr) <= anchorTol &&
                        Math.abs(((cU shr 8) and 0xFF) - tg) <= anchorTol &&
                        Math.abs((cU and 0xFF) - tb) <= anchorTol
                    ) return true
                }
                if (sy + 1 < sH) {
                    val cD = pSearch[baseIdx + sW]
                    if (Math.abs(((cD shr 16) and 0xFF) - tr) <= anchorTol &&
                        Math.abs(((cD shr 8) and 0xFF) - tg) <= anchorTol &&
                        Math.abs((cD and 0xFF) - tb) <= anchorTol
                    ) return true
                }
                return false
            }

            fun anchorsPass(dx: Int, dy: Int): Boolean {
                var matchedAnchors = 0
                for (i in 0 until 5) {
                    if (anchorMatchesAt(dx + anchorTx[i], dy + anchorTy[i], anchorColors[i])) {
                        matchedAnchors++
                        if (matchedAnchors >= 3) return true
                    } else if (i - matchedAnchors >= 2) {
                        // Already 3 failed anchors -> cannot reach 3/5
                        return false
                    }
                }
                return matchedAnchors >= 3
            }

            // Full template evaluation with sub-pixel edge resilience (1px neighbor fallback for anti-aliased edges)
            fun evaluateAt(dx: Int, dy: Int, requiredPct: Float): Float {
                val maxHalfMismatches = (((100f - requiredPct) / 100f) * totalPixelsChecked * 2f).toInt() + 2
                var halfMismatches = 0
                var ty = startTy
                while (ty < endTy) {
                    val tgtOffset = ty * tW
                    val sy = dy + ty
                    val srcOffset = sy * sW + dx
                    var tx = startTx
                    while (tx < endTx) {
                        val c2 = pTarget[tgtOffset + tx]
                        val tr = (c2 shr 16) and 0xFF
                        val tg = (c2 shr 8) and 0xFF
                        val tb = c2 and 0xFF

                        val sxIdx = srcOffset + tx
                        val c1 = pSearch[sxIdx]
                        val rDiff = Math.abs(((c1 shr 16) and 0xFF) - tr)
                        val gDiff = Math.abs(((c1 shr 8) and 0xFF) - tg)
                        val bDiff = Math.abs((c1 and 0xFF) - tb)

                        if (rDiff > tol || gDiff > tol || bDiff > tol) {
                            val avgDiff = (rDiff + gDiff + bDiff) / 3
                            if (avgDiff <= softTol) {
                                halfMismatches += 1
                            } else {
                                // Sub-pixel / anti-aliasing resilience: check if immediate 1px neighbor matches closely
                                val sx = dx + tx
                                var subPixelMatched = false
                                if (sx + 1 < sW) {
                                    val cn = pSearch[sxIdx + 1]
                                    if (Math.abs(((cn shr 16) and 0xFF) - tr) <= softTol &&
                                        Math.abs(((cn shr 8) and 0xFF) - tg) <= softTol &&
                                        Math.abs((cn and 0xFF) - tb) <= softTol
                                    ) subPixelMatched = true
                                }
                                if (!subPixelMatched && sx > 0) {
                                    val cn = pSearch[sxIdx - 1]
                                    if (Math.abs(((cn shr 16) and 0xFF) - tr) <= softTol &&
                                        Math.abs(((cn shr 8) and 0xFF) - tg) <= softTol &&
                                        Math.abs((cn and 0xFF) - tb) <= softTol
                                    ) subPixelMatched = true
                                }
                                if (!subPixelMatched && sy + 1 < sH) {
                                    val cn = pSearch[sxIdx + sW]
                                    if (Math.abs(((cn shr 16) and 0xFF) - tr) <= softTol &&
                                        Math.abs(((cn shr 8) and 0xFF) - tg) <= softTol &&
                                        Math.abs((cn and 0xFF) - tb) <= softTol
                                    ) subPixelMatched = true
                                }
                                if (!subPixelMatched && sy > 0) {
                                    val cn = pSearch[sxIdx - sW]
                                    if (Math.abs(((cn shr 16) and 0xFF) - tr) <= softTol &&
                                        Math.abs(((cn shr 8) and 0xFF) - tg) <= softTol &&
                                        Math.abs((cn and 0xFF) - tb) <= softTol
                                    ) subPixelMatched = true
                                }

                                halfMismatches += if (subPixelMatched) 1 else 2
                            }
                            if (halfMismatches > maxHalfMismatches) return 0f
                        }
                        tx += checkStep
                    }
                    ty += checkStep
                }
                val matchedEquivalent = totalPixelsChecked.toFloat() - (halfMismatches / 2f)
                return ((matchedEquivalent / totalPixelsChecked) * 100f).coerceIn(0f, 100f)
            }

            val maxX = sW - tW
            val maxY = sH - tH
            val totalPositions = (maxX + 1).toLong() * (maxY + 1).toLong()

            // Local peak refinement around a hit (dx, dy) within ±radius
            fun refinePeak(centerDx: Int, centerDy: Int, radius: Int): Triple<Int, Int, Float> {
                var bestDx = centerDx
                var bestDy = centerDy
                var bestLocalPct = evaluateAt(centerDx, centerDy, 0f)
                val minFy = maxOf(0, centerDy - radius)
                val maxFy = minOf(maxY, centerDy + radius)
                val minFx = maxOf(0, centerDx - radius)
                val maxFx = minOf(maxX, centerDx + radius)
                for (fdy in minFy..maxFy) {
                    for (fdx in minFx..maxFx) {
                        if (fdx == centerDx && fdy == centerDy) continue
                        val p = evaluateAt(fdx, fdy, maxOf(bestLocalPct, targetThreshold - 5f))
                        if (p > bestLocalPct) {
                            bestLocalPct = p
                            bestDx = fdx
                            bestDy = fdy
                        }
                    }
                }
                return Triple(bestDx, bestDy, bestLocalPct)
            }

            if (totalPositions <= 3600L) {
                var dy = 0
                while (dy <= maxY) {
                    var dx = 0
                    while (dx <= maxX) {
                        val pct = evaluateAt(dx, dy, if (wantMulti) targetThreshold else maxOf(0f, targetThreshold - 15f))
                        if (pct > globalBestPct) {
                            globalBestPct = pct
                            bestFallbackCenter = Pair(searchRectLeft + dx + tW / 2, searchRectTop + dy + tH / 2)
                        }
                        if (pct >= targetThreshold) {
                            val (rDx, rDy, rPct) = refinePeak(dx, dy, 2)
                            if (rPct > globalBestPct) globalBestPct = rPct
                            val cx = searchRectLeft + rDx + tW / 2
                            val cy = searchRectTop + rDy + tH / 2
                            if (!wantMulti) {
                                node.lastFoundCenterX = cx
                                node.lastFoundCenterY = cy
                                node.lastFoundCenters = listOf(Pair(cx, cy))
                                return Pair(true, rPct)
                            } else {
                                allCandidates.add(CandidateMatch(cx, cy, rPct, tpl.scaleFactor, tW, tH))
                                // Skip ahead a few pixels horizontally inside the same object to avoid redundant dense evaluations
                                dx += maxOf(1, tW / 3)
                                continue
                            }
                        }
                        dx++
                    }
                    dy++
                }
            } else {
                // Fast Anchor-Guided Coarse-to-Fine search for full screen / large zones
                val scanStep = if (isSmallTemplate) 2 else maxOf(2, minOf(tW, tH) / 4).coerceIn(2, 4)
                val coarseVerifyThreshold = maxOf(35f, targetThreshold - 22f)
                var bestCoarseX = 0
                var bestCoarseY = 0
                var bestScalePct = 0f

                var dy = 0
                while (dy <= maxY) {
                    var dx = 0
                    while (dx <= maxX) {
                        if (anchorsPass(dx, dy)) {
                            val coarsePct = evaluateAt(dx, dy, coarseVerifyThreshold)
                            if (coarsePct > bestScalePct) {
                                bestScalePct = coarsePct
                                bestCoarseX = dx
                                bestCoarseY = dy
                            }
                            if (coarsePct > globalBestPct) {
                                globalBestPct = coarsePct
                                bestFallbackCenter = Pair(searchRectLeft + dx + tW / 2, searchRectTop + dy + tH / 2)
                            }
                            if (coarsePct >= coarseVerifyThreshold) {
                                val (rDx, rDy, rPct) = refinePeak(dx, dy, scanStep)
                                if (rPct > bestScalePct) {
                                    bestScalePct = rPct
                                    bestCoarseX = rDx
                                    bestCoarseY = rDy
                                }
                                if (rPct > globalBestPct) {
                                    globalBestPct = rPct
                                    bestFallbackCenter = Pair(searchRectLeft + rDx + tW / 2, searchRectTop + rDy + tH / 2)
                                }
                                if (rPct >= targetThreshold) {
                                    val cx = searchRectLeft + rDx + tW / 2
                                    val cy = searchRectTop + rDy + tH / 2
                                    if (!wantMulti) {
                                        node.lastFoundCenterX = cx
                                        node.lastFoundCenterY = cy
                                        node.lastFoundCenters = listOf(Pair(cx, cy))
                                        return Pair(true, rPct)
                                    } else {
                                        allCandidates.add(CandidateMatch(cx, cy, rPct, tpl.scaleFactor, tW, tH))
                                        dx += maxOf(scanStep, tW / 3)
                                        continue
                                    }
                                }
                            }
                        }
                        dx += scanStep
                    }
                    dy += scanStep
                }

                // Final fine check around best coarse position for accurate diagnostic % even if below threshold
                if (bestScalePct > 0f) {
                    val (rDx, rDy, rPct) = refinePeak(bestCoarseX, bestCoarseY, scanStep)
                    if (rPct > globalBestPct) {
                        globalBestPct = rPct
                        bestFallbackCenter = Pair(searchRectLeft + rDx + tW / 2, searchRectTop + rDy + tH / 2)
                    }
                    if (!wantMulti && rPct >= targetThreshold) {
                        val cx = searchRectLeft + rDx + tW / 2
                        val cy = searchRectTop + rDy + tH / 2
                        node.lastFoundCenterX = cx
                        node.lastFoundCenterY = cy
                        node.lastFoundCenters = listOf(Pair(cx, cy))
                        return Pair(true, rPct)
                    }
                }
            }
        }

        if (wantMulti && allCandidates.isNotEmpty()) {
            // Non-Maximum Suppression (NMS) so overlapping detections on the same sprite are merged into the strongest peak
            val sortedByScore = allCandidates.sortedByDescending { it.pct }
            val suppressed = mutableListOf<CandidateMatch>()
            for (cand in sortedByScore) {
                val minDist = maxOf(8f, minOf(cand.w, cand.h) * 0.65f)
                val minDistSq = minDist * minDist
                val overlapsExisting = suppressed.any { kept ->
                    val ddx = (cand.centerX - kept.centerX).toFloat()
                    val ddy = (cand.centerY - kept.centerY).toFloat()
                    (ddx * ddx + ddy * ddy) < minDistSq
                }
                if (!overlapsExisting) {
                    suppressed.add(cand)
                }
            }

            // Order targets: either random (anti-detect) or natural reading order (top-to-bottom, left-to-right)
            val rowBand = maxOf(10, origH / 2)
            var ordered = if (node.imageMultiRandomOrder) {
                suppressed.shuffled()
            } else {
                suppressed.sortedWith(compareBy({ it.centerY / rowBand }, { it.centerX }))
            }

            if (node.imageMultiMaxTargets > 0 && ordered.size > node.imageMultiMaxTargets) {
                ordered = ordered.take(node.imageMultiMaxTargets)
            }

            node.lastFoundCenters = ordered.map { Pair(it.centerX, it.centerY) }
            node.lastFoundCenterX = ordered.first().centerX
            node.lastFoundCenterY = ordered.first().centerY
            val bestMultiPct = suppressed.maxOf { it.pct }
            return Pair(true, bestMultiPct)
        }

        node.lastFoundCenters = emptyList()
        if (bestFallbackCenter != null && globalBestPct >= targetThreshold) {
            node.lastFoundCenterX = bestFallbackCenter!!.first
            node.lastFoundCenterY = bestFallbackCenter!!.second
            node.lastFoundCenters = listOf(bestFallbackCenter!!)
            return Pair(true, globalBestPct)
        }

        return Pair(false, globalBestPct)
    }

    private fun compareTwoRegionsInBitmap(node: TargetNode, bitmap: Bitmap): Pair<Boolean, Float> {
        val l1 = minOf(node.textZoneStartX, node.textZoneEndX).coerceIn(0, bitmap.width - 1)
        val t1 = minOf(node.textZoneStartY, node.textZoneEndY).coerceIn(0, bitmap.height - 1)
        val r1 = maxOf(node.textZoneStartX, node.textZoneEndX).coerceIn(0, bitmap.width)
        val b1 = maxOf(node.textZoneStartY, node.textZoneEndY).coerceIn(0, bitmap.height)
        val w1 = (r1 - l1).coerceAtLeast(1)
        val h1 = (b1 - t1).coerceAtLeast(1)

        val l2 = minOf(node.compareZoneStartX, node.compareZoneEndX).coerceIn(0, bitmap.width - 1)
        val t2 = minOf(node.compareZoneStartY, node.compareZoneEndY).coerceIn(0, bitmap.height - 1)
        val r2 = maxOf(node.compareZoneStartX, node.compareZoneEndX).coerceIn(0, bitmap.width)
        val b2 = maxOf(node.compareZoneStartY, node.compareZoneEndY).coerceIn(0, bitmap.height)
        val w2 = (r2 - l2).coerceAtLeast(1)
        val h2 = (b2 - t2).coerceAtLeast(1)

        if (w1 < 2 || h1 < 2 || w2 < 2 || h2 < 2) return Pair(false, 0f)

        val bmp1 = Bitmap.createBitmap(bitmap, l1, t1, w1, h1)
        val bmp2Raw = Bitmap.createBitmap(bitmap, l2, t2, w2, h2)
        val bmp2 = if (w1 != w2 || h1 != h2) {
            Bitmap.createScaledBitmap(bmp2Raw, w1, h1, true)
        } else {
            bmp2Raw
        }

        return try {
            val px1 = IntArray(w1 * h1)
            val px2 = IntArray(w1 * h1)
            bmp1.getPixels(px1, 0, w1, 0, 0, w1, h1)
            bmp2.getPixels(px2, 0, w1, 0, 0, w1, h1)

            val step = maxOf(1, Math.sqrt((w1 * h1) / 500.0).toInt())
            val tol = node.colorTolerance
            var total = 0
            var matched = 0
            var y = 0
            while (y < h1) {
                var x = 0
                val rowOff = y * w1
                while (x < w1) {
                    val c1 = px1[rowOff + x]
                    val c2 = px2[rowOff + x]
                    val rDiff = Math.abs(((c1 shr 16) and 0xFF) - ((c2 shr 16) and 0xFF))
                    val gDiff = Math.abs(((c1 shr 8) and 0xFF) - ((c2 shr 8) and 0xFF))
                    val bDiff = Math.abs((c1 and 0xFF) - (c2 and 0xFF))
                    if (rDiff <= tol && gDiff <= tol && bDiff <= tol) {
                        matched++
                    }
                    total++
                    x += step
                }
                y += step
            }
            val pct = if (total > 0) (matched.toFloat() / total) * 100f else 0f
            val targetThreshold = node.imageThreshold.coerceIn(1f, 100f)
            node.lastFoundCenterX = l2 + w2 / 2
            node.lastFoundCenterY = t2 + h2 / 2
            Pair(pct >= targetThreshold, pct)
        } finally {
            if (bmp2 != bmp2Raw && !bmp2.isRecycled) bmp2.recycle()
            if (!bmp2Raw.isRecycled) bmp2Raw.recycle()
            if (!bmp1.isRecycled) bmp1.recycle()
        }
    }

    fun testImageFragmentSearch(node: TargetNode) {
        if (!node.compareZoneEnabled && node.targetImageBase64 == null) {
            handler.post { Toast.makeText(this, "Сначала захватите шаблон или включите сравнение двух областей", Toast.LENGTH_SHORT).show() }
            return
        }
        requestScreenshot { bitmap ->
            if (bitmap == null) {
                handler.post { Toast.makeText(this, "Не удалось получить снимок экрана", Toast.LENGTH_SHORT).show() }
                return@requestScreenshot
            }
            val t0 = System.currentTimeMillis()
            val (found, bestPct) = if (node.compareZoneEnabled) {
                compareTwoRegionsInBitmap(node, bitmap)
            } else {
                searchTemplateInBitmap(node, bitmap)
            }
            val dt = System.currentTimeMillis() - t0
            val finalMatch = if (node.colorOperator == "!=") !found else found
            val msg = if (node.compareZoneEnabled) {
                "Сходство Области 1 и Области 2: ${String.format("%.1f", bestPct)}% (порог ${node.imageThreshold}%, ${dt}мс). Условие (${node.colorOperator}): $finalMatch"
            } else if (found) {
                val multiCount = if (node.imageMultiTarget && node.lastFoundCenters.isNotEmpty()) " [Найдено целей: ${node.lastFoundCenters.size}]" else ""
                "Фрагмент НАЙДЕН (${String.format("%.1f", bestPct)}%)$multiCount в (${node.lastFoundCenterX}, ${node.lastFoundCenterY}) за ${dt}мс. Итог: $finalMatch"
            } else {
                "Фрагмент НЕ найден (макс. ${String.format("%.1f", bestPct)}%, порог ${node.imageThreshold}%, время ${dt}мс). Итог: $finalMatch"
            }
            handler.post {
                if (::uiManager.isInitialized) uiManager.logDebug("Шаг ${node.id}: [Тест Фрагмента] $msg")
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkNodeCondition(node: TargetNode, bitmap: Bitmap): Boolean {
        if (node.triggerMode == 1) {
            if (node.compareZoneEnabled) {
                try {
                    val (found, _) = compareTwoRegionsInBitmap(node, bitmap)
                    return if (node.colorOperator == "!=") !found else found
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (node.targetImageBase64 != null) {
                try {
                    val (found, _) = searchTemplateInBitmap(node, bitmap)
                    return if (node.colorOperator == "!=") !found else found
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
                val now = System.currentTimeMillis()
                val lookback = node.dynamicColorLookbackMs

                if (lookback > 0L) {
                    if (node.colorHistory.isEmpty()) {
                        node.colorHistory.addLast(Pair(now, color))
                        node.targetColor = color
                        return false
                    }
                    // Keep history entries within lookback window, retaining the entry closest to (now - lookback) at the front
                    while (node.colorHistory.size >= 2) {
                        val first = node.colorHistory.removeFirst()
                        val second = node.colorHistory.peekFirst()
                        if (second != null && now - second.first >= lookback) {
                            // second is also old enough to be the reference from lookbackMs ago
                            continue
                        } else {
                            // keep first as the reference from >= lookbackMs ago
                            node.colorHistory.addFirst(first)
                            break
                        }
                    }
                    val lastAdded = node.colorHistory.peekLast()
                    if (lastAdded == null || now - lastAdded.first >= 15L) {
                        node.colorHistory.addLast(Pair(now, color))
                    }

                    val refEntry = node.colorHistory.peekFirst()!!
                    val c1 = refEntry.second
                    node.targetColor = c1

                    val rDiff = Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(color))
                    val gDiff = Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(color))
                    val bDiff = Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(color))
                    val isSame = rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance

                    val result = if (node.colorOperator == "!=") {
                        !isSame
                    } else {
                        // For "==", require that the color remained stable for the full lookback window
                        isSame && (now - refEntry.first >= lookback)
                    }

                    if (result) {
                        // Reset baseline to the new color after triggering so it tracks the next change
                        node.colorHistory.clear()
                        node.colorHistory.addLast(Pair(now, color))
                        node.targetColor = color
                    }
                    return result
                } else {
                    if (node.targetColor == null) {
                        node.targetColor = color // save initial state
                        return false // wait for next cycle to compare
                    }
                    val c1 = node.targetColor!!
                    val rDiff = Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(color))
                    val gDiff = Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(color))
                    val bDiff = Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(color))
                    val isSame = rDiff <= node.colorTolerance && gDiff <= node.colorTolerance && bDiff <= node.colorTolerance

                    val result = if (node.colorOperator == "!=") !isSame else isSame
                    node.targetColor = color // always memorize current color for next cycle comparison
                    return result
                }
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
                private val locCache = IntArray(2)
                override fun onDraw(canvas: android.graphics.Canvas) {
                    super.onDraw(canvas)
                    if (isDragging && swipePoints.isNotEmpty()) {
                        getLocationOnScreen(locCache)
                        val firstX = swipePoints[0].first - locCache[0]
                        val firstY = swipePoints[0].second - locCache[1]
                        val path = buildSmoothSwipePath(firstX, firstY, swipePoints)
                        canvas.drawPath(path, paint)
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
                        swipePoints.add(Pair(event.rawX, event.rawY))
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
                        if (last == null || Math.hypot((event.rawX - last.first).toDouble(), (event.rawY - last.second).toDouble()) > 8) {
                            swipePoints.add(Pair(event.rawX, event.rawY))
                        }
                        drawingView.invalidate()
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        isDragging = false
                        swipePoints.add(Pair(event.rawX, event.rawY))
                        drawingView.invalidate()
                        val duration = System.currentTimeMillis() - recordDownTime
                        val startX = recordDownX
                        val startY = recordDownY
                        val upX = event.rawX
                        val upY = event.rawY
                        
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            val dx = upX - startX
                            val dy = upY - startY
                            val isSwipe = Math.hypot(dx.toDouble(), dy.toDouble()) > 10 || swipePoints.size > 4
                            
                            val path = if (isSwipe && swipePoints.size >= 2) {
                                buildSmoothSwipePath(startX, startY, swipePoints)
                            } else {
                                android.graphics.Path().apply {
                                    moveTo(startX, startY)
                                    if (isSwipe) lineTo(upX, upY) else lineTo(startX + 1f, startY + 1f)
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
                                uiManager.logDebug("[Запись] Создана метка ${node.id} (${if (isSwipe) "Свайп (${swipePoints.size} тчк)" else "Клик"})")
                                uiManager.invalidateLines()
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

    fun startSingleSwipeRecording(node: TargetNode, onFinished: () -> Unit) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val recordedPts = mutableListOf<Pair<Float, Float>>()
        var downTimeMs = 0L

        val container = android.widget.FrameLayout(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        val canvasView = object : View(this) {
            private val pathPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#00E5FF")
                strokeWidth = 8f
                style = android.graphics.Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
            }
            private val dotPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#FFEB3B")
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                canvas.drawColor(android.graphics.Color.parseColor("#33000000"))
                if (recordedPts.isNotEmpty()) {
                    val loc = IntArray(2)
                    getLocationOnScreen(loc)
                    val firstX = recordedPts.first().first - loc[0]
                    val firstY = recordedPts.first().second - loc[1]
                    if (recordedPts.size > 1) {
                        val path = buildSmoothSwipePath(firstX, firstY, recordedPts)
                        canvas.drawPath(path, pathPaint)
                    }
                    canvas.drawCircle(firstX, firstY, 12f, dotPaint)
                    val last = recordedPts.last()
                    canvas.drawCircle(last.first - loc[0], last.second - loc[1], 10f, dotPaint)
                }
            }
        }

        val banner = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(36, 20, 24, 20)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EE1A1A2E"))
                cornerRadius = 40f
                setStroke(3, android.graphics.Color.parseColor("#00E5FF"))
            }
        }
        val titleTv = android.widget.TextView(this).apply {
            text = "✍️ Нарисуйте свайп для Метки #${node.id}"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 24, 0)
        }
        val cancelBtn = android.widget.Button(this).apply {
            text = "✕ Отмена"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 12f
            setPadding(24, 8, 24, 8)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#C62828"))
                cornerRadius = 24f
            }
            setOnClickListener {
                try { windowManager.removeView(container) } catch (_: Exception) {}
                onFinished()
            }
        }
        banner.addView(titleTv)
        banner.addView(cancelBtn)

        val bannerParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            topMargin = 90
        }

        canvasView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    recordedPts.clear()
                    recordedPts.add(Pair(event.rawX, event.rawY))
                    downTimeMs = System.currentTimeMillis()
                    v.invalidate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val last = recordedPts.lastOrNull()
                    if (last == null || Math.hypot((event.rawX - last.first).toDouble(), (event.rawY - last.second).toDouble()) >= 6.0) {
                        recordedPts.add(Pair(event.rawX, event.rawY))
                        v.invalidate()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val upX = event.rawX
                    val upY = event.rawY
                    val last = recordedPts.lastOrNull()
                    if (last == null || last.first != upX || last.second != upY) {
                        recordedPts.add(Pair(upX, upY))
                    }
                    val durationMs = (System.currentTimeMillis() - downTimeMs).coerceIn(50L, 10000L)
                    if (recordedPts.size >= 2) {
                        node.isSwipe = true
                        node.isHold = false
                        node.swipeTargetNodeId = null
                        node.x = recordedPts.first().first.toInt()
                        node.y = recordedPts.first().second.toInt()
                        node.swipeEndX = recordedPts.last().first.toInt()
                        node.swipeEndY = recordedPts.last().second.toInt()
                        node.swipePathPoints = recordedPts.toList()
                        node.swipeDurationMs = durationMs
                        if (::uiManager.isInitialized) {
                            uiManager.createSwipeEndMarker(node)
                            uiManager.updateNodeViewPosition(node)
                            uiManager.invalidateLines()
                            uiManager.logDebug("Шаг ${node.id}: записана траектория (${recordedPts.size} точек, ${durationMs}мс)")
                        }
                    }
                    try { windowManager.removeView(container) } catch (_: Exception) {}
                    onFinished()
                    true
                }
                else -> false
            }
        }

        container.addView(canvasView, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        container.addView(banner, bannerParams)
        windowManager.addView(container, params)
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
            getSavedProfiles().filter { it != "Автосохранение" && !it.startsWith("AutoSave /") }.take(5).forEach { list.add(Pair(it, it)) }
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
        return prefs.all.keys
            .filter { !it.startsWith("AutoSave /") }
            .sortedWith(compareBy<String> { it != "Автосохранение" }.thenBy { it })
    }

    fun deleteProfile(name: String) {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove(name).apply()
        android.widget.Toast.makeText(this, "Сценарий '$name' удален", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun getRealScreenDimensions(): Pair<Int, Int> {
        val wm = getSystemService(android.content.Context.WINDOW_SERVICE) as? android.view.WindowManager
        if (wm != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val bounds = wm.maximumWindowMetrics.bounds
                return Pair(bounds.width(), bounds.height())
            } else {
                val dm = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealMetrics(dm)
                return Pair(dm.widthPixels, dm.heightPixels)
            }
        }
        val dm = resources.displayMetrics
        return Pair(dm.widthPixels, dm.heightPixels)
    }

    fun autoSave() {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        if (nodes.isEmpty()) {
            prefs.edit().remove("Автосохранение").apply()
            return
        }
        val obj = org.json.JSONObject()
        val (screenWidth, screenHeight) = getRealScreenDimensions()
        obj.put("screenWidth", screenWidth)
        obj.put("screenHeight", screenHeight)
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
        val (screenWidth, screenHeight) = getRealScreenDimensions()
        obj.put("screenWidth", screenWidth)
        obj.put("screenHeight", screenHeight)
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
        val (screenWidth, screenHeight) = getRealScreenDimensions()
        obj.put("screenWidth", screenWidth)
        obj.put("screenHeight", screenHeight)
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

    fun exportAllProfilesJson(): String {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val root = org.json.JSONObject()
        root.put("backupType", "UpwellClickFullBackup")
        root.put("version", 1)
        val profilesObj = org.json.JSONObject()
        for ((k, v) in prefs.all) {
            if (v is String && !k.startsWith("AutoSave /")) {
                profilesObj.put(k, v)
            }
        }
        root.put("profiles", profilesObj)
        return root.toString()
    }

    fun loadProfileFromJson(jsonText: String, append: Boolean = false) {
        var cleanText = jsonText.trim()
        if (cleanText.startsWith("```")) {
            cleanText = cleanText.substringAfter("\n")
            if (cleanText.contains("```")) {
                cleanText = cleanText.substringBeforeLast("```")
            }
            cleanText = cleanText.trim()
        }
        val firstBrace = cleanText.indexOf('{')
        val firstBracket = cleanText.indexOf('[')
        val startIndex = when {
            firstBrace >= 0 && firstBracket >= 0 -> minOf(firstBrace, firstBracket)
            firstBrace >= 0 -> firstBrace
            firstBracket >= 0 -> firstBracket
            else -> 0
        }
        if (startIndex > 0) {
            cleanText = cleanText.substring(startIndex)
        }
        val lastBrace = cleanText.lastIndexOf('}')
        val lastBracket = cleanText.lastIndexOf(']')
        val endIndex = when {
            lastBrace >= 0 && lastBracket >= 0 -> maxOf(lastBrace, lastBracket)
            lastBrace >= 0 -> lastBrace
            lastBracket >= 0 -> lastBracket
            else -> cleanText.length - 1
        }
        if (endIndex in 0 until cleanText.length - 1) {
            cleanText = cleanText.substring(0, endIndex + 1)
        }

        val tokener = org.json.JSONTokener(cleanText)
        val root = tokener.nextValue()

        val isBackup = root is org.json.JSONObject && (
            root.has("backupType") ||
            root.has("profiles") ||
            root.optString("backupType").isNotEmpty()
        )

        if (isBackup && root is org.json.JSONObject) {
            val profilesObj = root.optJSONObject("profiles")
            if (profilesObj != null) {
                val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
                val edit = prefs.edit()
                var count = 0
                var firstProfileJson: String? = null
                var firstProfileName: String? = null
                val keys = profilesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val rawVal = profilesObj.opt(k)
                    val v = when (rawVal) {
                        is org.json.JSONObject -> rawVal.toString()
                        is String -> rawVal
                        else -> rawVal?.toString() ?: ""
                    }
                    if (v.isNotEmpty()) {
                        edit.putString(k, v)
                        if (firstProfileJson == null) {
                            firstProfileJson = v
                            firstProfileName = k
                        }
                        count++
                    }
                }
                edit.apply()
                android.widget.Toast.makeText(this, "Восстановлено профилей из бэкапа: $count", android.widget.Toast.LENGTH_SHORT).show()
                if (firstProfileJson != null) {
                    try {
                        loadProfileFromJson(firstProfileJson, append = append)
                        if (firstProfileName != null) {
                            android.widget.Toast.makeText(this, "Загружен: $firstProfileName", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return
        }
        
        var arr: org.json.JSONArray
        var scaleX = 1f
        var scaleY = 1f
        
        if (root is org.json.JSONObject) {
            val targetObj = if (root.has("profile") && root.optJSONObject("profile") != null) {
                root.getJSONObject("profile")
            } else {
                root
            }
            arr = when {
                targetObj.has("nodes") -> targetObj.getJSONArray("nodes")
                targetObj.has("points") -> targetObj.getJSONArray("points")
                else -> throw Exception("JSON не содержит списка точек ('nodes')")
            }
            if (targetObj.has("screenWidth") && targetObj.has("screenHeight")) {
                val origWidth = targetObj.getInt("screenWidth")
                val origHeight = targetObj.getInt("screenHeight")
                val (curWidth, curHeight) = getRealScreenDimensions()
                if (origWidth > 0 && curWidth > 0 && origWidth != curWidth) {
                    scaleX = curWidth.toFloat() / origWidth
                }
                if (origHeight > 0 && curHeight > 0 && origHeight != curHeight) {
                    // If height difference is just due to navigation bar / system inset difference on same device, avoid squishing
                    if (Math.abs(curHeight - origHeight) > 250 || curWidth != origWidth) {
                        scaleY = curHeight.toFloat() / origHeight
                    }
                }
            }
        } else if (root is org.json.JSONArray) {
            arr = root
        } else {
            throw Exception("Invalid JSON root type")
        }

        if (!append) {
            isPlaying = false
            nodes.clear()
            uiManager.nodeViews.values.forEach { try { uiManager.windowManager.removeView(it) } catch (_: Exception) {} }
            uiManager.nodeViews.clear()
            uiManager.nodeParams.clear()
            uiManager.swipeEndViews.values.forEach { try { uiManager.windowManager.removeView(it) } catch (_: Exception) {} }
            uiManager.swipeEndViews.clear()
            uiManager.swipeEndParams.clear()
            uiManager.textZoneStartViews.values.forEach { try { uiManager.windowManager.removeView(it) } catch (_: Exception) {} }
            uiManager.textZoneStartViews.clear()
            uiManager.textZoneStartParams.clear()
            uiManager.textZoneEndViews.values.forEach { try { uiManager.windowManager.removeView(it) } catch (_: Exception) {} }
            uiManager.textZoneEndViews.clear()
            uiManager.textZoneEndParams.clear()
            uiManager.colorCompareViews.values.forEach { try { uiManager.windowManager.removeView(it) } catch (_: Exception) {} }
            uiManager.colorCompareViews.clear()
            uiManager.colorCompareParams.clear()
        }
        
        var maxId = if (nodes.isNotEmpty()) nodes.maxOf { it.id } else 0
        val idOffset = if (append) maxId else 0
        
        val newNodes = mutableListOf<TargetNode>()
        for (i in 0 until arr.length()) {
            val node = TargetNode.fromJson(arr.getJSONObject(i))
            
            if (scaleX != 1f || scaleY != 1f) {
                node.x = (node.x * scaleX).toInt()
                node.y = (node.y * scaleY).toInt()
                node.swipeEndX = (node.swipeEndX * scaleX).toInt()
                node.swipeEndY = (node.swipeEndY * scaleY).toInt()
                if (node.colorCompareX != null) node.colorCompareX = (node.colorCompareX!! * scaleX).toInt()
                if (node.colorCompareY != null) node.colorCompareY = (node.colorCompareY!! * scaleY).toInt()
                node.textZoneStartX = (node.textZoneStartX * scaleX).toInt()
                node.textZoneStartY = (node.textZoneStartY * scaleY).toInt()
                node.textZoneEndX = (node.textZoneEndX * scaleX).toInt()
                node.textZoneEndY = (node.textZoneEndY * scaleY).toInt()
                node.compareZoneStartX = (node.compareZoneStartX * scaleX).toInt()
                node.compareZoneStartY = (node.compareZoneStartY * scaleY).toInt()
                node.compareZoneEndX = (node.compareZoneEndX * scaleX).toInt()
                node.compareZoneEndY = (node.compareZoneEndY * scaleY).toInt()
                
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
                if (node.swipeTargetNodeId != null && node.swipeTargetNodeId!! > 0) node.swipeTargetNodeId = node.swipeTargetNodeId!! + idOffset
                if (node.nextNodeIdOnSuccess != null && node.nextNodeIdOnSuccess!! > 0) node.nextNodeIdOnSuccess = node.nextNodeIdOnSuccess!! + idOffset
                if (node.nextNodeIdOnFail != null && node.nextNodeIdOnFail!! > 0) node.nextNodeIdOnFail = node.nextNodeIdOnFail!! + idOffset
                if (node.linkedConditionNodeId != null && node.linkedConditionNodeId!! > 0) node.linkedConditionNodeId = node.linkedConditionNodeId!! + idOffset
                if (node.compareToNodeId != null && node.compareToNodeId!! > 0) node.compareToNodeId = node.compareToNodeId!! + idOffset
                if (node.ocrCompareToNodeId != null && node.ocrCompareToNodeId!! > 0) node.ocrCompareToNodeId = node.ocrCompareToNodeId!! + idOffset
                if (node.syncWithNodeIds.isNotEmpty()) {
                    node.syncWithNodeIds = node.syncWithNodeIds.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .map { if (it > 0) it + idOffset else it }
                        .joinToString(",")
                }
                if (node.managerRoutes.isNotEmpty()) {
                    node.managerRoutes = node.managerRoutes.map { r ->
                        ManagerRoute(
                            checkNodeId = if (r.checkNodeId > 0) r.checkNodeId + idOffset else r.checkNodeId,
                            onSuccessGoToId = if (r.onSuccessGoToId > 0) r.onSuccessGoToId + idOffset else r.onSuccessGoToId
                        )
                    }
                }
            }
            newNodes.add(node)
            if (node.id > maxId) maxId = node.id
        }
        
        nodes.addAll(newNodes)
        uiManager.nodeCounter = maxId + 1
        
        if (append) {
            uiManager.createViewsForNodes(newNodes)
            autoSave()
        } else {
            uiManager.recreateAllNodeViews()
        }
        if (::uiManager.isInitialized) uiManager.invalidateLines()
    }

    fun loadProfile(name: String, append: Boolean = false) {
        val prefs = getSharedPreferences("AutoClickerProfiles", android.content.Context.MODE_PRIVATE)
        val jsonString = prefs.getString(name, null) ?: return
        try {
            if (!append && name != "Автосохранение" && nodes.isNotEmpty()) {
                autoSave()
            }
            loadProfileFromJson(jsonString, append)
            android.widget.Toast.makeText(this, "Сценарий '$name' ${if(append) "добавлен" else "загружен"}", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Ошибка загрузки", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun performGlobalClick(x: Float, y: Float, duration: Long) {
        val path = android.graphics.Path().apply {
            moveTo(x, y)
            lineTo(x + 1f, y + 1f)
        }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build()
        gestureQueue.offer(QueuedGesture(gesture, duration, false))
        processGestureQueue()
    }
}
