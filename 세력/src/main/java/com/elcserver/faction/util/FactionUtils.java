package com.elcserver.faction.util;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * 유틸리티 클래스
 */
public class FactionUtils {
    
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.KOREA);
    
    /**
     * 색상 코드 변환
     */
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * 색상 코드 제거
     */
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }
    
    /**
     * 숫자 포맷 (1000 -> 1,000)
     */
    public static String formatNumber(long number) {
        return NUMBER_FORMAT.format(number);
    }
    
    /**
     * 숫자 포맷 (double)
     */
    public static String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
    
    /**
     * 시간 포맷 (밀리초 -> 읽기 쉬운 형태)
     */
    public static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("일 ");
        }
        if (hours > 0) {
            sb.append(hours).append("시간 ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("분 ");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            sb.append(seconds).append("초");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 위치 문자열 생성
     */
    public static String formatLocation(Location location) {
        if (location == null) return "알 수 없음";
        return String.format("%s (%d, %d, %d)",
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
    
    /**
     * 위치 문자열 (월드 이름 없이)
     */
    public static String formatCoordinates(Location location) {
        if (location == null) return "알 수 없음";
        return String.format("%d, %d, %d",
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
    
    /**
     * UUID 검증
     */
    public static boolean isValidUUID(String uuidString) {
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 문자열이 숫자인지 확인
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 안전한 Long 파싱
     */
    public static long parseLongSafe(String str, long defaultValue) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 안전한 Integer 파싱
     */
    public static int parseIntSafe(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 프로그레스 바 생성
     */
    public static String createProgressBar(double current, double max, int length) {
        double percent = Math.min(1.0, current / max);
        int filled = (int) (length * percent);
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        
        bar.append("§7");
        for (int i = 0; i < empty; i++) {
            bar.append("█");
        }
        
        return bar.toString();
    }
    
    /**
     * 퍼센트 계산
     */
    public static double calculatePercent(double current, double max) {
        if (max == 0) return 0;
        return (current / max) * 100;
    }
    
    /**
     * 두 위치 사이의 거리 (2D, Y축 무시)
     */
    public static double distance2D(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return Double.MAX_VALUE;
        if (!loc1.getWorld().equals(loc2.getWorld())) return Double.MAX_VALUE;
        
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * 랜덤 ID 생성
     */
    public static String generateRandomId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + 
            (int) (Math.random() * 10000);
    }
}
