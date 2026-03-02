package com.elcserver.faction.data;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

/**
 * MySQL 기반 데이터 저장소
 * DataManager 대체용
 */
public class MySQLDataManager {
    
    private final FactionCore plugin;
    private final DatabaseManager dbManager;
    
    // 캐시 (메모리 성능 최적화)
    private final Map<String, Faction> factionCache;
    private final Map<UUID, String> playerFactionCache;
    private final Map<String, Core> coreCache;
    private final Map<UUID, FactionInvite> inviteCache;
    
    private int factionIdCounter;
    private int coreIdCounter;
    
    public MySQLDataManager(FactionCore plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
        this.factionCache = new HashMap<>();
        this.playerFactionCache = new HashMap<>();
        this.coreCache = new HashMap<>();
        this.inviteCache = new HashMap<>();
        this.factionIdCounter = 0;
        this.coreIdCounter = 0;
    }
    
    // ===== 로드 =====
    
    public void loadAll() {
        loadFactions();
        loadCores();
        loadInvites();
        loadIdCounters();
        cleanExpiredInvites();
        plugin.getLogger().info("MySQL 데이터 로드 완료");
    }
    
    private void loadFactions() {
        String sql = "SELECT * FROM factions";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                UUID leaderId = UUID.fromString(rs.getString("leader_id"));
                String leaderName = rs.getString("leader_name");
                
                Faction faction = new Faction(id, name, leaderId, leaderName);
                faction.setIcon(rs.getString("icon"));
                faction.setTier(FactionTier.fromLevel(rs.getInt("tier")));
                faction.setBalance(rs.getLong("balance"));
                faction.setPoints(rs.getLong("points"));
                
                if (rs.getBoolean("fever_active")) {
                    long feverEnd = rs.getLong("fever_end");
                    if (feverEnd > System.currentTimeMillis()) {
                        faction.activateFeverTime(feverEnd - System.currentTimeMillis());
                    }
                }
                
                if (rs.getBoolean("demotion_warning")) {
                    faction.setDemotionWarning(true, rs.getLong("demotion_deadline"));
                }
                
                // 세력원 로드
                loadFactionMembers(faction);
                
                factionCache.put(id, faction);
            }
            
