with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = """                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)"""
rep = """                MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY
                    windowManager.updateViewLayout(view, params)"""
text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Added bounding box clamp to setupTextZoneTouchListener")
