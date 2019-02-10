package com.github.isturdy.automaticorders

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.mission.FleetSide
import com.fs.starfarer.api.util.IntervalUtil
import com.github.isturdy.automaticorders.hullmods.OrderSearchAndDestroy

import java.awt.Color;

private val INTERVAL = 1.0f
private val CR_COLOR = Color.YELLOW
private val DAMAGE_COLOR = Color.ORANGE
private val ASSIGNMENT_COLOR = Color.GREEN
private val FRIEND_COLOR = Global.getSettings().getColor("textFriendColor");

private enum class RetreatReason {
    CR,
    DAMAGE,
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

    companion object {
        val LOGGER = Global.getLogger(AutomaticOrdersCombatPlugin::class.java)
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

            val ship = fleetManager.getShipFor(member)
            if (ship.shipAI == null) continue
            if (!ship.isAlive) continue

            if (shipsGivenInitialOrders.add(ship.id)) {
                for (hullMod in ship.variant.hullMods) {
                    if (hullMod == OrderSearchAndDestroy.ID) {
                        orderSearchAndDestroy(ship)
                    }
                }
            }

            if (settings.DEFAULT_CR_RETREAT_THRESHOLD != CrRetreatBehavior.NONE) {
                val maxPpt = ship.mutableStats.peakCRDuration.computeEffective(ship.hullSpec.noCRLossTime)
                val pptRemaining = maxPpt - ship.timeDeployedForCRReduction
                val pptThreshold: Float = when (settings.DEFAULT_CR_RETREAT_THRESHOLD) {
                    CrRetreatBehavior.ZERO_PPT -> 0.0f
                    CrRetreatBehavior.TEN_PERCENT_PPT -> maxPpt / 10.0f
                    CrRetreatBehavior.NONE ->
                        throw Exception("Automatic Orders reached an invalid branch--please report this to the author.")
                }
                LOGGER.debug("Ship: $ship, PPT remaining: $pptRemaining, PPT threshold: $pptThreshold")
                if (pptRemaining < pptThreshold) {
                    orderRetreat(ship, RetreatReason.CR, CR_COLOR)
                }
            }

            if (settings.DEFAULT_DAMAGE_RETREAT_THRESHOLD > 0) {
                if (ship.hullLevel < settings.DEFAULT_DAMAGE_RETREAT_THRESHOLD &&
                    ship.hullLevel < ship.hullLevelAtDeployment
                ) {
                    orderRetreat(ship, RetreatReason.DAMAGE, DAMAGE_COLOR)
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

    private fun orderRetreat(ship: ShipAPI, reason: RetreatReason, color: Color) {
        if (taskManager.getAssignmentFor(ship)?.type == CombatAssignmentType.RETREAT) return
        if (!existingOrders.add(SetKey(ship.id, reason))) return

        val reasonString = when (reason) {
            RetreatReason.CR -> "CR/PPT threshold reached"
            RetreatReason.DAMAGE -> "damage threshold reached"
        }
        val member = fleetManager.getDeployedFleetMember(ship)

        LOGGER.info("Ordering $ship to retreat: $reasonString")
        addMessageForShip(member, "retreating - $reasonString", color)
        taskManager.orderRetreat(member, false, false)
    }

    private fun addMessageForShip(member: DeployedFleetMemberAPI, message: String, color: Color) {
        val name = "${member.member.shipName} (${member.member.hullSpec.hullName}-class): "
        Global.getCombatEngine().combatUI.addMessage(1, member, FRIEND_COLOR, name, color, message)
    }
}