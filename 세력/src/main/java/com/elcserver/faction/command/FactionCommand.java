package com.elcserver.faction.command;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.FactionManager;
import com.elcserver.faction.manager.EconomyManager;
import com.elcserver.faction.manager.WarManager;
import com.elcserver.faction.model.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 세력 명령어 처리 클래스
 */
public class FactionCommand implements CommandExecutor, TabCompleter {
    
    private final FactionCore plugin;
    private final FactionManager factionManager;
    private final EconomyManager economyManager;
    
    private static final List<String> SUB_COMMANDS = Arrays.asList(
        "정보", "생성", "탈퇴", "초대", "수락", "거절", "추방",
        "승급", "강등", "위임", "계좌", "입금", "인출", "포인트",
        "목록", "도움말", "리로드", "격문", "선전포고", "전쟁종료", "격문작성소설정"
    );
    
    public FactionCommand(FactionCore plugin) {
        this.plugin = plugin;
        this.factionManager = plugin.getFactionManager();
        this.economyManager = plugin.getEconomyManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showFactionInfo(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "정보":
            case "info":
                showFactionInfo(player);
                break;
                
            case "생성":
            case "create":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 생성 <세력명>");
                    return true;
                }
                handleCreate(player, args[1]);
                break;
                
            case "탈퇴":
            case "leave":
                handleLeave(player);
                break;
                
            case "초대":
            case "invite":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 초대 <플레이어>");
                    return true;
                }
                handleInvite(player, args[1]);
                break;
                
            case "수락":
            case "accept":
                handleAccept(player);
                break;
                
            case "거절":
            case "decline":
                handleDecline(player);
                break;
                
