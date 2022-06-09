package ru.aiefu.fabricrestart.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.aiefu.fabricrestart.FabricRestart;
import ru.aiefu.fabricrestart.PlayerCountTracker;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerGamePacketListenerMixins {
    @Inject(method = "disconnect", at =@At("HEAD"))
    private void captureTime(Text component, CallbackInfo ci){
        PlayerCountTracker t = FabricRestart.tracker;
        if(t != null ){
            t.setTargetTime(System.currentTimeMillis());
        }
    }
}
