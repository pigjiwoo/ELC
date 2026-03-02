package org.examdd.sang.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.examdd.sang.gui.ShopGUI;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ShopGUI gui;
    private static final List<String> SUBS = List.of("판매", "도움말");

    public ShopCommand(ShopGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용 가능합니다.");
            return true;
        }
        if (args.length == 0) {
            gui.openMain(player);
            return true;
        }
        switch (args[0]) {
            case "판매", "sell" -> {
                if (args.length >= 2 && (args[1].equals("전체") || args[1].equalsIgnoreCase("all"))) {
                    gui.sellAll(player);
                } else {
                    player.sendMessage("§c사용법: /상점 판매 전체");
                }
            }
            case "도움말", "help" -> {
                player.sendMessage("§6§l═══ 상점 도움말 ═══");
                player.sendMessage("§f/상점           §7- 상점 GUI 열기");
                player.sendMessage("§f/상점 판매 전체  §7- 인벤토리 일괄 판매");
                player.sendMessage("§f/상점 도움말     §7- 이 메시지 표시");
            }
            default -> gui.openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                      @NotNull String label, String[] args) {
        if (args.length == 1)
            return SUBS.stream().filter(s -> s.startsWith(args[0])).toList();
        if (args.length == 2 && (args[0].equals("판매") || args[0].equals("sell")))
            return List.of("전체");
        return Collections.emptyList();
    }
}
