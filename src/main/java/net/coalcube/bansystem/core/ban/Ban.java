package net.coalcube.bansystem.core.ban;

import java.util.Date;
import java.util.UUID;

public class Ban {

    private static final long PERMANENT_BAN = -1L;  // Define constant for permanent ban

    private Type type;
    private String reason, creator, ip;
    private UUID player;
    private Date creationdate;
    private long duration;
    private String id;

    public Ban(String id, UUID player, Type type, String reason, String creator, String ip, Date creationdate, long duration) {
        this.id = id;
        this.player = player;
        this.reason = reason;
        this.creationdate = creationdate;
        this.creator = creator;
        this.ip = ip;
        this.type = type;
        this.duration = duration;
    }

    public Ban(String id, UUID player, Type type, String reason, UUID creator, String ip, Date creationdate, long duration) {
        this(id, player, type, reason, creator.toString(), ip, creationdate, duration);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public Date getCreationdate() {
        return creationdate;
    }

    public void setCreationdate(Date creationdate) {
        this.creationdate = creationdate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    /**
     * Gets the end time of the ban in milliseconds, or PERMANENT_BAN if it is a permanent ban.
     */
    public long getEnd() {
        return isPermanent() ? PERMANENT_BAN : creationdate.getTime() + duration;
    }

    /**
     * Gets the remaining time of the ban in milliseconds, or PERMANENT_BAN if it is a permanent ban.
     */
    public long getRemainingTime() {
        return isPermanent() ? PERMANENT_BAN : getEnd() - System.currentTimeMillis();
    }

    /**
     * Checks if the ban is permanent.
     */
    public boolean isPermanent() {
        return duration == PERMANENT_BAN;
    }

    /**
     * Checks if the ban is currently active based on the remaining time.
     */
    public boolean isActive() {
        return isPermanent() || getRemainingTime() > 0;
    }
}
