package com.github.isturdy.automaticorders

import org.apache.log4j.Level
import org.json.JSONObject

enum class CrRetreatBehavior {
    TEN_PERCENT_PPT,
    ZERO_PPT,
    MALFUNCTION,
    CRITICAL_MALFUNCTION,
    NONE,
}

data class Settings(val json: JSONObject) {
    val DEFAULT_CR_RETREAT_THRESHOLD = CrRetreatBehavior.valueOf(json.optString("default_cr_retreat_threshold", "NONE"))
    val DEFAULT_DAMAGE_RETREAT_THRESHOLD = json.optDouble("default_damage_retreat_threshold", 0.5)
    val HULLMOD_OVERRIDES_OFFICER_PERSONALITY = json.optBoolean("hullmod_overrides_officer_personality", false)
    val LOG_LEVEL = Level.toLevel(json.optString("log_level", "WARN"))!!
}