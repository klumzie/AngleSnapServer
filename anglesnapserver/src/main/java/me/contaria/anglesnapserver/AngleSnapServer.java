package me.contaria.anglesnapserver;

public class AngleSnapServer implements net.fabricmc.api.DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        new ArrowTracker();
    }
}
