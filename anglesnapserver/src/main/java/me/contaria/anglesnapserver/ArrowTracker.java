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
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            // FIX 1: The correct method is getWorld(), which needs to be cast to ServerWorld.
            ServerWorld world = (ServerWorld) player.getWorld();

            List<NbtCompound> arrowsToSave = new ArrayList<>();
            List<PersistentProjectileEntity> projectiles = world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                player.getBoundingBox().expand(128),
                entity -> entity.getOwner() != null && entity.getOwner().getUuid().equals(uuid)
            );

            for (PersistentProjectileEntity projectile : projectiles) {
                NbtCompound nbt = new NbtCompound();
                // FIX 2: The method 'writeNbt' was renamed to 'saveNbt'.
                projectile.saveNbt(nbt);
                arrowsToSave.add(nbt);
                projectile.discard();
            }

            if (!arrowsToSave.isEmpty()) {
                playerArrowData.put(uuid, arrowsToSave);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            // FIX 1 (repeated): Correctly get the world.
            ServerWorld world = (ServerWorld) player.getWorld();

            List<NbtCompound> arrowsToRestore = playerArrowData.remove(uuid);

            if (arrowsToRestore != null) {
                for (NbtCompound nbt : arrowsToRestore) {
                    // FIX 3: The method 'loadEntityFromNbt' was replaced with 'load'.
                    Optional<Entity> optionalEntity = EntityType.load(world, nbt);

                    optionalEntity.ifPresent(entity -> {
                        if (entity instanceof PersistentProjectileEntity) {
                            ((PersistentProjectileEntity) entity).setOwner(player);
                        }
                        world.spawnEntity(entity);
                    });
                }
            }
        });
    }
}
