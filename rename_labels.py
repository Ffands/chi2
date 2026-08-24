with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

# Replace confusing names with user friendly ones
text = text.replace('val items = arrayOf("КЛИК", "ТРИГГЕР", "ВЫЗОВ СКРИПТА", "МЕНЕДЖЕР")', 'val items = arrayOf("🎯 Действие (Клик/Свайп)", "👁 Условие (Поиск)", "⚡ Вызов скрипта", "🔀 Менеджер (Логика)")')

text = text.replace('addSection("Внешний вид", hasViewChanges)', 'addSection("🎨 Внешний вид (Цвет / Размер)", hasViewChanges)')
text = text.replace('addSection("Тайминги", hasTimingChanges)', 'addSection("⏱ Тайминги и Задержки", hasTimingChanges)')
text = text.replace('addSection("Настройки Менеджера", true)', 'addSection("🔀 Настройки Менеджера", true)')
text = text.replace('addSection("Анти-Детект", hasAntiDetect)', 'addSection("🛡 Анти-Детект (Случайности)", hasAntiDetect)')
text = text.replace('addSection("Блок команд (Вызов скрипта)", node.macroProfileName != null)', 'addSection("⚡ Настройки вызова скрипта", node.macroProfileName != null)')
text = text.replace('addSection("Настройки Триггера", hasLogicChanges)', 'addSection("👁 Настройки Поиска (Условие)", hasLogicChanges)')
text = text.replace('addSection("Маршрутизация (Ветвление)", hasRoutingChanges)', 'addSection("🛣 Маршрутизация (Куда идти дальше)", hasRoutingChanges)')
text = text.replace('addSection("Настройки Действий", hasSwipeChanges)', 'addSection("⚙️ Доп. настройки (Свайп / Синхронизация)", hasSwipeChanges)')

# Inside logic section
text = text.replace('text = "Режим работы триггера:"', 'text = "Что именно мы ищем на экране?"')
text = text.replace('arrayOf("Точное совпадение (1 Пиксель)", "Умный Поиск Цвета (Зона)", "OCR (Поиск Текста)")', 'arrayOf("Один пиксель (Точный цвет)", "Зона (Поиск объекта по цвету)", "OCR (Поиск текста/чисел)")')
text = text.replace('text = "Умный OCR: Извлечение чисел и сравнение"', 'text = "Извлечь найденное число и сравнить с:"')

# Timings
text = text.replace('text = "Задержка ПОСЛЕ нажатия (мс):"', 'text = "Задержка ПОСЛЕ выполнения (мс):"')

# Swipe 
text = text.replace('text = "Режим Свайпа:"', 'text = "Сделать свайп (провести пальцем):"')
text = text.replace('arrayOf("Выключен", "Координаты на экране", "До другой метки (Связь)")', 'arrayOf("Выключен (Обычный клик)", "По координатам (Появится маркер конца)", "До другой метки (Указать ID)")')

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Updated Labels")
