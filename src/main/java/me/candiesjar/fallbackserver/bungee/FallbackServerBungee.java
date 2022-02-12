package me.candiesjar.fallbackserver.bungee;

import com.google.common.io.ByteStreams;
import me.candiesjar.fallbackserver.bungee.commands.HubCommand;
import me.candiesjar.fallbackserver.bungee.commands.SubCommandManager;
import me.candiesjar.fallbackserver.bungee.enums.ConfigFields;
import me.candiesjar.fallbackserver.bungee.enums.MessagesFields;
import me.candiesjar.fallbackserver.bungee.listeners.ChatListener;
import me.candiesjar.fallbackserver.bungee.listeners.FallbackListener;
import me.candiesjar.fallbackserver.bungee.metrics.Metrics;
import me.candiesjar.fallbackserver.bungee.objects.FallingServer;
import me.candiesjar.fallbackserver.bungee.utils.Utils;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class FallbackServerBungee extends Plugin {

    private static FallbackServerBungee instance;
    private Configuration config;
    private Configuration messagesConfig;
    private final List<ServerInfo> availableServers = new ArrayList<>();

    public static FallbackServerBungee getInstance() {
        return instance;
    }

    public void onEnable() {

        // Instances
        getLogger().info("Loader FallBackServer");
        instance = this;
        loadConfig();
        loadMessages();

        // Listeners
        loadListeners();

        // Commands
        loadCommands();

        // Stats
        startMetrics();

        // Setup

        getLogger().info("§7Loadede fallbackserver!");

        startCheck();
        checkLobbies();
    }

    public void onDisable() {
        instance = null;
        availableServers.clear();
        getLogger().info("§7[§c!§7] Slår FallBackServer fra");
    }

    private void loadCommands() {
        getProxy().getPluginManager().registerCommand(this, new SubCommandManager());
        if (ConfigFields.USE_HUB_COMMAND.getBoolean()) {
            getProxy().getPluginManager().registerCommand(this, new HubCommand());
        }
    }

    private File loadConfigurations(String resource) {
        File folder = getDataFolder();
        if (!folder.exists())
            folder.mkdir();
        File resourceFile = new File(folder, resource);
        try {
            if (!resourceFile.exists()) {
                resourceFile.createNewFile();
                try (InputStream in = getResourceAsStream(resource);
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }

    private void loadConfig() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(
                    loadConfigurations("config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadMessages() {
        try {
            messagesConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(
                    loadConfigurations("messages.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkLobbies() {
        getProxy().getScheduler().schedule(this, () -> {
            FallingServer.getServers().clear();
            for (String serverName : ConfigFields.LOBBIES.getStringList()) {
                ServerInfo serverInfo = getProxy().getServerInfo(serverName);

                if (serverInfo == null) {
                    continue;
                }

                serverInfo.ping((result, error) -> {
                    if (error == null) {
                        new FallingServer(serverInfo);
                    }
                });
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void startCheck() {
        if (ConfigFields.UPDATE_CHECKER.getBoolean())
            if (Utils.getUpdates())
                getLogger().info(MessagesFields.NEW_UPDATE.getFormattedString()
                        .replace("%prefix%", MessagesFields.PREFIX.getFormattedString()));
    }

    private void loadListeners() {
        getProxy().getPluginManager().registerListener(this, new FallbackListener(this));
        if (ConfigFields.DISABLE_SERVERS.getBoolean())
            getProxy().getPluginManager().registerListener(this, new ChatListener());
    }

    private void startMetrics() {
        if (ConfigFields.STATS.getBoolean())
            new Metrics(this, 11817);
    }

    public boolean isHub(ServerInfo server) {
        return ConfigFields.LOBBIES.getStringList().contains(server.getName());
    }

    public Configuration getConfig() {
        return config;
    }

    public Configuration getMessagesConfig() {
        return messagesConfig;
    }

    public void reloadConfig() {
        loadConfig();
        loadMessages();
    }
}
