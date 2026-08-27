with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

text = text.replace('val items = arrayOf("КЛИК", "ТРИГГЕР", "МАКРОС", "МЕНЕДЖЕР")', 'val items = arrayOf("КЛИК", "ТРИГГЕР", "ВЫЗОВ СКРИПТА", "МЕНЕДЖЕР")')
text = text.replace('val macroSection = addSection("Настройки Макроса", node.macroProfileName != null) { body ->', 'val macroSection = addSection("Блок команд (Вызов скрипта)", node.macroProfileName != null) { body ->')
text = text.replace('val macroTitle = TextView(service).apply {\n                text = "Имя профиля макроса (сохраненного сценария):"', 'val macroTitle = TextView(service).apply {\n                text = "Имя сохраненного профиля для вызова:"')

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
print("Patched UI strings")
