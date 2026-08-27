with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target = """    private fun restoreTouchabilitySafe() {
        if (isDispatchingGesture || isDispatchingRecordGesture) {
            handler.postDelayed({ restoreTouchabilitySafe() }, 100)
        } else {
            if (::uiManager.isInitialized) {
                restoreTouchabilitySafe()
            }
        }
    }"""

rep = """    private fun restoreTouchabilitySafe() {
        if (isDispatchingGesture || isDispatchingRecordGesture) {
            handler.postDelayed({ restoreTouchabilitySafe() }, 100)
        } else {
            if (::uiManager.isInitialized) {
                uiManager.setNodesTouchable(true)
            }
        }
    }"""

text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Fixed infinite loop")
