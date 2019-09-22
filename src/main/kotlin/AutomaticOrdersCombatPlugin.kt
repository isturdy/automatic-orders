package com.github.isturdy.automaticorders

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.combat.CRPluginImpl
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import com.github.isturdy.automaticorders.hullmods.*

import java.awt.Color

private const val INTERVAL = 1.0f
private val CR_COLOR = Color.YELLOW
private val DAMAGE_COLOR = Color.ORANGE
private val ASSIGNMENT_COLOR = Color.GREEN
private val FRIEND_COLOR = Global.getSettings().getColor("textFriendColor")

private enum class RetreatReason {
    CR,
    DAMAGE,
    MISSILES,
}

private data class SetKey(val id: String, val reason: RetreatReason)

class AutomaticOrdersCombatPlugin : BaseEveryFrameCombatPlugin() {
    private var engine: CombatEngineAPI? = null
    private lateinit var fleetManager: CombatFleetManagerAPI
    private lateinit var taskManager: CombatTaskManagerAPI


    private val interval = IntervalUtil(INTERVAL, INTERVAL)
    private val settings = AutomaticOrders.SETTINGS
    private val existingOrders: MutableSet<SetKey> = mutableSetOf()
    private val shipsGivenInitialOrders = mutableSetOf<String>()
    private val shipsGivenInitialAiOrders = mutableSetOf<String>()

    companion object {
        val LOGGER = Global.getLogger(AutomaticOrdersCombatPlugin::class.java)!!
    }

    override fun init(engine: CombatEngineAPI?) {
        if (engine == null) return
        this.engine = engine
        this.fleetManager = engine.getFleetManager(FleetSide.PLAYER)
        this.taskManager = fleetManager.getTaskManager(false)
    }

    override fun advance(amount: Float, events: List<InputEventAPI>?) {
        if (Global.getCurrentState() != GameState.COMBAT) return
        val engine = this.engine
        engine ?: return
        if (engine.isSimulation) return

        interval.advance(amount)
        if (!interval.intervalElapsed()) return

        for (member in fleetManager.deployedCopy) {
            if (member.isFighterWing) continue
            if (member.isAlly) continue
            if (member.hullId in AutomaticOrders.BLACKLIST) continue

            val ship = fleetManager.getShipFor(member)
            if (!ship.isAlive || ship.isStationModule) continue

            val hullMods = ship.variant.hullMods

            if (shipsGivenInitialOrders.add(ship.id)) {
                if (EscortLight().id in hullMods) orderEscort(ship, CombatAssignmentType.LIGHT_ESCORT)
                if (EscortMedium().id in hullMods) orderEscort(ship, CombatAssignmentType.MEDIUM_ESCORT)
                if (EscortHeavy().id in hullMods) orderEscort(ship, CombatAssignmentType.HEAVY_ESCORT)
            }

            if (ship.shipAI == null) continue

            if (shipsGivenInitialAiOrders.add(ship.id)) {
                if (OrderSearchAndDestroy.ID in hullMods) orderSearchAndDestroy(ship)
            }

            val directRetreat = DirectRetreat.ID in hullMods
            if (RetreatNoMissiles.ID in hullMods && outOfMissiles(ship)) {
                orderRetreat(ship, RetreatReason.MISSILES, CR_COLOR, directRetreat)
            }

            val crRetreatThreshold = when {
                RetreatTenPPT().id in hullMods -> CrRetreatBehavior.TEN_PERCENT_PPT
                RetreatZeroPPT().id in hullMods -> CrRetreatBehavior.ZERO_PPT
                RetreatMalfunction().id in hullMods -> CrRetreatBehavior.MALFUNCTION
                RetreatCritical().id in hullMods -> CrRetreatBehavior.CRITICAL_MALFUNCTION
                NoCrRetreat().id in hullMods || NoRetreat().id in hullMods -> CrRetreatBehavior.NONE
                else -> settings.DEFAULT_CR_RETREAT_THRESHOLD.get(ship.hullSize)
            }

            if (crRetreatThreshold != CrRetreatBehavior.NONE) {
                val maxPpt = ship.mutableStats.peakCRDuration.computeEffective(ship.hullSpec.noCRLossTime)
                val pptRemaining = maxPpt - ship.timeDeployedForCRReduction
                val shouldRetreat = when (crRetreatThreshold) {
                    CrRetreatBehavior.TEN_PERCENT_PPT -> pptRemaining < maxPpt * 0.1f
                    CrRetreatBehavior.ZERO_PPT -> pptRemaining <= 0.0f
                    CrRetreatBehavior.MALFUNCTION -> ship.currentCR < CRPluginImpl.MALFUNCTION_START
                    CrRetreatBehavior.CRITICAL_MALFUNCTION -> ship.currentCR < CRPluginImpl.CRITICAL_MALFUNCTION_START
                    CrRetreatBehavior.NONE ->
                        throw Exception("Automatic Orders reached an invalid branch--please report this to the author.")
                }
                if (shouldRetreat) {
                    orderRetreat(ship, RetreatReason.CR, CR_COLOR, directRetreat)
                }
            }

            val damageSetting = settings.DEFAULT_DAMAGE_RETREAT_THRESHOLD.get(ship.hullSize)
            if (damageSetting > 0 && NoRetreat().id !in hullMods) {
                if (ship.hullLevel < damageSetting && ship.hullLevel < ship.hullLevelAtDeployment
                ) {
                    orderRetreat(ship, RetreatReason.DAMAGE, DAMAGE_COLOR, directRetreat)
                }
            }
        }
    }

