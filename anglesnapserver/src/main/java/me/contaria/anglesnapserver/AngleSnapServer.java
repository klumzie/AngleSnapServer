package me.contaria.anglesnapserver;

public class AngleSnapServer implements net.fabricmc.api.DedicatedServerModInitializer {
@Override
public void onInitialize() {
    new ArrowTracker(); // Register arrow saving logic
}
