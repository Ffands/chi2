with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'r') as f:
    text = f.read()

# 1. Add ExecutionFrame and modify ExecutionThread
target_thread = """    data class ExecutionThread(
        val threadId: Int,
        var currentNodeId: Int?,
        var currentCheckCycle: Int = 0,
        var currentRepetition: Int = 0,
        var currentCycle: Int = 0,
        var isWaiting: Boolean = false,
        var isActive: Boolean = true
    ) {
        val returnStack = java.util.Stack<Int>()
    }"""
rep_thread = """    data class ExecutionFrame(
        val scriptNodes: List<TargetNode>?,
        val returnNodeId: Int?,
        val repetition: Int
    )

    data class ExecutionThread(
        val threadId: Int,
        var currentNodeId: Int?,
        var currentCheckCycle: Int = 0,
        var currentRepetition: Int = 0,
        var currentCycle: Int = 0,
        var isWaiting: Boolean = false,
        var isActive: Boolean = true,
        var currentScriptNodes: List<TargetNode>? = null
    ) {
        val callStack = java.util.Stack<ExecutionFrame>()
    }"""
text = text.replace(target_thread, rep_thread)

# 2. Add parseProfileNodes
func_parse = """    private fun parseProfileNodes(profileName: String): List<TargetNode>? {
        val file = java.io.File(getExternalFilesDir(null), "profiles/$profileName.json")
        if (!file.exists()) return null
        try {
            val json = file.readText()
            val jsonObject = org.json.JSONObject(json)
            val nodesArray = jsonObject.getJSONArray("nodes")
            val parsedNodes = mutableListOf<TargetNode>()
            for (i in 0 until nodesArray.length()) {
                parsedNodes.add(TargetNode.fromJson(nodesArray.getJSONObject(i)))
            }
            return parsedNodes
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun evaluateManagerRoutes"""
text = text.replace("    private fun evaluateManagerRoutes", func_parse)

# 3. Patch getNextNodeLinear
target_getnext = """    private fun getNextNodeLinear(currentId: Int): Int? {
        val index = nodes.indexOfFirst { it.id == currentId }
        var nextIndex = index + 1
        val nSize = nodes.size
        
        while (nextIndex < nSize) {
            val n = nodes[nextIndex]"""
rep_getnext = """    private fun getNextNodeLinear(currentId: Int, nodesList: List<TargetNode> = nodes): Int? {
        val index = nodesList.indexOfFirst { it.id == currentId }
        var nextIndex = index + 1
        val nSize = nodesList.size
        
        while (nextIndex < nSize) {
            val n = nodesList[nextIndex]"""
text = text.replace(target_getnext, rep_getnext)

# 4. Patch executeThread - Part 1: returnStack -> callStack and nodes -> currentNodes
target_exec1 = """        if (thread.currentNodeId == -1) {
            if (thread.returnStack.isNotEmpty()) {
                thread.currentNodeId = thread.returnStack.pop()
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }"""
rep_exec1 = """        if (thread.currentNodeId == -1) {
            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }"""
text = text.replace(target_exec1, rep_exec1)

target_exec2 = """        val node = nodes.find { it.id == thread.currentNodeId }
        
        if (node == null) {
            if (thread.returnStack.isNotEmpty()) {
                thread.currentNodeId = thread.returnStack.pop()
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            
            thread.currentNodeId = nodes.firstOrNull { !it.skipSequentialExecution }?.id"""
rep_exec2 = """        val currentNodes = thread.currentScriptNodes ?: this.nodes
        val node = currentNodes.find { it.id == thread.currentNodeId }
        
        if (node == null) {
            if (thread.callStack.isNotEmpty()) {
                val frame = thread.callStack.pop()
                thread.currentNodeId = frame.returnNodeId
                thread.currentScriptNodes = frame.scriptNodes
                thread.currentRepetition = frame.repetition
                if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId}: Возврат из макроса на шаг ${thread.currentNodeId}")
                scheduleNextExecution(thread, 0L)
                return
            }
            
            thread.currentNodeId = currentNodes.firstOrNull { !it.skipSequentialExecution }?.id"""
