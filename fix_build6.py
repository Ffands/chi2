import re
with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

text = text.replace("contextNodes ?: nodes", "nodes")
text = text.replace("contextNodes: List<TargetNode>? = null", "")

# Fix the method signature of checkConditionForNode
text = text.replace("private fun checkConditionForNode(node: TargetNode, , callback: (Boolean) -> Unit)", "private fun checkConditionForNode(node: TargetNode, callback: (Boolean) -> Unit)")
text = text.replace("private fun performGestureForNodes(activeNodes: List<TargetNode>, )", "private fun performGestureForNodes(activeNodes: List<TargetNode>)")

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
