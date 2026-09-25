package com.example.autoclicker

object Limits {
    var CYCLE_END_DELAY = 0L
    var CHECK_COLOR_DELAY = 150L
    var CHECK_IMAGE_DELAY = 300L
    var MIN_CLICK_DURATION = 1L
    var MIN_SWIPE_DURATION = 10L

    fun getCycleEndDelay(allowExtremeSpeed: Boolean): Long {
        return if (allowExtremeSpeed) 0L else CYCLE_END_DELAY
    }

    fun getCheckDelay(allowExtremeSpeed: Boolean, isImageMatch: Boolean): Long {
        return if (allowExtremeSpeed) 0L else (if (isImageMatch) CHECK_IMAGE_DELAY else CHECK_COLOR_DELAY)
    }

    fun getClickDuration(allowExtremeSpeed: Boolean, requestedDuration: Long): Long {
        return Math.max(1L, requestedDuration)
    }

    fun getSwipeDuration(allowExtremeSpeed: Boolean, requestedDuration: Long): Long {
        return if (allowExtremeSpeed) Math.max(1L, requestedDuration) else Math.max(MIN_SWIPE_DURATION, requestedDuration)
    }
}
