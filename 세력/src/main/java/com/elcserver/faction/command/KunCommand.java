package com.elcserver.faction.command;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.manager.EconomyManager;
import com.elcserver.faction.model.Faction;
import com.elcserver.faction.model.FactionMember;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 쿤 화폐 명령어 처리 클래스
 * /쿤 - 내 잔액 확인
 * /쿤 잔액 - 내 잔액 확인
 * /쿤 확인 <플레이어> - 다른 플레이어 잔액 확인
 * /쿤 보내기 <플레이어> <금액> - 다른 플레이어에게 송금
 * /쿤 지급 <플레이어> <금액> - 관리자: 쿤 지급
 * /쿤 차감 <플레이어> <금액> - 관리자: 쿤 차감
 * /쿤 설정 <플레이어> <금액> - 관리자: 쿤 설정
 * /쿤 도움말 - 도움말
 */
public class KunCommand implements CommandExecutor, TabCompleter {

    private final FactionCore plugin;
    private final EconomyManager economyManager;

    private static final List<String> SUB_COMMANDS = Arrays.asList(
        "잔액", "확인", "보내기", "정보", "지급", "차감", "설정", "도움말"
    );

    private static final List<String> ADMIN_COMMANDS = Arrays.asList("지급", "차감", "설정");

    public KunCommand(FactionCore plugin) {
        this.plugin = plugin;
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
            showBalance(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "잔액":
            case "balance":
            case "bal":
                showBalance(player);
                break;

            case "확인":
            case "check":
                if (args.length < 2) {
                    player.sendMessage(prefix() + "사용법: /쿤 확인 <플레이어>");
                    return true;
                }
                showOtherBalance(player, args[1]);
                break;

            case "보내기":
            case "송금":
            case "pay":
            case "send":
                if (args.length < 3) {
                    player.sendMessage(prefix() + "사용법: /쿤 보내기 <플레이어> <금액>");
                    return true;
                }
                handleSend(player, args[1], args[2]);
                break;

            case "정보":
            case "info":
                showKunInfo(player);
                break;

            case "지급":
            case "give":
                if (!player.hasPermission("faction.admin")) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(prefix() + "사용법: /쿤 지급 <플레이어> <금액>");
                    return true;
                }
                handleGive(player, args[1], args[2]);
                break;

            case "차감":
            case "take":
                if (!player.hasPermission("faction.admin")) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(prefix() + "사용법: /쿤 차감 <플레이어> <금액>");
                    return true;
                }
                handleTake(player, args[1], args[2]);
                break;

            case "설정":
            case "set":
                if (!player.hasPermission("faction.admin")) {
                    player.sendMessage(plugin.getMessageManager().getPrefixedMessage("general.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(prefix() + "사용법: /쿤 설정 <플레이어> <금액>");
                    return true;
                }
                handleSet(player, args[1], args[2]);
                break;

            case "도움말":
            case "help":
                showHelp(player);
                break;

            default:
                showHelp(player);
                break;
        }

        return true;
    }

    // ===== 잔액 확인 =====

    private void showBalance(Player player) {
        long balance = economyManager.getPlayerBalanceLong(player.getUniqueId());
        player.sendMessage(prefix() + "§e내 잔액: §a" + economyManager.formatAmount(balance) + "쿤");
    }

    private void showOtherBalance(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }

