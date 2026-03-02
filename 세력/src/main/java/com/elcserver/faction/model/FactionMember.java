package com.elcserver.faction.model;

import java.util.UUID;

/**
 * 세력원 정보 클래스
 */
public class FactionMember {
    
    private final UUID playerId;
    private String playerName;
    private FactionRole role;
    private long joinTime;
    private long lastOnline;
    
    public FactionMember(UUID playerId, String playerName, FactionRole role) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.role = role;
        this.joinTime = System.currentTimeMillis();
        this.lastOnline = System.currentTimeMillis();
    }
    
    // Getter/Setter
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public FactionRole getRole() {
        return role;
    }
    
    public void setRole(FactionRole role) {
        this.role = role;
    }
    
    public long getJoinTime() {
        return joinTime;
    }
    
    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }
    
    public long getLastOnline() {
        return lastOnline;
    }
    
    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }
    
    /**
     * 당일 가입 여부 확인
     */
    public boolean isJoinedToday() {
        long dayStart = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000));
        return joinTime >= dayStart;
    }
    
    /**
     * 대장 여부 확인
     */
    public boolean isLeader() {
        return role == FactionRole.LEADER;
    }
    
    /**
     * 부대장 이상 여부 확인
     */
    public boolean isOfficerOrHigher() {
        return role == FactionRole.LEADER || role == FactionRole.OFFICER;
    }
}
