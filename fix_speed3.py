with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

text = text.replace(
"""        if (allowExtremeSpeed) {
            isDispatchingGesture = false
            processGestureQueue() // Continue immediately
        }""",
"""        if (allowExtremeSpeed) {
            isDispatchingGesture = false
            handler.post { processGestureQueue() } // Continue immediately
        }"""
)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
