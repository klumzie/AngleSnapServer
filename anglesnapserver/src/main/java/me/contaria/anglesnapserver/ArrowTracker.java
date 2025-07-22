package me.contaria.anglesnapserver;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World; // Required for EntityType.load

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ArrowTracker {

    private static final Map<UUID, List<NbtCompound>> playerArrowData = new HashMap<>();

    public void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            // In 1.21.6, player.getWorld() returns a ServerWorld directly in this context.
            ServerWorld world = player.getWorld();

            List<NbtCompound> arrowsToSave = new ArrayList<>();
            List<PersistentProjectileEntity> projectiles = world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                player.getBoundingBox().expand(128),
                entity -> entity.getOwner() != null && entity.getOwner().getUuid().equals(uuid)
            );

            for (PersistentProjectileEntity projectile : projectiles) {
                NbtCompound nbt = new NbtCompound();
                // This is the correct method for 1.21.6
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
            ServerWorld world = player.getWorld();

            List<NbtCompound> arrowsToRestore = playerArrowData.remove(uuid);

            if (arrowsToRestore != null) {
                for (NbtCompound nbt : arrowsToRestore) {
                    // This is the correct method for 1.21.6.
                    // Note: It takes a 'World', and ServerWorld is a type of World.
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
