with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target = """                            if (id != null) {
                                val syncNode = nodes.find { it.id == id }"""
rep = """                            if (id != null) {
                                val currentNodesList = thread.currentScriptNodes ?: this.nodes
                                val syncNode = currentNodesList.find { it.id == id }"""

text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Fixed sync nodes.find")
