import re

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

# We only want to clamp params.x and params.y for the floating markers, not necessarily the menus, but it's safe to clamp menus to the screen too.
# Let's target the exact string:
pattern = r"MotionEvent\.ACTION_MOVE -> \{\s+params\.x = initialX \+ \(event\.rawX - initialTouchX\)\.toInt\(\)\s+params\.y = initialY \+ \(event\.rawY - initialTouchY\)\.toInt\(\)"

replacement = """MotionEvent.ACTION_MOVE -> {
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + (event.rawX - initialTouchX).toInt()
                    var newY = initialY + (event.rawY - initialTouchY).toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY"""
text = re.sub(pattern, replacement, text)

# For layout variants (where it updates windowManager.updateViewLayout(layout, params)):
pattern2 = r"MotionEvent\.ACTION_MOVE -> \{\s+val dx = event\.rawX - initialTouchX\s+val dy = event\.rawY - initialTouchY\s+if \(Math\.abs\(dx\) > 10 \|\| Math\.abs\(dy\) > 10\) isMoved = true\s+params\.x = initialX \+ dx\.toInt\(\)\s+params\.y = initialY \+ dy\.toInt\(\)"

replacement2 = """MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isMoved = true
                    val metrics = service.resources.displayMetrics
                    var newX = initialX + dx.toInt()
                    var newY = initialY + dy.toInt()
                    if (newX < 0) newX = 0
                    if (newY < 0) newY = 0
                    if (newX > metrics.widthPixels - dpToPx(30)) newX = metrics.widthPixels - dpToPx(30)
                    if (newY > metrics.heightPixels - dpToPx(30)) newY = metrics.heightPixels - dpToPx(30)
                    params.x = newX
                    params.y = newY"""
text = re.sub(pattern2, replacement2, text)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Added universal bounding box clamp")
