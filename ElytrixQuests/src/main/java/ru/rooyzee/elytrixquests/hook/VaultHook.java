package ru.rooyzee.elytrixquests.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Economy economy;
    private boolean enabled = false;

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        enabled = true;
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBalance(OfflinePlayer player) {
        return economy != null ? economy.getBalance(player) : 0;
    }
}