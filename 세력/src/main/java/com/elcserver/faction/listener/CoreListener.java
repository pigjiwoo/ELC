package com.elcserver.faction.listener;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.CoreManager;
import com.elcserver.faction.model.Core;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionMember;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 코어 관련 이벤트 리스너
 */
public class CoreListener implements Listener {
    
    private final FactionCore plugin;
    private final CoreManager coreManager;
    
    // 코어 아이템 설정 (넥소 아이템 에더 호환)
    private static final String CORE_ITEM_NAME = "§6§l세력 코어";
    private static final int CORE_CUSTOM_MODEL_DATA = 5000;
    
    public CoreListener(FactionCore plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        
        // 우클릭만 처리
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        // 손에 코어 아이템이 있는지 확인
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isCoreStem(item)) {
            event.setCancelled(true);
            
            // 설치 위치 확인
            Block targetBlock = event.getClickedBlock();
            if (targetBlock != null) {
                Location location = targetBlock.getLocation().add(0, 1, 0);
                coreManager.startCoreInstall(player, location);
            }
            return;
        }
        
        // 코어 블록 클릭 처리
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.BEACON) {
                Core core = coreManager.getCoreAtLocation(block.getLocation());
                if (core != null) {
                    event.setCancelled(true);
                    
                    // 다른 세력 코어 상호작용 차단
                    Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
                    if (playerFaction == null || !playerFaction.getId().equals(core.getFactionId())) {
                        Faction coreFaction = plugin.getDataManager().getFaction(core.getFactionId());
                        String factionName = coreFaction != null ? coreFaction.getName() : "알 수 없는";
                        player.sendMessage(plugin.getMessageManager().getPrefix() + 
                            "§c이 코어는 §e" + factionName + " §c세력의 소유입니다. 상호작용할 수 없습니다.");
                        return;
                    }
                    
                    openCoreMenu(player, core);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // 코어 블록 파괴 방지
        if (block.getType() == Material.BEACON) {
            Core core = coreManager.getCoreAtLocation(block.getLocation());
            if (core != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§c코어는 직접 파괴할 수 없습니다. §7/코어 회수 명령어를 사용하세요.");
                return;
            }
        }
        
        // 코어 위 블록 파괴 (Y=64 이상, 코어는 Y=63에 설치)
        Location blockLoc = block.getLocation();
        int coreY = plugin.getConfigManager().getCoreInstallYLevel(); // 63
        
        if (blockLoc.getBlockY() > coreY) {
            // 해당 X,Z 좌표에 코어가 있는지 확인
            Location coreLoc = blockLoc.clone();
            coreLoc.setY(coreY);
            
            Core coreBelow = coreManager.getCoreAtLocation(coreLoc);
            if (coreBelow != null) {
                // 코어 위 블록은 자유롭게 파괴 가능 (방해 없음)
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();
        int coreY = plugin.getConfigManager().getCoreInstallYLevel(); // 63
        
        // 코어 위치(Y=63)에 블록 설치 방지
        if (blockLoc.getBlockY() == coreY) {
            Core coreAtLoc = coreManager.getCoreAtLocation(blockLoc);
            if (coreAtLoc != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§c코어 위치에는 블록을 설치할 수 없습니다.");
                return;
            }
        }
        
        // 코어 위쪽으로 블록 설치 방지 (Y > coreY, 같은 X/Z)
        if (blockLoc.getBlockY() > coreY) {
            Location coreLoc = blockLoc.clone();
            coreLoc.setY(coreY);
            
            Core coreBelow = coreManager.getCoreAtLocation(coreLoc);
            if (coreBelow != null) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§c코어 위쪽에는 블록을 설치할 수 없습니다.");
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 블록 변경 시에만 체크
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();
        
        // 영역 진입/이탈 알림 (성능 최적화를 위해 비동기로 처리하거나 캐싱 필요)
        Faction fromFaction = coreManager.getFactionAtLocation(from);
        Faction toFaction = coreManager.getFactionAtLocation(to);
        
        if (fromFaction != toFaction) {
            if (toFaction != null && fromFaction == null) {
                // 영역 진입
                player.sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§e" + toFaction.getName() + " §f영역에 진입하였습니다.");
            } else if (toFaction == null && fromFaction != null) {
                // 영역 이탈
                player.sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§7" + fromFaction.getName() + " §f영역에서 벗어났습니다.");
            } else if (toFaction != null) {
                // 다른 세력 영역으로 이동
                player.sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§e" + toFaction.getName() + " §f영역에 진입하였습니다.");
            }
        }
    }
    
    /**
     * 코어 아이템 확인 (넥소 아이템 에더 호환 - CustomModelData 5000)
     */
    private boolean isCoreStem(ItemStack item) {
        if (item == null || item.getType() != Material.BEACON) {
            return false;
        }
        
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 커스텀 모델 데이터로 확인 (넥소 아이템 에더 호환)
        if (meta.hasCustomModelData() && meta.getCustomModelData() == CORE_CUSTOM_MODEL_DATA) {
            return true;
        }
        
        // 이름으로도 확인 (하위 호환)
        if (meta.hasDisplayName() && meta.getDisplayName().equals(CORE_ITEM_NAME)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 코어 메뉴 열기 (GUI로 변경)
     */
    private void openCoreMenu(Player player, Core core) {
        Faction faction = plugin.getDataManager().getFaction(core.getFactionId());
        Faction playerFaction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        // 자신의 세력 코어인지 확인
        if (playerFaction == null || !playerFaction.getId().equals(core.getFactionId())) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§7이 코어는 §e" + (faction != null ? faction.getName() : "알 수 없는") + 
                " §7세력의 소유입니다.");
            return;
        }
        
        // 코어 메뉴 열기
        plugin.getCoreGUI().openMainMenu(player, core);
    }
    
    /**
     * 코어 아이템 생성 (상점 등에서 사용 - 넥소 아이템 에더 호환)
     */
    public static ItemStack createCoreItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(CORE_ITEM_NAME);
            meta.setCustomModelData(CORE_CUSTOM_MODEL_DATA); // 넥소 아이템 에더 호환
            
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7세력 영토의 중심이 되는 코어입니다.");
            lore.add("");
            lore.add("§e우클릭으로 설치");
            lore.add("§7설치 후 20분간 회수 불가");
            lore.add("");
            lore.add("§8CustomModelData: " + CORE_CUSTOM_MODEL_DATA);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
