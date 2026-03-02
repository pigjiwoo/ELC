package com.elcserver.faction.manager;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.config.ConfigManager;
import com.elcserver.faction.data.DataManager;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionMember;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 경제 관리 클래스 (Vault/EssentialsX 연동)
 * 쿤, 인출, 세금, 포인트 시스템 관리
 */
public class EconomyManager {
    
    private final FactionCore plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;
    
    // Vault Economy
    private Economy vaultEconomy;
    private boolean vaultEnabled;
    
    // 당일 인출 내역
    private final Map<UUID, Long> dailyWithdrawals;
    private long lastResetTime;
    
    public EconomyManager(FactionCore plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.configManager = plugin.getConfigManager();
        this.dailyWithdrawals = new HashMap<>();
        this.lastResetTime = System.currentTimeMillis();
        
        // Vault 연동 초기화
        setupVault();
    }
    
    /**
     * Vault Economy 연동 설정
     */
    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault 플러그인을 찾을 수 없습니다!");
            vaultEnabled = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Economy 프로바이더를 찾을 수 없습니다! (EssentialsX 등 경제 플러그인이 필요합니다)");
            vaultEnabled = false;
            return;
        }
        
        vaultEconomy = rsp.getProvider();
        vaultEnabled = true;
        plugin.getLogger().info("Vault Economy 연동 성공: " + vaultEconomy.getName());
    }
    
    /**
     * Vault 연동 상태 확인
     */
    public boolean isVaultEnabled() {
        return vaultEnabled && vaultEconomy != null;
    }
    
    /**
     * Economy 프로바이더 이름 반환
     */
    public String getEconomyName() {
        return vaultEnabled ? vaultEconomy.getName() : "없음";
    }
    
    // ===== 쿤 분할 시스템 =====
    
    /**
     * 쿤 획득 시 분배 처리
     * 개인:세력 = 7:3 비율 + 배수 적용
     */
    public void distributeKun(UUID playerId, long rawAmount) {
        Faction faction = dataManager.getPlayerFaction(playerId);
        
        if (faction == null) {
            // 개체: 전액 개인
            addPlayerBalance(playerId, rawAmount);
            return;
        }
        
        // 배수 계산
        double multiplier = faction.calculateKunMultiplier();
        if (faction.isFeverTimeActive()) {
            multiplier += 0.5; // 피버타임 추가 배수
        }
        
        long totalAmount = (long) (rawAmount * multiplier);
        
        // 분배 비율
        long personalAmount = (long) (totalAmount * configManager.getKunPersonalRatio());
        long factionAmount = (long) (totalAmount * configManager.getKunFactionRatio());
        
        // 개인 지급
        addPlayerBalance(playerId, personalAmount);
        
        // 세력 지급
        faction.addBalance(factionAmount);
        
        // 포인트 지급 (세력 입금액의 100%)
        long pointsEarned = (long) (factionAmount * configManager.getPointsEarnRatio());
        faction.addPoints(pointsEarned);
        
        dataManager.scheduleSave();
    }
    
    // ===== 세력 계좌 =====
    
    /**
     * 세력 계좌 입금
     */
    public boolean deposit(UUID playerId, long amount) {
        if (amount <= 0) return false;
        
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return false;
        
        if (!hasPlayerBalance(playerId, amount)) return false;
        
        removePlayerBalance(playerId, amount);
        faction.addBalance(amount);
        
        // 포인트도 지급
        long pointsEarned = (long) (amount * configManager.getPointsEarnRatio());
        faction.addPoints(pointsEarned);
        
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 세력 계좌 인출
     */
    public boolean withdraw(UUID playerId, long amount) {
        if (amount <= 0) return false;
        
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return false;
        
        FactionMember member = faction.getMember(playerId);
        if (member == null) return false;
        
        // 당일 가입자 인출 불가
        if (configManager.isWithdrawBlockSameDayJoin() && member.isJoinedToday()) {
            return false;
        }
        
        // 인출 한도 확인
        long limit = getWithdrawLimit(playerId);
        long alreadyWithdrawn = dailyWithdrawals.getOrDefault(playerId, 0L);
        
        if (alreadyWithdrawn + amount > limit) {
            return false;
        }
        
        // 세력 잔액 확인
        if (faction.getBalance() < amount) {
            return false;
        }
        
        // 인출 실행
        faction.withdraw(amount);
        addPlayerBalance(playerId, amount);
        dailyWithdrawals.put(playerId, alreadyWithdrawn + amount);
        
        dataManager.scheduleSave();
        return true;
    }
    
    /**
     * 인출 한도 계산
     */
    public long getWithdrawLimit(UUID playerId) {
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return 0;
        
        return faction.calculateWithdrawLimit();
    }
    
    /**
     * 남은 인출 가능 금액
     */
    public long getRemainingWithdrawLimit(UUID playerId) {
        long limit = getWithdrawLimit(playerId);
        long used = dailyWithdrawals.getOrDefault(playerId, 0L);
        return Math.max(0, limit - used);
    }
    
    // ===== 세금 시스템 =====
    
    /**
     * 세금 징수 (매일 00:00)
     */
    public void collectTax(Faction faction) {
        int coreCount = faction.getCoreCount();
        if (coreCount == 0) return;
        
        double taxRate = (configManager.getTaxRatePerCore() * coreCount) / 100.0;
        long taxAmount = (long) (faction.getBalance() * taxRate);
        
        if (taxAmount > 0) {
            faction.withdraw(taxAmount);
            
            // 알림
            plugin.getFactionManager().broadcastToFaction(faction,
                plugin.getMessageManager().getPrefixedMessage(
                    "account.tax-collected", "%amount%", String.valueOf(taxAmount)));
            
            dataManager.scheduleSave();
        }
    }
    
    /**
     * 인출 한도 초기화 (00:00, 전쟁 종료, 업그레이드 후)
     */
    public void resetWithdrawLimits() {
        dailyWithdrawals.clear();
        lastResetTime = System.currentTimeMillis();
    }
    
    // ===== Vault Economy 연동 - 개인 잔액 관리 =====
    
    /**
     * 플레이어 잔액 조회 (Vault)
     */
    public double getPlayerBalance(UUID playerId) {
        if (!isVaultEnabled()) {
            plugin.getLogger().warning("Vault가 연동되지 않았습니다!");
            return 0;
        }
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return vaultEconomy.getBalance(player);
    }
    
    /**
     * 플레이어 잔액 조회 (long 반환)
     */
    public long getPlayerBalanceLong(UUID playerId) {
        return (long) getPlayerBalance(playerId);
    }
    
    /**
     * 플레이어 잔액 설정 (Vault) - 주의: 현재 잔액을 고려하여 차액만큼 입/출금
     */
    public void setPlayerBalance(UUID playerId, long amount) {
        if (!isVaultEnabled()) return;
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        double current = vaultEconomy.getBalance(player);
        double diff = amount - current;
        
        if (diff > 0) {
            vaultEconomy.depositPlayer(player, diff);
        } else if (diff < 0) {
            vaultEconomy.withdrawPlayer(player, -diff);
        }
    }
    
    /**
     * 플레이어 잔액 추가 (Vault)
     */
    public void addPlayerBalance(UUID playerId, long amount) {
        if (!isVaultEnabled()) {
            plugin.getLogger().warning("Vault가 연동되지 않았습니다!");
            return;
        }
        
        if (amount <= 0) return;
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        EconomyResponse response = vaultEconomy.depositPlayer(player, amount);
        
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING, 
                "Vault 입금 실패: " + playerId + " - " + response.errorMessage);
        }
    }
    
    /**
     * 플레이어 잔액 차감 (Vault)
     */
    public boolean removePlayerBalance(UUID playerId, long amount) {
        if (!isVaultEnabled()) {
            plugin.getLogger().warning("Vault가 연동되지 않았습니다!");
            return false;
        }
        
        if (amount <= 0) return false;
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        
        // 잔액 확인
        if (vaultEconomy.getBalance(player) < amount) {
            return false;
        }
        
        EconomyResponse response = vaultEconomy.withdrawPlayer(player, amount);
        
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING, 
                "Vault 출금 실패: " + playerId + " - " + response.errorMessage);
            return false;
        }
        
        return true;
    }
    
    /**
     * 플레이어 잔액 확인 (Vault)
     */
    public boolean hasPlayerBalance(UUID playerId, long amount) {
        if (!isVaultEnabled()) return false;
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return vaultEconomy.has(player, amount);
    }
    
    // ===== 포인트 사용 =====
    
    /**
     * 포인트 사용
     */
    public boolean usePoints(UUID playerId, long amount) {
        Faction faction = dataManager.getPlayerFaction(playerId);
        if (faction == null) return false;
        
        FactionMember member = faction.getMember(playerId);
        if (member == null) return false;
        
        // 권한 확인
        if (!member.getRole().canUsePoints()) {
            return false;
        }
        
        // 포인트 확인 및 사용
        if (faction.spendPoints(amount)) {
            dataManager.scheduleSave();
            return true;
        }
        
        return false;
    }
    
    /**
     * 포인트 조회
     */
    public long getFactionPoints(UUID playerId) {
        Faction faction = dataManager.getPlayerFaction(playerId);
        return faction != null ? faction.getPoints() : 0;
    }
    
    // ===== 유틸리티 =====
    
    /**
     * 세력 잔액 조회
     */
    public long getFactionBalance(UUID playerId) {
        Faction faction = dataManager.getPlayerFaction(playerId);
        return faction != null ? faction.getBalance() : 0;
    }
    
    /**
     * 숫자 포맷 (1000 -> 1,000)
     */
    public String formatAmount(long amount) {
        return String.format("%,d", amount);
    }
    
    /**
     * 숫자 포맷 (double, 소수점 2자리)
     */
    public String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }
    
    /**
     * 통화 이름 반환
     */
    public String getCurrencyName() {
        if (!isVaultEnabled()) return "쿤";
        String name = vaultEconomy.currencyNamePlural();
        return (name == null || name.isEmpty()) ? "쿤" : name;
    }
    
    /**
     * 통화 이름 (단수)
     */
    public String getCurrencyNameSingular() {
        if (!isVaultEnabled()) return "쿤";
        String name = vaultEconomy.currencyNameSingular();
        return (name == null || name.isEmpty()) ? "쿤" : name;
    }
}
