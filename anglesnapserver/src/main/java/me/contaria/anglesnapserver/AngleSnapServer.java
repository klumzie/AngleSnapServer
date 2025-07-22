package me.contaria.anglesnapserver;

import net.fabricmc.api.ModInitializer;

public class AngleSnapServer implements ModInitializer {
    @Override
    public void onInitialize() {
        new ArrowTracker(); // Initialize arrow tracking
    }
}
