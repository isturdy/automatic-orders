package com.github.isturdy.automaticorders.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI


abstract class BasePersonalityOverride(personality: String) : BaseHullMod() {
    private val id = BasePersonalityOverride.PREFIX + personality;

    companion object {
        const val PREFIX = "automatic_orders_personality_"
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        for (hullMod in ship.variant.hullMods) {
            if (hullMod.startsWith(BasePersonalityOverride.PREFIX) && !hullMod.equals(id)) return false;
        }
        return true;
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        return "Personality overrides are mutually exclusive."
    }
}

class PersonalityOverrideReckless : BasePersonalityOverride("reckless")
class PersonalityOverrideAggressive : BasePersonalityOverride("aggressive")
class PersonalityOverrideSteady : BasePersonalityOverride("steady")
class PersonalityOverrideCautious : BasePersonalityOverride("cautious")
class PersonalityOverrideTimid : BasePersonalityOverride("timid")