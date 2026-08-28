with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target1 = """    private fun processGestureQueue() {
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
    }"""

rep1 = """    private fun processGestureQueue() {
        if (isDispatchingGesture || gestureQueue.isEmpty() || !isPlaying) return
        val gesture = gestureQueue.poll() ?: return
        
        isDispatchingGesture = true
        dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                processGestureQueue()
            }
            override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                processGestureQueue()
            }
        }, null)
    }"""

text = text.replace(target1, rep1)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
