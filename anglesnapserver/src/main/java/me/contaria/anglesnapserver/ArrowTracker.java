package me.contaria.anglesnapserver;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

public class ArrowTracker {

    private static final Map<UUID, List<NbtCompound>> playerArrowData = new HashMap<>();

    public ArrowTracker() {
        // Save arrows on player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = (ServerWorld) player.getWorld();

            List<NbtCompound> arrows = new ArrayList<>();

            // Find all arrows owned by the player
            for (PersistentProjectileEntity entity : world.getEntitiesByClass(
                    PersistentProjectileEntity.class,
                    player.getBoundingBox().expand(128),
                    e -> e.getOwner() != null && e.getOwner().getUuid().equals(uuid))
            ) {
                NbtCompound tag = new NbtCompound();
                entity.writeNbt(tag); // Includes "Owner", "HasBeenShot", etc.
                arrows.add(tag);
                entity.discard(); // Remove from world
            }

            playerArrowData.put(uuid, arrows);
        });

        // Restore arrows on player reconnect
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = (ServerWorld) player.getWorld();

            List<NbtCompound> arrows = playerArrowData.remove(uuid);
            if (arrows != null) {
                for (NbtCompound tag : arrows) {
                    ArrowEntity arrow = new ArrowEntity(world, player.getX(), player.getY(), player.getZ());
                    arrow.readCustomDataFromNbt(tag); // Restores owner and flight status
                    world.spawnEntity(arrow);
                }
            }
        });
    }
}
