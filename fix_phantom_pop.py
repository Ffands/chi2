import re

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

target1 = """            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            thread.isActive = false
            if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Достигнут шаг -1")"""

rep1 = """            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
                thread.phantomId = frame.phantomId
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            thread.isActive = false
            if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
            if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Достигнут шаг -1")"""

text = text.replace(target1, rep1)

target2 = """            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }"""

rep2 = """            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
                thread.phantomId = frame.phantomId
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }"""

# we might hit the first block again, so let's only replace the remaining ones
text = text.replace(target2, rep2)
# actually, target2 matches target1's inner part, so let's just do regex

text = re.sub(
r"""            if \(thread\.callStack\.isNotEmpty\(\)\) \{\s*val frame = thread\.callStack\.pop\(\)\s*thread\.currentNodeId = frame\.returnNodeId\s*thread\.currentScriptNodes = frame\.scriptNodes\s*thread\.currentRepetition = frame\.repetition\s*if \(::uiManager\.isInitialized\) uiManager\.logDebug\("Поток \$\{thread\.threadId\}: Возврат из макроса на шаг \$\{thread\.currentNodeId\}"\)\s*scheduleNextExecution\(thread, 0L\)\s*return\s*\}""",
r"""            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
                thread.phantomId = frame.phantomId
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }""", text)

text = re.sub(
r"""            thread\.isActive = false\s*if \(::uiManager\.isInitialized\) uiManager\.logDebug\("Поток \$\{thread\.threadId\}: Достигнут шаг -1"\)""",
r"""            thread.isActive = false
            if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
            if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Достигнут шаг -1")""", text)

text = re.sub(
r"""                if \(maxCycles != null && maxCycles!! > 0 && thread\.currentCycle >= maxCycles!!\) \{\s*thread\.isActive = false\s*if \(::uiManager\.isInitialized\) uiManager\.logDebug\("Поток \$\{thread\.threadId\}: Достигнут лимит циклов \$\{maxCycles\}"\)""",
r"""                if (maxCycles != null && maxCycles!! > 0 && thread.currentCycle >= maxCycles!!) {
                    thread.isActive = false
                    if (thread.phantomId != null && ::uiManager.isInitialized) uiManager.hidePhantomNodes(thread.phantomId!!)
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Достигнут лимит циклов ${maxCycles}")""", text)


with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)

