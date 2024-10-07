package net.coalcube.bansystem.bungee.listener;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.coalcube.bansystem.bungee.BanSystemBungee;
import net.coalcube.bansystem.bungee.util.BungeeUser;
import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.ban.Ban;
import net.coalcube.bansystem.core.ban.BanManager;
import net.coalcube.bansystem.core.ban.Type;
import net.coalcube.bansystem.core.listener.Event;
import net.coalcube.bansystem.core.listener.LoginListener;
import net.coalcube.bansystem.core.sql.Database;
import net.coalcube.bansystem.core.textcomponent.TextComponentmd5;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.core.uuidfetcher.UUIDFetcher;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.geysermc.floodgate.api.FloodgateApi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BungeeLoginListener implements Listener {

    private final BanSystem banSystem;
    private final BanManager banManager;
    private final YamlDocument config;
    private final Database sql;
    private final URLUtil urlUtil;
    private final ConfigurationUtil configurationUtil;
    private static Map<String, Boolean> vpnIpCache;
    private final LoginListener loginListener;

    public BungeeLoginListener(BanSystem banSystem, BanManager banManager, YamlDocument config, Database sql, URLUtil urlUtil, ConfigurationUtil configurationUtil, IDManager idManager) {
        this.banSystem = banSystem;
        this.banManager = banManager;
        this.config = config;
        this.sql = sql;
        this.urlUtil = urlUtil;
        this.configurationUtil = configurationUtil;

        net.coalcube.bansystem.core.textcomponent.TextComponent textComponent = new TextComponentmd5(configurationUtil);
        this.loginListener = new LoginListener(banSystem, banManager, configurationUtil, sql, idManager, urlUtil, textComponent);

        vpnIpCache = new HashMap<>();
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent e) {
        PendingConnection con = e.getConnection();
        UUID uuid = con.getUniqueId();
        String username = con.getName();
        InetAddress ip = e.getConnection().getAddress().getAddress();

        e.registerIntent(BanSystemBungee.getInstance());
        new Thread(() -> {
            Event event = loginListener.onJoin(uuid, username, ip);
            e.setCancelled(event.isCancelled());
            e.setReason(new TextComponent(event.getCancelReason()));

            if (!e.isCancelled()) {
                ProxyServer.getInstance().getScheduler().schedule(BanSystemBungee.getInstance(), () -> {
                    User user = banSystem.getUser(uuid);
                    try {
                        Event postEvent = loginListener.onPostJoin(user, ip);
                        e.setCancelled(postEvent.isCancelled());
                        e.setReason(new TextComponent(postEvent.getCancelReason()));
                    } catch (SQLException | ExecutionException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }, 1, TimeUnit.SECONDS);
            }
            e.completeIntent(BanSystemBungee.getInstance());
        }).start();
    }
}