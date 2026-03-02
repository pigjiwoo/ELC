package org.examdd.sang.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.examdd.sang.Sang;
import org.examdd.sang.manager.EconomyManager;
import org.examdd.sang.shop.ShopCategory;
import org.examdd.sang.shop.ShopItem;
import org.examdd.sang.shop.ShopRegistry;

import java.util.*;

public class ShopGUI {

    private static final String MAIN_TITLE  = "§6§l상점";
    private static final String BUY_SUFFIX  = " §8[§a구매§8]";
    private static final String SELL_SUFFIX = " §8[§c판매§8]";

    private final Sang plugin;
    private final EconomyManager economy;
    private final Map<UUID, ShopSession> sessions = new HashMap<>();

    public ShopGUI(Sang plugin) {
        this.plugin  = plugin;
        this.economy = plugin.getEconomyManager();
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);
        ShopCategory[] cats  = ShopCategory.values();
        int[]          slots = {10, 11, 12, 13, 14, 15, 16, 28};
        for (int i = 0; i < cats.length && i < slots.length; i++) {
            inv.setItem(slots[i], icon(cats[i].getIcon(), cats[i].getDisplayName(),
                    List.of("§7클릭하여 해당 카테고리 상점 열기")));
        }
        inv.setItem(49, icon(Material.GOLD_NUGGET, "§e§l인벤토리 일괄 판매",
                List.of("§7인벤토리의 판매 가능한 아이템을", "§7모두 판매합니다.", "", "§a▶ 클릭")));
        inv.setItem(53, icon(Material.BARRIER, "§c닫기", List.of()));
        sessions.put(player.getUniqueId(), ShopSession.main());
        player.openInventory(inv);
    }

    public void openModeSelect(Player player, ShopCategory cat) {
        Inventory inv = Bukkit.createInventory(null, 27, cat.getDisplayName() + " §8- 모드 선택");
        inv.setItem(11, icon(Material.EMERALD, "§a§l구매",
                List.of("§7상점에서 아이템을 구매합니다.", "", "§a▶ 클릭!")));
        inv.setItem(15, icon(Material.REDSTONE, "§c§l판매",
                List.of("§7아이템을 상점에 판매합니다.", "", "§c▶ 클릭!")));
        inv.setItem(22, icon(Material.DARK_OAK_DOOR, "§f◀ 뒤로", List.of()));
        sessions.put(player.getUniqueId(), ShopSession.modeSelect(cat));
        player.openInventory(inv);
    }

    public void openCategory(Player player, ShopCategory cat, boolean isBuy, int page) {
        List<ShopItem> filtered = ShopRegistry.getByCategory(cat).stream()
                .filter(si -> isBuy ? si.isBuyable() : si.isSellable())
                .toList();

        int perPage    = 45;
        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54,
                cat.getDisplayName() + (isBuy ? BUY_SUFFIX : SELL_SUFFIX));

        int start = page * perPage;
        int end   = Math.min(start + perPage, filtered.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildShopItemIcon(filtered.get(i), isBuy, player));
        }

        if (page > 0)
            inv.setItem(45, icon(Material.ARROW, "§f◀ 이전 페이지", List.of()));
        inv.setItem(49, icon(Material.COMPASS,
                "§f" + (page + 1) + " / " + totalPages + " 페이지", List.of()));
        if (page < totalPages - 1)
            inv.setItem(53, icon(Material.ARROW, "§f다음 페이지 ▶", List.of()));
        inv.setItem(47, icon(Material.DARK_OAK_DOOR, "§f◀ 뒤로 (모드 선택)", List.of()));
        inv.setItem(51, icon(Material.BARRIER, "§c닫기", List.of()));

        sessions.put(player.getUniqueId(), ShopSession.category(cat, isBuy, page, filtered));
        player.openInventory(inv);
    }

    public void openAmountSelect(Player player, ShopItem si, boolean isBuy) {
        Inventory inv = Bukkit.createInventory(null, 27,
                (isBuy ? "§a구매 - " : "§c판매 - ") + si.getDisplayName());

        int[]      amounts = {1, 4, 16, 32, 64};
        int[]      slots   = {10, 11, 12, 13, 14};
        Material[] icons   = {
            Material.STONE_BUTTON, Material.GOLD_NUGGET,
            Material.IRON_INGOT,   Material.GOLD_INGOT, Material.EMERALD
        };

        for (int i = 0; i < amounts.length; i++) {
            int  amt   = amounts[i];
            long price = isBuy ? si.getBuyTotal(amt) : si.getSellTotal(amt);
            if (price < 0) continue;

            List<String> lore = new ArrayList<>();
            lore.add("§7수량: §f" + amt + "개");
            lore.add((isBuy ? "§7비용: §c" : "§7수익: §a") + price + "쿤");

            if (isBuy) {
                long bal = economy.getBalanceLong(player.getUniqueId());
                lore.add("§7보유: §e" + bal + "쿤");
                lore.add(bal >= price ? "§a▶ 클릭하여 구매" : "§c잔액 부족");
            } else {
                int inInv = countInInventory(player, si.getMaterial());
                lore.add("§7인벤토리: §e" + inInv + "개");
                lore.add(inInv >= amt ? "§a▶ 클릭하여 판매" : "§c아이템 부족");
            }
            inv.setItem(slots[i], icon(icons[i], "§f" + amt + "개", lore));
        }
        inv.setItem(22, icon(Material.DARK_OAK_DOOR, "§f◀ 뒤로", List.of()));

        ShopSession prev = sessions.get(player.getUniqueId());
        sessions.put(player.getUniqueId(), ShopSession.amount(si, isBuy,
                prev != null ? prev.category : null,
                prev != null ? prev.page : 0,
                prev != null ? prev.filteredItems : List.of()));
        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot) {
        ShopSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        switch (session.state) {
            case MAIN        -> handleMain(player, slot);
            case MODE_SELECT -> handleModeSelect(player, slot, session);
            case CATEGORY    -> handleCategory(player, slot, session);
            case AMOUNT      -> handleAmount(player, slot, session);
        }
    }

    private void handleMain(Player player, int slot) {
        ShopCategory[] cats  = ShopCategory.values();
        int[]          slots = {10, 11, 12, 13, 14, 15, 16, 28};
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i] && i < cats.length) {
                openModeSelect(player, cats[i]);
                return;
            }
        }
        if (slot == 49) { sellAll(player); openMain(player); }
    }

    private void handleModeSelect(Player player, int slot, ShopSession session) {
        switch (slot) {
            case 11 -> openCategory(player, session.category, true,  0);
            case 15 -> openCategory(player, session.category, false, 0);
            case 22 -> openMain(player);
        }
    }

    private void handleCategory(Player player, int slot, ShopSession session) {
        if (slot == 47) { openModeSelect(player, session.category); return; }
        if (slot == 51) { player.closeInventory(); return; }
        if (slot == 45) { openCategory(player, session.category, session.isBuy, session.page - 1); return; }
        if (slot == 53) { openCategory(player, session.category, session.isBuy, session.page + 1); return; }
        if (slot >= 0 && slot < 45) {
            int idx = session.page * 45 + slot;
            if (idx < session.filteredItems.size()) {
                openAmountSelect(player, session.filteredItems.get(idx), session.isBuy);
            }
        }
    }

    private void handleAmount(Player player, int slot, ShopSession session) {
        if (slot == 22) {
            if (session.category != null)
                openCategory(player, session.category, session.isBuy, session.page);
            else
                openMain(player);
            return;
        }
        int[] amounts = {1, 4, 16, 32, 64};
        int[] slots   = {10, 11, 12, 13, 14};
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) {
                if (session.isBuy) executeBuy(player, session.selectedItem, amounts[i]);
                else               executeSell(player, session.selectedItem, amounts[i]);
                return;
            }
        }
    }

    private void executeBuy(Player player, ShopItem si, int amount) {
        if (si == null) return;
        long cost = si.getBuyTotal(amount);
        if (cost < 0) { player.sendMessage("§c이 아이템은 구매할 수 없습니다."); return; }

        if (!economy.has(player.getUniqueId(), cost)) {
            player.sendMessage("§c잔액 부족! 필요: §e" + cost + "쿤");
            openAmountSelect(player, si, true);
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§c인벤토리가 가득 찼습니다!");
            openAmountSelect(player, si, true);
            return;
        }
        economy.withdraw(player.getUniqueId(), cost);
        player.getInventory().addItem(new ItemStack(si.getMaterial(), amount));
        player.sendMessage("§a[상점] §f" + si.getDisplayName()
                + " §f" + amount + "개 구매 §8(§c-" + cost + "쿤§8)");
        openAmountSelect(player, si, true);
    }

    private void executeSell(Player player, ShopItem si, int amount) {
        if (si == null) return;
        long revenue = si.getSellTotal(amount);
        if (revenue < 0) { player.sendMessage("§c이 아이템은 판매할 수 없습니다."); return; }

        int inInv = countInInventory(player, si.getMaterial());
        if (inInv < amount) {
            player.sendMessage("§c아이템 부족! 보유: §e" + inInv + "개");
            openAmountSelect(player, si, false);
            return;
        }
        removeFromInventory(player, si.getMaterial(), amount);
        economy.deposit(player.getUniqueId(), revenue);
        player.sendMessage("§a[상점] §f" + si.getDisplayName()
                + " §f" + amount + "개 판매 §8(§a+" + revenue + "쿤§8)");
        openAmountSelect(player, si, false);
    }

    public void sellAll(Player player) {
        long total = 0;
        Map<String, Integer> sold = new LinkedHashMap<>();

        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            Optional<ShopItem> opt = ShopRegistry.findSellable(stack.getType());
            if (opt.isEmpty()) continue;
            ShopItem si      = opt.get();
            int      amt     = stack.getAmount();
            long     revenue = si.getSellTotal(amt);
            if (revenue <= 0) continue;
            total += revenue;
            sold.merge(si.getDisplayName(), amt, Integer::sum);
            stack.setAmount(0);
        }

        if (total > 0) {
            economy.deposit(player.getUniqueId(), total);
            player.sendMessage("§a[상점] §f일괄 판매 완료 §8(§a+" + total + "쿤§8)");
            sold.forEach((name, amt) ->
                    player.sendMessage("  §8- §f" + name + " §7" + amt + "개"));
        } else {
            player.sendMessage("§c[상점] 판매 가능한 아이템이 없습니다.");
        }
    }

    public boolean hasSession(Player p)    { return sessions.containsKey(p.getUniqueId()); }
    public void    removeSession(Player p) { sessions.remove(p.getUniqueId()); }

    private int countInInventory(Player player, Material mat) {
        int c = 0;
        for (ItemStack s : player.getInventory().getContents())
            if (s != null && s.getType() == mat) c += s.getAmount();
        return c;
    }

    private void removeFromInventory(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() != mat) continue;
            if (s.getAmount() <= remaining) {
                remaining -= s.getAmount();
                contents[i] = null;
            } else {
                s.setAmount(s.getAmount() - remaining);
                remaining = 0;
            }
        }
        player.getInventory().setContents(contents);
    }

    private ItemStack buildShopItemIcon(ShopItem si, boolean isBuy, Player player) {
        List<String> lore = new ArrayList<>();
        if (isBuy) {
            lore.add("§7구매가: §c" + si.getBuyPrice() + "쿤 / " + si.getAmountPerPrice() + "개");
            lore.add("§7보유 잔액: §e" + economy.getBalanceLong(player.getUniqueId()) + "쿤");
        } else {
            lore.add("§7판매가: §a" + si.getSellPrice() + "쿤 / " + si.getAmountPerPrice() + "개");
            lore.add("§7보유 수량: §e" + countInInventory(player, si.getMaterial()) + "개");
        }
        lore.add("");
        lore.add("§f▶ 클릭하여 " + (isBuy ? "구매" : "판매"));
        return icon(si.getMaterial(), si.getDisplayName(), lore);
    }

    private ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
