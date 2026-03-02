package com.elcserver.faction.gui;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.Core;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 코어 GUI 메뉴
 */
public class CoreGUI implements Listener {
    
    private final FactionCore plugin;
    private final Map<UUID, String> openMenus; // playerId -> menuType
    private final Map<UUID, String> selectedCores; // playerId -> coreId
    
    private static final String CORE_MENU_TITLE = "§6코어 메뉴";
    private static final String CORE_LIST_TITLE = "§6코어 목록";
    private static final String CORE_TP_TITLE = "§6코어 텔레포트";
    
    public CoreGUI(FactionCore plugin) {
        this.plugin = plugin;
        this.openMenus = new HashMap<>();
        this.selectedCores = new HashMap<>();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 코어 메인 메뉴 열기
     */
    public void openMainMenu(Player player, Core core) {
        Inventory inv = Bukkit.createInventory(null, 27, CORE_MENU_TITLE);
        
        Faction faction = plugin.getDataManager().getFaction(core.getFactionId());
        FactionMember member = faction != null ? faction.getMember(player.getUniqueId()) : null;
        boolean canManage = member != null && member.getRole().canManageCore();
        
        // 코어 정보
        String coreName = core.hasRegisteredName() ? core.getRegisteredName() : "미등록";
        ItemStack infoItem = createItem(Material.BEACON, "§e" + coreName,
            "§7단계: §f" + core.getLevel() + "단계",
            "§7범위: §f" + core.getRange() + "×" + core.getRange(),
            "§7위치: §f" + core.getX() + ", " + core.getY() + ", " + core.getZ());
        inv.setItem(4, infoItem);
        
        if (canManage) {
            // 업그레이드 버튼
            if (core.canUpgrade()) {
                ItemStack upgradeItem = createItem(Material.EXPERIENCE_BOTTLE, "§a코어 업그레이드",
                    "§7비용: §e" + core.getUpgradeCost() + "쿤",
                    "§7현재: §f" + core.getLevel() + "단계 → " + (core.getLevel() + 1) + "단계",
                    "",
                    "§e클릭하여 업그레이드");
                inv.setItem(11, upgradeItem);
            } else {
                ItemStack maxItem = createItem(Material.BARRIER, "§c최대 단계",
                    "§7이미 최대 단계입니다.");
                inv.setItem(11, maxItem);
            }
            
            // 회수 버튼
            if (core.canRetrieve()) {
                ItemStack retrieveItem = createItem(Material.CHEST, "§e코어 회수",
                    "§7비용: §e" + plugin.getConfigManager().getCoreRetrieveCost() + "쿤",
                    "",
                    "§e클릭하여 회수");
                inv.setItem(13, retrieveItem);
            } else {
                ItemStack cooldownItem = createItem(Material.CLOCK, "§c회수 대기중",
                    "§7" + core.getRetrieveCooldownMinutes() + "분 후 회수 가능");
                inv.setItem(13, cooldownItem);
            }
            
            // 등록/해제 버튼
            if (core.isRegistered()) {
                ItemStack unregisterItem = createItem(Material.ENDER_PEARL, "§c등록 해제",
                    "§7현재 슬롯: §e#" + core.getRegisteredSlot(),
                    "",
                    "§e클릭하여 해제");
                inv.setItem(15, unregisterItem);
            } else {
                ItemStack registerItem = createItem(Material.ENDER_EYE, "§a텔레포트 등록",
                    "§7코어를 등록하여 빠르게 이동",
                    "",
                    "§e클릭하여 등록");
                inv.setItem(15, registerItem);
            }
        }
        
        // 닫기 버튼
        ItemStack closeItem = createItem(Material.BARRIER, "§c닫기", "§7메뉴를 닫습니다.");
        inv.setItem(22, closeItem);
        
        openMenus.put(player.getUniqueId(), "CORE_MENU");
        selectedCores.put(player.getUniqueId(), core.getId());
        player.openInventory(inv);
    }
    
    /**
     * 코어 텔레포트 메뉴 열기
     */
    public void openTeleportMenu(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        Inventory inv = Bukkit.createInventory(null, 27, CORE_TP_TITLE);
        
        List<Core> cores = plugin.getCoreManager().getFactionCores(faction.getId());
        
        int slot = 0;
        for (Core core : cores) {
            if (!core.hasRegisteredName()) continue;
            if (slot >= 18) break;
            
            Material material = core.getLevel() == 3 ? Material.DIAMOND_BLOCK :
                               core.getLevel() == 2 ? Material.GOLD_BLOCK : Material.IRON_BLOCK;
            
            ItemStack coreItem = createItem(material, 
                "§e" + core.getRegisteredName(),
                "§7단계: §f" + core.getLevel() + "단계",
                "§7위치: §f" + core.getX() + ", " + core.getY() + ", " + core.getZ(),
                "",
                "§a클릭하여 이동");
            
            inv.setItem(slot, coreItem);
            slot++;
        }
        
        if (slot == 0) {
            ItemStack emptyItem = createItem(Material.BARRIER, "§c등록된 코어 없음",
                "§7코어를 우클릭하여 등록하세요.");
            inv.setItem(13, emptyItem);
        }
        
        // 닫기 버튼
        ItemStack closeItem = createItem(Material.BARRIER, "§c닫기", "§7메뉴를 닫습니다.");
        inv.setItem(22, closeItem);
        
        openMenus.put(player.getUniqueId(), "CORE_TP");
        player.openInventory(inv);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String menuType = openMenus.get(player.getUniqueId());
        
        if (menuType == null) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        String itemName = clicked.getItemMeta().getDisplayName();
        
        switch (menuType) {
            case "CORE_MENU":
                handleCoreMenuClick(player, itemName);
                break;
            case "CORE_TP":
                handleTeleportMenuClick(player, event.getSlot());
                break;
        }
    }
    
    private void handleCoreMenuClick(Player player, String itemName) {
        String coreId = selectedCores.get(player.getUniqueId());
        if (coreId == null) return;
        
        Core core = plugin.getCoreManager().getCore(coreId);
        if (core == null) {
            player.closeInventory();
            return;
        }
        
        if (itemName.contains("업그레이드")) {
            player.closeInventory();
            plugin.getCoreManager().upgradeCore(player, coreId);
        } else if (itemName.contains("회수")) {
            player.closeInventory();
            plugin.getCoreManager().retrieveCore(player, coreId);
        } else if (itemName.contains("텔레포트 등록")) {
            Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
            if (faction != null && plugin.getCoreManager().registerCore(faction, coreId)) {
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                    "core.registered", "%slot%", String.valueOf(core.getRegisteredSlot())));
            } else {
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.register-full"));
            }
            player.closeInventory();
        } else if (itemName.contains("등록 해제")) {
            plugin.getCoreManager().unregisterCore(coreId);
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.deleted"));
            player.closeInventory();
        } else if (itemName.contains("닫기")) {
            player.closeInventory();
        }
    }
    
    private void handleTeleportMenuClick(Player player, int slot) {
        if (slot >= 18) {
            player.closeInventory();
            return;
        }
        
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction == null) return;
        
        List<Core> cores = plugin.getCoreManager().getFactionCores(faction.getId());
        
        int index = 0;
        for (Core core : cores) {
            if (!core.hasRegisteredName()) continue;
            
            if (index == slot) {
                player.closeInventory();
                plugin.getCoreManager().teleportToCore(player, core.getId());
                return;
            }
            index++;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            openMenus.remove(player.getUniqueId());
            selectedCores.remove(player.getUniqueId());
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
