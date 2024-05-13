package net.coalcube.bansystem.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.sql.MySQL;
import net.coalcube.bansystem.core.sql.SQLite;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.util.SpigotConfig;
import net.coalcube.bansystem.velocity.listener.PlayerChatEvent;
import net.coalcube.bansystem.velocity.util.VelocityConfig;
import net.coalcube.bansystem.velocity.util.VelocityUser;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Plugin(id = "bansystemwithids", name = "BanSystem", version = "2.9",
        url = "https://www.spigotmc.org/resources/bansystem-mit-ids-spigot-bungeecord.65863/",
        description = "Punishment System", authors = {"Tobi"})
public class BanSystemVelocity implements BanSystem {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private static BanManager banManager;
    private static IDManager idManager;
    private static URLUtil urlUtil;
    private static ConfigurationUtil configurationUtil;
    private BlacklistUtil blacklistUtil;
    private Database sql;
    private MySQL mysql;
    private TimeFormatUtil timeFormatUtil;
    private Config config, messages, blacklist;
    private static String Banscreen;
    private static List<String> blockedCommands, ads, blockedWords;
    private File sqlitedatabase, configFile, messagesFile, blacklistFile;
    private String hostname, database, user, pw;
    private int port;
    public static String prefix = "§8§l┃ §cBanSystem §8» §7";

