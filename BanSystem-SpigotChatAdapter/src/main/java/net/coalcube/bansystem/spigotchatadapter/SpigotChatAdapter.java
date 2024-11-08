package net.coalcube.bansystem.spigotchatadapter;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.*;

public final class SpigotChatAdapter extends JavaPlugin implements PluginMessageListener {

    private final String CHANNEL_NAME = "bansys:chatsign";

    @Override
    public void onEnable(){
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL_NAME, this);
        Bukkit.getConsoleSender().sendMessage("§8§l┃ §cBanSystem-ChatAdapter §8» §7Plugin wurde gestartet.");
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, CHANNEL_NAME, this);
    }

    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        if(s.equalsIgnoreCase("bansys:chatsign")){
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            byteArrayInputStream.skip(2);
            try {
                String message = IOUtils.toString(byteArrayInputStream, StandardCharsets.UTF_8);
                player.chat(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
