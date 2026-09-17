package com.example.autoclicker

object Limits {
    // Parasitic limits that cut CPS down
    var CYCLE_END_DELAY = 100L
    var CHECK_COLOR_DELAY = 150L
    var CHECK_IMAGE_DELAY = 300L
    var MIN_CLICK_DURATION = 10L
    var MIN_SWIPE_DURATION = 30L

    fun getCycleEndDelay(allowExtremeSpeed: Boolean): Long {
        return if (allowExtremeSpeed) 0L else CYCLE_END_DELAY
    }

    fun getCheckDelay(allowExtremeSpeed: Boolean, isImageMatch: Boolean): Long {
        return if (allowExtremeSpeed) 0L else (if (isImageMatch) CHECK_IMAGE_DELAY else CHECK_COLOR_DELAY)
    }

    fun getClickDuration(allowExtremeSpeed: Boolean, requestedDuration: Long): Long {
        return if (allowExtremeSpeed) Math.max(1L, requestedDuration) else Math.max(MIN_CLICK_DURATION, requestedDuration)
    }

    fun getSwipeDuration(allowExtremeSpeed: Boolean, requestedDuration: Long): Long {
        return if (allowExtremeSpeed) Math.max(1L, requestedDuration) else Math.max(MIN_SWIPE_DURATION, requestedDuration)
    }
}
