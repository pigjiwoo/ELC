package com.elcserver.faction.manager;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.config.ConfigManager;
import com.elcserver.faction.data.DataManager;
import com.elcserver.faction.model.Core;
import com.elcserver.faction.model.Faction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 코어 관리 클래스
 */
public class CoreManager {
    
    private final FactionCore plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    
    // 설치 중인 플레이어 (쿨다운 방지)
    private final Map<UUID, Long> installingPlayers;
    
    // 코어 아이템을 들고 있는 플레이어 (구매 후 20분 제한)
    private final Map<UUID, Long> pendingInstalls;
    
    public CoreManager(FactionCore plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
        this.installingPlayers = new HashMap<>();
        this.pendingInstalls = new HashMap<>();
    }
    
    // ===== 코어 설치 =====
    
    /**
     * 코어 설치 시작 (3초 딜레이)
     */
    public void startCoreInstall(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        
        // 이미 설치 중인지 확인
        if (installingPlayers.containsKey(playerId)) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.install-cancelled"));
            return;
        }
        
        // 세력 확인
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        // 권한 확인
        if (!faction.getMember(playerId).getRole().canManageCore()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
            return;
        }
        
        // 설치 위치 조정 (Y=63)
        Location installLocation = location.clone();
        installLocation.setY(configManager.getCoreInstallYLevel());
        
