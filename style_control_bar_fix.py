with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = """        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FF111111"))
            setPadding(5, 5, 5, 5)
            
            // Add a border
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(Color.parseColor("#FF111111"))
            drawable.setStroke(dpToPx(2), Color.parseColor("#FF4CAF50"))
            drawable.cornerRadius = dpToPx(8).toFloat()
            background = drawable
        }"""
rep = """        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5))
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(Color.parseColor("#E61E1E1E")) // 90% opacity dark grey
            drawable.setStroke(dpToPx(1), Color.parseColor("#333333"))
            drawable.cornerRadius = dpToPx(12).toFloat()
            background = drawable
            clipToOutline = true
        }"""
text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Fixed Control Bar Style")
