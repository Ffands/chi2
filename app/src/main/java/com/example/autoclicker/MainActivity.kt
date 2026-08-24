package com.example.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.RadioGroup
import android.widget.RadioButton
import android.content.Context

class MainActivity : Activity() {
    companion object {
        const val REQ_EXPORT = 1001
        const val REQ_IMPORT = 1002
        var pendingExportData: String? = null
        var pendingImportData: String? = null
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.action ?: return
        intent.action = null // Clear action so we don't trigger it again on rotation
        
        when (action) {
            "ACTION_EXPORT_PROFILE" -> {
                val name = intent.getStringExtra("profile_name") ?: "AutoClickerProfile"
                val sfIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "$name.json")
                }
                startActivityForResult(sfIntent, REQ_EXPORT)
            }
            "ACTION_IMPORT_PROFILE" -> {
                val sfIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(sfIntent, REQ_IMPORT)
            }
            "ACTION_SHARE_PROFILE" -> {
                val data = pendingExportData
                val title = intent.getStringExtra("profile_name") ?: "AutoClicker Profile"
                try {
                    val cachePath = java.io.File(cacheDir, "shared_profiles")
                    cachePath.mkdirs()
                    val newFile = java.io.File(cachePath, "${title.replace(" ", "_")}.json")
                    newFile.writeText(data ?: "{}")
                    
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(this, "com.example.autoclicker.fileprovider", newFile)
                    
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TITLE, title)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(sendIntent, "Поделиться сценарием"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(this, "Ошибка при отправке сценария", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    try {
                        val fileContent = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                        if (fileContent != null) {
                            val instance = AutoClickService.instance
                            if (instance != null) {
                                instance.loadProfileFromJson(fileContent, append = false)
                                android.widget.Toast.makeText(this, "Профиль успешно загружен", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                pendingImportData = fileContent
                                android.widget.Toast.makeText(this, "Включите службу Автокликера для импорта профиля", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(this, "Ошибка загрузки: неверный формат файла", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                if (requestCode == REQ_EXPORT) {
                    try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(pendingExportData?.toByteArray() ?: ByteArray(0))
                        }
                        android.widget.Toast.makeText(this, "Профиль сохранен в файл", android.widget.Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(this, "Ошибка сохранения", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else if (requestCode == REQ_IMPORT) {
                    try {
                        val content = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                        if (content != null) {
                            val instance = AutoClickService.instance
                            if (instance != null) {
                                instance.loadProfileFromJson(content)
                                android.widget.Toast.makeText(this, "Профиль успешно импортирован", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(this, "Служба не запущена", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(this, "Ошибка загрузки", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#121212"))
        }
        
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 80)
        }

        val title = TextView(this).apply {
            text = "UpwellClick\nУльтимативный Автокликер"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#4CAF50"))
            setPadding(0, 0, 0, 60)
        }
        contentLayout.addView(title)

        // Card 1: How to enable
        val card1 = createCard(
            "Как включить автокликер",
            "Для работы программы необходимо разрешение на симуляцию нажатий.\n\n" +
            "1. Нажмите кнопку ниже.\n" +
            "2. Найдите 'UpwellClick' (может быть в разделе 'Установленные приложения').\n" +
            "3. Переведите переключатель в положение ВКЛ.",
            Color.parseColor("#1E1E1E")
        )
        contentLayout.addView(card1)

        // Card 2: Android 13+ constraints
        val card2 = createCard(
            "⚠️ ВАЖНО: Android 13+ (Ограниченные настройки)",
            "Если система пишет «Ограниченные настройки» и не дает нажать на тумблер:\n\n" +
            "1. Нажмите кнопку ниже «Настройки приложения».\n" +
            "2. Нажмите на 3 точки в правом верхнем углу (или полистайте вниз) и выберите «Разрешить ограниченные настройки».\n" +
            "3. Подтвердите паролем или отпечатком.\n" +
            "4. Теперь общие настройки автокликера будут разблокированы.",
            Color.parseColor("#2C1A1A") // subtle dark red
        )
        contentLayout.addView(card2)

        val appSettingsBtn = Button(this).apply {
            text = "ОТКРЫТЬ НАСТРОЙКИ ПРИЛОЖЕНИЯ (Android 13+)"
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 30, 0, 30)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 40)
            layoutParams = params
            setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + packageName)))
            }
        }
        contentLayout.addView(appSettingsBtn)

        // Card 3: Brief instructions for new users
        val card3 = createCard(
            "📖 Как пользоваться (Справка)",
            "Этот автокликер умеет нажимать на экран за вас!\n\n" +
            "• Одиночные клики: Нажмите [+] для добавления метки, наведите её на нужную кнопку и нажмите [▶].\n" +
            "• Свайпы: Добавьте две метки, зайдите в настройки (шестеренка) первой метки и укажите «Вести к метке № 2».\n" +
            "• Продвинутый режим: Метку можно сделать Триггером (ждать цвета или текста), чтобы кликер принимал решения!\n" +
            "• Запись: В меню переключите режим на «Запись», нажмите [REC] и прокликайте свой сценарий вручную. Кликер повторит всё точь-в-точь!\n\n" +
            "В МЕНЮ АВТОКЛИКЕРА (шестеренка) есть отдельный раздел «Справка» со всеми деталями.",
            Color.parseColor("#1B2A38")
        )
        contentLayout.addView(card3)
        
        val modeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1B2A38"))
            setPadding(40, 40, 40, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 40)
            layoutParams = params
        }
        val modeTitle = TextView(this).apply {
            text = "РЕЖИМ РАБОТЫ"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }
        modeLayout.addView(modeTitle)
        
        val modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        
        val modes = listOf(
            "SINGLE" to "Одиночный режим (1 метка)",
            "SEQUENTIAL" to "Многоцелевой режим (Цепочка)",
            "ADVANCED" to "Инженерный режим (Сложная логика)",
            "RECORD" to "Запись макроса (Ручной ввод)"
        )
        
        val prefs = getSharedPreferences("AutoClickerSettings", Context.MODE_PRIVATE)
        val currentMode = prefs.getString("AppMode", "ADVANCED")
        
        for ((modeId, modeDesc) in modes) {
            val rb = RadioButton(this).apply {
                text = modeDesc
                setTextColor(Color.WHITE)
                textSize = 16f
                setPadding(0, 20, 0, 20)
                tag = modeId
                isChecked = modeId == currentMode
            }
            modeGroup.addView(rb)
        }
        
        modeGroup.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<RadioButton>(checkedId)
            val selectedMode = rb.tag.toString()
            prefs.edit().putString("AppMode", selectedMode).apply()
            
            // Apply to running service if possible
            if (AutoClickService.instance != null) {
                AutoClickService.instance!!.updateAppMode(selectedMode)
            }
        }
        
        modeLayout.addView(modeGroup)
        contentLayout.addView(modeLayout)

        val fullGuideBtn = Button(this).apply {
            text = "📖 ПОЛНОЕ РУКОВОДСТВО"
            setBackgroundColor(Color.parseColor("#9C27B0")) // Purple
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 30, 0, 30)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 40)
            layoutParams = params
            setOnClickListener {
                showFullGuideDialog()
            }
        }
        contentLayout.addView(fullGuideBtn)

        val statusLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#222222"))
            setPadding(40, 40, 40, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 40, 0, 40)
            layoutParams = params
        }

        val statusText = TextView(this).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        statusLayout.addView(statusText)
        contentLayout.addView(statusLayout)

        val settingsBtn = Button(this).apply {
            text = "ОТКРЫТЬ НАСТРОЙКИ"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        contentLayout.addView(settingsBtn)

        val startUiBtn = Button(this).apply {
            text = "МЕНЮ АВТОКЛИКЕРА"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 30, 0, 30)
            visibility = View.GONE
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 30, 0, 0)
            layoutParams = params

            setOnClickListener {
                if (AutoClickService.instance != null) {
                    val intent = Intent(this@MainActivity, AutoClickService::class.java)
                    intent.action = "SHOW_UI"
                    startService(intent)
                    finish()
                }
            }
        }
        contentLayout.addView(startUiBtn)
        
        val showTreeBtn = Button(this).apply {
            text = "ДЕРЕВО СЦЕНАРИЯ"
            setBackgroundColor(Color.parseColor("#9C27B0"))
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 30, 0, 30)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 30, 0, 0)
            layoutParams = params
            setOnClickListener {
                showScenarioTree()
            }
        }
        contentLayout.addView(showTreeBtn)

        contentLayout.post {
            if (AutoClickService.instance != null) {
                statusText.text = "Статус: СЛУЖБА ВКЛЮЧЕНА ✔"
                statusText.setTextColor(Color.parseColor("#4CAF50"))
                startUiBtn.visibility = View.VISIBLE
            } else {
                statusText.text = "Статус: СЛУЖБА ОТКЛЮЧЕНА ❌"
                statusText.setTextColor(Color.parseColor("#F44336"))
                startUiBtn.visibility = View.GONE
            }
        }

        scroll.addView(contentLayout)
        setContentView(scroll)

        handleIntent(intent)
    }