        long balance = economyManager.getPlayerBalanceLong(target.getUniqueId());
        player.sendMessage(prefix() + "§e" + target.getName() + "님의 잔액: §a" + economyManager.formatAmount(balance) + "쿤");
    }

    // ===== 송금 =====

    private void handleSend(Player player, String targetName, String amountStr) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(prefix() + "§c자기 자신에게 송금할 수 없습니다.");
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }

        if (amount <= 0) {
            player.sendMessage(prefix() + "§c0보다 큰 금액을 입력해주세요.");
            return;
        }

        if (!economyManager.hasPlayerBalance(player.getUniqueId(), amount)) {
            player.sendMessage(prefix() + "§c잔액이 부족합니다.");
            return;
        }

        // 송금 처리
        if (economyManager.removePlayerBalance(player.getUniqueId(), amount)) {
            economyManager.addPlayerBalance(target.getUniqueId(), amount);
            String formatted = economyManager.formatAmount(amount);
            player.sendMessage(prefix() + "§a" + target.getName() + "§f님에게 §e" + formatted + "쿤§f을 보냈습니다.");
            target.sendMessage(prefix() + "§a" + player.getName() + "§f님에게서 §e" + formatted + "쿤§f을 받았습니다.");
        } else {
            player.sendMessage(prefix() + "§c송금에 실패하였습니다.");
        }
    }

    // ===== 관리자 명령어 =====

    private void handleGive(Player sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix() + "§c0보다 큰 금액을 입력해주세요.");
            return;
        }

        economyManager.addPlayerBalance(target.getUniqueId(), amount);
        String formatted = economyManager.formatAmount(amount);
        sender.sendMessage(prefix() + "§a" + target.getName() + "§f님에게 §e" + formatted + "쿤§f을 지급하였습니다.");
        target.sendMessage(prefix() + "§a관리자로부터 §e" + formatted + "쿤§f을 지급받았습니다.");
    }

    private void handleTake(Player sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }

        if (amount <= 0) {
            sender.sendMessage(prefix() + "§c0보다 큰 금액을 입력해주세요.");
            return;
        }

        if (economyManager.removePlayerBalance(target.getUniqueId(), amount)) {
            String formatted = economyManager.formatAmount(amount);
            sender.sendMessage(prefix() + "§c" + target.getName() + "§f님에게서 §e" + formatted + "쿤§f을 차감하였습니다.");
            target.sendMessage(prefix() + "§c관리자에 의해 §e" + formatted + "쿤§f이 차감되었습니다.");
        } else {
            sender.sendMessage(prefix() + "§c잔액이 부족하여 차감할 수 없습니다.");
        }
    }

    private void handleSet(Player sender, String targetName, String amountStr) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.player-not-found"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix() + plugin.getMessageManager().getMessage("general.invalid-amount"));
            return;
        }

        if (amount < 0) {
            sender.sendMessage(prefix() + "§c0 이상의 금액을 입력해주세요.");
            return;
        }

        economyManager.setPlayerBalance(target.getUniqueId(), amount);
        String formatted = economyManager.formatAmount(amount);
        sender.sendMessage(prefix() + "§a" + target.getName() + "§f님의 잔액을 §e" + formatted + "쿤§f(으)로 설정하였습니다.");
        target.sendMessage(prefix() + "§a관리자에 의해 잔액이 §e" + formatted + "쿤§f(으)로 설정되었습니다.");
    }

    // ===== 쿤 정보 =====

    private void showKunInfo(Player player) {
        player.sendMessage("§6§l========== 쿤 시스템 정보 ==========");

        // 개인 잔액
        long personalBalance = economyManager.getPlayerBalanceLong(player.getUniqueId());
        player.sendMessage("§e개인 잔액: §a" + economyManager.formatAmount(personalBalance) + "쿤");

        // 세력 정보
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        if (faction != null) {
            player.sendMessage("");
            player.sendMessage("§e§l[세력 경제]");
            player.sendMessage("§e세력 계좌: §a" + economyManager.formatAmount(faction.getBalance()) + "쿤");
            player.sendMessage("§e세력 포인트: §d" + economyManager.formatAmount(faction.getPoints()) + "점");

            player.sendMessage("");
            player.sendMessage("§e§l[쿤 분할 시스템]");
            player.sendMessage("§7분할 비율: §f개인 " + (int)(plugin.getConfigManager().getKunPersonalRatio() * 100)
                    + "% : 세력 " + (int)(plugin.getConfigManager().getKunFactionRatio() * 100) + "%");
            player.sendMessage("§7※ 송금 및 플레이어 처치 보상은 분할 대상에서 제외");

            // 배수 정보
            double multiplier = faction.calculateKunMultiplier();
            boolean feverActive = faction.isFeverTimeActive();
            double totalMultiplier = multiplier + (feverActive ? 0.5 : 0);

            player.sendMessage("");
            player.sendMessage("§e§l[쿤 배수]");
            player.sendMessage("§7기본 배수: §f1.5배");
            player.sendMessage("§7코어 보너스: §f+" + String.format("%.2f", faction.getCoreCount() * 0.05) + "배 "
                    + "§7(코어 " + faction.getCoreCount() + "개 × 0.05)");
            player.sendMessage("§7피버타임: " + (feverActive ? "§d활성화 (+0.5배)" : "§7비활성"));
            player.sendMessage("§e→ 현재 총 배수: §a" + String.format("%.2f", totalMultiplier) + "배 "
                    + "§7(최대 2.6배" + (feverActive ? " + 피버" : "") + ")");

            // 세금 정보
            int coreCount = faction.getCoreCount();
            double taxRate = plugin.getConfigManager().getTaxRatePerCore() * coreCount;
            long estimatedTax = (long) (faction.getBalance() * taxRate / 100.0);

            player.sendMessage("");
            player.sendMessage("§e§l[세금]");
            player.sendMessage("§7세금율: §f" + String.format("%.1f", taxRate) + "% "
                    + "§7(" + String.format("%.1f", plugin.getConfigManager().getTaxRatePerCore()) + "% × 코어 " + coreCount + "개)");
            player.sendMessage("§7예상 세금: §c" + economyManager.formatAmount(estimatedTax) + "쿤 §7(00:00시 부과)");

            // 인출 한도 정보
            long withdrawLimit = economyManager.getWithdrawLimit(player.getUniqueId());
            long remaining = economyManager.getRemainingWithdrawLimit(player.getUniqueId());

            player.sendMessage("");
            player.sendMessage("§e§l[인출 한도]");
            player.sendMessage("§7오늘 인출 한도: §f" + economyManager.formatAmount(withdrawLimit) + "쿤");
            player.sendMessage("§7남은 인출 가능액: §a" + economyManager.formatAmount(remaining) + "쿤");
            player.sendMessage("§7※ 한도 공식: (세력 계좌 ÷ 세력원 수) × 60%");

            FactionMember member = faction.getMember(player.getUniqueId());
            if (member != null && member.isJoinedToday()) {
                player.sendMessage("§c※ 당일 가입자는 인출 불가");
            }
        } else {
            player.sendMessage("");
            player.sendMessage("§7세력에 소속되지 않아 쿤 분할 시스템이 적용되지 않습니다.");
            player.sendMessage("§7몰 처치 보상이 전액 개인에게 지급됩니다.");
        }

        player.sendMessage("§6================================");
    }

    // ===== 도움말 =====

    private void showHelp(Player player) {
        player.sendMessage("§6§l========== 쿤 도움말 ==========");
        player.sendMessage("§e/쿤 §7- 내 잔액 확인");
        player.sendMessage("§e/쿤 잔액 §7- 내 잔액 확인");
        player.sendMessage("§e/쿤 확인 <플레이어> §7- 다른 플레이어 잔액 확인");
        player.sendMessage("§e/쿤 보내기 <플레이어> <금액> §7- 쿤 송금 (분할 미적용)");
        player.sendMessage("§e/쿤 정보 §7- 쿤 시스템 상세 정보");

        if (player.hasPermission("faction.admin")) {
            player.sendMessage("§c§l--- 관리자 명령어 ---");
            player.sendMessage("§e/쿤 지급 <플레이어> <금액> §7- 쿤 지급");
            player.sendMessage("§e/쿤 차감 <플레이어> <금액> §7- 쿤 차감");
            player.sendMessage("§e/쿤 설정 <플레이어> <금액> §7- 잔액 설정");
        }

        player.sendMessage("§6================================");
    }

    // ===== 유틸리티 =====

    private String prefix() {
        return "§6[쿤] §f";
    }

    // ===== 탭 자동완성 =====

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(input)) {
                    if (ADMIN_COMMANDS.contains(sub) && !sender.hasPermission("faction.admin")) {
                        continue;
                    }
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("확인") || sub.equals("보내기") || sub.equals("송금") ||
                sub.equals("지급") || sub.equals("차감") || sub.equals("설정") ||
                sub.equals("check") || sub.equals("pay") || sub.equals("send") ||
                sub.equals("give") || sub.equals("take") || sub.equals("set")) {
                String input = args[1].toLowerCase();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getName().toLowerCase().startsWith(input)) {
                        completions.add(online.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("보내기") || sub.equals("송금") || sub.equals("지급") ||
                sub.equals("차감") || sub.equals("설정")) {
                completions.add("<금액>");
            }
        }

        return completions;
    }
}
