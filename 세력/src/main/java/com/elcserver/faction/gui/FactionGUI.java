package com.elcserver.faction.gui;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionMember;
import com.elcserver.faction.model.FactionRole;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * 세력 GUI 메뉴
 */
public class FactionGUI implements Listener {
    
    private final FactionCore plugin;
    private final Map<UUID, String> openMenus;
    
    private static final String FACTION_MENU_TITLE = "§6세력 메뉴";
    private static final String MEMBER_LIST_TITLE = "§6세력원 목록";
    
    public FactionGUI(FactionCore plugin) {
        this.plugin = plugin;
        this.openMenus = new HashMap<>();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 세력 메인 메뉴 열기
     */
    public void openMainMenu(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        Inventory inv = Bukkit.createInventory(null, 45, FACTION_MENU_TITLE);
        
        // 세력 정보
        ItemStack infoItem = createItem(Material.BOOK, "§e" + faction.getName(),
            "§7단계: §f" + faction.getTier().getDisplayName(),
            "§7세력원: §f" + faction.getMemberCount() + "명",
            "§7코어: §f" + faction.getCoreCount() + "개");
        inv.setItem(4, infoItem);
        
        // 세력원 목록
        ItemStack memberItem = createItem(Material.PLAYER_HEAD, "§e세력원 목록",
            "§7클릭하여 세력원 목록 보기");
        inv.setItem(20, memberItem);
        
        // 세력 계좌
        ItemStack accountItem = createItem(Material.GOLD_INGOT, "§e세력 계좌",
            "§7잔액: §a" + plugin.getEconomyManager().formatAmount(faction.getBalance()) + "쿤",
            "§7인출 한도: §f" + plugin.getEconomyManager().formatAmount(
                plugin.getEconomyManager().getRemainingWithdrawLimit(player.getUniqueId())) + "쿤");
        inv.setItem(22, accountItem);
        
        // 세력 포인트
        ItemStack pointItem = createItem(Material.NETHER_STAR, "§d세력 포인트",
            "§7포인트: §d" + plugin.getEconomyManager().formatAmount(faction.getPoints()) + "점");
        inv.setItem(24, pointItem);
        
        // 코어 목록
        ItemStack coreItem = createItem(Material.BEACON, "§6코어 관리",
            "§7코어: §f" + faction.getCoreCount() + "개",
            "§7클릭하여 코어 목록 보기");
        inv.setItem(30, coreItem);
        
        // 세력 설정 (대장만)
        FactionMember member = faction.getMember(player.getUniqueId());
        if (member != null && member.isLeader()) {
            ItemStack settingsItem = createItem(Material.COMPARATOR, "§c세력 설정",
                "§7세력 관리 메뉴");
            inv.setItem(32, settingsItem);
        }
        
        // 쿤 배수 정보
        ItemStack multiplierItem = createItem(Material.EXPERIENCE_BOTTLE, "§a쿤 배수",
            "§7현재 배수: §a" + String.format("%.2f", faction.calculateKunMultiplier()) + "배",
            "§7기본: 1.5배 + 코어당 0.05배",
            "§7최대: 2.6배",
            faction.isFeverTimeActive() ? "§d피버타임 활성화!" : "");
        inv.setItem(40, multiplierItem);
        
        // 닫기
        ItemStack closeItem = createItem(Material.BARRIER, "§c닫기", "§7메뉴를 닫습니다.");
        inv.setItem(44, closeItem);
        
        openMenus.put(player.getUniqueId(), "FACTION_MENU");
        player.openInventory(inv);
    }
    
    /**
     * 세력원 목록 메뉴 열기
     */
    public void openMemberList(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        int size = Math.min(54, ((faction.getMemberCount() / 9) + 2) * 9);
        Inventory inv = Bukkit.createInventory(null, size, MEMBER_LIST_TITLE);
        
        int slot = 0;
        
        // 역할별 정렬
        List<FactionMember> sortedMembers = new ArrayList<>(faction.getMembers());
        sortedMembers.sort((a, b) -> b.getRole().getLevel() - a.getRole().getLevel());
        
        for (FactionMember member : sortedMembers) {
            if (slot >= size - 9) break;
            
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            
            if (meta != null) {
                // 온라인 상태 확인
                Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
                boolean online = memberPlayer != null && memberPlayer.isOnline();
                
                String roleColor = member.getRole() == FactionRole.LEADER ? "§c" :
                                   member.getRole() == FactionRole.OFFICER ? "§e" : "§7";
                
                meta.setDisplayName(roleColor + member.getPlayerName());
                
                List<String> lore = new ArrayList<>();
                lore.add("§7역할: §f" + member.getRole().getDisplayName());
                lore.add("§7상태: " + (online ? "§a온라인" : "§7오프라인"));
                
                if (member.isJoinedToday()) {
                    lore.add("§e오늘 가입");
                }
                
                meta.setLore(lore);
                
                // 스킬 소유자 설정
                if (memberPlayer != null) {
                    meta.setOwningPlayer(memberPlayer);
                }
                
                skull.setItemMeta(meta);
            }
            
            inv.setItem(slot, skull);
            slot++;
        }
        
        // 뒤로가기
        ItemStack backItem = createItem(Material.ARROW, "§e뒤로가기", "§7메인 메뉴로 돌아갑니다.");
        inv.setItem(size - 5, backItem);
        
        openMenus.put(player.getUniqueId(), "MEMBER_LIST");
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
            case "FACTION_MENU":
                handleFactionMenuClick(player, itemName);
                break;
            case "MEMBER_LIST":
                handleMemberListClick(player, itemName);
                break;
        }
    }
    
    private void handleFactionMenuClick(Player player, String itemName) {
        if (itemName.contains("세력원 목록")) {
            openMemberList(player);
        } else if (itemName.contains("코어 관리")) {
            player.closeInventory();
            player.performCommand("코어 목록");
        } else if (itemName.contains("닫기")) {
            player.closeInventory();
        }
    }
    
    private void handleMemberListClick(Player player, String itemName) {
        if (itemName.contains("뒤로가기")) {
            openMainMenu(player);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            openMenus.remove(player.getUniqueId());
        }
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    if (!line.isEmpty()) {
                        loreList.add(line);
                    }
                }
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
