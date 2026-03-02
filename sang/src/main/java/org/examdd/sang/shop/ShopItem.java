package org.examdd.sang.shop;

import org.bukkit.Material;

public class ShopItem {

    private final String id;
    private final String displayName;
    private final Material material;
    private final long buyPrice;
    private final long sellPrice;
    private final int amountPerPrice;
    private final ShopCategory category;

    public ShopItem(String id, String displayName, Material material,
                    long buyPrice, long sellPrice, int amountPerPrice,
                    ShopCategory category) {
        this.id             = id;
        this.displayName    = displayName;
        this.material       = material;
        this.buyPrice       = buyPrice;
        this.sellPrice      = sellPrice;
        this.amountPerPrice = amountPerPrice;
        this.category       = category;
    }

    public long getBuyTotal(int amount) {
        if (buyPrice <= 0) return -1;
        return (long) Math.ceil((double) amount / amountPerPrice * buyPrice);
    }

    public long getSellTotal(int amount) {
        if (sellPrice <= 0) return -1;
        return (long) ((double) amount / amountPerPrice * sellPrice);
    }

    public String       getId()             { return id; }
    public String       getDisplayName()    { return displayName; }
    public Material     getMaterial()       { return material; }
    public long         getBuyPrice()       { return buyPrice; }
    public long         getSellPrice()      { return sellPrice; }
    public int          getAmountPerPrice() { return amountPerPrice; }
    public ShopCategory getCategory()       { return category; }
    public boolean      isBuyable()         { return buyPrice > 0; }
    public boolean      isSellable()        { return sellPrice > 0; }
}
