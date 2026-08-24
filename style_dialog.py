with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = """        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(20, 20, 20, 20)
        }"""
rep = """        val layout = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#1E1E1E"))
                cornerRadius = dpToPx(16).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#444444"))
            }
            setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
        }"""
text = text.replace(target, rep)

# Also let's style the header of the dialog (where the spinner is)
header_target = """        val headerTitleLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(10))
        }
        val headerTitle = TextView(service).apply {
            text = "Шаг ${node.id}"
            setTextColor(Color.WHITE)
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }"""
header_rep = """        val headerTitleLayout = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(15))
        }
        val headerTitle = TextView(service).apply {
            text = "Настройка Шага ${node.id}"
            setTextColor(Color.parseColor("#FFFFFF"))
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
        }"""
text = text.replace(header_target, header_rep)


with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Updated Main Dialog Style")
