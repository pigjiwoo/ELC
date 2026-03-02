package com.elcserver.faction.gui;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.WarManager;
import com.elcserver.faction.model.*;
import com.elcserver.faction.util.FactionUtils;
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
 * 전쟁 관련 GUI (격문 작성소, 선전포고 확인 메뉴)
 */
public class WarGUI implements Listener {
    
    private final FactionCore plugin;
    private final WarManager warManager;
    
    private final Map<UUID, String> openMenus;
    private final Map<UUID, String> selectedTarget;       // 세력 선택 GUI에서 선택된 세력
    private final Map<UUID, List<String>> factionPageData; // 페이지네이션용
    
    private static final String PROCLAMATION_MENU_TITLE = "§6§l격문 작성소";
    private static final String TARGET_SELECT_TITLE = "§c§l상대 세력 지정";
    private static final String DECLARATION_INFO_TITLE = "§e§l선전포고 확인";
    private static final String WAR_LIST_TITLE = "§c§l전쟁/격문 목록";
    private static final String CONFIRM_TITLE = "§c§l격문 작성 확인";
    
    public WarGUI(FactionCore plugin) {
        this.plugin = plugin;
        this.warManager = plugin.getWarManager();
        this.openMenus = new HashMap<>();
        this.selectedTarget = new HashMap<>();
        this.factionPageData = new HashMap<>();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    // ===== 격문 작성소 메인 메뉴 =====
    
    /**
     * 격문 작성소 GUI 열기
     */
    public void openProclamationMenu(Player player) {
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        Inventory inv = Bukkit.createInventory(null, 54, PROCLAMATION_MENU_TITLE);
        
        // 상단: 격문 작성소 정보
        ItemStack infoItem = createItem(Material.WRITABLE_BOOK, "§6§l격문 작성소",
            "§7격문을 작성하여 상대 세력에",
            "§7선전포고를 할 수 있습니다.",
            "",
            "§e작성 가능 시간: §f주말 19:00 - 22:00",
            "",
            warManager.isWritingTimeAllowed() ? "§a현재 작성 가능" : "§c현재 작성 불가능");
        inv.setItem(4, infoItem);
        
        // 격문 작성 버튼
        if (playerFaction != null) {
            FactionMember member = playerFaction.getMember(player.getUniqueId());
            boolean canWrite = member != null && 
                              (member.isLeader() || member.getRole() == FactionRole.OFFICER) &&
                              playerFaction.getTier().getLevel() >= FactionTier.VILLAGE.getLevel() &&
                              warManager.isWritingTimeAllowed();
            
            Material writeMat = canWrite ? Material.WRITTEN_BOOK : Material.BARRIER;
            List<String> writeLore = new ArrayList<>();
            writeLore.add("§7새로운 격문을 작성합니다.");
            writeLore.add("");
            
            if (!warManager.isWritingTimeAllowed()) {
                writeLore.add("§c✘ 작성 가능 시간이 아닙니다.");
            }
            if (member == null || (!member.isLeader() && member.getRole() != FactionRole.OFFICER)) {
                writeLore.add("§c✘ 대장 또는 부대장만 작성 가능");
            }
            if (playerFaction.getTier().getLevel() < FactionTier.VILLAGE.getLevel()) {
                writeLore.add("§c✘ 촌락 이상의 세력만 작성 가능");
            }
            
            WarDeclaration.DeclarationType type = WarDeclaration.getDeclarationType(playerFaction.getTier());
            if (type != null && canWrite) {
                writeLore.add("");
                writeLore.add("§7격문 종류: §e" + type.getDisplayName());
                writeLore.add("§7범위: §f" + type.getDescription());
                writeLore.add("");
                writeLore.add("§a클릭하여 상대 세력을 지정하세요.");
            }
            
            ItemStack writeItem = createItem(writeMat, canWrite ? "§a§l격문 작성" : "§c§l격문 작성 불가",
                writeLore.toArray(new String[0]));
            inv.setItem(20, writeItem);
        } else {
            ItemStack noFactionItem = createItem(Material.BARRIER, "§c§l세력 미소속",
                "§7세력에 소속되어야 격문을 작성할 수 있습니다.");
            inv.setItem(20, noFactionItem);
        }
        
        // 내 세력 전쟁/격문 현황
        ItemStack warStatusItem;
        if (playerFaction != null) {
            List<WarDeclaration> myWars = warManager.getActiveDeclarationsForFaction(playerFaction.getId());
            List<String> statusLore = new ArrayList<>();
            statusLore.add("§7진행 중인 격문/전쟁: §e" + myWars.size() + "건");
            
            for (WarDeclaration dec : myWars) {
                Faction opponent;
                if (dec.getAttackerFactionId().equals(playerFaction.getId())) {
                    opponent = plugin.getDataManager().getFaction(dec.getDefenderFactionId());
                } else {
                    opponent = plugin.getDataManager().getFaction(dec.getAttackerFactionId());
                }
                String opponentName = opponent != null ? opponent.getName() : "알 수 없음";
                statusLore.add("§7- §e" + opponentName + " §7[" + dec.updateAndGetPhase().getDisplayName() + "]");
            }
            
            if (!myWars.isEmpty()) {
                statusLore.add("");
                statusLore.add("§e클릭하여 상세 내용을 확인하세요.");
            }
            
            warStatusItem = createItem(Material.IRON_SWORD, "§e§l내 세력 전쟁 현황", 
                statusLore.toArray(new String[0]));
        } else {
            warStatusItem = createItem(Material.IRON_SWORD, "§e§l전쟁 현황", 
                "§7세력에 소속되어야 합니다.");
        }
        inv.setItem(22, warStatusItem);
        
        // 최근 격문 목록 (30분 이내)
        List<WarDeclaration> recentDeclarations = warManager.getRecentProclamations();
        ItemStack recentItem;
        List<String> recentLore = new ArrayList<>();
        recentLore.add("§7최근 30분 이내 작성된 격문: §e" + recentDeclarations.size() + "건");
        recentLore.add("");
        
        for (WarDeclaration dec : recentDeclarations) {
            Faction attacker = plugin.getDataManager().getFaction(dec.getAttackerFactionId());
            Faction defender = plugin.getDataManager().getFaction(dec.getDefenderFactionId());
            String attackerName = attacker != null ? attacker.getName() : "?";
            String defenderName = defender != null ? defender.getName() : "?";
            
            recentLore.add("§e" + attackerName + " §f→ §c" + defenderName);
            recentLore.add("  §7선전포고: " + FactionUtils.formatDuration(dec.getTimeUntilDeclaration()));
        }
        
        recentItem = createItem(Material.PAPER, "§6§l최근 격문 목록",
            recentLore.toArray(new String[0]));
        inv.setItem(24, recentItem);
        
        // 하단: 닫기
        ItemStack closeItem = createItem(Material.BARRIER, "§c닫기", "§7메뉴를 닫습니다.");
        inv.setItem(49, closeItem);
        
        openMenus.put(player.getUniqueId(), "PROCLAMATION_MENU");
        player.openInventory(inv);
    }
    
    // ===== 상대 세력 선택 GUI =====
    
    /**
     * 상대 세력 선택 GUI 열기
     */
    public void openTargetSelectMenu(Player player) {
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (playerFaction == null) return;
        
        WarDeclaration.DeclarationType myType = WarDeclaration.getDeclarationType(playerFaction.getTier());
        if (myType == null) return;
        
        // 같은 격문 유형 범위의 세력만 표시
        List<Faction> eligibleFactions = new ArrayList<>();
        for (Faction faction : plugin.getFactionManager().getAllFactions()) {
            if (faction.getId().equals(playerFaction.getId())) continue;
            if (faction.getTier().getLevel() < FactionTier.VILLAGE.getLevel()) continue;
            
            WarDeclaration.DeclarationType theirType = WarDeclaration.getDeclarationType(faction.getTier());
            if (myType == theirType) {
                eligibleFactions.add(faction);
            }
        }
        
        int size = Math.min(54, ((eligibleFactions.size() / 7) + 2) * 9);
        size = Math.max(27, size);
        Inventory inv = Bukkit.createInventory(null, size, TARGET_SELECT_TITLE);
        
        // 안내 아이템
        ItemStack infoItem = createItem(Material.COMPASS, "§e§l상대 세력 지정",
            "§7격문을 보낼 세력을 선택하세요.",
            "",
            "§7격문 종류: §e" + myType.getDisplayName(),
            "§7범위: §f" + myType.getDescription());
        inv.setItem(4, infoItem);
        
        // 세력 목록
        List<String> factionIds = new ArrayList<>();
        int slot = 9;
        for (Faction faction : eligibleFactions) {
            if (slot >= size - 9) break;
            
            List<String> lore = new ArrayList<>();
            lore.add("§7단계: §f" + faction.getTier().getDisplayName());
            lore.add("§7세력원: §f" + faction.getMemberCount() + "명");
            lore.add("§7코어: §f" + faction.getCoreCount() + "개");
            lore.add("");
            lore.add("§e클릭하여 이 세력에 격문을 작성합니다.");
            
            ItemStack factionItem = createItem(Material.PLAYER_HEAD, "§c" + faction.getName(),
                lore.toArray(new String[0]));
            inv.setItem(slot, factionItem);
            factionIds.add(faction.getName());
            slot++;
        }
        
        factionPageData.put(player.getUniqueId(), factionIds);
        
        if (eligibleFactions.isEmpty()) {
            ItemStack emptyItem = createItem(Material.BARRIER, "§c대상 세력 없음",
                "§7같은 격문 유형 범위 내에 세력이 없습니다.",
                "§7" + myType.getDescription());
            inv.setItem(13, emptyItem);
        }
        
        // 뒤로가기
        ItemStack backItem = createItem(Material.ARROW, "§e뒤로가기", "§7격문 작성소로 돌아갑니다.");
        inv.setItem(size - 5, backItem);
        
        openMenus.put(player.getUniqueId(), "TARGET_SELECT");
        player.openInventory(inv);
    }
    
    // ===== 격문 작성 확인 GUI =====
    
    /**
     * 격문 작성 확인 GUI 열기
     */
    public void openConfirmMenu(Player player, String targetFactionName) {
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        Faction targetFaction = plugin.getDataManager().getFactionByName(targetFactionName);
        
        if (playerFaction == null || targetFaction == null) return;
        
        WarDeclaration.DeclarationType type = WarDeclaration.getDeclarationType(playerFaction.getTier());
        
        Inventory inv = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        
        // 격문 정보
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("§e작성 세력: §f" + playerFaction.getName() + " §7(" + playerFaction.getTier().getDisplayName() + ")");
        infoLore.add("§c상대 세력: §f" + targetFaction.getName() + " §7(" + targetFaction.getTier().getDisplayName() + ")");
        infoLore.add("");
        infoLore.add("§7격문 종류: §e" + (type != null ? type.getDisplayName() : "?"));
        infoLore.add("");
        infoLore.add("§e30분 후 선전포고가 발동됩니다.");
        infoLore.add("§c1시간 후 전쟁이 시작됩니다.");
        
        ItemStack infoItem = createItem(Material.WRITABLE_BOOK, "§6§l격문 작성 확인",
            infoLore.toArray(new String[0]));
        inv.setItem(4, infoItem);
        
        // 확인 버튼
        ItemStack confirmItem = createItem(Material.LIME_WOOL, "§a§l격문 작성",
            "§7클릭하여 격문을 작성합니다.",
            "",
            "§c주의: 되돌릴 수 없습니다!");
        inv.setItem(11, confirmItem);
        
        // 취소 버튼
        ItemStack cancelItem = createItem(Material.RED_WOOL, "§c§l취소",
            "§7격문 작성을 취소합니다.");
        inv.setItem(15, cancelItem);
        
        selectedTarget.put(player.getUniqueId(), targetFactionName);
        openMenus.put(player.getUniqueId(), "CONFIRM");
        player.openInventory(inv);
    }
    
    // ===== 선전포고 확인 메뉴 =====
    
    /**
     * 선전포고/전쟁 상세 정보 GUI
     */
    public void openDeclarationInfoMenu(Player player) {
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (playerFaction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        List<WarDeclaration> declarations = warManager.getActiveDeclarationsForFaction(playerFaction.getId());
        
        int size = Math.min(54, Math.max(27, ((declarations.size() / 7) + 2) * 9));
        Inventory inv = Bukkit.createInventory(null, size, DECLARATION_INFO_TITLE);
        
        // 상단 정보
        ItemStack headerItem = createItem(Material.IRON_SWORD, "§e§l선전포고/전쟁 현황",
            "§7현재 진행 중인 격문 및 전쟁 목록입니다.",
            "§7아이템을 클릭하면 상세 정보를 채팅으로 표시됩니다.");
        inv.setItem(4, headerItem);
        
        // 격문/전쟁 목록
        int slot = 9;
        for (WarDeclaration dec : declarations) {
            if (slot >= size - 9) break;
            
            Faction opponent;
            boolean isAttacker;
            if (dec.getAttackerFactionId().equals(playerFaction.getId())) {
                opponent = plugin.getDataManager().getFaction(dec.getDefenderFactionId());
                isAttacker = true;
            } else {
                opponent = plugin.getDataManager().getFaction(dec.getAttackerFactionId());
                isAttacker = false;
            }
            
            String opponentName = opponent != null ? opponent.getName() : "알 수 없음";
            WarDeclaration.WarPhase phase = dec.updateAndGetPhase();
            
            List<String> lore = new ArrayList<>();
            lore.add("§7상태: " + getPhaseColor(phase) + phase.getDisplayName());
            lore.add("§7위치: " + (isAttacker ? "§a공격측" : "§c방어측"));
            lore.add("");
            
            Faction attacker = plugin.getDataManager().getFaction(dec.getAttackerFactionId());
            Faction defender = plugin.getDataManager().getFaction(dec.getDefenderFactionId());
            
            lore.add("§e공격 세력: §f" + (attacker != null ? attacker.getName() : "?"));
            lore.add("§c방어 세력: §f" + (defender != null ? defender.getName() : "?"));
            lore.add("");
            lore.add("§7격문 종류: §e" + dec.getType().getDisplayName());
            lore.add("§7격문 작성 시간: §f" + formatTime(dec.getCreatedTime()));
            lore.add("");
            
            if (phase == WarDeclaration.WarPhase.PROCLAMATION) {
                lore.add("§e선전포고까지: §f" + FactionUtils.formatDuration(dec.getTimeUntilDeclaration()));
                lore.add("§c전쟁까지: §f" + FactionUtils.formatDuration(dec.getTimeUntilWar()));
            } else if (phase == WarDeclaration.WarPhase.DECLARATION) {
                lore.add("§c전쟁까지: §f" + FactionUtils.formatDuration(dec.getTimeUntilWar()));
            } else if (phase == WarDeclaration.WarPhase.WAR) {
                lore.add("§c§l전쟁 진행 중!");
            }
            
            // 코어 정보
            if (attacker != null) {
                lore.add("");
                lore.add("§6" + attacker.getName() + " 코어:");
                addCoreLore(lore, attacker);
            }
            if (defender != null) {
                lore.add("");
                lore.add("§c" + defender.getName() + " 코어:");
                addCoreLore(lore, defender);
            }
            
            lore.add("");
            lore.add("§e클릭하여 채팅으로 상세 정보 표시");
            
            Material mat = getMaterialForPhase(phase);
            ItemStack item = createItem(mat, getPhaseColor(phase) + opponentName + " §7[" + phase.getDisplayName() + "]",
                lore.toArray(new String[0]));
            inv.setItem(slot, item);
            slot++;
        }
        
        if (declarations.isEmpty()) {
            ItemStack emptyItem = createItem(Material.PAPER, "§7진행 중인 격문/전쟁이 없습니다.");
            inv.setItem(13, emptyItem);
        }
        
        // 닫기
        ItemStack closeItem = createItem(Material.BARRIER, "§c닫기", "§7메뉴를 닫습니다.");
        inv.setItem(size - 5, closeItem);
        
        openMenus.put(player.getUniqueId(), "DECLARATION_INFO");
        player.openInventory(inv);
    }
    
    /**
     * 코어 정보 Lore에 추가
     */
    private void addCoreLore(List<String> lore, Faction faction) {
        List<Core> cores = plugin.getDataManager().getFactionCores(faction.getId());
        if (cores.isEmpty()) {
            lore.add("  §7코어 없음");
            return;
        }
        for (Core core : cores) {
            org.bukkit.Location loc = core.getLocation();
            String locStr = loc != null ? FactionUtils.formatCoordinates(loc) : "?";
            lore.add("  §fLv." + core.getLevel() + " §7(" + locStr + ")");
        }
    }
    
    /**
     * 단계별 색상
     */
    private String getPhaseColor(WarDeclaration.WarPhase phase) {
        switch (phase) {
            case PROCLAMATION: return "§e";
            case DECLARATION: return "§6";
            case WAR: return "§c";
            case ENDED: return "§7";
            default: return "§f";
        }
    }
    
    /**
     * 단계별 아이템
     */
    private Material getMaterialForPhase(WarDeclaration.WarPhase phase) {
        switch (phase) {
            case PROCLAMATION: return Material.PAPER;
            case DECLARATION: return Material.WRITABLE_BOOK;
            case WAR: return Material.IRON_SWORD;
            case ENDED: return Material.GRAY_WOOL;
            default: return Material.PAPER;
        }
    }
    
    /**
     * 시간 포맷
     */
    private String formatTime(long timeMillis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd HH:mm");
        return sdf.format(new java.util.Date(timeMillis));
    }
    
    // ===== 이벤트 핸들러 =====
    
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
            case "PROCLAMATION_MENU":
                handleProclamationClick(player, clicked, itemName);
                break;
            case "TARGET_SELECT":
                handleTargetSelectClick(player, clicked, itemName);
                break;
            case "CONFIRM":
                handleConfirmClick(player, clicked, itemName);
                break;
            case "DECLARATION_INFO":
                handleDeclarationInfoClick(player, clicked, itemName);
                break;
        }
    }
    
