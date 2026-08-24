import re
with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

text = re.sub(r'checkConditionForNode\(([^,]+),\s*[^)]+\)\s*\{', r'checkConditionForNode(\1) {', text)
text = re.sub(r'performGestureForNodes\(([^,]+),\s*[^)]+\)', r'performGestureForNodes(\1)', text)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
