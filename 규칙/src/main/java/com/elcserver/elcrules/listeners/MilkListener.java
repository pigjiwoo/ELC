package com.elcserver.elcrules.listeners;

import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class MilkListener implements Listener {

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && e.getItem().getType() == Material.MILK_BUCKET) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c우유를 섭취할 수 없습니다!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("elcrules.bypass")) return;
        if (e.getRightClicked() instanceof Cow || e.getRightClicked() instanceof Goat) {
            if (p.getInventory().getItemInMainHand().getType() == Material.BUCKET ||
                p.getInventory().getItemInOffHand().getType() == Material.BUCKET) {
                e.setCancelled(true);
                p.sendMessage("§c우유를 얻을 수 없습니다!");
            }
        }
    }
}
