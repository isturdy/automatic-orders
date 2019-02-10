package com.github.isturdy.automaticorders.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

const val PERSONALITY_PREFIX = "automatic_orders_personality_"

abstract class BasePersonalityOverride(private val personality: String) : BaseHullMod() {
    private val hullModId = PERSONALITY_PREFIX + personality;

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        for (hullMod in ship.variant.hullMods) {
            if (hullMod.startsWith(PERSONALITY_PREFIX) && !hullMod.equals(hullModId)) return false;
        }
        return true;
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        return "Personality overrides are mutually exclusive."
    }
}