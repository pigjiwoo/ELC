package com.elcserver.elcrules.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class GolemListener implements Listener {

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        SpawnReason r = e.getSpawnReason();
        if (r == SpawnReason.BUILD_IRONGOLEM || r == SpawnReason.BUILD_SNOWMAN) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("elcrules.bypass")) return;
        Material m = e.getBlock().getType();
        if (m == Material.CARVED_PUMPKIN || m == Material.JACK_O_LANTERN) {
            Material b = e.getBlock().getRelative(0, -1, 0).getType();
            if (b == Material.IRON_BLOCK || b == Material.SNOW_BLOCK) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("§c골렘을 생성할 수 없습니다!");
            }
        }
    }
}
