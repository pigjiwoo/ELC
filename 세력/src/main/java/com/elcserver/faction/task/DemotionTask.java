package com.elcserver.faction.task;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.FactionManager;
import com.elcserver.faction.model.Faction;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 강등 체크 스케줄러
 * 매 5분마다 강등 조건을 확인하고 처리
 */
public class DemotionTask extends BukkitRunnable {
    
    private final FactionCore plugin;
    private final FactionManager factionManager;
    
    public DemotionTask(FactionCore plugin) {
        this.plugin = plugin;
        this.factionManager = plugin.getFactionManager();
    }
    
    @Override
    public void run() {
        // ConcurrentModificationException 방지를 위해 복사본 사용
        List<Faction> factions = new ArrayList<>(factionManager.getAllFactions());
        
        for (Faction faction : factions) {
            // 강등 경고 상태 확인
            if (faction.isDemotionWarning()) {
                // 데드라인 확인
                if (System.currentTimeMillis() >= faction.getDemotionDeadline()) {
                    // 메인 스레드에서 강등 실행
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        factionManager.executeDemotion(faction);
                    });
                }
            }
        }
    }
}