        // 설치 가능 여부 확인
        String blockReason = getInstallBlockReasonDetailed(faction, installLocation);
        if (blockReason != null) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c" + blockReason);
            return;
        }
        
        // 설치 시작
        installingPlayers.put(playerId, System.currentTimeMillis());
        
        int delay = configManager.getCoreInstallDelaySeconds();
        
        // 카운트다운
        for (int i = delay; i > 0; i--) {
            final int seconds = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (installingPlayers.containsKey(playerId)) {
                    player.sendMessage(plugin.getMessageManager().getMessage(
                        "core.install-countdown", "%seconds%", String.valueOf(seconds)));
                }
            }, (delay - i) * 20L);
        }
        
        // 설치 완료
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (installingPlayers.containsKey(playerId)) {
                installingPlayers.remove(playerId);
                installCore(faction, installLocation);
                
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                    "core.installed",
                    "%x%", String.valueOf(installLocation.getBlockX()),
                    "%y%", String.valueOf(installLocation.getBlockY()),
                    "%z%", String.valueOf(installLocation.getBlockZ())));
            }
        }, delay * 20L);
    }
    
    /**
     * 코어 설치 취소
     */
    public void cancelInstall(UUID playerId) {
        installingPlayers.remove(playerId);
    }
    
    /**
     * 설치 가능 여부 확인
     */
    public boolean canInstallCore(Faction faction, Location location) {
        // 티어별 코어 설치 가능 여부
        if (!faction.getTier().canInstallCore()) {
            return false;
        }
        
        // 코어 개수 제한 확인
        int maxCores = faction.getTier().getMaxCores();
        if (faction.getCoreIds().size() >= maxCores) {
            return false;
        }
        
        // 위에 블록이 있는지 확인
        Block above = location.clone().add(0, 1, 0).getBlock();
        if (above.getType() != Material.AIR) {
            return false;
        }
        
        // 다른 세력 코어와 겹침 확인
        Core tempCore = new Core("temp", faction.getId(), location);
        
        for (Core existingCore : dataManager.getAllCores()) {
            // 같은 세력은 겹침 허용
            if (existingCore.getFactionId().equals(faction.getId())) {
                continue;
            }
            
            // 다른 세력과 겹침 불가
            if (tempCore.overlaps(existingCore)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 코어 설치 불가 사유 반환
     */
    public String getInstallBlockReason(Faction faction) {
        if (!faction.getTier().canInstallCore()) {
            return "현재 티어("
 + faction.getTier().getDisplayName() + ")에서는 코어를 설치할 수 없습니다.";
        }
        
        int maxCores = faction.getTier().getMaxCores();
        if (faction.getCoreIds().size() >= maxCores) {
            return "코어 최대 개수(" + maxCores + "개)에 도달했습니다. 업그레이드 후 추가 설치 가능합니다.";
        }
        
        return null;
    }
    
    /**
     * 코어 설치 불가 사유 반환 (위치 포함, 겹침 세력명 표시)
     */
    public String getInstallBlockReasonDetailed(Faction faction, Location location) {
        if (!faction.getTier().canInstallCore()) {
            return "현재 티어(" + faction.getTier().getDisplayName() + ")에서는 코어를 설치할 수 없습니다.";
        }
        
        int maxCores = faction.getTier().getMaxCores();
        if (faction.getCoreIds().size() >= maxCores) {
            return "코어 최대 개수(" + maxCores + "개)에 도달했습니다.";
        }
        
        // 코어 위에 블록이 있는지 확인
        Block above = location.clone().add(0, 1, 0).getBlock();
        if (above.getType() != Material.AIR) {
            return "코어 설치 위치 위에 블록이 있습니다.";
        }
        
        // 다른 세력 코어와 겹침 확인 (세력명 표시)
        Core tempCore = new Core("temp", faction.getId(), location);
        
        for (Core existingCore : dataManager.getAllCores()) {
            if (existingCore.getFactionId().equals(faction.getId())) {
                continue;
            }
            
            if (tempCore.overlaps(existingCore)) {
                Faction otherFaction = dataManager.getFaction(existingCore.getFactionId());
                String otherName = otherFaction != null ? otherFaction.getName() : "알 수 없는 세력";
                return "§e" + otherName + " §c세력의 코어 영역과 겹칩니다!";
            }
        }
        
        return null;
    }
    
    /**
     * 코어 실제 설치
     */
    private Core installCore(Faction faction, Location location) {
        String coreId = dataManager.generateCoreId();
        Core core = new Core(coreId, faction.getId(), location);
        
        // 블록 배치
        location.getBlock().setType(Material.BEACON);
        
        dataManager.addCore(core);
        faction.addCore(coreId);
        dataManager.scheduleSave();
        
        return core;
    }
    
    // ===== 코어 회수 =====
    
    /**
     * 코어 회수
     */
    public boolean retrieveCore(Player player, String coreId) {
        UUID playerId = player.getUniqueId();
        
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return false;
        
        Core core = dataManager.getCore(coreId);
        if (core == null) return false;
        
        // 소유권 확인
        if (!core.getFactionId().equals(faction.getId())) {
            return false;
        }
        
        // 권한 확인
        if (!faction.getMember(playerId).getRole().canManageCore()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
            return false;
        }
        
        // 쿨다운 확인
        if (!core.canRetrieve()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "core.retrieve-cooldown", "%minutes%", String.valueOf(core.getRetrieveCooldownMinutes())));
            return false;
        }
        
        // 비용 확인
        int cost = configManager.getCoreRetrieveCost();
        if (faction.getBalance() < cost) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("account.insufficient-funds"));
            return false;
        }
        
        // 비용 차감
        faction.withdraw(cost);
        
        // 블록 제거
        Location location = core.getLocation();
        if (location != null) {
            location.getBlock().setType(Material.AIR);
        }
        
        // 코어 상태 업데이트
        core.setLastRetrievedTime(System.currentTimeMillis());
        
        // 재설치 제한 등록
        pendingInstalls.put(playerId, System.currentTimeMillis());
        
        // 코어 제거 (세력에서만, 데이터는 유지하여 재설치 가능)
        faction.removeCore(coreId);
        
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.retrieved"));
        dataManager.scheduleSave();
        
        return true;
    }
    
    // ===== 코어 업그레이드 =====
    
    /**
     * 코어 업그레이드
     */
    public boolean upgradeCore(Player player, String coreId) {
        UUID playerId = player.getUniqueId();
        
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return false;
        
        Core core = dataManager.getCore(coreId);
        if (core == null) return false;
        
        // 소유권 확인
        if (!core.getFactionId().equals(faction.getId())) {
            return false;
        }
        
        // 권한 확인
        if (!faction.getMember(playerId).getRole().canUpgradeCore()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
            return false;
        }
        
        // 업그레이드 가능 확인 (기본)
        if (!core.canUpgrade()) {
            return false;
        }
        
        // 티어별 코어 레벨 제한 확인
        int targetLevel = core.getLevel() + 1;
        if (!faction.getTier().canUpgradeCoreToLevel(targetLevel)) {
            player.sendMessage(plugin.getMessageManager().getMessage(
                "core.level-limit", 
                "%tier%", faction.getTier().getDisplayName(),
                "%max_level%", String.valueOf(faction.getTier().getMaxCoreLevel())));
            return false;
        }
        
        // 비용 확인
        int cost = core.getUpgradeCost();
        if (faction.getBalance() < cost) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("account.insufficient-funds"));
            return false;
        }
        
        // 업그레이드 후 겹침 확인
        for (Core existingCore : dataManager.getAllCores()) {
            if (existingCore.getId().equals(coreId)) continue;
            if (existingCore.getFactionId().equals(faction.getId())) continue;
            
            if (core.wouldOverlapAfterUpgrade(existingCore)) {
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.upgrade-overlap"));
                return false;
            }
        }
        
        // 비용 차감 및 업그레이드
        faction.withdraw(cost);
        core.upgrade();
        
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
            "core.upgraded", "%level%", String.valueOf(core.getLevel())));
        
        // 세력 업그레이드 체크
        plugin.getFactionManager().checkAutoUpgrade(faction);
        
        dataManager.scheduleSave();
        return true;
    }
    
    // ===== 코어 파괴 =====
    
    /**
     * 코어 파괴 (강등/해체 시)
     */
    public void destroyCore(String coreId) {
        Core core = dataManager.getCore(coreId);
        if (core == null) return;
        
        // 블록 제거
        Location location = core.getLocation();
        if (location != null) {
            location.getBlock().setType(Material.AIR);
        }
        
        dataManager.removeCore(coreId);
    }
    
    // ===== 코어 등록 (TP) =====
    
    /**
     * 코어 등록 (TP용)
     */
    public boolean registerCore(Faction faction, String coreId) {
        Core core = dataManager.getCore(coreId);
        if (core == null) return false;
        
        if (core.isRegistered()) return false;
        
        // 사용 가능한 가장 낮은 슬롯 찾기
        int slot = findAvailableSlot(faction);
        if (slot == -1) {
            return false;
        }
        
        core.setRegisteredSlot(slot);
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 코어 등록 해제
     */
    public void unregisterCore(String coreId) {
        Core core = dataManager.getCore(coreId);
        if (core != null) {
            core.unregister();
            dataManager.scheduleSave();
        }
    }
    
    /**
     * 등록된 코어로 텔레포트
     */
    public boolean teleportToCore(Player player, String coreId) {
        Core core = dataManager.getCore(coreId);
        if (core == null) return false;
        
        Location tpLocation = core.getTeleportLocation();
        if (tpLocation == null) return false;
        
        player.teleport(tpLocation);
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.teleported"));
        return true;
    }
    
    private int findAvailableSlot(Faction faction) {
        Set<Integer> usedSlots = new HashSet<>();
        
        for (String coreId : faction.getCoreIds()) {
            Core core = dataManager.getCore(coreId);
            if (core != null && core.isRegistered()) {
                usedSlots.add(core.getRegisteredSlot());
            }
        }
        
        // 최대 9개 슬롯 (1-9)
        for (int i = 1; i <= 9; i++) {
            if (!usedSlots.contains(i)) {
                return i;
            }
        }
        
        return -1; // 슬롯 없음
    }
    
    // ===== 유틸리티 =====
    
    /**
     * 위치에 있는 코어 찾기
     */
    public Core getCoreAtLocation(Location location) {
        for (Core core : dataManager.getAllCores()) {
            Location coreLoc = core.getLocation();
            if (coreLoc != null && 
                coreLoc.getBlockX() == location.getBlockX() &&
                coreLoc.getBlockY() == location.getBlockY() &&
                coreLoc.getBlockZ() == location.getBlockZ() &&
                coreLoc.getWorld().getName().equals(location.getWorld().getName())) {
                return core;
            }
        }
        return null;
    }
    
    /**
     * 위치가 특정 세력 영역 내인지 확인
     */
    public boolean isInFactionTerritory(Location location, String factionId) {
        List<Core> cores = dataManager.getFactionCores(factionId);
        for (Core core : cores) {
            if (core.isInRange(location)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 위치가 어느 세력 영역인지 확인
     */
    public Faction getFactionAtLocation(Location location) {
        for (Core core : dataManager.getAllCores()) {
            if (core.isInRange(location)) {
                return dataManager.getFaction(core.getFactionId());
            }
        }
        return null;
    }
    
    /**
     * 코어 조회
     */
    public Core getCore(String coreId) {
        return dataManager.getCore(coreId);
    }
    
    /**
     * 세력의 모든 코어 조회
     */
    public List<Core> getFactionCores(String factionId) {
        return dataManager.getFactionCores(factionId);
    }
}
