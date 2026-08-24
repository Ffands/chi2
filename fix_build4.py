with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

# Fix getNextNodeLinear signature where there were parameters
import re
text = re.sub(r'getNextNodeLinear\(node\.id,\s*currentNodesList\)', 'getNextNodeLinear(node.id)', text)

# Just to be sure about contextNodes
text = text.replace('this.contextNodes', 'this.nodes')
text = text.replace('contextNodes.', 'nodes.')

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
