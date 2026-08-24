with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

# Make the menu more readable
old_add_section = """        fun addSection(title: String, hasChanges: Boolean, buildContent: (LinearLayout) -> Unit): LinearLayout {
            val sectionLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 5, 0, 5)
            }
            
            val headerLayout = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#444444"))
                setPadding(20, 20, 20, 20)
            }
            
            val titleText = TextView(service).apply {
                text = title
                setTextColor(Color.WHITE)
                setScaledTextSize(14f)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val checkIcon = TextView(service).apply {
                text = " ✔ "
                setTextColor(Color.parseColor("#4CAF50"))
                setScaledTextSize(14f)
                visibility = if (hasChanges) View.VISIBLE else View.GONE
            }
            
            val icon = TextView(service).apply {
                text = "▼"
                setTextColor(Color.parseColor("#E0E0E0"))
                setScaledTextSize(14f)
            }
            
            headerLayout.addView(titleText)
            headerLayout.addView(checkIcon)
            headerLayout.addView(icon)
            
            val bodyLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)
                visibility = View.GONE
                setBackgroundColor(Color.parseColor("#2A2A2A"))
            }
            
            buildContent(bodyLayout)
            
            headerLayout.setOnClickListener {
                if (bodyLayout.visibility == View.VISIBLE) {
                    bodyLayout.visibility = View.GONE
                    icon.text = "▼"
                } else {
                    bodyLayout.visibility = View.VISIBLE
                    icon.text = "▲"
                }
            }
            
            sectionLayout.addView(headerLayout)
            sectionLayout.addView(bodyLayout)
            content.addView(sectionLayout)
            return sectionLayout
        }"""

new_add_section = """        fun addSection(title: String, hasChanges: Boolean, buildContent: (LinearLayout) -> Unit): LinearLayout {
            val sectionLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                val marginParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                marginParams.setMargins(0, dpToPx(5), 0, dpToPx(5))
                layoutParams = marginParams
                
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#252525"))
                    cornerRadius = dpToPx(12).toFloat()
                    setStroke(dpToPx(1), Color.parseColor("#333333"))
                }
                clipToOutline = true
            }
            
            val headerLayout = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
            }
            
            val titleText = TextView(service).apply {
                text = title
                setTextColor(Color.parseColor("#EEEEEE"))
                setScaledTextSize(14f)
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val checkIcon = TextView(service).apply {
                text = " ● "
                setTextColor(Color.parseColor("#4CAF50"))
                setScaledTextSize(12f)
                visibility = if (hasChanges) View.VISIBLE else View.GONE
            }
            
            val icon = TextView(service).apply {
                text = "﹀"
                setTextColor(Color.parseColor("#888888"))
                setScaledTextSize(16f)
            }
            
            headerLayout.addView(titleText)
            headerLayout.addView(checkIcon)
            headerLayout.addView(icon)
            
            val bodyLayout = LinearLayout(service).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(15), 0, dpToPx(15), dpToPx(15))
                visibility = View.GONE
            }
            
            buildContent(bodyLayout)
            
            headerLayout.setOnClickListener {
                if (bodyLayout.visibility == View.VISIBLE) {
                    bodyLayout.visibility = View.GONE
                    icon.text = "﹀"
                } else {
                    bodyLayout.visibility = View.VISIBLE
                    icon.text = "︿"
                }
            }
            
            sectionLayout.addView(headerLayout)
            sectionLayout.addView(bodyLayout)
            content.addView(sectionLayout)
            return sectionLayout
        }"""

text = text.replace(old_add_section, new_add_section)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Updated addSection styling")
