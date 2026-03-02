package com.elcserver.faction.command;

import com.elcserver.faction.FactionCore;
import com.elcserver.faction.listener.CoreListener;
import com.elcserver.faction.manager.CoreManager;
import com.elcserver.faction.manager.EconomyManager;
import com.elcserver.faction.model.Core;
import com.elcserver.faction.model.Faction;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 코어 명령어 처리 클래스
 */
public class CoreCommand implements CommandExecutor, TabCompleter {
    
    private final FactionCore plugin;
    private final CoreManager coreManager;
    private final EconomyManager economyManager;
    
    private static final List<String> SUB_COMMANDS = Arrays.asList(
        "목록", "정보", "업그레이드", "등록", "해제", "이동", "지급", "도움말"
    );
    
    private static final List<String> ADMIN_COMMANDS = Arrays.asList("지급");
    
    public CoreCommand(FactionCore plugin) {
        this.plugin = plugin;
        this.coreManager = plugin.getCoreManager();
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
            showCoreList(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "목록":
            case "list":
                showCoreList(player);
                break;
                
            case "정보":
            case "info":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /코어 정보 <번호>");
                    return true;
                }
                showCoreInfo(player, args[1]);
                break;
                
            case "업그레이드":
            case "upgrade":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /코어 업그레이드 <번호>");
                    return true;
                }
                handleUpgrade(player, args[1]);
                break;
                
