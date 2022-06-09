package ru.aiefu.fabricrestart;

import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.HashMap;
import java.util.List;

public class RestartDataHolder {
    private long restart_time;
    private final long countdown;
    private MessagesHolder msgData;

    private final String countdownMsg;
    private final boolean enableScript;
    private final String pathToScript;
    private final String disconnectMessage;

    public final boolean tpsWatcherEnabled;
    public final boolean memoryWatcherEnabled;

    private boolean triggered;

    public RestartDataHolder(long restart_time, long countdown, HashMap<Long, String> msgs, List<Long> timestamps, ConfigManager.Config cfg){
        this.restart_time = restart_time;
        this.countdown = countdown;
        this.msgData = new MessagesHolder(timestamps, msgs);

        this.countdownMsg = cfg.countdownMsg;
        this.enableScript = cfg.enableScript;
        this.pathToScript = cfg.pathToScript;
        this.tpsWatcherEnabled = cfg.enableTpsWatcher;
        this.memoryWatcherEnabled = cfg.enableMemoryWatcher;
        this.disconnectMessage = cfg.disconnectMessage;
    }

    public void update(MinecraftServer server, long ms){
        sendTimedMsg(server, ms);
        tryCountdown(server, ms);
        tryRestart(server, ms);
    }

    public void shutdown(MinecraftServer server){
        if(!triggered) {
            triggered = true;
            //server.getPlayerList().getPlayers().forEach(p -> p.connection.disconnect(new TextComponent(disconnectMessage)));
            server.stop(false);
        }
    }

    private void tryRestart(MinecraftServer server, long currentMillis){
        if(currentMillis > restart_time){
            shutdown(server);
        }
    }

    private void tryCountdown(MinecraftServer server, long currentMillis){
        if(currentMillis > countdown){
            server.getPlayerManager().broadcast(Text.literal(String.format(countdownMsg, (restart_time - currentMillis) / 1000)).setStyle(Style.EMPTY.withColor(TextColor.parse("RED"))), MessageType.SYSTEM);
        }
    }

    private void sendTimedMsg(MinecraftServer server, long currentMillis){
        msgData.nextMsg(server, currentMillis);
    }

    public boolean isScriptEnabled() {
        return enableScript;
    }

    public String getPathToScript() {
        return pathToScript;
    }

    public void setRestartTime(long restart_time) {
        this.restart_time = restart_time;
    }

    public long getRestartTime() {
        return restart_time;
    }

    public void disableMessages(){
        this.msgData.disableMsgs = true;
    }

    public static class MessagesHolder {
        private final List<Long> timestamps;
        private long nextMsg;
        private int position;
        private boolean disableMsgs;
        private final HashMap<Long, String> msgs;

        public MessagesHolder(List<Long> timestamps, HashMap<Long, String> msgs) {
            this.timestamps = timestamps;
            this.msgs = msgs;
            setup();
        }

        private void nextMsg(MinecraftServer server, long currentMillis){
            if(!disableMsgs && currentMillis > nextMsg){
                server.getPlayerManager().broadcast(Text.literal(msgs.get(nextMsg)).setStyle(Style.EMPTY.withColor(TextColor.parse("GREEN"))), MessageType.SYSTEM);
                for(ServerPlayerEntity p : server.getPlayerManager().getPlayerList()){
                    p.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1.0F, 1.0F);
                }
                int j = position + 1;
                if(j < msgs.size()){
                    nextMsg = timestamps.get(j);
                    position = j;
                } else {
                    this.disableMsgs = true;
                }
            }
        }
        private void setup(){
            long ms = System.currentTimeMillis();
            for (int i = 0; i < timestamps.size(); i++) {
                long time = timestamps.get(i);
                if (ms < time) {
                    nextMsg = time;
                    position = i;
                    return;
                }
            }
            this.disableMsgs = true;
        }
    }
}
