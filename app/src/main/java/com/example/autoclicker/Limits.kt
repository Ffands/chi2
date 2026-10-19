package com.example.autoclicker

object Limits {
    var CYCLE_END_DELAY = 0L
    var CHECK_COLOR_DELAY = 150L
    var CHECK_IMAGE_DELAY = 300L
    var MIN_STEP_DELAY = 3L
    var MIN_CLICK_DURATION = 1L
    var MIN_SWIPE_DURATION = 10L

    fun getStepDelay(allowExtremeSpeed: Boolean, requestedDelay: Long): Long {
        return if (allowExtremeSpeed) Math.max(0L, requestedDelay) else Math.max(MIN_STEP_DELAY, requestedDelay)
    }

    fun getCycleEndDelay(allowExtremeSpeed: Boolean, requestedDelay: Long = CYCLE_END_DELAY): Long {
        return Math.max(0L, requestedDelay)
    }

    fun getCheckDelay(allowExtremeSpeed: Boolean, isHeavyCheck: Boolean, requestedDelay: Long = 0L): Long {
        val minCheck = if (isHeavyCheck) CHECK_IMAGE_DELAY else CHECK_COLOR_DELAY
        return if (allowExtremeSpeed) Math.max(0L, requestedDelay) else Math.max(minCheck, requestedDelay)
    }

    fun getClickDuration(allowExtremeSpeed: Boolean, requestedDuration: Long): Long {
        return if (allowExtremeSpeed) Math.max(1L, requestedDuration) else Math.max(MIN_CLICK_DURATION, requestedDuration)
    }

    fun getSwipeDuration(allowExtremeSpeed: Boolean, requestedDuration: Long): Long {
        return if (allowExtremeSpeed) Math.max(1L, requestedDuration) else Math.max(MIN_SWIPE_DURATION, requestedDuration)
    }
}