    private fun showScenarioTree() {
        val scroll = ScrollView(this).apply {
            setPadding(40, 40, 40, 40)
        }
        
        val treeText = TextView(this).apply {
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            setTextColor(Color.BLACK)
        }
        
        val instance = AutoClickService.instance
        if (instance == null || instance.nodes.isEmpty()) {
            treeText.text = "Сценарий пуст или служба не запущена."
        } else {
            val sb = java.lang.StringBuilder()
            sb.append("Ваш сценарий:\n\n")
            for (node in instance.nodes) {
                sb.append("Метка [${node.id}]: ${if(node.isSwipe) "Свайп" else "Клик"}\n")
                if (node.triggerMode == 0) sb.append("  ↳ Условие: Цвет ${node.colorOperator}\n")
                if (node.triggerMode == 1) sb.append("  ↳ Условие: Картинка\n")
                if (node.triggerMode == 2) sb.append("  ↳ Условие: Текст '${node.targetText}'\n")
                
                if (node.linkedConditionNodeId != null) {
                    sb.append("  ↳ Логика: ${node.linkedConditionOperator} условие Метки [${node.linkedConditionNodeId}]\n")
                }
                
                if (node.nextNodeIdOnSuccess != null) {
                    sb.append("  ↳ При успехе -> [${node.nextNodeIdOnSuccess}]\n")
                } else if (!node.skipSequentialExecution) {
                    sb.append("  ↳ При успехе -> [Следующая по списку]\n")
                }
                
                if (node.maxCheckCycles != null && node.maxCheckCycles!! > 0) {
                    sb.append("  ↳ Циклов проверок: ${node.maxCheckCycles}\n")
                    if (node.nextNodeIdOnFail != null) {
                        sb.append("  ↳ При провале -> [${node.nextNodeIdOnFail}]\n")
                    } else {
                        sb.append("  ↳ При провале -> [Следующая по списку]\n")
                    }
                }
                sb.append("\n")
            }
            treeText.text = sb.toString()
        }
        
        scroll.addView(treeText)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("ДЕРЕВО СЦЕНАРИЯ")
            .setView(scroll)
            .setPositiveButton("ЗАКРЫТЬ", null)
            .show()
    }
    private fun createCard(titleText: String, bodyText: String, bgColor: Int): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(40, 40, 40, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 40)
            layoutParams = params
        }

        val title = TextView(this).apply {
            text = titleText
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }
        container.addView(title)

        val body = TextView(this).apply {
            text = bodyText
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setLineSpacing(0f, 1.2f)
        }
        container.addView(body)

        return container
    }

    private fun showFullGuideDialog() {
        val scroll = ScrollView(this).apply {
            setPadding(40, 40, 40, 40)
        }
        val text = TextView(this).apply {
            textSize = 14f
            setLineSpacing(0f, 1.2f)
            text = "--- ГЛАВНОЕ МЕНЮ И ХУД ---\n" +
                   "• [▶] / [⏸] - Запуск и пауза автокликера.\n" +
                   "• [➖] / [➕] - Свернуть / Развернуть панель.\n" +
                   "• [👁] - Скрыть/показать все метки на экране.\n" +
                   "• [⚙] - Открыть главное меню (настройки, профили, режим записи).\n" +
                   "• [✖] - Закрыть приложение.\n" +
                   "• Громкость ВНИЗ (зажать) - быстро скрыть или показать HUD-панель.\n\n" +
                   "--- БАЗОВЫЕ МЕХАНИКИ ---\n" +
                   "1. Одиночный Клик:\n" +
                   "В главном меню нажмите 'Добавить метку (Клик)'. Появится прицел.\n" +
                   "Нажмите на сам прицел, чтобы открыть его личные настройки. Вы можете настроить задержку до/после клика и длительность удержания.\n\n" +
                   "2. Свайп (Пролистывание):\n" +
                   "Чтобы сделать свайп, добавьте две обычные метки. Зайдите в настройки первой метки (начало свайпа), и внизу в 'Вести к метке №' впишите номер второй метки.\n" +
                   "Вы можете настроить длительность свайпа (в мс). Вторая метка станет концом свайпа.\n\n" +
                   "--- ПРЕДВЫЧИСЛЯЕМЫЕ ТРИГГЕРЫ (УСЛОВИЯ) ---\n" +
                   "В настройках любой метки можно выбрать 'Режим Триггера'. Метка перестает быть глупым кликом и начинает проверять экран, прежде чем сработать или передать очередь дальше.\n\n" +
                   "ЦВЕТНОЙ ПИКСЕЛЬ (Триггер Цвета):\n" +
                   "• Наведите метку на нужное место.\n" +
                   "• В настройках нажмите 'ЗАХВАТИТЬ ЦВЕТ ПОД МЕТКОЙ'.\n" +
                   "• Кликер дойдет до этой метки и будет ждать, пока пиксель не станет нужного цвета (или наоборот, исчезнет, если выбрать оператор '!=').\n" +
                   "• 'Динамическое отслеживание': кликер запомнит цвет в начале и будет ждать, пока он НЕ ИЗМЕНИТСЯ.\n\n" +
                   "ФРАГМЕНТ ИЗОБРАЖЕНИЯ (Поиск картинки):\n" +
                   "• Выберите режим 'Фрагмент Изображения'.\n" +
                   "• Нажмите 'ОПРЕДЕЛИТЬ ЗОНУ ФРАГМЕНТА' - появятся зеленые рамки, выделите ими нужную кнопку или картинку.\n" +
                   "• Нажмите 'ЗАХВАТИТЬ ФРАГМЕНТ'.\n" +
                   "• Теперь метка будет искать этот фрагмент на экране.\n" +
                   "• Можно настроить 'Качество (0.1 - 1.0)'. Снижение качества (например 0.5) ускорит поиск, но немного снизит точность. Используйте это для оптимизации в играх!\n" +
                   "• Точность совпадения % - насколько картинка на экране должна быть похожа на сохраненную.\n\n" +
                   "РАСПОЗНАВАНИЕ ТЕКСТА (OCR):\n" +
                   "• Поиск определенного текста (например 'Победа' или 'Skip').\n" +
                   "• Выделите зону, введите искомый текст и выберите язык.\n" +
                   "• 'Клик по найденному тексту' - если текст будет найден, метка нажмет прямо на него, игнорируя позицию самой метки.\n\n" +
                   "--- ЛОГИКА И МАРШРУТИЗАЦИЯ ---\n" +
                   "По умолчанию метки выполняются по очереди: 1 -> 2 -> 3. Но вы можете это изменить!\n" +
                   "Внизу настроек метки есть блок 'Маршрутизация':\n" +
                   "• 'Перейти к шагу № (при успехе)' - если клик сделан (или триггер сработал), перепрыгнуть на другой шаг.\n" +
                   "• 'Перейти к шагу № (при провале)' - если триггер не нашел цвет/картинку, шаг провален. Можно пустить сценарий по другому пути.\n" +
                   "• 'Ограничение циклов проверок' - сколько раз триггер будет пытаться найти объект. По умолчанию он ждет бесконечно.\n\n" +
                   "--- РЕЖИМ ЗАПИСИ (МАКРОС) ---\n" +
                   "В главном меню переключите 'Режим' на ЗАПИСЬ.\n" +
                   "В HUD панели появится красная кнопка [🔴]. Нажмите её, и начинайте кликать/свайпать по экрану.\n" +
                   "Когда закончите, нажмите [■]. Кликер автоматически создаст все метки с правильными задержками и координатами!"
        }
        scroll.addView(text)
        
        android.app.AlertDialog.Builder(this)
            .setTitle("ПОЛНОЕ РУКОВОДСТВО")
            .setView(scroll)
            .setPositiveButton("ЗАКРЫТЬ", null)
            .show()
    }
}
