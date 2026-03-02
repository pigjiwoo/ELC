package com.elcserver.elcrules.listeners;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerListener implements Listener {

    private boolean isVillager(Entity e) { return e instanceof Villager || e instanceof WanderingTrader; }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && isVillager(e.getRightClicked())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c주민 및 떠돌이 상인과 상호작용할 수 없습니다!");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && !p.hasPermission("elcrules.bypass") && isVillager(e.getEntity())) {
            e.setCancelled(true);
            p.sendMessage("§c주민 및 떠돌이 상인을 공격할 수 없습니다!");
        }
    }
}
