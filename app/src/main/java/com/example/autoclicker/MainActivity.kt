package com.example.autoclicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnAccessibility: Button
    private lateinit var btnOverlay: Button
    private lateinit var btnExportProfiles: Button
    private lateinit var btnImportProfiles: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
            setBackgroundColor(0xFF121214.toInt())
        }

        val title = TextView(this).apply {
            text = "⚡ UpwellClick v3"
            textSize = 24f
            setTextColor(0xFF00E5FF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 24)
        }
        rootLayout.addView(title)

        statusText = TextView(this).apply {
            text = "Проверка разрешений..."
            textSize = 15f
            setTextColor(0xFFE4E4E7.toInt())
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(statusText)

        btnAccessibility = Button(this).apply {
            text = "1. Включить службу спец. возможностей"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        rootLayout.addView(btnAccessibility)

        val spacer1 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 24
            )
        }
        rootLayout.addView(spacer1)

        btnOverlay = Button(this).apply {
            text = "2. Разрешить показ поверх других окон"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
            }
        }
        rootLayout.addView(btnOverlay)

        val spacer2 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 36
            )
        }
        rootLayout.addView(spacer2)

        btnToggleService = Button(this).apply {
            text = "▶ Запустить плавающую панель"
            setBackgroundColor(0xFF00E5FF.toInt())
            setTextColor(0xFF09090B.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
            setOnClickListener {
                val service = AutoClickService.instance
                if (service != null) {
                    service.toggleServiceUI(true)
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Сначала включите службу в Спец. возможностях!",
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
        rootLayout.addView(btnToggleService)

        val spacer3 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 48
            )
        }
        rootLayout.addView(spacer3)

        val profileSectionTitle = TextView(this).apply {
            text = "💾 Профили и сценарии"
            textSize = 17f
            setTextColor(0xFFA1A1AA.toInt())
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(profileSectionTitle)

        btnExportProfiles = Button(this).apply {
            text = "📤 Экспортировать профили"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFE4E4E7.toInt())
            setOnClickListener { exportProfiles() }
        }
        rootLayout.addView(btnExportProfiles)

        val spacer4 = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16
            )
        }
        rootLayout.addView(spacer4)

        btnImportProfiles = Button(this).apply {
            text = "📥 Импортировать профили"
            setBackgroundColor(0xFF27272A.toInt())
            setTextColor(0xFFE4E4E7.toInt())
            setOnClickListener { importProfiles() }
        }
        rootLayout.addView(btnImportProfiles)

        setContentView(rootLayout)
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }

        val isServiceRunning = AutoClickService.instance != null

        val status = StringBuilder()
        status.append("• Служба доступности: ")
        status.append(if (isServiceRunning) "✅ АКТИВНА\n" else "❌ ВЫКЛЮЧЕНА\n")
        status.append("• Показ поверх окон: ")
        status.append(if (hasOverlay) "✅ РАЗРЕШЕНО\n" else "❌ ЗАПРЕЩЕНО\n")

        statusText.text = status.toString()

        if (isServiceRunning && hasOverlay) {
            btnToggleService.isEnabled = true
            btnToggleService.alpha = 1.0f
        } else {
            btnToggleService.isEnabled = true
            btnToggleService.alpha = 0.85f
        }
    }

    private fun exportProfiles() {
        try {
            val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE)
            val allData = prefs.all
            val jsonBuilder = StringBuilder("{")
            var first = true
            for ((key, value) in allData) {
                if (!first) jsonBuilder.append(",")
                first = false
                jsonBuilder.append("\"").append(key).append("\":\"")
                jsonBuilder.append(value.toString().replace("\"", "\\\"")).append("\"")
            }
            jsonBuilder.append("}")

            val exportFile = File(cacheDir, "upwell_click_profiles.json")
            exportFile.writeText(jsonBuilder.toString())

            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                exportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Экспорт профилей UpwellClick"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val IMPORT_REQUEST_CODE = 101

    private fun importProfiles() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Выберите файл JSON"), IMPORT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val uri = data.data ?: return
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    val prefs = getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE).edit()
                    // basic key-value populate
                    if (content.startsWith("{") && content.endsWith("}")) {
                        // stored profile strings
                        prefs.putString("saved_profiles_data", content)
                        prefs.apply()
                        Toast.makeText(this, "Профили успешно импортированы!", Toast.LENGTH_SHORT).show()
                        AutoClickService.instance?.loadSavedProfiles()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
