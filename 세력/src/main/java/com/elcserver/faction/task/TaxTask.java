package com.elcserver.faction.task;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.EconomyManager;
import com.elcserver.faction.model.Faction;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 세금 징수 스케줄러
 * 매일 00:00에 세력 세금을 징수
 */
public class TaxTask extends BukkitRunnable {
    
    private final FactionCore plugin;
    private final EconomyManager economyManager;
    
    private boolean taxCollectedToday = false;
    private int lastCheckedDay = -1;
    
    public TaxTask(FactionCore plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
    }
    
    @Override
    public void run() {
        LocalTime now = LocalTime.now();
        int currentDay = java.time.LocalDate.now().getDayOfYear();
        
        // 날짜가 바뀌면 리셋
        if (currentDay != lastCheckedDay) {
            taxCollectedToday = false;
            lastCheckedDay = currentDay;
        }
        
        // 설정된 시간 파싱
        String taxTimeStr = plugin.getConfigManager().getTaxExecutionTime();
        LocalTime taxTime;
        try {
            taxTime = LocalTime.parse(taxTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            taxTime = LocalTime.MIDNIGHT;
        }
        
        // 세금 징수 시간 확인 (1분 오차 허용)
        if (!taxCollectedToday && isWithinMinute(now, taxTime)) {
            collectTaxes();
            resetWithdrawLimits();
            taxCollectedToday = true;
        }
    }
    
    private boolean isWithinMinute(LocalTime now, LocalTime target) {
        int nowMinutes = now.getHour() * 60 + now.getMinute();
        int targetMinutes = target.getHour() * 60 + target.getMinute();
        return Math.abs(nowMinutes - targetMinutes) <= 1;
    }
    
    private void collectTaxes() {
        List<Faction> factions = new ArrayList<>(plugin.getFactionManager().getAllFactions());
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (Faction faction : factions) {
                economyManager.collectTax(faction);
            }
            
            plugin.getLogger().info("세금 징수 완료: " + factions.size() + "개 세력");
        });
    }
    
    private void resetWithdrawLimits() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            economyManager.resetWithdrawLimits();
            plugin.getLogger().info("인출 한도 초기화 완료");
        });
    }
}