            case "추방":
            case "kick":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 추방 <플레이어>");
                    return true;
                }
                handleKick(player, args[1]);
                break;
                
            case "승급":
            case "promote":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 승급 <플레이어>");
                    return true;
                }
                handlePromote(player, args[1]);
                break;
                
            case "강등":
            case "demote":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 강등 <플레이어>");
                    return true;
                }
                handleDemote(player, args[1]);
                break;
                
            case "위임":
            case "transfer":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 위임 <플레이어>");
                    return true;
                }
                handleTransfer(player, args[1]);
                break;
                
            case "계좌":
            case "balance":
                showBalance(player);
                break;
                
            case "입금":
            case "deposit":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 입금 <금액>");
                    return true;
                }
                handleDeposit(player, args[1]);
                break;
                
            case "인출":
            case "withdraw":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 인출 <금액>");
                    return true;
                }
                handleWithdraw(player, args[1]);
                break;
                
            case "포인트":
            case "points":
                showPoints(player);
                break;
                
            case "목록":
            case "list":
                showFactionList(player);
                break;
                
            case "도움말":
            case "help":
                showHelp(player);
                break;
                
            case "리로드":
            case "reload":
                if (player.hasPermission("faction.admin")) {
                    plugin.getConfigManager().reload();
                    plugin.getMessageManager().reload();
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "설정이 리로드되었습니다.");
                } else {
                    player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
                }
                break;
                
            case "격문":
            case "proclamation":
                handleProclamation(player);
                break;
                
            case "선전포고":
            case "declaration":
                handleDeclarationInfo(player);
                break;
                
            case "전쟁종료":
            case "endwar":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /세력 전쟁종료 <격문ID>");
                    return true;
                }
                handleEndWar(player, args[1]);
                break;
                
            case "격문작성소설정":
            case "setstation":
                handleSetProclamationStation(player);
                break;
                
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void handleCreate(Player player, String factionName) {
        // 이미 세력에 가입되어 있는지 확인
        Faction existingFaction = factionManager.getPlayerFaction(player.getUniqueId());
        if (existingFaction != null) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c이미 세력에 가입되어 있습니다.");
            return;
        }
        
        // 세력 이름 유효성 검사
        if (factionName.length() < 2 || factionName.length() > 16) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c세력 이름은 2~16자여야 합니다.");
            return;
        }
        
        // 특수문자 검사
        if (!factionName.matches("^[가-힣a-zA-Z0-9_]+$")) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c세력 이름에 특수문자를 사용할 수 없습니다.");
            return;
        }
        
        // 중복 이름 검사
        if (plugin.getDataManager().getFactionByName(factionName) != null) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c이미 존재하는 세력 이름입니다.");
            return;
        }
        
        // 세력 생성 비용 확인 (설정에서 가져오기)
        long createCost = plugin.getConfigManager().getFactionCreateCost();
        if (createCost > 0 && !economyManager.hasPlayerBalance(player.getUniqueId(), createCost)) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§c세력 창설 비용이 부족합니다. §e(필요: " + economyManager.formatAmount(createCost) + "쿤)");
            return;
        }
        
        // 비용 차감
        if (createCost > 0) {
            economyManager.removePlayerBalance(player.getUniqueId(), createCost);
        }
        
        // 세력 생성
        Faction newFaction = factionManager.createFaction(
            factionName, 
            player.getUniqueId(), 
            player.getName(),
            "⚔",  // 기본 아이콘
            new ArrayList<>()  // 초기 부대장 없음
        );
        
        if (newFaction != null) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§a세력 '§e" + factionName + "§a'이(가) 창설되었습니다!");
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§7/세력 초대 <플레이어> 명령어로 세력원을 초대하세요.");
        } else {
            // 실패 시 비용 환불
            if (createCost > 0) {
                economyManager.addPlayerBalance(player.getUniqueId(), createCost);
            }
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c세력 생성에 실패했습니다.");
        }
    }
    
    private void showFactionInfo(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        player.sendMessage("§6§l========== 세력 정보 ==========");
        player.sendMessage("§e세력 이름: §f" + faction.getName());
        player.sendMessage("§e단계: §f" + faction.getTier().getDisplayName());
        player.sendMessage("§e세력원: §f" + faction.getMemberCount() + "명");
        player.sendMessage("§e코어: §f" + faction.getCoreCount() + "개");
        player.sendMessage("§e계좌: §f" + economyManager.formatAmount(faction.getBalance()) + "쿤");
        player.sendMessage("§e포인트: §f" + economyManager.formatAmount(faction.getPoints()) + "점");
        player.sendMessage("§e쿤 배수: §f" + String.format("%.2f", faction.calculateKunMultiplier()) + "배");
        
        if (faction.isFeverTimeActive()) {
            player.sendMessage("§d피버타임: §a활성화");
        }
        
        if (faction.isDemotionWarning()) {
            long remaining = faction.getDemotionDeadline() - System.currentTimeMillis();
            long hours = remaining / (60 * 60 * 1000);
            player.sendMessage("§c§l[경고] §c강등까지 " + hours + "시간 남음!");
        }
        
        player.sendMessage("§6================================");
        
        // 세력원 목록
        player.sendMessage("§e세력원 목록:");
        for (FactionMember member : faction.getMembers()) {
            String status = Bukkit.getPlayer(member.getPlayerId()) != null ? "§a●" : "§7●";
            player.sendMessage("  " + status + " §f" + member.getPlayerName() + 
                " §7(" + member.getRole().getDisplayName() + ")");
        }
    }
    
    private void handleLeave(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        FactionMember member = faction.getMember(player.getUniqueId());
        if (member != null && member.isLeader()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.cannot-leave-leader"));
            return;
        }
        
        if (factionManager.leaveFaction(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.left"));
        }
    }
    
    private void handleInvite(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.player-not-found"));
            return;
        }
        
        Faction targetFaction = factionManager.getPlayerFaction(target.getUniqueId());
        if (targetFaction != null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.already-in-faction"));
            return;
        }
        
        if (factionManager.invitePlayer(player.getUniqueId(), target.getUniqueId())) {
            Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
            
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "invite.sent", "%player%", target.getName()));
            
            target.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "invite.received", "%faction%", faction.getName()));
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
        }
    }
    
    private void handleAccept(Player player) {
        if (factionManager.isInFaction(player.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.already-in-faction"));
            return;
        }
        
        FactionInvite invite = plugin.getDataManager().getInvite(player.getUniqueId());
        if (invite == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("invite.expired"));
            return;
        }
        
        if (factionManager.acceptInvite(player.getUniqueId())) {
            Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "invite.accepted", "%faction%", faction.getName()));
        }
    }
    
    private void handleDecline(Player player) {
        factionManager.declineInvite(player.getUniqueId());
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage("invite.declined"));
    }
    
    private void handleKick(Player player, String targetName) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        // 타겟 찾기 (온라인/오프라인)
        UUID targetId = null;
        String actualName = targetName;
        
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            targetId = target.getUniqueId();
            actualName = target.getName();
        } else {
            // 세력원 목록에서 검색
            for (FactionMember member : faction.getMembers()) {
                if (member.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetId = member.getPlayerId();
                    actualName = member.getPlayerName();
                    break;
                }
            }
        }
        
        if (targetId == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.player-not-found"));
            return;
        }
        
        if (player.getUniqueId().equals(targetId)) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("kick.cannot-kick-self"));
            return;
        }
        
        FactionMember targetMember = faction.getMember(targetId);
        if (targetMember != null && targetMember.isLeader()) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("kick.cannot-kick-leader"));
            return;
        }
        
        if (factionManager.kickMember(player.getUniqueId(), targetId)) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "kick.success", "%player%", actualName));
            
            if (target != null) {
                target.sendMessage(plugin.getMessageManager().getPrefixedMessage("kick.kicked"));
            }
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
        }
    }
    
    private void handlePromote(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.player-not-found"));
            return;
        }
        
        if (factionManager.promoteToOfficer(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "role.promoted", "%player%", target.getName(), "%role%", "부대장"));
            target.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "role.promoted", "%player%", "당신이", "%role%", "부대장"));
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
        }
    }
    
    private void handleDemote(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.player-not-found"));
            return;
        }
        
        if (factionManager.demoteOfficer(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "role.demoted", "%player%", target.getName(), "%role%", "세력원"));
            target.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "role.demoted", "%player%", "당신이", "%role%", "세력원"));
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
        }
    }
    
    private void handleTransfer(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.player-not-found"));
            return;
        }
        
        if (factionManager.transferLeadership(player.getUniqueId(), target.getUniqueId())) {
            Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
            factionManager.broadcastToFaction(faction, 
                plugin.getMessageManager().getPrefixedMessage(
                    "role.transferred", "%player%", target.getName()));
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
        }
    }
    
    private void showBalance(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
            "account.balance", "%amount%", economyManager.formatAmount(faction.getBalance())));
        
        long limit = economyManager.getRemainingWithdrawLimit(player.getUniqueId());
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
            "account.withdraw-limit", "%limit%", economyManager.formatAmount(limit)));
    }
    
    private void handleDeposit(Player player, String amountStr) {
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.invalid-amount"));
            return;
        }
        
        if (amount <= 0) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.invalid-amount"));
            return;
        }
        
        if (economyManager.deposit(player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "account.deposited", "%amount%", economyManager.formatAmount(amount)));
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("account.insufficient-funds"));
        }
    }
    
    private void handleWithdraw(Player player, String amountStr) {
        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.invalid-amount"));
            return;
        }
        
        if (amount <= 0) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.invalid-amount"));
            return;
        }
        
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        if (faction != null) {
            FactionMember member = faction.getMember(player.getUniqueId());
            if (member != null && member.isJoinedToday()) {
                player.sendMessage(plugin.getMessageManager().getPrefixedMessage("account.withdraw-blocked-new"));
                return;
            }
        }
        
        if (economyManager.withdraw(player.getUniqueId(), amount)) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "account.withdrawn", "%amount%", economyManager.formatAmount(amount)));
        } else {
            long limit = economyManager.getRemainingWithdrawLimit(player.getUniqueId());
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "account.withdraw-limit", "%limit%", economyManager.formatAmount(limit)));
        }
    }
    
    private void showPoints(Player player) {
        Faction faction = factionManager.getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
            "points.balance", "%amount%", economyManager.formatAmount(faction.getPoints())));
    }
    
    private void showFactionList(Player player) {
        Collection<Faction> factions = factionManager.getAllFactions();
        
        player.sendMessage("§6§l========== 세력 목록 ==========");
        
        if (factions.isEmpty()) {
            player.sendMessage("§7등록된 세력이 없습니다.");
        } else {
            for (Faction faction : factions) {
                player.sendMessage("§e" + faction.getName() + " §7- " + 
                    faction.getTier().getDisplayName() + " §f(" + 
                    faction.getMemberCount() + "명)");
            }
        }
        
        player.sendMessage("§6================================");
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l========== 세력 도움말 ==========");
        player.sendMessage("§e/세력 §7- 세력 정보 보기");
        player.sendMessage("§e/세력 생성 <세력명> §7- 새 세력 창설");
        player.sendMessage("§e/세력 탈퇴 §7- 세력 탈퇴");;
        player.sendMessage("§e/세력 초대 <플레이어> §7- 세력 초대");
        player.sendMessage("§e/세력 수락 §7- 초대 수락");
        player.sendMessage("§e/세력 거절 §7- 초대 거절");
        player.sendMessage("§e/세력 추방 <플레이어> §7- 세력원 추방");
        player.sendMessage("§e/세력 승급 <플레이어> §7- 부대장으로 승급");
        player.sendMessage("§e/세력 강등 <플레이어> §7- 세력원으로 강등");
        player.sendMessage("§e/세력 위임 <플레이어> §7- 대장 위임");
        player.sendMessage("§e/세력 계좌 §7- 세력 계좌 확인");
        player.sendMessage("§e/세력 입금 <금액> §7- 세력 계좌 입금");
        player.sendMessage("§e/세력 인출 <금액> §7- 세력 계좌 인출");
        player.sendMessage("§e/세력 포인트 §7- 세력 포인트 확인");
        player.sendMessage("§e/세력 목록 §7- 모든 세력 목록");
        player.sendMessage("§e/세력 격문 §7- 격문 작성소 열기");
        player.sendMessage("§e/세력 선전포고 §7- 선전포고/전쟁 현황 보기");
        player.sendMessage("§e/세력 전쟁종료 <ID> §7- 전쟁 종료 (관리자)");
        player.sendMessage("§6=================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("초대") || subCommand.equals("추방") || 
                subCommand.equals("승급") || subCommand.equals("강등") || 
                subCommand.equals("위임")) {
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
            if (subCommand.equals("전쟁종료")) {
                String input = args[1].toLowerCase();
                WarManager warMgr = plugin.getWarManager();
                if (warMgr != null) {
                    for (WarDeclaration dec : warMgr.getAllActiveDeclarations()) {
                        if (dec.getId().toLowerCase().startsWith(input)) {
                            completions.add(dec.getId());
                        }
                    }
                }
            }
        }
        
        return completions;
    }
    
    // ===== 전쟁 관련 명령어 =====
    
    private void handleProclamation(Player player) {
        plugin.getWarGUI().openProclamationMenu(player);
    }
    
    private void handleDeclarationInfo(Player player) {
        plugin.getWarGUI().openDeclarationInfoMenu(player);
    }
    
    private void handleEndWar(Player player, String declarationId) {
        if (!player.hasPermission("faction.admin")) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
            return;
        }
        
        WarManager warMgr = plugin.getWarManager();
        WarDeclaration dec = warMgr.getDeclaration(declarationId);
        if (dec == null) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c해당 격문/전쟁을 찾을 수 없습니다.");
            return;
        }
        
        warMgr.endWar(declarationId);
        player.sendMessage(plugin.getMessageManager().getPrefix() + "§a전쟁이 종료되었습니다.");
    }
    
    private void handleSetProclamationStation(Player player) {
        if (!player.hasPermission("faction.admin")) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
            return;
        }
        
        plugin.getWarManager().setProclamationStationLocation(player.getLocation());
        player.sendMessage(plugin.getMessageManager().getPrefix() + 
            "§a격문 작성소 위치가 현재 위치로 설정되었습니다.");
    }
}
