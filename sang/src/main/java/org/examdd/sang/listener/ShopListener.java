package org.examdd.sang.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.examdd.sang.Sang;
import org.examdd.sang.gui.ShopGUI;

public class ShopListener implements Listener {

    private final Sang plugin;
    private final ShopGUI gui;

    public ShopListener(Sang plugin, ShopGUI gui) {
        this.plugin = plugin;
        this.gui    = gui;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!gui.hasSession(player)) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        gui.handleClick(player, e.getRawSlot());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (gui.hasSession(player)) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING) {
                gui.removeSession(player);
            }
        });
    }
}