    private fun orderSearchAndDestroy(ship: ShipAPI) {
        val member = fleetManager.getDeployedFleetMember(ship)
        LOGGER.info("Assigning $ship to search and destroy.")
        addMessageForShip(member, "assigned to search and destroy", ASSIGNMENT_COLOR)
        taskManager.orderSearchAndDestroy(fleetManager.getDeployedFleetMember(ship), false)
    }

    private fun orderEscort(ship: ShipAPI, type: CombatAssignmentType) {
        val member = fleetManager.getDeployedFleetMember(ship)
        val typeString = when (type) {
            CombatAssignmentType.LIGHT_ESCORT -> "light"
            CombatAssignmentType.MEDIUM_ESCORT -> "medium"
            CombatAssignmentType.HEAVY_ESCORT -> "heavy"
            else ->
                throw Exception("Automatic Orders reached an invalid branch--please report this to the author.")
        }
        LOGGER.info("Assigning $ship a $typeString escort.")
        addMessageForShip(member, "assigned a $typeString escort", ASSIGNMENT_COLOR)
        taskManager.createAssignment(type, member as AssignmentTargetAPI, false)
    }

    private fun orderRetreat(ship: ShipAPI, reason: RetreatReason, color: Color, direct: Boolean) {
        if (taskManager.getAssignmentFor(ship)?.type == CombatAssignmentType.RETREAT) return
        if (!existingOrders.add(SetKey(ship.id, reason))) return

        val reasonString = when (reason) {
            RetreatReason.CR -> "CR/PPT threshold reached"
            RetreatReason.DAMAGE -> "damage threshold reached"
            RetreatReason.MISSILES -> "all missiles expended"
        }
        val member = fleetManager.getDeployedFleetMember(ship)

        LOGGER.info("Ordering $ship to retreat: $reasonString")
        addMessageForShip(member, "retreating - $reasonString", color)
        taskManager.orderRetreat(member, false, direct)
    }

    private fun addMessageForShip(member: DeployedFleetMemberAPI, message: String, color: Color) {
        val name = "${member.member.shipName} (${member.member.hullSpec.hullName}-class): "
        Global.getCombatEngine().combatUI.addMessage(1, member, FRIEND_COLOR, name, color, message)
    }

    private fun outOfMissiles(ship: ShipAPI): Boolean {
        if (ship.system.id in AutomaticOrders.MISSILE_REGEN_SYSTEMS && !ship.system.isOutOfAmmo) {
            return true
        }

        var hasMissiles = false
        for (weapon in ship.allWeapons) {
            if (weapon.type == WeaponAPI.WeaponType.MISSILE) {
                val canFire = if (weapon.usesAmmo()) weapon.ammo != 0 else weapon.cooldownRemaining == 0.0f
                if (canFire && !weapon.isPermanentlyDisabled) return false
                hasMissiles = true
            }
        }
        return hasMissiles
    }
}