with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target1 = """                    if (node.macroRunParallel) {
                        val newThread = ExecutionThread(
                            threadId = activeThreads.size + 1,
                            currentNodeId = macroNodes.firstOrNull()?.id,
                            currentScriptNodes = macroNodes
                        )"""
rep1 = """                    if (node.macroRunParallel) {
                        val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                        val newThread = ExecutionThread(
                            threadId = activeThreads.size + 1,
                            currentNodeId = macroNodes.firstOrNull()?.id,
                            currentScriptNodes = macroNodes,
                            phantomId = pId
                        )"""
text = text.replace(target1, rep1)

target2 = """                    } else {
                        val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        thread.callStack.push(ExecutionFrame(thread.currentScriptNodes, nextId, thread.currentRepetition))"""
rep2 = """                    } else {
                        val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                        thread.callStack.push(ExecutionFrame(thread.currentScriptNodes, nextId, thread.currentRepetition, thread.phantomId))
                        thread.phantomId = pId"""
text = text.replace(target2, rep2)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Patched macro phantom properly")
