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

    // This map stores the arrow data for offline players, keyed by the player's UUID.
    private static final Map<UUID, List<NbtCompound>> playerArrowData = new HashMap<>();

    /**
     * Registers the server events to save and restore player arrows on logout and login.
     */
    public void register() {
        // --- Event for when a player disconnects from the server ---
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = player.getServerWorld();

            List<NbtCompound> arrowsToSave = new ArrayList<>();

            // Find all persistent projectiles (arrows, tridents, etc.) owned by the disconnecting player.
            List<PersistentProjectileEntity> projectiles = world.getEntitiesByClass(
                PersistentProjectileEntity.class,
                player.getBoundingBox().expand(128), // Search in a 128-block radius around the player.
                entity -> entity.getOwner() != null && entity.getOwner().getUuid().equals(uuid)
            );

            for (PersistentProjectileEntity projectile : projectiles) {
                // Create a new NBT tag to store the arrow's data.
                NbtCompound nbt = new NbtCompound();
                projectile.writeNbt(nbt);
                arrowsToSave.add(nbt);

                // Remove the original arrow entity from the world.
                projectile.discard();
            }

            // If any arrows were found and saved, add them to our map.
            if (!arrowsToSave.isEmpty()) {
                playerArrowData.put(uuid, arrowsToSave);
            }
        });

        // --- Event for when a player joins the server ---
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.player;
            UUID uuid = player.getUuid();
            ServerWorld world = player.getServerWorld();

            // Get and simultaneously remove the arrow data for the joining player.
            List<NbtCompound> arrowsToRestore = playerArrowData.remove(uuid);

            if (arrowsToRestore != null) {
                for (NbtCompound nbt : arrowsToRestore) {
                    // Use Minecraft's built-in loader to safely recreate the entity from its saved data.
                    // This correctly restores its type, position, motion, and all other properties.
                    Optional<Entity> optionalEntity = EntityType.loadEntityFromNbt(nbt, world);

                    optionalEntity.ifPresent(entity -> {
                        // Ensure the owner is set to the player instance that just logged in.
                        if (entity instanceof PersistentProjectileEntity) {
                            ((PersistentProjectileEntity) entity).setOwner(player);
                        }
                        // Spawn the restored arrow back into the world.
                        world.spawnEntity(entity);
                    });
                }
            }
        });
    }
}
