package org.examdd.sang.shop;

import org.bukkit.Material;

import java.util.*;
import java.util.stream.Collectors;

public class ShopRegistry {

    private static final Map<String, ShopItem> ITEMS = new LinkedHashMap<>();

    static {
        sell("cobblestone",   "§f조약돌",       Material.COBBLESTONE,    30,   64);
        sell("coal",          "§f석탄",          Material.COAL,           64,   64);
        sell("raw_copper",    "§f구리 원석",     Material.RAW_COPPER,     64,   64);
        sell("copper_ingot",  "§f구리 주괴",     Material.COPPER_INGOT,   128,  64);
        sell("raw_iron",      "§f철 원석",       Material.RAW_IRON,       256,  64);
        sell("iron_ingot",    "§f철 주괴",       Material.IRON_INGOT,     384,  64);
        sell("redstone",      "§f레드스톤",      Material.REDSTONE,       128,  64);
        sell("raw_gold",      "§f금 원석",       Material.RAW_GOLD,       384,  64);
        sell("gold_ingot",    "§f금 주괴",       Material.GOLD_INGOT,     512,  64);
        sell("lapis",         "§9청금석",        Material.LAPIS_LAZULI,   192,  64);
        sell("diamond",       "§b다이아몬드",    Material.DIAMOND,        2304, 64);
        sell("amethyst",      "§d자수정",        Material.AMETHYST_SHARD, 640,  64);
        sell("emerald",       "§a에메랄드",      Material.EMERALD,        2816, 64);

        buySell("wheat",          "§f밀",       Material.WHEAT,          120, 50,  64);
        buySell("potato",         "§f감자",      Material.POTATO,         100, 40,  64);
        buySell("carrot",         "§f당근",      Material.CARROT,         150, 120, 64);
        buySell("beetroot",       "§f비트",      Material.BEETROOT,       130, 130, 64);
        buySell("sugar_cane",     "§f사탕수수",  Material.SUGAR_CANE,     180, 160, 64);
        buySell("cocoa_beans",    "§f코코아 콩", Material.COCOA_BEANS,    250, 140, 64);
        buySell("pumpkin",        "§f호박",      Material.PUMPKIN,        350, 200, 64);
        buySell("melon_slice",    "§f수박",      Material.MELON_SLICE,    300, 80,  64);
        sell("beef",              "§f생고기",    Material.BEEF,           100, 64);
        sell("leather",           "§f가죽",      Material.LEATHER,        230, 64);
        sell("white_wool",        "§f양털",      Material.WHITE_WOOL,     150, 64);
        sell("feather",           "§f깃털",      Material.FEATHER,        80,  64);
        buySell("nether_wart",    "§c네더와트",  Material.NETHER_WART,    800, 800, 4);
        buy("wheat_seeds",    "§f씨앗",          Material.WHEAT_SEEDS,    120, 64);
        buy("melon_seeds",    "§f수박 씨",        Material.MELON_SEEDS,    300, 64);
        buy("pumpkin_seeds",  "§f호박 씨",        Material.PUMPKIN_SEEDS,  350, 64);
        buy("beetroot_seeds", "§f비트 씨",        Material.BEETROOT_SEEDS, 130, 64);

        sell("oak_log",              "§2참나무 원목",         Material.OAK_LOG,              60, 64);
        sell("birch_log",            "§f자작나무 원목",       Material.BIRCH_LOG,            60, 64);
        sell("spruce_log",           "§8가문비 원목",         Material.SPRUCE_LOG,           60, 64);
        sell("jungle_log",           "§2정글 원목",           Material.JUNGLE_LOG,           60, 64);
        sell("acacia_log",           "§6아카시아 원목",       Material.ACACIA_LOG,           60, 64);
        sell("dark_oak_log",         "§8짙은참나무 원목",     Material.DARK_OAK_LOG,         60, 64);
        sell("mangrove_log",         "§4맹그로브 원목",       Material.MANGROVE_LOG,         60, 64);
        sell("cherry_log",           "§d벚나무 원목",         Material.CHERRY_LOG,           60, 64);
        sell("stripped_oak_log",     "§2껍질 벗긴 참나무",    Material.STRIPPED_OAK_LOG,     70, 64);
        sell("stripped_birch_log",   "§f껍질 벗긴 자작나무",  Material.STRIPPED_BIRCH_LOG,   70, 64);
        sell("stripped_spruce_log",  "§8껍질 벗긴 가문비",    Material.STRIPPED_SPRUCE_LOG,  70, 64);
        sell("stripped_jungle_log",  "§2껍질 벗긴 정글",      Material.STRIPPED_JUNGLE_LOG,  70, 64);
        sell("stripped_acacia_log",  "§6껍질 벗긴 아카시아",  Material.STRIPPED_ACACIA_LOG,  70, 64);
        sell("stripped_dark_oak_log","§8껍질 벗긴 짙참",      Material.STRIPPED_DARK_OAK_LOG,70, 64);
        sell("stripped_mangrove_log","§4껍질 벗긴 맹그로브",  Material.STRIPPED_MANGROVE_LOG,70, 64);
        sell("stripped_cherry_log",  "§d껍질 벗긴 벚나무",    Material.STRIPPED_CHERRY_LOG,  70, 64);

        sell("pufferfish",     "§e복어",      Material.PUFFERFISH,     300,  64);
        sell("cod",            "§f대구",      Material.COD,            120,  64);
        sell("salmon",         "§f연어",      Material.SALMON,         150,  64);
        sell("tropical_fish",  "§6열대어",    Material.TROPICAL_FISH,  350,  64);
        sell("enchanted_book", "§5인챈트 북", Material.ENCHANTED_BOOK, 1200, 1);

        buy("soul_sand",       "§8영혼 모래",      Material.SOUL_SAND,           3000, 64);
        buy("white_concrete",  "§f흰 콘크리트",    Material.WHITE_CONCRETE,      300,  64);
        buy("orange_concrete", "§6주황 콘크리트",  Material.ORANGE_CONCRETE,     300,  64);
        buy("magenta_concrete","§d자홍 콘크리트",  Material.MAGENTA_CONCRETE,    300,  64);
        buy("lb_concrete",     "§b하늘 콘크리트",  Material.LIGHT_BLUE_CONCRETE, 300,  64);
        buy("yellow_concrete", "§e노랑 콘크리트",  Material.YELLOW_CONCRETE,     300,  64);
        buy("lime_concrete",   "§a연두 콘크리트",  Material.LIME_CONCRETE,       300,  64);
        buy("pink_concrete",   "§d분홍 콘크리트",  Material.PINK_CONCRETE,       300,  64);
        buy("gray_concrete",   "§8회색 콘크리트",  Material.GRAY_CONCRETE,       300,  64);
        buy("lg_concrete",     "§7밝은회색 콘크리트", Material.LIGHT_GRAY_CONCRETE, 300, 64);
        buy("cyan_concrete",   "§3청록 콘크리트",  Material.CYAN_CONCRETE,       300,  64);
        buy("purple_concrete", "§5보라 콘크리트",  Material.PURPLE_CONCRETE,     300,  64);
        buy("blue_concrete",   "§1파랑 콘크리트",  Material.BLUE_CONCRETE,       300,  64);
        buy("brown_concrete",  "§6갈색 콘크리트",  Material.BROWN_CONCRETE,      300,  64);
        buy("green_concrete",  "§2초록 콘크리트",  Material.GREEN_CONCRETE,      300,  64);
        buy("red_concrete",    "§c빨강 콘크리트",  Material.RED_CONCRETE,        300,  64);
        buy("black_concrete",  "§0검정 콘크리트",  Material.BLACK_CONCRETE,      300,  64);
        buy("glass",           "§b유리",           Material.GLASS,               280,  64);
        buy("white_glass",     "§f흰 유리",        Material.WHITE_STAINED_GLASS, 280,  64);
        buy("orange_glass",    "§6주황 유리",      Material.ORANGE_STAINED_GLASS,280,  64);
        buy("clay",            "§7점토",           Material.CLAY,                300,  64);
        buy("wool_buy",        "§f양털",           Material.WHITE_WOOL,          170,  64);

        buy("diamond_pickaxe",     "§b다이아 곡괭이", Material.DIAMOND_PICKAXE,     800,  1);
        buy("diamond_axe",         "§b다이아 도끼",   Material.DIAMOND_AXE,         800,  1);
        buy("diamond_shovel",      "§b다이아 삽",     Material.DIAMOND_SHOVEL,      800,  1);
        buy("diamond_hoe",         "§b다이아 괭이",   Material.DIAMOND_HOE,         800,  1);
        buy("diamond_sword",       "§b다이아 검",     Material.DIAMOND_SWORD,       800,  1);
        buy("diamond_helmet",      "§b다이아 투구",   Material.DIAMOND_HELMET,      1200, 1);
        buy("diamond_chestplate",  "§b다이아 흉갑",   Material.DIAMOND_CHESTPLATE,  1200, 1);
        buy("diamond_leggings",    "§b다이아 각반",   Material.DIAMOND_LEGGINGS,    1200, 1);
        buy("diamond_boots",       "§b다이아 장화",   Material.DIAMOND_BOOTS,       1200, 1);
        buy("netherite_pickaxe",   "§5네라 곡괭이",   Material.NETHERITE_PICKAXE,   1500, 1);
        buy("netherite_axe",       "§5네라 도끼",     Material.NETHERITE_AXE,       1500, 1);
        buy("netherite_shovel",    "§5네라 삽",       Material.NETHERITE_SHOVEL,    1500, 1);
        buy("netherite_hoe",       "§5네라 괭이",     Material.NETHERITE_HOE,       1500, 1);
        buy("netherite_sword",     "§5네라 검",       Material.NETHERITE_SWORD,     1500, 1);
        buy("netherite_helmet",    "§5네라 투구",     Material.NETHERITE_HELMET,    2000, 1);
        buy("netherite_chestplate","§5네라 흉갑",     Material.NETHERITE_CHESTPLATE,2000, 1);
        buy("netherite_leggings",  "§5네라 각반",     Material.NETHERITE_LEGGINGS,  2000, 1);
        buy("netherite_boots",     "§5네라 장화",     Material.NETHERITE_BOOTS,     2000, 1);

        buy("shulker_box",     "§d셜커상자",  Material.SHULKER_BOX,    500,  1);
        buy("ender_chest",     "§5엔더상자",  Material.ENDER_CHEST,    700,  1);
        buy("ender_pearl",     "§5엔더진주",  Material.ENDER_PEARL,    500,  16);

        buy("old_declaration", "§7낡은 격문", Material.PAPER,          800,  1);
        buy("declaration",     "§f격문",      Material.WRITABLE_BOOK,  1500, 1);
    }

