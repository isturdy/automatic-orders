package com.github.isturdy.automaticorders

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.thoughtworks.xstream.XStream
import org.json.JSONObject
import org.apache.log4j.Level

class AutomaticOrders : BaseModPlugin() {
    companion object {
        val LOGGER = Global.getLogger(AutomaticOrders::class.java)
        val SETTINGS_FILE: String = "automatic_orders_settings.json"
        var SETTINGS: Settings = Settings(JSONObject())
    }

    override fun onApplicationLoad() {
        SETTINGS = Settings(Global.getSettings().loadJSON(SETTINGS_FILE));
        LOGGER.info("Automatic Orders settings: $SETTINGS")
        setLogLevel(SETTINGS.LOG_LEVEL)
    }

    private fun setLogLevel(level: Level) {
        AutomaticOrdersCombatPlugin.LOGGER.level = level
    }
}