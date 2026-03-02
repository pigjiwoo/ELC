package com.elcserver.faction.manager;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.model.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.text.DecimalFormat;
import java.util.UUID;

/**
 * 사이드바 스코어보드 관리 클래스
 * 경제 정보(쿤)와 세력 정보를 실시간 표시
 */
public class ScoreboardManager {

    private final FactionCore plugin;
    private final DecimalFormat formatter = new DecimalFormat("#,###");
    private int taskId = -1;

    // 고유 색상 코드로 각 줄 식별 (팀 엔트리용)
    private static final String[] ENTRIES = {
            ChatColor.BLACK.toString(),                                    // 0: 빈 줄 (상단)
            ChatColor.DARK_BLUE.toString(),                                // 1: 닉네임
            ChatColor.DARK_GREEN.toString(),                               // 2: 빈 줄
            ChatColor.DARK_AQUA.toString(),                                // 3: --- 경제 정보 ---
            ChatColor.DARK_RED.toString(),                                 // 4: Koon :
            ChatColor.DARK_PURPLE.toString(),                              // 5: 빈 줄
            ChatColor.GOLD.toString(),                                     // 6: --- 세력 정보 ---
            ChatColor.GRAY.toString(),                                     // 7: 접속자 :
            ChatColor.DARK_GRAY.toString(),                                // 8: 피버타임:
            ChatColor.BLUE.toString()                                      // 9: 빈 줄 (하단)
    };

    public ScoreboardManager(FactionCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 스코어보드 업데이트 스케줄러 시작 (매 1초)
     */
    public void start() {
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 0L, 20L).getTaskId(); // 20틱 = 1초
    }

    /**
     * 스케줄러 중지
     */
    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * 특정 플레이어의 스코어보드 업데이트
     */
    private void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();

        // 최초 생성 시에만 새 스코어보드 할당
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()
                || board.getObjective("sidebar") == null) {
            board = createScoreboard(player);
            final Scoreboard finalBoard = board;
            Bukkit.getScheduler().runTask(plugin, () -> player.setScoreboard(finalBoard));
        }

        // 값 갱신
        updateValues(player, board);
    }

    /**
     * 스코어보드 초기 생성
     */
    private Scoreboard createScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("sidebar", Criteria.DUMMY,
                ChatColor.YELLOW + "" + ChatColor.BOLD + "ELC Server");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // 점수 높은 순서대로 위에 표시
        for (int i = 0; i < ENTRIES.length; i++) {
            obj.getScore(ENTRIES[i]).setScore(ENTRIES.length - 1 - i);
        }

        // 팀 생성 (각 줄의 prefix로 내용 제어)
        createTeam(board, "line_blank1", ENTRIES[0], "");
        createTeam(board, "line_name",   ENTRIES[1], "");
        createTeam(board, "line_blank2", ENTRIES[2], "");
        createTeam(board, "line_econ",   ENTRIES[3], ChatColor.AQUA + "--- 경제 정보 ---");
        createTeam(board, "line_koon",   ENTRIES[4], "");
        createTeam(board, "line_blank3", ENTRIES[5], "");
        createTeam(board, "line_fact",   ENTRIES[6], ChatColor.GREEN + "--- 세력 정보 ---");
        createTeam(board, "line_online", ENTRIES[7], "");
        createTeam(board, "line_fever",  ENTRIES[8], "");
        createTeam(board, "line_blank4", ENTRIES[9], "");

        return board;
    }

    /**
     * 팀 생성 헬퍼 - 엔트리에 초기 prefix를 설정
     */
    private void createTeam(Scoreboard board, String name, String entry, String prefix) {
        Team team = board.registerNewTeam(name);
        team.addEntry(entry);
        team.setPrefix(prefix);
    }

    /**
     * 스코어보드 값 갱신
     */
    private void updateValues(Player player, Scoreboard board) {
        UUID playerId = player.getUniqueId();

        // --- 닉네임 ---
        setTeamPrefix(board, "line_name",
                ChatColor.WHITE + player.getName());

        // --- Koon 잔액 ---
        long balance = plugin.getEconomyManager().getPlayerBalanceLong(playerId);
        setTeamPrefix(board, "line_koon",
                ChatColor.WHITE + "Koon : " + ChatColor.GOLD + formatter.format(balance));

        // --- 세력 정보 ---
        Faction faction = plugin.getDataManager().getPlayerFaction(playerId);
        if (faction != null) {
            // 온라인 세력원 수
            int onlineCount = 0;
            for (UUID memberId : faction.getMemberIds()) {
                if (Bukkit.getPlayer(memberId) != null) {
                    onlineCount++;
                }
            }
            int totalCount = faction.getMemberCount();

            setTeamPrefix(board, "line_online",
                    ChatColor.WHITE + "접속자 : " + ChatColor.AQUA + onlineCount
                            + ChatColor.GRAY + " / " + ChatColor.AQUA + totalCount);

            // 피버타임
            boolean fever = faction.isFeverTimeActive();
            setTeamPrefix(board, "line_fever",
                    ChatColor.WHITE + "피버타임: " + (fever
                            ? ChatColor.GREEN + "활성화"
                            : ChatColor.RED + "비활성화"));
        } else {
            setTeamPrefix(board, "line_online",
                    ChatColor.WHITE + "접속자 : " + ChatColor.GRAY + "없음");
            setTeamPrefix(board, "line_fever",
                    ChatColor.WHITE + "피버타임: " + ChatColor.GRAY + "없음");
        }
    }

    /**
     * 팀 prefix 업데이트 헬퍼
     */
    private void setTeamPrefix(Scoreboard board, String teamName, String prefix) {
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.setPrefix(prefix);
        }
    }
}