    @Inject
    public BanSystemVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        onEnable();
    }

    public void onEnable() {

        BanSystem.setInstance(this);

        PluginManager pluginmanager = server.getPluginManager();

        UpdateChecker updatechecker = new UpdateChecker(65863);

        server.sendMessage(Component.text("§c  ____                    ____                  _                      "));
        server.sendMessage(Component.text("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  "));
        server.sendMessage(Component.text("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ "));
        server.sendMessage(Component.text("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |"));
        server.sendMessage(Component.text("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|"));
        server.sendMessage(Component.text("§c                                  |___/                           §7v" + this.getVersion()));

        createConfig();

        configurationUtil = new ConfigurationUtil(config, messages, blacklist, configFile, messagesFile, blacklistFile, this);
        timeFormatUtil = new TimeFormatUtil(configurationUtil);
        try {
            configurationUtil.update();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        loadConfig();

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            sql = mysql;
            banManager = new BanManagerMySQL(mysql);
            try {
                mysql.connect();
                logger.info(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                logger.info(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                logger.info(prefix + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml.");
                logger.info(prefix + "§cDebug Message: §e" + e.getMessage());
            }
            try {
                if(mysql.isConnected()) {
                    if(mysql.isOldDatabase()) {
                        logger.info(prefix + "§7Die MySQL Daten vom dem alten BanSystem wurden §2importiert§7.");
                    }
                    mysql.createTables(config);
                    logger.info(prefix + "§7Die MySQL Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException | ExecutionException | InterruptedException e) {
                logger.info(prefix + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden.");
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    logger.info(prefix + "§7Die Ban IDs wurden §2synchronisiert§7.");
                }

            } catch (SQLException e) {
                logger.info(prefix + "§7Die IDs konnten nicht mit MySQL synchronisiert werden.");
                e.printStackTrace();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

        } else {
            createFileDatabase();
            SQLite sqlite = new SQLite(sqlitedatabase);
            banManager = new BanManagerSQLite(sqlite);
            sql = sqlite;
            try {
                sqlite.connect();
                logger.info(prefix + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                logger.info(prefix + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                logger.info(prefix + "§cBitte überprüfe die eingetragenen SQlite daten in der Config.yml.");
                e.printStackTrace();
            }
            try {
                if(sqlite.isConnected()) {
                    sqlite.createTables(config);
                    logger.info(prefix + "§7Die SQLite Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException e) {
                logger.info(prefix + "§7Die SQLite Tabellen §ckonnten nicht §7erstellt werden.");
                logger.info(prefix + e.getMessage() + " " + e.getCause());
            }
        }


        server.getScheduler()
                .buildTask(this, UUIDFetcher::clearCache)
                .delay(1, TimeUnit.HOURS)
                .schedule();

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.enable"))
            logger.info(
                    prefix + "§cBitte trage die IP des Servers in der config.yml ein.");


        logger.info(prefix + "§7Das BanSystem wurde gestartet.");

        try {
            if (updatechecker.checkForUpdates()) {
                logger.info(prefix + "§cEin neues Update ist verfügbar.");
                logger.info(prefix + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        idManager = new IDManager(config, sql, new File(dataDirectory.toFile(), "config.yml"));
        urlUtil = new URLUtil(configurationUtil, config);
        blacklistUtil = new BlacklistUtil(blacklist);

    }

    @Override
    public void onDisable() {
        try {
            if (sql.isConnected())
                sql.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.info(BanSystemVelocity.prefix + "§7Das BanSystem wurde gestoppt.");

    }


    // create Config files
    private void createConfig() {
        try {
            if(!dataDirectory.toFile().exists()) {
                dataDirectory.toFile().mkdir();
            }

            configFile = new File(dataDirectory.toFile(), "config.yml");
            if (!configFile.exists()) {
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.yml");
                Files.copy(in, configFile.toPath());
                config = new VelocityConfig(configFile);
            }

            messagesFile = new File(dataDirectory.toFile(), "messages.yml");
            if (!messagesFile.exists()) {
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("messages.yml");
                Files.copy(in, messagesFile.toPath());
                messages = new VelocityConfig(messagesFile);
            }

            blacklistFile = new File(dataDirectory.toFile(), "blacklist.yml");
            if (!blacklistFile.exists()) {
                InputStream in = this.getClass().getClassLoader().getResourceAsStream("blacklist.yml");
                Files.copy(in, blacklistFile.toPath());
                blacklist = new VelocityConfig(blacklistFile);
            }
            messages = new VelocityConfig(configFile);
            config = new VelocityConfig(messagesFile);
            blacklist = new VelocityConfig(blacklistFile);

        } catch (IOException e) {
            System.err.println("[Bansystem] Dateien konnten nicht erstellt werden.");
        }
    }

    private void createFileDatabase() {
        try {
            sqlitedatabase = new File(dataDirectory.toFile(), "database.db");

            if (!sqlitedatabase.exists()) {
                sqlitedatabase.createNewFile();
            }
        } catch (IOException e) {
            logger.error(prefix + "Die SQLite datenbank konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {
        try {
            prefix = messages.getString("prefix").replaceAll("&", "§");

            Banscreen = "";
            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                if (Banscreen == null) {
                    Banscreen = screen.replaceAll("%P%", prefix) + "\n";
                } else
                    Banscreen = Banscreen + screen.replaceAll("%P%", prefix) + "\n";
            }
            user = config.getString("mysql.user");
            hostname = config.getString("mysql.host");
            port = config.getInt("mysql.port");
            pw = config.getString("mysql.password");
            database = config.getString("mysql.database");

            ads = new ArrayList<>();
            blockedCommands = new ArrayList<>();
            blockedWords = new ArrayList<>();

            ads.addAll(blacklist.getStringList("Ads"));

            blockedCommands.addAll(config.getStringList("mute.blockedCommands"));

            blockedWords.addAll(blacklist.getStringList("Words"));

        } catch (NullPointerException e) {
            System.err.println("[Bansystem] Es ist ein Fehler beim laden der Config/messages Datei aufgetreten.");
            e.printStackTrace();
        }
    }

    @Override
    public Database getSQL() {
        return sql;
    }

    @Override
    public User getUser(String name) {
        return new VelocityUser(server.getPlayer(name).get());
    }

    @Override
    public User getUser(UUID uniqueId) {
        return new VelocityUser(server.getPlayer(uniqueId).get());
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        CommandManager commandManager = server.getCommandManager();

        CommandMeta commandBanMeta = commandManager.metaBuilder("ban")
                .plugin(this)
                .build();
        CommandMeta commandBanSystemMeta = commandManager.metaBuilder("bansystem")
                .aliases("bansys")
                .plugin(this)
                .build();
        CommandMeta commandCheckMeta = commandManager.metaBuilder("check")
                .plugin(this)
                .build();
        CommandMeta commandDeleteHistoryMeta = commandManager.metaBuilder("deletehistory")
                .aliases("delhistory")
                .plugin(this)
                .build();
        CommandMeta commandHistoryMeta = commandManager.metaBuilder("history")
                .plugin(this)
                .build();
        CommandMeta commandKickMeta = commandManager.metaBuilder("kick")
                .plugin(this)
                .build();
        CommandMeta commandUnBanMeta = commandManager.metaBuilder("unban")
                .plugin(this)
                .build();
        CommandMeta commandUnMuteMeta = commandManager.metaBuilder("unmute")
                .plugin(this)
                .build();

        SimpleCommand commandBan = new CommandWrapper(
                new CMDban(banManager, config, messages, sql, configurationUtil));
        SimpleCommand commandBanSystem = new CommandWrapper(
                new CMDbansystem(config, sql, mysql, idManager, timeFormatUtil, banManager, configurationUtil));
        SimpleCommand commandCheck = new CommandWrapper(
                new CMDcheck(banManager, sql, configurationUtil));
        SimpleCommand commandDeleteHistory = new CommandWrapper(
                new CMDdeletehistory(banManager, sql, configurationUtil));
        SimpleCommand commandHistory = new CommandWrapper(
                new CMDhistory(banManager, config, sql, configurationUtil));
        SimpleCommand commandKick = new CommandWrapper(
                new CMDkick(sql, banManager, configurationUtil));
        SimpleCommand commandUnBan = new CommandWrapper(
                new CMDunban(banManager, sql, config, configurationUtil));
        SimpleCommand commandUnMute = new CommandWrapper(
                new CMDunmute(banManager, config, sql, configurationUtil));

        commandManager.register(commandBanMeta, commandBan);
        commandManager.register(commandBanSystemMeta, commandBanSystem);
        commandManager.register(commandCheckMeta, commandCheck);
        commandManager.register(commandDeleteHistoryMeta, commandDeleteHistory);
        commandManager.register(commandHistoryMeta, commandHistory);
        commandManager.register(commandKickMeta, commandKick);
        commandManager.register(commandUnBanMeta, commandUnBan);
        commandManager.register(commandUnMuteMeta, commandUnMute);

        server.getEventManager().register(this,
                new PlayerChatEvent(server, banManager, config, sql, blacklistUtil, configurationUtil));
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return timeFormatUtil;
    }

    @Override
    public String getBanScreen() {
        return Banscreen;
    }

    @Override
    public void sendConsoleMessage(String msg) {
        for (String line : msg.split("\n")) {
            logger.info(line);
        }
    }

    @Override
    public InputStream getResourceAsInputStream(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

    @Override
    public List<User> getAllPlayers() {
        List<User> users = new ArrayList<>();
        for (Player p : server.getAllPlayers()) {
            users.add(new VelocityUser(p));
        }
        return users;
    }

    @Override
    public User getConsole() {
        return new VelocityUser((Player) server.getConsoleCommandSource());
    }

    @Override
    public String getVersion() {
        PluginContainer pluginContainer = server.getPluginManager().getPlugin("bansystemwithids").orElse(null);
        if (pluginContainer != null) {
            return pluginContainer.getDescription().getVersion().orElse("Version not available");
        } else {
            return "Plugin not found";
        }
    }

    @Override
    public ConfigurationUtil getConfigurationUtil() {
        return configurationUtil;
    }

    public static BanManager getBanmanager() {
        return banManager;
    }

    @Override
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof Player) {
            ((Player) u.getRawUser()).disconnect(Component.text(msg));
        }
    }
}
