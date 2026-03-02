package com.elcserver.faction.manager;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.data.DataManager;
import com.elcserver.faction.model.*;
import com.elcserver.faction.util.DiscordWebhook;
import com.elcserver.faction.util.FactionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 전쟁 시스템 관리 클래스
 * 
 * 격문 작성 → 30분 후 선전포고 → 1시간 후 전쟁 시작
 */
public class WarManager {
    
    private final FactionCore plugin;
    private final DataManager dataManager;
    
    // 활성 격문 목록 (declarationId -> WarDeclaration)
    private final Map<String, WarDeclaration> activeDeclarations;
    
    // 격문 작성소 위치 (config에서 로드)
    private Location proclamationStationLocation;
    
    // Discord 알림
    private final DiscordWebhook discordWebhook;
    
    public WarManager(FactionCore plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.activeDeclarations = new ConcurrentHashMap<>();
        this.discordWebhook = new DiscordWebhook(plugin);
        
        loadProclamationStation();
    }
    
    /**
     * 격문 작성소 위치 로드
     */
    private void loadProclamationStation() {
        String worldName = plugin.getConfig().getString("war.proclamation-station.world", "world");
        int x = plugin.getConfig().getInt("war.proclamation-station.x", 0);
        int y = plugin.getConfig().getInt("war.proclamation-station.y", 64);
        int z = plugin.getConfig().getInt("war.proclamation-station.z", 0);
        
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world != null) {
            proclamationStationLocation = new Location(world, x, y, z);
        }
    }
    
    // ===== 격문 작성 =====
    
    /**
     * 격문 작성 가능 시간 확인 (주말 19:00 - 22:00)
     */
    public boolean isWritingTimeAllowed() {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek day = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();
        
        // 주말 확인
        boolean isWeekend = (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY);
        if (!isWeekend) return false;
        
        // 시간 확인 (19:00 - 22:00)
        LocalTime startTime = LocalTime.of(19, 0);
        LocalTime endTime = LocalTime.of(22, 0);
        
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }
    
    /**
     * 격문 작성 가능 여부 전체 검증
     */
    public String validateDeclaration(Player player, String targetFactionName) {
        // 1. 시간 확인
        if (!isWritingTimeAllowed()) {
            return "§c격문 작성은 주말 19:00 - 22:00에만 가능합니다.";
        }
        
        // 2. 세력 확인
        Faction playerFaction = dataManager.getPlayerFaction(player.getUniqueId());
        if (playerFaction == null) {
            return "§c세력에 소속되어 있지 않습니다.";
        }
        
        // 3. 권한 확인 (대장 또는 부대장)
        FactionMember member = playerFaction.getMember(player.getUniqueId());
        if (member == null || (!member.isLeader() && member.getRole() != FactionRole.OFFICER)) {
            return "§c격문은 세력 대장 또는 부대장만 작성할 수 있습니다.";
        }
        
        // 4. 티어 확인 (촌락 이상)
        if (playerFaction.getTier().getLevel() < FactionTier.VILLAGE.getLevel()) {
            return "§c촌락 이상의 세력만 격문을 작성할 수 있습니다.";
        }
        
        // 5. 상대 세력 확인
        Faction targetFaction = dataManager.getFactionByName(targetFactionName);
        if (targetFaction == null) {
            return "§c'%target%' 세력을 찾을 수 없습니다.".replace("%target%", targetFactionName);
        }
        
        // 6. 자기 세력 체크
        if (playerFaction.getId().equals(targetFaction.getId())) {
            return "§c자기 세력에 격문을 작성할 수 없습니다.";
        }
        
        // 7. 상대 세력 티어 확인
        if (targetFaction.getTier().getLevel() < FactionTier.VILLAGE.getLevel()) {
            return "§c상대 세력이 촌락 이상이어야 합니다.";
        }
        
        // 8. 격문 유형 호환 확인 (같은 범위 내)
        if (!WarDeclaration.canDeclare(playerFaction.getTier(), targetFaction.getTier())) {
            WarDeclaration.DeclarationType myType = WarDeclaration.getDeclarationType(playerFaction.getTier());
            WarDeclaration.DeclarationType targetType = WarDeclaration.getDeclarationType(targetFaction.getTier());
            return "§c격문 유형이 맞지 않습니다. §7(내 세력: " + 
                   (myType != null ? myType.getDisplayName() : "불가") + 
                   ", 상대: " + (targetType != null ? targetType.getDisplayName() : "불가") + ")";
        }
        
        // 9. 이미 진행 중인 전쟁/격문 확인
        for (WarDeclaration dec : activeDeclarations.values()) {
            if (dec.getPhase() == WarDeclaration.WarPhase.ENDED) continue;
            
            boolean involves = (dec.getAttackerFactionId().equals(playerFaction.getId()) && 
                               dec.getDefenderFactionId().equals(targetFaction.getId())) ||
                              (dec.getAttackerFactionId().equals(targetFaction.getId()) && 
                               dec.getDefenderFactionId().equals(playerFaction.getId()));
            if (involves) {
                return "§c이미 해당 세력과 격문/전쟁이 진행 중입니다.";
            }
        }
        
        return null; // 검증 통과
    }
    
    /**
     * 격문 작성
     */
    public WarDeclaration createDeclaration(Player author, Faction attackerFaction, Faction defenderFaction) {
        WarDeclaration.DeclarationType type = WarDeclaration.getDeclarationType(attackerFaction.getTier());
        
        String declarationId = "war_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000);
        WarDeclaration declaration = new WarDeclaration(
            declarationId,
            attackerFaction.getId(),
            defenderFaction.getId(),
            author.getUniqueId(),
            type
        );
        
        activeDeclarations.put(declarationId, declaration);
        
        // 스폰에 있는 모든 플레이어에게 알림
        notifySpawnPlayers();
        
        // 데이터 저장
        saveDeclarations();
        
        return declaration;
    }
    
    /**
     * 스폰에 있는 모든 플레이어에게 격문 작성 알림
     */
    private void notifySpawnPlayers() {
        String msg = plugin.getMessageManager().getPrefix() + "§e새로운 격문이 작성되었습니다.";
        
        // 스폰 월드의 모든 플레이어에게 전송 (스폰 월드 판단)
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 스폰 근처에 있는지 확인 (기본적으로 스폰 월드에 있는 모든 플레이어)
            if (proclamationStationLocation != null && 
                player.getWorld().equals(proclamationStationLocation.getWorld())) {
                player.sendMessage(msg);
            } else {
                // 스폰 설정이 없으면 모든 플레이어에게 전송
                player.sendMessage(msg);
            }
        }
    }
    
    // ===== 선전포고 처리 =====
    
    /**
     * 선전포고 알림 전송
     */
    public void sendDeclarationNotification(WarDeclaration declaration) {
        if (declaration.isDeclarationNotified()) return;
        
        Faction attacker = dataManager.getFaction(declaration.getAttackerFactionId());
        Faction defender = dataManager.getFaction(declaration.getDefenderFactionId());
        
        if (attacker == null || defender == null) return;
        
        // Discord 알림 전송
        if (discordWebhook.isEnabled() && plugin.getConfig().getBoolean("discord.notifications.declaration", true)) {
            List<Core> attackerCores = dataManager.getFactionCores(attacker.getId());
            List<Core> defenderCores = dataManager.getFactionCores(defender.getId());
            discordWebhook.sendDeclarationNotification(declaration, attacker, defender, attackerCores, defenderCores);
        }
        
        // 상대 세력 온라인 멤버에게 알림
        String chatMsg = plugin.getMessageManager().getPrefix() + 
            "§c§l[선전포고] §e" + attacker.getName() + "§f 세력이 §c" + 
            defender.getName() + "§f 세력에 선전포고를 하였습니다!";
        
        for (UUID memberId : defender.getMemberIds()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(chatMsg);
                member.sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§7선전포고 내용은 §e/세력 선전포고§7 에서 확인하세요.");
            }
        }
        
        // 공격 세력에도 알림
        String attackerMsg = plugin.getMessageManager().getPrefix() + 
            "§a§l[선전포고 발동] §f" + defender.getName() + 
            "§f 세력에 대한 선전포고가 발동되었습니다!";
        
        for (UUID memberId : attacker.getMemberIds()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(attackerMsg);
            }
        }
        
        declaration.setDeclarationNotified(true);
        saveDeclarations();
    }
    
    /**
     * 전쟁 시작 알림
     */
    public void sendWarStartNotification(WarDeclaration declaration) {
        if (declaration.isWarStartNotified()) return;
        
        Faction attacker = dataManager.getFaction(declaration.getAttackerFactionId());
        Faction defender = dataManager.getFaction(declaration.getDefenderFactionId());
        
        if (attacker == null || defender == null) return;
        
        // Discord 알림 전송
        if (discordWebhook.isEnabled() && plugin.getConfig().getBoolean("discord.notifications.war-start", true)) {
            List<Core> attackerCores = dataManager.getFactionCores(attacker.getId());
            List<Core> defenderCores = dataManager.getFactionCores(defender.getId());
            discordWebhook.sendWarStartNotification(declaration, attacker, defender, attackerCores, defenderCores);
        }
        
        String warMsg = plugin.getMessageManager().getPrefix() + 
            "§c§l[전쟁 시작] §e" + attacker.getName() + "§f 세력과 §c" + 
            defender.getName() + "§f 세력 사이의 전쟁이 시작되었습니다!";
        
        // 양쪽 세력 모두에게 알림
        Set<UUID> allMembers = new HashSet<>();
        allMembers.addAll(attacker.getMemberIds());
        allMembers.addAll(defender.getMemberIds());
        
        for (UUID memberId : allMembers) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(warMsg);
            }
        }
        
        // 서버 전체 공지
        Bukkit.broadcastMessage(plugin.getMessageManager().getPrefix() + 
            "§c§l⚔ " + attacker.getName() + " §fvs §c" + defender.getName() + " §f전쟁이 시작되었습니다!");
        
        declaration.setWarStartNotified(true);
        saveDeclarations();
    }
    
    // ===== 선전포고 내용 생성 =====
    
    /**
     * 선전포고 내용 생성 (메뉴에서 표시)
     */
    public List<String> getDeclarationContent(WarDeclaration declaration) {
        List<String> content = new ArrayList<>();
        
        Faction attacker = dataManager.getFaction(declaration.getAttackerFactionId());
        Faction defender = dataManager.getFaction(declaration.getDefenderFactionId());
        
        if (attacker == null || defender == null) {
            content.add("§c세력 정보를 불러올 수 없습니다.");
            return content;
        }
        
        content.add("§6§l══════ 선전포고 ══════");
        content.add("");
        content.add("§e격문 작성 세력: §f" + attacker.getName() + 
                    " §7(" + attacker.getTier().getDisplayName() + ")");
        content.add("§c상대 세력: §f" + defender.getName() + 
                    " §7(" + defender.getTier().getDisplayName() + ")");
        content.add("");
        content.add("§7격문 종류: §e" + declaration.getType().getDisplayName());
        content.add("§7격문 작성 시간: §f" + formatTime(declaration.getCreatedTime()));
        content.add("");
        
        // 전쟁까지 남은 시간
        long timeUntilWar = declaration.getTimeUntilWar();
        if (timeUntilWar > 0) {
            content.add("§c전쟁까지 남은 시간: §f" + FactionUtils.formatDuration(timeUntilWar));
        } else {
            content.add("§c§l전쟁 진행 중!");
        }
        
        content.add("");
        content.add("§6§l── 공격 세력 코어 정보 ──");
        addCoreInfo(content, attacker);
        
        content.add("");
        content.add("§c§l── 방어 세력 코어 정보 ──");
        addCoreInfo(content, defender);
        
        content.add("");
        content.add("§6§l═══════════════════");
        
        return content;
    }
    
    /**
     * 코어 정보를 목록에 추가
     */
    private void addCoreInfo(List<String> content, Faction faction) {
        List<Core> cores = dataManager.getFactionCores(faction.getId());
        
        if (cores.isEmpty()) {
            content.add("  §7코어 없음");
            return;
        }
        
        for (Core core : cores) {
            Location loc = core.getLocation();
            String locStr = loc != null ? FactionUtils.formatCoordinates(loc) : "알 수 없음";
            String name = core.hasRegisteredName() ? core.getRegisteredName() : core.getId();
            content.add("  §e" + name + " §7[Lv." + core.getLevel() + "] §f위치: " + locStr);
        }
    }
    
    /**
     * 시간 포맷
     */
    private String formatTime(long timeMillis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timeMillis));
    }
    
    // ===== 조회 =====
    
    /**
     * 최근 격문 목록 (30분 이내, 격문 작성소 표시용)
     */
    public List<WarDeclaration> getRecentProclamations() {
        return activeDeclarations.values().stream()
            .filter(WarDeclaration::isRecentProclamation)
            .sorted(Comparator.comparingLong(WarDeclaration::getCreatedTime).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 특정 세력이 관련된 활성 격문/전쟁 조회
     */
    public List<WarDeclaration> getActiveDeclarationsForFaction(String factionId) {
        return activeDeclarations.values().stream()
            .filter(d -> d.getPhase() != WarDeclaration.WarPhase.ENDED)
            .filter(d -> d.getAttackerFactionId().equals(factionId) || 
                        d.getDefenderFactionId().equals(factionId))
            .sorted(Comparator.comparingLong(WarDeclaration::getCreatedTime).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 모든 활성 격문 조회
     */
    public Collection<WarDeclaration> getAllActiveDeclarations() {
        return activeDeclarations.values().stream()
            .filter(d -> d.getPhase() != WarDeclaration.WarPhase.ENDED)
            .collect(Collectors.toList());
    }
    
    /**
     * 격문 조회
     */
    public WarDeclaration getDeclaration(String id) {
        return activeDeclarations.get(id);
    }
    
    /**
     * 두 세력이 현재 전쟁 중인지 확인
     */
    public boolean isAtWar(String factionId1, String factionId2) {
        return activeDeclarations.values().stream()
            .filter(d -> d.getPhase() == WarDeclaration.WarPhase.WAR)
            .anyMatch(d -> 
                (d.getAttackerFactionId().equals(factionId1) && d.getDefenderFactionId().equals(factionId2)) ||
                (d.getAttackerFactionId().equals(factionId2) && d.getDefenderFactionId().equals(factionId1))
            );
    }
    
    /**
     * 특정 세력이 전쟁 중인지 확인
     */
    public boolean isInWar(String factionId) {
        return activeDeclarations.values().stream()
            .filter(d -> d.getPhase() == WarDeclaration.WarPhase.WAR)
            .anyMatch(d -> d.getAttackerFactionId().equals(factionId) || 
                          d.getDefenderFactionId().equals(factionId));
    }
    
    /**
     * 전쟁 종료
     */
    public void endWar(String declarationId) {
        WarDeclaration declaration = activeDeclarations.get(declarationId);
        if (declaration != null) {
            declaration.setPhase(WarDeclaration.WarPhase.ENDED);
            
            Faction attacker = dataManager.getFaction(declaration.getAttackerFactionId());
            Faction defender = dataManager.getFaction(declaration.getDefenderFactionId());
            
            if (attacker != null && defender != null) {
                Bukkit.broadcastMessage(plugin.getMessageManager().getPrefix() + 
                    "§a⚔ " + attacker.getName() + " §fvs §a" + defender.getName() + " §f전쟁이 종료되었습니다.");
            }
            
            saveDeclarations();
        }
    }
    
    /**
     * 종료된 격문 정리
     */
    public void cleanupEndedDeclarations() {
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L);
        activeDeclarations.entrySet().removeIf(entry -> 
            entry.getValue().getPhase() == WarDeclaration.WarPhase.ENDED &&
            entry.getValue().getCreatedTime() < oneHourAgo
        );
    }
    
    // ===== 격문 작성소 =====
    
    /**
     * 격문 작성소 위치 설정
     */
    public void setProclamationStationLocation(Location location) {
        this.proclamationStationLocation = location;
        plugin.getConfig().set("war.proclamation-station.world", location.getWorld().getName());
        plugin.getConfig().set("war.proclamation-station.x", location.getBlockX());
        plugin.getConfig().set("war.proclamation-station.y", location.getBlockY());
        plugin.getConfig().set("war.proclamation-station.z", location.getBlockZ());
        plugin.saveConfig();
    }
    
    /**
     * 격문 작성소 위치 반환
     */
    public Location getProclamationStationLocation() {
        return proclamationStationLocation;
    }
    
    /**
     * 플레이어가 격문 작성소 근처에 있는지 확인 (반경 5블록)
     */
    public boolean isNearProclamationStation(Player player) {
        if (proclamationStationLocation == null) return true; // 설정 없으면 제한 없음
        
        if (!player.getWorld().equals(proclamationStationLocation.getWorld())) return false;
        
        return player.getLocation().distance(proclamationStationLocation) <= 5.0;
    }
    
    // ===== 데이터 저장/로드 =====
    
    /**
     * 격문 데이터 저장
     */
    public void saveDeclarations() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
            
            for (WarDeclaration dec : activeDeclarations.values()) {
                String path = "declarations." + dec.getId();
                config.set(path + ".attacker", dec.getAttackerFactionId());
                config.set(path + ".defender", dec.getDefenderFactionId());
                config.set(path + ".author", dec.getAuthorId().toString());
                config.set(path + ".type", dec.getType().name());
                config.set(path + ".created-time", dec.getCreatedTime());
                config.set(path + ".phase", dec.getPhase().name());
                config.set(path + ".declaration-notified", dec.isDeclarationNotified());
                config.set(path + ".war-start-notified", dec.isWarStartNotified());
            }
            
            try {
                java.io.File file = new java.io.File(plugin.getDataFolder(), "data/war_declarations.yml");
                config.save(file);
            } catch (java.io.IOException e) {
                plugin.getLogger().warning("격문 데이터 저장 실패: " + e.getMessage());
            }
        });
    }
    
    /**
     * 격문 데이터 로드
     */
    public void loadDeclarations() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "data/war_declarations.yml");
        if (!file.exists()) return;
        
        org.bukkit.configuration.file.YamlConfiguration config = 
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("declarations");
        if (section == null) return;
        
        for (String id : section.getKeys(false)) {
            try {
                org.bukkit.configuration.ConfigurationSection ds = section.getConfigurationSection(id);
                if (ds == null) continue;
                
                String attackerFactionId = ds.getString("attacker");
                String defenderFactionId = ds.getString("defender");
                UUID authorId = UUID.fromString(ds.getString("author"));
                WarDeclaration.DeclarationType type = WarDeclaration.DeclarationType.valueOf(ds.getString("type"));
                long createdTime = ds.getLong("created-time");
                WarDeclaration.WarPhase phase = WarDeclaration.WarPhase.valueOf(ds.getString("phase"));
                boolean declarationNotified = ds.getBoolean("declaration-notified", false);
                boolean warStartNotified = ds.getBoolean("war-start-notified", false);
                
                WarDeclaration declaration = new WarDeclaration(
                    id, attackerFactionId, defenderFactionId, authorId, type,
                    createdTime, phase, declarationNotified, warStartNotified
                );
                
                activeDeclarations.put(id, declaration);
                
            } catch (Exception e) {
                plugin.getLogger().warning("격문 로드 실패: " + id + " - " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("격문 " + activeDeclarations.size() + "개 로드됨");
    }
    
    /**
     * 모든 격문 데이터 저장 (동기)
     */
    public void saveAllSync() {
        org.bukkit.configuration.file.YamlConfiguration config = new org.bukkit.configuration.file.YamlConfiguration();
        
        for (WarDeclaration dec : activeDeclarations.values()) {
            String path = "declarations." + dec.getId();
            config.set(path + ".attacker", dec.getAttackerFactionId());
            config.set(path + ".defender", dec.getDefenderFactionId());
            config.set(path + ".author", dec.getAuthorId().toString());
            config.set(path + ".type", dec.getType().name());
            config.set(path + ".created-time", dec.getCreatedTime());
            config.set(path + ".phase", dec.getPhase().name());
            config.set(path + ".declaration-notified", dec.isDeclarationNotified());
            config.set(path + ".war-start-notified", dec.isWarStartNotified());
        }
        
        try {
            java.io.File file = new java.io.File(plugin.getDataFolder(), "data/war_declarations.yml");
            config.save(file);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("격문 데이터 저장 실패: " + e.getMessage());
        }
    }
}
