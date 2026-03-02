package com.elcserver.faction.util;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.*;

import org.bukkit.Location;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Discord 웹훅 유틸리티 클래스
 * 선전포고 알림을 Discord 채널로 전송
 */
public class DiscordWebhook {
    
    private final FactionCore plugin;
    private final boolean enabled;
    private final String defaultWebhookUrl;
    
    public DiscordWebhook(FactionCore plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.defaultWebhookUrl = plugin.getConfig().getString("discord.default-webhook-url", "");
    }
    
    /**
     * Discord 웹훅이 활성화되어 있는지 확인
     */
    public boolean isEnabled() {
        return enabled && !defaultWebhookUrl.isEmpty();
    }
    
    /**
     * 선전포고 알림을 Discord로 전송
     * 
     * @param declaration 격문/선전포고 정보
     * @param attacker 공격 세력
     * @param defender 방어 세력
     * @param attackerCores 공격 세력 코어 목록
     * @param defenderCores 방어 세력 코어 목록
     */
    public void sendDeclarationNotification(WarDeclaration declaration, 
                                             Faction attacker, 
                                             Faction defender,
                                             List<Core> attackerCores,
                                             List<Core> defenderCores) {
        if (!isEnabled()) return;
        
        // 세력별 웹훅 URL (설정에서 가져오거나 기본값 사용)
        String attackerWebhook = getFactionWebhook(attacker.getId());
        String defenderWebhook = getFactionWebhook(defender.getId());
        
        // 비동기로 전송
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // 공격 세력 알림
            if (attackerWebhook != null && !attackerWebhook.isEmpty()) {
                sendWebhook(attackerWebhook, buildDeclarationEmbed(
                    declaration, attacker, defender, attackerCores, defenderCores, true));
            }
            
            // 방어 세력 알림
            if (defenderWebhook != null && !defenderWebhook.isEmpty()) {
                sendWebhook(defenderWebhook, buildDeclarationEmbed(
                    declaration, attacker, defender, attackerCores, defenderCores, false));
            }
        });
    }
    
    /**
     * 전쟁 시작 알림을 Discord로 전송
     */
    public void sendWarStartNotification(WarDeclaration declaration,
                                          Faction attacker,
                                          Faction defender,
                                          List<Core> attackerCores,
                                          List<Core> defenderCores) {
        if (!isEnabled()) return;
        
        String attackerWebhook = getFactionWebhook(attacker.getId());
        String defenderWebhook = getFactionWebhook(defender.getId());
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // 공격 세력 알림
            if (attackerWebhook != null && !attackerWebhook.isEmpty()) {
                sendWebhook(attackerWebhook, buildWarStartEmbed(
                    declaration, attacker, defender, attackerCores, defenderCores, true));
            }
            
            // 방어 세력 알림
            if (defenderWebhook != null && !defenderWebhook.isEmpty()) {
                sendWebhook(defenderWebhook, buildWarStartEmbed(
                    declaration, attacker, defender, attackerCores, defenderCores, false));
            }
        });
    }
    
    /**
     * 세력별 Discord 웹훅 URL 가져오기
     */
    private String getFactionWebhook(String factionId) {
        String customUrl = plugin.getConfig().getString("discord.faction-webhooks." + factionId, "");
        if (!customUrl.isEmpty()) {
            return customUrl;
        }
        return defaultWebhookUrl;
    }
    
    /**
     * 선전포고 Discord Embed 생성
     */
    private String buildDeclarationEmbed(WarDeclaration declaration,
                                          Faction attacker,
                                          Faction defender,
                                          List<Core> attackerCores,
                                          List<Core> defenderCores,
                                          boolean isAttackerView) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String createdTimeStr = sdf.format(new Date(declaration.getCreatedTime()));
        String warTimeStr = sdf.format(new Date(declaration.getCreatedTime() + WarDeclaration.getWarDelayMs()));
        long timeUntilWar = declaration.getTimeUntilWar();
        
        String title = isAttackerView ? "⚔️ 선전포고 발동" : "⚠️ 선전포고 수신";
        int color = isAttackerView ? 0x00FF00 : 0xFF0000; // 초록(공격) / 빨강(방어)
        
        StringBuilder coreInfo = new StringBuilder();
        
        // 공격 세력 코어 정보
        coreInfo.append("**").append(escapeJson(attacker.getName())).append(" 코어 정보:**\\n");
        if (attackerCores.isEmpty()) {
            coreInfo.append("코어 없음\\n");
        } else {
            for (Core core : attackerCores) {
                Location loc = core.getLocation();
                String locStr = loc != null ? formatCoords(loc) : "알 수 없음";
                String name = core.hasRegisteredName() ? core.getRegisteredName() : "코어";
                coreInfo.append("• ").append(escapeJson(name))
                        .append(" [Lv.").append(core.getLevel()).append("] ")
                        .append("위치: ").append(locStr).append("\\n");
            }
        }
        
        coreInfo.append("\\n**").append(escapeJson(defender.getName())).append(" 코어 정보:**\\n");
        if (defenderCores.isEmpty()) {
            coreInfo.append("코어 없음\\n");
        } else {
            for (Core core : defenderCores) {
                Location loc = core.getLocation();
                String locStr = loc != null ? formatCoords(loc) : "알 수 없음";
                String name = core.hasRegisteredName() ? core.getRegisteredName() : "코어";
                coreInfo.append("• ").append(escapeJson(name))
                        .append(" [Lv.").append(core.getLevel()).append("] ")
                        .append("위치: ").append(locStr).append("\\n");
            }
        }
        
        String description = String.format(
            "**%s** 세력이 **%s** 세력에 선전포고를 하였습니다!\\n\\n" +
            "**격문 종류:** %s\\n" +
            "**격문 작성 시간:** %s\\n" +
            "**전쟁 시작 시간:** %s\\n" +
            "**전쟁까지 남은 시간:** %s\\n\\n" +
            "%s",
            escapeJson(attacker.getName()),
            escapeJson(defender.getName()),
            declaration.getType().getDisplayName(),
            createdTimeStr,
            warTimeStr,
            FactionUtils.formatDuration(timeUntilWar),
            coreInfo.toString()
        );
        
        return buildJsonPayload(title, description, color);
    }
    
    /**
     * 전쟁 시작 Discord Embed 생성
     */
    private String buildWarStartEmbed(WarDeclaration declaration,
                                       Faction attacker,
                                       Faction defender,
                                       List<Core> attackerCores,
                                       List<Core> defenderCores,
                                       boolean isAttackerView) {
        String title = "⚔️ 전쟁 시작!";
        int color = 0xFF4500; // 주황
        
        StringBuilder coreInfo = new StringBuilder();
        
        coreInfo.append("**").append(escapeJson(attacker.getName())).append(" 코어:**\\n");
        for (Core core : attackerCores) {
            Location loc = core.getLocation();
            String locStr = loc != null ? formatCoords(loc) : "?";
            String name = core.hasRegisteredName() ? core.getRegisteredName() : "코어";
            coreInfo.append("• ").append(escapeJson(name))
                    .append(" [Lv.").append(core.getLevel()).append("] ").append(locStr).append("\\n");
        }
        
        coreInfo.append("\\n**").append(escapeJson(defender.getName())).append(" 코어:**\\n");
        for (Core core : defenderCores) {
            Location loc = core.getLocation();
            String locStr = loc != null ? formatCoords(loc) : "?";
            String name = core.hasRegisteredName() ? core.getRegisteredName() : "코어";
            coreInfo.append("• ").append(escapeJson(name))
                    .append(" [Lv.").append(core.getLevel()).append("] ").append(locStr).append("\\n");
        }
        
        String description = String.format(
            "**%s** 세력과 **%s** 세력의 전쟁이 시작되었습니다!\\n\\n%s",
            escapeJson(attacker.getName()),
            escapeJson(defender.getName()),
            coreInfo.toString()
        );
        
        return buildJsonPayload(title, description, color);
    }
    
    /**
     * Discord Webhook 전송
     */
    private void sendWebhook(String webhookUrl, String jsonPayload) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Discord 웹훅 전송 실패: HTTP " + responseCode);
            }
            
            connection.disconnect();
            
        } catch (IOException e) {
            plugin.getLogger().warning("Discord 웹훅 전송 중 오류: " + e.getMessage());
        }
    }
    
    /**
     * JSON 페이로드 생성
     */
    private String buildJsonPayload(String title, String description, int color) {
        return String.format(
            "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d,\"footer\":{\"text\":\"ELC 세력 시스템\"}}]}",
            escapeJson(title),
            description,
            color
        );
    }
    
    /**
     * JSON 문자열 이스케이프
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
    
    /**
     * 좌표 포맷
     */
    private String formatCoords(Location loc) {
        return String.format("X:%d Y:%d Z:%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
