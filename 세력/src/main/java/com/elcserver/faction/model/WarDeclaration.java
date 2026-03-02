package com.elcserver.faction.model;

import java.util.UUID;

/**
 * 격문 (전쟁 선언서) 데이터 클래스
 * 
 * 격문 작성 → 30분 후 선전포고 → 1시간 후 전쟁 시작
 */
public class WarDeclaration {
    
    /**
     * 격문 상태
     */
    public enum WarPhase {
        PROCLAMATION("격문 작성됨"),     // 격문 작성 직후 (30분 대기)
        DECLARATION("선전포고"),         // 선전포고 상태 (추가 30분 대기)
        WAR("전쟁 중"),                  // 전쟁 진행 중
        ENDED("종료");                   // 전쟁 종료
        
        private final String displayName;
        
        WarPhase(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 격문 종류
     */
    public enum DeclarationType {
        OLD("낡은 격문", "촌락, 마을 사이"),         // 촌락, 마을
        NORMAL("격문", "도시, 국가, 황국 사이");      // 도시, 국가, 황국
        
        private final String displayName;
        private final String description;
        
        DeclarationType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final String id;
    private final String attackerFactionId;     // 격문 작성 세력
    private final String defenderFactionId;     // 상대 세력
    private final UUID authorId;                // 작성자 UUID
    private final DeclarationType type;         // 격문 종류
    private final long createdTime;             // 격문 작성 시간
    
    private WarPhase phase;                     // 현재 단계
    private boolean declarationNotified;        // 선전포고 알림 전송 여부
    private boolean warStartNotified;           // 전쟁 시작 알림 전송 여부
    
    // 30분 = 1,800,000ms, 1시간 = 3,600,000ms
    private static final long DECLARATION_DELAY_MS = 30 * 60 * 1000L;   // 격문 → 선전포고 (30분)
    private static final long WAR_DELAY_MS = 60 * 60 * 1000L;           // 격문 → 전쟁 시작 (1시간)
    
    public WarDeclaration(String id, String attackerFactionId, String defenderFactionId, 
                          UUID authorId, DeclarationType type) {
        this.id = id;
        this.attackerFactionId = attackerFactionId;
        this.defenderFactionId = defenderFactionId;
        this.authorId = authorId;
        this.type = type;
        this.createdTime = System.currentTimeMillis();
        this.phase = WarPhase.PROCLAMATION;
        this.declarationNotified = false;
        this.warStartNotified = false;
    }
    
    /**
     * 복원용 생성자
     */
    public WarDeclaration(String id, String attackerFactionId, String defenderFactionId,
                          UUID authorId, DeclarationType type, long createdTime, 
                          WarPhase phase, boolean declarationNotified, boolean warStartNotified) {
        this.id = id;
        this.attackerFactionId = attackerFactionId;
        this.defenderFactionId = defenderFactionId;
        this.authorId = authorId;
        this.type = type;
        this.createdTime = createdTime;
        this.phase = phase;
        this.declarationNotified = declarationNotified;
        this.warStartNotified = warStartNotified;
    }
    
    // ===== 시간 계산 =====
    
    /**
     * 선전포고까지 남은 시간 (ms)
     */
    public long getTimeUntilDeclaration() {
        long target = createdTime + DECLARATION_DELAY_MS;
        long remaining = target - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * 전쟁 시작까지 남은 시간 (ms)
     */
    public long getTimeUntilWar() {
        long target = createdTime + WAR_DELAY_MS;
        long remaining = target - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * 선전포고 시간 도래 여부
     */
    public boolean isDeclarationTime() {
        return System.currentTimeMillis() >= createdTime + DECLARATION_DELAY_MS;
    }
    
    /**
     * 전쟁 시작 시간 도래 여부
     */
    public boolean isWarTime() {
        return System.currentTimeMillis() >= createdTime + WAR_DELAY_MS;
    }
    
    /**
     * 격문이 30분 이내인지 (격문 작성소 표시용)
     */
    public boolean isRecentProclamation() {
        return System.currentTimeMillis() - createdTime < DECLARATION_DELAY_MS;
    }
    
    /**
     * 격문 작성 후 경과 시간 (ms)
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - createdTime;
    }
    
    // ===== 단계 관리 =====
    
    /**
     * 현재 단계 자동 업데이트 및 반환
     */
    public WarPhase updateAndGetPhase() {
        if (phase == WarPhase.ENDED) return phase;
        
        if (isWarTime()) {
            phase = WarPhase.WAR;
        } else if (isDeclarationTime()) {
            phase = WarPhase.DECLARATION;
        }
        
        return phase;
    }
    
    // ===== 티어 검증 =====
    
    /**
     * 해당 세력 티어에서 이 격문을 작성할 수 있는지 검증
     */
    public static boolean canDeclare(FactionTier attackerTier, FactionTier defenderTier) {
        // 촌락 미만은 격문 불가
        if (attackerTier.getLevel() < FactionTier.VILLAGE.getLevel()) return false;
        if (defenderTier.getLevel() < FactionTier.VILLAGE.getLevel()) return false;
        
        // 같은 격문 유형 범위 내에서만 가능
        DeclarationType attackerType = getDeclarationType(attackerTier);
        DeclarationType defenderType = getDeclarationType(defenderTier);
        
        return attackerType == defenderType;
    }
    
    /**
     * 세력 티어에 맞는 격문 유형 반환
     */
    public static DeclarationType getDeclarationType(FactionTier tier) {
        switch (tier) {
            case VILLAGE:
            case TOWN:
                return DeclarationType.OLD;
            case CITY:
            case NATION:
            case EMPIRE:
                return DeclarationType.NORMAL;
            default:
                return null;
        }
    }
    
    // ===== Getter/Setter =====
    
    public String getId() { return id; }
    public String getAttackerFactionId() { return attackerFactionId; }
    public String getDefenderFactionId() { return defenderFactionId; }
    public UUID getAuthorId() { return authorId; }
    public DeclarationType getType() { return type; }
    public long getCreatedTime() { return createdTime; }
    public WarPhase getPhase() { return phase; }
    
    public void setPhase(WarPhase phase) { this.phase = phase; }
    
    public boolean isDeclarationNotified() { return declarationNotified; }
    public void setDeclarationNotified(boolean notified) { this.declarationNotified = notified; }
    
    public boolean isWarStartNotified() { return warStartNotified; }
    public void setWarStartNotified(boolean notified) { this.warStartNotified = notified; }
    
    public static long getDeclarationDelayMs() { return DECLARATION_DELAY_MS; }
    public static long getWarDelayMs() { return WAR_DELAY_MS; }
}
