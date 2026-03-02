package org.examdd.sang.shop;

import org.bukkit.Material;

public enum ShopCategory {
    MINING ("§6⛏ 광질",       Material.IRON_PICKAXE),
    FARMING("§a🌾 농축산",     Material.WHEAT),
    LOGGING("§2🌳 벌목",       Material.OAK_LOG),
    FISHING("§b🐟 낚시",       Material.COD),
    BLOCKS ("§7🧱 블록",       Material.BRICKS),
    TOOLS  ("§e⚔ 도구·방어구", Material.DIAMOND_SWORD),
    SPECIAL("§d✦ 특수 아이템", Material.ENDER_CHEST),
    WAR    ("§c📜 전쟁 아이템", Material.WRITABLE_BOOK);

    private final String   displayName;
    private final Material icon;

    ShopCategory(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon        = icon;
    }

    public String   getDisplayName() { return displayName; }
    public Material getIcon()        { return icon; }
}
