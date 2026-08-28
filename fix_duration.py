import re

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target1 = """                val stroke = GestureDescription.StrokeDescription(path, 0, if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)"""
rep1 = """                val duration = if (allowExtremeSpeed) 1L else (if (node.isSwipe) node.swipeDurationMs else node.clickDurationMs)
                val stroke = GestureDescription.StrokeDescription(path, 0, duration)"""

text = text.replace(target1, rep1)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
