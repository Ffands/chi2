package com.example.autoclicker

data class TargetNode(
    val id: String,
    var x: Int,
    var y: Int,
    var clickDurationMs: Long = 20L,
    var delayAfterMs: Long = 100L,
    var repeatCount: Int = 1,
    var isSwipe: Boolean = false,
    var swipeEndX: Int = 0,
    var swipeEndY: Int = 0,
    var swipeTargetNodeId: String? = null,
    var randomizeRadius: Int = 0,
    var stopOnSuccess: Boolean = false,
    var textCondition: String? = null,
    var textConditionExact: Boolean = false,
    var nextNodeIdOnSuccess: String? = null,
    var nextNodeIdOnFail: String? = null,
    var macroScriptId: String? = null,
    var loopUntilTextAppears: Boolean = false,
    var ocrRegionLeft: Int = 0,
    var ocrRegionTop: Int = 0,
    var ocrRegionRight: Int = 0,
    var ocrRegionBottom: Int = 0
)

data class ScriptProfile(
    val id: String,
    val name: String,
    val nodes: List<TargetNode>,
    val loopCount: Int = 1,
    val isInfinite: Boolean = true,
    val timeLimitSec: Long = 0L,
    val randomizeRadius: Int = 0,
    val antiDetectJitter: Boolean = false
)

enum class ExecutionState {
    STOPPED,
    RUNNING,
    PAUSED
}
