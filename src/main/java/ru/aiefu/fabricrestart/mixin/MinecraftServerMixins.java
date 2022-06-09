package ru.aiefu.fabricrestart.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import ru.aiefu.fabricrestart.FabricRestart;
import ru.aiefu.fabricrestart.ITPS;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixins implements ITPS {
	//Tps measurement

	private long timeRefMs = 0L;
	private final List<Long> samples = new ArrayList<>();
	private final List<Double> tpsSamples = new ArrayList<>();
	private static final double NANO_PER_SECOND_DOUBLE = TimeHelper.SECOND_IN_NANOS;

	private long nanoTimeStart;

	@Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;ticks:I", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
	private void captureMSTimeCall(BooleanSupplier shouldKeepTicking, CallbackInfo ci, long l){
		this.nanoTimeStart = l;
	}


	@Inject(method = "tick", at = @At("TAIL"))
	private void invokeTPSFRCalc(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
		if(FabricRestart.rdata.tpsWatcherEnabled) {
			this.calculateTPSFR(nanoTimeStart, Util.getMeasuringTimeNano());
		}
	}

	@Inject(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tick(Ljava/util/function/BooleanSupplier;)V", shift = At.Shift.AFTER))
	private void updateTrackers(CallbackInfo ci){
		if(FabricRestart.rdata!= null){
			FabricRestart.rdata.update((MinecraftServer) (Object)this, System.currentTimeMillis());
		}
		if(FabricRestart.tracker != null){
			FabricRestart.tracker.playersChecker((MinecraftServer) (Object)this);
		}
	}

	private void calculateTPSFR(long l, long n){
		samples.add(n - l);
		if(n >= timeRefMs || samples.size() > 19){
			timeRefMs = n + TimeHelper.SECOND_IN_NANOS;
			tpsSamples.add(Math.min(20.0D / (samples.stream().mapToLong(Long::longValue).sum() / NANO_PER_SECOND_DOUBLE), 20.0D));
			samples.clear();
			if(tpsSamples.size() > 30){
				tpsSamples.remove(0);
			}
		}
	}

	public double getAverageTPS(){
		return Math.min(tpsSamples.stream().mapToDouble(Double::doubleValue).sum() / tpsSamples.size(), 20.0D);
	}
}
