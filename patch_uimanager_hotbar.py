with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = """    private fun updateHotbar(container: LinearLayout) {
        container.removeAllViews()
        val profiles = service.getSavedProfiles()
        if (profiles.isEmpty()) {
            val tv = TextView(service).apply {
                text = "Нет профилей"
                setTextColor(Color.GRAY)
                setPadding(dpToPx(10), 0, dpToPx(10), 0)
            }
            container.addView(tv)
            return
        }
        for (p in profiles) {
            val btn = Button(service).apply {
                text = p
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#222222"))
                setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, dpToPx(5), 0)
                layoutParams = params
                
                setOnClickListener {
                    service.loadProfile(p)
                    service.uiManager.updateMenu()
                    android.widget.Toast.makeText(service, "Загружен: $p", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            container.addView(btn)
        }
    }"""

rep = """    private fun showHotbarConfigDialog() {
        val allProfiles = service.getSavedProfiles()
        val currentHotbar = service.getHotbarItems().toMutableList()
        
        val dialogView = ScrollView(service).apply {
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }
        val layout = LinearLayout(service).apply { orientation = LinearLayout.VERTICAL }
        dialogView.addView(layout)
        
        val resultList = mutableListOf<Pair<String, android.widget.EditText>>()
        
        for (p in allProfiles) {
            val itemLayout = LinearLayout(service).apply { 
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dpToPx(10))
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            
            val cb = android.widget.CheckBox(service).apply {
                text = p
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val et = android.widget.EditText(service).apply {
                hint = "Смайл/Имя"
                setHintTextColor(Color.GRAY)
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(dpToPx(100), LinearLayout.LayoutParams.WRAP_CONTENT)
            }
            
            val existing = currentHotbar.find { it.first == p }
            if (existing != null) {
                cb.isChecked = true
                et.setText(existing.second)
            } else {
                cb.isChecked = false
                et.setText(p)
            }
            
            itemLayout.addView(cb)
            itemLayout.addView(et)
            layout.addView(itemLayout)
            
            resultList.add(Pair(p, et))
            
            cb.setOnCheckedChangeListener { _, _ ->
                // Do nothing, just state
            }
        }
        
        val dialog = android.app.AlertDialog.Builder(service, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Настройка Хотбара")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newItems = mutableListOf<Pair<String, String>>()
                for (i in 0 until layout.childCount) {
                    val row = layout.getChildAt(i) as LinearLayout
                    val cb = row.getChildAt(0) as android.widget.CheckBox
                    val et = row.getChildAt(1) as android.widget.EditText
                    if (cb.isChecked) {
                        newItems.add(Pair(cb.text.toString(), et.text.toString().takeIf { it.isNotBlank() } ?: cb.text.toString()))
                    }
                }
                service.saveHotbarItems(newItems)
                updateHotbar(hotbarContainer!!)
            }
            .setNegativeButton("Отмена", null)
            .create()
            
        dialog.window?.setType(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        )
        dialog.show()
    }

    var hotbarContainer: LinearLayout? = null

    private fun updateHotbar(container: LinearLayout) {
        hotbarContainer = container
        container.removeAllViews()
        val profiles = service.getHotbarItems()
        
        val configBtn = Button(service).apply {
            text = "⚙️"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#444444"))
            setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, dpToPx(5), 0)
            layoutParams = params
            setOnClickListener { showHotbarConfigDialog() }
        }
        container.addView(configBtn)
        
        if (profiles.isEmpty()) {
            val tv = TextView(service).apply {
                text = "Пусто"
                setTextColor(Color.GRAY)
                setPadding(dpToPx(10), 0, dpToPx(10), 0)
            }
            container.addView(tv)
            return
        }
        for (p in profiles) {
            val btn = Button(service).apply {
                text = p.second
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#222222"))
                setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, dpToPx(5), 0)
                layoutParams = params
                
                setOnClickListener {
                    val df = java.text.SimpleDateFormat("HH-mm-ss", java.util.Locale.getDefault())
                    val autoSaveName = "AutoSave / " + df.format(java.util.Date())
                    service.saveProfile(autoSaveName)
                    
                    service.loadProfile(p.first)
                    service.uiManager.updateMenu()
                    android.widget.Toast.makeText(service, "Автосохранено '$autoSaveName'. Загружен: ${p.first}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            container.addView(btn)
        }
    }"""

text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Updated updateHotbar in UIManager")
