package com.elcserver.faction.manager;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionRole;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * LuckPerms 연동 매니저
 * 세력 가입/탈퇴 시 그룹 및 권한 관리
 */
public class LuckPermsManager {
    
    private final FactionCore plugin;
    private LuckPerms luckPerms;
    private boolean enabled;
    
    // 세력 그룹 접두사 (faction_세력명)
    private static final String FACTION_GROUP_PREFIX = "faction_";
    
    // 역할별 권한 노드
    private static final String PERM_FACTION_LEADER = "faction.role.leader";
    private static final String PERM_FACTION_OFFICER = "faction.role.officer";
    private static final String PERM_FACTION_MEMBER = "faction.role.member";
    
    public LuckPermsManager(FactionCore plugin) {
        this.plugin = plugin;
        this.enabled = false;
        
        setupLuckPerms();
    }
    
    /**
     * LuckPerms 연동 초기화
     */
    private void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            plugin.getLogger().info("LuckPerms 플러그인을 찾을 수 없습니다. 권한 연동이 비활성화됩니다.");
            return;
        }
        
        try {
            luckPerms = LuckPermsProvider.get();
            enabled = true;
            plugin.getLogger().info("LuckPerms 연동 성공!");
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("LuckPerms API를 로드할 수 없습니다: " + e.getMessage());
            enabled = false;
        }
    }
    
    /**
     * LuckPerms 연동 상태 확인
     */
    public boolean isEnabled() {
        return enabled && luckPerms != null;
    }
    
    /**
     * 세력 그룹 이름 생성
     */
    private String getFactionGroupName(String factionName) {
        // 특수문자 제거, 소문자로 변환
        return FACTION_GROUP_PREFIX + factionName.toLowerCase()
                .replaceAll("[^a-z0-9가-힣]", "")
                .replaceAll("\\s+", "_");
    }
    
    /**
     * 세력 그룹 생성 또는 가져오기
     */
    public CompletableFuture<Group> getOrCreateFactionGroup(Faction faction) {
        if (!isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        
        String groupName = getFactionGroupName(faction.getName());
        
        return luckPerms.getGroupManager().loadGroup(groupName).thenCompose(optGroup -> {
            if (optGroup.isPresent()) {
                return CompletableFuture.completedFuture(optGroup.get());
            }
            
            // 그룹 생성
            return luckPerms.getGroupManager().createAndLoadGroup(groupName).thenApply(group -> {
                // 그룹 메타데이터 설정
                group.data().add(MetaNode.builder("faction", faction.getName()).build());
                group.data().add(MetaNode.builder("faction-tier", faction.getTier().name()).build());
                
                // 기본 세력원 권한 부여
                group.data().add(Node.builder(PERM_FACTION_MEMBER).build());
                
                // 프리픽스 설정 [세력명]
                group.data().add(PrefixNode.builder("[" + faction.getName() + "] ", 50).build());
                
                luckPerms.getGroupManager().saveGroup(group);
                plugin.getLogger().info("세력 그룹 생성됨: " + groupName);
                
                return group;
            });
        });
    }
    
    /**
     * 플레이어를 세력 그룹에 추가
     */
    public void addPlayerToFaction(UUID playerId, Faction faction, FactionRole role) {
        if (!isEnabled()) return;
        
        String groupName = getFactionGroupName(faction.getName());
        
        luckPerms.getUserManager().loadUser(playerId).thenAccept(user -> {
            if (user == null) return;
            
            // 기존 세력 그룹 제거 (다른 세력)
            removeAllFactionGroups(user);
            
            // 새 세력 그룹 추가
            InheritanceNode groupNode = InheritanceNode.builder(groupName).build();
            user.data().add(groupNode);
            
            // 역할별 권한 노드 추가
            addRolePermission(user, role);
            
            // 메타데이터 설정
            user.data().add(MetaNode.builder("faction", faction.getName()).build());
            user.data().add(MetaNode.builder("faction-role", role.name()).build());
            
            luckPerms.getUserManager().saveUser(user);
            
            plugin.getLogger().info("플레이어 " + playerId + " -> 세력 그룹 " + groupName + " 추가됨");
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "LuckPerms 세력 추가 실패: " + playerId, ex);
            return null;
        });
    }
    
    /**
     * 플레이어를 세력 그룹에서 제거
     */
    public void removePlayerFromFaction(UUID playerId) {
        if (!isEnabled()) return;
        
        luckPerms.getUserManager().loadUser(playerId).thenAccept(user -> {
            if (user == null) return;
            
            // 모든 세력 그룹 제거
            removeAllFactionGroups(user);
            
            // 역할 권한 제거
            removeAllRolePermissions(user);
            
            // 메타데이터 제거
            user.data().clear(node -> 
                node instanceof MetaNode && 
                (((MetaNode) node).getMetaKey().equals("faction") || 
                 ((MetaNode) node).getMetaKey().equals("faction-role"))
            );
            
            luckPerms.getUserManager().saveUser(user);
            
            plugin.getLogger().info("플레이어 " + playerId + " -> 세력 그룹에서 제거됨");
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "LuckPerms 세력 제거 실패: " + playerId, ex);
            return null;
        });
    }
    
    /**
     * 플레이어 역할 업데이트
     */
    public void updatePlayerRole(UUID playerId, FactionRole newRole) {
        if (!isEnabled()) return;
        
        luckPerms.getUserManager().loadUser(playerId).thenAccept(user -> {
            if (user == null) return;
            
            // 기존 역할 권한 제거
            removeAllRolePermissions(user);
            
            // 새 역할 권한 추가
            addRolePermission(user, newRole);
            
            // 메타데이터 업데이트
            user.data().clear(node -> 
                node instanceof MetaNode && 
                ((MetaNode) node).getMetaKey().equals("faction-role")
            );
            user.data().add(MetaNode.builder("faction-role", newRole.name()).build());
            
            luckPerms.getUserManager().saveUser(user);
            
            plugin.getLogger().info("플레이어 " + playerId + " 역할 변경: " + newRole.getDisplayName());
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.WARNING, "LuckPerms 역할 변경 실패: " + playerId, ex);
            return null;
        });
    }
    
    /**
     * 모든 세력 그룹 제거
     */
    private void removeAllFactionGroups(User user) {
        user.data().clear(node -> 
            node instanceof InheritanceNode && 
            ((InheritanceNode) node).getGroupName().startsWith(FACTION_GROUP_PREFIX)
        );
    }
    
    /**
     * 역할 권한 추가
     */
    private void addRolePermission(User user, FactionRole role) {
        switch (role) {
            case LEADER:
                user.data().add(Node.builder(PERM_FACTION_LEADER).build());
                user.data().add(Node.builder(PERM_FACTION_OFFICER).build());
                user.data().add(Node.builder(PERM_FACTION_MEMBER).build());
                break;
            case OFFICER:
                user.data().add(Node.builder(PERM_FACTION_OFFICER).build());
                user.data().add(Node.builder(PERM_FACTION_MEMBER).build());
                break;
            case MEMBER:
                user.data().add(Node.builder(PERM_FACTION_MEMBER).build());
                break;
        }
    }
    
    /**
     * 모든 역할 권한 제거
     */
    private void removeAllRolePermissions(User user) {
        user.data().remove(Node.builder(PERM_FACTION_LEADER).build());
        user.data().remove(Node.builder(PERM_FACTION_OFFICER).build());
        user.data().remove(Node.builder(PERM_FACTION_MEMBER).build());
    }
    
    /**
     * 세력 그룹 삭제
     */
    public void deleteFactionGroup(String factionName) {
        if (!isEnabled()) return;
        
        String groupName = getFactionGroupName(factionName);
        
        luckPerms.getGroupManager().loadGroup(groupName).thenAccept(optGroup -> {
            optGroup.ifPresent(group -> {
                luckPerms.getGroupManager().deleteGroup(group);
                plugin.getLogger().info("세력 그룹 삭제됨: " + groupName);
            });
        });
    }
    
    /**
     * 세력 그룹 프리픽스 업데이트
     */
    public void updateFactionPrefix(Faction faction) {
        if (!isEnabled()) return;
        
        String groupName = getFactionGroupName(faction.getName());
        
        luckPerms.getGroupManager().loadGroup(groupName).thenAccept(optGroup -> {
            optGroup.ifPresent(group -> {
                // 기존 프리픽스 제거
                group.data().clear(node -> node instanceof PrefixNode);
                
                // 새 프리픽스 추가 (티어에 따라 색상 변경)
                String prefix = getTierPrefix(faction);
                group.data().add(PrefixNode.builder(prefix, 50).build());
                
                // 티어 메타 업데이트
                group.data().clear(node -> 
                    node instanceof MetaNode && 
                    ((MetaNode) node).getMetaKey().equals("faction-tier")
                );
                group.data().add(MetaNode.builder("faction-tier", faction.getTier().name()).build());
                
                luckPerms.getGroupManager().saveGroup(group);
            });
        });
    }
    
    /**
     * 티어별 프리픽스 생성
     */
    private String getTierPrefix(Faction faction) {
        String tierColor;
        switch (faction.getTier()) {
            case NATION:
                tierColor = "&6"; // 금색
                break;
            case CITY:
                tierColor = "&b"; // 하늘색
                break;
            case VILLAGE:
                tierColor = "&a"; // 연두색
                break;
            default:
                tierColor = "&7"; // 회색
                break;
        }
        return tierColor + "[" + faction.getName() + "] &r";
    }
    
    /**
     * 플레이어가 특정 권한을 가지고 있는지 확인
     */
    public boolean hasPermission(Player player, String permission) {
        if (!isEnabled()) {
            return player.hasPermission(permission);
        }
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return player.hasPermission(permission);
        }
        
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
    
    /**
     * LuckPerms API 인스턴스 반환
     */
    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
