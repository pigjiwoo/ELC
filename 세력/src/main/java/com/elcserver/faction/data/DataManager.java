package com.elcserver.faction.data;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * 데이터 저장/로드 관리 클래스
 */
public class DataManager {
    
    private final FactionCore plugin;
    private final File dataFolder;
    
    // 데이터 저장소
    private final Map<String, Faction> factions;          // factionId -> Faction
    private final Map<UUID, String> playerFactions;       // playerId -> factionId
    private final Map<String, Core> cores;                // coreId -> Core
    private final Map<UUID, FactionInvite> pendingInvites; // inviteeId -> Invite
    
    // 파일
    private File factionsFile;
    private File coresFile;
    private File playersFile;
    
    public DataManager(FactionCore plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.factions = new HashMap<>();
        this.playerFactions = new HashMap<>();
        this.cores = new HashMap<>();
        this.pendingInvites = new HashMap<>();
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        factionsFile = new File(dataFolder, "factions.yml");
        coresFile = new File(dataFolder, "cores.yml");
        playersFile = new File(dataFolder, "players.yml");
    }
    
    // ===== 로드/저장 =====
    
    public void loadAll() {
        loadFactions();
        loadCores();
        loadPlayers();
        plugin.getLogger().info("모든 데이터 로드 완료");
    }
    
    public void saveAll() {
        saveFactions();
        saveCores();
        savePlayers();
        plugin.getLogger().info("모든 데이터 저장 완료");
    }
    
    // ===== 세력 데이터 =====
    
    private void loadFactions() {
        if (!factionsFile.exists()) return;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(factionsFile);
        ConfigurationSection factionsSection = config.getConfigurationSection("factions");
        
        if (factionsSection == null) return;
        
        for (String factionId : factionsSection.getKeys(false)) {
            try {
                ConfigurationSection fs = factionsSection.getConfigurationSection(factionId);
                if (fs == null) continue;
                
                String name = fs.getString("name");
                UUID leaderId = UUID.fromString(fs.getString("leader"));
                String leaderName = fs.getString("leader-name", "Unknown");
                
                Faction faction = new Faction(factionId, name, leaderId, leaderName);
                faction.setIcon(fs.getString("icon"));
                faction.setTier(FactionTier.fromLevel(fs.getInt("tier", 0)));
                faction.setBalance(fs.getLong("balance", 0));
                faction.setPoints(fs.getLong("points", 0));
                
                // 피버타임
                if (fs.getBoolean("fever-active", false)) {
                    long feverEnd = fs.getLong("fever-end", 0);
                    if (feverEnd > System.currentTimeMillis()) {
                        faction.activateFeverTime(feverEnd - System.currentTimeMillis());
                    }
                }
                
                // 강등 경고
                if (fs.getBoolean("demotion-warning", false)) {
                    faction.setDemotionWarning(true, fs.getLong("demotion-deadline", 0));
                }
                
                // 세력원 로드
                ConfigurationSection membersSection = fs.getConfigurationSection("members");
                if (membersSection != null) {
                    for (String uuidStr : membersSection.getKeys(false)) {
                        ConfigurationSection ms = membersSection.getConfigurationSection(uuidStr);
                        if (ms == null) continue;
                        
                        UUID playerId = UUID.fromString(uuidStr);
                        String playerName = ms.getString("name", "Unknown");
                        FactionRole role = FactionRole.valueOf(ms.getString("role", "MEMBER"));
                        
                        FactionMember member = new FactionMember(playerId, playerName, role);
                        member.setJoinTime(ms.getLong("join-time", System.currentTimeMillis()));
                        member.setLastOnline(ms.getLong("last-online", System.currentTimeMillis()));
                        
                        faction.removeMember(playerId); // 중복 방지
                        faction.addMember(playerId, playerName, role);
                    }
                }
                
                // 코어 ID 로드
                List<String> coreIds = fs.getStringList("cores");
                for (String coreId : coreIds) {
                    faction.addCore(coreId);
                }
                
                factions.put(factionId, faction);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "세력 로드 실패: " + factionId, e);
            }
        }
        
