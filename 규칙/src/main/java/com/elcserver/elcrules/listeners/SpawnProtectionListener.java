package com.elcserver.elcrules.listeners;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;

public class SpawnProtectionListener implements Listener {

    private boolean inZone(Location l) {
        if (l == null || l.getWorld() == null || l.getWorld().getEnvironment() != Environment.NORMAL) return false;
        int x = l.getBlockX(), z = l.getBlockZ();
        return x >= -25 && x <= 25 && z >= -25 && z <= 25;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && inZone(e.getBlock().getLocation())) {
            e.setCancelled(true); e.getPlayer().sendMessage("§c스폰 보호 구역입니다!");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && inZone(e.getBlock().getLocation())) {
            e.setCancelled(true); e.getPlayer().sendMessage("§c스폰 보호 구역입니다!");
        }
    }

    @EventHandler
    public void onBucket1(PlayerBucketEmptyEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && inZone(e.getBlock().getLocation())) {
            e.setCancelled(true); e.getPlayer().sendMessage("§c스폰 보호 구역입니다!");
        }
    }

    @EventHandler
    public void onBucket2(PlayerBucketFillEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && inZone(e.getBlock().getLocation())) {
            e.setCancelled(true); e.getPlayer().sendMessage("§c스폰 보호 구역입니다!");
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) { e.blockList().removeIf(b -> inZone(b.getLocation())); }

    @EventHandler
    public void onHanging(HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player p && !p.hasPermission("elcrules.bypass") && inZone(e.getEntity().getLocation())) {
            e.setCancelled(true); p.sendMessage("§c스폰 보호 구역입니다!");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && inZone(p.getLocation())) e.setCancelled(true);
    }
}
