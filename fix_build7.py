import re
with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

text = text.replace("checkConditionForNode(node, callback = {", "checkConditionForNode(node) {")
text = text.replace("checkConditionForNode(node, {", "checkConditionForNode(node) {")
text = text.replace("private fun checkConditionForNode(node: TargetNode, callback: (Boolean) -> Unit) {", "private fun checkConditionForNode(node: TargetNode, callback: (Boolean) -> Unit) {")

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
