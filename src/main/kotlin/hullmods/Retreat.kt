package com.github.isturdy.automaticorders.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.ShipAPI

class DirectRetreat : BaseHullMod() {
    companion object {
        const val ID = "automatic_orders_direct_retreat"
    }
}

class RetreatNoMissiles : BaseHullMod() {
    companion object {
        const val ID = "automatic_orders_retreat_no_missiles"
    }
}

abstract class BaseRetreatThreshold(val id: String) : BaseHullMod() {
    companion object {
        val IDS = setOf(TenPPT().id, ZeroPPT().id, RetreatMalfunction().id, RetreatCritical().id, NoCrRetreat().id, NoRetreat().id)
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        for (hullMod in ship.variant.hullMods) {
            if (hullMod in IDS && hullMod != id) return false
        }
        return true
    }

    override fun getUnapplicableReason(ship: ShipAPI?): String {
        return "Personality overrides are mutually exclusive."
    }
}

class TenPPT : BaseRetreatThreshold("automatic_orders_retreat_ten_ppt")
class ZeroPPT : BaseRetreatThreshold("automatic_orders_retreat_zero_ppt")
class RetreatMalfunction : BaseRetreatThreshold("automatic_orders_retreat_malfunction")
class RetreatCritical : BaseRetreatThreshold("automatic_orders_retreat_critical")
class NoCrRetreat : BaseRetreatThreshold("automatic_orders_no_cr_retreat")
class NoRetreat : BaseRetreatThreshold("automatic_orders_no_retreat")