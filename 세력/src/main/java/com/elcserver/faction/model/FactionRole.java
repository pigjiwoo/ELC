package com.elcserver.faction.model;

/**
 * 세력 내 역할 열거형
 */
public enum FactionRole {
    
    LEADER("대장", 3),       // 세력 대장 - 최고 권한
    OFFICER("부대장", 2),    // 세력 부대장 - 보조 관리자
    MEMBER("세력원", 1);     // 일반 세력원
    
    private final String displayName;
    private final int level;
    
    FactionRole(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * 초대/추방 권한 확인
     */
    public boolean canInviteOrKick() {
        return this == LEADER || this == OFFICER;
    }
    
    /**
     * 코어 설치/회수 권한 확인
     */
    public boolean canManageCore() {
        return this == LEADER || this == OFFICER;
    }
    
    /**
     * 코어 업그레이드 권한 확인
     */
    public boolean canUpgradeCore() {
        return this == LEADER || this == OFFICER;
    }
    
    /**
     * 포인트 사용 권한 확인
     */
    public boolean canUsePoints() {
        return this == LEADER || this == OFFICER;
    }
    
    /**
     * 세력 업그레이드 권한 확인 (대장만)
     */
    public boolean canUpgradeFaction() {
        return this == LEADER;
    }
    
    /**
     * 권한 레벨 비교
     */
    public boolean isHigherThan(FactionRole other) {
        return this.level > other.level;
    }
    
    public boolean isHigherOrEqual(FactionRole other) {
        return this.level >= other.level;
    }
}
