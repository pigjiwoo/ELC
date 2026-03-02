package org.examdd.sang.gui;

import org.examdd.sang.shop.ShopCategory;
import org.examdd.sang.shop.ShopItem;

import java.util.Collections;
import java.util.List;

public class ShopSession {

    public enum State { MAIN, MODE_SELECT, CATEGORY, AMOUNT }

    public final State          state;
    public final ShopItem       selectedItem;
    public final boolean        isBuy;
    public final ShopCategory   category;
    public final int            page;
    public final List<ShopItem> filteredItems;

    private ShopSession(State state, ShopItem selectedItem, boolean isBuy,
                        ShopCategory category, int page, List<ShopItem> filteredItems) {
        this.state         = state;
        this.selectedItem  = selectedItem;
        this.isBuy         = isBuy;
        this.category      = category;
        this.page          = page;
        this.filteredItems = filteredItems != null ? filteredItems : Collections.emptyList();
    }

    public static ShopSession main() {
        return new ShopSession(State.MAIN, null, true, null, 0, null);
    }

    public static ShopSession modeSelect(ShopCategory cat) {
        return new ShopSession(State.MODE_SELECT, null, true, cat, 0, null);
    }

    public static ShopSession category(ShopCategory cat, boolean isBuy, int page, List<ShopItem> items) {
        return new ShopSession(State.CATEGORY, null, isBuy, cat, page, items);
    }

    public static ShopSession amount(ShopItem item, boolean isBuy,
                                     ShopCategory cat, int page, List<ShopItem> items) {
        return new ShopSession(State.AMOUNT, item, isBuy, cat, page, items);
    }
}
