package com.elcserver.elcrules.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

public class AutomationListener implements Listener {

    private boolean isBlocked(Material m) {
        String n = m.name();
        return n.contains("REDSTONE") || n.contains("PISTON") || n.contains("HOPPER") ||
               n.contains("DISPENSER") || n.contains("DROPPER") || n.contains("OBSERVER") ||
               n.contains("COMPARATOR") || n.contains("REPEATER") || n.contains("LEVER") ||
               n.contains("BUTTON") || n.contains("PRESSURE_PLATE") || n.contains("TRIPWIRE") ||
               n.contains("SCULK_SENSOR") || n.contains("DAYLIGHT") || n.contains("TARGET") ||
               m == Material.TNT || m == Material.CRAFTER;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && isBlocked(e.getBlock().getType())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c자동화 및 복사 시스템은 사용할 수 없습니다!");
        }
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) { e.setCancelled(true); }

    @EventHandler
    public void onPiston2(BlockPistonRetractEvent e) { e.setCancelled(true); }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent e) { e.setNewCurrent(0); }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent e) { e.setCancelled(true); }
}
