package com.github.isturdy.automaticorders.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

abstract class BaseEscort(level: String) : BaseHullMod() {
    val id = PREFIX + level;

    companion object {
        const val PREFIX = "automatic_orders_escort_"
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        for (hullMod in ship.variant.hullMods) {
            if (hullMod.startsWith(PREFIX) && hullMod != id) return false;
        }
        return true;
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        return "Escort orders are mutually exclusive."
    }
}

class EscortLight : BaseEscort("light")
class EscortMedium : BaseEscort("medium")
class EscortHeavy : BaseEscort("heavy")