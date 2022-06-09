package ru.aiefu.fabricrestart;

import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class ServerWatcher {
    private final RestartDataHolder rda;
    private final boolean enableTpsW;
    private final boolean enableMemW;

    //Tps Watcher
    private final float tpsThreshold;
    private final int tpsDelay;
    private final boolean killInstantly;
    private final String killMsg;

    private int tpsWatcherTimer;
    private boolean tpsWatcherTriggered;

    //MemoryWatcher
    private final long memThreshold;
    private final boolean killInstantlyM;
    private final String memKillMsg;

    private boolean memWatcherTriggered;

    public ServerWatcher(MinecraftServer server, ConfigManager.Config cfg, RestartDataHolder rda){

        this.rda = rda;
        this.enableTpsW = cfg.enableTpsWatcher;
        this.tpsThreshold = cfg.tpsThreshold;
        this.tpsDelay = cfg.tpsDelay;
        this.killInstantly = cfg.tpsKillInstantly;
        this.killMsg = cfg.tpsKillMsg;

        this.enableMemW = cfg.enableMemoryWatcher;
        this.memThreshold = cfg.memThreshold * 1024 * 1024;
        this.killInstantlyM = cfg.memKillInstantly;
        this.memKillMsg = cfg.memKillMsg;

        if(enableTpsW && enableMemW){
            FabricRestart.executor.scheduleAtFixedRate(() -> watch(server), 0L, 1L, TimeUnit.SECONDS);
        } else if(enableTpsW){
            FabricRestart.executor.scheduleAtFixedRate(() -> watchTps(server), 0L, 1L, TimeUnit.SECONDS);
        } else if(enableMemW){
            FabricRestart.executor.scheduleAtFixedRate(() -> watchMemory(server), 0L, 1L, TimeUnit.SECONDS);
        }
    }
    private void watch(MinecraftServer server){
        watchTps(server);
    }

    private void watchTps(MinecraftServer server){
        double tps = ((ITPS) server).getAverageTPS();
        if(tps < tpsThreshold){
            ++tpsWatcherTimer;
        } else tpsWatcherTimer = 0;
        if(tpsWatcherTimer > tpsDelay && !tpsWatcherTriggered){
            if(!killInstantly) {
                server.getPlayerManager().getPlayerList().forEach(playerEntity -> playerEntity
                        .sendMessage(Text.literal(killMsg).setStyle(Style.EMPTY.withColor(TextColor.parse("RED"))), MessageType.SYSTEM));
                initiateShutdown(server, System.currentTimeMillis() + 20000, false);
            } else initiateShutdown(server, 0, true);
            tpsWatcherTriggered = true;
        }
    }

    private void watchMemory(MinecraftServer server){
        com.sun.management.OperatingSystemMXBean sys = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        if(sys.getFreeMemorySize() < memThreshold && !memWatcherTriggered){
            if(!killInstantlyM) {
                server.getPlayerManager().getPlayerList().forEach(playerEntity -> playerEntity
                        .sendMessage(Text.literal(memKillMsg).setStyle(Style.EMPTY.withColor(TextColor.parse("RED"))), MessageType.SYSTEM));
                initiateShutdown(server, System.currentTimeMillis() + 20000, false);
            } else initiateShutdown(server, 0, true);
            memWatcherTriggered = true;
        }
    }

    private void initiateShutdown(MinecraftServer server, long ms, boolean now){
        if(!now) {
            if (rda != null) {
                rda.setRestartTime(ms);
                rda.disableMessages();
            } else FabricRestart.executor.schedule(() -> server.stop(false), 20L, TimeUnit.SECONDS);
        } else server.stop(false);
    }
}
