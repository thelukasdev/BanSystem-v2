package net.coalcube.bansystem.spigot.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.listener.LoginListener;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.textcomponent.TextComponentmd5;
import net.coalcube.bansystem.core.util.ConfigurationUtil;
import net.coalcube.bansystem.core.util.URLUtil;
import net.coalcube.bansystem.spigot.BanSystemSpigot;
import net.coalcube.bansystem.spigot.util.SpigotUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SpigotPlayerConnectionListener implements Listener {

    private final BanSystem banSystem;
    private final BanManager banManager;
    private final YamlDocument config;
    private final String banScreenRow;
    private final Plugin instance;
    private final URLUtil urlUtil;
    private final ConfigurationUtil configurationUtil;
    private static final Map<String, Boolean> vpnIpCache = new HashMap<>();
    private final LoginListener loginListener;

    public SpigotPlayerConnectionListener(
            BanSystem banSystem, BanManager banManager, YamlDocument config, String banScreen, 
            Plugin instance, URLUtil urlUtil, ConfigurationUtil configurationUtil, Database sql, IDManager idManager) {

        this.banSystem = banSystem;
        this.banManager = banManager;
        this.config = config;
        this.banScreenRow = banScreen;
        this.instance = instance;
        this.urlUtil = urlUtil;
        this.configurationUtil = configurationUtil;

        var textComponent = new TextComponentmd5(configurationUtil);
        this.loginListener = new LoginListener(banSystem, banManager, configurationUtil, sql, idManager, urlUtil, textComponent);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        InetAddress ip = event.getAddress();
        Database sql = banSystem.getSQL();

        if (!sql.isConnected()) {
            return;
        }

        Event joinEvent = loginListener.onJoin(uuid, event.getName(), ip);
        if (joinEvent.isCancelled()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, joinEvent.getCancelReason());
        }
    }

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        try {
            Ban ban = banManager.getBan(player.getUniqueId(), Type.NETWORK);
            if (ban != null) {
                event.setQuitMessage(null);
            }
        } catch (SQLException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        var user = new SpigotUser(player);
        InetAddress ip = player.getAddress().getAddress();

        try {
            Event postJoinEvent = loginListener.onPostJoin(user, ip);
            event.setJoinMessage("");

            if (postJoinEvent.isCancelled()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.kickPlayer(postJoinEvent.getCancelReason());
                    }
                }.runTaskLater(BanSystemSpigot.getPlugin(), 40L);
            }
        } catch (SQLException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
