import re

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target_queue = """    private fun processGestureQueue() {
        if (isDispatchingGesture || gestureQueue.isEmpty() || !isPlaying) return
        val gesture = gestureQueue.poll() ?: return
        
        isDispatchingGesture = true
        val success = dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                processGestureQueue()
            }
            override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                processGestureQueue()
            }
        }, null)
        
        if (!success) {
            isDispatchingGesture = false
            if (::uiManager.isInitialized) uiManager.logDebug("Ошибка: dispatchGesture вернул false")
            processGestureQueue()
        }
    }"""

rep_queue = """    private fun processGestureQueue() {
        if (gestureQueue.isEmpty() || !isPlaying) return
        if (isDispatchingGesture && !allowExtremeSpeed) return
        
        val gesture = gestureQueue.poll() ?: return
        
        isDispatchingGesture = true
        val success = dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
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
        
        if (!success) {
            if (!allowExtremeSpeed) {
                isDispatchingGesture = false
                processGestureQueue()
            }
        }
        
        if (allowExtremeSpeed) {
            isDispatchingGesture = false
            processGestureQueue() // Continue immediately
        }
    }"""

text = text.replace(target_queue, rep_queue)

target_delay = """            val minDelay = 1L
            
            val finalDelay = if (!isMatch && thread.currentNodeId == node.id) {
                val pollDelay = if (node.triggerMode == 2) 300L else 150L
                Math.max(pollDelay, minDelay)
            } else {
                if (allowExtremeSpeed) 1L else Math.max(minDelay, node.delayAfterMs + randomDelay)
            }

            handler.postDelayed({ executeThread(thread) }, finalDelay)"""

rep_delay = """            val finalDelay = if (!isMatch && thread.currentNodeId == node.id) {
                if (node.triggerMode == 2) 300L else 150L
            } else {
                if (allowExtremeSpeed) 0L else Math.max(0L, node.delayAfterMs + randomDelay)
            }

            if (finalDelay <= 0L) {
                handler.post { executeThread(thread) }
            } else {
                handler.postDelayed({ executeThread(thread) }, finalDelay)
            }"""

text = text.replace(target_delay, rep_delay)

target_click_dur = """val duration = if (allowExtremeSpeed) 10L else (if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)"""
rep_click_dur = """val duration = if (allowExtremeSpeed) 1L else (if (node.isSwipe) node.swipeDurationMs else Math.max(1L, node.clickDurationMs))"""

text = text.replace(target_click_dur, rep_click_dur)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
