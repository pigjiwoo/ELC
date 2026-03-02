package com.elcserver.faction.manager;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.config.ConfigManager;
import com.elcserver.faction.data.DataManager;
import com.elcserver.faction.model.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 세력 관리 클래스
 */
public class FactionManager {
    
    private final FactionCore plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    
    public FactionManager(FactionCore plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
    }
    
    // ===== 세력 생성/해체 =====
    
    /**
     * 새 세력 생성 (촌락 등록)
     */
    public Faction createFaction(String name, UUID leaderId, String leaderName, 
                                  String icon, List<UUID> officers) {
        String factionId = dataManager.generateFactionId();
        Faction faction = new Faction(factionId, name, leaderId, leaderName);
        faction.setIcon(icon);
        faction.setTier(FactionTier.VILLAGE); // 촌락으로 시작
        
        // 부대장 설정
        for (UUID officerId : officers) {
            Player officer = Bukkit.getPlayer(officerId);
            if (officer != null) {
                faction.addMember(officerId, officer.getName(), FactionRole.OFFICER);
            }
        }
        
        dataManager.addFaction(faction);
        dataManager.scheduleSave();
        
        // LuckPerms 연동 - 세력 그룹 생성 및 멤버 추가
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.getOrCreateFactionGroup(faction).thenRun(() -> {
                // 대장 추가
                lpm.addPlayerToFaction(leaderId, faction, FactionRole.LEADER);
                // 부대장들 추가
                for (UUID officerId : officers) {
                    lpm.addPlayerToFaction(officerId, faction, FactionRole.OFFICER);
                }
            });
        }
        
