package com.elcserver.elcrules.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;

public class BrewingListener implements Listener {

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player p && !p.hasPermission("elcrules.bypass") && 
            e.getInventory().getType() == InventoryType.BREWING) {
            e.setCancelled(true);
            p.sendMessage("§c양조기를 사용할 수 없습니다!");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && e.getBlock().getType() == Material.BREWING_STAND) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c양조기를 설치할 수 없습니다!");
        }
    }
}
