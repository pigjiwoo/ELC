package com.elcserver.faction.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

/**
 * 코어 데이터 클래스
 */
public class Core {
    
    private final String id;
    private String factionId;
    private int level;
    
    // 위치 정보
    private String worldName;
    private int x;
    private int y;
    private int z;
    
    // 시간 정보
    private long installedTime;
    private long lastRetrievedTime;
    
    // 등록 슬롯 (-1: 미등록)
    private int registeredSlot;
    
    // 등록 이름 (null: 미등록, "코어 1", "코어 2" 등)
    private String registeredName;
    
    // 설치 상태
    private boolean installed;
    
    public Core(String id, String factionId, Location location) {
        this.id = id;
        this.factionId = factionId;
        this.level = 1;
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.installedTime = System.currentTimeMillis();
        this.lastRetrievedTime = 0;
        this.registeredSlot = -1;
        this.registeredName = null;
        this.installed = true;
    }
    
    // ===== 위치 관련 =====
    
    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }
    
    public void setLocation(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.installedTime = System.currentTimeMillis();
    }
    
    /**
     * 텔레포트 위치 (코어 바로 위)
     */
    public Location getTeleportLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }
    
    // ===== 범위 계산 =====
    
    /**
     * 코어 단계별 범위 반환
     */
    public int getRange() {
        switch (level) {
            case 1: return 11;
            case 2: return 21;
            case 3: return 41;
            default: return 11;
        }
    }
    
    /**
     * 범위 절반 (중심에서 거리)
     */
    public int getHalfRange() {
        return getRange() / 2;
    }
    
    /**
     * 특정 위치가 코어 범위 내에 있는지 확인 (Y축 무시)
     */
    public boolean isInRange(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        int halfRange = getHalfRange();
        int dx = Math.abs(location.getBlockX() - x);
        int dz = Math.abs(location.getBlockZ() - z);
        
        return dx <= halfRange && dz <= halfRange;
    }
    
    /**
     * 다른 코어와 범위가 겹치는지 확인
     */
    public boolean overlaps(Core other) {
        if (!this.worldName.equals(other.worldName)) {
            return false;
        }
        
        int thisHalf = this.getHalfRange();
        int otherHalf = other.getHalfRange();
        
        int dx = Math.abs(this.x - other.x);
        int dz = Math.abs(this.z - other.z);
        
        return dx < (thisHalf + otherHalf) && dz < (thisHalf + otherHalf);
    }
    
    /**
     * 업그레이드 후 다른 코어와 겹치는지 미리 확인
     */
    public boolean wouldOverlapAfterUpgrade(Core other) {
        if (!this.worldName.equals(other.worldName)) {
            return false;
        }
        
        // 업그레이드 후 범위
        int nextHalf = getNextLevelHalfRange();
        int otherHalf = other.getHalfRange();
        
        int dx = Math.abs(this.x - other.x);
        int dz = Math.abs(this.z - other.z);
        
        return dx < (nextHalf + otherHalf) && dz < (nextHalf + otherHalf);
    }
    
    private int getNextLevelHalfRange() {
        switch (level) {
            case 1: return 21 / 2;  // 2단계
            case 2: return 41 / 2;  // 3단계
            default: return getHalfRange();
        }
    }
    
    // ===== 업그레이드 =====
    
    public boolean canUpgrade() {
        return level < 3;
    }
    
    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }
    
    public void downgrade() {
        if (level > 1) {
            level--;
        }
    }
    
    public int getUpgradeCost() {
        switch (level) {
            case 1: return 500;   // 1 -> 2
            case 2: return 1000;  // 2 -> 3
            default: return 0;
        }
    }
    
    // ===== 쿨다운 =====
    
    /**
     * 설치 후 회수 가능 여부 (20분 쿨다운)
     */
    public boolean canRetrieve() {
        long cooldown = 20 * 60 * 1000; // 20분
        return System.currentTimeMillis() - installedTime >= cooldown;
    }
    
    /**
     * 회수까지 남은 시간 (분)
     */
    public int getRetrieveCooldownMinutes() {
        long cooldown = 20 * 60 * 1000;
        long elapsed = System.currentTimeMillis() - installedTime;
        if (elapsed >= cooldown) return 0;
        return (int) ((cooldown - elapsed) / (60 * 1000));
    }
    
    // ===== 등록 =====
    
    public boolean isRegistered() {
        return registeredSlot > 0;
    }
    
    public int getRegisteredSlot() {
        return registeredSlot;
    }
    
    public void setRegisteredSlot(int slot) {
        this.registeredSlot = slot;
    }
    
    public void unregister() {
        this.registeredSlot = -1;
        // registeredName은 유지 (변경 불가)
    }
    
    // ===== 등록 이름 =====
    
    public String getRegisteredName() {
        return registeredName;
    }
    
    public void setRegisteredName(String name) {
        // 한 번 등록되면 변경 불가
        if (this.registeredName == null) {
            this.registeredName = name;
        }
    }
    
    public boolean hasRegisteredName() {
        return registeredName != null && !registeredName.isEmpty();
    }
    
    // ===== 설치 상태 =====
    
    public boolean isInstalled() {
        return installed;
    }
    
    public void setInstalled(boolean installed) {
        this.installed = installed;
        if (installed) {
            this.installedTime = System.currentTimeMillis();
        }
    }
    
    public void setInstallTime(long time) {
        this.installedTime = time;
    }
    
    // ===== Getter/Setter =====
    
    public String getId() {
        return id;
    }
    
    public String getFactionId() {
        return factionId;
    }
    
    public void setFactionId(String factionId) {
        this.factionId = factionId;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(3, level));
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getZ() {
        return z;
    }
    
    public long getInstalledTime() {
        return installedTime;
    }
    
    public void setInstalledTime(long installedTime) {
        this.installedTime = installedTime;
    }
    
    public long getLastRetrievedTime() {
        return lastRetrievedTime;
    }
    
    public void setLastRetrievedTime(long lastRetrievedTime) {
        this.lastRetrievedTime = lastRetrievedTime;
    }
}
