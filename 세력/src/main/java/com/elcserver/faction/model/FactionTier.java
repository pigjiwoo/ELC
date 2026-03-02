package com.elcserver.faction.model;

/**
 * 세력 단계 열거형
 * 개체(1) → 무리(2-3) → 촌락(4-5) → 마을(6-8) → 도시(9-14) → 국가(15-30) → 황국(31+)
 */
public enum FactionTier {
    
    // 티어(표시명, 레벨, 최소인원, 최대인원, 업그레이드비용, 최대코어수, 최대코어레벨)
    INDIVIDUAL("개체", 0, 1, 1, 0, 0, 0),        // 세력 미소속
    MURI("무리", 1, 2, 3, 0, 0, 0),              // 무리
    VILLAGE("촌락", 2, 4, 5, 0, 4, 1),           // 촌락
    TOWN("마을", 3, 6, 8, 0, 8, 1),              // 마을
    CITY("도시", 4, 9, 14, 5000, 9, 2),          // 도시
    NATION("국가", 5, 15, 30, 10000, 16, 3),     // 국가
    EMPIRE("황국", 6, 31, Integer.MAX_VALUE, 20000, 36, 3);  // 황국
    
    private final String displayName;
    private final int level;
    private final int minMembers;
    private final int maxMembers;
    private final int upgradeCost;
    private final int maxCores;
    private final int maxCoreLevel;
    
    FactionTier(String displayName, int level, int minMembers, int maxMembers, 
                int upgradeCost, int maxCores, int maxCoreLevel) {
        this.displayName = displayName;
        this.level = level;
        this.minMembers = minMembers;
        this.maxMembers = maxMembers;
        this.upgradeCost = upgradeCost;
        this.maxCores = maxCores;
        this.maxCoreLevel = maxCoreLevel;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getMinMembers() {
        return minMembers;
    }
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public int getUpgradeCost() {
        return upgradeCost;
    }
    
    public int getMaxCores() {
        return maxCores;
    }
    
    public int getMaxCoreLevel() {
        return maxCoreLevel;
    }
    
    /**
     * 다음 단계 반환
     */
    public FactionTier getNextTier() {
        switch (this) {
            case INDIVIDUAL: return MURI;
            case MURI: return VILLAGE;
            case VILLAGE: return TOWN;
            case TOWN: return CITY;
            case CITY: return NATION;
            case NATION: return EMPIRE;
            default: return null;
        }
    }
    
    /**
     * 이전 단계 반환 (강등용)
     */
    public FactionTier getPreviousTier() {
        switch (this) {
            case EMPIRE: return NATION;
            case NATION: return CITY;
            case CITY: return TOWN;
            case TOWN: return VILLAGE;
            case VILLAGE: return MURI;
            case MURI: return INDIVIDUAL;
            default: return null;
        }
    }
    
    /**
     * 레벨로 단계 찾기
     */
    public static FactionTier fromLevel(int level) {
        for (FactionTier tier : values()) {
            if (tier.getLevel() == level) {
                return tier;
            }
        }
        return INDIVIDUAL;
    }
    
    /**
     * 인원수로 적합한 티어 찾기
     */
    public static FactionTier fromMemberCount(int memberCount) {
        if (memberCount <= 1) return INDIVIDUAL;
        if (memberCount <= 3) return MURI;
        if (memberCount <= 5) return VILLAGE;
        if (memberCount <= 8) return TOWN;
        if (memberCount <= 14) return CITY;
        if (memberCount <= 30) return NATION;
        return EMPIRE;
    }
    
    /**
     * 코어 설치 가능 여부
     */
    public boolean canInstallCore() {
        return maxCores > 0;
    }
    
    /**
     * 특정 코어 레벨로 업그레이드 가능 여부
     */
    public boolean canUpgradeCoreToLevel(int targetLevel) {
        return targetLevel <= maxCoreLevel;
    }
}