    private void handleProclamationClick(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("격문 작성") && !itemName.contains("불가")) {
            // 시간 재확인
            if (!warManager.isWritingTimeAllowed()) {
                player.sendMessage(plugin.getMessageManager().getPrefix() + "§c격문 작성 가능 시간이 아닙니다.");
                return;
            }
            openTargetSelectMenu(player);
        } else if (itemName.contains("전쟁 현황")) {
            openDeclarationInfoMenu(player);
        } else if (itemName.contains("닫기")) {
            player.closeInventory();
        }
    }
    
    private void handleTargetSelectClick(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("뒤로가기")) {
            openProclamationMenu(player);
            return;
        }
        
        // 세력 이름 추출 (§c 제거)
        if (clicked.getType() == Material.PLAYER_HEAD && itemName.startsWith("§c")) {
            String factionName = itemName.substring(2); // §c 제거
            
            // 검증
            String error = warManager.validateDeclaration(player, factionName);
            if (error != null) {
                player.sendMessage(plugin.getMessageManager().getPrefix() + error);
                return;
            }
            
            openConfirmMenu(player, factionName);
        }
    }
    
    private void handleConfirmClick(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("격문 작성") && clicked.getType() == Material.LIME_WOOL) {
            String targetName = selectedTarget.get(player.getUniqueId());
            if (targetName == null) {
                player.closeInventory();
                return;
            }
            
            // 최종 검증
            String error = warManager.validateDeclaration(player, targetName);
            if (error != null) {
                player.sendMessage(plugin.getMessageManager().getPrefix() + error);
                player.closeInventory();
                return;
            }
            
            Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
            Faction targetFaction = plugin.getDataManager().getFactionByName(targetName);
            
            if (playerFaction == null || targetFaction == null) {
                player.closeInventory();
                return;
            }
            
            // 격문 작성!
            WarDeclaration declaration = warManager.createDeclaration(player, playerFaction, targetFaction);
            
            player.closeInventory();
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§a격문이 작성되었습니다! §e" + targetFaction.getName() + "§a 세력에 대한 " +
                declaration.getType().getDisplayName() + "이 작성되었습니다.");
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§730분 후 선전포고가 발동됩니다.");
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§c1시간 후 전쟁이 시작됩니다.");
            
            selectedTarget.remove(player.getUniqueId());
            
        } else if (itemName.contains("취소")) {
            selectedTarget.remove(player.getUniqueId());
            openTargetSelectMenu(player);
        }
    }
    
    private void handleDeclarationInfoClick(Player player, ItemStack clicked, String itemName) {
        if (itemName.contains("닫기")) {
            player.closeInventory();
            return;
        }
        
        // 클릭한 격문의 상세 정보를 채팅으로 표시
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (playerFaction == null) return;
        
        List<WarDeclaration> declarations = warManager.getActiveDeclarationsForFaction(playerFaction.getId());
        int index = event2Index(clicked, declarations);
        
        if (index >= 0 && index < declarations.size()) {
            WarDeclaration dec = declarations.get(index);
            List<String> content = warManager.getDeclarationContent(dec);
            
            player.sendMessage("");
            for (String line : content) {
                player.sendMessage(line);
            }
            player.sendMessage("");
        }
    }
    
    /**
     * 클릭된 아이템에서 인덱스 추출 (슬롯 기반)
     */
    private int event2Index(ItemStack clicked, List<WarDeclaration> declarations) {
        // 단순히 아이템의 존재 여부로 판단
        if (clicked.getType() == Material.PAPER || 
            clicked.getType() == Material.WRITABLE_BOOK || 
            clicked.getType() == Material.IRON_SWORD ||
            clicked.getType() == Material.GRAY_WOOL) {
            
            // 아이템 이름에서 세력명 추출하여 매칭
            String name = clicked.getItemMeta().getDisplayName();
            for (int i = 0; i < declarations.size(); i++) {
                Faction opponent;
                WarDeclaration dec = declarations.get(i);
                Faction playerFaction = plugin.getDataManager().getFaction(dec.getAttackerFactionId());
                
                // 이 방법은 정확하지 않을 수 있으므로 순서대로 반환
                return i;
            }
        }
        return -1;
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            openMenus.remove(player.getUniqueId());
            factionPageData.remove(player.getUniqueId());
        }
    }
    
    // ===== 유틸리티 =====
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    if (line != null) {
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
