package com.elcserver.faction;

import com.elcserver.faction.command.CoreCommand;
import com.elcserver.faction.command.FactionCommand;
import com.elcserver.faction.command.KunCommand;
import com.elcserver.faction.config.ConfigManager;
import com.elcserver.faction.config.MessageManager;
import com.elcserver.faction.data.DataManager;
import com.elcserver.faction.data.DatabaseManager;
import com.elcserver.faction.data.MySQLDataManager;
import com.elcserver.faction.gui.CoreGUI;
import com.elcserver.faction.gui.WarGUI;
import com.elcserver.faction.listener.CoreListener;
import com.elcserver.faction.listener.PlayerListener;
import com.elcserver.faction.listener.WarListener;
import com.elcserver.faction.manager.CoreManager;
import com.elcserver.faction.manager.FactionManager;
import com.elcserver.faction.manager.EconomyManager;
import com.elcserver.faction.manager.LuckPermsManager;
import com.elcserver.faction.manager.ScoreboardManager;
import com.elcserver.faction.manager.WarManager;
import com.elcserver.faction.task.DemotionTask;
import com.elcserver.faction.task.TaxTask;
import com.elcserver.faction.task.WarTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * 세력·코어 시스템 메인 플러그인 클래스
 * Paper 서버 기반 장기 성장형 세력 시스템
 */
public class FactionCore extends JavaPlugin {

    private static FactionCore instance;
    
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DataManager dataManager;
    private DatabaseManager databaseManager;
    private MySQLDataManager mysqlDataManager;
    private FactionManager factionManager;
    private CoreManager coreManager;
    private EconomyManager economyManager;
    private LuckPermsManager luckPermsManager;
    private CoreGUI coreGUI;
    private WarManager warManager;
    private WarGUI warGUI;
    private WarListener warListener;
    private ScoreboardManager scoreboardManager;
    
    private DemotionTask demotionTask;
    private TaxTask taxTask;
    private WarTask warTask;
    
    private boolean usingMySQL = false;

    @Override
    public void onEnable() {
        instance = this;
        
        // 설정 파일 초기화
        saveDefaultConfig();
        
        try {
            // 매니저 초기화
            initializeManagers();
            
            // 명령어 등록
            registerCommands();
            
            // 이벤트 리스너 등록
            registerListeners();
            
            // 스케줄러 시작
            startSchedulers();
            
            // 스코어보드 시작
            scoreboardManager = new ScoreboardManager(this);
            scoreboardManager.start();
            
            getLogger().info("세력·코어 시스템 플러그인이 활성화되었습니다!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "플러그인 초기화 중 오류 발생!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // 데이터 저장
        if (usingMySQL && mysqlDataManager != null) {
            mysqlDataManager.saveAll();
        } else if (dataManager != null) {
            dataManager.saveAll();
        }
        
        // 전쟁 데이터 저장
        if (warManager != null) {
            warManager.saveAllSync();
        }
        
        // 전쟁 홀로그램 정리
        if (warListener != null) {
            warListener.cleanup();
        }
        
        // MySQL 연결 종료
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        
        // 스코어보드 종료
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }
        
        // 스케줄러 종료
        if (demotionTask != null) {
            demotionTask.cancel();
        }
        if (taxTask != null) {
            taxTask.cancel();
        }
        if (warTask != null) {
            warTask.cancel();
        }
        
        getLogger().info("세력·코어 시스템 플러그인이 비활성화되었습니다.");
    }

    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this);
        
        // 저장 방식 확인 (YAML 또는 MYSQL)
        String storageType = getConfig().getString("database.storage-type", "YAML").toUpperCase();
        
        if (storageType.equals("MYSQL")) {
            // MySQL 초기화
            databaseManager = new DatabaseManager(this);
            if (databaseManager.initialize()) {
                mysqlDataManager = new MySQLDataManager(this, databaseManager);
                mysqlDataManager.loadAll();
                usingMySQL = true;
                getLogger().info("데이터 저장 방식: MySQL");
            } else {
                getLogger().warning("MySQL 연결 실패! YAML 모드로 전환합니다.");
                dataManager = new DataManager(this);
                dataManager.loadAll();
                usingMySQL = false;
            }
        } else {
            // YAML 초기화
            dataManager = new DataManager(this);
            dataManager.loadAll();
            usingMySQL = false;
            getLogger().info("데이터 저장 방식: YAML");
        }
        
        economyManager = new EconomyManager(this);
        luckPermsManager = new LuckPermsManager(this);
        factionManager = new FactionManager(this);
        coreManager = new CoreManager(this);
        coreGUI = new CoreGUI(this);
        
        // 전쟁 시스템 초기화
        warManager = new WarManager(this);
        warManager.loadDeclarations();
        warGUI = new WarGUI(this);
    }

    private void registerCommands() {
        FactionCommand factionCommand = new FactionCommand(this);
        CoreCommand coreCommand = new CoreCommand(this);
        KunCommand kunCommand = new KunCommand(this);
        
        getCommand("세력").setExecutor(factionCommand);
        getCommand("세력").setTabCompleter(factionCommand);
        getCommand("코어").setExecutor(coreCommand);
        getCommand("코어").setTabCompleter(coreCommand);
        getCommand("쿤").setExecutor(kunCommand);
        getCommand("쿤").setTabCompleter(kunCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CoreListener(this), this);
        warListener = new WarListener(this);
        getServer().getPluginManager().registerEvents(warListener, this);
    }

    private void startSchedulers() {
        demotionTask = new DemotionTask(this);
        taxTask = new TaxTask(this);
        
        // 강등 체크: 매 5분마다
        demotionTask.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60 * 5);
        
        // 세금 체크: 매 분마다 (00:00 체크용)
        taxTask.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60);
        
        // 전쟁 체크: 매 10초마다
        warTask = new WarTask(this);
        warTask.runTaskTimerAsynchronously(this, 20L * 10, 20L * 10);
    }

    // Getter 메소드들
    public static FactionCore getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public FactionManager getFactionManager() {
        return factionManager;
    }

    public CoreManager getCoreManager() {
        return coreManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public LuckPermsManager getLuckPermsManager() {
        return luckPermsManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MySQLDataManager getMySQLDataManager() {
        return mysqlDataManager;
    }

    public boolean isUsingMySQL() {
        return usingMySQL;
    }

    public CoreGUI getCoreGUI() {
        return coreGUI;
    }

    public WarManager getWarManager() {
        return warManager;
    }

    public WarGUI getWarGUI() {
        return warGUI;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
