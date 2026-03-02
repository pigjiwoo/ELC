package com.elcserver.elcrules;

import com.elcserver.elcrules.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public class ELCRules extends JavaPlugin {

    private static ELCRules instance;

    @Override
    public void onEnable() {
        instance = this;

        // 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(new GolemListener(), this);
        getServer().getPluginManager().registerEvents(new AutomationListener(), this);
        getServer().getPluginManager().registerEvents(new MilkListener(), this);
        getServer().getPluginManager().registerEvents(new BrewingListener(), this);
        getServer().getPluginManager().registerEvents(new DimensionListener(), this);
        getServer().getPluginManager().registerEvents(new VillagerListener(), this);
        getServer().getPluginManager().registerEvents(new SpawnProtectionListener(), this);

        getLogger().info("ELC 규칙 플러그인이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ELC 규칙 플러그인이 비활성화되었습니다!");
    }

    public static ELCRules getInstance() {
        return instance;
    }
}
