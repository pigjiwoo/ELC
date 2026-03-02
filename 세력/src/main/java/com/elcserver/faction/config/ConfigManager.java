package com.elcserver.faction.config;

import com.elcserver.faction.FactionCore;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 설정 관리 클래스
 */
public class ConfigManager {
    
    private final FactionCore plugin;
    private FileConfiguration config;
    
    // 세력 단계별 조건
    private int villageMinMembers;
    private int cityMinMembers;
    private int nationMinMembers;
    private int cityUpgradeCost;
    private int nationUpgradeCost;
    private int nationMinCoreLevel;
    private long factionCreateCost;
    
    // 강등 설정
    private int demotionGracePeriodHours;
    private int villageRefundPerCore;
    private int villageAccountRefundPercent;
    private int villageFeverBonus;
    private int cityRefund;
    private int cityDowngradeRefundPerCore;
    private int nationRefund;
    private int nationDowngradeRefundPerCore;
    
    // 쿤 분할 설정
    private double kunPersonalRatio;
    private double kunFactionRatio;
    private double kunBaseMultiplier;
    private double kunCoreMultiplierBonus;
    private double kunMaxMultiplier;
    
    // 세금 설정
    private String taxExecutionTime;
    private double taxRatePerCore;
    
    // 인출 설정
    private boolean withdrawBlockSameDayJoin;
    private double withdrawLimitPercent;
    
    // 코어 설정
    private int coreInstallCooldownMinutes;
    private int corePurchaseInstallLimitMinutes;
    private int coreRetrieveReinstallLimitMinutes;
    private int coreRetrieveCost;
    private int coreInstallYLevel;
    private int coreInstallDelaySeconds;
    
    // 코어 범위
    private int coreRangeLevel1;
    private int coreRangeLevel2;
    private int coreRangeLevel3;
    
    // 코어 업그레이드 비용
    private int coreUpgradeCost1to2;
    private int coreUpgradeCost2to3;
    
    // 포인트 설정
    private double pointsEarnRatio;
    
