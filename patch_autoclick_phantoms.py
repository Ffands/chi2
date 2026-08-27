import re

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

# 1. Update ExecutionFrame
text = text.replace(
    "val repetition: Int\n    )",
    "val repetition: Int,\n        val phantomId: Int?\n    )"
)

# 2. Update ExecutionThread
text = text.replace(
    "var currentScriptNodes: List<TargetNode>? = null\n    ) {",
    "var currentScriptNodes: List<TargetNode>? = null,\n        var phantomId: Int? = null\n    ) {"
)

# 3. Use it in MACRO execution
target_macro = """                                    val newThread = ExecutionThread(
                                        threadId = nextThreadId++,
                                        currentNodeId = nextId,
                                        currentScriptNodes = macroNodes
                                    )"""
rep_macro = """                                    val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                                    val newThread = ExecutionThread(
                                        threadId = nextThreadId++,
                                        currentNodeId = nextId,
                                        currentScriptNodes = macroNodes,
                                        phantomId = pId
                                    )"""
text = text.replace(target_macro, rep_macro)

target_macro_seq = """                                    thread.callStack.push(ExecutionFrame(
                                        thread.currentScriptNodes,
                                        nextNode?.id,
                                        thread.currentRepetition
                                    ))"""
rep_macro_seq = """                                    val pId = if (::uiManager.isInitialized) uiManager.showPhantomNodes(macroNodes) else null
                                    thread.callStack.push(ExecutionFrame(
                                        thread.currentScriptNodes,
                                        nextNode?.id,
                                        thread.currentRepetition,
                                        thread.phantomId
                                    ))
                                    thread.phantomId = pId"""
text = text.replace(target_macro_seq, rep_macro_seq)

# 4. Hide when frame ends
target_pop = """                            val frame = thread.callStack.pop()
                            thread.currentScriptNodes = frame.scriptNodes
                            thread.currentNodeId = frame.returnNodeId
                            thread.currentRepetition = frame.repetition"""
rep_pop = """                            val frame = thread.callStack.pop()
                            thread.phantomId?.let { if (::uiManager.isInitialized) uiManager.hidePhantomNodes(it) }
                            thread.phantomId = frame.phantomId
                            thread.currentScriptNodes = frame.scriptNodes
                            thread.currentNodeId = frame.returnNodeId
                            thread.currentRepetition = frame.repetition"""
text = text.replace(target_pop, rep_pop)

# 5. Hide when thread ends (maxCycles)
target_inactive_cycles = """                        if (node.maxCycles > 0 && thread.currentCycle >= node.maxCycles) {
                            thread.isActive = false
                            continue
                        }"""
rep_inactive_cycles = """                        if (node.maxCycles > 0 && thread.currentCycle >= node.maxCycles) {
                            thread.phantomId?.let { if (::uiManager.isInitialized) uiManager.hidePhantomNodes(it) }
                            thread.phantomId = null
                            thread.isActive = false
                            continue
                        }"""
text = text.replace(target_inactive_cycles, rep_inactive_cycles)

# 6. Hide when thread finishes naturally (null node)
target_null_node = """                    if (node == null) {
                        if (thread.callStack.isNotEmpty()) {
                            val frame = thread.callStack.pop()
                            thread.phantomId?.let { if (::uiManager.isInitialized) uiManager.hidePhantomNodes(it) }
                            thread.phantomId = frame.phantomId
                            thread.currentScriptNodes = frame.scriptNodes
                            thread.currentNodeId = frame.returnNodeId
                            thread.currentRepetition = frame.repetition
                        } else {
                            thread.isActive = false
                        }
                        continue
                    }"""
# Wait, I already modified the pop block inside the null check earlier! Let's just do a regex for the thread.isActive = false inside null check
target_end_thread = """                        } else {
                            thread.isActive = false
                        }
                        continue"""
rep_end_thread = """                        } else {
                            thread.phantomId?.let { if (::uiManager.isInitialized) uiManager.hidePhantomNodes(it) }
                            thread.phantomId = null
                            thread.isActive = false
                        }
                        continue"""
text = text.replace(target_end_thread, rep_end_thread)


# 7. clear all on stop/destroy
text = text.replace(
    "activeThreads.clear()",
    "activeThreads.clear()\n        if (::uiManager.isInitialized) uiManager.removeAllPhantomNodes()"
)

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Updated AutoClickService for phantom views")
