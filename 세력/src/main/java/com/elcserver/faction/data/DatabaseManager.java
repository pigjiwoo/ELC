package com.elcserver.faction.data;

import com.elcserver.faction.FactionCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * MySQL 데이터베이스 연결 관리자
 * HikariCP 커넥션 풀 사용
 */
public class DatabaseManager {
    
    private final FactionCore plugin;
    private HikariDataSource dataSource;
    private boolean enabled;
    
    public DatabaseManager(FactionCore plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }
    
    /**
     * 데이터베이스 연결 초기화
     */
    public boolean initialize() {
        ConfigurationSection dbConfig = plugin.getConfig().getConfigurationSection("database.mysql");
        if (dbConfig == null) {
            plugin.getLogger().warning("MySQL 설정을 찾을 수 없습니다!");
            return false;
        }
        
        String host = dbConfig.getString("host", "localhost");
        int port = dbConfig.getInt("port", 3306);
        String database = dbConfig.getString("database", "faction_db");
        String username = dbConfig.getString("username", "root");
        String password = dbConfig.getString("password", "");
        
        // 풀 설정
        ConfigurationSection poolConfig = dbConfig.getConfigurationSection("pool");
        int maxPoolSize = poolConfig != null ? poolConfig.getInt("maximum-pool-size", 10) : 10;
        int minIdle = poolConfig != null ? poolConfig.getInt("minimum-idle", 2) : 2;
        long connectionTimeout = poolConfig != null ? poolConfig.getLong("connection-timeout", 30000) : 30000;
        long idleTimeout = poolConfig != null ? poolConfig.getLong("idle-timeout", 600000) : 600000;
        long maxLifetime = poolConfig != null ? poolConfig.getLong("max-lifetime", 1800000) : 1800000;
        
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);
            config.setPoolName("FactionCore-Pool");
            
            // 추가 설정
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            
            dataSource = new HikariDataSource(config);
            enabled = true;
            
            plugin.getLogger().info("MySQL 연결 성공! (" + host + ":" + port + "/" + database + ")");
            
            // 테이블 생성
            createTables();
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "MySQL 연결 실패!", e);
            enabled = false;
            return false;
        }
    }
    
    /**
     * 데이터베이스 테이블 생성
     */
    private void createTables() {
        try (Connection conn = getConnection()) {
            // 세력 테이블
            executeUpdate(conn, """
                CREATE TABLE IF NOT EXISTS factions (
                    id VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    icon VARCHAR(50),
                    leader_id VARCHAR(36) NOT NULL,
                    leader_name VARCHAR(50),
                    tier INT DEFAULT 0,
                    balance BIGINT DEFAULT 0,
                    points BIGINT DEFAULT 0,
                    fever_active BOOLEAN DEFAULT FALSE,
                    fever_end BIGINT DEFAULT 0,
                    demotion_warning BOOLEAN DEFAULT FALSE,
                    demotion_deadline BIGINT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_name (name),
                    INDEX idx_leader (leader_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            
            // 세력원 테이블
            executeUpdate(conn, """
                CREATE TABLE IF NOT EXISTS faction_members (
                    player_id VARCHAR(36) PRIMARY KEY,
                    faction_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(50) NOT NULL,
                    role ENUM('LEADER', 'OFFICER', 'MEMBER') DEFAULT 'MEMBER',
                    join_time BIGINT NOT NULL,
                    last_online BIGINT NOT NULL,
                    FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    INDEX idx_faction (faction_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            
            // 코어 테이블
            executeUpdate(conn, """
                CREATE TABLE IF NOT EXISTS cores (
                    id VARCHAR(36) PRIMARY KEY,
                    faction_id VARCHAR(36) NOT NULL,
                    world VARCHAR(100) NOT NULL,
                    x INT NOT NULL,
                    y INT NOT NULL,
                    z INT NOT NULL,
                    level INT DEFAULT 1,
                    installed BOOLEAN DEFAULT FALSE,
                    install_time BIGINT DEFAULT 0,
                    FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    INDEX idx_faction (faction_id),
                    INDEX idx_location (world, x, y, z)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            
            // 초대 테이블
            executeUpdate(conn, """
                CREATE TABLE IF NOT EXISTS faction_invites (
                    invitee_id VARCHAR(36) PRIMARY KEY,
                    faction_id VARCHAR(36) NOT NULL,
                    inviter_id VARCHAR(36) NOT NULL,
                    invite_time BIGINT NOT NULL,
                    expire_time BIGINT NOT NULL,
                    FOREIGN KEY (faction_id) REFERENCES factions(id) ON DELETE CASCADE,
                    INDEX idx_faction (faction_id),
                    INDEX idx_expire (expire_time)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            
            // 인출 기록 테이블
            executeUpdate(conn, """
                CREATE TABLE IF NOT EXISTS withdrawal_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_id VARCHAR(36) NOT NULL,
                    faction_id VARCHAR(36) NOT NULL,
                    amount BIGINT NOT NULL,
                    withdraw_date DATE NOT NULL,
                    withdraw_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_player_date (player_id, withdraw_date),
                    INDEX idx_faction (faction_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
            
            plugin.getLogger().info("데이터베이스 테이블 생성/확인 완료");
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "테이블 생성 실패!", e);
        }
    }
    
    /**
     * SQL 업데이트 실행
     */
    private void executeUpdate(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
    
    /**
     * 커넥션 획득
     */
    public Connection getConnection() throws SQLException {
        if (!enabled || dataSource == null) {
            throw new SQLException("데이터베이스가 초기화되지 않았습니다!");
        }
        return dataSource.getConnection();
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isEnabled() {
        return enabled && dataSource != null && !dataSource.isClosed();
    }
    
    /**
     * 데이터베이스 연결 종료
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("MySQL 연결 종료됨");
        }
        enabled = false;
    }
    
    /**
     * 커넥션 풀 상태 정보
     */
    public String getPoolStats() {
        if (!isEnabled()) return "비활성";
        return String.format("활성: %d, 유휴: %d, 대기: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
    }
}