    public ConfigManager(FactionCore plugin) {
        this.plugin = plugin;
        reload();
    }
    
    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadValues();
    }
    
    private void loadValues() {
        // 세력 단계별 조건
        villageMinMembers = config.getInt("faction-tiers.village.min-members", 4);
        cityMinMembers = config.getInt("faction-tiers.city.min-members", 9);
        nationMinMembers = config.getInt("faction-tiers.nation.min-members", 15);
        cityUpgradeCost = config.getInt("faction-tiers.city.upgrade-cost", 5000);
        nationUpgradeCost = config.getInt("faction-tiers.nation.upgrade-cost", 10000);
        nationMinCoreLevel = config.getInt("faction-tiers.nation.min-core-level", 2);
        factionCreateCost = config.getLong("faction.create-cost", 0);
        
        // 강등 설정
        demotionGracePeriodHours = config.getInt("demotion.grace-period-hours", 48);
        villageRefundPerCore = config.getInt("demotion.village-refund.core-refund-per-core", 1000);
        villageAccountRefundPercent = config.getInt("demotion.village-refund.account-refund-percent", 80);
        villageFeverBonus = config.getInt("demotion.village-refund.fever-bonus", 1000);
        cityRefund = config.getInt("demotion.city-refund.account-refund", 4000);
        cityDowngradeRefundPerCore = config.getInt("demotion.city-refund.core-downgrade-refund-per-core", 500);
        nationRefund = config.getInt("demotion.nation-refund.account-refund", 8000);
        nationDowngradeRefundPerCore = config.getInt("demotion.nation-refund.core-downgrade-refund-per-core", 1000);
        
        // 쿤 분할 설정
        kunPersonalRatio = config.getDouble("kun-distribution.personal-ratio", 0.7);
        kunFactionRatio = config.getDouble("kun-distribution.faction-ratio", 0.3);
        kunBaseMultiplier = config.getDouble("kun-distribution.base-multiplier", 1.5);
        kunCoreMultiplierBonus = config.getDouble("kun-distribution.core-multiplier-bonus", 0.05);
        kunMaxMultiplier = config.getDouble("kun-distribution.max-multiplier", 2.6);
        
        // 세금 설정
        taxExecutionTime = config.getString("tax.execution-time", "00:00");
        taxRatePerCore = config.getDouble("tax.rate-per-core", 0.8);
        
        // 인출 설정
        withdrawBlockSameDayJoin = config.getBoolean("withdrawal.block-same-day-join", true);
        withdrawLimitPercent = config.getDouble("withdrawal.limit-percent", 60);
        
        // 코어 설정
        coreInstallCooldownMinutes = config.getInt("core.install-cooldown-minutes", 20);
        corePurchaseInstallLimitMinutes = config.getInt("core.purchase-install-limit-minutes", 20);
        coreRetrieveReinstallLimitMinutes = config.getInt("core.retrieve-reinstall-limit-minutes", 20);
        coreRetrieveCost = config.getInt("core.retrieve-cost", 1000);
        coreInstallYLevel = config.getInt("core.install-y-level", 63);
        coreInstallDelaySeconds = config.getInt("core.install-delay-seconds", 3);
        
        // 코어 범위
        coreRangeLevel1 = config.getInt("core-ranges.level-1", 11);
        coreRangeLevel2 = config.getInt("core-ranges.level-2", 21);
        coreRangeLevel3 = config.getInt("core-ranges.level-3", 41);
        
        // 코어 업그레이드 비용
        coreUpgradeCost1to2 = config.getInt("core-upgrade-costs.1-to-2", 500);
        coreUpgradeCost2to3 = config.getInt("core-upgrade-costs.2-to-3", 1000);
        
        // 포인트 설정
        pointsEarnRatio = config.getDouble("points.earn-ratio", 1.0);
    }
    
    // ===== Getter 메소드들 =====
    
    public int getVillageMinMembers() { return villageMinMembers; }
    public int getCityMinMembers() { return cityMinMembers; }
    public int getNationMinMembers() { return nationMinMembers; }
    public int getCityUpgradeCost() { return cityUpgradeCost; }
    public int getNationUpgradeCost() { return nationUpgradeCost; }
    public int getNationMinCoreLevel() { return nationMinCoreLevel; }
    public long getFactionCreateCost() { return factionCreateCost; }
    
    public int getDemotionGracePeriodHours() { return demotionGracePeriodHours; }
    public int getVillageRefundPerCore() { return villageRefundPerCore; }
    public int getVillageAccountRefundPercent() { return villageAccountRefundPercent; }
    public int getVillageFeverBonus() { return villageFeverBonus; }
    public int getCityRefund() { return cityRefund; }
    public int getCityDowngradeRefundPerCore() { return cityDowngradeRefundPerCore; }
    public int getNationRefund() { return nationRefund; }
    public int getNationDowngradeRefundPerCore() { return nationDowngradeRefundPerCore; }
    
    public double getKunPersonalRatio() { return kunPersonalRatio; }
    public double getKunFactionRatio() { return kunFactionRatio; }
    public double getKunBaseMultiplier() { return kunBaseMultiplier; }
    public double getKunCoreMultiplierBonus() { return kunCoreMultiplierBonus; }
    public double getKunMaxMultiplier() { return kunMaxMultiplier; }
    
    public String getTaxExecutionTime() { return taxExecutionTime; }
    public double getTaxRatePerCore() { return taxRatePerCore; }
    
    public boolean isWithdrawBlockSameDayJoin() { return withdrawBlockSameDayJoin; }
    public double getWithdrawLimitPercent() { return withdrawLimitPercent; }
    
    public int getCoreInstallCooldownMinutes() { return coreInstallCooldownMinutes; }
    public int getCorePurchaseInstallLimitMinutes() { return corePurchaseInstallLimitMinutes; }
    public int getCoreRetrieveReinstallLimitMinutes() { return coreRetrieveReinstallLimitMinutes; }
    public int getCoreRetrieveCost() { return coreRetrieveCost; }
    public int getCoreInstallYLevel() { return coreInstallYLevel; }
    public int getCoreInstallDelaySeconds() { return coreInstallDelaySeconds; }
    
    public int getCoreRangeLevel1() { return coreRangeLevel1; }
    public int getCoreRangeLevel2() { return coreRangeLevel2; }
    public int getCoreRangeLevel3() { return coreRangeLevel3; }
    
    public int getCoreUpgradeCost1to2() { return coreUpgradeCost1to2; }
    public int getCoreUpgradeCost2to3() { return coreUpgradeCost2to3; }
    
    public double getPointsEarnRatio() { return pointsEarnRatio; }
    
    public int getCoreRange(int level) {
        switch (level) {
            case 1: return coreRangeLevel1;
            case 2: return coreRangeLevel2;
            case 3: return coreRangeLevel3;
            default: return coreRangeLevel1;
        }
    }
    
    public int getCoreUpgradeCost(int currentLevel) {
        switch (currentLevel) {
            case 1: return coreUpgradeCost1to2;
            case 2: return coreUpgradeCost2to3;
            default: return 0;
        }
    }
    
    public int getMinMembers(int tierLevel) {
        switch (tierLevel) {
            case 1: return villageMinMembers;
            case 2: return cityMinMembers;
            case 3: return nationMinMembers;
            default: return 1;
        }
    }
}
