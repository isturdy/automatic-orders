package com.github.isturdy.automaticorders

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.CampaignPlugin
import com.fs.starfarer.api.combat.ShipAIConfig
import com.fs.starfarer.api.combat.ShipAIPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.github.isturdy.automaticorders.hullmods.BasePersonalityOverride
import org.json.JSONObject
import org.apache.log4j.Level

class AutomaticOrders : BaseModPlugin() {
    companion object {
        val LOGGER = Global.getLogger(AutomaticOrders::class.java)!!
        const val SETTINGS_FILE: String = "automatic_orders_settings.json"
        const val BLACKLIST_FILE = "data/config/automatic_orders/ship_blacklist.csv"
        var SETTINGS: Settings = Settings(JSONObject())
        var BLACKLIST = mutableSetOf<String>()
    }

    override fun onApplicationLoad() {
        val settings = Global.getSettings()
        SETTINGS = Settings(settings.loadJSON(SETTINGS_FILE))
        LOGGER.info("Automatic Orders settings: $SETTINGS")
        setLogLevel(SETTINGS.LOG_LEVEL)
        val blacklistArray = settings.getMergedSpreadsheetDataForMod("hullid", BLACKLIST_FILE, "automatic-orders")
        for (i in 0 until blacklistArray.length()) {
            val id = blacklistArray.getJSONObject(i).getString("hullid")
            LOGGER.debug("Blacklisting $id")
            BLACKLIST.add(id)
        }
    }

    override fun pickShipAI(member: FleetMemberAPI?, ship: ShipAPI): PluginPick<ShipAIPlugin>? {
        if (!SETTINGS.HULLMOD_OVERRIDES_OFFICER_PERSONALITY && ship.captain?.isDefault == false && !ship.captain.isPlayer) {
            LOGGER.debug("Skipping personality override checks for $ship--ship has captain ${ship.captain}.")
            return null
        }

        for (hullMod in ship.variant.hullMods) {
            if (!hullMod.startsWith(BasePersonalityOverride.PREFIX)) continue
            val personality = hullMod.removePrefix(BasePersonalityOverride.PREFIX)
            val config = ShipAIConfig()
            config.personalityOverride = personality

            LOGGER.info("Setting personality of $ship to $personality")
            return PluginPick(
                Global.getSettings().createDefaultShipAI(ship, config),
                CampaignPlugin.PickPriority.MOD_SPECIFIC
            )
        }
        return null
    }

    private fun setLogLevel(level: Level) {
        AutomaticOrders.LOGGER.level = level
        AutomaticOrdersCombatPlugin.LOGGER.level = level
    }
}