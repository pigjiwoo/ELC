package com.elcserver.faction.model;

import java.util.*;

/**
 * 세력 데이터 클래스
 */
public class Faction {
    
    private final String id;
    private String name;
    private String icon;
    private FactionTier tier;
    private UUID leaderId;
    
    // 세력원 목록 (UUID -> FactionMember)
    private final Map<UUID, FactionMember> members;
    
    // 세력 코어 목록
    private final List<String> coreIds;
    
    // 경제
    private long balance;           // 세력 계좌 (쿤)
    private long points;            // 세력 포인트
    
    // 피버타임
    private boolean feverTimeActive;
    private long feverTimeEnd;
    
    // 강등 관련
    private boolean demotionWarning;
    private long demotionDeadline;
    
    // 생성 시간
    private final long createdTime;
    
    public Faction(String id, String name, UUID leaderId, String leaderName) {
        this.id = id;
        this.name = name;
        this.tier = FactionTier.MURI;
        this.leaderId = leaderId;
        this.members = new HashMap<>();
        this.coreIds = new ArrayList<>();
        this.balance = 0;
        this.points = 0;
        this.feverTimeActive = false;
        this.demotionWarning = false;
        this.createdTime = System.currentTimeMillis();
        
        // 대장 추가
        addMember(leaderId, leaderName, FactionRole.LEADER);
    }
    
    // ===== 세력원 관리 =====
    
    public void addMember(UUID playerId, String playerName, FactionRole role) {
        FactionMember member = new FactionMember(playerId, playerName, role);
        members.put(playerId, member);
    }
    
    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }
    
    public FactionMember getMember(UUID playerId) {
        return members.get(playerId);
    }
    
    public boolean hasMember(UUID playerId) {
        return members.containsKey(playerId);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public Collection<FactionMember> getMembers() {
        return members.values();
    }
    
    public Set<UUID> getMemberIds() {
        return members.keySet();
    }
    
    // ===== 역할 관리 =====
    
    public FactionRole getRole(UUID playerId) {
        FactionMember member = members.get(playerId);
        return member != null ? member.getRole() : null;
    }
    
    public void setRole(UUID playerId, FactionRole role) {
        FactionMember member = members.get(playerId);
        if (member != null) {
            member.setRole(role);
        }
    }
    
    public UUID getLeaderId() {
        return leaderId;
    }
    
    public void setLeader(UUID newLeaderId) {
        // 기존 대장 강등
        if (leaderId != null && members.containsKey(leaderId)) {
            members.get(leaderId).setRole(FactionRole.MEMBER);
        }
        // 새 대장 설정
        this.leaderId = newLeaderId;
        if (members.containsKey(newLeaderId)) {
            members.get(newLeaderId).setRole(FactionRole.LEADER);
        }
    }
    
    public List<UUID> getOfficers() {
        List<UUID> officers = new ArrayList<>();
        for (FactionMember member : members.values()) {
            if (member.getRole() == FactionRole.OFFICER) {
                officers.add(member.getPlayerId());
            }
        }
        return officers;
    }
    
    // ===== 코어 관리 =====
    
    public void addCore(String coreId) {
        if (!coreIds.contains(coreId)) {
            coreIds.add(coreId);
        }
    }
    
    public void removeCore(String coreId) {
        coreIds.remove(coreId);
    }
    
    public List<String> getCoreIds() {
        return new ArrayList<>(coreIds);
    }
    
    public int getCoreCount() {
        return coreIds.size();
    }
    
    public boolean hasCore(String coreId) {
        return coreIds.contains(coreId);
    }
    
    // ===== 경제 =====
    
    public long getBalance() {
        return balance;
    }
    
    public void setBalance(long balance) {
        this.balance = Math.max(0, balance);
    }
    
    public void addBalance(long amount) {
        this.balance += amount;
    }
    
    public boolean withdraw(long amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }
    
    public long getPoints() {
        return points;
    }
    
    public void setPoints(long points) {
        this.points = Math.max(0, points);
    }
    
    public void addPoints(long amount) {
        this.points += amount;
    }
    
    public boolean spendPoints(long amount) {
        if (points >= amount) {
            points -= amount;
            return true;
        }
        return false;
    }
    
    // ===== 세력 단계 =====
    
    public FactionTier getTier() {
        return tier;
    }
    
    public void setTier(FactionTier tier) {
        this.tier = tier;
    }
    
    public boolean canUpgrade() {
        FactionTier next = tier.getNextTier();
        if (next == null) return false;
        
        // 인원 조건 확인
        if (getMemberCount() < next.getMinMembers()) return false;
        
        // 비용 조건 확인
        if (balance < next.getUpgradeCost()) return false;
        
        return true;
    }
    
    // ===== 피버타임 =====
    
    public boolean isFeverTimeActive() {
        if (feverTimeActive && System.currentTimeMillis() > feverTimeEnd) {
            feverTimeActive = false;
        }
        return feverTimeActive;
    }
    
    public void activateFeverTime(long durationMs) {
        this.feverTimeActive = true;
        this.feverTimeEnd = System.currentTimeMillis() + durationMs;
    }
    
    public void deactivateFeverTime() {
        this.feverTimeActive = false;
    }
    
    public long getFeverTimeEnd() {
        return feverTimeEnd;
    }
    
    // ===== 강등 경고 =====
    
    public boolean isDemotionWarning() {
        return demotionWarning;
    }
    
    public void setDemotionWarning(boolean warning, long deadline) {
        this.demotionWarning = warning;
        this.demotionDeadline = deadline;
    }
    
    public void clearDemotionWarning() {
        this.demotionWarning = false;
        this.demotionDeadline = 0;
    }
    
    public long getDemotionDeadline() {
        return demotionDeadline;
    }
    
    // ===== 기타 Getter/Setter =====
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.icon = icon;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    /**
     * 인출 한도 계산
     * (세력 계좌 총액 ÷ 총 세력원) × 60%
     */
    public long calculateWithdrawLimit() {
        if (getMemberCount() == 0) return 0;
        return (long) ((balance / getMemberCount()) * 0.6);
    }
    
    /**
     * 쿤 배수 계산
     * 기본 1.5배 + 코어당 0.05배, 최대 2.6배
     */
    public double calculateKunMultiplier() {
        double multiplier = 1.5 + (getCoreCount() * 0.05);
        return Math.min(multiplier, 2.6);
    }
}
