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
        const val REQ_IMPORT_IMAGE = 1003
        var pendingExportData: String? = null
        var pendingImportData: String? = null
        var pendingImageNodeId: Int? = null
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
            "ACTION_IMPORT_IMAGE" -> {
                pendingImageNodeId = intent.getIntExtra("node_id", intent.getIntExtra("NODE_ID", -1))
                val imgIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                startActivityForResult(imgIntent, REQ_IMPORT_IMAGE)
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
                } else if (requestCode == REQ_IMPORT_IMAGE) {
                    try {
                        val rawBmp = contentResolver.openInputStream(uri)?.use { stream ->
                            android.graphics.BitmapFactory.decodeStream(stream)
                        }
                        val instance = AutoClickService.instance
                        val targetId = pendingImageNodeId
                        if (rawBmp != null && instance != null && targetId != null) {
                            val maxDim = 360
                            val finalBmp = if (rawBmp.width > maxDim || rawBmp.height > maxDim) {
                                val ratio = minOf(maxDim.toFloat() / rawBmp.width, maxDim.toFloat() / rawBmp.height)
                                val sw = maxOf(1, (rawBmp.width * ratio).toInt())
                                val sh = maxOf(1, (rawBmp.height * ratio).toInt())
                                val scaled = android.graphics.Bitmap.createScaledBitmap(rawBmp, sw, sh, true)
                                if (scaled != rawBmp) rawBmp.recycle()
                                scaled
                            } else {
                                rawBmp
                            }
                            val baos = java.io.ByteArrayOutputStream()
                            finalBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                            val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
                            val node = instance.nodes.find { it.id == targetId }
                            if (node != null) {
                                node.invalidateImageCache()
                                node.targetImageBase64 = base64
                                instance.autoSave()
                                instance.uiManager.reopenNodeEditMenuFromExternal(node)
                                android.widget.Toast.makeText(this, "Изображение загружено (${finalBmp.width}x${finalBmp.height})", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            finalBmp.recycle()
                            finish()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        android.widget.Toast.makeText(this, "Ошибка загрузки изображения", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun roundedBg(colorHex: String, strokeHex: String? = null, radiusDp: Int = 14): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeHex != null) {
                setStroke(dp(1), Color.parseColor(strokeHex))
            }
        }
    }

    private lateinit var statusCardView: LinearLayout
    private lateinit var statusBadgeTv: TextView
    private lateinit var statusSubTv: TextView
    private lateinit var startUiBtnView: Button

    override fun onResume() {
        super.onResume()
        refreshServiceStatusUI()
    }

    private fun refreshServiceStatusUI() {
        if (!::statusCardView.isInitialized) return
        val active = AutoClickService.instance != null
        if (active) {
            statusCardView.background = roundedBg("#13281C", "#00E676", 16)
            statusBadgeTv.text = "● СЛУЖБА АКТИВНА"
            statusBadgeTv.setTextColor(Color.parseColor("#00E676"))
            statusSubTv.text = "Автокликер готов к работе. Нажмите «Показать панель HUD», если она скрыта."
            startUiBtnView.visibility = View.VISIBLE
        } else {
            statusCardView.background = roundedBg("#2B1619", "#FF5252", 16)
            statusBadgeTv.text = "● СЛУЖБА ОТКЛЮЧЕНА"
            statusBadgeTv.setTextColor(Color.parseColor("#FF5252"))
            statusSubTv.text = "Включите «UpwellClick» в разделе Специальных возможностей Android."
            startUiBtnView.visibility = View.GONE
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
            setBackgroundColor(Color.parseColor("#0F131A"))
        }
        
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(32))
        }

        val prefs = getSharedPreferences("AutoClickerSettings", Context.MODE_PRIVATE)
        var currentAccentHex = prefs.getString("uiAccentColor", "#00C853") ?: "#00C853"

        // Header (clean without CPS subtitle)
        val title = TextView(this).apply {
            text = "UpwellClick"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(18))
        }
        contentLayout.addView(title)

        // Status Card at the very top
        statusCardView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(16)) }
        }
        statusBadgeTv = TextView(this).apply {
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }
        statusSubTv = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#CFD8DC"))
            setPadding(0, dp(6), 0, dp(14))
        }
        val actionBtnsRow = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val settingsBtn = Button(this).apply {
            text = "⚙ Включить / Настроить в системе"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg(currentAccentHex, null, 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        startUiBtnView = Button(this).apply {
            text = "🚀 Показать панель HUD на экране"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = roundedBg("#00C853", null, 12)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                if (AutoClickService.instance != null) {
                    val sIntent = Intent(this@MainActivity, AutoClickService::class.java)
                    sIntent.action = "SHOW_UI"
                    startService(sIntent)
                    finish()
                }
            }
        }
        actionBtnsRow.addView(settingsBtn)
        actionBtnsRow.addView(startUiBtnView)
        statusCardView.addView(statusBadgeTv)
        statusCardView.addView(statusSubTv)
        statusCardView.addView(actionBtnsRow)
        contentLayout.addView(statusCardView)

        // Mode Selection Card
        val modeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#171C26", "#263238", 16)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(16)) }
        }
        modeLayout.addView(TextView(this).apply {
            text = "🎛 Выберите режим работы"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(4))
        })
        modeLayout.addView(TextView(this).apply {
            text = "Режим можно переключать в любой момент прямо из меню кликера:"
            textSize = 12f
            setTextColor(Color.parseColor("#90A4AE"))
            setPadding(0, 0, 0, dp(10))
        })
        
        val modeGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }
        val modes = listOf(
            "SEQUENTIAL" to "🎯 Простой (Цепочка кликов и свайпов по очереди)",
            "ADVANCED" to "🧠 Умный (Поиск цвета, картинок, OCR текста и логика)",
            "RECORD" to "🎥 Запись жестов (Повторяет ваши нажатия вживую)"
        )
        val savedModeRaw = prefs.getString("AppMode", "ADVANCED") ?: "ADVANCED"
        val currentMode = if (savedModeRaw == "SINGLE") "SEQUENTIAL" else savedModeRaw
        
        for ((modeId, modeDesc) in modes) {
            val rb = RadioButton(this).apply {
                text = modeDesc
                setTextColor(Color.WHITE)
                textSize = 14f
                setPadding(dp(6), dp(10), dp(6), dp(10))
                tag = modeId
                isChecked = modeId == currentMode
            }
            modeGroup.addView(rb)
        }
        modeGroup.setOnCheckedChangeListener { group, checkedId ->
            val rb = group.findViewById<RadioButton>(checkedId)
            val selectedMode = rb.tag.toString()
            prefs.edit().putString("AppMode", selectedMode).apply()
            AutoClickService.instance?.updateAppMode(selectedMode)
        }
        modeLayout.addView(modeGroup)
        contentLayout.addView(modeLayout)

        // Theme / Accent Color Customization Card
        val themeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#171C26", "#263238", 16)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(16)) }
        }
        themeCard.addView(TextView(this).apply {
            text = "🎨 Цветовая тема интерфейса"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(4))
        })
        themeCard.addView(TextView(this).apply {
            text = "Выберите цвет акцентов меню, кнопок и рамок (также доступно в ⚙ Опциях меню):"
            textSize = 12f
            setTextColor(Color.parseColor("#90A4AE"))
            setPadding(0, 0, 0, dp(10))
        })
        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val themePresets = listOf(
            "🟢 Зелёный" to "#00C853",
            "🔵 Синий" to "#2962FF",
            "🟣 Фиолет" to "#7C4DFF",
            "🟠 Оранж" to "#FF6D00",
            "🩵 Кибер" to "#00838F"
        )
        val themeBtns = mutableListOf<Button>()
        for ((idx, pair) in themePresets.withIndex()) {
            val tBtn = Button(this).apply {
                text = pair.first
                textSize = 11f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                isAllCaps = false
                setPadding(dp(4), dp(8), dp(4), dp(8))
                val isSel = currentAccentHex.equals(pair.second, ignoreCase = true)
                background = roundedBg(pair.second, if (isSel) "#FFFFFF" else null, 10)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx < themePresets.size - 1) setMargins(0, 0, dp(4), 0)
                }
                setOnClickListener {
                    currentAccentHex = pair.second
                    prefs.edit().putString("uiAccentColor", pair.second).apply()
                    settingsBtn.background = roundedBg(pair.second, null, 12)
                    themeBtns.forEachIndexed { bIdx, b ->
                        val hex = themePresets[bIdx].second
                        b.background = roundedBg(hex, if (hex.equals(currentAccentHex, ignoreCase = true)) "#FFFFFF" else null, 10)
                    }
                    AutoClickService.instance?.uiManager?.applyThemeColor(pair.second)
                }
            }
            themeBtns.add(tBtn)
            themeRow.addView(tBtn)
        }
        themeCard.addView(themeRow)
        contentLayout.addView(themeCard)

        // Quick 3-step onboarding card for beginners
        val cardGuide = createCard(
            "💡 Быстрый старт за 3 шага",
            "1️⃣ Нажмите [➕] на плавающей панели, чтобы добавить метку на экран.\n" +
            "2️⃣ Перетащите прицел в нужное место (нажатие по прицелу открывает его настройки: тайминги, свайп, удержание или поиск цвета/картинки).\n" +
            "3️⃣ Нажмите [▶] для запуска! Чтобы свернуть панель в одну кнопку — нажмите [◂].",
            Color.parseColor("#171C26")
        )
        contentLayout.addView(cardGuide)

        // Android 13+ helper card
        val cardAndroid13 = createCard(
            "⚠️ Не включается тумблер на Android 13+?",
            "Если система пишет «Ограниченные настройки»:\n" +
            "1. Откройте «Настройки приложения» по кнопке ниже.\n" +
            "2. Нажмите на ⋮ (три точки в углу) ➔ «Разрешить ограниченные настройки».\n" +
            "3. Вернитесь и включите службу UpwellClick.",
            Color.parseColor("#24181C")
        )
        contentLayout.addView(cardAndroid13)

        val appSettingsBtn = Button(this).apply {
            text = "🔓 Открыть настройки приложения (Android 13+)"
            background = roundedBg("#E65100", null, 12)
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(16)) }
            setOnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + packageName)))
            }
        }
        contentLayout.addView(appSettingsBtn)

        val bottomBtnsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val fullGuideBtn = Button(this).apply {
            text = "📖 Подробная справка"
            background = roundedBg("#263238", "#37474F", 12)
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(10), dp(12), dp(10), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            setOnClickListener { showFullGuideDialog() }
        }
        val showTreeBtn = Button(this).apply {
            text = "🌳 Схема сценария"
            background = roundedBg("#263238", "#37474F", 12)
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(10), dp(12), dp(10), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(6), 0, 0, 0)
            }
            setOnClickListener { showScenarioTree() }
        }
        bottomBtnsRow.addView(fullGuideBtn)
        bottomBtnsRow.addView(showTreeBtn)
        contentLayout.addView(bottomBtnsRow)

        refreshServiceStatusUI()

        scroll.addView(contentLayout)
        setContentView(scroll)

        handleIntent(intent)
    }

    private fun showScenarioTree() {
        val scroll = ScrollView(this).apply {
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#181818"))
        }
        
        val treeText = TextView(this).apply {
            textSize = 14f
            setLineSpacing(0f, 1.25f)
            setTextColor(Color.WHITE)
        }
        
        val instance = AutoClickService.instance
        if (instance == null || instance.nodes.isEmpty()) {
            treeText.text = "Сценарий пуст или служба не запущена."
        } else {
            val sb = java.lang.StringBuilder()
            sb.append("🌳 Древо сценария (${instance.nodes.size} шаг.):\n\n")
            for ((idx, node) in instance.nodes.withIndex()) {
                val typeStr = when (node.type) {
                    NodeType.CLICK -> if (node.isSwipe) "🎯 Метка (Свайп ${node.swipeDurationMs} мс)" else "🎯 Метка (Клик ${node.clickDurationMs} мс, x${node.repetitions})"
                    NodeType.CHECK_COLOR -> "👁 Триггер (Проверка условия)"
                    NodeType.MANAGER -> "🔀 Менеджер (${node.managerRoutes.size} усл.)"
                    NodeType.MACRO -> "⚡ Вызов скрипта '${node.macroProfileName ?: "Не выбран"}'"
                }
                sb.append("[$idx] Шаг [${node.id}]: $typeStr\n")
                if (node.isIndependentThread) sb.append("  ↳ Поток: Отдельный параллельный поток\n")
                if (node.skipSequentialExecution) sb.append("  ↳ Режим: Функция (пропуск в линейной очереди)\n")

                if (node.type == NodeType.MANAGER) {
                    for ((rIdx, r) in node.managerRoutes.withIndex()) {
                        sb.append("  ↳ Маршрут #${rIdx + 1}: Если [${r.checkNodeId}] ➔ Идти к [${r.onSuccessGoToId}]\n")
                    }
                    sb.append("  ↳ Иначе (Fallback) -> [${node.nextNodeIdOnFail ?: "Следующая"}]\n\n")
                    continue
                }

                when (node.triggerMode) {
                    -1 -> sb.append("  ↳ Условие: Безусловный\n")
                    0 -> {
                        val cDesc = when {
                            node.dynamicColorUpdate -> "Динамический цвет (при изменении)"
                            node.compareToNodeId != null -> "Сравнение цвета с [${node.compareToNodeId}]"
                            node.colorCompareX != null -> "Сравнение с точкой (${node.colorCompareX}, ${node.colorCompareY})"
                            else -> "Цвет пикселя ${node.colorOperator} (допуск ${node.colorTolerance})"
                        }
                        sb.append("  ↳ Условие: $cDesc\n")
                    }
                    1 -> sb.append("  ↳ Условие: Фрагмент экрана (${node.colorOperator}, порог ${node.imageThreshold.toInt()}%)\n")
                    2 -> {
                        if (node.isSmartOcr) {
                            val cmp = when {
                                node.compareZoneEnabled -> "Область 2"
                                node.ocrCompareToNodeId != null -> "Метка [${node.ocrCompareToNodeId}]"
                                else -> "${node.ocrTargetValue}"
                            }
                            sb.append("  ↳ Условие: Умный OCR (Число ${node.ocrOperator} $cmp, набор '${node.ocrSuffixSetName}')\n")
                        } else if (node.compareZoneEnabled) {
                            sb.append("  ↳ Условие: Сравнение текста Область 1 ${node.colorOperator} Область 2\n")
                        } else {
                            sb.append("  ↳ Условие: Текст '${node.targetText ?: ""}' (${node.colorOperator})\n")
                        }
                    }
                }
                
                if (node.linkedConditionNodeId != null) {
                    sb.append("  ↳ Связка: ${node.linkedConditionOperator} условие Метки [${node.linkedConditionNodeId}]\n")
                }
                
                if (node.nextNodeIdOnSuccess != null) {
                    sb.append("  ↳ При успехе -> [${node.nextNodeIdOnSuccess}]\n")
                } else if (!node.skipSequentialExecution) {
                    sb.append("  ↳ При успехе -> [Следующая по списку]\n")
                }
                
                if (node.triggerMode != -1) {
                    if (node.maxCheckCycles != null && node.maxCheckCycles!! > 0) {
                        sb.append("  ↳ Лимит проверок: ${node.maxCheckCycles} -> При провале: [${node.nextNodeIdOnFail ?: "Следующая"}]\n")
                    } else {
                        sb.append("  ↳ При провале -> [${node.nextNodeIdOnFail ?: "Ждать на [${node.id}] ∞"}]\n")
                    }
                }
                sb.append("\n")
            }
            treeText.text = sb.toString()
        }
        
        scroll.addView(treeText)
        
        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("ДЕРЕВО СЦЕНАРИЯ")
            .setView(scroll)
            .setPositiveButton("ЗАКРЫТЬ", null)
            .show()
    }
    private fun createCard(titleText: String, bodyText: String, bgColor: Int): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.parseColor("#263238"))
            }
            setPadding(dp(18), dp(16), dp(18), dp(16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, dp(16))
            layoutParams = params
        }

        val title = TextView(this).apply {
            text = titleText
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(8))
        }
        container.addView(title)

        val body = TextView(this).apply {
            text = bodyText
            textSize = 13.5f
            setTextColor(Color.parseColor("#CFD8DC"))
            setLineSpacing(0f, 1.25f)
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
