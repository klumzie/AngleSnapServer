package me.contaria.anglesnapserver;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AngleSnapServer implements ModInitializer {

    public static final String MOD_ID = "anglesnapserver";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing AngleSnapServer...");

        // Register arrow tracker for player join/disconnect
        ArrowTracker tracker = new ArrowTracker();
        tracker.register();

        LOGGER.info("AngleSnapServer loaded!");
    }
}