            plugin.getLogger().info("세력 " + factionCache.size() + "개 로드됨");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "세력 로드 실패!", e);
        }
    }
    
    private void loadFactionMembers(Faction faction) {
        String sql = "SELECT * FROM faction_members WHERE faction_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, faction.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerId = UUID.fromString(rs.getString("player_id"));
                    String playerName = rs.getString("player_name");
                    FactionRole role = FactionRole.valueOf(rs.getString("role"));
                    long joinTime = rs.getLong("join_time");
                    long lastOnline = rs.getLong("last_online");
                    
                    FactionMember member = new FactionMember(playerId, playerName, role);
                    member.setJoinTime(joinTime);
                    member.setLastOnline(lastOnline);
                    
                    faction.addMember(playerId, playerName, role);
                    playerFactionCache.put(playerId, faction.getId());
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "세력원 로드 실패: " + faction.getId(), e);
        }
    }
    
    private void loadCores() {
        String sql = "SELECT * FROM cores";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String id = rs.getString("id");
                String factionId = rs.getString("faction_id");
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                int level = rs.getInt("level");
                boolean installed = rs.getBoolean("installed");
                long installTime = rs.getLong("install_time");
                
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("코어 로드 실패 (월드 없음): " + id);
                    continue;
                }
                
                Location location = new Location(world, x, y, z);
                Core core = new Core(id, factionId, location);
                core.setLevel(level);
                core.setInstalled(installed);
                core.setInstallTime(installTime);
                
                coreCache.put(id, core);
                
                // 세력에 코어 연결
                Faction faction = factionCache.get(factionId);
                if (faction != null) {
                    faction.addCore(id);
                }
            }
            
            plugin.getLogger().info("코어 " + coreCache.size() + "개 로드됨");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "코어 로드 실패!", e);
        }
    }
    
    private void loadInvites() {
        String sql = "SELECT * FROM faction_invites WHERE expire_time > ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID inviteeId = UUID.fromString(rs.getString("invitee_id"));
                    String factionId = rs.getString("faction_id");
                    UUID inviterId = UUID.fromString(rs.getString("inviter_id"));
                    long inviteTime = rs.getLong("invite_time");
                    
                    FactionInvite invite = new FactionInvite(factionId, inviterId, inviteeId);
                    inviteCache.put(inviteeId, invite);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "초대 로드 실패!", e);
        }
    }
    
    private void loadIdCounters() {
        // 세력 ID 카운터
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)) as max_id FROM factions WHERE id LIKE 'F%'");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                factionIdCounter = rs.getInt("max_id");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "세력 ID 카운터 로드 실패", e);
        }
        
        // 코어 ID 카운터
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT MAX(CAST(SUBSTRING(id, 2) AS UNSIGNED)) as max_id FROM cores WHERE id LIKE 'C%'");
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                coreIdCounter = rs.getInt("max_id");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "코어 ID 카운터 로드 실패", e);
        }
    }
    
    private void cleanExpiredInvites() {
        String sql = "DELETE FROM faction_invites WHERE expire_time <= ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                plugin.getLogger().info("만료된 초대 " + deleted + "개 삭제됨");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "만료 초대 정리 실패", e);
        }
    }
    
    // ===== 세력 CRUD =====
    
    public String generateFactionId() {
        return "F" + (++factionIdCounter);
    }
    
    public void addFaction(Faction faction) {
        factionCache.put(faction.getId(), faction);
        
        String sql = """
            INSERT INTO factions (id, name, icon, leader_id, leader_name, tier, balance, points, 
                                  fever_active, fever_end, demotion_warning, demotion_deadline)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, faction.getId());
                stmt.setString(2, faction.getName());
                stmt.setString(3, faction.getIcon());
                stmt.setString(4, faction.getLeaderId().toString());
                // 대장 이름은 멤버에서 조회
                FactionMember leader = faction.getMember(faction.getLeaderId());
                stmt.setString(5, leader != null ? leader.getPlayerName() : "");
                stmt.setInt(6, faction.getTier().getLevel());
                stmt.setLong(7, faction.getBalance());
                stmt.setLong(8, faction.getPoints());
                stmt.setBoolean(9, faction.isFeverTimeActive());
                stmt.setLong(10, faction.getFeverTimeEnd());
                stmt.setBoolean(11, faction.isDemotionWarning());
                stmt.setLong(12, faction.getDemotionDeadline());
                
                stmt.executeUpdate();
                
                // 대장 멤버 추가
                FactionMember leaderMember = faction.getMember(faction.getLeaderId());
                addMember(faction.getLeaderId(), faction.getId(), leaderMember != null ? leaderMember.getPlayerName() : "", FactionRole.LEADER);
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "세력 저장 실패: " + faction.getId(), e);
            }
        });
    }
    
    public void updateFaction(Faction faction) {
        String sql = """
            UPDATE factions SET name = ?, icon = ?, leader_id = ?, leader_name = ?, 
                                tier = ?, balance = ?, points = ?, fever_active = ?, fever_end = ?,
                                demotion_warning = ?, demotion_deadline = ?
            WHERE id = ?
            """;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, faction.getName());
                stmt.setString(2, faction.getIcon());
                stmt.setString(3, faction.getLeaderId().toString());
                // 대장 이름은 멤버에서 조회
                FactionMember leader = faction.getMember(faction.getLeaderId());
                stmt.setString(4, leader != null ? leader.getPlayerName() : "");
                stmt.setInt(5, faction.getTier().getLevel());
                stmt.setLong(6, faction.getBalance());
                stmt.setLong(7, faction.getPoints());
                stmt.setBoolean(8, faction.isFeverTimeActive());
                stmt.setLong(9, faction.getFeverTimeEnd());
                stmt.setBoolean(10, faction.isDemotionWarning());
                stmt.setLong(11, faction.getDemotionDeadline());
                stmt.setString(12, faction.getId());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "세력 업데이트 실패: " + faction.getId(), e);
            }
        });
    }
    
    public void removeFaction(String factionId) {
        Faction faction = factionCache.remove(factionId);
        if (faction != null) {
            for (UUID memberId : faction.getMemberIds()) {
                playerFactionCache.remove(memberId);
            }
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM factions WHERE id = ?")) {
                
                stmt.setString(1, factionId);
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "세력 삭제 실패: " + factionId, e);
            }
        });
    }
    
    public Faction getFaction(String factionId) {
        return factionCache.get(factionId);
    }
    
    public Faction getFactionByName(String name) {
        return factionCache.values().stream()
                .filter(f -> f.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    public Collection<Faction> getAllFactions() {
        return factionCache.values();
    }
    
    // ===== 세력원 =====
    
    public void addMember(UUID playerId, String factionId, String playerName, FactionRole role) {
        playerFactionCache.put(playerId, factionId);
        
        String sql = """
            INSERT INTO faction_members (player_id, faction_id, player_name, role, join_time, last_online)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE faction_id = ?, player_name = ?, role = ?, last_online = ?
            """;
        
        long now = System.currentTimeMillis();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, factionId);
                stmt.setString(3, playerName);
                stmt.setString(4, role.name());
                stmt.setLong(5, now);
                stmt.setLong(6, now);
                stmt.setString(7, factionId);
                stmt.setString(8, playerName);
                stmt.setString(9, role.name());
                stmt.setLong(10, now);
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "세력원 추가 실패: " + playerId, e);
            }
        });
    }
    
    public void removeMember(UUID playerId) {
        playerFactionCache.remove(playerId);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM faction_members WHERE player_id = ?")) {
                
                stmt.setString(1, playerId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "세력원 삭제 실패: " + playerId, e);
            }
        });
    }
    
    public void updateMemberRole(UUID playerId, FactionRole role) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE faction_members SET role = ? WHERE player_id = ?")) {
                
                stmt.setString(1, role.name());
                stmt.setString(2, playerId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "역할 업데이트 실패: " + playerId, e);
            }
        });
    }
    
    public void updateMemberLastOnline(UUID playerId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("UPDATE faction_members SET last_online = ? WHERE player_id = ?")) {
                
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setString(2, playerId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "마지막 접속 업데이트 실패: " + playerId, e);
            }
        });
    }
    
    public Faction getPlayerFaction(UUID playerId) {
        String factionId = playerFactionCache.get(playerId);
        return factionId != null ? factionCache.get(factionId) : null;
    }
    
    public void setPlayerFaction(UUID playerId, String factionId) {
        if (factionId == null) {
            playerFactionCache.remove(playerId);
        } else {
            playerFactionCache.put(playerId, factionId);
        }
    }
    
    // ===== 코어 =====
    
    public String generateCoreId() {
        return "C" + (++coreIdCounter);
    }
    
    public void addCore(Core core) {
        coreCache.put(core.getId(), core);
        
        String sql = """
            INSERT INTO cores (id, faction_id, world, x, y, z, level, installed, install_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, core.getId());
                stmt.setString(2, core.getFactionId());
                stmt.setString(3, core.getLocation().getWorld().getName());
                stmt.setInt(4, core.getLocation().getBlockX());
                stmt.setInt(5, core.getLocation().getBlockY());
                stmt.setInt(6, core.getLocation().getBlockZ());
                stmt.setInt(7, core.getLevel());
                stmt.setBoolean(8, core.isInstalled());
                stmt.setLong(9, core.getInstalledTime());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "코어 저장 실패: " + core.getId(), e);
            }
        });
    }
    
    public void updateCore(Core core) {
        String sql = """
            UPDATE cores SET world = ?, x = ?, y = ?, z = ?, level = ?, installed = ?, install_time = ?
            WHERE id = ?
            """;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, core.getLocation().getWorld().getName());
                stmt.setInt(2, core.getLocation().getBlockX());
                stmt.setInt(3, core.getLocation().getBlockY());
                stmt.setInt(4, core.getLocation().getBlockZ());
                stmt.setInt(5, core.getLevel());
                stmt.setBoolean(6, core.isInstalled());
                stmt.setLong(7, core.getInstalledTime());
                stmt.setString(8, core.getId());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "코어 업데이트 실패: " + core.getId(), e);
            }
        });
    }
    
    public void removeCore(String coreId) {
        coreCache.remove(coreId);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM cores WHERE id = ?")) {
                
                stmt.setString(1, coreId);
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "코어 삭제 실패: " + coreId, e);
            }
        });
    }
    
    public Core getCore(String coreId) {
        return coreCache.get(coreId);
    }
    
    public Collection<Core> getAllCores() {
        return coreCache.values();
    }
    
    // ===== 초대 =====
    
    public void addInvite(FactionInvite invite) {
        inviteCache.put(invite.getInviteeId(), invite);
        
        String sql = """
            INSERT INTO faction_invites (invitee_id, faction_id, inviter_id, invite_time, expire_time)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE faction_id = ?, inviter_id = ?, invite_time = ?, expire_time = ?
            """;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, invite.getInviteeId().toString());
                stmt.setString(2, invite.getFactionId());
                stmt.setString(3, invite.getInviterId().toString());
                stmt.setLong(4, invite.getExpireTime() - 5 * 60 * 1000);
                stmt.setLong(5, invite.getExpireTime());
                stmt.setString(6, invite.getFactionId());
                stmt.setString(7, invite.getInviterId().toString());
                stmt.setLong(8, invite.getExpireTime() - 5 * 60 * 1000);
                stmt.setLong(9, invite.getExpireTime());
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "초대 저장 실패", e);
            }
        });
    }
    
    public void removeInvite(UUID inviteeId) {
        inviteCache.remove(inviteeId);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM faction_invites WHERE invitee_id = ?")) {
                
                stmt.setString(1, inviteeId.toString());
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "초대 삭제 실패", e);
            }
        });
    }
    
    public FactionInvite getInvite(UUID inviteeId) {
        FactionInvite invite = inviteCache.get(inviteeId);
        if (invite != null && invite.isExpired()) {
            removeInvite(inviteeId);
            return null;
        }
        return invite;
    }
    
    public boolean hasInvite(UUID inviteeId) {
        return getInvite(inviteeId) != null;
    }
    
    // ===== 인출 기록 =====
    
    public long getTodayWithdrawal(UUID playerId) {
        String sql = "SELECT SUM(amount) as total FROM withdrawal_logs WHERE player_id = ? AND withdraw_date = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerId.toString());
            stmt.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "인출 기록 조회 실패", e);
        }
        
        return 0;
    }
    
    public void addWithdrawalLog(UUID playerId, String factionId, long amount) {
        String sql = "INSERT INTO withdrawal_logs (player_id, faction_id, amount, withdraw_date) VALUES (?, ?, ?, ?)";
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, playerId.toString());
                stmt.setString(2, factionId);
                stmt.setLong(3, amount);
                stmt.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
                
                stmt.executeUpdate();
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "인출 기록 저장 실패", e);
            }
        });
    }
    
    // ===== 저장 (동기화) =====
    
    public void saveAll() {
        // 캐시의 모든 세력 저장
        for (Faction faction : factionCache.values()) {
            updateFaction(faction);
        }
        
        // 캐시의 모든 코어 저장
        for (Core core : coreCache.values()) {
            updateCore(core);
        }
        
        plugin.getLogger().info("MySQL 데이터 저장 완료");
    }
    
    public void scheduleSave() {
        // MySQL은 즉시 저장되므로 별도 스케줄링 불필요
    }
}
