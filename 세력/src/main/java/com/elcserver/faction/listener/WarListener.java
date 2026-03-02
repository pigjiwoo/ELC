package com.elcserver.faction.listener;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.WarManager;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.WarDeclaration;
import com.elcserver.faction.util.FactionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 전쟁 관련 이벤트 리스너
 * 
 * - 격문 작성소 상호작용
 * - 격문 작성소 주위 격문 목록 디스플레이 (아머스탠드 홀로그램)
 */
public class WarListener implements Listener {
    
    private final FactionCore plugin;
    private final WarManager warManager;
    
    // 격문 작성소 블록 (LECTERN - 독서대)
    private static final Material PROCLAMATION_BLOCK = Material.LECTERN;
    
    // 홀로그램 태그
    private static final String HOLOGRAM_TAG = "war_hologram";
    
    // 홀로그램 업데이트 태스크
    private BukkitRunnable hologramTask;
    
    public WarListener(FactionCore plugin) {
        this.plugin = plugin;
        this.warManager = plugin.getWarManager();
        
        // 홀로그램 업데이트 시작 (10초마다)
        startHologramUpdater();
    }
    
    /**
     * 격문 작성소 상호작용 (LECTERN 우클릭)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        // 격문 작성소 블록 확인
        if (block.getType() != PROCLAMATION_BLOCK) return;
        
        // 격문 작성소 위치 확인
        Location stationLoc = warManager.getProclamationStationLocation();
        if (stationLoc == null) return;
        
        // 위치가 격문 작성소 근처인지 확인 (5블록 이내)
        if (!block.getWorld().equals(stationLoc.getWorld())) return;
        if (block.getLocation().distance(stationLoc) > 5.0) return;
        
        event.setCancelled(true);
        
        // 격문 작성소 GUI 열기
        Player player = event.getPlayer();
        plugin.getWarGUI().openProclamationMenu(player);
    }
    
    /**
     * 격문 작성소 주위 홀로그램 업데이트 태스크
     */
    private void startHologramUpdater() {
        hologramTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateProclamationHolograms();
            }
        };
        
        // 10초 (200틱)마다 업데이트
        hologramTask.runTaskTimer(plugin, 60L, 200L);
    }
    
    /**
     * 격문 작성소 주위 홀로그램 업데이트
     */
    private void updateProclamationHolograms() {
        Location stationLoc = warManager.getProclamationStationLocation();
        if (stationLoc == null || stationLoc.getWorld() == null) return;
        
        // 기존 홀로그램 제거
        removeExistingHolograms(stationLoc);
        
        // 최근 격문 목록 가져오기
        List<WarDeclaration> recentDeclarations = warManager.getRecentProclamations();
        if (recentDeclarations.isEmpty()) {
            // 격문이 없으면 기본 텍스트만 표시
            createHologram(stationLoc.clone().add(0.5, 2.5, 0.5), "§6§l⚔ 격문 작성소 ⚔");
            createHologram(stationLoc.clone().add(0.5, 2.2, 0.5), "§7우클릭으로 격문 작성");
            return;
        }
        
        // 헤더
        double y = 2.5 + (recentDeclarations.size() * 0.6);
        createHologram(stationLoc.clone().add(0.5, y, 0.5), "§6§l⚔ 격문 작성소 ⚔");
        y -= 0.3;
        createHologram(stationLoc.clone().add(0.5, y, 0.5), "§e최근 격문 목록");
        y -= 0.3;
        createHologram(stationLoc.clone().add(0.5, y, 0.5), "§7──────────────");
        y -= 0.3;
        
        // 격문 목록 표시
        for (WarDeclaration dec : recentDeclarations) {
            Faction attacker = plugin.getDataManager().getFaction(dec.getAttackerFactionId());
            Faction defender = plugin.getDataManager().getFaction(dec.getDefenderFactionId());
            
            String attackerName = attacker != null ? attacker.getName() : "?";
            String defenderName = defender != null ? defender.getName() : "?";
            
            // 세력 이름
            createHologram(stationLoc.clone().add(0.5, y, 0.5), 
                "§e" + attackerName + " §f→ §c" + defenderName);
            y -= 0.3;
            
            // 선전포고까지 남은 시간
            long timeUntil = dec.getTimeUntilDeclaration();
            if (timeUntil > 0) {
                createHologram(stationLoc.clone().add(0.5, y, 0.5),
                    "§7선전포고: §f" + FactionUtils.formatDuration(timeUntil));
            } else {
                createHologram(stationLoc.clone().add(0.5, y, 0.5),
                    "§c선전포고 발동됨");
            }
            y -= 0.3;
            
            createHologram(stationLoc.clone().add(0.5, y, 0.5), "§7──────────────");
            y -= 0.3;
        }
    }
    
    /**
     * 기존 홀로그램 제거
     */
    private void removeExistingHolograms(Location stationLoc) {
        if (stationLoc.getWorld() == null) return;
        
        for (Entity entity : stationLoc.getWorld().getNearbyEntities(stationLoc, 10, 10, 10)) {
            if (entity.getType() == EntityType.ARMOR_STAND && entity.getScoreboardTags().contains(HOLOGRAM_TAG)) {
                entity.remove();
            }
        }
    }
    
    /**
     * 아머스탠드 홀로그램 생성
     */
    private void createHologram(Location location, String text) {
        if (location.getWorld() == null) return;
        
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setCustomName(text);
        armorStand.setCustomNameVisible(true);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setSmall(true);
        armorStand.setMarker(true);
        armorStand.setInvulnerable(true);
        armorStand.addScoreboardTag(HOLOGRAM_TAG);
    }
    
    /**
     * 홀로그램 태스크 취소
     */
    public void cleanup() {
        if (hologramTask != null) {
            hologramTask.cancel();
        }
        
        // 모든 홀로그램 제거
        Location stationLoc = warManager.getProclamationStationLocation();
        if (stationLoc != null) {
            removeExistingHolograms(stationLoc);
        }
    }
}
