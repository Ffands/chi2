import re

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target = """            val finalDelay = if (!isMatch && thread.currentNodeId == node.id) {
                val pollDelay = if (node.triggerMode == 2) 300L else 150L
                Math.max(pollDelay, minDelay)
            } else {
                Math.max(minDelay, node.delayAfterMs + randomDelay)
            }"""

rep = """            val finalDelay = if (!isMatch && thread.currentNodeId == node.id) {
                val pollDelay = if (node.triggerMode == 2) 300L else 150L
                Math.max(pollDelay, minDelay)
            } else {
                if (allowExtremeSpeed) 1L else Math.max(minDelay, node.delayAfterMs + randomDelay)
            }"""

text = text.replace(target, rep)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