        return faction;
    }
    
    /**
     * 세력 해체
     */
    public void disbandFaction(String factionId) {
        Faction faction = dataManager.getFaction(factionId);
        if (faction == null) return;
        
        // LuckPerms 연동 - 모든 멤버 그룹에서 제거
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            for (UUID memberId : faction.getMemberIds()) {
                lpm.removePlayerFromFaction(memberId);
            }
            // 세력 그룹 삭제
            lpm.deleteFactionGroup(faction.getName());
        }
        
        // 모든 세력원에게 알림
        for (UUID memberId : faction.getMemberIds()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.disbanded"));
            }
        }
        
        // 코어 삭제
        for (String coreId : faction.getCoreIds()) {
            plugin.getCoreManager().destroyCore(coreId);
        }
        
        dataManager.removeFaction(factionId);
        dataManager.scheduleSave();
    }
    
    // ===== 세력원 관리 =====
    
    /**
     * 플레이어가 세력에 가입
     */
    public boolean joinFaction(UUID playerId, String factionId) {
        Faction faction = dataManager.getFaction(factionId);
        if (faction == null) return false;
        
        // 이미 세력에 소속된 경우
        if (dataManager.getPlayerFaction(playerId) != null) {
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        String playerName = player != null ? player.getName() : "Unknown";
        
        faction.addMember(playerId, playerName, FactionRole.MEMBER);
        dataManager.setPlayerFaction(playerId, factionId);
        
        // LuckPerms 연동
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.addPlayerToFaction(playerId, faction, FactionRole.MEMBER);
        }
        
        // 업그레이드 조건 체크
        checkAutoUpgrade(faction);
        
        // 강등 경고 체크 (인원 회복)
        checkDemotionRecovery(faction);
        
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 플레이어가 세력 탈퇴
     */
    public boolean leaveFaction(UUID playerId) {
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return false;
        
        FactionMember member = faction.getMember(playerId);
        if (member == null) return false;
        
        // 대장은 탈퇴 불가
        if (member.isLeader()) {
            return false;
        }
        
        faction.removeMember(playerId);
        dataManager.setPlayerFaction(playerId, null);
        
        // LuckPerms 연동
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.removePlayerFromFaction(playerId);
        }
        
        // 강등 조건 체크
        checkDemotionCondition(faction);
        
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 세력원 추방
     */
    public boolean kickMember(UUID kickerId, UUID targetId) {
        Faction faction = dataManager.getPlayerFaction(kickerId);
        if (faction == null) return false;
        
        FactionMember kicker = faction.getMember(kickerId);
        FactionMember target = faction.getMember(targetId);
        
        if (kicker == null || target == null) return false;
        
        // 권한 확인
        if (!kicker.getRole().canInviteOrKick()) return false;
        
        // 대장은 추방 불가
        if (target.isLeader()) return false;
        
        // 자신은 추방 불가
        if (kickerId.equals(targetId)) return false;
        
        // 부대장은 부대장 추방 불가
        if (kicker.getRole() == FactionRole.OFFICER && target.getRole() == FactionRole.OFFICER) {
            return false;
        }
        
        faction.removeMember(targetId);
        dataManager.setPlayerFaction(targetId, null);
        
        // LuckPerms 연동
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.removePlayerFromFaction(targetId);
        }
        
        // 강등 조건 체크
        checkDemotionCondition(faction);
        
        dataManager.scheduleSave();
        return true;
    }
    
    // ===== 초대 시스템 =====
    
    /**
     * 세력 초대
     */
    public boolean invitePlayer(UUID inviterId, UUID inviteeId) {
        Faction faction = dataManager.getPlayerFaction(inviterId);
        if (faction == null) return false;
        
        FactionMember inviter = faction.getMember(inviterId);
        if (inviter == null || !inviter.getRole().canInviteOrKick()) {
            return false;
        }
        
        // 이미 세력에 소속된 경우
        if (dataManager.getPlayerFaction(inviteeId) != null) {
            return false;
        }
        
        // 이미 초대된 경우
        if (dataManager.hasInvite(inviteeId)) {
            return false;
        }
        
        FactionInvite invite = new FactionInvite(faction.getId(), inviterId, inviteeId);
        dataManager.addInvite(invite);
        
        return true;
    }
    
    /**
     * 초대 수락
     */
    public boolean acceptInvite(UUID playerId) {
        FactionInvite invite = dataManager.getInvite(playerId);
        if (invite == null || invite.isExpired()) {
            dataManager.removeInvite(playerId);
            return false;
        }
        
        boolean success = joinFaction(playerId, invite.getFactionId());
        dataManager.removeInvite(playerId);
        
        return success;
    }
    
    /**
     * 초대 거절
     */
    public void declineInvite(UUID playerId) {
        dataManager.removeInvite(playerId);
    }
    
    // ===== 역할 관리 =====
    
    /**
     * 부대장 승급
     */
    public boolean promoteToOfficer(UUID promoterId, UUID targetId) {
        Faction faction = dataManager.getPlayerFaction(promoterId);
        if (faction == null) return false;
        
        FactionMember promoter = faction.getMember(promoterId);
        FactionMember target = faction.getMember(targetId);
        
        if (promoter == null || target == null) return false;
        if (!promoter.isLeader()) return false;
        if (target.getRole() != FactionRole.MEMBER) return false;
        
        faction.setRole(targetId, FactionRole.OFFICER);
        
        // LuckPerms 연동
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.updatePlayerRole(targetId, FactionRole.OFFICER);
        }
        
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 부대장 강등
     */
    public boolean demoteOfficer(UUID demoterId, UUID targetId) {
        Faction faction = dataManager.getPlayerFaction(demoterId);
        if (faction == null) return false;
        
        FactionMember demoter = faction.getMember(demoterId);
        FactionMember target = faction.getMember(targetId);
        
        if (demoter == null || target == null) return false;
        if (!demoter.isLeader()) return false;
        if (target.getRole() != FactionRole.OFFICER) return false;
        
        faction.setRole(targetId, FactionRole.MEMBER);
        
        // LuckPerms 연동
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.updatePlayerRole(targetId, FactionRole.MEMBER);
        }
        
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 대장 위임
     */
    public boolean transferLeadership(UUID currentLeaderId, UUID newLeaderId) {
        Faction faction = dataManager.getPlayerFaction(currentLeaderId);
        if (faction == null) return false;
        
        FactionMember currentLeader = faction.getMember(currentLeaderId);
        FactionMember newLeader = faction.getMember(newLeaderId);
        
        if (currentLeader == null || newLeader == null) return false;
        if (!currentLeader.isLeader()) return false;
        
        faction.setLeader(newLeaderId);
        
        // LuckPerms 연동
        LuckPermsManager lpm = plugin.getLuckPermsManager();
        if (lpm.isEnabled()) {
            lpm.updatePlayerRole(currentLeaderId, FactionRole.MEMBER);
            lpm.updatePlayerRole(newLeaderId, FactionRole.LEADER);
        }
        
        dataManager.scheduleSave();
        return true;
    }
    
    // ===== 업그레이드/강등 =====
    
    /**
     * 자동 업그레이드 체크
     */
    public void checkAutoUpgrade(Faction faction) {
        FactionTier currentTier = faction.getTier();
        FactionTier nextTier = currentTier.getNextTier();
        
        if (nextTier == null) return;
        
        // 조건 확인
        if (!canUpgrade(faction, nextTier)) return;
        
        // 비용 차감
        if (nextTier.getUpgradeCost() > 0) {
            faction.withdraw(nextTier.getUpgradeCost());
        }
        
        // 업그레이드
        faction.setTier(nextTier);
        
        // 알림
        broadcastToFaction(faction, plugin.getMessageManager().getPrefixedMessage(
            "upgrade.auto-upgrade", "%tier%", nextTier.getDisplayName()));
        
        dataManager.scheduleSave();
    }
    
    /**
     * 업그레이드 가능 여부 확인
     */
    public boolean canUpgrade(Faction faction, FactionTier targetTier) {
        // 인원 조건
        if (faction.getMemberCount() < targetTier.getMinMembers()) {
            return false;
        }
        
        // 비용 조건
        if (faction.getBalance() < targetTier.getUpgradeCost()) {
            return false;
        }
        
        // 국가 업그레이드: 모든 코어 2단계 이상
        if (targetTier == FactionTier.NATION) {
            int minCoreLevel = configManager.getNationMinCoreLevel();
            List<Core> cores = dataManager.getFactionCores(faction.getId());
            for (Core core : cores) {
                if (core.getLevel() < minCoreLevel) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 강등 조건 체크
     */
    public void checkDemotionCondition(Faction faction) {
        FactionTier currentTier = faction.getTier();
        int minMembers = configManager.getMinMembers(currentTier.getLevel());
        
        if (faction.getMemberCount() < minMembers) {
            // 강등 경고 설정
            if (!faction.isDemotionWarning()) {
                long gracePeriodMs = configManager.getDemotionGracePeriodHours() * 60 * 60 * 1000L;
                long deadline = System.currentTimeMillis() + gracePeriodMs;
                faction.setDemotionWarning(true, deadline);
                
                // 경고 메시지
                broadcastToFaction(faction, plugin.getMessageManager().getPrefixedMessage(
                    "demotion.warning", "%hours%", String.valueOf(configManager.getDemotionGracePeriodHours())));
                
                dataManager.scheduleSave();
            }
        }
    }
    
    /**
     * 강등 경고 해제 체크 (인원 회복)
     */
    public void checkDemotionRecovery(Faction faction) {
        if (!faction.isDemotionWarning()) return;
        
        FactionTier currentTier = faction.getTier();
        int minMembers = configManager.getMinMembers(currentTier.getLevel());
        
        if (faction.getMemberCount() >= minMembers) {
            faction.clearDemotionWarning();
            
            broadcastToFaction(faction, plugin.getMessageManager().getPrefixedMessage("demotion.cancelled"));
            dataManager.scheduleSave();
        }
    }
    
    /**
     * 강등 실행
     */
    public void executeDemotion(Faction faction) {
        FactionTier currentTier = faction.getTier();
        FactionTier previousTier = currentTier.getPreviousTier();
        
        if (previousTier == null) return;
        
        switch (currentTier) {
            case VILLAGE:
                executeVillageDemotion(faction);
                break;
            case CITY:
                executeCityDemotion(faction);
                break;
            case NATION:
                executeNationDemotion(faction);
                break;
        }
        
        faction.setTier(previousTier);
        faction.clearDemotionWarning();
        
        broadcastToFaction(faction, plugin.getMessageManager().getPrefixedMessage(
            "demotion.executed", "%tier%", previousTier.getDisplayName()));
        
        dataManager.scheduleSave();
    }
    
    private void executeVillageDemotion(Faction faction) {
        // 모든 코어 파괴
        int coreCount = faction.getCoreCount();
        for (String coreId : new ArrayList<>(faction.getCoreIds())) {
            plugin.getCoreManager().destroyCore(coreId);
            faction.removeCore(coreId);
        }
        
        // 보상 계산
        long coreRefund = coreCount * configManager.getVillageRefundPerCore();
        long accountRefund = (long) (faction.getBalance() * configManager.getVillageAccountRefundPercent() / 100.0);
        long feverBonus = faction.isFeverTimeActive() ? configManager.getVillageFeverBonus() : 0;
        
        long totalRefund = coreRefund + accountRefund + feverBonus;
        long perMember = faction.getMemberCount() > 0 ? totalRefund / faction.getMemberCount() : 0;
        
        // 각 세력원에게 분배 (실제로는 EconomyManager를 통해 처리)
        for (UUID memberId : faction.getMemberIds()) {
            plugin.getEconomyManager().addPlayerBalance(memberId, perMember);
        }
        
        // 세력 계좌 초기화
        faction.setBalance(0);
        faction.deactivateFeverTime();
    }
    
    private void executeCityDemotion(Faction faction) {
        // 세력 계좌에 4000쿤 반환
        faction.addBalance(configManager.getCityRefund());
        
        // 2단계 코어 → 1단계로 하향
        int downgradedCores = 0;
        for (String coreId : faction.getCoreIds()) {
            Core core = dataManager.getCore(coreId);
            if (core != null && core.getLevel() >= 2) {
                core.setLevel(1);
                downgradedCores++;
            }
        }
        
        // 코어당 500쿤 대장에게 지급
        long leaderRefund = downgradedCores * configManager.getCityDowngradeRefundPerCore();
        plugin.getEconomyManager().addPlayerBalance(faction.getLeaderId(), leaderRefund);
    }
    
    private void executeNationDemotion(Faction faction) {
        // 세력 계좌에 8000쿤 반환
        faction.addBalance(configManager.getNationRefund());
        
        // 3단계 코어 → 2단계로 하향
        int downgradedCores = 0;
        for (String coreId : faction.getCoreIds()) {
            Core core = dataManager.getCore(coreId);
            if (core != null && core.getLevel() >= 3) {
                core.setLevel(2);
                downgradedCores++;
            }
        }
        
        // 코어당 1000쿤 대장에게 지급
        long leaderRefund = downgradedCores * configManager.getNationDowngradeRefundPerCore();
        plugin.getEconomyManager().addPlayerBalance(faction.getLeaderId(), leaderRefund);
    }
    
    // ===== 유틸리티 =====
    
    /**
     * 세력원 전체에게 메시지 전송
     */
    public void broadcastToFaction(Faction faction, String message) {
        for (UUID memberId : faction.getMemberIds()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * 플레이어 세력 조회
     */
    public Faction getPlayerFaction(UUID playerId) {
        return dataManager.getPlayerFaction(playerId);
    }
    
    /**
     * 플레이어가 세력에 소속되어 있는지 확인
     */
    public boolean isInFaction(UUID playerId) {
        return dataManager.getPlayerFaction(playerId) != null;
    }
    
    /**
     * 세력 조회
     */
    public Faction getFaction(String factionId) {
        return dataManager.getFaction(factionId);
    }
    
    /**
     * 세력 이름으로 조회
     */
    public Faction getFactionByName(String name) {
        return dataManager.getFactionByName(name);
    }
    
    /**
     * 모든 세력 목록
     */
    public Collection<Faction> getAllFactions() {
        return dataManager.getAllFactions();
    }
}
