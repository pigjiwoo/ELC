package org.examdd.sang;

import org.bukkit.plugin.java.JavaPlugin;
import org.examdd.sang.command.ShopCommand;
import org.examdd.sang.gui.ShopGUI;
import org.examdd.sang.listener.ShopListener;
import org.examdd.sang.manager.EconomyManager;

public final class Sang extends JavaPlugin {

    private EconomyManager economyManager;
    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        economyManager = new EconomyManager(this);
        if (!economyManager.isEnabled()) {
            getLogger().severe("Vault Economy 연동 실패! 플러그인을 비활성화합니다.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shopGUI = new ShopGUI(this);

        var cmd = getCommand("상점");
        if (cmd != null) {
            ShopCommand shopCommand = new ShopCommand(shopGUI);
            cmd.setExecutor(shopCommand);
            cmd.setTabCompleter(shopCommand);
        }

        getServer().getPluginManager().registerEvents(new ShopListener(this, shopGUI), this);
        getLogger().info("상점 플러그인 활성화 완료!");
    }

    @Override
    public void onDisable() {
        getLogger().info("상점 플러그인 비활성화.");
    }

    public EconomyManager getEconomyManager() { return economyManager; }
    public ShopGUI getShopGUI() { return shopGUI; }
}