text = text.replace(target_exec2, rep_exec2)

# 5. Patch "nodes.find" in executeThread scope to use currentNodes
target_blackhole = """            if (nodes.find { it.id == node.id } == null) {
                thread.isActive = false
                checkAllThreadsStopped()
                return@checkConditionForNode
            }"""
rep_blackhole = """            val currentNodesList = thread.currentScriptNodes ?: this.nodes
            if (currentNodesList.find { it.id == node.id } == null) {
                thread.isActive = false
                checkAllThreadsStopped()
                return@checkConditionForNode
            }"""
text = text.replace(target_blackhole, rep_blackhole)

target_sync = """                            for (id in node.syncWithNodeIds) {
                                val syncNode = nodes.find { it.id == id }"""
rep_sync = """                            for (id in node.syncWithNodeIds) {
                                val currentNodesList = thread.currentScriptNodes ?: this.nodes
                                val syncNode = currentNodesList.find { it.id == id }"""
text = text.replace(target_sync, rep_sync)

# 6. Patch NodeType.MACRO block
target_macro = """                } else if (node.type == NodeType.MACRO && !node.macroProfileName.isNullOrEmpty()) {
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Запуск макроса ${node.macroProfileName} (Параллельно: ${node.macroRunParallel})")
                    val oldMaxId = if (nodes.isNotEmpty()) nodes.maxOf { it.id } else 0
                    val offset = oldMaxId + 1
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        loadProfile(node.macroProfileName!!, append = true)
                        
                        if (node.macroRunParallel) {
                            val newThread = ExecutionThread(
                                threadId = activeThreads.size + 1,
                                currentNodeId = offset
                            )
                            activeThreads.add(newThread)
                            executeThread(newThread)
                            
                            thread.currentRepetition = 0
                            thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)
                            scheduleNextExecution(thread, node.delayAfterMs)
                        } else {
                            val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)
                            if (nextId != -1) {
                                thread.returnStack.push(nextId)
                            }
                            thread.currentRepetition = 0
                            thread.currentNodeId = offset
                            scheduleNextExecution(thread, node.delayAfterMs)
                        }
                        
                        uiManager.recreateFloatingControlBar()
                    }
                    return@checkConditionForNode // Wait for the async load"""
rep_macro = """                } else if (node.type == NodeType.MACRO && !node.macroProfileName.isNullOrEmpty()) {
                    if (::uiManager.isInitialized) uiManager.logDebug("Поток ${thread.threadId} Шаг ${node.id}: Запуск блока команд ${node.macroProfileName} (Параллельно: ${node.macroRunParallel})")
                    val currentNodesList = thread.currentScriptNodes ?: this.nodes
                    val macroNodes = parseProfileNodes(node.macroProfileName!!)
                    
                    if (macroNodes.isNullOrEmpty()) {
                        if (::uiManager.isInitialized) uiManager.logDebug("Ошибка: Не удалось загрузить блок команд ${node.macroProfileName}")
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        scheduleNextExecution(thread, node.delayAfterMs)
                        return@checkConditionForNode
                    }
                    
                    if (node.macroRunParallel) {
                        val newThread = ExecutionThread(
                            threadId = activeThreads.size + 1,
                            currentNodeId = macroNodes.firstOrNull()?.id,
                            currentScriptNodes = macroNodes
                        )
                        activeThreads.add(newThread)
                        executeThread(newThread)
                        
                        thread.currentRepetition = 0
                        thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        scheduleNextExecution(thread, node.delayAfterMs)
                    } else {
                        val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, currentNodesList)
                        thread.callStack.push(ExecutionFrame(thread.currentScriptNodes, nextId, thread.currentRepetition))
                        
                        thread.currentScriptNodes = macroNodes
                        thread.currentRepetition = 0
                        thread.currentNodeId = macroNodes.firstOrNull()?.id
                        scheduleNextExecution(thread, node.delayAfterMs)
                    }
                    return@checkConditionForNode"""
