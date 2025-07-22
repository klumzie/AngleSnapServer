package me.contaria.anglesnapserver;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

public class ArrowTracker {

    private static final Map<UUID, List<NbtCompound>> playerArrowData = new HashMap<>();

    public void register() {

        // Save player arrows when disconnecting
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = player.getWorld();

            List<NbtCompound> arrowsToSave = new ArrayList<>();

            List<PersistentProjectileEntity> projectiles = world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                player.getBoundingBox().expand(128),
                entity -> entity.getOwner() != null && entity.getOwner().getUuid().equals(uuid)
            );

            for (PersistentProjectileEntity projectile : projectiles) {
                NbtCompound nbt = new NbtCompound();
                projectile.writeNbt(nbt); // ✅ Works in 1.21.6
                arrowsToSave.add(nbt);
                projectile.discard(); // Remove from world
            }

            if (!arrowsToSave.isEmpty()) {
                playerArrowData.put(uuid, arrowsToSave);
            }
        });

        // Restore arrows on login
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = player.getWorld();

            List<NbtCompound> arrowsToRestore = playerArrowData.remove(uuid);

            if (arrowsToRestore != null) {
                for (NbtCompound nbt : arrowsToRestore) {
                    EntityType.getEntityFromNbt(nbt, world).ifPresent(entity -> {
                        if (entity instanceof PersistentProjectileEntity projectile) {
                            projectile.setOwner(player); // Re-assign owner
                        }
                        world.spawnEntity(entity);
                    });
                }
            }
        });
    }
}
