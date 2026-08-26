package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class AutoClickService : AccessibilityService() {

    companion object {
        var instance: AutoClickService? = null
    }

    lateinit var uiManager: UIManager
    lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val imageHelper = ImageHelper()

    val nodes = CopyOnWriteArrayList<TargetNode>()
    private val profiles = ArrayList<ScriptProfile>()

    var currentState = ExecutionState.STOPPED
    var allowExtremeSpeed = true
    var enableMultitouch = false
    var targetLoopCount = 0
    var targetTimeLimitSec = 0L
    private var scriptStartTime = 0L
    private var isNodesVisible = true

    private var currentNodeIndex = 0
    private var loopCounter = 0
    private var isSubScriptRunning = false
    private var subScriptNodes: List<TargetNode>? = null
    private var subScriptIndex = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        uiManager = UIManager(this, windowManager)

        loadSavedProfiles()

        val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE)
        val wasExplicitlyClosed = prefs.getBoolean("was_explicitly_closed", false)
        if (!wasExplicitlyClosed) {
            uiManager.showControlBar()
        }
    }

    fun toggleNodesVisibility() {
        isNodesVisible = !isNodesVisible
        uiManager.setNodesVisible(isNodesVisible)
        Toast.makeText(this, if (isNodesVisible) "Метки показаны" else "Метки скрыты", Toast.LENGTH_SHORT).show()
    }

    fun clearAllNodes() {
        nodes.clear()
        uiManager.clearAllNodes()
    }

    fun deleteProfile(profileId: String) {
        profiles.removeAll { it.id == profileId }
        saveProfilesToPrefs()
    }

    fun toggleServiceUI(forceOpen: Boolean = false) {
        val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE).edit()
        prefs.putBoolean("was_explicitly_closed", false)
        prefs.apply()
        uiManager.showControlBar()
    }

    fun closeServiceUI() {
        val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE).edit()
        prefs.putBoolean("was_explicitly_closed", true)
        prefs.apply()

        stopExecution()
        uiManager.clearAllNodes()
        uiManager.hideControlBar()
    }

    fun addClickNode() {
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        display.getRealMetrics(metrics)

        val node = TargetNode(
            id = UUID.randomUUID().toString(),
            x = metrics.widthPixels / 2,
            y = metrics.heightPixels / 2,
            clickDurationMs = 20L,
            delayAfterMs = 100L
        )
        nodes.add(node)
        uiManager.addNodeView(node, nodes.size - 1)
    }

    fun addSwipeNode() {
        val display = windowManager.defaultDisplay
        val metrics = android.util.DisplayMetrics()
        display.getRealMetrics(metrics)

        val node = TargetNode(
            id = UUID.randomUUID().toString(),
            x = metrics.widthPixels / 2,
            y = metrics.heightPixels / 2 + 200,
            isSwipe = true,
            swipeEndX = metrics.widthPixels / 2,
            swipeEndY = metrics.heightPixels / 2 - 200,
            clickDurationMs = 300L,
            delayAfterMs = 200L
        )
        nodes.add(node)
        uiManager.addNodeView(node, nodes.size - 1)
    }

    fun removeLastNode() {
        if (nodes.isNotEmpty()) {
            val last = nodes.removeAt(nodes.size - 1)
            uiManager.removeNodeView(last.id)
        }
    }

    fun removeNodeById(id: String) {
        val idx = nodes.indexOfFirst { it.id == id }
        if (idx >= 0) {
            nodes.removeAt(idx)
            uiManager.removeNodeView(id)
            for (i in nodes.indices) {
                uiManager.updateNodeView(nodes[i], i)
            }
        }
    }

    fun toggleExecution() {
        when (currentState) {
            ExecutionState.STOPPED, ExecutionState.PAUSED -> startExecution()
            ExecutionState.RUNNING -> pauseExecution()
        }
    }

    fun startExecution() {
        if (nodes.isEmpty()) {
            Toast.makeText(this, "Добавьте хотя бы одну метку!", Toast.LENGTH_SHORT).show()
            return
        }
        currentState = ExecutionState.RUNNING
        uiManager.updatePlayButtonState(currentState)
        currentNodeIndex = 0
        loopCounter = 0
        scriptStartTime = System.currentTimeMillis()
        isSubScriptRunning = false
        subScriptNodes = null
        scheduleNextStep(0L)
    }

    fun pauseExecution() {
        currentState = ExecutionState.PAUSED
        uiManager.updatePlayButtonState(currentState)
        uiManager.clearPhantomNodes()
    }

    fun stopExecution() {
        currentState = ExecutionState.STOPPED
        uiManager.updatePlayButtonState(currentState)
        uiManager.clearPhantomNodes()
        isSubScriptRunning = false
        subScriptNodes = null
    }

    private fun scheduleNextStep(delayMs: Long) {
        if (currentState != ExecutionState.RUNNING) return
        handler.postDelayed({
            executeCurrentStep()
        }, Math.max(0L, delayMs))
    }

    private fun executeCurrentStep() {
        if (currentState != ExecutionState.RUNNING) return

        // Time limit check
        if (targetTimeLimitSec > 0) {
            val elapsedSec = (System.currentTimeMillis() - scriptStartTime) / 1000
            if (elapsedSec >= targetTimeLimitSec) {
                Toast.makeText(this, "Лимит времени ($targetTimeLimitSec сек) исчерпан", Toast.LENGTH_SHORT).show()
                stopExecution()
                return
            }
        }

        // Multitouch parallel execution
        if (enableMultitouch && nodes.isNotEmpty()) {
            executeMultiTouchStep()
            return
        }

        if (isSubScriptRunning) {
            val subNodes = subScriptNodes
            if (subNodes != null && subScriptIndex < subNodes.size) {
                val node = subNodes[subScriptIndex]
                subScriptIndex++
                executeNodeGesture(node) {
                    if (subScriptIndex >= subNodes.size) {
                        isSubScriptRunning = false
                        subScriptNodes = null
                        uiManager.clearPhantomNodes()
                        currentNodeIndex++
                        scheduleNextStep(node.delayAfterMs)
                    } else {
                        scheduleNextStep(node.delayAfterMs)
                    }
                }
                return
            } else {
                isSubScriptRunning = false
                subScriptNodes = null
                uiManager.clearPhantomNodes()
            }
        }

        if (currentNodeIndex >= nodes.size) {
            currentNodeIndex = 0
            loopCounter++
            if (targetLoopCount > 0 && loopCounter >= targetLoopCount) {
                Toast.makeText(this, "Выполнено $targetLoopCount циклов", Toast.LENGTH_SHORT).show()
                stopExecution()
                return
            }
        }

        if (nodes.isEmpty()) {
            stopExecution()
            return
        }

        val node = nodes[currentNodeIndex]

        if (!node.macroScriptId.isNullOrEmpty()) {
            val macroProfile = profiles.find { it.id == node.macroScriptId }
            if (macroProfile != null && macroProfile.nodes.isNotEmpty()) {
                isSubScriptRunning = true
                subScriptNodes = macroProfile.nodes
                subScriptIndex = 0
                uiManager.showPhantomNodes(macroProfile.nodes)
                scheduleNextStep(0L)
                return
            }
        }

        if (!node.textCondition.isNullOrEmpty()) {
            evaluateOcrAndExecute(node)
            return
        }

        val delayBefore = node.delayBeforeMs
        if (delayBefore > 0) {
            handler.postDelayed({
                executeNodeGesture(node) {
                    currentNodeIndex++
                    scheduleNextStep(node.delayAfterMs)
                }
            }, delayBefore)
        } else {
            executeNodeGesture(node) {
                currentNodeIndex++
                scheduleNextStep(node.delayAfterMs)
            }
        }
    }

    private fun executeMultiTouchStep() {
        val builder = GestureDescription.Builder()
        var maxDelay = 100L
        for (node in nodes) {
            uiManager.updateNodeScreenPosition(node)
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
            val duration = Math.max(10L, node.clickDurationMs)
            builder.addStroke(GestureDescription.StrokeDescription(path, 0L, duration))
            if (node.delayAfterMs > maxDelay) maxDelay = node.delayAfterMs
        }

        try {
            dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    loopCounter++
                    if (targetLoopCount > 0 && loopCounter >= targetLoopCount) {
                        stopExecution()
                    } else {
                        scheduleNextStep(maxDelay)
                    }
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    scheduleNextStep(maxDelay)
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
            scheduleNextStep(maxDelay)
        }
    }


    private fun evaluateOcrAndExecute(node: TargetNode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                    if (bitmap != null) {
                        val softwareBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        var cropRect: Rect? = null
                        if (node.ocrRegionRight > node.ocrRegionLeft && node.ocrRegionBottom > node.ocrRegionTop) {
                            cropRect = Rect(node.ocrRegionLeft, node.ocrRegionTop, node.ocrRegionRight, node.ocrRegionBottom)
                        }

                        imageHelper.findTextInBitmap(
                            softwareBitmap,
                            node.textCondition ?: "",
                            node.textConditionExact,
                            cropRect
                        ) { found, rect ->
                            if (found) {
                                executeNodeGesture(node) {
                                    if (node.stopOnSuccess) {
                                        stopExecution()
                                    } else {
                                        currentNodeIndex++
                                        scheduleNextStep(node.delayAfterMs)
                                    }
                                }
                            } else {
                                currentNodeIndex++
                                scheduleNextStep(node.delayAfterMs)
                            }
                        }
                    } else {
                        executeNodeGesture(node) {
                            currentNodeIndex++
                            scheduleNextStep(node.delayAfterMs)
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    executeNodeGesture(node) {
                        currentNodeIndex++
                        scheduleNextStep(node.delayAfterMs)
                    }
                }
            })
        } else {
            executeNodeGesture(node) {
                currentNodeIndex++
                scheduleNextStep(node.delayAfterMs)
            }
        }
    }

    private fun executeNodeGesture(node: TargetNode, onCompleted: () -> Unit) {
        if (currentState != ExecutionState.RUNNING) return

        uiManager.updateNodeScreenPosition(node)

        // EXTREME BURST MODE: If extreme speed is allowed and delay is very low (<= 30ms),
        // we bundle multiple micro-strokes into a single GestureDescription timeline.
        val isFastClick = !node.isSwipe && node.delayAfterMs <= 30L

        if (allowExtremeSpeed && isFastClick) {
            val burstCount = 5
            val tapDuration = Math.max(1L, Math.min(10L, node.clickDurationMs))
            val tapInterval = Math.max(12L, node.delayAfterMs + tapDuration)

            val builder = GestureDescription.Builder()
            for (b in 0 until burstCount) {
                val startTime = b * tapInterval
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
                builder.addStroke(GestureDescription.StrokeDescription(path, startTime, tapDuration))
            }

            try {
                dispatchGesture(builder.build(), object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        onCompleted()
                    }
                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        onCompleted()
                    }
                }, null)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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

        val duration = if (node.isSwipe) Math.max(50L, node.clickDurationMs) else Math.max(10L, node.clickDurationMs)

        if (node.isSwipe) {
            var endX = node.swipeEndX.toFloat()
            var endY = node.swipeEndY.toFloat()
            if (node.randomizeRadius > 0) {
                val angle = Math.random() * Math.PI * 2
                val r = Math.random() * node.randomizeRadius
                endX += (Math.cos(angle) * r).toFloat()
                endY += (Math.sin(angle) * r).toFloat()
            }
            path.lineTo(endX, endY)
        }

        builder.addStroke(GestureDescription.StrokeDescription(path, 0L, duration))

        try {
            dispatchGesture(builder.build(), object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    onCompleted()
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    onCompleted()
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
            onCompleted()
        }
    }

    fun toggleRecordMode(start: Boolean) {
        if (start) {
            Toast.makeText(this, "Запись нажатий активна", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Запись завершена", Toast.LENGTH_SHORT).show()
        }
    }

    fun getProfiles(): List<ScriptProfile> = profiles

    fun saveCurrentAsProfile(name: String) {
        val profile = ScriptProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            nodes = ArrayList(nodes)
        )
        profiles.add(profile)
        saveProfilesToPrefs()
    }

    fun loadProfile(profile: ScriptProfile) {
        stopExecution()
        uiManager.clearAllNodes()
        nodes.clear()
        nodes.addAll(profile.nodes)
        for (i in nodes.indices) {
            uiManager.addNodeView(nodes[i], i)
        }
        Toast.makeText(this, "Загружен: ${profile.name}", Toast.LENGTH_SHORT).show()
    }

    private fun saveProfilesToPrefs() {
        val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE).edit()
        prefs.putInt("profiles_count", profiles.size)
        for (i in profiles.indices) {
            val p = profiles[i]
            prefs.putString("profile_${i}_id", p.id)
            prefs.putString("profile_${i}_name", p.name)
            prefs.putInt("profile_${i}_node_count", p.nodes.size)
            for (j in p.nodes.indices) {
                val n = p.nodes[j]
                prefs.putString("profile_${i}_node_${j}_id", n.id)
                prefs.putInt("profile_${i}_node_${j}_x", n.x)
                prefs.putInt("profile_${i}_node_${j}_y", n.y)
                prefs.putLong("profile_${i}_node_${j}_delay", n.delayAfterMs)
                prefs.putLong("profile_${i}_node_${j}_duration", n.clickDurationMs)
                prefs.putBoolean("profile_${i}_node_${j}_is_swipe", n.isSwipe)
                prefs.putInt("profile_${i}_node_${j}_end_x", n.swipeEndX)
                prefs.putInt("profile_${i}_node_${j}_end_y", n.swipeEndY)
            }
        }
        prefs.apply()
    }

    fun loadSavedProfiles() {
        profiles.clear()
        val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE)
        val count = prefs.getInt("profiles_count", 0)
        for (i in 0 until count) {
            val id = prefs.getString("profile_${i}_id", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()
            val name = prefs.getString("profile_${i}_name", "Сценарий $i") ?: "Сценарий $i"
            val nodeCount = prefs.getInt("profile_${i}_node_count", 0)
            val pNodes = ArrayList<TargetNode>()
            for (j in 0 until nodeCount) {
                val nId = prefs.getString("profile_${i}_node_${j}_id", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()
                val x = prefs.getInt("profile_${i}_node_${j}_x", 500)
                val y = prefs.getInt("profile_${i}_node_${j}_y", 500)
                val delay = prefs.getLong("profile_${i}_node_${j}_delay", 100L)
                val duration = prefs.getLong("profile_${i}_node_${j}_duration", 20L)
                val isSwipe = prefs.getBoolean("profile_${i}_node_${j}_is_swipe", false)
                val endX = prefs.getInt("profile_${i}_node_${j}_end_x", 500)
                val endY = prefs.getInt("profile_${i}_node_${j}_end_y", 300)

                pNodes.add(
                    TargetNode(
                        id = nId,
                        x = x,
                        y = y,
                        delayAfterMs = delay,
                        clickDurationMs = duration,
                        isSwipe = isSwipe,
                        swipeEndX = endX,
                        swipeEndY = endY
                    )
                )
            }
            profiles.add(ScriptProfile(id, name, pNodes))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        imageHelper.release()
        instance = null
    }
}
