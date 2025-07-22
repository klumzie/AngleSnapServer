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

    // Stores arrow NBT data for each player UUID
    private static final Map<UUID, List<NbtCompound>> playerArrowData = new HashMap<>();

    public void register() {

        // Save arrows when a player disconnects
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = player.getWorld();

            List<NbtCompound> arrowsToSave = new ArrayList<>();

            // Find all arrows owned by this player
            List<PersistentProjectileEntity> projectiles = world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                player.getBoundingBox().expand(128),
                entity -> entity.getOwner() != null && entity.getOwner().getUuid().equals(uuid)
            );

            for (PersistentProjectileEntity projectile : projectiles) {
                // Save the arrow's full NBT data
                NbtCompound nbt = projectile.createNbtWithId(world);
                arrowsToSave.add(nbt);
                projectile.discard(); // Remove the entity from the world
            }

            if (!arrowsToSave.isEmpty()) {
                playerArrowData.put(uuid, arrowsToSave);
            }
        });

        // Restore arrows when the player logs back in
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = player.getWorld();

            List<NbtCompound> arrowsToRestore = playerArrowData.remove(uuid);
            if (arrowsToRestore != null) {
                for (NbtCompound nbt : arrowsToRestore) {
                    Optional<Entity> optionalEntity = EntityType.load(world, nbt);
                    optionalEntity.ifPresent(entity -> {
                        if (entity instanceof PersistentProjectileEntity projectile) {
                            projectile.setOwner(player);
                        }
                        world.spawnEntity(entity);
                    });
                }
            }
        });
    }
}
