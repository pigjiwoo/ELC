package com.elcserver.faction.task;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.WarManager;
import com.elcserver.faction.model.WarDeclaration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * 전쟁 진행 상태 체크 태스크
 * 
 * - 격문 작성 30분 후 선전포고 알림 전송
 * - 격문 작성 1시간 후 전쟁 시작 알림 전송
 * - 종료된 격문 정리
 */
public class WarTask extends BukkitRunnable {
    
    private final FactionCore plugin;
    private final WarManager warManager;
    
    public WarTask(FactionCore plugin) {
        this.plugin = plugin;
        this.warManager = plugin.getWarManager();
    }
    
    @Override
    public void run() {
        if (warManager == null) return;
        
        Collection<WarDeclaration> activeDeclarations = warManager.getAllActiveDeclarations();
        
        for (WarDeclaration declaration : activeDeclarations) {
            WarDeclaration.WarPhase phase = declaration.updateAndGetPhase();
            
            switch (phase) {
                case PROCLAMATION:
                    // 아직 선전포고 전 - 대기
                    break;
                    
                case DECLARATION:
                    // 선전포고 시간 도래 - 알림 전송
                    if (!declaration.isDeclarationNotified()) {
                        // 메인 스레드에서 실행
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            warManager.sendDeclarationNotification(declaration);
                        });
                    }
                    break;
                    
                case WAR:
                    // 전쟁 시작 시간 도래 - 알림 전송
                    if (!declaration.isWarStartNotified()) {
                        // 메인 스레드에서 실행
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            warManager.sendWarStartNotification(declaration);
                        });
                    }
                    break;
                    
                case ENDED:
                    // 종료 상태 - 정리 대상
                    break;
            }
        }
        
        // 종료된 격문 정리 (1시간 경과)
        warManager.cleanupEndedDeclarations();
    }
}
