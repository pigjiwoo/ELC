package com.elcserver.faction.listener;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionMember;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 플레이어 이벤트 리스너
 */
public class PlayerListener implements Listener {
    
    private final FactionCore plugin;
    
    public PlayerListener(FactionCore plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 세력 소속 확인 및 정보 업데이트
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction != null) {
            FactionMember member = faction.getMember(player.getUniqueId());
            if (member != null) {
                // 이름 및 마지막 접속 시간 업데이트
                member.setPlayerName(player.getName());
                member.setLastOnline(System.currentTimeMillis());
                
                // 세력 환영 메시지
                player.sendMessage(plugin.getMessageManager().getPrefix() + 
                    "§e" + faction.getName() + " §f세력에 오신 것을 환영합니다!");
                
                // 강등 경고 알림
                if (faction.isDemotionWarning()) {
                    long remaining = faction.getDemotionDeadline() - System.currentTimeMillis();
                    long hours = remaining / (60 * 60 * 1000);
                    player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                        "demotion.warning", "%hours%", String.valueOf(hours)));
                }
            }
        }
        
        // 초대 알림
        if (plugin.getDataManager().hasInvite(player.getUniqueId())) {
            String factionId = plugin.getDataManager().getInvite(player.getUniqueId()).getFactionId();
            Faction invitingFaction = plugin.getDataManager().getFaction(factionId);
            if (invitingFaction != null) {
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                    "invite.received", "%faction%", invitingFaction.getName()));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 코어 설치 취소
        plugin.getCoreManager().cancelInstall(player.getUniqueId());
        
        // 마지막 접속 시간 업데이트
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction != null) {
            FactionMember member = faction.getMember(player.getUniqueId());
            if (member != null) {
                member.setLastOnline(System.currentTimeMillis());
            }
        }
        
        // 데이터 저장
        plugin.getDataManager().scheduleSave();
    }
}
