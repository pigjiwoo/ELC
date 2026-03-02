package org.examdd.sang.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.examdd.sang.Sang;

import java.util.UUID;

public class EconomyManager {

    private final Sang plugin;
    private Economy economy;
    private boolean enabled = false;

    public EconomyManager(Sang plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault 플러그인이 없습니다! 상점을 사용할 수 없습니다.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("Economy 프로바이더가 없습니다! EssentialsX 등을 설치하세요.");
            return;
        }
        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Vault Economy 연동 성공: " + economy.getName());
    }

    public boolean isEnabled() { return enabled && economy != null; }

    public double getBalance(UUID uuid) {
        if (!isEnabled()) return 0;
        return economy.getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public long getBalanceLong(UUID uuid) {
        return (long) getBalance(uuid);
    }

    public boolean has(UUID uuid, long amount) {
        if (!isEnabled()) return false;
        return economy.has(Bukkit.getOfflinePlayer(uuid), amount);
    }

    public boolean withdraw(UUID uuid, long amount) {
        if (!isEnabled() || amount <= 0) return false;
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        if (!economy.has(p, amount)) return false;
        return economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(UUID uuid, long amount) {
        if (!isEnabled() || amount <= 0) return false;
        return economy.depositPlayer(Bukkit.getOfflinePlayer(uuid), amount).transactionSuccess();
    }

    public String getCurrencyName() {
        if (!isEnabled()) return "쿤";
        String name = economy.currencyNamePlural();
        return (name == null || name.isBlank()) ? "쿤" : name;
    }
}