text = text.replace(target_macro, rep_macro)

# 7. Update evaluateManagerRoutes to use thread's nodes
target_mgr = """        val route = routes[index]
        val nodeToCheck = nodes.find { it.id == route.checkNodeId }
        if (nodeToCheck == null || !nodeHasCondition(nodeToCheck)) {"""
rep_mgr = """        val route = routes[index]
        val currentNodesList = thread.currentScriptNodes ?: this.nodes
        val nodeToCheck = currentNodesList.find { it.id == route.checkNodeId }
        if (nodeToCheck == null || !nodeHasCondition(nodeToCheck)) {"""
text = text.replace(target_mgr, rep_mgr)

target_mgr_next1 = """            thread.currentNodeId = managerNode.nextNodeIdOnFail ?: getNextNodeLinear(managerNode.id)"""
rep_mgr_next1 = """            val currentNodesList = thread.currentScriptNodes ?: this.nodes
            thread.currentNodeId = managerNode.nextNodeIdOnFail ?: getNextNodeLinear(managerNode.id, currentNodesList)"""
text = text.replace(target_mgr_next1, rep_mgr_next1)

# 8. Update performGestureForNodes to take currentNodes and use them for swipe targets
target_perf = """    private fun performGestureForNodes(activeNodes: List<TargetNode>) {"""
rep_perf = """    private fun performGestureForNodes(activeNodes: List<TargetNode>, contextNodes: List<TargetNode>? = null) {"""
text = text.replace(target_perf, rep_perf)

text = text.replace("""val tgtNode = nodes.find { it.id == node.swipeTargetNodeId }""", """val tgtNode = (contextNodes ?: nodes).find { it.id == node.swipeTargetNodeId }""")
text = text.replace("""performGestureForNodes(activeNodes)""", """performGestureForNodes(activeNodes, thread.currentScriptNodes)""")

# 9. Update checkConditionForNode to take contextNodes and use them
target_cond = """    private fun checkConditionForNode(node: TargetNode, callback: (Boolean) -> Unit) {"""
rep_cond = """    private fun checkConditionForNode(node: TargetNode, contextNodes: List<TargetNode>? = null, callback: (Boolean) -> Unit) {"""
text = text.replace(target_cond, rep_cond)

text = text.replace("""val linkedNode = nodes.find { it.id == node.linkedConditionNodeId }""", """val linkedNode = (contextNodes ?: nodes).find { it.id == node.linkedConditionNodeId }""")
text = text.replace("""val otherNode = nodes.find { it.id == node.ocrCompareToNodeId }""", """val otherNode = (contextNodes ?: nodes).find { it.id == node.ocrCompareToNodeId }""")
text = text.replace("""val otherNode = nodes.find { it.id == node.compareToNodeId }""", """val otherNode = (contextNodes ?: nodes).find { it.id == node.compareToNodeId }""")

text = text.replace("""checkConditionForNode(nodeToCheck) { isMatch ->""", """checkConditionForNode(nodeToCheck, thread.currentScriptNodes) { isMatch ->""")
text = text.replace("""checkConditionForNode(node) { isMatch ->""", """checkConditionForNode(node, currentNodes) { isMatch ->""")

# 10. Update getNextNodeLinear usages in executeThread
text = text.replace("""thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)""", """thread.currentNodeId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, thread.currentScriptNodes ?: this.nodes)""")
text = text.replace("""thread.currentNodeId = node.nextNodeIdOnFail ?: getNextNodeLinear(node.id)""", """thread.currentNodeId = node.nextNodeIdOnFail ?: getNextNodeLinear(node.id, thread.currentScriptNodes ?: this.nodes)""")
text = text.replace("""val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id)""", """val nextId = node.nextNodeIdOnSuccess ?: getNextNodeLinear(node.id, thread.currentScriptNodes ?: this.nodes)""")

with open('app/src/main/java/com/example/autoclicker/AutoClickService.kt', 'w') as f:
    f.write(text)
print("Patched Macro Isolation in AutoClickService")
