package com.elcserver.faction.config;

import com.elcserver.faction.FactionCore;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 메시지 관리 클래스
 */
public class MessageManager {
    
    private final FactionCore plugin;
    private FileConfiguration messagesConfig;
    private String prefix;
    
    // 메시지 캐시
    private final Map<String, String> messageCache;
    
    public MessageManager(FactionCore plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        loadMessages();
    }
    
    public void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages_ko.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages_ko.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // 기본 메시지 병합
        try {
            InputStreamReader reader = new InputStreamReader(
                plugin.getResource("messages_ko.yml"), StandardCharsets.UTF_8);
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
            messagesConfig.setDefaults(defaultConfig);
        } catch (Exception e) {
            plugin.getLogger().warning("기본 메시지 파일 로드 실패");
        }
        
        prefix = colorize(messagesConfig.getString("prefix", "&6[세력] &f"));
        messageCache.clear();
    }
    
    /**
     * 메시지 가져오기
     */
    public String getMessage(String path) {
        if (messageCache.containsKey(path)) {
            return messageCache.get(path);
        }
        
        String message = messagesConfig.getString(path);
        if (message == null) {
            message = "&c메시지를 찾을 수 없음: " + path;
        }
        
        message = colorize(message);
        messageCache.put(path, message);
        return message;
    }
    
    /**
     * 플레이스홀더가 있는 메시지 가져오기
     */
    public String getMessage(String path, Object... replacements) {
        String message = getMessage(path);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                message = message.replace(placeholder, value);
            }
        }
        
        return message;
    }
    
    /**
     * 접두사 포함 메시지
     */
    public String getPrefixedMessage(String path) {
        return prefix + getMessage(path);
    }
    
    /**
     * 접두사 포함 + 플레이스홀더 메시지
     */
    public String getPrefixedMessage(String path, Object... replacements) {
        return prefix + getMessage(path, replacements);
    }
    
    /**
     * 색상 코드 변환
     */
    public String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 접두사 반환
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * 메시지 리로드
     */
    public void reload() {
        loadMessages();
    }
}
