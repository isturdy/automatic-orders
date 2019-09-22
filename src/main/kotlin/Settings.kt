package com.github.isturdy.automaticorders

import com.fs.starfarer.api.combat.ShipAPI
import org.apache.log4j.Level
import org.json.JSONObject

enum class CrRetreatBehavior {
    TEN_PERCENT_PPT,
    ZERO_PPT,
    MALFUNCTION,
    CRITICAL_MALFUNCTION,
    NONE,
}

sealed class ByHullSize<T> {
    abstract fun get(size: ShipAPI.HullSize): T
}
data class Scalar<T>(val value: T): ByHullSize<T>() {
    override fun get(size: ShipAPI.HullSize): T {
        return value
    }
}
class SizeMap<T>(accessor: (ShipAPI.HullSize) -> T): ByHullSize<T>() {
    private val frigate = accessor(ShipAPI.HullSize.FRIGATE)
    private val destroyer = accessor(ShipAPI.HullSize.DESTROYER)
    private val cruiser = accessor(ShipAPI.HullSize.CRUISER)
    private val capital = accessor(ShipAPI.HullSize.CAPITAL_SHIP)

    override fun get(size: ShipAPI.HullSize): T {
        return when(size) {
            ShipAPI.HullSize.FRIGATE -> frigate
            ShipAPI.HullSize.DESTROYER -> destroyer
            ShipAPI.HullSize.CRUISER -> cruiser
            ShipAPI.HullSize.CAPITAL_SHIP -> capital
            else -> frigate
        }
    }
}

fun floatByHullSize(value: Any): ByHullSize<Float> {
    return when (value) {
        is Float -> Scalar(value)
        else -> {
            require(value is JSONObject)
            SizeMap { size -> value.getDouble(size.toString()).toFloat() }
        }
    }
}

fun retreatBehaviorByHullSize(value: Any): ByHullSize<CrRetreatBehavior> {
    return when (value) {
        is String -> Scalar(CrRetreatBehavior.valueOf(value))
        else -> {
            require(value is JSONObject)
            SizeMap { size -> CrRetreatBehavior.valueOf(value.getString(size.toString())) }
        }
    }
}

data class Settings(private val json: JSONObject) {
    val DEFAULT_CR_RETREAT_THRESHOLD = retreatBehaviorByHullSize(json.get("default_cr_retreat_threshold"))
    val DEFAULT_DAMAGE_RETREAT_THRESHOLD = floatByHullSize(json.get("default_damage_retreat_threshold"))
    val HULLMOD_OVERRIDES_OFFICER_PERSONALITY = json.optBoolean("hullmod_overrides_officer_personality", false)
    val LOG_LEVEL = Level.toLevel(json.optString("log_level", "WARN"))!!
}