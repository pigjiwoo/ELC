package com.elcserver.faction.model;

import java.util.UUID;

/**
 * 세력 초대 정보 클래스
 */
public class FactionInvite {
    
    private final String factionId;
    private final UUID inviterId;
    private final UUID inviteeId;
    private final long expireTime;
    
    // 초대 유효 시간: 5분
    private static final long INVITE_DURATION = 5 * 60 * 1000;
    
    public FactionInvite(String factionId, UUID inviterId, UUID inviteeId) {
        this.factionId = factionId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.expireTime = System.currentTimeMillis() + INVITE_DURATION;
    }
    
    public String getFactionId() {
        return factionId;
    }
    
    public UUID getInviterId() {
        return inviterId;
    }
    
    public UUID getInviteeId() {
        return inviteeId;
    }
    
    public long getExpireTime() {
        return expireTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
    
    public long getRemainingTime() {
        return Math.max(0, expireTime - System.currentTimeMillis());
    }
    
    public int getRemainingSeconds() {
        return (int) (getRemainingTime() / 1000);
    }
}
