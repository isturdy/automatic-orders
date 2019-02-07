package com.github.isturdy.automaticorders

import org.apache.log4j.Level
import org.json.JSONObject

enum class CrRetreatBehavior {
    ZERO_PPT,
    TEN_PERCENT_PPT,
    NONE,
}

data class Settings(val json: JSONObject) {
    val DEFAULT_CR_RETREAT_THRESHOLD: CrRetreatBehavior
    val DEFAULT_DAMAGE_RETREAT_THRESHOLD: Double
    val LOG_LEVEL: Level

    init {
        DEFAULT_CR_RETREAT_THRESHOLD = CrRetreatBehavior.valueOf(json.optString("default_cr_retreat_threshold", "NONE"))
        DEFAULT_DAMAGE_RETREAT_THRESHOLD = json.optDouble("default_damage_retreat_threshold", 0.5)
        LOG_LEVEL = Level.toLevel(json.optString("log_level", "WARN"))
    }
}