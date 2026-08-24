with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

phantom_code = """
    val phantomMap = mutableMapOf<Int, List<View>>()
    private var phantomIdCounter = 0

    fun showPhantomNodes(nodes: List<TargetNode>): Int {
        val id = ++phantomIdCounter
        val views = mutableListOf<View>()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            for (node in nodes) {
                val crosshair = CrosshairView(service, node)
                crosshair.alpha = 0.5f // Phantom transparency
                
                val params = WindowManager.LayoutParams(
                    dpToPx(60), dpToPx(60),
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    else
                        WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.TOP or Gravity.START
                params.x = node.x - dpToPx(30)
                params.y = node.y - dpToPx(30)
                
                try {
                    windowManager.addView(crosshair, params)
                    views.add(crosshair)
                } catch(e: Exception){}
                
                if (node.isSwipe) {
                    val endCrosshair = CrosshairView(service, node)
                    endCrosshair.alpha = 0.5f
                    val endParams = WindowManager.LayoutParams(
                        dpToPx(60), dpToPx(60),
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        else
                            WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    )
                    endParams.gravity = Gravity.TOP or Gravity.START
                    endParams.x = node.swipeEndX - dpToPx(30)
                    endParams.y = node.swipeEndY - dpToPx(30)
                    try {
                        windowManager.addView(endCrosshair, endParams)
                        views.add(endCrosshair)
                    } catch(e: Exception){}
                }
            }
            phantomMap[id] = views
        }
        return id
    }

    fun hidePhantomNodes(id: Int) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val views = phantomMap.remove(id)
            views?.forEach { 
                try { windowManager.removeView(it) } catch(e: Exception){} 
            }
        }
    }

    fun removeAllPhantomNodes() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            phantomMap.values.flatten().forEach { 
                try { windowManager.removeView(it) } catch(e: Exception){} 
            }
            phantomMap.clear()
        }
    }
"""

if "fun removeAllPhantomNodes" not in text:
    target = "    fun dpToPx(dp: Int): Int ="
    text = text.replace(target, phantom_code + "\n" + target)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
