with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = """        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FF111111"))
            setPadding(5, 5, 5, 5)
            
            // Add a border
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF111111"))
                setStroke(2, Color.parseColor("#555555"))
                cornerRadius = 10f
            }
        }"""
rep = """        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5))
            
            // Add a modern border
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#E61E1E1E")) // 90% opacity dark grey
                setStroke(dpToPx(1), Color.parseColor("#333333"))
                cornerRadius = dpToPx(12).toFloat()
            }
            clipToOutline = true
        }"""
text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Updated Control Bar Style")
