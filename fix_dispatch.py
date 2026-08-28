with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target = """        isDispatchingGesture = true
        dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                processGestureQueue()
            }
            override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                isDispatchingGesture = false
                processGestureQueue()
            }
        }, null)"""

rep = """        isDispatchingGesture = true
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
        }"""

text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
