with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'r') as f:
    text = f.read()

target = 'text = "Выполнять параллельно (Не прерывать текущий)"'
rep = 'text = "Исполнять фоном (Не прерывать текущий сценарий)"'
text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/UIManager.kt', 'w') as f:
    f.write(text)
