package org.vaultsrc.noSkeletonHorses;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class noSkeletonHorses extends JavaPlugin implements Listener {

    private boolean isEnabled = true;
    private BukkitTask cleanupTask;
    private static final String PREFIX = ChatColor.DARK_GRAY + "» " + ChatColor.GOLD + "NSH" + ChatColor.DARK_GRAY + " » " + ChatColor.RESET;
    private static final String LINE = ChatColor.GRAY + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    @Override
    public void onEnable() {
        displayBranding();
        setupPlugin();
        getLogger().info("NSH Activated");
    }

    private void displayBranding() {
        getServer().getConsoleSender().sendMessage(LINE);
        getServer().getConsoleSender().sendMessage(ChatColor.GOLD + "NoSkeletonHorses " +
                ChatColor.GRAY + "v" + getDescription().getVersion());
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "Developed by " +
                ChatColor.RED + "Vaultsrc.com" + ChatColor.GRAY + " - Code Secured");
        getServer().getConsoleSender().sendMessage(LINE);
    }

    private void setupPlugin() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nsh").setExecutor(new SkeletonHorseCommand());
        removeExistingSkeletonHorses();
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!isEnabled || !isSkeletonHorse(event.getEntity())) return;
        event.setCancelled(true);
        logRemoval("Blocked spawn", event.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!isEnabled || !isSkeletonHorse(event.getEntity())) return;
        event.setCancelled(true);
        logRemoval("Blocked entity spawn", event.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!isEnabled) return;
        Bukkit.getScheduler().runTask(this, () -> {
            for (Entity entity : event.getChunk().getEntities()) {
                if (isSkeletonHorse(entity)) {
                    entity.remove();
                    logRemoval("Removed from chunk", entity.getLocation());
                }
            }
        });
    }

    private boolean isSkeletonHorse(Entity entity) {
        if (entity == null) return false;

        // Modern version check
        try {
            if (entity.getType() == EntityType.valueOf("SKELETON_HORSE")) return true;
        } catch (IllegalArgumentException ignored) {}

        // Name-based check
        if (entity.getType().name().equals("SKELETON_HORSE")) return true;

        // Legacy version check
        if (entity instanceof Horse) {
            try {
                Horse horse = (Horse) entity;
                return horse.getVariant().name().contains("SKELETON") ||
                        horse.getVariant().name().equals("UNDEAD_HORSE");
            } catch (Exception ignored) {}
        }

        return false;
    }

    private void removeExistingSkeletonHorses() {
        if (!isEnabled) return;
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isSkeletonHorse(entity)) {
                    entity.remove();
                    count++;
                }
            }
        }
        if (count > 0) {
            getLogger().info("Removed " + count + " skeleton horse(s)");
        }
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            removeExistingSkeletonHorses();
            if (cleanupTask.getTaskId() % 5 == 0) {
                deepScanForSkeletonHorses();
            }
        }, 0L, 1200L);
    }

    private void deepScanForSkeletonHorses() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity == null) continue;
                if (entity instanceof Horse ||
                        entity.getType().name().contains("HORSE") ||
                        (entity.getCustomName() != null &&
                                entity.getCustomName().toLowerCase().contains("skeleton"))) {
                    if (isSkeletonHorse(entity)) {
                        entity.remove();
                        logRemoval("Deep scan removed", entity.getLocation());
                    }
                }
            }
        }
    }

    private void logRemoval(String action, Location loc) {
        getLogger().info(String.format("%s at World: %s, X: %d, Y: %d, Z: %d",
                action,
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()));
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        getLogger().info("Protection system deactivated.");
    }

    private class SkeletonHorseCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("skeletonhorse.admin")) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Insufficient permissions.");
                return true;
            }

            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "enable":
                    if (isEnabled) {
                        sender.sendMessage(PREFIX + ChatColor.YELLOW + "Protection is already active.");
                        return true;
                    }
                    isEnabled = true;
                    startCleanupTask();
                    removeExistingSkeletonHorses();
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "Protection enabled successfully.");
                    break;

                case "disable":
                    if (!isEnabled) {
                        sender.sendMessage(PREFIX + ChatColor.YELLOW + "Protection is already inactive.");
                        return true;
                    }
                    isEnabled = false;
                    if (cleanupTask != null) {
                        cleanupTask.cancel();
                        cleanupTask = null;
                    }
                    sender.sendMessage(PREFIX + ChatColor.RED + "Protection disabled.");
                    break;

                case "status":
                    sender.sendMessage(PREFIX + ChatColor.GOLD + "Protection Status: " +
                            (isEnabled ? ChatColor.GREEN + "ACTIVE" : ChatColor.RED + "INACTIVE"));
                    break;

                default:
                    sendHelp(sender);
            }
            return true;
        }

        private void sendHelp(CommandSender sender) {
            sender.sendMessage(LINE);
            sender.sendMessage(ChatColor.GOLD + "NoSkeletonHorses " +
                    ChatColor.GRAY + "Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/nsh enable  " +
                    ChatColor.GRAY + "- Activate protection");
            sender.sendMessage(ChatColor.YELLOW + "/nsh disable " +
                    ChatColor.GRAY + "- Deactivate protection");
            sender.sendMessage(ChatColor.YELLOW + "/nsh status  " +
                    ChatColor.GRAY + "- View protection status");
            sender.sendMessage(LINE);
        }
    }
}