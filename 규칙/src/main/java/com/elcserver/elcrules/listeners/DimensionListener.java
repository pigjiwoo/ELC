package com.elcserver.elcrules.listeners;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class DimensionListener implements Listener {

    private boolean isBanned(Environment e) { return e == Environment.NETHER || e == Environment.THE_END; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPortal(PlayerPortalEvent e) {
        if (e.getPlayer().hasPermission("elcrules.bypass")) return;
        TeleportCause cause = e.getCause();
        if (cause == TeleportCause.NETHER_PORTAL || cause == TeleportCause.END_PORTAL) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c지옥과 엔더 월드는 이용할 수 없습니다!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTp(PlayerTeleportEvent e) {
        if (e.getPlayer().hasPermission("elcrules.bypass")) return;
        if (e.getTo() == null || e.getTo().getWorld() == null) return;
        TeleportCause cause = e.getCause();
        if (cause == TeleportCause.NETHER_PORTAL || cause == TeleportCause.END_PORTAL || isBanned(e.getTo().getWorld().getEnvironment())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c지옥과 엔더 월드는 이용할 수 없습니다!");
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!e.getPlayer().hasPermission("elcrules.bypass") && e.getBlock().getType() == Material.END_PORTAL_FRAME) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c엔드 포탈 프레임을 설치할 수 없습니다!");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission("elcrules.bypass") || e.getClickedBlock() == null || e.getItem() == null) return;
        Material b = e.getClickedBlock().getType(), i = e.getItem().getType();
        if ((b == Material.END_PORTAL_FRAME && i == Material.ENDER_EYE) || (b == Material.OBSIDIAN && i == Material.FLINT_AND_STEEL)) {
            e.setCancelled(true);
            p.sendMessage("§c포탈을 활성화할 수 없습니다!");
        }
    }
}
