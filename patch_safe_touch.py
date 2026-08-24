with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

func = """    private fun restoreTouchabilitySafe() {
        if (isDispatchingGesture || isDispatchingRecordGesture) {
            handler.postDelayed({ restoreTouchabilitySafe() }, 100)
        } else {
            if (::uiManager.isInitialized) {
                uiManager.setNodesTouchable(true)
            }
        }
    }
"""

if "fun restoreTouchabilitySafe" not in text:
    target_insert = "    private fun checkAllThreadsStopped() {"
    text = text.replace(target_insert, func + "\n" + target_insert)

# Replace all uiManager.setNodesTouchable(true) with restoreTouchabilitySafe(), EXCEPT line 297?
# Actually, it's safe to use it everywhere.
text = text.replace("uiManager.setNodesTouchable(true)", "restoreTouchabilitySafe()")

# Find togglePlay to clear gestureQueue
target_toggle = """        if (isPlaying) {
            isPlaying = false
            if (::uiManager.isInitialized) uiManager.logDebug("--- СТОП ---")
            restoreTouchabilitySafe()"""
rep_toggle = """        if (isPlaying) {
            isPlaying = false
            gestureQueue.clear()
            if (::uiManager.isInitialized) uiManager.logDebug("--- СТОП ---")
            restoreTouchabilitySafe()"""
text = text.replace(target_toggle, rep_toggle)

# Find checkAllThreadsStopped to clear gestureQueue
target_check = """    private fun checkAllThreadsStopped() {
        if (activeThreads.all { !it.isActive }) {
            isPlaying = false
            restoreTouchabilitySafe()"""
rep_check = """    private fun checkAllThreadsStopped() {
        if (activeThreads.all { !it.isActive }) {
            isPlaying = false
            gestureQueue.clear()
            restoreTouchabilitySafe()"""
text = text.replace(target_check, rep_check)

# Find maxPlayDurationMs block to clear gestureQueue
target_max = """            if (elapsed >= maxPlayDurationMs!!) {
                isPlaying = false
                if (::uiManager.isInitialized) uiManager.logDebug("СТОП: Лимит времени истек")
                restoreTouchabilitySafe()"""
rep_max = """            if (elapsed >= maxPlayDurationMs!!) {
                isPlaying = false
                gestureQueue.clear()
                if (::uiManager.isInitialized) uiManager.logDebug("СТОП: Лимит времени истек")
                restoreTouchabilitySafe()"""
text = text.replace(target_max, rep_max)

# Fix Black Hole in executeNode
target_exec = """        checkConditionForNode(node) { isMatch ->
            if (!isPlaying || !thread.isActive) return@checkConditionForNode"""
rep_exec = """        checkConditionForNode(node) { isMatch ->
            if (!isPlaying || !thread.isActive) return@checkConditionForNode
            
            if (nodes.find { it.id == node.id } == null) {
                thread.isActive = false
                checkAllThreadsStopped()
                return@checkConditionForNode
            }"""
text = text.replace(target_exec, rep_exec)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Patch safe touch and black hole")