        plugin.getLogger().info("세력 " + factions.size() + "개 로드됨");
    }
    
    public void saveFactions() {
        YamlConfiguration config = new YamlConfiguration();
        
        for (Faction faction : factions.values()) {
            String path = "factions." + faction.getId();
            
            config.set(path + ".name", faction.getName());
            config.set(path + ".icon", faction.getIcon());
            config.set(path + ".leader", faction.getLeaderId().toString());
            config.set(path + ".tier", faction.getTier().getLevel());
            config.set(path + ".balance", faction.getBalance());
            config.set(path + ".points", faction.getPoints());
            config.set(path + ".fever-active", faction.isFeverTimeActive());
            config.set(path + ".fever-end", faction.getFeverTimeEnd());
            config.set(path + ".demotion-warning", faction.isDemotionWarning());
            config.set(path + ".demotion-deadline", faction.getDemotionDeadline());
            config.set(path + ".cores", faction.getCoreIds());
            
            // 세력원 저장
            for (FactionMember member : faction.getMembers()) {
                String memberPath = path + ".members." + member.getPlayerId().toString();
                config.set(memberPath + ".name", member.getPlayerName());
                config.set(memberPath + ".role", member.getRole().name());
                config.set(memberPath + ".join-time", member.getJoinTime());
                config.set(memberPath + ".last-online", member.getLastOnline());
            }
        }
        
        try {
            config.save(factionsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "세력 저장 실패", e);
        }
    }
    
    // ===== 코어 데이터 =====
    
    private void loadCores() {
        if (!coresFile.exists()) return;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(coresFile);
        ConfigurationSection coresSection = config.getConfigurationSection("cores");
        
        if (coresSection == null) return;
        
        for (String coreId : coresSection.getKeys(false)) {
            try {
                ConfigurationSection cs = coresSection.getConfigurationSection(coreId);
                if (cs == null) continue;
                
                String factionId = cs.getString("faction");
                String worldName = cs.getString("world");
                int x = cs.getInt("x");
                int y = cs.getInt("y");
                int z = cs.getInt("z");
                
                org.bukkit.World world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("코어 로드 실패 (월드 없음): " + coreId);
                    continue;
                }
                
                org.bukkit.Location location = new org.bukkit.Location(world, x, y, z);
                Core core = new Core(coreId, factionId, location);
                core.setLevel(cs.getInt("level", 1));
                core.setInstalledTime(cs.getLong("installed-time", System.currentTimeMillis()));
                core.setLastRetrievedTime(cs.getLong("last-retrieved-time", 0));
                core.setRegisteredSlot(cs.getInt("registered-slot", -1));
                
                cores.put(coreId, core);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "코어 로드 실패: " + coreId, e);
            }
        }
        
        plugin.getLogger().info("코어 " + cores.size() + "개 로드됨");
    }
    
    public void saveCores() {
        YamlConfiguration config = new YamlConfiguration();
        
        for (Core core : cores.values()) {
            String path = "cores." + core.getId();
            
            config.set(path + ".faction", core.getFactionId());
            config.set(path + ".world", core.getWorldName());
            config.set(path + ".x", core.getX());
            config.set(path + ".y", core.getY());
            config.set(path + ".z", core.getZ());
            config.set(path + ".level", core.getLevel());
            config.set(path + ".installed-time", core.getInstalledTime());
            config.set(path + ".last-retrieved-time", core.getLastRetrievedTime());
            config.set(path + ".registered-slot", core.getRegisteredSlot());
        }
        
        try {
            config.save(coresFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "코어 저장 실패", e);
        }
    }
    
    // ===== 플레이어 데이터 =====
    
    private void loadPlayers() {
        if (!playersFile.exists()) return;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playersFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        
        if (playersSection == null) return;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(uuidStr);
                String factionId = playersSection.getString(uuidStr);
                if (factionId != null && !factionId.isEmpty()) {
                    playerFactions.put(playerId, factionId);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "플레이어 데이터 로드 실패: " + uuidStr, e);
            }
        }
        
        plugin.getLogger().info("플레이어 " + playerFactions.size() + "명 로드됨");
    }
    
    public void savePlayers() {
        YamlConfiguration config = new YamlConfiguration();
        
        for (Map.Entry<UUID, String> entry : playerFactions.entrySet()) {
            config.set("players." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            config.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "플레이어 저장 실패", e);
        }
    }
    
    // ===== 세력 관리 =====
    
    public Faction getFaction(String factionId) {
        return factions.get(factionId);
    }
    
    public Faction getFactionByName(String name) {
        for (Faction faction : factions.values()) {
            if (faction.getName().equalsIgnoreCase(name)) {
                return faction;
            }
        }
        return null;
    }
    
    public Faction getPlayerFaction(UUID playerId) {
        String factionId = playerFactions.get(playerId);
        return factionId != null ? factions.get(factionId) : null;
    }
    
    public void addFaction(Faction faction) {
        factions.put(faction.getId(), faction);
        for (UUID memberId : faction.getMemberIds()) {
            playerFactions.put(memberId, faction.getId());
        }
    }
    
    public void removeFaction(String factionId) {
        Faction faction = factions.remove(factionId);
        if (faction != null) {
            for (UUID memberId : faction.getMemberIds()) {
                playerFactions.remove(memberId);
            }
            // 코어도 삭제
            for (String coreId : faction.getCoreIds()) {
                cores.remove(coreId);
            }
        }
    }
    
    public Collection<Faction> getAllFactions() {
        return factions.values();
    }
    
    public boolean factionExists(String name) {
        return getFactionByName(name) != null;
    }
    
    public void setPlayerFaction(UUID playerId, String factionId) {
        if (factionId == null) {
            playerFactions.remove(playerId);
        } else {
            playerFactions.put(playerId, factionId);
        }
    }
    
    // ===== 코어 관리 =====
    
    public Core getCore(String coreId) {
        return cores.get(coreId);
    }
    
    public void addCore(Core core) {
        cores.put(core.getId(), core);
    }
    
    public void removeCore(String coreId) {
        cores.remove(coreId);
    }
    
    public Collection<Core> getAllCores() {
        return cores.values();
    }
    
    public List<Core> getFactionCores(String factionId) {
        List<Core> factionCores = new ArrayList<>();
        for (Core core : cores.values()) {
            if (core.getFactionId().equals(factionId)) {
                factionCores.add(core);
            }
        }
        return factionCores;
    }
    
    public String generateCoreId() {
        return "core_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
    }
    
    public String generateFactionId() {
        return "faction_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
    }
    
    // ===== 초대 관리 =====
    
    public void addInvite(FactionInvite invite) {
        pendingInvites.put(invite.getInviteeId(), invite);
    }
    
    public FactionInvite getInvite(UUID inviteeId) {
        FactionInvite invite = pendingInvites.get(inviteeId);
        if (invite != null && invite.isExpired()) {
            pendingInvites.remove(inviteeId);
            return null;
        }
        return invite;
    }
    
    public void removeInvite(UUID inviteeId) {
        pendingInvites.remove(inviteeId);
    }
    
    public boolean hasInvite(UUID inviteeId) {
        return getInvite(inviteeId) != null;
    }
    
    // ===== 자동 저장 =====
    
    public void scheduleSave() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }
}