            case "등록":
            case "register":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /코어 등록 <번호>");
                    return true;
                }
                handleRegister(player, args[1]);
                break;
                
            case "해제":
            case "unregister":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /코어 해제 <번호>");
                    return true;
                }
                handleUnregister(player, args[1]);
                break;
                
            case "이동":
            case "tp":
            case "teleport":
                if (args.length < 2) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "사용법: /코어 이동 <번호>");
                    return true;
                }
                handleTeleport(player, args[1]);
                break;
                
            case "지급":
            case "give":
                handleGiveCore(player, args);
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
    
    private void showCoreList(Player player) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        List<Core> cores = coreManager.getFactionCores(faction.getId());
        
        player.sendMessage("§6§l========== 코어 목록 ==========");
        
        if (cores.isEmpty()) {
            player.sendMessage("§7설치된 코어가 없습니다.");
        } else {
            int index = 1;
            for (Core core : cores) {
                String registered = core.isRegistered() ? 
                    "§a[등록 #" + core.getRegisteredSlot() + "]" : "§7[미등록]";
                    
                player.sendMessage("§e" + index + ". §f" + core.getLevel() + "단계 " + registered);
                player.sendMessage("   §7위치: " + core.getWorldName() + " (" + 
                    core.getX() + ", " + core.getY() + ", " + core.getZ() + ")");
                player.sendMessage("   §7범위: " + core.getRange() + "×" + core.getRange());
                index++;
            }
        }
        
        player.sendMessage("§6================================");
    }
    
    private void showCoreInfo(Player player, String indexStr) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "올바른 번호를 입력해주세요.");
            return;
        }
        
        List<Core> cores = coreManager.getFactionCores(faction.getId());
        
        if (index < 0 || index >= cores.size()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "존재하지 않는 코어 번호입니다.");
            return;
        }
        
        Core core = cores.get(index);
        
        player.sendMessage("§6§l========== 코어 정보 ==========");
        player.sendMessage("§e단계: §f" + core.getLevel() + "단계");
        player.sendMessage("§e범위: §f" + core.getRange() + "×" + core.getRange() + " (Y축 무시)");
        player.sendMessage("§e위치: §f" + core.getWorldName() + " (" + 
            core.getX() + ", " + core.getY() + ", " + core.getZ() + ")");
        player.sendMessage("§e등록: §f" + (core.isRegistered() ? 
            "슬롯 #" + core.getRegisteredSlot() : "미등록"));
        
        if (core.canRetrieve()) {
            player.sendMessage("§e회수: §a가능");
        } else {
            player.sendMessage("§e회수: §c" + core.getRetrieveCooldownMinutes() + "분 후 가능");
        }
        
        if (core.canUpgrade()) {
            player.sendMessage("§e업그레이드 비용: §f" + 
                economyManager.formatAmount(core.getUpgradeCost()) + "쿤");
        } else {
            player.sendMessage("§e업그레이드: §c최대 단계");
        }
        
        player.sendMessage("§6================================");
    }
    
    private void handleUpgrade(Player player, String indexStr) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "올바른 번호를 입력해주세요.");
            return;
        }
        
        List<Core> cores = coreManager.getFactionCores(faction.getId());
        
        if (index < 0 || index >= cores.size()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "존재하지 않는 코어 번호입니다.");
            return;
        }
        
        Core core = cores.get(index);
        
        if (!core.canUpgrade()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "이미 최대 단계입니다.");
            return;
        }
        
        if (coreManager.upgradeCore(player, core.getId())) {
            // 성공 메시지는 CoreManager에서 처리
        }
    }
    
    private void handleRegister(Player player, String indexStr) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "올바른 번호를 입력해주세요.");
            return;
        }
        
        List<Core> cores = coreManager.getFactionCores(faction.getId());
        
        if (index < 0 || index >= cores.size()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "존재하지 않는 코어 번호입니다.");
            return;
        }
        
        Core core = cores.get(index);
        
        if (core.isRegistered()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "이미 등록된 코어입니다.");
            return;
        }
        
        if (coreManager.registerCore(faction, core.getId())) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage(
                "core.registered", "%slot%", String.valueOf(core.getRegisteredSlot())));
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.register-full"));
        }
    }
    
    private void handleUnregister(Player player, String indexStr) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "올바른 번호를 입력해주세요.");
            return;
        }
        
        List<Core> cores = coreManager.getFactionCores(faction.getId());
        
        if (index < 0 || index >= cores.size()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "존재하지 않는 코어 번호입니다.");
            return;
        }
        
        Core core = cores.get(index);
        
        if (!core.isRegistered()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "등록되지 않은 코어입니다.");
            return;
        }
        
        coreManager.unregisterCore(core.getId());
        player.sendMessage(plugin.getMessageManager().getPrefixedMessage("core.deleted"));
    }
    
    private void handleTeleport(Player player, String indexStr) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
        
        if (faction == null) {
            player.sendMessage(plugin.getMessageManager().getPrefixedMessage("faction.not-in-faction"));
            return;
        }
        
        int index;
        try {
            index = Integer.parseInt(indexStr) - 1;
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "올바른 번호를 입력해주세요.");
            return;
        }
        
        List<Core> cores = coreManager.getFactionCores(faction.getId());
        
        if (index < 0 || index >= cores.size()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "존재하지 않는 코어 번호입니다.");
            return;
        }
        
        Core core = cores.get(index);
        
        if (!core.isRegistered()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "등록된 코어만 텔레포트 가능합니다. /코어 등록 " + (index + 1));
            return;
        }
        
        coreManager.teleportToCore(player, core.getId());
    }
    
    /**
     * 코어 아이템 지급 (관리자 전용)
     */
    private void handleGiveCore(Player player, String[] args) {
        // 권한 확인
        if (!player.hasPermission("faction.admin") && !player.isOp()) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + "§c관리자 권한이 필요합니다.");
            return;
        }
        
        Player target;
        int amount = 1;
        
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getMessageManager().getPrefix() + "§c플레이어를 찾을 수 없습니다: " + args[1]);
                return;
            }
            
            if (args.length >= 3) {
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount < 1 || amount > 64) {
                        player.sendMessage(plugin.getMessageManager().getPrefix() + "§c수량은 1~64 사이여야 합니다.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() + "§c올바른 수량을 입력해주세요.");
                    return;
                }
            }
        } else {
            target = player;
        }
        
        // 코어 아이템 생성 및 지급
        ItemStack coreItem = CoreListener.createCoreItem();
        coreItem.setAmount(amount);
        
        // 인벤토리에 추가
        target.getInventory().addItem(coreItem);
        
        if (player.equals(target)) {
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§a코어 아이템 §e" + amount + "개§a를 받았습니다.");
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§e" + target.getName() + "§a에게 코어 아이템 §e" + amount + "개§a를 지급했습니다.");
            target.sendMessage(plugin.getMessageManager().getPrefix() + 
                "§a코어 아이템 §e" + amount + "개§a를 받았습니다.");
        }
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l========== 코어 도움말 ==========");
        player.sendMessage("§e/코어 §7- 코어 목록 보기");
        player.sendMessage("§e/코어 정보 <번호> §7- 코어 상세 정보");
        player.sendMessage("§e/코어 업그레이드 <번호> §7- 코어 업그레이드");
        player.sendMessage("§e/코어 등록 <번호> §7- 텔레포트 등록");
        player.sendMessage("§e/코어 해제 <번호> §7- 텔레포트 등록 해제");
        player.sendMessage("§e/코어 이동 <번호> §7- 등록된 코어로 이동");
        
        // 관리자 명령어
        if (player.hasPermission("faction.admin") || player.isOp()) {
            player.sendMessage("§c§l[관리자]");
            player.sendMessage("§e/코어 지급 [플레이어] [수량] §7- 코어 아이템 지급");
        }
        
        player.sendMessage("§6=================================");
        player.sendMessage("§7※ 코어 설치/회수: 코어 블록 우클릭");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                // 관리자 명령어는 권한 있을 때만 표시
                if (ADMIN_COMMANDS.contains(sub)) {
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        if (!p.hasPermission("faction.admin") && !p.isOp()) {
                            continue;
                        }
                    }
                }
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            
            // 지급 명령어: 온라인 플레이어 목록
            if (subCmd.equals("지급") || subCmd.equals("give")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    if (p.hasPermission("faction.admin") || p.isOp()) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            if (online.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(online.getName());
                            }
                        }
                    }
                }
            } else if (sender instanceof Player) {
                // 다른 명령어: 코어 번호
                Player player = (Player) sender;
                Faction faction = plugin.getFactionManager().getPlayerFaction(player.getUniqueId());
                
                if (faction != null) {
                    List<Core> cores = coreManager.getFactionCores(faction.getId());
                    for (int i = 1; i <= cores.size(); i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            
            // 지급 명령어: 수량
            if (subCmd.equals("지급") || subCmd.equals("give")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    if (p.hasPermission("faction.admin") || p.isOp()) {
                        completions.addAll(Arrays.asList("1", "5", "10", "16", "32", "64"));
                    }
                }
            }
        }
        
        return completions;
    }
}
