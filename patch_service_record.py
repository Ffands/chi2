with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target_hud_check = """                    android.view.MotionEvent.ACTION_DOWN -> {
                        val currentTime = System.currentTimeMillis()"""

rep_hud_check = """                    android.view.MotionEvent.ACTION_DOWN -> {
                        if (::uiManager.isInitialized && uiManager.floatingControlBar != null) {
                            val hudRect = android.graphics.Rect()
                            uiManager.floatingControlBar?.getGlobalVisibleRect(hudRect)
                            val pad = 10
                            if (event.rawX >= hudRect.left - pad && event.rawX <= hudRect.right + pad &&
                                event.rawY >= hudRect.top - pad && event.rawY <= hudRect.bottom + pad) {
                                return@setOnTouchListener false
                            }
                        }
                        val currentTime = System.currentTimeMillis()"""

target_coords = """                        recordDownX = event.rawX
                        recordDownY = event.rawY
                        dragX = event.rawX
                        dragY = event.rawY
                        isDragging = true
                        swipePoints.clear()
                        swipePoints.add(Pair(event.rawX, event.rawY))"""

rep_coords = """                        recordDownX = event.rawX
                        recordDownY = event.rawY
                        dragX = event.x
                        dragY = event.y
                        isDragging = true
                        swipePoints.clear()
                        swipePoints.add(Pair(event.x, event.y))"""

target_move = """                    android.view.MotionEvent.ACTION_MOVE -> {
                        dragX = event.rawX
                        dragY = event.rawY
                        val last = swipePoints.lastOrNull()
                        if (last == null || Math.hypot((event.rawX - last.first).toDouble(), (event.rawY - last.second).toDouble()) > 10) {
                            swipePoints.add(Pair(event.rawX, event.rawY))
                        }"""

rep_move = """                    android.view.MotionEvent.ACTION_MOVE -> {
                        dragX = event.x
                        dragY = event.y
                        val last = swipePoints.lastOrNull()
                        if (last == null || Math.hypot((event.x - last.first).toDouble(), (event.y - last.second).toDouble()) > 10) {
                            swipePoints.add(Pair(event.x, event.y))
                        }"""

target_up = """                    android.view.MotionEvent.ACTION_UP -> {
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
                            val isSwipe = Math.hypot(dx.toDouble(), dy.toDouble()) > 20
                            
                            val path = android.graphics.Path().apply {
                                if (isSwipe && swipePoints.size >= 2) {
                                    moveTo(swipePoints[0].first, swipePoints[0].second)
                                    for (i in 1 until swipePoints.size) {
                                        lineTo(swipePoints[i].first, swipePoints[i].second)
                                    }
                                } else {
                                    moveTo(startX, startY)
                                    if (isSwipe) lineTo(upX, upY)
                                }
                            }
                            val gestureDur = Math.max(duration, 30L)"""

rep_up = """                    android.view.MotionEvent.ACTION_UP -> {
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
                            val gestureDur = if (isSwipe) Math.max(duration, 30L) else Math.max(duration, 10L)"""

if target_hud_check in text and target_coords in text and target_move in text and target_up in text:
    text = text.replace(target_hud_check, rep_hud_check)
    text = text.replace(target_coords, rep_coords)
    text = text.replace(target_move, rep_move)
    text = text.replace(target_up, rep_up)
    with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
        f.write(text)
    print("Success patch AutoClickService")
else:
    print("Failed to find targets in AutoClickService")