    private static void sell(String id, String name, Material mat, long price, int amt) {
        ITEMS.put(id, new ShopItem(id, name, mat, 0, price, amt, categoryOf(mat)));
    }

    private static void buy(String id, String name, Material mat, long price, int amt) {
        ITEMS.put(id, new ShopItem(id, name, mat, price, 0, amt, categoryOf(mat)));
    }

    private static void buySell(String id, String name, Material mat, long buy, long sell, int amt) {
        ITEMS.put(id, new ShopItem(id, name, mat, buy, sell, amt, categoryOf(mat)));
    }

    private static ShopCategory categoryOf(Material mat) {
        String n = mat.name();
        if (n.contains("LOG") || n.contains("STRIPPED"))  return ShopCategory.LOGGING;
        if (n.equals("WHEAT") || n.contains("SEED")    ||
            n.contains("CARROT")  || n.contains("POTATO") ||
            n.contains("SUGAR")   || n.contains("COCOA")  ||
            n.contains("PUMPKIN") || n.contains("MELON")  ||
            n.contains("BEETROOT")|| n.contains("BEEF")   ||
            n.contains("LEATHER") || n.contains("WOOL")   ||
            n.contains("FEATHER") || n.equals("NETHER_WART")) return ShopCategory.FARMING;
        if (n.contains("COD")    || n.contains("SALMON")  ||
            n.contains("PUFFER") || n.contains("TROPICAL")||
            n.equals("ENCHANTED_BOOK"))                    return ShopCategory.FISHING;
        if (n.contains("CONCRETE") || n.contains("GLASS") ||
            n.equals("CLAY") || n.equals("SOUL_SAND"))    return ShopCategory.BLOCKS;
        if (n.contains("DIAMOND") || n.contains("NETHERITE")) return ShopCategory.TOOLS;
        if (n.equals("SHULKER_BOX") || n.equals("ENDER_CHEST") ||
            n.equals("ENDER_PEARL"))                       return ShopCategory.SPECIAL;
        if (n.equals("PAPER") || n.equals("WRITABLE_BOOK")) return ShopCategory.WAR;
        return ShopCategory.MINING;
    }

    public static ShopItem getItem(String id) { return ITEMS.get(id); }

    public static Collection<ShopItem> getAllItems() {
        return Collections.unmodifiableCollection(ITEMS.values());
    }

    public static List<ShopItem> getByCategory(ShopCategory category) {
        return ITEMS.values().stream()
                .filter(i -> i.getCategory() == category)
                .collect(Collectors.toList());
    }

    public static Optional<ShopItem> findSellable(Material material) {
        return ITEMS.values().stream()
                .filter(i -> i.getMaterial() == material && i.isSellable())
                .findFirst();
    }
}
