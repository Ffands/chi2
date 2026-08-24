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


target2 = """                        val nextNode = getNextNodeLinearOrLinked(node, currentNodesList)
                        thread.callStack.push(ExecutionFrame(
                            thread.currentScriptNodes,
                            nextNode?.id,
                            thread.currentRepetition,
                            thread.phantomId
                        ))"""
# Oh wait, my previous python script might have failed entirely on the sequential block too! Let's check what is in the file.
