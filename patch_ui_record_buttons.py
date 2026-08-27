with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = """        layout.addView(headerRow)
        layout.addView(startBtn)
        layout.addView(addClickBtn)"""

rep = """        layout.addView(headerRow)
        
        val recordPlayLayout = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL }
        if (isRec) {
            val rpStart = Button(service).apply {
                text = if (service.isRecording) "■ СТОП" else "🔴 ЗАПИСЬ"
                setBackgroundColor(if (service.isRecording) Color.RED else Color.GREEN)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { 
                    service.toggleRecording()
                    updateMenu()
                }
            }
            val rpPlay = Button(service).apply {
                text = if (service.isPlaying) "■ СТОП" else "▶ ПРОИГРАТЬ"
                setBackgroundColor(if (service.isPlaying) Color.RED else Color.parseColor("#FF00C853"))
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { 
                    service.togglePlay()
                    updateMenu()
                }
            }
            recordPlayLayout.addView(rpStart)
            recordPlayLayout.addView(rpPlay)
            layout.addView(recordPlayLayout)
        } else {
            layout.addView(startBtn)
        }
        
        layout.addView(addClickBtn)"""

if target in text:
    text = text.replace(target, rep)
    with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
        f.write(text)
    print("Success UI Buttons")
else:
    print("Not found UI Buttons")
